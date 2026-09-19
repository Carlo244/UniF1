package com.example.unif1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPeach,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A342B),
    onPrimaryContainer = DarkPeach,
    secondary = DarkLavender,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF332A44),
    onSecondaryContainer = DarkLavender,
    tertiary = DarkBlue,
    onTertiary = Color.Black,
    background = DeepCharcoal,
    onBackground = DarkOnSurface,
    surface = DarkGreySurface,
    onSurface = DarkOnSurface,
    surfaceVariant = ElevatedDarkSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF666666),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = PeachAccent,
    onPrimary = PrimaryDarkText,
    primaryContainer = Color(0xFFFDE7E0),
    onPrimaryContainer = PrimaryDarkText,
    secondary = LavenderAccent,
    onSecondary = PrimaryDarkText,
    secondaryContainer = Color(0xFFF2EAF9),
    onSecondaryContainer = PrimaryDarkText,
    tertiary = PowderBlueAccent,
    onTertiary = PrimaryDarkText,
    background = BackgroundLight,
    onBackground = PrimaryDarkText,
    surface = Color.White,
    onSurface = PrimaryDarkText,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = MutedBrownText,
    outline = Color(0xFFCCCCCC),
    error = Color(0xFFB00020)
)

/**
 * Custom semantic colors that don't fit directly into Material 3 ColorScheme roles
 */
data class UniF1Colors(
    val run: Color,
    val bike: Color,
    val swim: Color,
    val ready: Color,
    val caution: Color,
    val recovery: Color,
    val glass: Color
)

val LocalUniF1Colors = compositionLocalOf {
    UniF1Colors(
        run = Color.Unspecified,
        bike = Color.Unspecified,
        swim = Color.Unspecified,
        ready = Color.Unspecified,
        caution = Color.Unspecified,
        recovery = Color.Unspecified,
        glass = Color.Unspecified
    )
}

val MaterialTheme.unif1Colors: UniF1Colors
    @Composable
    @ReadOnlyComposable
    get() = LocalUniF1Colors.current

@Composable
fun UniF1Theme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val unif1Colors = if (darkTheme) {
        UniF1Colors(
            run = DarkKitRun,
            bike = DarkKitBike,
            swim = DarkKitSwim,
            ready = DarkReadySage,
            caution = DarkCautionAmber,
            recovery = DarkRecoveryClay,
            glass = GlassDark
        )
    } else {
        UniF1Colors(
            run = KitRun,
            bike = KitBike,
            swim = KitSwim,
            ready = ReadySage,
            caution = CautionAmber,
            recovery = RecoveryClay,
            glass = GlassWhite
        )
    }

    CompositionLocalProvider(LocalUniF1Colors provides unif1Colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
