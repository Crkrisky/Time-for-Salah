package com.crk.timeforsalah.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.crk.timeforsalah.R

object NotificationHelper {
    const val CHANNEL_ID_ADHAN = "adhan_channel" // legacy fallback

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = mgr.getNotificationChannel(CHANNEL_ID_ADHAN)
        if (existing != null) return
        val ch = NotificationChannel(
            CHANNEL_ID_ADHAN,
            "Salah Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for Salah start and Jamaat"
            setShowBadge(true)
        }
        mgr.createNotificationChannel(ch)
    }

    /**
     * Create (or return) a channel dedicated to the selected sound key.
     * On Android O+, sound must be configured on the Channel (not per-notification).
     */
    fun getOrCreateChannelForSound(context: Context, soundKey: String?): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Pre-O: per-notification sound would work, but we keep a single channel id.
            return CHANNEL_ID_ADHAN
        }
        val key = soundKey?.ifBlank { null } ?: "azaan_common"
        val channelId = "adhan_channel_$key"
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = mgr.getNotificationChannel(channelId)
        if (existing != null) return channelId

        val (resId, label) = when (key) {
            "adhan_full_1" -> R.raw.adhan_full_1 to "Adhan – Abdul Basit"
            "adhan_full_2" -> R.raw.adhan_full_2 to "Adhan – Mishary"
            "adhan_short"  -> R.raw.adhan_short  to "Adhan – Short"
            "adhan_short_2"-> R.raw.adhan_short_2 to "Adhan – Short 2"
            else           -> R.raw.azaan_common to "Adhan – Classic"
        }

        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val ch = NotificationChannel(
            channelId,
            "Salah Alerts ($label)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Prayer alarms using $label"
            setShowBadge(true)
            setSound(uri, attrs)
        }
        mgr.createNotificationChannel(ch)
        return channelId
    }
}
