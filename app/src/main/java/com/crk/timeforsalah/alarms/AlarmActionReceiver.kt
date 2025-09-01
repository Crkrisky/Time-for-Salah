package com.crk.timeforsalah.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.crk.timeforsalah.core.media.AdhanPlayerService
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Handles notification actions from prayer alarms:
 *  - Snooze: schedule a one-off alarm X minutes later
 *  - I'm on my way: quick gentle follow-up in 2 minutes
 *  - Dismiss: stop audio & clear notification
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Always stop any playing Adhan audio
        AdhanPlayerService.stop(context)

        // Clear the current notification if we have an id
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notifId != 0) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }

        when (action) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MIN, 5).coerceIn(1, 30)
                val at = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).plusMinutes(minutes.toLong())
                scheduleOneOff(
                    context = context,
                    at = at,
                    title = intent.getStringExtra(PrayerAlarmReceiver.EXTRA_TITLE) ?: "Salah Reminder",
                    body = "Snoozed reminder"
                )
            }

            ACTION_ON_MY_WAY -> {
                val at = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).plusMinutes(2)
                scheduleOneOff(
                    context = context,
                    at = at,
                    title = intent.getStringExtra(PrayerAlarmReceiver.EXTRA_TITLE) ?: "Salah Reminder",
                    body = "Quick reminder while you’re on the way"
                )
            }

            ACTION_DISMISS -> {
                // Nothing else to do; already stopped audio & cleared notification
            }
        }
    }

    private fun scheduleOneOff(
        context: Context,
        at: ZonedDateTime,
        title: String,
        body: String
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                // Optionally, direct the user to settings
                // Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                //     data = Uri.parse("package:${context.packageName}")
                //     context.startActivity(this)
                // }
                return // Cannot schedule exact alarms
            }
        }

        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_TITLE, title)
            putExtra(PrayerAlarmReceiver.EXTRA_BODY, body)
            putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, at.toEpochSecond().toInt())
        }
        val pi = PendingIntent.getBroadcast(
            context,
            at.toEpochSecond().toInt(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pi)
    }

    companion object {
        const val ACTION_SNOOZE = "com.crk.timeforsalah.action.SNOOZE"
        const val ACTION_ON_MY_WAY = "com.crk.timeforsalah.action.ON_MY_WAY"
        const val ACTION_DISMISS = "com.crk.timeforsalah.action.DISMISS"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SNOOZE_MIN = "extra_snooze_min"
    }
}
