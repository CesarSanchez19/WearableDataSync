package com.example.wearabledatasync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// NomadKit defines a single light palette, so the app always uses it
private val NomadKitColorScheme = lightColorScheme(
    primary = NomadSand,
    onPrimary = NomadInk,
    secondary = NomadOcean,
    onSecondary = NomadWhite,
    tertiary = NomadForest,
    onTertiary = NomadWhite,
    background = NomadBackground,
    onBackground = NomadInk,
    surface = NomadSurface,
    onSurface = NomadInk,
    error = NomadError,
    onError = NomadWhite
)

@Composable
fun WearableDataSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NomadKitColorScheme,
        typography = Typography,
        content = content
    )
}