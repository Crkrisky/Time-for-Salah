package com.crk.timeforsalah.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Per-prayer alarm preferences + selected Adhan sound keys and offsets.
 * Uses the SAME DataStore file via Context.appDataStore (see AppDataStore.kt).
 */
data class AlarmPrefs(
    val startEnabled: Map<String, Boolean>,          // Fajr..Isha
    val jamaatEnabled: Map<String, Boolean>,         // Fajr..Isha + Jumuah
    val startPreMinutes: Map<String, Int>,           // Fajr..Isha (X minutes BEFORE start)
    val jamaatPreMinutes: Map<String, Int>,          // Fajr..Isha + Jumuah (X minutes BEFORE jamaat)
    val perPrayerSoundKey: Map<String, String>,      // Fajr..Isha (+optional Jumuah)
    val soundKey: String                             // legacy global fallback
) {
    companion object {
        fun default() = AlarmPrefs(
            startEnabled = mapOf(
                "Fajr" to true, "Dhuhr" to true, "Asr" to true, "Maghrib" to true, "Isha" to true
            ),
            jamaatEnabled = mapOf(
                "Fajr" to true, "Dhuhr" to true, "Asr" to true, "Maghrib" to true, "Isha" to true, "Jumuah" to true
            ),
            startPreMinutes = mapOf(
                "Fajr" to 0, "Dhuhr" to 0, "Asr" to 0, "Maghrib" to 0, "Isha" to 0
            ),
            jamaatPreMinutes = mapOf(
                "Fajr" to 0, "Dhuhr" to 0, "Asr" to 0, "Maghrib" to 0, "Isha" to 0, "Jumuah" to 0
            ),
            perPrayerSoundKey = emptyMap(),
            soundKey = "adhan_traditional"
        )
    }
}

class AlarmPrefsStore(private val context: Context) {

    // Boolean keys (start)
    private val START_FAJR     = booleanPreferencesKey("alarm_start_fajr")
    private val START_DHUHR    = booleanPreferencesKey("alarm_start_dhuhr")
    private val START_ASR      = booleanPreferencesKey("alarm_start_asr")
    private val START_MAGHRIB  = booleanPreferencesKey("alarm_start_maghrib")
    private val START_ISHA     = booleanPreferencesKey("alarm_start_isha")

    // Boolean keys (jamaat)
    private val JAM_FAJR       = booleanPreferencesKey("alarm_jam_fajr")
    private val JAM_DHUHR      = booleanPreferencesKey("alarm_jam_dhuhr")
    private val JAM_ASR        = booleanPreferencesKey("alarm_jam_asr")
    private val JAM_MAGHRIB    = booleanPreferencesKey("alarm_jam_maghrib")
    private val JAM_ISHA       = booleanPreferencesKey("alarm_jam_isha")
    private val JAM_JUMUAH     = booleanPreferencesKey("alarm_jam_jumuah")

    // Int keys (EXISTING: minutes before *jamaat*) — keep as-is
    private val PRE_FAJR       = intPreferencesKey("alarm_pre_fajr")
    private val PRE_DHUHR      = intPreferencesKey("alarm_pre_dhuhr")
    private val PRE_ASR        = intPreferencesKey("alarm_pre_asr")
    private val PRE_MAGHRIB    = intPreferencesKey("alarm_pre_maghrib")
    private val PRE_ISHA       = intPreferencesKey("alarm_pre_isha")
    private val PRE_JUMUAH     = intPreferencesKey("alarm_pre_jumuah")

    // Int keys (NEW): minutes before *start*
    private val STARTPRE_FAJR  = intPreferencesKey("alarm_startpre_fajr")
    private val STARTPRE_DHUHR = intPreferencesKey("alarm_startpre_dhuhr")
    private val STARTPRE_ASR   = intPreferencesKey("alarm_startpre_asr")
    private val STARTPRE_MAG   = intPreferencesKey("alarm_startpre_maghrib")
    private val STARTPRE_ISHA  = intPreferencesKey("alarm_startpre_isha")

    // Sound key (global + per-prayer)
    private val SOUND_KEY      = stringPreferencesKey("alarm_sound_key")
    private val SND_FAJR       = stringPreferencesKey("alarm_sound_fajr")
    private val SND_DHUHR      = stringPreferencesKey("alarm_sound_dhuhr")
    private val SND_ASR        = stringPreferencesKey("alarm_sound_asr")
    private val SND_MAG        = stringPreferencesKey("alarm_sound_maghrib")
    private val SND_ISHA       = stringPreferencesKey("alarm_sound_isha")
    private val SND_JUM        = stringPreferencesKey("alarm_sound_jumuah")

