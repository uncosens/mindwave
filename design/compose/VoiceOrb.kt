// Reference voice orb for Mindwave. Math is specified in design/DESIGN.md section 5.
// AGSL RuntimeShader on API 33+, Canvas Path fallback below. Change the package to match the app.
package com.mindwave.ui.orb

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

enum class OrbState { Idle, Listening, Analyzing, Done, Error }

/** Written by the audio thread, read by the orb once per frame. */
class VoiceOrbInput {
    @Volatile var level: Float = 0f      // 0..1, see DESIGN.md 5.1
    @Volatile var pitchHz: Float = 0f    // 0 = unvoiced
    @Volatile var speaking: Boolean = false
}

data class OrbPalette(val c1: Color, val c2: Color, val c3: Color, val glow: Float)

object OrbPalettes {
    val Idle = OrbPalette(Color(0xFFA9B8FF), Color(0xFFD3C6FF), Color(0xFFF4F2FF), 0.25f)
    val Listening = OrbPalette(Color(0xFF4F6BFF), Color(0xFF9B7BFF), Color(0xFF6FE3FF), 0.45f)
    val Analyzing = OrbPalette(Color(0xFF7C5CFF), Color(0xFFFF6FB5), Color(0xFFFFC48A), 0.40f)
    val DoneLow = OrbPalette(Color(0xFF5E86D9), Color(0xFF8FB3FF), Color(0xFFD6E2FF), 0.30f)
    val DoneNeutral = OrbPalette(Color(0xFF3FB59A), Color(0xFF9BE3C9), Color(0xFFE6F8F0), 0.30f)
    val DoneHigh = OrbPalette(Color(0xFFFF8E5E), Color(0xFFFF7FA0), Color(0xFFFFE0B0), 0.30f)
    val Error = OrbPalette(Color(0xFF9A9AA3), Color(0xFFC4C2D6), Color(0xFFECECEF), 0.15f)

    /** valence in -1..1 */
    fun done(valence: Float): OrbPalette {
        val v = valence.coerceIn(-1f, 1f)
        val (a, b, t) = if (v < 0) Triple(DoneLow, DoneNeutral, v + 1f) else Triple(DoneNeutral, DoneHigh, v)
        return OrbPalette(lerp(a.c1, b.c1, t), lerp(a.c2, b.c2, t), lerp(a.c3, b.c3, t), a.glow + (b.glow - a.glow) * t)
    }

    fun of(state: OrbState, valence: Float) = when (state) {
        OrbState.Idle -> Idle
        OrbState.Listening -> Listening
        OrbState.Analyzing -> Analyzing
        OrbState.Done -> done(valence)
        OrbState.Error -> Error
    }
}

/** Per-frame smoothed values. Plain fields: changes do not trigger recomposition. */
private class OrbMotion {
    var time = 0f
    var level = 0f
    var speak = 0f
    var pitch = 0.5f
    var phase = 0f
    var amp = 0.03f
    var scale = 1f
    var scaleVel = 0f
    var flow = 1f
}

private fun k(dt: Float, tau: Float) = 1f - exp(-dt / tau)

private fun OrbMotion.step(dt: Float, input: VoiceOrbInput, state: OrbState, basePitchHz: Float, reduced: Boolean) {
    time += dt
    val listening = state == OrbState.Listening
    val rawLevel = when (state) {
        OrbState.Listening -> input.level
        OrbState.Analyzing -> 0.15f + 0.10f * sin(2f * PI.toFloat() * time / 1.2f)
        else -> 0f
    }
    level += (rawLevel - level) * k(dt, if (rawLevel > level) 0.06f else 0.25f)
    speak += ((if (listening && input.speaking) 1f else 0f) - speak) * k(dt, 0.30f)

    val hz = if (listening) input.pitchHz else 0f
    val pitchTarget = when {
        state == OrbState.Analyzing -> 0.5f + 0.2f * sin(time)
        hz > 0f -> (0.5f + 0.5f * (ln(hz / basePitchHz) / ln(2f))).coerceIn(0f, 1f)
        else -> 0.5f
    }
    pitch += (pitchTarget - pitch) * k(dt, if (hz > 0f) 0.15f else 1.0f)

    if (!reduced) phase += dt * (0.35f + 1.6f * level)
    flow += ((if (state == OrbState.Analyzing) 2f else 1f) - flow) * k(dt, 0.3f)

    val ampTarget = (if (state == OrbState.Done) 0.02f else 0.03f + 0.10f * level + 0.02f * speak) *
        (if (reduced) 0.5f else 1f)
    amp += (ampTarget - amp) * k(dt, 0.25f)

    // Spring toward target scale (damping 0.55, stiffness 180).
    val breath = if (reduced) 0f else sin(2f * PI.toFloat() * time / 5f)
    val base = when (state) {
        OrbState.Analyzing -> 0.85f
        OrbState.Error -> 0.92f
        else -> 1f
    }
    val target = base + 0.025f * breath * (1f - speak) + 0.16f * level
    val stiffness = 180f
    val damping = 2f * 0.55f * kotlin.math.sqrt(stiffness)
    scaleVel += (stiffness * (target - scale) - damping * scaleVel) * dt
    scale += scaleVel * dt
}

