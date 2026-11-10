package hu.toliver.matutine.settings

object NotificationScheduleCallbacks {
    var reschedule: (() -> Unit)? = null
}
