package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val AbdullahDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = NavyElevated,
    onPrimaryContainer = CyanGlow,
    secondary = VioletBright,
    onSecondary = Color.White,
    secondaryContainer = RoyalPurple.copy(alpha = 0.35f),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = GoldBright,
    onTertiary = NavyVoid,
    tertiaryContainer = GoldDeep.copy(alpha = 0.25f),
    onTertiaryContainer = GoldBright,
    background = NavyVoid,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = ElectricBlue.copy(alpha = 0.38f),
    outlineVariant = NavyElevated,
    error = CrimsonError,
    onError = Color.White
)

private val AbdullahLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = SkyCard,
    onPrimaryContainer = LightTextPrimary,
    secondary = RoyalPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DEFF),
    onSecondaryContainer = Color(0xFF2D0A4E),
    tertiary = GoldRich,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0C2),
    onTertiaryContainer = Color(0xFF5C3A00),
    background = SkyCanvasTop,
    onBackground = LightTextPrimary,
    surface = SkySurface,
    onSurface = LightTextPrimary,
    surfaceVariant = SkyCard,
    onSurfaceVariant = LightTextSecondary,
    outline = SkyBorder,
    outlineVariant = Color(0xFFBDDEFF),
    error = CrimsonError,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun AbdullahTheme(
    darkTheme: Boolean = false,
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AbdullahDarkColorScheme else AbdullahLightColorScheme
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
