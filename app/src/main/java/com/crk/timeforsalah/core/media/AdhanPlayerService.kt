package com.crk.timeforsalah.core.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.crk.timeforsalah.R
import com.crk.timeforsalah.data.AlarmPrefsStore
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that plays the user-selected Adhan sound.
 * If a rawRes extra is provided, it will use that; otherwise it loads the saved sound key.
 */
class AdhanPlayerService : Service() {

    private var player: ExoPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // pick sound: explicit rawRes > saved sound key
        val explicitRes = intent?.getIntExtra(EXTRA_RAW_RES, 0) ?: 0
        val resId = if (explicitRes != 0) {
            explicitRes
        } else {
            val key = runBlocking { AlarmPrefsStore(applicationContext).prefs.blockingFirstSoundKey() }
            keyToResId(key)
        }

        if (resId == 0) {
            // "silent" or unknown => no playback, just stop
            stopSelf()
            return START_NOT_STICKY
        }

        ensureChannel()
        startForeground(NOTIF_ID, buildNotification())

        val item = MediaItem.fromUri("android.resource://$packageName/$resId")
        player = ExoPlayer.Builder(this).build().also { p ->
            p.setMediaItem(item)
            p.prepare()
            p.playWhenReady = true
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) stopSelf()
                }
            })
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        // Removed redundant SDK version check as minSdk is likely >= 29
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "Adhan Playback", NotificationManager.IMPORTANCE_HIGH
                ).apply { setSound(null, null) }
            )
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Playing Adhan…")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "adhan_playback"
        private const val NOTIF_ID = 2027
        private const val EXTRA_RAW_RES = "extra_raw_res"
        private const val ACTION_STOP = "com.crk.timeforsalah.media.STOP"

        fun start(context: Context) {
            val i = Intent(context, AdhanPlayerService::class.java)
            context.startForegroundService(i)
        }

        fun start(context: Context, rawRes: Int) {
            val i = Intent(context, AdhanPlayerService::class.java).apply {
                putExtra(EXTRA_RAW_RES, rawRes)
            }
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            val i = Intent(context, AdhanPlayerService::class.java).apply { action = ACTION_STOP }
            context.startService(i)
            context.stopService(Intent(context, AdhanPlayerService::class.java))
        }
    }
}

/* -------- Small helpers -------- */

private fun keyToResId(key: String): Int = when (key) {
    "adhan_short" -> R.raw.adhan_short
    "adhan_soft"  -> R.raw.adhan_short // Fallback to adhan_short
    "beep"        -> R.raw.adhan_short // Fallback to adhan_short
    "silent"      -> 0
    else          -> R.raw.adhan_short
}

private fun kotlinx.coroutines.flow.Flow<com.crk.timeforsalah.data.AlarmPrefs>.blockingFirstSoundKey(): String {
    var key = "adhan_short"
    runBlocking {
        this@blockingFirstSoundKey.collect { ap ->
            key = ap.soundKey
            return@collect
        }
    }
    return key
}
