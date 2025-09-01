package com.crk.timeforsalah.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.crk.timeforsalah.core.AsrJuristic
import com.crk.timeforsalah.core.CalculationMethod as CoreCalc
import com.crk.timeforsalah.core.CityCoordinates
import com.crk.timeforsalah.core.JamaatCalculator
import com.crk.timeforsalah.core.PrayerTimesCalculator
import com.crk.timeforsalah.data.AlarmPrefsStore
import com.crk.timeforsalah.data.AsrMethod as StoreAsr
import com.crk.timeforsalah.data.CalculationMethod as StoreCalc
import com.crk.timeforsalah.data.JamaatRulesStore
import com.crk.timeforsalah.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Keeps alarms “always on” by (a) rescheduling on system events
 * and (b) scheduling a daily midnight rescheduler.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val interested = action == null ||
                action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED ||
                action == Intent.ACTION_DATE_CHANGED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == ACTION_MIDNIGHT

        if (!interested) {
            Log.w(TAG, "Ignoring action: $action")
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Log.d(TAG, "Rescheduling alarms…")
                // Recompute & schedule for today (and tomorrow as buffer)
                rescheduleFor(context, LocalDate.now())
                rescheduleFor(context, LocalDate.now().plusDays(1))
                // Ensure the next midnight tick is set
                scheduleNextMidnight(context)
                Log.d(TAG, "Reschedule done.")
            } catch (t: Throwable) {
                Log.e(TAG, "Reschedule error", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "RescheduleReceiver"
        private const val ACTION_MIDNIGHT = "com.crk.timeforsalah.action.MIDNIGHT_ROLLOVER"

        /** Call once (e.g., from Application.onCreate) to bootstrap rolling schedule. */
        fun kickoff(context: Context) {
            val i = Intent(context, RescheduleReceiver::class.java)
            context.sendBroadcast(i)
        }

        /** Schedule an exact alarm for ~00:01 next day to roll the schedule forward. */
        private fun scheduleNextMidnight(context: Context) {
            val zone = ZoneId.systemDefault()
            val at = LocalDate.now(zone).plusDays(1).atTime(0, 1).atZone(zone).toInstant().toEpochMilli()

            val intent = Intent(context, RescheduleReceiver::class.java).apply {
                action = ACTION_MIDNIGHT // explicit target; no filter needed
            }
            val pi = PendingIntent.getBroadcast(
                context,
                ACTION_MIDNIGHT.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val am = context.getSystemService(AlarmManager::class.java)
            am?.cancel(pi)
            try {
                am?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to schedule next midnight alarm due to SecurityException. " +
                        "Ensure SCHEDULE_EXACT_ALARM permission is granted.", e)
            }
        }

        /** Compute and schedule date’s Start + Jamaat alarms using saved prefs. */
        private suspend fun rescheduleFor(context: Context, date: LocalDate) {
            val settings = SettingsStore.getInstance(context).settings.first()
            val rules    = JamaatRulesStore(context).rules.first()
            val alarm    = AlarmPrefsStore(context).prefs.first()

            val geo = CityCoordinates.resolve(settings.manualCity)

            val coreMethod: CoreCalc = when (settings.calcMethod) {
                StoreCalc.MWL       -> CoreCalc.MWL
                StoreCalc.UmmAlQura -> CoreCalc.UmmAlQura
                StoreCalc.ISNA      -> CoreCalc.ISNA
                StoreCalc.Egyptian  -> CoreCalc.Egyptian
                StoreCalc.Karachi   -> CoreCalc.Karachi
                StoreCalc.Tehran    -> CoreCalc.Tehran
            }
            val coreAsr = if (settings.asrMethod == StoreAsr.Hanafi)
                AsrJuristic.Hanafi else AsrJuristic.Shafii

            val pt = PrayerTimesCalculator.calculate(
                date = date,
                latitude = geo.lat,
                longitude = geo.lon,
                tz = geo.zone,
                method = coreMethod,
                asrJuristic = coreAsr
            )
            val j = JamaatCalculator.compute(date, pt, rules)

            val isFriday = date.dayOfWeek == DayOfWeek.FRIDAY

            val enableStart = mapOf(
                "Fajr" to (alarm.startEnabled["Fajr"] == true),
                "Dhuhr" to (alarm.startEnabled["Dhuhr"] == true),
                "Asr" to (alarm.startEnabled["Asr"] == true),
                "Maghrib" to (alarm.startEnabled["Maghrib"] == true),
                "Isha" to (alarm.startEnabled["Isha"] == true),
            )
            val enableJamaat = mapOf(
                "Fajr" to (alarm.jamaatEnabled["Fajr"] == true),
                "Dhuhr" to (if (isFriday) (alarm.jamaatEnabled["Jumuah"] == true) else (alarm.jamaatEnabled["Dhuhr"] == true)),
                "Asr" to (alarm.jamaatEnabled["Asr"] == true),
                "Maghrib" to (alarm.jamaatEnabled["Maghrib"] == true),
                "Isha" to (alarm.jamaatEnabled["Isha"] == true),
            )

            // minutes BEFORE start
            val preStart = mapOf(
                "Fajr" to (alarm.startPreMinutes["Fajr"] ?: 0),
                "Dhuhr" to (alarm.startPreMinutes["Dhuhr"] ?: 0),
                "Asr" to (alarm.startPreMinutes["Asr"] ?: 0),
                "Maghrib" to (alarm.startPreMinutes["Maghrib"] ?: 0),
                "Isha" to (alarm.startPreMinutes["Isha"] ?: 0),
            )

            // minutes BEFORE Jamaat (Jumuah on Fridays)
            val preJamaat = mapOf(
                "Fajr" to (alarm.jamaatPreMinutes["Fajr"] ?: 0),
                "Dhuhr" to (if (isFriday) (alarm.jamaatPreMinutes["Jumuah"] ?: 0) else (alarm.jamaatPreMinutes["Dhuhr"] ?: 0)),
                "Asr" to (alarm.jamaatPreMinutes["Asr"] ?: 0),
                "Maghrib" to (alarm.jamaatPreMinutes["Maghrib"] ?: 0),
                "Isha" to (alarm.jamaatPreMinutes["Isha"] ?: 0),
            )

            PrayerAlarmScheduler.scheduleForToday(
                context = context,
                times = pt,
                jamaat = j,
                enableStart = enableStart,
                enableJamaat = enableJamaat,
                startPreMinutes = preStart,
                jamaatPreMinutes = preJamaat
            )
        }
    }
}
