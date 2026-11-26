package com.example.vela.ui.theme

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

private val VelaDarkColorScheme = darkColorScheme(
    primary = VelaVioletSoft,
    onPrimary = Color(0xFF120B2E),
    primaryContainer = VelaVioletContainer,
    onPrimaryContainer = Color(0xFFE9E2FF),
    secondary = VelaMint,
    onSecondary = Color(0xFF052417),
    secondaryContainer = Color(0xFF103B2A),
    onSecondaryContainer = Color(0xFFB5FCE0),
    tertiary = VelaCoral,
    onTertiary = Color(0xFF2B0E07),
    background = ObsidianBg,
    onBackground = ObsidianText,
    surface = ObsidianSurface,
    onSurface = ObsidianText,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = ObsidianTextMuted,
    outline = ObsidianOutline,
    error = Color(0xFFFF6B7A)
)

private val VelaLightColorScheme = lightColorScheme(
    primary = VelaViolet,
    onPrimary = Color.White,
    primaryContainer = LightVioletContainer,
    onPrimaryContainer = Color(0xFF201059),
    secondary = Color(0xFF0FA96E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F8E7),
    onSecondaryContainer = Color(0xFF052B1B),
    tertiary = VelaCoral,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextMuted,
    outline = LightOutline,
    error = Color(0xFFD93848)
)

@Composable
fun VelaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VelaDarkColorScheme
        else -> VelaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
