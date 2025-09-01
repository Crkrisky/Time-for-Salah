package com.crk.timeforsalah.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crk.timeforsalah.alarms.RescheduleReceiver
import com.crk.timeforsalah.data.AppSettings
import com.crk.timeforsalah.data.SettingsStore
import com.crk.timeforsalah.data.JamaatRulesStore
import com.crk.timeforsalah.data.AlarmPrefsStore
import com.crk.timeforsalah.data.AlarmPrefs

// Alias Data-layer enums so we never confuse them with UI enums
import com.crk.timeforsalah.data.AppLanguage as StoreLang
import com.crk.timeforsalah.data.LocationMode as StoreLoc
import com.crk.timeforsalah.data.CalculationMethod as StoreCalc
import com.crk.timeforsalah.data.AsrMethod as StoreAsr
import com.crk.timeforsalah.data.JamaatRule as StoreRule
import com.crk.timeforsalah.data.RuleType as StoreRuleType
import com.crk.timeforsalah.data.Prayer as StorePrayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Bridges SettingsStore + JamaatRulesStore + AlarmPrefsStore to the UI.
 * All mappings between UI (com.crk.timeforsalah.ui.*) and Data (com.crk.timeforsalah.data.*)
 * live here to avoid type mix-ups.
 */
class SettingsViewModel(
    private val store: SettingsStore,
    private val jamaatStore: JamaatRulesStore,
    private val appContext: Context,
    private val alarmStore: AlarmPrefsStore = AlarmPrefsStore(appContext)
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        // Core settings -> UI
        viewModelScope.launch {
            store.settings.collectLatest { appSettings: AppSettings ->
                _uiState.value = appSettings.toUiState(previous = _uiState.value)
            }
        }

        // Jamaat rules (Data) -> UI
        viewModelScope.launch {
            jamaatStore.rules.collectLatest { rules ->
                val list = listOf(
                    rules[StorePrayer.Fajr].toUiRule(Prayer.Fajr),
                    rules[StorePrayer.Dhuhr].toUiRule(Prayer.Dhuhr),
                    rules[StorePrayer.Asr].toUiRule(Prayer.Asr),
                    rules[StorePrayer.Maghrib].toUiRule(Prayer.Maghrib),
                    rules[StorePrayer.Isha].toUiRule(Prayer.Isha),
                    rules[StorePrayer.Jumuah].toUiRule(Prayer.Jumuah),
                )
                _uiState.value = _uiState.value.copy(jamaatRules = list)
            }
        }

        // Alarm prefs (Data) -> UI
        viewModelScope.launch {
            alarmStore.prefs.collectLatest { prefs: AlarmPrefs ->
                _uiState.value = _uiState.value.copy(alarmPrefs = prefs.toUi())
            }
        }
    }

    /** Save core settings + Jamaat rules + Alarm prefs, then reschedule alarms. */
    fun save(state: SettingsUiState = _uiState.value, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            // Core
            store.save { current ->
                current.copy(
                    locationMode = state.locationMode.toStore(),
                    manualCity   = state.manualCity,
                    calcMethod   = state.calcMethod.toStore(),
                    asrMethod    = state.asrMethod.toStore(),
                    hijriAdjust  = state.hijriAdjust,
                    language     = state.language.toStore()
                )
            }

            // Jamaat rules: UI -> Data (Map<StorePrayer, StoreRule>)
            val ruleMap: Map<StorePrayer, StoreRule> =
                state.jamaatRules.associate { uiRule ->
                    uiRule.prayer.toStorePrayer() to uiRule.toStoreRule()
                }
            jamaatStore.save(com.crk.timeforsalah.data.JamaatRules(ruleMap))

            // Alarm prefs: UI -> Data (keep global sound; per-prayer sounds are saved on the Alarms screen)
            alarmStore.save(state.alarmPrefs.toStore(state.alarmSoundKey))

            // Refresh alarms immediately so changes take effect
            RescheduleReceiver.kickoff(appContext)
            onDone()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext as Application
                    val settings = SettingsStore.getInstance(app)
                    val jRules = JamaatRulesStore(app)
                    return SettingsViewModel(settings, jRules, app, AlarmPrefsStore(app)) as T
                }
            }
    }
}

