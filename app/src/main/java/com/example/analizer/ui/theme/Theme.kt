package com.example.analizer.ui.theme

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

private val FintechDarkColorScheme = darkColorScheme(
    primary = FintechAmber,
    onPrimary = FintechNavyDark,
    primaryContainer = FintechNavySurface,
    onPrimaryContainer = Color.White,
    secondary = FintechTerracotta,
    onSecondary = Color.White,
    secondaryContainer = FintechNavySurfaceVariant,
    onSecondaryContainer = FintechAmber,
    tertiary = FintechSage,
    onTertiary = Color.White,
    background = FintechNavyDark,
    onBackground = Color.White,
    surface = FintechNavySurface,
    onSurface = Color.White,
    surfaceVariant = FintechNavySurfaceVariant,
    onSurfaceVariant = FintechTextLightMuted,
    surfaceContainer = FintechNavyDark,
    surfaceContainerHigh = FintechNavySurface,
    outline = FintechNavyDivider,
    outlineVariant = Color(0xFF283652)
)

private val FintechLightColorScheme = lightColorScheme(
    primary = FintechNavyDark,
    onPrimary = Color.White,
    primaryContainer = FintechNavySurface,
    onPrimaryContainer = Color.White,
    secondary = FintechAmber,
    onSecondary = FintechNavyDark,
    secondaryContainer = FintechAmberLight,
    onSecondaryContainer = FintechNavyDark,
    tertiary = FintechTerracotta,
    onTertiary = Color.White,
    background = FintechCreamBg,
    onBackground = FintechTextDark,
    surface = FintechCardWhite,
    onSurface = FintechTextDark,
    surfaceVariant = Color(0xFFF1ECE1),
    onSurfaceVariant = FintechTextMuted,
    surfaceContainer = FintechNavyDark, // Deep navy for TopAppBar and NavigationBar
    surfaceContainerHigh = FintechCardWhite,
    outline = FintechCardBorder,
    outlineVariant = Color(0xFFE2D7C5)
)

@Composable
fun AnaliZerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional Fintech Management Dashboard theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FintechDarkColorScheme
        else -> FintechLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}