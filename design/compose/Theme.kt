// Reference theme for Mindwave. Values come from design/tokens.md.
// Change the package to match the app.
package com.mindwave.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    background = Color(0xFFF6F5F2), onBackground = Color(0xFF16161A),
    surface = Color(0xFFF6F5F2), onSurface = Color(0xFF16161A),
    surfaceVariant = Color(0xFFECEBE7), onSurfaceVariant = Color(0xFF6A6A72),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFBFAF8),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF0EFEB),
    surfaceContainerHighest = Color(0xFFE8E7E2),
    primary = Color(0xFF3D4BE0), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E6FF), onPrimaryContainer = Color(0xFF121A6B),
    secondary = Color(0xFF5E5C70), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7E5F2), onSecondaryContainer = Color(0xFF1C1B29),
    tertiary = Color(0xFFD9653A), onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFBA3A26), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE3DE), onErrorContainer = Color(0xFF4A0D04),
    outline = Color(0xFFD3D2CC), outlineVariant = Color(0xFFE6E5E0),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF0D0D10), onBackground = Color(0xFFECECEF),
    surface = Color(0xFF0D0D10), onSurface = Color(0xFFECECEF),
    surfaceVariant = Color(0xFF1B1B20), onSurfaceVariant = Color(0xFF9A9AA3),
    surfaceContainerLowest = Color(0xFF0A0A0D), surfaceContainerLow = Color(0xFF121216),
    surfaceContainer = Color(0xFF16161B), surfaceContainerHigh = Color(0xFF1D1D23),
    surfaceContainerHighest = Color(0xFF25252C),
    primary = Color(0xFF9AA5FF), onPrimary = Color(0xFF10166B),
    primaryContainer = Color(0xFF262E86), onPrimaryContainer = Color(0xFFE0E3FF),
    secondary = Color(0xFFC4C2D6), onSecondary = Color(0xFF2E2D3D),
    secondaryContainer = Color(0xFF2E2D3D), onSecondaryContainer = Color(0xFFE7E5F2),
    tertiary = Color(0xFFFFB08F), onTertiary = Color(0xFF4A1A06),
    error = Color(0xFFFF8A75), onError = Color(0xFF4A0D04),
    errorContainer = Color(0xFF5C1A10), onErrorContainer = Color(0xFFFFDAD3),
    outline = Color(0xFF36363E), outlineVariant = Color(0xFF24242B),
    scrim = Color(0xFF000000),
)

/** Data colors that are not part of the M3 scheme. */
@Immutable
data class MindwaveColors(
    val valence: Color,
    val arousal: Color,
    val stability: Color,
    val metric: Color,
    val deviationTrack: Color,
    val deviationBand: Color,
)

private val LightExtra = MindwaveColors(
    valence = Color(0xFFE2683F), arousal = Color(0xFF4F5FF0), stability = Color(0xFF23977C),
    metric = Color(0xFF16161A), deviationTrack = Color(0xFFECEBE7), deviationBand = Color(0xFFDCDBD5),
)

private val DarkExtra = MindwaveColors(
    valence = Color(0xFFFF9C78), arousal = Color(0xFF8F9BFF), stability = Color(0xFF5FD3B4),
    metric = Color(0xFFECECEF), deviationTrack = Color(0xFF1F1F25), deviationBand = Color(0xFF2C2C34),
)

val LocalMindwaveColors = staticCompositionLocalOf { LightExtra }

private val Base = FontFamily.Default

val MindwaveTypography = Typography(
    displayMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

val MindwaveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object Dimens {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
    val orbSize = 240.dp
    val orbSizeSmall = 120.dp
    val orbCanvasPadding = 40.dp
    val countdownRingGap = 20.dp
    val countdownRingStroke = 2.dp
    val buttonHeight = 56.dp
}

object Motion {
    const val DUR_SHORT = 150
    const val DUR_MEDIUM = 300
    const val DUR_LONG = 500
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseExit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

@Composable
fun MindwaveTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMindwaveColors provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = MindwaveTypography,
            shapes = MindwaveShapes,
            content = content,
        )
    }
}
