package com.uncosens.mindwave.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.uncosens.mindwave.MainViewModel
import com.uncosens.mindwave.Screen
import com.uncosens.mindwave.analysis.TextTools

private val StepOrb = 200.dp

// Recording cap for reading a text: generous time per syllable, at least 12 s.
private fun readLimit(text: String) = (TextTools.syllables(text) * 0.4f + 6f).toInt().coerceAtLeast(12)

// What one step shows above the orb. `reading` is the text to read aloud, if any.
private data class StepText(val label: String, val prompt: String, val sub: String?, val reading: String?, val small: Boolean)

// One voice capture step: label, prompt, orb, hint or problem, one button. Only called from VoiceStepScreen.
@Composable
private fun VoiceStep(
    vm: MainViewModel,
    top: @Composable () -> Unit,
    onClose: () -> Unit,
    text: StepText,
    transcript: String? = null,
    idleHint: String,
    startLabel: String,
    minSeconds: Int = 0,
    maxSeconds: Int = 12,
    ring: Boolean = false,
    onNeedPermission: (() -> Unit) -> Unit,
    idleActions: @Composable () -> Unit = {},
) = Page {
    val s = LocalStrings.current
    // Words read so far; never goes backwards within a take.
    val target = remember(text.reading) { text.reading?.let(TextTools::words).orEmpty() }
    var read by remember(text.reading) { mutableIntStateOf(0) }
    LaunchedEffect(vm.recording) { if (vm.recording) read = 0 }
    LaunchedEffect(vm.liveText) { read = maxOf(read, TextTools.readProgress(target, vm.liveText)) }

    var elapsed by remember { mutableFloatStateOf(0f) }
    var quiet by remember { mutableStateOf(false) }
    LaunchedEffect(vm.recording) {
        elapsed = 0f
        quiet = false
        if (!vm.recording) return@LaunchedEffect
        val start = withFrameNanos { it }
        var lastVoice = start
        var silent = 0f
        while (vm.recording) {
            withFrameNanos {
                elapsed = (it - start) / 1e9f
                if (vm.recorder.orb.speaking) lastVoice = it
                silent = (it - lastVoice) / 1e9f
            }
            quiet = vm.recorder.tooQuiet
            // Reading steps finish by themselves once the whole text has been heard and the voice stops.
            val doneReading = target.isNotEmpty() &&
                ((read >= target.size && silent > 0.5f) || (read >= target.size - 1 && silent > 1.2f))
            if (elapsed >= maxSeconds || doneReading) {
                android.util.Log.i("mindwave", "auto stop at $elapsed s: read $read of ${target.size}, silent $silent s")
                vm.stopRecording()
            }
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
    val gap by animateDpAsState(if (ring) Dimens.l else Dimens.xxl, tween(Motion.DUR_MEDIUM, easing = Motion.EaseStandard), label = "gap")
    Spacer(Modifier.height(gap))
    val rise = with(LocalDensity.current) { 24.dp.roundToPx() }
    AnimatedContent(
        text,
        transitionSpec = {
            (fadeIn(tween(Motion.DUR_MEDIUM)) + slideInVertically(tween(Motion.DUR_MEDIUM, easing = Motion.EaseStandard)) { rise }) togetherWith
                fadeOut(tween(Motion.DUR_MEDIUM))
        },
        label = "step",
    ) { t ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(t.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.xs))
            val style = if (t.small) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineMedium
            // Dim unread words only while this take is being heard.
            val progress = if (t.reading != null && t == text && (vm.recording || vm.busy)) read else null
            ReadingText(t.prompt, progress, Modifier.widthIn(max = 360.dp), style)
            t.sub?.let { Spacer(Modifier.height(Dimens.xs)); Muted(it) }
        }
    }
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
                vm.busy -> s.oneMoment
                !vm.recording -> idleHint
                quiet -> s.tooQuietLive
                !ring -> s.listening
                canFinish -> s.tapDoneAnytime
                else -> s.keepGoing
            },
        )
    }
    Spacer(Modifier.height(Dimens.m))
    PrimaryButton(
        when {
            vm.recording -> s.done
            p != null -> s.tryAgain
            else -> startLabel
        },
        toggle,
        enabled = !vm.busy && (!vm.recording || canFinish),
    )
    // Same height in every state so the primary button does not jump.
    Box(Modifier.padding(top = Dimens.s).height(Dimens.buttonHeight), contentAlignment = Alignment.Center) {
        when {
            vm.recording && vm.redoLeft -> SecondaryButton(s.tryAgain, vm::redo)
            !vm.recording && !vm.busy -> idleActions()
        }
    }
    Spacer(Modifier.height(Dimens.xs))

    StopDialog(vm)
}

@Composable
private fun StopDialog(vm: MainViewModel) {
    val s = LocalStrings.current
    if (vm.confirmStop) AlertDialog(
        onDismissRequest = { vm.confirmStop = false },
        title = { Text(s.stopTitle) },
        text = { Text(s.stopBody) },
        confirmButton = { TextButton(onClick = { vm.confirmStop = false }) { Text(s.keepGoingButton) } },
        dismissButton = { TextButton(onClick = vm::goHome) { Text(s.stop) } },
    )
}

private fun reading(label: String, text: String) = StepText(label, "“$text”", null, text, small = false)

