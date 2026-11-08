package com.stemstream.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

val Ink = Color(0xFF08090A)
val Surface = Color(0xFF121316)
val SurfaceRaised = Color(0xFF1A1C20)
val Line = Color(0x1FFFFFFF)
val Muted = Color(0xFFA1A1AA)
val Faint = Color(0x8AFFFFFF)

val Accent = Color(0xFFFF2D55)
val AccentWarm = Color(0xFFFF7A66)
val AccentAmber = Color(0xFFFF9A3C)

private val AppColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Accent,
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFFFFB3C0),
    secondary = AccentWarm,
    onSecondary = Color(0xFF1A0A0C),
    secondaryContainer = Color(0xFF3A1118),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF140A1F),
    tertiaryContainer = Color(0xFF2A1B45),
    onTertiaryContainer = Color(0xFFE9DDFF),
    background = Ink,
    onBackground = Color(0xFFF4F4F5),
    surface = Surface,
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    surfaceTint = Accent,
    inverseSurface = Color(0xFFF4F4F5),
    inverseOnSurface = Ink,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1A0000),
    errorContainer = Color(0xFF410002),
    onErrorContainer = Color(0xFFFFDAD6),
    border = Color(0xB3FFFFFF),
    borderVariant = Color(0x33FFFFFF),
    scrim = Color(0xE6000000),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.0).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun StemStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        content = content,
    )
}
