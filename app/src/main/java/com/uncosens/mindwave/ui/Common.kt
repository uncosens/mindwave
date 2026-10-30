package com.uncosens.mindwave.ui

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uncosens.mindwave.analysis.Metric
import com.uncosens.mindwave.data.CheckIn
import kotlin.math.abs

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
class ShownMetric(val label: String, val chip: String, val up: String, val down: String, val z: (CheckIn) -> Float)

private fun CheckIn.z(m: Metric) = z[m.name] ?: 0f

val shownMetrics = listOf(
    ShownMetric("Tone", "Tone", "brighter", "darker") { it.z(Metric.ENERGY) },
    ShownMetric("Speech rate", "Speech rate", "faster", "slower") { it.z(Metric.RATE) },
    ShownMetric("Pitch", "Pitch", "higher", "lower") { it.z(Metric.PITCH) },
    ShownMetric("Pitch variety", "Pitch variety", "more varied", "flatter") { it.z(Metric.PITCH_VAR) },
    ShownMetric("Pauses & breath", "Pauses", "more pauses", "fewer pauses") { it.z(Metric.PAUSES) },
    ShownMetric("Voice tremor", "Tremor", "shakier", "steadier") { (it.z(Metric.JITTER) + it.z(Metric.SHIMMER)) / 2 },
)

fun ShownMetric.phrase(z: Float): String = when {
    abs(z) < 0.5f -> "As usual"
    abs(z) <= 1.5f -> "A bit ${if (z > 0) up else down}"
    else -> "Much ${if (z > 0) up else down}"
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
