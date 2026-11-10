package hu.toliver.matutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Composable
fun rememberMatutineTrayIcon(): Painter = remember {
    BitmapPainter(buildTrayImageBitmap())
}

private fun buildTrayImageBitmap(): ImageBitmap {
    val size = 22
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.color = java.awt.Color(26, 26, 26)
    graphics.fillRoundRect(1, 1, size - 2, size - 2, 4, 4)
    graphics.color = java.awt.Color(245, 241, 232)
    graphics.fillRect(5, 6, 12, 2)
    graphics.fillRect(5, 10, 10, 1)
    graphics.fillRect(5, 13, 11, 1)
    graphics.dispose()
    return image.toComposeImageBitmap()
}
