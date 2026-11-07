package hu.toliver.matutine.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import matutine.app.shared.generated.resources.Res
import matutine.app.shared.generated.resources.ibm_plex_mono_regular
import matutine.app.shared.generated.resources.newsreader_medium
import matutine.app.shared.generated.resources.source_serif4_regular
import org.jetbrains.compose.resources.Font

@Immutable
data class MatutineEditionTypography(
    val masthead: TextStyle,
    val mastheadCompact: TextStyle,
    val lead: TextStyle,
    val leadCompact: TextStyle,
    val story: TextStyle,
    val note: TextStyle,
    val briefing: TextStyle,
    val body: TextStyle,
    val meta: TextStyle,
    val kicker: TextStyle,
    val typewriter: TextStyle,
)

@Composable
fun rememberMatutineEditionTypography(compactMasthead: Boolean = true): MatutineEditionTypography {
    val newsreader = FontFamily(Font(Res.font.newsreader_medium, weight = FontWeight.Medium))
    val sourceSerif = FontFamily(
        Font(Res.font.source_serif4_regular, weight = FontWeight.Normal),
        Font(Res.font.source_serif4_regular, weight = FontWeight.SemiBold),
    )
    val plexMono = FontFamily(Font(Res.font.ibm_plex_mono_regular))

    return remember(compactMasthead, newsreader, sourceSerif, plexMono) {
        val mastheadSize = if (compactMasthead) 32.sp else 40.sp
        val leadSize = if (compactMasthead) 28.sp else 34.sp
        MatutineEditionTypography(
            masthead = TextStyle(
                fontFamily = newsreader,
                fontWeight = FontWeight.Medium,
                fontSize = mastheadSize,
                lineHeight = mastheadSize * 1.05f,
                letterSpacing = (-0.02).em,
                color = NewsprintColors.ink,
            ),
            mastheadCompact = TextStyle(
                fontFamily = newsreader,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
                lineHeight = 33.6.sp,
                letterSpacing = (-0.02).em,
                color = NewsprintColors.ink,
            ),
            lead = TextStyle(
                fontFamily = newsreader,
                fontWeight = FontWeight.Medium,
                fontSize = leadSize,
                lineHeight = leadSize * 1.15f,
                letterSpacing = (-0.015).em,
                color = NewsprintColors.ink,
            ),
            leadCompact = TextStyle(
                fontFamily = newsreader,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 32.2.sp,
                color = NewsprintColors.ink,
            ),
            story = TextStyle(
                fontFamily = newsreader,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 27.5.sp,
                color = NewsprintColors.ink,
            ),
            note = TextStyle(
                fontFamily = newsreader,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 23.4.sp,
                color = NewsprintColors.ink,
            ),
            briefing = TextStyle(
                fontFamily = sourceSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 26.1.sp,
                color = NewsprintColors.ink,
            ),
            body = TextStyle(
                fontFamily = sourceSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 23.2.sp,
                color = NewsprintColors.ink,
            ),
            meta = TextStyle(
                fontFamily = sourceSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 17.55.sp,
                color = NewsprintColors.inkSecondary,
            ),
            kicker = TextStyle(
                fontFamily = sourceSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 15.6.sp,
                letterSpacing = 0.08.em,
                color = NewsprintColors.inkSecondary,
            ),
            typewriter = TextStyle(
                fontFamily = plexMono,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.8.sp,
                color = NewsprintColors.ink,
            ),
        )
    }
}