    val prefs: Flow<AlarmPrefs> = context.appDataStore.data.map { p: Preferences ->
        val def = AlarmPrefs.default()
        val startEnabled = mapOf(
            "Fajr" to (p[START_FAJR] ?: def.startEnabled["Fajr"]!!),
            "Dhuhr" to (p[START_DHUHR] ?: def.startEnabled["Dhuhr"]!!),
            "Asr" to (p[START_ASR] ?: def.startEnabled["Asr"]!!),
            "Maghrib" to (p[START_MAGHRIB] ?: def.startEnabled["Maghrib"]!!),
            "Isha" to (p[START_ISHA] ?: def.startEnabled["Isha"]!!)
        )
        val jamaatEnabled = mapOf(
            "Fajr" to (p[JAM_FAJR] ?: def.jamaatEnabled["Fajr"]!!),
            "Dhuhr" to (p[JAM_DHUHR] ?: def.jamaatEnabled["Dhuhr"]!!),
            "Asr" to (p[JAM_ASR] ?: def.jamaatEnabled["Asr"]!!),
            "Maghrib" to (p[JAM_MAGHRIB] ?: def.jamaatEnabled["Maghrib"]!!),
            "Isha" to (p[JAM_ISHA] ?: def.jamaatEnabled["Isha"]!!),
            "Jumuah" to (p[JAM_JUMUAH] ?: def.jamaatEnabled["Jumuah"]!!)
        )
        val startPre = mapOf(
            "Fajr" to (p[STARTPRE_FAJR] ?: def.startPreMinutes["Fajr"]!!),
            "Dhuhr" to (p[STARTPRE_DHUHR] ?: def.startPreMinutes["Dhuhr"]!!),
            "Asr" to (p[STARTPRE_ASR] ?: def.startPreMinutes["Asr"]!!),
            "Maghrib" to (p[STARTPRE_MAG] ?: def.startPreMinutes["Maghrib"]!!),
            "Isha" to (p[STARTPRE_ISHA] ?: def.startPreMinutes["Isha"]!!)
        )
        val jamaatPre = mapOf(
            "Fajr" to (p[PRE_FAJR] ?: def.jamaatPreMinutes["Fajr"]!!),
            "Dhuhr" to (p[PRE_DHUHR] ?: def.jamaatPreMinutes["Dhuhr"]!!),
            "Asr" to (p[PRE_ASR] ?: def.jamaatPreMinutes["Asr"]!!),
            "Maghrib" to (p[PRE_MAGHRIB] ?: def.jamaatPreMinutes["Maghrib"]!!),
            "Isha" to (p[PRE_ISHA] ?: def.jamaatPreMinutes["Isha"]!!),
            "Jumuah" to (p[PRE_JUMUAH] ?: def.jamaatPreMinutes["Jumuah"]!!)
        )
        val perPrayerSounds = buildMap {
            p[SND_FAJR]?.let { put("Fajr", it) }
            p[SND_DHUHR]?.let { put("Dhuhr", it) }
            p[SND_ASR]?.let { put("Asr", it) }
            p[SND_MAG]?.let { put("Maghrib", it) }
            p[SND_ISHA]?.let { put("Isha", it) }
            p[SND_JUM]?.let { put("Jumuah", it) }
        }
        AlarmPrefs(
            startEnabled = startEnabled,
            jamaatEnabled = jamaatEnabled,
            startPreMinutes = startPre,
            jamaatPreMinutes = jamaatPre,
            perPrayerSoundKey = perPrayerSounds,
            soundKey = p[SOUND_KEY] ?: def.soundKey
        )
    }

    suspend fun save(newPrefs: AlarmPrefs) {
        context.appDataStore.edit { p ->
            p[START_FAJR]    = newPrefs.startEnabled["Fajr"] == true
            p[START_DHUHR]   = newPrefs.startEnabled["Dhuhr"] == true
            p[START_ASR]     = newPrefs.startEnabled["Asr"] == true
            p[START_MAGHRIB] = newPrefs.startEnabled["Maghrib"] == true
            p[START_ISHA]    = newPrefs.startEnabled["Isha"] == true

            p[JAM_FAJR]      = newPrefs.jamaatEnabled["Fajr"] == true
            p[JAM_DHUHR]     = newPrefs.jamaatEnabled["Dhuhr"] == true
            p[JAM_ASR]       = newPrefs.jamaatEnabled["Asr"] == true
            p[JAM_MAGHRIB]   = newPrefs.jamaatEnabled["Maghrib"] == true
            p[JAM_ISHA]      = newPrefs.jamaatEnabled["Isha"] == true
            p[JAM_JUMUAH]    = newPrefs.jamaatEnabled["Jumuah"] == true

            p[STARTPRE_FAJR] = newPrefs.startPreMinutes["Fajr"] ?: 0
            p[STARTPRE_DHUHR]= newPrefs.startPreMinutes["Dhuhr"] ?: 0
            p[STARTPRE_ASR]  = newPrefs.startPreMinutes["Asr"] ?: 0
            p[STARTPRE_MAG]  = newPrefs.startPreMinutes["Maghrib"] ?: 0
            p[STARTPRE_ISHA] = newPrefs.startPreMinutes["Isha"] ?: 0

            p[PRE_FAJR]      = newPrefs.jamaatPreMinutes["Fajr"] ?: 0
            p[PRE_DHUHR]     = newPrefs.jamaatPreMinutes["Dhuhr"] ?: 0
            p[PRE_ASR]       = newPrefs.jamaatPreMinutes["Asr"] ?: 0
            p[PRE_MAGHRIB]   = newPrefs.jamaatPreMinutes["Maghrib"] ?: 0
            p[PRE_ISHA]      = newPrefs.jamaatPreMinutes["Isha"] ?: 0
            p[PRE_JUMUAH]    = newPrefs.jamaatPreMinutes["Jumuah"] ?: 0

            p[SOUND_KEY]     = newPrefs.soundKey

            newPrefs.perPrayerSoundKey["Fajr"]?.let   { p[SND_FAJR] = it }
            newPrefs.perPrayerSoundKey["Dhuhr"]?.let  { p[SND_DHUHR] = it }
            newPrefs.perPrayerSoundKey["Asr"]?.let    { p[SND_ASR] = it }
            newPrefs.perPrayerSoundKey["Maghrib"]?.let{ p[SND_MAG] = it }
            newPrefs.perPrayerSoundKey["Isha"]?.let   { p[SND_ISHA] = it }
            newPrefs.perPrayerSoundKey["Jumuah"]?.let { p[SND_JUM] = it }
        }
    }
}
