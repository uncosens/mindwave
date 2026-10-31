package com.uncosens.mindwave.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

// The five slices of the "Slices" mark, left to right, in the 108-unit space of ic_launcher_foreground.xml.
private val SLICES = listOf(
    "M36.59,48.91L36.44,48.31L36.13,47.77L35.69,47.34L35.16,47.03L34.56,46.86L33.95,46.85L33.65,46.91L33.07,47.13L32.58,47.50L32.19,47.99L32.06,48.26L31.81,49.09L31.55,50.23L31.27,52.40L31.23,54.58L31.42,56.79L31.87,59.12L32.59,61.57L33.54,63.97L34.42,65.78L35.32,67.33L35.53,67.56L36.04,67.93L36.64,68.16L37.27,68.22L37.57,68.18L38.18,68.01L38.71,67.66L38.94,67.44L39.29,66.91L39.42,66.63L39.54,66.00L39.54,65.69L37.65,55.79L37.05,52.22L36.59,48.91Z",
    "M40.24,36.70L38.84,37.91L38.51,38.42L38.39,38.70L38.27,39.31L38.16,42.39L38.23,45.95L38.46,49.08L38.84,52.21L39.35,55.34L40.07,58.92L40.91,62.49L41.97,66.50L43.29,70.97L44.32,74.16L44.49,74.45L44.93,74.93L45.47,75.29L46.49,75.58L46.80,75.64L47.43,75.62L48.03,75.44L48.31,75.29L48.79,74.88L49.15,74.37L49.36,73.78L49.41,73.47L49.33,71.42L48.79,62.49L48.14,55.34L47.23,48.19L45.48,36.83L45.22,36.25L45.05,35.99L44.58,35.56L44.31,35.39L43.72,35.17L43.09,35.12L42.77,35.16L42.18,35.35L41.49,35.78L40.24,36.70Z",
    "M52.10,32.17L49.80,32.42L49.22,32.66L48.73,33.06L48.53,33.29L48.24,33.84L48.08,34.46L48.17,37.03L48.41,41.04L49.24,49.53L51.13,65.17L51.81,70.08L52.43,74.08L52.62,74.69L52.78,74.98L52.97,75.23L53.44,75.66L54.02,75.94L54.33,76.02L54.97,76.06L57.02,75.56L57.82,75.23L58.30,74.81L58.51,74.56L58.80,73.98L58.87,73.66L59.28,65.17L59.40,58.47L59.24,53.10L59.06,50.42L58.80,47.75L58.25,43.73L57.58,40.16L56.51,35.69L56.02,33.89L55.79,33.34L55.62,33.09L55.21,32.66L54.70,32.37L54.13,32.19L52.10,32.17Z",
    "M62.63,34.88L62.01,34.78L61.36,34.84L60.77,35.06L60.50,35.23L60.25,35.44L59.86,35.94L59.61,36.53L59.54,37.16L60.33,43.72L60.85,49.98L61.13,55.34L61.47,66.51L61.66,70.43L61.84,71.01L61.98,71.27L62.37,71.74L62.86,72.10L63.14,72.23L63.43,72.31L64.03,72.38L64.63,72.30L65.20,72.06L67.28,70.71L68.91,69.43L69.27,68.88L69.41,68.58L69.52,67.92L69.64,63.38L69.63,59.36L69.55,55.79L69.25,50.87L68.74,45.95L68.02,41.49L67.56,39.25L67.08,37.37L66.77,36.83L66.57,36.59L66.08,36.21L64.58,35.57L62.63,34.88Z",
    "M71.45,58.29L71.56,58.91L71.82,59.47L72.23,59.94L72.74,60.30L73.32,60.50L73.94,60.56L74.55,60.44L75.11,60.18L75.36,59.99L75.58,59.78L75.93,59.27L76.51,57.57L77.03,55.40L77.35,53.17L77.43,51.11L77.30,49.05L76.97,47.18L76.44,45.37L75.62,43.48L75.09,42.53L74.66,41.90L74.18,41.49L73.91,41.32L73.32,41.12L73.01,41.09L72.69,41.09L72.07,41.22L71.52,41.50L71.27,41.70L71.04,41.93L70.71,42.46L70.59,42.76L70.50,43.37L71.01,49.98L71.45,58.29Z",
)
private const val LEFT = 28f // crop of the 108-unit space around the mark
private const val SPAN = 52f

private val Fill = Brush.linearGradient(
    0f to Color(0xFF3554FF), 0.5f to Color(0xFF8366FF), 1f to Color(0xFFDB86F2),
    start = Offset(35.02f, 75.35f), end = Offset(72.97f, 32.65f),
)
private val Glow = Brush.radialGradient(
    listOf(Color(0x996FE3FF), Color(0x006FE3FF)), center = Offset(43.80f, 41.43f), radius = 27.28f,
)

// The logo with its slices swelling in turn, like a voice level meter. Shown while the speech model loads.
@Composable
fun LogoPulse(modifier: Modifier = Modifier) {
    val paths = remember { SLICES.map { PathParser().parsePathString(it).toPath() } }
    val phase by rememberInfiniteTransition(label = "logo").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    Canvas(modifier) {
        val unit = size.minDimension / SPAN
        scale(unit, pivot = Offset.Zero) {
            translate(-LEFT, -LEFT) {
                paths.forEachIndexed { i, p ->
                    // Each slice lags the one before it, so a wave runs across the mark.
                    val k = 0.7f + 0.3f * (0.5f + 0.5f * sin(2 * PI * (phase - i * 0.16f)).toFloat())
                    val b = p.getBounds()
                    scale(1f, k, pivot = b.center) {
                        drawPath(p, Fill)
                        drawPath(p, Glow)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(label: String) = Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
) {
    LogoPulse(Modifier.size(120.dp))
}
