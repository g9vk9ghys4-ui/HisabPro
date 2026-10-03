package com.example.ui.theme

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
    primary = HisabBlueLight,
    onPrimary = Color.White,
    primaryContainer = HisabBlue,
    onPrimaryContainer = HisabBlueContainer,
    secondary = HisabGreenLight,
    onSecondary = Color.White,
    secondaryContainer = HisabGreenOnContainer,
    onSecondaryContainer = HisabGreenContainer,
    error = HisabRedLight,
    onError = Color.White,
    background = NeutralDark,
    onBackground = Color(0xFFF1F5F9),
    surface = NeutralCardDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = NeutralBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = HisabBlue,
    onPrimary = Color.White,
    primaryContainer = HisabBlueContainer,
    onPrimaryContainer = HisabBlueDark,
    secondary = HisabGreen,
    onSecondary = Color.White,
    secondaryContainer = HisabGreenContainer,
    onSecondaryContainer = HisabGreenOnContainer,
    error = HisabRed,
    onError = Color.White,
    background = NeutralLight,
    onBackground = Color(0xFF0F172A),
    surface = NeutralCardLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = NeutralBorderLight
)

@Composable
fun HisabProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded theme by default for recognizable brand identity
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
