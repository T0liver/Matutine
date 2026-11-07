package hu.toliver.matutine

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Default app entry used by previews and web targets without a platform data root.
 */
@Composable
@Preview
fun App() {
    MatutineApp(dataRoot = ".")
}
