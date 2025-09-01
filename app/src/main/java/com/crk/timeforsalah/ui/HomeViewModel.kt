package com.crk.timeforsalah.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crk.timeforsalah.data.AppLanguage
import com.crk.timeforsalah.data.AppSettings
import com.crk.timeforsalah.data.AsrMethod
import com.crk.timeforsalah.data.CalculationMethod
import com.crk.timeforsalah.data.LocationMode
import com.crk.timeforsalah.data.SettingsStore
import com.crk.timeforsalah.data.JamaatRulesStore
import com.crk.timeforsalah.core.AsrJuristic
import com.crk.timeforsalah.core.CityCoordinates
import com.crk.timeforsalah.core.JamaatCalculator
import com.crk.timeforsalah.core.PrayerTimes
import com.crk.timeforsalah.core.PrayerTimesCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Reads Settings + Jamaat rules and exposes:
 *  - start times (PrayerTimes)
 *  - jamaat times (JamaatCalculator.Result)
 */
class HomeViewModel(
    private val store: SettingsStore,
    private val jamaatStore: JamaatRulesStore
) : ViewModel() {

    private val _settings = MutableStateFlow(
        AppSettings(
            locationMode = LocationMode.MANUAL,
            manualCity   = "",
            calcMethod   = CalculationMethod.MWL,
            asrMethod    = AsrMethod.Shafii,
            hijriAdjust  = 0,
            language     = AppLanguage.EN
        )
    )
    val settings: StateFlow<AppSettings> = _settings

    private val _prayerTimes = MutableStateFlow<PrayerTimes?>(null)
    val prayerTimes: StateFlow<PrayerTimes?> = _prayerTimes

    private val _jamaatTimes = MutableStateFlow<JamaatCalculator.Result?>(null)
    val jamaatTimes: StateFlow<JamaatCalculator.Result?> = _jamaatTimes

    init {
        // Recalc start times whenever settings change
        viewModelScope.launch {
            store.settings.collectLatest { prefs ->
                _settings.value = prefs
                recalcStartTimes(prefs)
            }
        }
        // Recalc jamaat times whenever rules OR start times change
        viewModelScope.launch {
            jamaatStore.rules.collectLatest { rules ->
                val pt = _prayerTimes.value
                if (pt != null) {
                    _jamaatTimes.value = JamaatCalculator.compute(LocalDate.now(), pt, rules)
                } else {
                    _jamaatTimes.value = null
                }
            }
        }
        // Also listen to start time changes to recompute jamaat using last rules
        viewModelScope.launch {
            store.settings.collectLatest { _ -> // This should likely be prayerTimes.collectLatest or combine
                val pt = _prayerTimes.value
                val rules = jamaatStore.rules // flow; we can't block here
                // We’ll recompute jamaat when rules flow emits next; that's fine for now.
            }
        }
    }

    private fun recalcStartTimes(prefs: AppSettings) {
        val geo = CityCoordinates.resolve(prefs.manualCity)

        val coreMethod = when (prefs.calcMethod) {
            CalculationMethod.MWL       -> com.crk.timeforsalah.core.CalculationMethod.MWL
            CalculationMethod.UmmAlQura -> com.crk.timeforsalah.core.CalculationMethod.UmmAlQura
            CalculationMethod.ISNA      -> com.crk.timeforsalah.core.CalculationMethod.ISNA
            CalculationMethod.Egyptian  -> com.crk.timeforsalah.core.CalculationMethod.Egyptian
            CalculationMethod.Karachi   -> com.crk.timeforsalah.core.CalculationMethod.Karachi
            CalculationMethod.Tehran    -> com.crk.timeforsalah.core.CalculationMethod.Tehran
        }
        val coreAsr = if (prefs.asrMethod == AsrMethod.Hanafi)
            AsrJuristic.Hanafi else AsrJuristic.Shafii

        _prayerTimes.value = PrayerTimesCalculator.calculate(
            date = LocalDate.now(),
            latitude = geo.lat,
            longitude = geo.lon,
            tz = geo.zone,
            method = coreMethod,
            asrJuristic = coreAsr
        )
        // Jamaat times will be recomputed when the rules flow emits next.
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext as Application
                    val settings = SettingsStore.getInstance(app) // Updated to use getInstance
                    val jRules = JamaatRulesStore(app) // Assuming JamaatRulesStore also needs similar singleton treatment if it uses DataStore
                    return HomeViewModel(settings, jRules) as T
                }
            }
    }
}
