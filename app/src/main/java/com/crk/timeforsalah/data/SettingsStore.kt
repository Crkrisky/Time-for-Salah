package com.crk.timeforsalah.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first storage for app settings.
 *
 * Usage:
 *   val store = SettingsStore.getInstance(context)
 *   val flow: Flow<AppSettings> = store.settings
 *   store.save { it.copy(manualCity = "Karachi, Pakistan") }
 */

private const val DS_NAME = "time_for_salah_settings"

// Removed duplicate appDataStore definition here

class SettingsStore(private val context: Context) {

    private object Keys {
        val LOCATION_MODE = intPreferencesKey("location_mode") // 0 = MANUAL, 1 = GPS
        val MANUAL_CITY   = stringPreferencesKey("manual_city")
        val CALC_METHOD   = intPreferencesKey("calc_method")
        val ASR_METHOD    = intPreferencesKey("asr_method")
        val HIJRI_ADJUST  = intPreferencesKey("hijri_adjust")
        val LANGUAGE      = intPreferencesKey("language")
    }

    // Public stream of settings
    val settings: Flow<AppSettings> =
        context.appDataStore.data.map { p ->
            AppSettings(
                locationMode = LocationMode.fromOrdinal(p[Keys.LOCATION_MODE] ?: 0),
                manualCity   = p[Keys.MANUAL_CITY] ?: "",
                calcMethod   = CalculationMethod.fromOrdinal(p[Keys.CALC_METHOD] ?: 0),
                asrMethod    = AsrMethod.fromOrdinal(p[Keys.ASR_METHOD] ?: 1),
                hijriAdjust  = p[Keys.HIJRI_ADJUST] ?: 0,
                language     = AppLanguage.fromOrdinal(p[Keys.LANGUAGE] ?: 0),
            )
        }

    // Save with a reducer to keep callsites simple
    suspend fun save(reducer: (AppSettings) -> AppSettings) {
        context.appDataStore.edit { p ->
            val current = AppSettings(
                locationMode = LocationMode.fromOrdinal(p[Keys.LOCATION_MODE] ?: 0),
                manualCity   = p[Keys.MANUAL_CITY] ?: "",
                calcMethod   = CalculationMethod.fromOrdinal(p[Keys.CALC_METHOD] ?: 0),
                asrMethod    = AsrMethod.fromOrdinal(p[Keys.ASR_METHOD] ?: 1),
                hijriAdjust  = p[Keys.HIJRI_ADJUST] ?: 0,
                language     = AppLanguage.fromOrdinal(p[Keys.LANGUAGE] ?: 0),
            )
            val next = reducer(current)
            p[Keys.LOCATION_MODE] = next.locationMode.ordinal
            p[Keys.MANUAL_CITY]   = next.manualCity
            p[Keys.CALC_METHOD]   = next.calcMethod.ordinal
            p[Keys.ASR_METHOD]    = next.asrMethod.ordinal
            p[Keys.HIJRI_ADJUST]  = next.hijriAdjust
            p[Keys.LANGUAGE]      = next.language.ordinal
        }
    }

    companion object {
        @Volatile private var INSTANCE: SettingsStore? = null
        fun getInstance(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                val instance = SettingsStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
    }
}

/* ----------------- Models (mirror your SettingsScreen enums) ----------------- */

data class AppSettings(
    val locationMode: LocationMode,
    val manualCity: String,
    val calcMethod: CalculationMethod,
    val asrMethod: AsrMethod,
    val hijriAdjust: Int,
    val language: AppLanguage
)

enum class LocationMode { MANUAL, GPS;
    companion object { fun fromOrdinal(i: Int) = entries.getOrElse(i) { MANUAL } }
}
enum class CalculationMethod {
    MWL, UmmAlQura, ISNA, Egyptian, Karachi, Tehran;
    companion object { fun fromOrdinal(i: Int) = entries.getOrElse(i) { MWL } }
}
enum class AsrMethod { Hanafi, Shafii;
    companion object { fun fromOrdinal(i: Int) = entries.getOrElse(i) { Shafii } }
}
enum class AppLanguage { EN, UR, AR;
    companion object { fun fromOrdinal(i: Int) = entries.getOrElse(i) { EN } }
}
