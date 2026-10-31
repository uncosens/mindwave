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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uncosens.mindwave.MainViewModel
import com.uncosens.mindwave.Strings
import com.uncosens.mindwave.data.Lang
import com.uncosens.mindwave.analysis.Analyzer
import com.uncosens.mindwave.analysis.FeatureExtractor
import com.uncosens.mindwave.data.CheckIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ---------- Onboarding ----------

// First launch: pick the app language. Shown in both languages.
@Composable
fun LanguageScreen(vm: MainViewModel) = Page {
    Spacer(Modifier.height(96.dp))
    VoiceOrb(vm.recorder.orb, OrbState.Idle, size = 160.dp)
    Spacer(Modifier.height(Dimens.xl))
    Text("언어를 선택하세요", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Text("Choose your language", style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
    PrimaryButton("한국어", { vm.chooseLanguage(Lang.KO) })
    Spacer(Modifier.height(Dimens.s))
    SecondaryButton("English") { vm.chooseLanguage(Lang.EN) }
    Spacer(Modifier.height(Dimens.l))
}

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
    vm, OrbState.Idle, LocalStrings.current.welcomeTitle, null,
    LocalStrings.current.getStarted, if (hasMic) vm::openCalibrationIntro else vm::openPermission,
)

// Shown once before the system mic dialog.
@Composable
fun PermissionScreen(vm: MainViewModel, onNeedPermission: (() -> Unit) -> Unit) = Page {
    Spacer(Modifier.weight(1f))
    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        StrokeIcon(Icons.MIC, Modifier.size(28.dp), MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Spacer(Modifier.height(Dimens.l))
    Text(LocalStrings.current.micPrompt, Modifier.widthIn(max = 300.dp),
        style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
    PrimaryButton(LocalStrings.current.allowMic, { onNeedPermission(vm::openCalibrationIntro) })
    Spacer(Modifier.height(Dimens.l))
}

@Composable
fun CalibrationIntroScreen(vm: MainViewModel) = Intro(
    vm, OrbState.Idle, LocalStrings.current.calibTitle,
    LocalStrings.current.calibBody,
    LocalStrings.current.start, vm::startCalibration,
) {
    Spacer(Modifier.height(Dimens.m))
    Text(LocalStrings.current.setupTime, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ReadyScreen(vm: MainViewModel) = Intro(
    vm, OrbState.Done, LocalStrings.current.readyTitle, null,
    LocalStrings.current.goToday, vm::goHome,
)

@Composable
fun MicDeniedScreen(vm: MainViewModel, openSettings: Boolean, onRetry: () -> Unit) = Intro(
    vm, OrbState.Error, LocalStrings.current.micNeeded, null,
    if (openSettings) LocalStrings.current.openSettings else LocalStrings.current.allowMic, onRetry,
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
    val s = LocalStrings.current
    val today = LocalDate.now()
    val checkIns = vm.data.checkIns
    val todays = checkIns.lastOrNull()?.takeIf { it.date == today.toString() }
    val hour = LocalTime.now().hour
    val greeting = when (hour) { in 5..11 -> s.morning; in 12..17 -> s.afternoon; else -> s.evening }
    val streak = streak(checkIns)

    // Yesterday's (or the latest earlier) record, shown on the card.
    val previous = checkIns.lastOrNull { it.date < today.toString() }

    Spacer(Modifier.height(Dimens.m))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
            Muted(today.format(DateTimeFormatter.ofPattern(s.dayPattern, s.locale)), Modifier.padding(top = 2.dp), align = TextAlign.Start)
        }
        IconButton(onClick = vm::openHistory, Modifier.semantics { contentDescription = s.history }) { StrokeIcon(Icons.CHART) }
        IconButton(onClick = vm::openSettings, Modifier.offset(x = 12.dp).semantics { contentDescription = s.settings }) {
            StrokeIcon(Icons.GEAR, width = 1.5f)
        }
    }
    if (vm.setupChanged) {
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = vm::openCalibrationIntro).padding(Dimens.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.setupChanged, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(s.recalibrate, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
            Text(s.streak(streak), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
        Text(if (todays == null) s.todaysCheckIn else s.todayIs(label(todays, s)), style = MaterialTheme.typography.titleLarge)
        if (previous != null) {
            Spacer(Modifier.height(Dimens.l))
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { vm.openResult(previous) }.padding(Dimens.m),
            ) {
                Text(s.lastCheckIn(relativeDay(previous.date, s)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(label(previous, s), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Dimens.s))
                MiniBar(s.energy, (previous.arousal + 1) / 2, LocalMindwaveColors.current.arousal)
                MiniBar(s.mood, (previous.valence + 1) / 2, LocalMindwaveColors.current.valence)
                MiniBar(s.stability, previous.stability, LocalMindwaveColors.current.stability)
            }
        }
    }
    Spacer(Modifier.height(Dimens.m))
    if (todays == null) PrimaryButton(s.startCheckIn, vm::startCheckIn) else {
        PrimaryButton(s.seeTodaysResult, { vm.openResult(todays) })
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(s.redoToday, vm::startCheckIn)
    }
    Spacer(Modifier.height(Dimens.l))
}

private fun relativeDay(iso: String, s: Strings): String {
    val d = LocalDate.parse(iso)
    return when (d) {
        LocalDate.now().minusDays(1) -> s.yesterday
        else -> d.format(DateTimeFormatter.ofPattern(s.shortPattern, s.locale))
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
    val s = LocalStrings.current
    val isToday = c.date == LocalDate.now().toString()
    var detail by remember(c) { mutableStateOf<ShownMetric?>(null) }
    val c3 = LocalMindwaveColors.current
    TopBar("✕", vm::goHome) {
        Text(if (isToday) s.today else LocalDate.parse(c.date).format(DateTimeFormatter.ofPattern(s.shortPattern, s.locale)), style = MaterialTheme.typography.titleLarge)
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        VoiceOrb(vm.recorder.orb, OrbState.Done, size = Dimens.orbSizeSmall, valence = c.valence)
        Text(label(c, s), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        // Few usual readings yet: show the numbers, but quieter.
        val learning = c.baselineSize < MainViewModel.MIN_BASELINE
        if (learning) {
            Text(s.learning, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.alpha(if (learning) 0.5f else 1f)) {
        Spacer(Modifier.height(Dimens.l))
        Card {
            Text(s.yourState, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.s))
            StateRow(s.energy, s.calm, s.energized, (c.arousal + 1) / 2, c3.arousal)
            StateRow(s.mood, s.low, s.positive, (c.valence + 1) / 2, c3.valence)
            StateRow(s.stability, "", s.steady, c.stability, c3.stability)
            Spacer(Modifier.height(Dimens.xs))
            Muted(s.youSaid(c.mood, s.moodLabels[c.mood - 1], c.energy), align = TextAlign.Start)
        }
        Spacer(Modifier.height(Dimens.l))
        Text(s.comparedToUsual, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.s))
        Card {
            shownMetrics(s).forEach { m ->
                val z = m.z(c)
                Column(Modifier.fillMaxWidth().clickable { detail = m }) {
                    Row(Modifier.fillMaxWidth().padding(top = Dimens.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.label, style = MaterialTheme.typography.titleMedium)
                        Muted(m.phrase(z, s))
                    }
                    Spacer(Modifier.height(6.dp))
                    DeviationBar(z)
                    Spacer(Modifier.height(Dimens.xs))
                }
            }
        }
        }
        Spacer(Modifier.height(Dimens.l))
        Text(s.notMedical, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.m))
    }
    PrimaryButton(s.done, vm::goHome)
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
    val s = LocalStrings.current
    // Values from older feature versions are on another scale, so they get no usual value.
    val base = remember(c.date) { vm.baselineBefore(c.date).takeIf { c.anchor.version == FeatureExtractor.VERSION } }
    val recent = vm.data.checkIns.filter { it.date <= c.date }.takeLast(Analyzer.WINDOW_DAYS)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.l).padding(bottom = Dimens.l)) {
        Text(m.label, style = MaterialTheme.typography.titleLarge)
        Muted(m.phrase(m.z(c), s), align = TextAlign.Start)
        Spacer(Modifier.height(Dimens.m))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(s.today, Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium, color = muted, textAlign = TextAlign.End)
            Text(s.usual, Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium, color = muted, textAlign = TextAlign.End)
        }
        m.metrics.forEach { metric ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(s.metricName(metric), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(metric.get(c.anchor)?.let { metric.format(it, s) } ?: "—", Modifier.width(88.dp),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                Text(base?.center(metric)?.let { metric.format(it, s) } ?: "—", Modifier.width(88.dp),
                    style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(Dimens.m))
        Text(s.lastN(recent.size), style = MaterialTheme.typography.labelMedium, color = muted)
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
    val s = LocalStrings.current
    var confirm by remember { mutableStateOf(false) }
    TopBar("←", vm::goHome) { Text(s.settings, style = MaterialTheme.typography.titleLarge) }
    Spacer(Modifier.height(Dimens.m))
    SettingRow(s.recalibrateVoice, onClick = vm::openCalibrationIntro)
    SettingRow(s.deleteAll, MaterialTheme.colorScheme.error) { confirm = true }
    HorizontalDivider(Modifier.padding(vertical = Dimens.m), color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.fillMaxWidth()) {
        Text(s.about, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.xs))
        Muted(s.version(version), align = TextAlign.Start)
        Muted(s.notMedical, align = TextAlign.Start)
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(s.deleteTitle) },
        text = { Text(s.deleteBody) },
        confirmButton = { TextButton(onClick = { confirm = false; vm.deleteAll() }) { Text(s.delete, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(s.cancel) } },
    )
}

@Composable
private fun SettingRow(title: String, color: Color = Color.Unspecified, onClick: () -> Unit) =
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Dimens.m)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
    }
