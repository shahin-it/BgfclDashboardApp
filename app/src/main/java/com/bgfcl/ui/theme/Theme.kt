package com.bgfcl.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = BgfclCyanLight,
    onPrimary = BgfclNavyDark,
    primaryContainer = BgfclNavyPrimary,
    onPrimaryContainer = Color.White,
    secondary = BgfclCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = BgfclNavyDark,
    onSecondaryContainer = BgfclCyanLight,
    tertiary = BgfclAmberFlame,
    onTertiary = Color.Black,
    background = NeutralBackgroundDark,
    onBackground = NeutralTextPrimaryDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralTextPrimaryDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = NeutralTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = BgfclNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = BgfclNavyDark,
    secondary = BgfclCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F9FF),
    onSecondaryContainer = BgfclNavyDark,
    tertiary = BgfclAmberFlame,
    onTertiary = Color.White,
    background = NeutralBackgroundLight,
    onBackground = NeutralTextPrimaryLight,
    surface = Color.White,
    onSurface = NeutralTextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = NeutralTextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use customized BGFCL brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