/* -------------------- MAPPERS (Data <-> UI) -------------------- */

private fun AppSettings.toUiState(previous: SettingsUiState): SettingsUiState {
    return SettingsUiState(
        locationMode = when (locationMode) {
            StoreLoc.MANUAL -> LocationMode.MANUAL
            StoreLoc.GPS    -> LocationMode.GPS
        },
        manualCity = manualCity,
        calcMethod = when (calcMethod) {
            StoreCalc.MWL       -> CalculationMethod.MWL
            StoreCalc.UmmAlQura -> CalculationMethod.UmmAlQura
            StoreCalc.ISNA      -> CalculationMethod.ISNA
            StoreCalc.Egyptian  -> CalculationMethod.Egyptian
            StoreCalc.Karachi   -> CalculationMethod.Karachi
            StoreCalc.Tehran    -> CalculationMethod.Tehran
        },
        asrMethod = when (asrMethod) {
            StoreAsr.Hanafi -> AsrMethod.Hanafi
            StoreAsr.Shafii -> AsrMethod.Shafii
        },
        hijriAdjust = hijriAdjust,
        language = when (language) {
            StoreLang.EN -> AppLanguage.EN
            StoreLang.UR -> AppLanguage.UR
            StoreLang.AR -> AppLanguage.AR
        },
        // Keep whatever Jamaat/Alarm UI currently shows until their stores emit
        jamaatRules = previous.jamaatRules,
        alarmPrefs  = previous.alarmPrefs
    )
}

private fun LocationMode.toStore(): StoreLoc = when (this) {
    LocationMode.MANUAL -> StoreLoc.MANUAL
    LocationMode.GPS    -> StoreLoc.GPS
}

private fun CalculationMethod.toStore(): StoreCalc = when (this) {
    CalculationMethod.MWL       -> StoreCalc.MWL
    CalculationMethod.UmmAlQura -> StoreCalc.UmmAlQura
    CalculationMethod.ISNA      -> StoreCalc.ISNA
    CalculationMethod.Egyptian  -> StoreCalc.Egyptian
    CalculationMethod.Karachi   -> StoreCalc.Karachi
    CalculationMethod.Tehran    -> StoreCalc.Tehran
}

private fun AsrMethod.toStore(): StoreAsr = when (this) {
    AsrMethod.Hanafi -> StoreAsr.Hanafi
    AsrMethod.Shafii -> StoreAsr.Shafii
}

private fun AppLanguage.toStore(): StoreLang = when (this) {
    AppLanguage.EN -> StoreLang.EN
    AppLanguage.UR -> StoreLang.UR
    AppLanguage.AR -> StoreLang.AR
}

/* ---- Jamaat rules: Data <-> UI ---- */

private fun com.crk.timeforsalah.data.JamaatRule.toUiRule(p: Prayer): JamaatRule =
    JamaatRule(
        prayer = p,
        type = if (this.type == com.crk.timeforsalah.data.RuleType.Offset) RuleType.Offset else RuleType.Fixed,
        offsetMinutes = this.offsetMinutes,
        fixedHour12 = this.fixedHour12,
        fixedMinute = this.fixedMinute,
        fixedIsPm = this.fixedIsPm
    )

private fun Prayer.toStorePrayer(): StorePrayer = when (this) {
    Prayer.Fajr    -> StorePrayer.Fajr
    Prayer.Dhuhr   -> StorePrayer.Dhuhr
    Prayer.Asr     -> StorePrayer.Asr
    Prayer.Maghrib -> StorePrayer.Maghrib
    Prayer.Isha    -> StorePrayer.Isha
    Prayer.Jumuah  -> StorePrayer.Jumuah
    Prayer.Sunrise -> StorePrayer.Dhuhr // Sunrise has no Jamaat; never saved
}

private fun JamaatRule.toStoreRule(): StoreRule =
    StoreRule(
        prayer = this.prayer.toStorePrayer(),
        type = if (this.type == RuleType.Offset) StoreRuleType.Offset else StoreRuleType.Fixed,
        offsetMinutes = this.offsetMinutes,
        fixedHour12 = this.fixedHour12,
        fixedMinute = this.fixedMinute,
        fixedIsPm = this.fixedIsPm
    )

