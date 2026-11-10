package hu.toliver.matutine.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import hu.toliver.matutine.edition.DEFAULT_EDITION_THEME

data class MatutineEditionColors(
    val paper: Color,
    val ink: Color,
    val inkSecondary: Color,
    val rule: Color,
    val accent: Color,
    val error: Color,
) {
    companion object {
        fun newsprint(): MatutineEditionColors = MatutineEditionColors(
            paper = NewsprintColors.paper,
            ink = NewsprintColors.ink,
            inkSecondary = NewsprintColors.inkSecondary,
            rule = NewsprintColors.rule,
            accent = NewsprintColors.accent,
            error = NewsprintColors.error,
        )
    }
}

val LocalMatutineEditionColors = staticCompositionLocalOf { MatutineEditionColors.newsprint() }

object EditionThemeColors {
    val current: MatutineEditionColors
        @Composable get() = LocalMatutineEditionColors.current
}

fun resolveEditionTheme(themeId: String): MatutineEditionColors = when (themeId.trim().lowercase()) {
    DEFAULT_EDITION_THEME -> MatutineEditionColors.newsprint()
    else -> MatutineEditionColors.newsprint()
}
