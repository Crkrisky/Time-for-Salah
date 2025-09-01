package com.crk.timeforsalah.alarms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.crk.timeforsalah.R
import com.crk.timeforsalah.data.AlarmPrefsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PrayerAlarmReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val title   = intent.getStringExtra(EXTRA_TITLE) ?: "Salah Reminder"
        val body    = intent.getStringExtra(EXTRA_BODY)  ?: "It's time."
        val id      = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())
        val prayer  = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: ""     // "Fajr","Dhuhr",...
        val kind    = intent.getStringExtra(EXTRA_ALARM_KIND) ?: "start" // "start"|"pre"|"jamaat"

        // Read prefs synchronously (ok in receiver)
        val soundKey: String = runBlocking {
            val p = AlarmPrefsStore(context).prefs.first()
            p.perPrayerSoundKey[prayer] ?: p.soundKey // fallback to global
        }

        val builder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = ensureSoundChannel(context, soundKey)
                NotificationCompat.Builder(context, channelId)
            } else {
                NotificationCompat.Builder(context).apply {
                    soundUri(context, soundKey)?.let { setSound(it) }
                }
            }

        val notif = builder
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notif)
    }

    /** Create (or return) a channel bound to a specific raw sound. */
    private fun ensureSoundChannel(context: Context, soundKey: String): String {
        val id = "adhan_$soundKey"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return id

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(id)
        if (existing != null) return id

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val ch = NotificationChannel(
            id,
            "Salah Alerts • ${displayName(soundKey)}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for Salah using ${displayName(soundKey)}"
            val uri = soundUri(context, soundKey)
            if (uri != null) setSound(uri, attrs)
        }
        nm.createNotificationChannel(ch)
        return id
    }

    private fun soundUri(context: Context, key: String): Uri? {
        val resId = context.resources.getIdentifier(key, "raw", context.packageName)
        return if (resId != 0) Uri.parse("android.resource://${context.packageName}/$resId") else null
    }

    private fun displayName(key: String): String = when (key) {
        "adhan_traditional" -> "Adhan Traditional"
        "adhan_makkah"      -> "Adhan Makkah"
        "adhan_madinah"     -> "Adhan Madinah"
        "adhan_short"       -> "Adhan Short"
        else -> key
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY  = "extra_body"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        // identify prayer & kind
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"   // "Fajr","Dhuhr","Asr","Maghrib","Isha","Jumuah"
        const val EXTRA_ALARM_KIND  = "extra_alarm_kind"    // "start" | "pre" | "jamaat"
    }
}
