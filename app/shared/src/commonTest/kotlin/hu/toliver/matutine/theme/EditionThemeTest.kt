package hu.toliver.matutine.theme

import hu.toliver.matutine.edition.DEFAULT_EDITION_THEME
import kotlin.test.Test
import kotlin.test.assertEquals

class EditionThemeTest {
    @Test
    fun resolveEditionTheme_usesNewsprintForDefaultAndUnknown() {
        val newsprint = resolveEditionTheme(DEFAULT_EDITION_THEME)
        assertEquals(MatutineEditionColors.newsprint().paper, newsprint.paper)
        assertEquals(MatutineEditionColors.newsprint().ink, newsprint.ink)
        assertEquals(resolveEditionTheme("future-name").paper, newsprint.paper)
    }
}
