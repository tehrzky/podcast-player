package com.example.podcast.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.podcast.data.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF141218),
    surface = Color(0xFF141218)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)

private val OledColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = OledBlack,
    surface = OledBlack,
    surfaceVariant = OledSurface,
    surfaceContainer = OledCard
)

@Composable
fun PodcastTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accentSeedColor: Long = 0xFF6650a4,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.OLED_BLACK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        themeMode == AppThemeMode.OLED_BLACK -> OledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme.copy(primary = Color(accentSeedColor))
        else -> LightColorScheme.copy(primary = Color(accentSeedColor))
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        content = content
    )
}
