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
    content: @Composable () -> Unit,
) {
    val editionTypography = rememberMatutineEditionTypography(compactMasthead = true)
    val colorScheme = lightColorScheme(
        primary = NewsprintColors.accent,
        onPrimary = NewsprintColors.paper,
        secondary = NewsprintColors.inkSecondary,
        onSecondary = NewsprintColors.paper,
        background = NewsprintColors.paper,
        onBackground = NewsprintColors.ink,
        surface = NewsprintColors.paper,
        onSurface = NewsprintColors.ink,
        onSurfaceVariant = NewsprintColors.inkSecondary,
        error = NewsprintColors.error,
        onError = NewsprintColors.paper,
    )
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalMatutineEditionTypography provides editionTypography,
            LocalContentColor provides NewsprintColors.ink,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                content()
            }
        }
    }
}
