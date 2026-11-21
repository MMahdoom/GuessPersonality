package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = OnSaffronPrimary,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = PeacockSecondary,
    onSecondary = OnPeacockSecondary,
    secondaryContainer = PeacockContainer,
    onSecondaryContainer = OnPeacockContainer,
    tertiary = MarigoldTertiary,
    onTertiary = OnMarigoldTertiary,
    tertiaryContainer = MarigoldContainer,
    onTertiaryContainer = OnMarigoldContainer,
    background = WarmIvoryBackground,
    onBackground = DeepAubergineInk,
    surface = WarmIvorySurface,
    onSurface = DeepAubergineInk,
    surfaceVariant = WarmSurfaceContainer,
    onSurfaceVariant = MutedPlumInk,
    surfaceContainerLowest = WarmSurfaceElevated,
    surfaceContainerLow = WarmIvoryBackground,
    surfaceContainer = WarmSurfaceContainer,
    outline = WarmOutline,
    error = VermilionError
)

private val DarkColorScheme = darkColorScheme(
    primary = SpotlightGold,
    onPrimary = StageDeepIndigo,
    primaryContainer = StageElevatedCard,
    onPrimaryContainer = SpotlightGoldLight,
    secondary = StageTeal,
    onSecondary = StageDeepIndigo,
    secondaryContainer = ColorTokens.DarkTealContainer,
    onSecondaryContainer = StageTeal,
    tertiary = StageCrimson,
    onTertiary = StageIvoryText,
    background = StageDeepIndigo,
    onBackground = StageIvoryText,
    surface = StageVelvetSurface,
    onSurface = StageIvoryText,
    surfaceVariant = StageElevatedCard,
    onSurfaceVariant = StageMutedText,
    surfaceContainerLowest = StageDeepIndigo,
    surfaceContainerLow = StageVelvetSurface,
    surfaceContainer = StageElevatedCard,
    outline = StageOutline,
    error = StageCrimson
)

private object ColorTokens {
    val DarkTealContainer = androidx.compose.ui.graphics.Color(0xFF134E4A)
}

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GuessPersonalityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