/* ---- Alarm prefs: Data <-> UI ---- */
/* The Settings screen historically exposed only one "preMinutes" slider (for Jamaat).
 * To stay backward compatible, we map that to jamaatPreMinutes here.
 * Per-prayer sounds and startPreMinutes are handled on the dedicated Alarms screen.
 */
private fun AlarmPrefs.toUi(): List<AlarmToggle> = listOf(
    AlarmToggle(Prayer.Fajr,    startEnabled["Fajr"] == true,    jamaatEnabled["Fajr"] == true,    jamaatPreMinutes["Fajr"] ?: 0),
    AlarmToggle(Prayer.Dhuhr,   startEnabled["Dhuhr"] == true,   jamaatEnabled["Dhuhr"] == true,   jamaatPreMinutes["Dhuhr"] ?: 0),
    AlarmToggle(Prayer.Asr,     startEnabled["Asr"] == true,     jamaatEnabled["Asr"] == true,     jamaatPreMinutes["Asr"] ?: 0),
    AlarmToggle(Prayer.Maghrib, startEnabled["Maghrib"] == true, jamaatEnabled["Maghrib"] == true, jamaatPreMinutes["Maghrib"] ?: 0),
    AlarmToggle(Prayer.Isha,    startEnabled["Isha"] == true,    jamaatEnabled["Isha"] == true,    jamaatPreMinutes["Isha"] ?: 0),
    AlarmToggle(Prayer.Jumuah,  startEnabled = false,            jamaatEnabled["Jumuah"] == true,  jamaatPreMinutes["Jumuah"] ?: 0)
)

private fun List<AlarmToggle>.toStore(soundKey: String): AlarmPrefs = AlarmPrefs(
    startEnabled = mapOf(
        "Fajr" to (find { it.prayer == Prayer.Fajr }?.startEnabled ?: true),
        "Dhuhr" to (find { it.prayer == Prayer.Dhuhr }?.startEnabled ?: true),
        "Asr" to (find { it.prayer == Prayer.Asr }?.startEnabled ?: true),
        "Maghrib" to (find { it.prayer == Prayer.Maghrib }?.startEnabled ?: true),
        "Isha" to (find { it.prayer == Prayer.Isha }?.startEnabled ?: true)
    ),
    jamaatEnabled = mapOf(
        "Fajr" to (find { it.prayer == Prayer.Fajr }?.jamaatEnabled ?: true),
        "Dhuhr" to (find { it.prayer == Prayer.Dhuhr }?.jamaatEnabled ?: true),
        "Asr" to (find { it.prayer == Prayer.Asr }?.jamaatEnabled ?: true),
        "Maghrib" to (find { it.prayer == Prayer.Maghrib }?.jamaatEnabled ?: true),
        "Isha" to (find { it.prayer == Prayer.Isha }?.jamaatEnabled ?: true),
        "Jumuah" to (find { it.prayer == Prayer.Jumuah }?.jamaatEnabled ?: true)
    ),
    // Settings screen doesn't set start offsets → default to 0
    startPreMinutes = mapOf(
        "Fajr" to 0, "Dhuhr" to 0, "Asr" to 0, "Maghrib" to 0, "Isha" to 0
    ),
    // Back-compat: the old single "preMinutes" was for Jamaat
    jamaatPreMinutes = mapOf(
        "Fajr" to (find { it.prayer == Prayer.Fajr }?.preMinutes ?: 0),
        "Dhuhr" to (find { it.prayer == Prayer.Dhuhr }?.preMinutes ?: 0),
        "Asr" to (find { it.prayer == Prayer.Asr }?.preMinutes ?: 0),
        "Maghrib" to (find { it.prayer == Prayer.Maghrib }?.preMinutes ?: 0),
        "Isha" to (find { it.prayer == Prayer.Isha }?.preMinutes ?: 0),
        "Jumuah" to (find { it.prayer == Prayer.Jumuah }?.preMinutes ?: 0)
    ),
    // Per-prayer sounds are set on the Alarms screen; keep empty here.
    perPrayerSoundKey = emptyMap(),
    // Global fallback sound key
    soundKey = soundKey
)
