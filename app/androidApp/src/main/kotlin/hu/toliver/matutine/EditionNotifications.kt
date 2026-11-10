package hu.toliver.matutine

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hu.toliver.matutine.edition.EDITION_NOTIFICATION_TITLE
import hu.toliver.matutine.edition.EDITION_READY_NOTIFICATION_BODY

private const val CHANNEL_ID = "matutine-edition-ready"
private const val NOTIFICATION_ID = 1

fun notificationsMayPost(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

fun postEditionReadyNotification(
    context: Context,
    body: String = EDITION_READY_NOTIFICATION_BODY,
) {
    ensureEditionChannel(context)
    val open = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notificationText = body.trim().ifEmpty { EDITION_READY_NOTIFICATION_BODY }
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(EDITION_NOTIFICATION_TITLE)
        .setContentText(notificationText)
        .setContentIntent(open)
        .setAutoCancel(true)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
        .build()
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
}

private fun ensureEditionChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            CHANNEL_ID,
            EDITION_NOTIFICATION_TITLE,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = EDITION_READY_NOTIFICATION_BODY
        },
    )
}
