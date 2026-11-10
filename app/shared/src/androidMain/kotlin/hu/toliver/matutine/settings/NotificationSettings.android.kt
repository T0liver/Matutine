package hu.toliver.matutine.settings

actual fun showsNotificationTimeRow(): Boolean = true

actual fun onNotificationTimeSaved() {
    NotificationScheduleCallbacks.reschedule?.invoke()
}
