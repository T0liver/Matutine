package hu.toliver.matutine

import android.app.Application
import hu.toliver.matutine.settings.NotificationScheduleCallbacks

class MatutineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MatutineActivityHolder.context = applicationContext
        scheduleMorningRun(this)
        scheduleEditionNotification(this)
        NotificationScheduleCallbacks.reschedule = { scheduleEditionNotification(this) }
    }
}
