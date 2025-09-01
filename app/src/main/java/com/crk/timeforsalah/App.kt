package com.crk.timeforsalah

import android.app.Application
import com.crk.timeforsalah.alarms.NotificationHelper
import com.crk.timeforsalah.alarms.RescheduleReceiver

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure channel exists, and bootstrap rolling schedule
        NotificationHelper.ensureChannel(this)
        RescheduleReceiver.kickoff(this)
    }
}
