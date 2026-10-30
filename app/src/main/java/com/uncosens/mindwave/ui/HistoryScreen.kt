package com.uncosens.mindwave.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uncosens.mindwave.MainViewModel
import com.uncosens.mindwave.data.CheckIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private class Line(val values: List<Float>, val color: Color)

private val fmt = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun HistoryScreen(vm: MainViewModel) = Page {
    var range by remember { mutableIntStateOf(1) } // 0: 7 days, 1: 30 days, 2: all
    var metric by remember { mutableIntStateOf(0) }
    val colors = LocalMindwaveColors.current
    val from = when (range) { 0 -> LocalDate.now().minusDays(6); 1 -> LocalDate.now().minusDays(29); else -> LocalDate.MIN }
    val shown = vm.data.checkIns.filter { !LocalDate.parse(it.date).isBefore(from) }

    TopBar("←", vm::goHome) { Text("History", style = MaterialTheme.typography.titleLarge) }
    if (vm.data.checkIns.isEmpty()) {
        Spacer(Modifier.weight(1f))
        VoiceOrb(vm.recorder.orb, OrbState.Idle, size = Dimens.orbSizeSmall)
        Muted("No check-ins yet.")
        Spacer(Modifier.weight(1f))
        PrimaryButton("Check in now", vm::startCheckIn)
        Spacer(Modifier.height(Dimens.l))
        return@Page
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            listOf("7 days", "30 days", "All").forEachIndexed { i, s -> FilterChip(range == i, { range = i }, { Text(s) }) }
        }
        Spacer(Modifier.height(Dimens.s))
        val days = shown.map { LocalDate.parse(it.date).toEpochDay() }
        Card {
            Text("Mood & energy", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.padding(vertical = Dimens.xs), horizontalArrangement = Arrangement.spacedBy(Dimens.m)) {
                Legend("Mood", colors.valence)
                Legend("Energy", colors.arousal)
            }
            Chart(days, listOf(Line(shown.map { it.valence }, colors.valence), Line(shown.map { it.arousal }, colors.arousal)), -1f, 1f, null)
            DateAxis(shown)
        }
        Spacer(Modifier.height(Dimens.m))
        Card {
            val chips = shownMetrics.map { it.chip } + listOf("Stability", "Self rating")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                chips.forEachIndexed { i, s -> FilterChip(metric == i, { metric = i }, { Text(s) }) }
            }
            Spacer(Modifier.height(Dimens.xs))
            when (val i = metric) {
                in shownMetrics.indices -> Chart(days, listOf(Line(shown.map(shownMetrics[i].z), colors.metric)), -2.5f, 2.5f, -1f..1f)
                shownMetrics.size -> Chart(days, listOf(Line(shown.map { it.stability }, colors.stability)), 0f, 1f, null)
                else -> Chart(days, listOf(Line(shown.map { it.mood.toFloat() }, colors.metric)), 1f, 5f, null)
            }
            DateAxis(shown)
        }
        Spacer(Modifier.height(Dimens.m))
        shown.reversed().forEach { c ->
            Row(
                Modifier.fillMaxWidth().height(56.dp).clickable { vm.openResult(c) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(LocalDate.parse(c.date).format(fmt), Modifier.width(72.dp), style = MaterialTheme.typography.titleMedium)
                Muted(c.label, Modifier.weight(1f), align = TextAlign.Start)
                Box(Modifier.size(10.dp).clip(CircleShape).background(OrbPalettes.done(c.valence).c1))
            }
        }
        Spacer(Modifier.height(Dimens.l))
    }
}

@Composable
private fun Legend(label: String, color: Color) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun DateAxis(shown: List<CheckIn>) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    if (shown.isEmpty()) return@Row
    val style = MaterialTheme.typography.labelSmall
    Text(LocalDate.parse(shown.first().date).format(fmt), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (shown.size > 1) Text(LocalDate.parse(shown.last().date).format(fmt), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// Points at each day; consecutive days are joined, missing days leave a gap.
@Composable
private fun Chart(days: List<Long>, lines: List<Line>, lo: Float, hi: Float, band: ClosedFloatingPointRange<Float>?) {
    val zero = MaterialTheme.colorScheme.outlineVariant
    val bandColor = LocalMindwaveColors.current.deviationBand
    Canvas(Modifier.fillMaxWidth().height(200.dp).padding(vertical = Dimens.xs)) {
        if (days.isEmpty()) return@Canvas
        val first = days.first()
        val span = (days.last() - first).coerceAtLeast(1)
        val pad = 6.dp.toPx()
        fun x(d: Long) = if (days.size == 1) size.width / 2 else pad + (size.width - 2 * pad) * (d - first) / span
        fun y(v: Float) = size.height * (1f - (v.coerceIn(lo, hi) - lo) / (hi - lo))
        band?.let { drawRect(bandColor, Offset(0f, y(it.endInclusive)), Size(size.width, y(it.start) - y(it.endInclusive))) }
        val mid = if (lo < 0f && hi > 0f) 0f else (lo + hi) / 2
        drawLine(zero, Offset(0f, y(mid)), Offset(size.width, y(mid)), 1.dp.toPx())
        for (line in lines) {
            if (days.size >= 3) {
                for (i in 1 until days.size) {
                    if (days[i] - days[i - 1] > 1) continue
                    drawLine(line.color, Offset(x(days[i - 1]), y(line.values[i - 1])), Offset(x(days[i]), y(line.values[i])), 2.dp.toPx(), StrokeCap.Round)
                }
            }
            days.indices.forEach { drawCircle(line.color, 2.5.dp.toPx(), Offset(x(days[it]), y(line.values[it]))) }
        }
    }
}
