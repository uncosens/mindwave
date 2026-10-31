package com.uncosens.mindwave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uncosens.mindwave.MainViewModel
import com.uncosens.mindwave.analysis.Analyzer
import com.uncosens.mindwave.analysis.FeatureExtractor
import com.uncosens.mindwave.data.CheckIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")

// ---------- Onboarding ----------

@Composable
private fun Intro(vm: MainViewModel, state: OrbState, title: String, body: String?, button: String, onClick: () -> Unit, extra: @Composable ColumnScope.() -> Unit = {}) = Page {
    Spacer(Modifier.height(96.dp))
    VoiceOrb(vm.recorder.orb, state, size = 160.dp)
    Spacer(Modifier.height(Dimens.xl))
    Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
    if (body != null) {
        Spacer(Modifier.height(Dimens.s))
        Text(body, Modifier.widthIn(max = 300.dp), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
    extra()
    Spacer(Modifier.weight(1f))
    PrimaryButton(button, onClick)
    Spacer(Modifier.height(Dimens.l))
}

@Composable
fun WelcomeScreen(vm: MainViewModel, hasMic: Boolean) = Intro(
    vm, OrbState.Idle, "Hear how you feel.", null,
    "Get started", if (hasMic) vm::openCalibrationIntro else vm::openPermission,
)

// Shown once before the system mic dialog.
@Composable
fun PermissionScreen(vm: MainViewModel, onNeedPermission: (() -> Unit) -> Unit) = Page {
    Spacer(Modifier.weight(1f))
    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        StrokeIcon(Icons.MIC, Modifier.size(28.dp), MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Spacer(Modifier.height(Dimens.l))
    Text("mindwave listens only while the orb is lit.", Modifier.widthIn(max = 300.dp),
        style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
    PrimaryButton("Allow microphone", { onNeedPermission(vm::openCalibrationIntro) })
    Spacer(Modifier.height(Dimens.l))
}

@Composable
fun CalibrationIntroScreen(vm: MainViewModel) = Intro(
    vm, OrbState.Idle, "Let's learn your voice.",
    "Read the same sentence three times. Find a quiet spot.",
    "Start", vm::startCalibration,
) {
    Spacer(Modifier.height(Dimens.m))
    Text("Setup · 1 minute", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ReadyScreen(vm: MainViewModel) = Intro(
    vm, OrbState.Done, "You're all set.", null,
    "Go to today", vm::goHome,
)

@Composable
fun MicDeniedScreen(vm: MainViewModel, openSettings: Boolean, onRetry: () -> Unit) = Intro(
    vm, OrbState.Error, "mindwave needs the microphone", null,
    if (openSettings) "Open settings" else "Allow microphone", onRetry,
)

// ---------- Home ----------

private fun streak(checkIns: List<CheckIn>): Int {
    val days = checkIns.map { LocalDate.parse(it.date) }.toSet()
    var d = LocalDate.now().let { if (it in days) it else it.minusDays(1) }
    var n = 0
    while (d in days) { n++; d = d.minusDays(1) }
    return n
}

@Composable
fun HomeScreen(vm: MainViewModel) = Page {
    val today = LocalDate.now()
    val checkIns = vm.data.checkIns
    val todays = checkIns.lastOrNull()?.takeIf { it.date == today.toString() }
    val hour = LocalTime.now().hour
    val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    val streak = streak(checkIns)

    // Yesterday's (or the latest earlier) record, shown on the card.
    val previous = checkIns.lastOrNull { it.date < today.toString() }

    Spacer(Modifier.height(Dimens.m))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
            Muted(today.format(dayFormat), Modifier.padding(top = 2.dp), align = TextAlign.Start)
        }
        IconButton(onClick = vm::openHistory) { StrokeIcon(Icons.CHART) }
        IconButton(onClick = vm::openSettings, Modifier.offset(x = 12.dp)) { StrokeIcon(Icons.GEAR) }
    }
    if (vm.setupChanged) {
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = vm::openCalibrationIntro).padding(Dimens.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Your phone or microphone changed.", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("Recalibrate", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (streak > 0) {
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.align(Alignment.Start).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = Dimens.s, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(6.dp))
            Text("$streak-day streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    Box(Modifier.fillMaxWidth().padding(top = Dimens.xs).height(220.dp), contentAlignment = Alignment.Center) {
        VoiceOrb(
            vm.recorder.orb, if (todays != null) OrbState.Done else OrbState.Idle, size = 160.dp, valence = todays?.valence ?: 0f,
            modifier = Modifier.clickable(interactionSource = null, indication = null) {
                if (todays != null) vm.openResult(todays) else vm.startCheckIn()
            },
        )
    }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
        Text(if (todays == null) "Today's check-in" else "Today: ${todays.label}", style = MaterialTheme.typography.titleLarge)
        if (previous != null) {
            Spacer(Modifier.height(Dimens.l))
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { vm.openResult(previous) }.padding(Dimens.m),
            ) {
                Text("Last check-in · ${relativeDay(previous.date)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(previous.label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Dimens.s))
                MiniBar("Energy", (previous.arousal + 1) / 2, LocalMindwaveColors.current.arousal)
                MiniBar("Mood", (previous.valence + 1) / 2, LocalMindwaveColors.current.valence)
                MiniBar("Stability", previous.stability, LocalMindwaveColors.current.stability)
            }
        }
    }
    Spacer(Modifier.height(Dimens.m))
    if (todays == null) PrimaryButton("Start check-in", vm::startCheckIn) else {
        PrimaryButton("See today's result", { vm.openResult(todays) })
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton("Redo today's check-in", vm::startCheckIn)
    }
    Spacer(Modifier.height(Dimens.l))
}

private fun relativeDay(iso: String): String {
    val d = LocalDate.parse(iso)
    return when (d) {
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

@Composable
private fun MiniBar(label: String, t: Float, color: Color) = Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, Modifier.width(72.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    StateBar(t, color)
}

// ---------- Result ----------

@Composable
fun ResultScreen(vm: MainViewModel, c: CheckIn) = Page {
    val isToday = c.date == LocalDate.now().toString()
    var detail by remember(c) { mutableStateOf<ShownMetric?>(null) }
    val c3 = LocalMindwaveColors.current
    TopBar("✕", vm::goHome) {
        Text(if (isToday) "Today" else LocalDate.parse(c.date).format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.titleLarge)
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        VoiceOrb(vm.recorder.orb, OrbState.Done, size = Dimens.orbSizeSmall, valence = c.valence)
        Text(c.label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        // Few usual readings yet: show the numbers, but quieter.
        val learning = c.baselineSize < MainViewModel.MIN_BASELINE
        if (learning) {
            Text("Still learning your usual voice", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.alpha(if (learning) 0.5f else 1f)) {
        Spacer(Modifier.height(Dimens.l))
        Card {
            Text("Your state", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.s))
            StateRow("Energy", "calm", "energized", (c.arousal + 1) / 2, c3.arousal)
            StateRow("Mood", "low", "positive", (c.valence + 1) / 2, c3.valence)
            StateRow("Stability", "", "steady", c.stability, c3.stability)
            Spacer(Modifier.height(Dimens.xs))
            val moodLabel = listOf("Very low", "Low", "Okay", "Good", "Great")[c.mood - 1]
            val energyPart = c.energy?.let { " · Energy $it" }.orEmpty()
            Muted("You said ${c.mood} · $moodLabel$energyPart", align = TextAlign.Start)
        }
        Spacer(Modifier.height(Dimens.l))
        Text("Compared to your usual", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.s))
        Card {
            shownMetrics.forEach { m ->
                val z = m.z(c)
                Column(Modifier.fillMaxWidth().clickable { detail = m }) {
                    Row(Modifier.fillMaxWidth().padding(top = Dimens.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.label, style = MaterialTheme.typography.titleMedium)
                        Muted(m.phrase(z))
                    }
                    Spacer(Modifier.height(6.dp))
                    DeviationBar(z)
                    Spacer(Modifier.height(Dimens.xs))
                }
            }
        }
        }
        Spacer(Modifier.height(Dimens.l))
        Text("mindwave is not a medical tool.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.m))
    }
    PrimaryButton("Done", vm::goHome)
    Spacer(Modifier.height(Dimens.l))

    detail?.let { MetricSheet(vm, c, it) { detail = null } }
}

// Today's values next to the usual ones, and this metric over the last 14 check-ins.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSheet(vm: MainViewModel, c: CheckIn, m: ShownMetric, onDismiss: () -> Unit) = ModalBottomSheet(
    onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
) {
    // Values from older feature versions are on another scale, so they get no usual value.
    val base = remember(c.date) { vm.baselineBefore(c.date).takeIf { c.anchor.version == FeatureExtractor.VERSION } }
    val recent = vm.data.checkIns.filter { it.date <= c.date }.takeLast(Analyzer.WINDOW_DAYS)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.l).padding(bottom = Dimens.l)) {
        Text(m.label, style = MaterialTheme.typography.titleLarge)
        Muted(m.phrase(m.z(c)), align = TextAlign.Start)
        Spacer(Modifier.height(Dimens.m))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text("Today", Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium, color = muted, textAlign = TextAlign.End)
            Text("Usual", Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium, color = muted, textAlign = TextAlign.End)
        }
        m.metrics.forEach { metric ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(metric.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(metric.get(c.anchor)?.let(metric::format) ?: "—", Modifier.width(88.dp),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                Text(base?.center(metric)?.let(metric::format) ?: "—", Modifier.width(88.dp),
                    style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(Dimens.m))
        Text("Last ${recent.size} check-ins", style = MaterialTheme.typography.labelMedium, color = muted)
        Spacer(Modifier.height(Dimens.xs))
        Sparkline(recent.map(m.z))
    }
}

@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(Dimens.m),
    content = content,
)

@Composable
private fun StateRow(label: String, left: String, right: String, t: Float, color: Color) =
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.bodyMedium)
        Text(left, Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StateBar(t, color, Modifier.weight(1f), dot = 10)
        Text(right, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

// ---------- Settings ----------

@Composable
fun SettingsScreen(vm: MainViewModel, version: String) = Page {
    var confirm by remember { mutableStateOf(false) }
    TopBar("←", vm::goHome) { Text("Settings", style = MaterialTheme.typography.titleLarge) }
    Spacer(Modifier.height(Dimens.m))
    SettingRow("Recalibrate voice", onClick = vm::openCalibrationIntro)
    SettingRow("Delete all data", MaterialTheme.colorScheme.error) { confirm = true }
    HorizontalDivider(Modifier.padding(vertical = Dimens.m), color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.fillMaxWidth()) {
        Text("About", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.xs))
        Muted("Version $version", align = TextAlign.Start)
        Muted("mindwave is not a medical tool.", align = TextAlign.Start)
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete everything?") },
        text = { Text("This can't be undone.") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.deleteAll() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
}

@Composable
private fun SettingRow(title: String, color: Color = Color.Unspecified, onClick: () -> Unit) =
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Dimens.m)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
    }
