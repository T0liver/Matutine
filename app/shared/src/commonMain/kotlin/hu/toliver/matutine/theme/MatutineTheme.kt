package hu.toliver.matutine.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
val LocalMatutineEditionTypography = staticCompositionLocalOf<MatutineEditionTypography> {
    error("Matutine edition typography not provided")
}

@Composable
fun MatutineTheme(
    editionColors: MatutineEditionColors = MatutineEditionColors.newsprint(),
    content: @Composable () -> Unit,
) {
    val editionTypography = rememberMatutineEditionTypography(compactMasthead = true)
    val colorScheme = lightColorScheme(
        primary = editionColors.accent,
        onPrimary = editionColors.paper,
        secondary = editionColors.inkSecondary,
        onSecondary = editionColors.paper,
        background = editionColors.paper,
        onBackground = editionColors.ink,
        surface = editionColors.paper,
        onSurface = editionColors.ink,
        onSurfaceVariant = editionColors.inkSecondary,
        error = editionColors.error,
        onError = editionColors.paper,
    )
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalMatutineEditionTypography provides editionTypography,
            LocalMatutineEditionColors provides editionColors,
            LocalContentColor provides editionColors.ink,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                content()
            }
        }
    }
}
