package com.crk.timeforsalah.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

/**
 * Prayer time calculator using standard solar geometry.
 * - Names kept stable; only formulas fixed.
 * - Two-pass refinement around local solar noon for better accuracy.
 *
 * Angles:
 *  - Fajr / Isha: depression angles below horizon (e.g., 18°).
 *  - Sunrise / Sunset: 0.833° (refraction + semidiameter).
 *  - Asr: shadow-length rule (Shafi‘i=1, Hanafi=2).
 */
enum class AsrJuristic { Shafii, Hanafi }

data class PrayerTimes(
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime,
)

/**
 * Common calculation methods.
 * If ishaIntervalMinutes is non-null, Isha is a fixed offset after Maghrib.
 */
enum class CalculationMethod(
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null
) {
    MWL(fajrAngle = 18.0, ishaAngle = 17.0),
    ISNA(fajrAngle = 15.0, ishaAngle = 15.0),
    Egyptian(fajrAngle = 19.5, ishaAngle = 17.5),
    Karachi(fajrAngle = 18.0, ishaAngle = 18.0),
    UmmAlQura(fajrAngle = 18.5, ishaAngle = 0.0, ishaIntervalMinutes = 90),
    Tehran(fajrAngle = 19.5, ishaAngle = 0.0, ishaIntervalMinutes = 90),
}

object PrayerTimesCalculator {

    /**
     * Main calculation entry point.
     * Names/params preserved; formulas corrected.
     */
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        tz: ZoneId,
        method: CalculationMethod,
        asrJuristic: AsrJuristic,
    ): PrayerTimes {

        // --- Pass 1: solar coordinates at 00:00 (UTC) of this civil date ---
        val jd0 = julianDay(date)
        var (decl0, eqt0Hours) = sunDeclAndEqtHours(jd0)

        // local time zone offset in hours for this date
        val tzOffsetHours =
            ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, tz).offset.totalSeconds / 3600.0

        // local apparent solar noon (hours, local clock)
        var noon = 12.0 + tzOffsetHours - (longitude / 15.0) - eqt0Hours

        // --- Pass 2: refine using coordinates at computed noon ---
        val jdAtNoon = jd0 + noon / 24.0
        val (decl, eqtHours) = sunDeclAndEqtHours(jdAtNoon)
        noon = 12.0 + tzOffsetHours - (longitude / 15.0) - eqtHours

        // Hour angles (hours from local solar noon)
        val sunriseHA = hourAngle(0.833, latitude, decl) // refraction + semidiameter
        val fajrHA = hourAngle(method.fajrAngle, latitude, decl)

        val asrFactor = if (asrJuristic == AsrJuristic.Hanafi) 2.0 else 1.0
        val asrHA = asrHourAngle(asrFactor, latitude, decl)

        // Times in decimal hours (local clock)
        val fajrTime    = noon - fajrHA+0.017
        val sunriseTime = noon - sunriseHA+0.017
        val dhuhrTime   = noon+0.017
        val asrTime     = noon + asrHA+0.017
        val maghribTime = noon + sunriseHA+0.017
        val ishaTime    =
            if (method.ishaIntervalMinutes == null) {
                val ishaHA = hourAngle(method.ishaAngle, latitude, decl)
                noon + ishaHA+0.017
            } else {
                maghribTime + method.ishaIntervalMinutes / 60.0+0.017
            }

        return PrayerTimes(
            fajr = toLocal(date, tz, fajrTime),
            sunrise = toLocal(date, tz, sunriseTime),
            dhuhr = toLocal(date, tz, dhuhrTime),
            asr = toLocal(date, tz, asrTime),
            maghrib = toLocal(date, tz, maghribTime),
            isha = toLocal(date, tz, ishaTime),
        )
    }

    // ---------------- Solar geometry (corrected formulas) ----------------

    /** Julian Day at 00:00 UTC for a civil date. */
    fun julianDay(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        val d = date.dayOfMonth

        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = floor(y / 100.0)
        val B = 2 - A + floor(A / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + B - 1524.5
    }

    /**
     * Sun declination (degrees) and Equation of Time (hours) for a given JD.
     * Uses RA from true longitude and obliquity.
     */
    private fun sunDeclAndEqtHours(jd: Double): Pair<Double, Double> {
        val D = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * D)            // mean anomaly (deg)
        val q = fixAngle(280.459 + 0.98564736 * D)            // mean longitude (deg)
        val L = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2.0 * g)) // true longitude (deg)
        val e = 23.439 - 0.00000036 * D                       // obliquity (deg)

        val decl = asinDeg(sinDeg(e) * sinDeg(L))             // declination (deg)

        // right ascension (deg) -> hours; Equation of Time in hours
        val raDeg = Math.toDegrees(atan2(cosDeg(e) * sinDeg(L), cosDeg(L)))
        val eqtHours = fixHour((q / 15.0) - (fixAngle(raDeg) / 15.0))

        return decl to eqtHours
    }

    /**
     * Generic hour angle for a given depression angle (positive value, e.g., 18).
     * Returns hours from local solar noon.
     */
    fun hourAngle(angle: Double, latitude: Double, decl: Double): Double {
        val alt = -angle // Sun below horizon
        val num = sinDeg(alt) - sinDeg(latitude) * sinDeg(decl)
        val den = cosDeg(latitude) * cosDeg(decl)
        val x = (num / den).coerceIn(-1.0, 1.0)
        return acosDeg(x) / 15.0
    }

    /**
     * Asr hour angle using the shadow-length rule.
     * factor = 1 (Shafi‘i) or 2 (Hanafi)
     */
    fun asrHourAngle(factor: Double, lat: Double, decl: Double): Double {
        // a_asr = -arccot( factor + tan(|φ − δ|) )
        val aAsr = Math.toDegrees(atan(1.0 / (factor + tan(Math.toRadians(abs(lat - decl))))))
        val num = sinDeg(aAsr) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val x = (num / den).coerceIn(-1.0, 1.0)
        return acosDeg(x) / 15.0
    }

    // ---------------- Small helpers (names preserved) ----------------

    fun fixAngle(a: Double): Double {
        var x = a % 360.0
        if (x < 0) x += 360.0
        return x
    }

    fun fixHour(h: Double): Double {
        var x = h % 24.0
        if (x < 0) x += 24.0
        return x
    }

    fun sinDeg(d: Double): Double = sin(Math.toRadians(d))
    fun cosDeg(d: Double): Double = cos(Math.toRadians(d))
    fun tanDeg(d: Double): Double = tan(Math.toRadians(d))
    fun asinDeg(x: Double): Double = Math.toDegrees(asin(x))
    fun acosDeg(x: Double): Double = Math.toDegrees(acos(x))

    /** Convert decimal hours on a civil date to LocalDateTime in a ZoneId. */
    fun toLocal(date: LocalDate, tz: ZoneId, hours: Double): LocalDateTime {
        val h = fixHour(hours)
        val hh = h.toInt()
        val mm = ((h - hh) * 60.0).toInt()
        val ss = (((h - hh) * 60.0 - mm) * 60.0).roundToInt().coerceAtMost(59)
        val dt = LocalDateTime.of(date, java.time.LocalTime.of(hh, mm, ss))
        return ZonedDateTime.of(dt, tz).toLocalDateTime()
    }
}
