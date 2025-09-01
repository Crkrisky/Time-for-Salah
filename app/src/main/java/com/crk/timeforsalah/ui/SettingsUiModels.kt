package com.crk.timeforsalah.ui

/* Core enums (UI) */
enum class LocationMode { MANUAL, GPS }
enum class CalculationMethod { MWL, UmmAlQura, ISNA, Egyptian, Karachi, Tehran }
enum class AsrMethod { Hanafi, Shafii }
enum class AppLanguage { EN, UR, AR }

/* Prayers (UI) */
enum class Prayer(val displayName: String) {
    Fajr("Fajr"),
    Sunrise("Sunrise"),
    Dhuhr("Dhuhr"),
    Asr("Asr"),
    Maghrib("Maghrib"),
    Isha("Isha"),
    Jumuah("Jumu’ah")
}

/* Jamaat rules (UI) */
enum class RuleType { Offset, Fixed }

data class JamaatRule(
    val prayer: Prayer,
    val type: RuleType,
    val offsetMinutes: Int = 0,
    val fixedHour12: Int = 12,
    val fixedMinute: Int = 0,
    val fixedIsPm: Boolean = true
)

/* Alarm toggles (UI) */
data class AlarmToggle(
    val prayer: Prayer,
    val startEnabled: Boolean = true,
    val jamaatEnabled: Boolean = true,
    val preMinutes: Int = 0
)

/* Global Settings UI State */
data class SettingsUiState(
    val locationMode: LocationMode = LocationMode.MANUAL,
    val manualCity: String = "",
    val calcMethod: CalculationMethod = CalculationMethod.MWL,
    val asrMethod: AsrMethod = AsrMethod.Shafii,
    val hijriAdjust: Int = 0,
    val language: AppLanguage = AppLanguage.EN,
    val jamaatRules: List<JamaatRule> = defaultJamaatRules(),
    val alarmPrefs: List<AlarmToggle> = defaultAlarmPrefs(),
    val alarmSoundKey: String = "adhan_short" // NEW
)

/* Defaults */
fun defaultJamaatRules(): List<JamaatRule> = listOf(
    JamaatRule(Prayer.Fajr, RuleType.Offset, offsetMinutes = 20),
    JamaatRule(Prayer.Dhuhr, RuleType.Offset, offsetMinutes = 10),
    JamaatRule(Prayer.Asr, RuleType.Offset, offsetMinutes = 10),
    JamaatRule(Prayer.Maghrib, RuleType.Offset, offsetMinutes = 5),
    JamaatRule(Prayer.Isha, RuleType.Offset, offsetMinutes = 10),
    JamaatRule(Prayer.Jumuah, RuleType.Fixed, fixedHour12 = 1, fixedMinute = 30, fixedIsPm = true)
)

fun defaultAlarmPrefs(): List<AlarmToggle> = listOf(
    AlarmToggle(Prayer.Fajr,    startEnabled = true,  jamaatEnabled = true,  preMinutes = 0),
    AlarmToggle(Prayer.Dhuhr,   startEnabled = true,  jamaatEnabled = true,  preMinutes = 0),
    AlarmToggle(Prayer.Asr,     startEnabled = true,  jamaatEnabled = true,  preMinutes = 0),
    AlarmToggle(Prayer.Maghrib, startEnabled = true,  jamaatEnabled = true,  preMinutes = 0),
    AlarmToggle(Prayer.Isha,    startEnabled = true,  jamaatEnabled = true,  preMinutes = 0),
    AlarmToggle(Prayer.Jumuah,  startEnabled = false, jamaatEnabled = true,  preMinutes = 0)
)
