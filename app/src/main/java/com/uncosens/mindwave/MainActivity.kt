package com.uncosens.mindwave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.uncosens.mindwave.ui.AnalyzingScreen
import com.uncosens.mindwave.ui.CalibrationIntroScreen
import com.uncosens.mindwave.ui.HistoryScreen
import com.uncosens.mindwave.ui.HomeScreen
import com.uncosens.mindwave.ui.LanguageScreen
import com.uncosens.mindwave.ui.LocalStrings
import com.uncosens.mindwave.ui.MessageScreen
import com.uncosens.mindwave.ui.MicDeniedScreen
import com.uncosens.mindwave.ui.MindwaveTheme
import com.uncosens.mindwave.ui.MoodScreen
import com.uncosens.mindwave.ui.Motion
import com.uncosens.mindwave.ui.ReadyScreen
import com.uncosens.mindwave.ui.ResultScreen
import com.uncosens.mindwave.ui.SettingsScreen
import com.uncosens.mindwave.ui.VoiceStepScreen
import com.uncosens.mindwave.ui.PermissionScreen
import com.uncosens.mindwave.ui.ReviewScreen
import com.uncosens.mindwave.ui.WelcomeScreen

private fun transitionKey(s: Screen): Any = when (s) {
    is Screen.Calibrate, Screen.ReadAnchor, Screen.ReadDaily, Screen.FreeTalk -> "voice"
    else -> s
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var afterGrant: (() -> Unit)? = null
    private var micDenied by mutableStateOf(false)
    private var micBlocked by mutableStateOf(false) // "don't ask again": only Settings can fix it
    private val askMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        micBlocked = !granted && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        if (granted) afterGrant?.invoke()
    }

    private fun hasMic() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // Runs the action now if the mic is allowed, otherwise asks first.
    private fun withMic(action: () -> Unit) {
        if (hasMic()) action() else { afterGrant = action; askMic.launch(Manifest.permission.RECORD_AUDIO) }
    }

    override fun onResume() {
        super.onResume()
        // The user may have allowed the mic in system settings.
        if (micDenied && hasMic()) { micDenied = false; afterGrant?.invoke() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark status and navigation bar icons, since the app is always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        setContent {
            CompositionLocalProvider(LocalStrings provides vm.strings) { MindwaveTheme {
                val s = vm.screen
                val inCheckIn = s == Screen.ReadAnchor || s == Screen.ReadDaily || s == Screen.FreeTalk || s == Screen.Review || s == Screen.Mood
                BackHandler(enabled = s != Screen.Home && s != Screen.Welcome && s != Screen.Language && s != Screen.Analyzing) {
                    if (inCheckIn) vm.confirmStop = true else vm.goHome()
                }
                if (micDenied) {
                    BackHandler { micDenied = false }
                    MicDeniedScreen(vm, micBlocked) {
                        if (micBlocked) startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                        else askMic.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    return@MindwaveTheme
                }
                // Shared-axis X between screens. Voice steps share one key, so moving between them
                // only changes the text and the orb stays where it is.
                val slide = with(LocalDensity.current) { 30.dp.roundToPx() }
                AnimatedContent(
                    s,
                    contentKey = ::transitionKey,
                    transitionSpec = {
                        val spec = tween<Float>(Motion.DUR_LONG, easing = Motion.EaseStandard)
                        val move = tween<IntOffset>(Motion.DUR_LONG, easing = Motion.EaseStandard)
                        (fadeIn(spec) + slideInHorizontally(move) { slide }) togetherWith (fadeOut(spec) + slideOutHorizontally(move) { -slide })
                    },
                    label = "screen",
                ) { shown ->
                    when (shown) {
                        Screen.Loading -> MessageScreen(vm.strings.preparing)
                        Screen.Language -> LanguageScreen(vm)
                        is Screen.Error -> MessageScreen(shown.text)
                        Screen.Welcome -> WelcomeScreen(vm, hasMic())
                        Screen.Permission -> PermissionScreen(vm, ::withMic)
                        Screen.CalibrationIntro -> CalibrationIntroScreen(vm)
                        is Screen.Calibrate, Screen.ReadAnchor, Screen.ReadDaily, Screen.FreeTalk -> VoiceStepScreen(vm, shown, ::withMic)
                        Screen.Ready -> ReadyScreen(vm)
                        Screen.Home -> HomeScreen(vm)
                        Screen.Review -> ReviewScreen(vm)
                        Screen.Mood -> MoodScreen(vm)
                        Screen.Analyzing -> AnalyzingScreen(vm)
                        is Screen.Result -> ResultScreen(vm, shown.checkIn)
                        Screen.History -> HistoryScreen(vm)
                        Screen.Settings -> SettingsScreen(vm, version)
                    }
                }
            } }
        }
    }
}
