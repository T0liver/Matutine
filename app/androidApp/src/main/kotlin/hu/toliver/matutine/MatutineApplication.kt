package hu.toliver.matutine

import android.app.Application

class MatutineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MatutineActivityHolder.context = applicationContext
        scheduleMorningRun(this)
    }
}
