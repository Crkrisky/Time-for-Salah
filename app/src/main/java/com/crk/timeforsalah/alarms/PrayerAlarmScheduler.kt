package com.crk.timeforsalah.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.crk.timeforsalah.core.JamaatCalculator
import com.crk.timeforsalah.core.PrayerTimes
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerAlarmScheduler {

    /** Preferred API: pass per-prayer start/jamaat offsets (minutes BEFORE), plus isFriday. */
    fun scheduleForToday(
        context: Context,
        times: PrayerTimes,
        jamaat: JamaatCalculator.Result?,
        enableStart: Map<String, Boolean> = emptyMap(),
        enableJamaat: Map<String, Boolean> = emptyMap(),
        startPreMinutes: Map<String, Int> = emptyMap(),
        jamaatPreMinutes: Map<String, Int> = emptyMap(),
        isFriday: Boolean = false
    ) {
        // Start (at START - startPre)
        scheduleStart(context, "Fajr",    times.fajr,    enableStart, startPreMinutes)
        scheduleStart(context, "Dhuhr",   times.dhuhr,   enableStart, startPreMinutes)
        scheduleStart(context, "Asr",     times.asr,     enableStart, startPreMinutes)
        scheduleStart(context, "Maghrib", times.maghrib, enableStart, startPreMinutes)
        scheduleStart(context, "Isha",    times.isha,    enableStart, startPreMinutes)

        // Jamaat (at JAMAAT - jamaatPre)
        jamaat?.let { j ->
            scheduleJamaat(context, "Fajr",    j.fajr,    enableJamaat, jamaatPreMinutes)
            // IMPORTANT: Use Jumuah time on Fridays; Dhuhr time otherwise
            val dhuhrJamaatTime: LocalDateTime? = if (isFriday) j.jumuah else j.dhuhr
            scheduleJamaat(context, "Dhuhr",   dhuhrJamaatTime, enableJamaat, jamaatPreMinutes)
            scheduleJamaat(context, "Asr",     j.asr,     enableJamaat, jamaatPreMinutes)
            scheduleJamaat(context, "Maghrib", j.maghrib, enableJamaat, jamaatPreMinutes)
            scheduleJamaat(context, "Isha",    j.isha,    enableJamaat, jamaatPreMinutes)
        }
    }

    /** Backward-compat overload. */
    fun scheduleForToday(
        context: Context,
        times: PrayerTimes,
        jamaat: JamaatCalculator.Result?,
        enableStart: Map<String, Boolean>,
        enableJamaat: Map<String, Boolean>,
        minutesBeforeJamaat: Map<String, Int>,
        isFriday: Boolean = false
    ) {
        scheduleForToday(
            context = context,
            times = times,
            jamaat = jamaat,
            enableStart = enableStart,
            enableJamaat = enableJamaat,
            startPreMinutes = emptyMap(), // old API had no start offsets
            jamaatPreMinutes = minutesBeforeJamaat,
            isFriday = isFriday
        )
    }

    private fun scheduleStart(
        context: Context,
        prayer: String,
        time: LocalDateTime?,
        enableStart: Map<String, Boolean>,
        startPre: Map<String, Int>
    ) {
        if (time == null || enableStart[prayer] != true) return
        val pre = (startPre[prayer] ?: 0).coerceAtLeast(0)
        val at = time.minusMinutes(pre.toLong())
        scheduleExact(
            context = context,
            whenMillis = at.toEpochMilli(),
            title = "$prayer",
            body = if (pre > 0) "Starts in $pre min" else "It's time for $prayer",
            prayer = prayer,
            kind = "start"
        )
    }

    private fun scheduleJamaat(
        context: Context,
        prayer: String,
        time: LocalDateTime?,
        enableJamaat: Map<String, Boolean>,
        jamaatPre: Map<String, Int>
    ) {
        if (time == null || enableJamaat[prayer] != true) return
        val pre = (jamaatPre[prayer] ?: 0).coerceAtLeast(0)
        val at = time.minusMinutes(pre.toLong())
        scheduleExact(
            context = context,
            whenMillis = at.toEpochMilli(),
            title = "Jamaat • $prayer",
            body = if (pre > 0) "Jamaat in $pre min" else "It's time for $prayer Jamaat",
            prayer = prayer,
            kind = if (pre > 0) "pre" else "jamaat"
        )
    }

    private fun scheduleExact(
        context: Context,
        whenMillis: Long,
        title: String,
        body: String,
        prayer: String,
        kind: String
    ) {
        if (whenMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(AlarmManager::class.java)

        val requestCode = (prayer + kind + whenMillis).hashCode()
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_TITLE, title)
            putExtra(PrayerAlarmReceiver.EXTRA_BODY, body)
            putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer)
            putExtra(PrayerAlarmReceiver.EXTRA_ALARM_KIND, kind) // "start" | "pre" | "jamaat"
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, whenMillis, pi)
        }
    }

    private fun LocalDateTime.toEpochMilli(): Long =
        this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
