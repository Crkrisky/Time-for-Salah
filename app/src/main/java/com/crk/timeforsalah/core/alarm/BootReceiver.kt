package com.crk.timeforsalah.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.crk.timeforsalah.alarms.RescheduleReceiver

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
                // If your receiver is directBootAware in the manifest, this will also fire:
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("BootReceiver", "Device booted. Kicking off reschedule…")
                // Delegate to your central rescheduler (reads prefs + sets exact alarms)
                RescheduleReceiver.kickoff(context.applicationContext)
            }
            else -> {
                // Ignore other actions
            }
        }
    }
}
