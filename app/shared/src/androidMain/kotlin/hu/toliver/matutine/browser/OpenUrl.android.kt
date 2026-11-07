package hu.toliver.matutine.browser

import android.content.Intent
import android.net.Uri
import hu.toliver.matutine.MatutineActivityHolder

actual fun openUrl(url: String) {
    val context = MatutineActivityHolder.context ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
