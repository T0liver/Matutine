package hu.toliver.matutine.edition

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun chromeControlMinHeight(): Dp {
    if (editionForceSingleColumn()) return 48.dp
    return with(LocalDensity.current) { 40.toDp() }
}
