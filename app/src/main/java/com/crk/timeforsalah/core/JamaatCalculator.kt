package com.crk.timeforsalah.core

import com.crk.timeforsalah.data.JamaatRule
import com.crk.timeforsalah.data.JamaatRules
import com.crk.timeforsalah.data.Prayer
import com.crk.timeforsalah.data.RuleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Computes Jamaat times for a given day using:
 *  - Start times from [PrayerTimes] (already computed)
 *  - Per-prayer rules (Offset OR Fixed)
 * Friday behavior:
 *  - Dhuhr START is still computed (from PrayerTimes)
 *  - "Jumu'ah" replaces Dhuhr for Jamaat time in UI/alarms
 */
object JamaatCalculator {

    data class Result(
        val fajr: LocalDateTime?,
        val dhuhr: LocalDateTime?,    // Dhuhr Jamaat (if Friday, prefer Jumu'ah)
        val asr: LocalDateTime?,
        val maghrib: LocalDateTime?,
        val isha: LocalDateTime?,
        val jumuah: LocalDateTime?    // Only meaningful for Friday UI
    )

    fun compute(
        date: LocalDate,
        startTimes: PrayerTimes,
        rules: JamaatRules
    ): Result {
        val isFriday = date.dayOfWeek == DayOfWeek.FRIDAY

        val fajrJ = applyRule(date, startTimes.fajr, rules[Prayer.Fajr])
        val dhuhrJ = applyRule(date, startTimes.dhuhr, rules[Prayer.Dhuhr])
        val asrJ = applyRule(date, startTimes.asr, rules[Prayer.Asr])
        val maghribJ = applyRule(date, startTimes.maghrib, rules[Prayer.Maghrib])
        val ishaJ = applyRule(date, startTimes.isha, rules[Prayer.Isha])
        val jumuahJ = if (isFriday) fixedToDate(date, rules[Prayer.Jumuah]) else null

        return Result(
            fajr = fajrJ,
            dhuhr = if (isFriday && jumuahJ != null) jumuahJ else dhuhrJ,
            asr = asrJ,
            maghrib = maghribJ,
            isha = ishaJ,
            jumuah = jumuahJ
        )
    }

    private fun applyRule(date: LocalDate, start: LocalDateTime, rule: JamaatRule): LocalDateTime? {
        return when (rule.type) {
            RuleType.Offset -> start.plusMinutes(rule.offsetMinutes.toLong())
            RuleType.Fixed  -> fixedToDate(date, rule)
        }
    }

    private fun fixedToDate(date: LocalDate, rule: JamaatRule): LocalDateTime {
        val hour24 = to24(rule.fixedHour12, rule.fixedIsPm)
        return LocalDateTime.of(date, LocalTime.of(hour24, rule.fixedMinute))
    }

    private fun to24(hour12: Int, isPm: Boolean): Int {
        val h = hour12 % 12
        return if (isPm) h + 12 else if (h == 12) 0 else h
    }
}
