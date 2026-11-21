package com.ibrah.parkasistani.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Park tabelası mavisi tek vurgu; geri kalan her şey sakin nötrler. */
object ParkColors {
    val Blue = Color(0xFF1F5EFF)
    val BlueDeep = Color(0xFF1546C7)
    val BlueSoft = Color(0xFFE6EDFF)
    val Green = Color(0xFF16A34A)
    val GreenSoft = Color(0xFFE3F6EA)
    val Amber = Color(0xFFD97706)
    val AmberSoft = Color(0xFFFEF3E2)
}

private val Light = lightColorScheme(
    primary = ParkColors.Blue,
    onPrimary = Color.White,
    primaryContainer = ParkColors.BlueSoft,
    onPrimaryContainer = ParkColors.BlueDeep,
    secondary = Color(0xFF475569),
    secondaryContainer = Color(0xFFEEF1F6),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = ParkColors.Green,
    tertiaryContainer = ParkColors.GreenSoft,
    onTertiaryContainer = Color(0xFF14532D),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF5F7FB),
    onSurface = Color(0xFF0F172A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F3F8),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    errorContainer = ParkColors.AmberSoft,
    onErrorContainer = Color(0xFF7C2D12),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FA2FF),
    onPrimary = Color(0xFF0A1F5C),
    primaryContainer = Color(0xFF1C2E66),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF1E2633),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF4ADE80),
    tertiaryContainer = Color(0xFF123222),
    onTertiaryContainer = Color(0xFFBBF7D0),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFE5E9F0),
    surface = Color(0xFF0B0F17),
    onSurface = Color(0xFFE5E9F0),
    surfaceContainerLowest = Color(0xFF131A26),
    surfaceContainerLow = Color(0xFF131A26),
    surfaceContainer = Color(0xFF131A26),
    surfaceContainerHigh = Color(0xFF1A2231),
    surfaceVariant = Color(0xFF1A2231),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    errorContainer = Color(0xFF3B2410),
    onErrorContainer = Color(0xFFFED7AA),
)

private val ParkShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val ParkType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun ParkTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = ParkShapes,
        typography = ParkType,
        content = content,
    )
}
