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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.uncosens.mindwave.ui.AnalyzingScreen
import com.uncosens.mindwave.ui.AnchorScreen
import com.uncosens.mindwave.ui.CalibrateScreen
import com.uncosens.mindwave.ui.CalibrationIntroScreen
import com.uncosens.mindwave.ui.DailyScreen
import com.uncosens.mindwave.ui.FreeTalkScreen
import com.uncosens.mindwave.ui.HistoryScreen
import com.uncosens.mindwave.ui.HomeScreen
import com.uncosens.mindwave.ui.MessageScreen
import com.uncosens.mindwave.ui.MicDeniedScreen
import com.uncosens.mindwave.ui.MindwaveTheme
import com.uncosens.mindwave.ui.MoodScreen
import com.uncosens.mindwave.ui.ReadyScreen
import com.uncosens.mindwave.ui.ResultScreen
import com.uncosens.mindwave.ui.SettingsScreen
import com.uncosens.mindwave.ui.WelcomeScreen

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
        enableEdgeToEdge()
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        setContent {
            MindwaveTheme {
                val s = vm.screen
                val inCheckIn = s == Screen.ReadAnchor || s == Screen.ReadDaily || s == Screen.FreeTalk || s == Screen.Mood
                BackHandler(enabled = s != Screen.Home && s != Screen.Welcome && s != Screen.Analyzing) {
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
                when (s) {
                    Screen.Loading -> MessageScreen("Preparing…")
                    is Screen.Error -> MessageScreen(s.text)
                    Screen.Welcome -> WelcomeScreen(vm, ::withMic)
                    Screen.CalibrationIntro -> CalibrationIntroScreen(vm)
                    is Screen.Calibrate -> CalibrateScreen(vm, s.round, ::withMic)
                    Screen.Ready -> ReadyScreen(vm)
                    Screen.Home -> HomeScreen(vm)
                    Screen.ReadAnchor -> AnchorScreen(vm, ::withMic)
                    Screen.ReadDaily -> DailyScreen(vm, ::withMic)
                    Screen.FreeTalk -> FreeTalkScreen(vm, ::withMic)
                    Screen.Mood -> MoodScreen(vm)
                    Screen.Analyzing -> AnalyzingScreen(vm)
                    is Screen.Result -> ResultScreen(vm, s.checkIn)
                    Screen.History -> HistoryScreen(vm)
                    Screen.Settings -> SettingsScreen(vm, version)
                }
            }
        }
    }
}
