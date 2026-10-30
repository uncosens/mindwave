package com.uncosens.mindwave.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.uncosens.mindwave.MainViewModel
import com.uncosens.mindwave.analysis.TextTools
import com.uncosens.mindwave.data.Sentences

private val StepOrb = 200.dp

// Recording cap for reading a text: generous time per syllable, at least 12 s.
private fun readLimit(text: String) = (TextTools.syllables(text) * 0.4f + 6f).toInt().coerceAtLeast(12)

// One voice capture step: label, prompt, orb, hint or problem, one button.
@Composable
fun VoiceStep(
    vm: MainViewModel,
    top: @Composable () -> Unit,
    onClose: () -> Unit,
    label: String,
    prompt: String,
    promptStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    sub: String? = null,
    transcript: String? = null,
    idleHint: String = "Tap the orb and read aloud",
    startLabel: String = "Start reading",
    minSeconds: Int = 0,
    maxSeconds: Int = 12,
    ring: Boolean = false,
    onNeedPermission: (() -> Unit) -> Unit,
    idleActions: @Composable () -> Unit = {},
) = Page {
    var elapsed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vm.recording) {
        elapsed = 0f
        if (!vm.recording) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (vm.recording) {
            withFrameNanos { elapsed = (it - start) / 1e9f }
            if (elapsed >= maxSeconds) vm.stopRecording()
        }
    }
    val canFinish = elapsed >= minSeconds
    val toggle = {
        when {
            vm.busy -> Unit
            vm.recording -> if (canFinish) vm.stopRecording()
            else -> onNeedPermission(vm::startRecording)
        }
    }
    val state = when {
        vm.problem != null -> OrbState.Error
        vm.recording -> OrbState.Listening
        vm.busy -> OrbState.Analyzing
        else -> OrbState.Idle
    }

    TopBar("✕", onClose, top)
    Spacer(Modifier.height(if (ring) Dimens.l else Dimens.xxl))
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(Dimens.xs))
    Text(prompt, Modifier.widthIn(max = 360.dp), style = promptStyle, textAlign = TextAlign.Center)
    sub?.let { Spacer(Modifier.height(Dimens.xs)); Muted(it) }
    Spacer(Modifier.weight(1f))
    Box(contentAlignment = Alignment.Center) {
        if (ring) {
            CountdownRing(
                progress = elapsed / maxSeconds, ready = vm.recording && canFinish,
                track = MaterialTheme.colorScheme.outlineVariant, idle = MaterialTheme.colorScheme.onSurfaceVariant,
                active = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(StepOrb + Dimens.countdownRingGap * 2),
            )
        }
        VoiceOrb(
            vm.recorder.orb, state, size = StepOrb, basePitchHz = vm.basePitchHz,
            modifier = Modifier.clickable(interactionSource = null, indication = null, onClick = toggle),
        )
    }
    if (ring) {
        val left = (maxSeconds - elapsed.toInt()).coerceAtLeast(0)
        Text("0:%02d".format(left), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (transcript != null) {
        // Show the latest words; older ones scroll off the top.
        val tail = if (transcript.length > 160) "…" + transcript.takeLast(160).substringAfter(' ') else transcript
        Text(
            tail.ifEmpty { if (vm.recording) "…" else " " },
            Modifier.widthIn(max = 360.dp).height(72.dp).fillMaxWidth().padding(top = Dimens.xs),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.weight(1f))
    val p = vm.problem
    if (p != null) {
        Text(p.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Muted(p.body)
    } else {
        Muted(
            when {
                vm.busy -> "One moment…"
                !vm.recording -> idleHint
                !ring -> "Listening…"
                canFinish -> "Tap Done anytime"
                else -> "Keep going…"
            },
        )
    }
    Spacer(Modifier.height(Dimens.m))
    PrimaryButton(
        when {
            vm.recording -> "Done"
            p != null -> "Try again"
            else -> startLabel
        },
        toggle,
        enabled = !vm.busy && (!vm.recording || canFinish),
    )
    Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
        when {
            vm.recording && vm.redoLeft -> TextButton(onClick = vm::redo) { Text("Start over (once)") }
            !vm.recording && !vm.busy -> idleActions()
        }
    }
    Spacer(Modifier.height(Dimens.xs))

    StopDialog(vm)
}

@Composable
private fun StopDialog(vm: MainViewModel) {
    if (vm.confirmStop) AlertDialog(
        onDismissRequest = { vm.confirmStop = false },
        title = { Text("Stop check-in?") },
        text = { Text("Nothing from today will be saved.") },
        confirmButton = { TextButton(onClick = { vm.confirmStop = false }) { Text("Keep going") } },
        dismissButton = { TextButton(onClick = vm::goHome) { Text("Stop") } },
    )
}

@Composable
fun CalibrateScreen(vm: MainViewModel, round: Int, onNeedPermission: (() -> Unit) -> Unit) = VoiceStep(
    vm, top = { Text("$round of 3", style = MaterialTheme.typography.labelLarge) }, onClose = vm::goHome,
    label = "Setup", prompt = "“${Sentences.ANCHOR}”", maxSeconds = readLimit(Sentences.ANCHOR), onNeedPermission = onNeedPermission,
)

@Composable
fun AnchorScreen(vm: MainViewModel, onNeedPermission: (() -> Unit) -> Unit) = VoiceStep(
    vm, top = { StepDots(1) }, onClose = { vm.confirmStop = true },
    label = "Warm up", prompt = "“${Sentences.ANCHOR}”", maxSeconds = readLimit(Sentences.ANCHOR), onNeedPermission = onNeedPermission,
)

@Composable
fun DailyScreen(vm: MainViewModel, onNeedPermission: (() -> Unit) -> Unit) = VoiceStep(
    vm, top = { StepDots(2) }, onClose = { vm.confirmStop = true },
    label = "Sentence of the day", prompt = "“${vm.dailySentence}”", maxSeconds = readLimit(vm.dailySentence), onNeedPermission = onNeedPermission,
)

@Composable
fun FreeTalkScreen(vm: MainViewModel, onNeedPermission: (() -> Unit) -> Unit) {
    val passage = vm.passage
    if (passage == null) VoiceStep(
        vm, top = { StepDots(3) }, onClose = { vm.confirmStop = true },
        label = "Talk freely", prompt = vm.freeTalkPrompt, sub = "Anything is fine. 10–20 s.", transcript = vm.liveText,
        idleHint = "Tap the orb and start talking", startLabel = "Start talking",
        minSeconds = 10, maxSeconds = 20, ring = true, onNeedPermission = onNeedPermission,
    ) {
        TextButton(onClick = vm::choosePassage) { Text("Rather not talk? Read a passage") }
    } else VoiceStep(
        vm, top = { StepDots(3) }, onClose = { vm.confirmStop = true },
        label = "Read aloud", prompt = passage, promptStyle = MaterialTheme.typography.bodyLarge, transcript = vm.liveText,
        maxSeconds = readLimit(passage), onNeedPermission = onNeedPermission,
    ) {
        Row {
            TextButton(onClick = vm::choosePassage) { Text("Another passage") }
            TextButton(onClick = vm::talkInstead) { Text("Talk instead") }
        }
    }
}

@Composable
fun MoodScreen(vm: MainViewModel) = Page {
    var picked by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val labels = listOf("Very low", "Low", "Okay", "Good", "Great")
    TopBar("✕", { vm.confirmStop = true }) { StepDots(4) }
    Spacer(Modifier.weight(1f))
    Text("How do you feel right now?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(Dimens.xxl))
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        for (i in 1..5) {
            val on = picked == i
            val scale by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pick")
            val bg by animateColorAsState(
                if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer, tween(Motion.DUR_SHORT), label = "bg",
            )
            Box(
                Modifier.size(52.dp).scale(scale).clip(CircleShape).background(bg).clickable {
                    if (picked != i) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    picked = i
                },
                contentAlignment = Alignment.Center,
            ) {
                Text("$i", style = MaterialTheme.typography.titleMedium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
    Spacer(Modifier.height(Dimens.s))
    Text(if (picked == 0) " " else labels[picked - 1], style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(Dimens.m))
    Row(Modifier.widthIn(max = 308.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Very low", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Great", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.weight(1f))
    PrimaryButton("See result", { vm.finish(picked) }, enabled = picked > 0)
    Spacer(Modifier.height(Dimens.l))

    StopDialog(vm)
}

@Composable
fun AnalyzingScreen(vm: MainViewModel) = Page {
    Spacer(Modifier.weight(1f))
    VoiceOrb(vm.recorder.orb, OrbState.Analyzing, size = 240.dp)
    Text("Listening back…", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.weight(1f))
}
