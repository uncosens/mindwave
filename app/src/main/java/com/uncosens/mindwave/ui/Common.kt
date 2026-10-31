package com.uncosens.mindwave.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uncosens.mindwave.Strings
import com.uncosens.mindwave.analysis.Analyzer
import com.uncosens.mindwave.analysis.Metric
import com.uncosens.mindwave.data.CheckIn
import kotlin.math.abs

// Text in the app language, provided by MainActivity.
val LocalStrings = staticCompositionLocalOf<Strings> { Strings.En }

// The summary label in the current language, whatever language it was stored in.
fun label(c: CheckIn, s: Strings) = Analyzer.label(c.arousal, c.valence, c.stability, s)

@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier.widthIn(max = 480.dp).fillMaxSize().safeDrawingPadding().padding(horizontal = Dimens.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) =
    Button(onClick, Modifier.fillMaxWidth().height(Dimens.buttonHeight), enabled = enabled, shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }

// Line icon from a 24x24 SVG path, drawn with a 1.8 stroke like the prototype.
@Composable
fun StrokeIcon(path: String, modifier: Modifier = Modifier.size(24.dp), color: Color = MaterialTheme.colorScheme.onSurface, width: Float = 1.8f) {
    val p = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier) {
        scale(size.minDimension / 24f, pivot = Offset.Zero) {
            drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

object Icons {
    const val CHART = "M4 19V5M4 19h16M8 15l3-4 3 2 5-6"
    // Material "settings" gear outline, so it is not mistaken for a theme (sun) toggle.
    const val GEAR = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM15.2 12a3.2 3.2 0 1 1-6.4 0a3.2 3.2 0 1 1 6.4 0"
    const val MIC = "M9 6a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0zM5 11a7 7 0 0 0 14 0M12 18v3"
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) =
    OutlinedButton(onClick, Modifier.fillMaxWidth().height(Dimens.buttonHeight), shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }

@Composable
fun MessageScreen(text: String) = Page {
    Spacer(Modifier.weight(1f))
    Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
}

// 56 dp bar with a text glyph button on the left and a centered middle slot.
@Composable
fun TopBar(glyph: String, onClick: () -> Unit, middle: @Composable () -> Unit = {}) =
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        TextButton(onClick, Modifier.align(Alignment.CenterStart)) {
            Text(glyph, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(Modifier.align(Alignment.Center)) { middle() }
    }

// Four step dots: done, current (wide pill), upcoming.
@Composable
fun StepDots(current: Int) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    for (i in 1..4) {
        val color = when {
            i < current -> MaterialTheme.colorScheme.onSurface
            i == current -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }
        Box(Modifier.width(if (i == current) 18.dp else 6.dp).height(6.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) =
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = align)

// Metrics as shown to the user. Tremor merges the two tremor measures.
class ShownMetric(
    val label: String, val chip: String, val up: String, val down: String,
    val metrics: List<Metric>, // what the detail sheet lists values for
    val z: (CheckIn) -> Float,
)

private fun CheckIn.z(m: Metric) = z[m.name] ?: 0f

private fun shown(t: List<String>, metrics: List<Metric>, z: (CheckIn) -> Float) = ShownMetric(t[0], t[1], t[2], t[3], metrics, z)

fun shownMetrics(s: Strings) = listOf(
    shown(s.tone, listOf(Metric.ENERGY)) { it.z(Metric.ENERGY) },
    shown(s.rate, listOf(Metric.RATE)) { it.z(Metric.RATE) },
    shown(s.pitch, listOf(Metric.PITCH)) { it.z(Metric.PITCH) },
    shown(s.pitchVariety, listOf(Metric.PITCH_VAR)) { it.z(Metric.PITCH_VAR) },
    shown(s.pauses, listOf(Metric.PAUSES)) { it.z(Metric.PAUSES) },
    shown(s.tremor, listOf(Metric.JITTER, Metric.SHIMMER)) {
        (it.z(Metric.JITTER) + it.z(Metric.SHIMMER)) / 2
    },
)

// A metric value with its unit, e.g. "182 Hz".
fun Metric.format(v: Float, s: Strings): String = when (this) {
    Metric.PITCH -> "%.0f Hz".format(v)
    Metric.PITCH_VAR -> "%.1f st".format(v)
    Metric.ENERGY -> "%.1f dB".format(v).replace('-', '−')
    Metric.RATE -> "%.1f ${s.syllablesPerSec}".format(v)
    Metric.PAUSES -> "%.0f %%".format(v * 100)
    Metric.JITTER -> "%.2f %%".format(v)
    Metric.SHIMMER -> "%.1f %%".format(v)
}

// Signed number with a real minus sign, e.g. "+0.3" or "−1.2".
fun signed(v: Float): String = "%+.1f".format(v).replace('-', '−')

// Small z-score line with the ±1σ band; the last point is marked.
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val line = LocalMindwaveColors.current.metric
    val band = LocalMindwaveColors.current.deviationBand
    Canvas(modifier.fillMaxWidth().height(64.dp)) {
        fun y(v: Float) = size.height * (1f - (v.coerceIn(-2.5f, 2.5f) + 2.5f) / 5f)
        drawRect(band, Offset(0f, y(1f)), Size(size.width, y(-1f) - y(1f)))
        if (values.isEmpty()) return@Canvas
        val pad = 5.dp.toPx()
        fun x(i: Int) = if (values.size == 1) size.width / 2 else pad + (size.width - 2 * pad) * i / (values.size - 1)
        for (i in 1 until values.size) {
            drawLine(line, Offset(x(i - 1), y(values[i - 1])), Offset(x(i), y(values[i])), 2.dp.toPx(), StrokeCap.Round)
        }
        drawCircle(line, 4.dp.toPx(), Offset(x(values.lastIndex), y(values.last())))
    }
}

fun ShownMetric.phrase(z: Float, s: Strings): String = when {
    abs(z) < 0.5f -> s.asUsual
    abs(z) <= 1.5f -> s.aBit(if (z > 0) up else down)
    else -> s.much(if (z > 0) up else down)
}

// Track from -2σ to +2σ with the ±1σ band and a dot at z.
@Composable
fun DeviationBar(z: Float, modifier: Modifier = Modifier) {
    val c = LocalMindwaveColors.current
    val t = (z.coerceIn(-2f, 2f) + 2f) / 4f
    Box(modifier.fillMaxWidth().height(10.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.deviationTrack))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.weight(2f).height(6.dp).background(c.deviationBand), contentAlignment = Alignment.Center) {
                Box(Modifier.width(1.dp).height(6.dp).background(MaterialTheme.colorScheme.outline))
            }
            Spacer(Modifier.weight(1f))
        }
        DotAt(t, MaterialTheme.colorScheme.onSurface, 10)
    }
}

// Bipolar or unipolar state track with a dot at t in 0..1.
@Composable
fun StateBar(t: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, dot: Int = 8) =
    Box(modifier.fillMaxWidth().height(dot.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(LocalMindwaveColors.current.deviationTrack))
        DotAt(t, color, dot)
    }

@Composable
private fun DotAt(t: Float, color: androidx.compose.ui.graphics.Color, dot: Int) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    val f = t.coerceIn(0.001f, 0.999f)
    Spacer(Modifier.weight(f))
    Box(Modifier.size(dot.dp).clip(CircleShape).background(color))
    Spacer(Modifier.weight(1f - f))
}