/**
 * The voice orb. [size] is the orb body; the canvas adds glow padding around it.
 * Feed [input] from the audio thread. [valence] tints the Done state.
 */
@Composable
fun VoiceOrb(
    input: VoiceOrbInput,
    state: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    valence: Float = 0f,
    basePitchHz: Float = 165f,
) {
    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val target = OrbPalettes.of(state, valence)
    val anim = tween<Color>(300)
    val c1 = animateColorAsState(target.c1, anim, label = "c1")
    val c2 = animateColorAsState(target.c2, anim, label = "c2")
    val c3 = animateColorAsState(target.c3, anim, label = "c3")
    val glow = animateFloatAsState(target.glow, tween(300), label = "glow")

    val motion = remember { OrbMotion() }
    val frame = remember { mutableFloatStateOf(0f) }
    val currentState by rememberUpdatedState(state)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 1f / 30f)
                last = now
                motion.step(dt, input, currentState, basePitchHz, reduced)
                frame.floatValue = motion.time // invalidates draw only
            }
        }
    }

    // Glow padding: the body radius is 0.62 of half the canvas (see DESIGN.md 5.3).
    val canvasSize = size / 0.62f
    Box(modifier.size(canvasSize)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ShaderOrb(Modifier.size(canvasSize), frame, motion, c1, c2, c3, glow)
        } else {
            PathOrb(Modifier.size(canvasSize), frame, motion, c1, c2, c3, glow)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderOrb(
    modifier: Modifier,
    frame: State<Float>,
    m: OrbMotion,
    c1: State<Color>, c2: State<Color>, c3: State<Color>, glow: State<Float>,
) {
    val shader = remember { RuntimeShader(ORB_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(modifier) {
        frame.value // subscribe to frame ticks
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uPhase", m.phase)
        shader.setFloatUniform("uScale", m.scale)
        shader.setFloatUniform("uAmp", m.amp)
        shader.setFloatUniform("uLevel", m.level)
        shader.setFloatUniform("uPitch", m.pitch)
        shader.setFloatUniform("uFlow", m.flow)
        shader.setFloatUniform("uGlow", glow.value)
        shader.setColor("uC1", c1.value)
        shader.setColor("uC2", c2.value)
        shader.setColor("uC3", c3.value)
        drawRect(brush)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setColor(name: String, c: Color) = setFloatUniform(name, c.red, c.green, c.blue)

@Composable
private fun PathOrb(
    modifier: Modifier,
    frame: State<Float>,
    m: OrbMotion,
    c1: State<Color>, c2: State<Color>, c3: State<Color>, glow: State<Float>,
) {
    val path = remember { Path() }
    val xs = remember { FloatArray(POINTS) }
    val ys = remember { FloatArray(POINTS) }
    Canvas(modifier) {
        frame.value
        drawPathOrb(path, xs, ys, m, c1.value, c2.value, c3.value, glow.value)
    }
}

private const val POINTS = 96
private const val R0 = 0.62f

private fun radiusAt(screenAngle: Float, m: OrbMotion): Float {
    val a = screenAngle + 0.15f * m.phase
    val p = m.pitch
    val ph = m.phase
    val wob = (0.55f - 0.20f * p) * sin(2f * a + ph) +
        0.35f * sin(3f * a - 1.3f * ph + 1.7f) +
        (0.15f + 0.35f * p) * sin(5f * a + 1.9f * ph + 4.1f) +
        (0.10f + 0.15f * p) * sin(7f * a - 2.3f * ph + 2.3f)
    return R0 * m.scale * (1f + m.amp * wob)
}

private fun DrawScope.drawPathOrb(
    path: Path, xs: FloatArray, ys: FloatArray, m: OrbMotion,
    c1: Color, c2: Color, c3: Color, glowA: Float,
) {
    val half = size.minDimension / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val bodyR = R0 * m.scale * half

    // Glow first, behind the body.
    val g = glowA * (0.6f + 0.4f * m.level)
    drawCircle(
        Brush.radialGradient(
            0f to c1.copy(alpha = g), 0.62f to c1.copy(alpha = g), 1f to Color.Transparent,
            center = Offset(cx, cy), radius = half,
        ),
        radius = half, center = Offset(cx, cy),
    )

    for (i in 0 until POINTS) {
        val ang = (2.0 * PI * i / POINTS).toFloat()
        val r = radiusAt(ang, m) * half
        xs[i] = cx + cos(ang) * r
        ys[i] = cy - sin(ang) * r // y up in the math
    }
    path.rewind()
    path.moveTo((xs[POINTS - 1] + xs[0]) / 2f, (ys[POINTS - 1] + ys[0]) / 2f)
    for (i in 0 until POINTS) {
        val n = (i + 1) % POINTS
        path.quadraticTo(xs[i], ys[i], (xs[i] + xs[n]) / 2f, (ys[i] + ys[n]) / 2f)
    }
    path.close()

    val th = m.flow * 0.4f * m.phase + (m.pitch - 0.5f) * 0.7f
    val dx = cos(th) * bodyR
    val dy = -sin(th) * bodyR
    drawPath(path, Brush.linearGradient(listOf(c1, c2), Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy)))
    val hl = Offset(cx - 0.30f * bodyR, cy - 0.38f * bodyR)
    drawPath(
        path,
        Brush.radialGradient(
            listOf(c3.copy(alpha = 0.55f + 0.35f * m.pitch), Color.Transparent),
            center = hl, radius = 0.95f * bodyR,
        ),
    )
}

/** Thin countdown ring for free talk (DESIGN.md 4.5). [progress] 0..1 over 20 s. */
@Composable
fun CountdownRing(progress: Float, ready: Boolean, track: Color, idle: Color, active: Color, modifier: Modifier = Modifier) {
    val color by animateColorAsState(if (ready) active else idle, tween(300), label = "ring")
    Canvas(modifier.padding(1.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(track, 0f, 360f, false, style = stroke)
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, style = stroke)
        // 10 s tick at the bottom.
        val r = size.minDimension / 2f
        drawCircle(if (ready) active else track, 2.dp.toPx(), Offset(size.width / 2f, size.height / 2f + r))
    }
}

// Same math as DESIGN.md 5.3–5.4. y is flipped so +y points up.
private const val ORB_AGSL = """
uniform float2 uSize;
uniform float uPhase;
uniform float uScale;
uniform float uAmp;
uniform float uLevel;
uniform float uPitch;
uniform float uFlow;
uniform float uGlow;
uniform float3 uC1;
uniform float3 uC2;
uniform float3 uC3;

half4 main(float2 fragCoord) {
    float s = min(uSize.x, uSize.y);
    float2 p = (fragCoord - 0.5 * uSize) / (0.5 * s);
    p.y = -p.y;
    float r = length(p);
    float a = atan(p.y, p.x) + 0.15 * uPhase;
    float P = uPitch;
    float wob = (0.55 - 0.20 * P) * sin(2.0 * a + uPhase)
              + 0.35 * sin(3.0 * a - 1.3 * uPhase + 1.7)
              + (0.15 + 0.35 * P) * sin(5.0 * a + 1.9 * uPhase + 4.1)
              + (0.10 + 0.15 * P) * sin(7.0 * a - 2.3 * uPhase + 2.3);
    float R = 0.62 * uScale * (1.0 + uAmp * wob);
    float aa = 3.0 / s;
    float alpha = 1.0 - smoothstep(R - aa, R + aa, r);

    float2 q = p / R + 0.25 * float2(sin(2.2 * p.y + 0.9 * uPhase), cos(2.0 * p.x - 0.7 * uPhase));
    float th = uFlow * 0.4 * uPhase + (P - 0.5) * 0.7;
    float g = dot(q, float2(cos(th), sin(th)));
    float3 col = mix(uC1, uC2, smoothstep(-0.8, 0.8, g));
    float hl = 1.0 - length(p - float2(-0.30, 0.38) * R) / (0.95 * R);
    col = mix(col, uC3, smoothstep(0.25, 1.0, hl) * (0.55 + 0.35 * P));
    col *= 0.90 + 0.10 * (1.0 - clamp(r / R, 0.0, 1.0));

    float glow = uGlow * (0.6 + 0.4 * uLevel) * exp(-max(r - R, 0.0) * 7.0 / 0.62);
    float3 rgb = col * alpha + uC1 * glow * (1.0 - alpha);
    float outA = alpha + glow * (1.0 - alpha);
    return half4(half3(rgb), half(outA));
}
"""