// Calibration rounds and check-in steps A-C. VoiceStep is called from one place so the
// orb keeps its state across steps; a call per branch would recreate it.
@Composable
fun VoiceStepScreen(vm: MainViewModel, screen: Screen, onNeedPermission: (() -> Unit) -> Unit) {
    val s = LocalStrings.current
    val passage = vm.passage
    val talk = screen == Screen.FreeTalk && passage == null
    val text = when {
        screen is Screen.Calibrate -> reading(s.stepSetup, vm.anchorText)
        screen == Screen.ReadAnchor -> reading(s.stepWarmUp, vm.anchorText)
        screen == Screen.ReadDaily -> reading(s.stepDaily, vm.dailySentence)
        talk -> StepText(s.stepTalk, vm.freeTalkPrompt, s.talkSub, null, small = false)
        else -> StepText(s.stepReadAloud, passage!!, null, passage, small = true)
    }
    VoiceStep(
        vm,
        top = {
            when (screen) {
                is Screen.Calibrate -> Text(s.roundOf(screen.round), style = MaterialTheme.typography.labelLarge)
                Screen.ReadAnchor -> StepDots(1)
                Screen.ReadDaily -> StepDots(2)
                else -> StepDots(3)
            }
        },
        onClose = if (screen is Screen.Calibrate) vm::goHome else { { vm.confirmStop = true } },
        text = text,
        // Reading steps show progress through the highlight, so only free talk shows a transcript.
        transcript = if (talk) vm.liveText else null,
        idleHint = if (talk) s.tapAndTalk else s.tapAndRead,
        startLabel = if (talk) s.startTalking else s.startReading,
        minSeconds = if (talk) 10 else 0,
        maxSeconds = if (talk) 20 else readLimit(text.reading!!),
        ring = talk,
        onNeedPermission = onNeedPermission,
    ) {
        when {
            screen != Screen.FreeTalk -> Unit
            talk -> TextButton(onClick = vm::choosePassage) { Text(s.readPassageInstead) }
            else -> Row {
                TextButton(onClick = vm::choosePassage) { Text(s.anotherPassage) }
                TextButton(onClick = vm::talkInstead) { Text(s.talkInstead) }
            }
        }
    }
}

// Text to read aloud. With progress, unread words are dimmed and fade in as they are read.
// It stays one text node, so TalkBack reads the whole sentence.
@Composable
private fun ReadingText(text: String, progress: Int?, modifier: Modifier, style: TextStyle) {
    val tokens = remember(text) { text.split(' ') }
    // Words up to and including each token, so punctuation-only tokens follow their neighbour.
    val ends = remember(text) { tokens.runningFold(0) { n, t -> n + TextTools.words(t).size }.drop(1) }
    val color = MaterialTheme.colorScheme.onSurface
    val alphas = key(text) {
        tokens.indices.map { i ->
            val on = progress == null || ends[i] <= progress
            animateFloatAsState(if (on) 1f else 0.38f, tween(200), label = "word").value
        }
    }
    val shown = buildAnnotatedString {
        tokens.forEachIndexed { i, t ->
            if (i > 0) append(' ')
            withStyle(SpanStyle(color = color.copy(alpha = alphas[i]))) { append(t) }
        }
    }
    Text(shown, modifier, style = style, textAlign = TextAlign.Center)
}

@Composable
fun MoodScreen(vm: MainViewModel) = Page {
    var mood by remember { mutableIntStateOf(0) }
    var energy by remember { mutableIntStateOf(0) }
    TopBar("✕", { vm.confirmStop = true }) { StepDots(4) }
    Spacer(Modifier.weight(1f))
    RatingRow(LocalStrings.current.moodQuestion, LocalStrings.current.moodLabels, mood) { mood = it }
    Spacer(Modifier.height(Dimens.xxl))
    RatingRow(LocalStrings.current.energyQuestion, LocalStrings.current.energyLabels, energy) { energy = it }
    Spacer(Modifier.weight(1f))
    PrimaryButton(LocalStrings.current.seeResult, { vm.finish(mood, energy) }, enabled = mood > 0 && energy > 0)
    Spacer(Modifier.height(Dimens.l))

    StopDialog(vm)
}

// A 1-5 question: five circles, the picked label under them, and end labels.
@Composable
private fun RatingRow(question: String, labels: List<String>, picked: Int, onPick: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Text(question, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(Dimens.l))
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
                    onPick(i)
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
    Spacer(Modifier.height(Dimens.xs))
    Row(Modifier.widthIn(max = 308.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(labels.first(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(labels.last(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Lets the user fix what the recognizer heard before it is scored.
@Composable
fun ReviewScreen(vm: MainViewModel) = Page {
    var text by remember { mutableStateOf(vm.transcript) }
    TopBar("✕", { vm.confirmStop = true }) { StepDots(3) }
    Spacer(Modifier.height(Dimens.l))
    Text(LocalStrings.current.reviewTitle, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(Dimens.l))
    OutlinedTextField(
        text, { text = it }, Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 160.dp),
        textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
    )
    Spacer(Modifier.weight(1f))
    PrimaryButton(LocalStrings.current.looksRight, { vm.confirmTranscript(text) }, enabled = text.isNotBlank())
    Spacer(Modifier.height(Dimens.l))

    StopDialog(vm)
}

@Composable
fun AnalyzingScreen(vm: MainViewModel) = Page {
    Spacer(Modifier.weight(1f))
    VoiceOrb(vm.recorder.orb, OrbState.Analyzing, size = 240.dp)
    Text(LocalStrings.current.listeningBack, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.weight(1f))
}
