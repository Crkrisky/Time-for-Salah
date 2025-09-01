package com.crk.timeforsalah.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.crk.timeforsalah.data.appDataStore // Added import

/**
 * Stores Jamaat rules in a single Preferences string (no extra dependencies).
 * Format v1: "Fajr:O:15|Dhuhr:O:15|Asr:O:15|Maghrib:O:5|Isha:O:15|Jumuah:F:1,30,PM"
 */
class JamaatRulesStore(private val context: Context) {

    private object Keys {
        val RULES = stringPreferencesKey("jamaat_rules_v1")
    }

    val rules: Flow<JamaatRules> = context.appDataStore.data.map { p ->
        val raw = p[Keys.RULES]
        if (raw.isNullOrBlank()) JamaatRules.default() else JamaatRules.parse(raw)
    }

    suspend fun save(rules: JamaatRules) {
        context.appDataStore.edit { p ->
            p[Keys.RULES] = rules.format()
        }
    }
}

/* ----------------- Model + parse/format helpers ----------------- */

enum class RuleType { Offset, Fixed }
enum class Prayer(val displayName: String) {
    Fajr("Fajr"),
    Dhuhr("Dhuhr"),
    Asr("Asr"),
    Maghrib("Maghrib"),
    Isha("Isha"),
    Jumuah("Jumu'ah")
}

data class JamaatRule(
    val prayer: Prayer,
    val type: RuleType = RuleType.Offset,
    val offsetMinutes: Int = 15,
    val fixedHour12: Int = 1,
    val fixedMinute: Int = 30,
    val fixedIsPm: Boolean = true
)

data class JamaatRules(
    val rules: Map<Prayer, JamaatRule>
) {
    companion object {
        fun default() = JamaatRules(
            mapOf(
                Prayer.Fajr    to JamaatRule(Prayer.Fajr,    RuleType.Offset, 15),
                Prayer.Dhuhr   to JamaatRule(Prayer.Dhuhr,   RuleType.Offset, 15),
                Prayer.Asr     to JamaatRule(Prayer.Asr,     RuleType.Offset, 15),
                Prayer.Maghrib to JamaatRule(Prayer.Maghrib, RuleType.Offset, 5),
                Prayer.Isha    to JamaatRule(Prayer.Isha,    RuleType.Offset, 15),
                Prayer.Jumuah  to JamaatRule(Prayer.Jumuah,  RuleType.Fixed,  0, 1, 30, true)
            )
        )

        fun parse(raw: String): JamaatRules {
            val map = mutableMapOf<Prayer, JamaatRule>()
            raw.split("|").forEach { token ->
                if (token.isBlank() || !token.contains(":")) return@forEach
                val (pName, rest) = token.split(":", limit = 2)
                val prayer = Prayer.entries.find { it.name.equals(pName, ignoreCase = true) } ?: return@forEach
                val kind = rest.firstOrNull() ?: return@forEach
                when (kind) {
                    'O' -> {
                        val min = rest.substringAfter("O:", "15").toIntOrNull() ?: 15
                        map[prayer] = JamaatRule(prayer, RuleType.Offset, min)
                    }
                    'F' -> {
                        val tail = rest.substringAfter("F:", "")
                        val parts = tail.split(",")
                        if (parts.size >= 3) {
                            val h = parts[0].toIntOrNull() ?: 1
                            val m = parts[1].toIntOrNull() ?: 30
                            val pm = parts[2].equals("PM", ignoreCase = true)
                            map[prayer] = JamaatRule(
                                prayer, RuleType.Fixed,
                                offsetMinutes = 15,
                                fixedHour12 = h.coerceIn(1, 12),
                                fixedMinute = m.coerceIn(0, 59),
                                fixedIsPm = pm
                            )
                        }
                    }
                }
            }
            return if (map.isEmpty()) default() else JamaatRules(map)
        }
    }

    fun format(): String =
        Prayer.entries.joinToString("|") { p ->
            val r = rules[p] ?: return@joinToString ""
            when (r.type) {
                RuleType.Offset -> "${p.name}:O:${r.offsetMinutes}"
                RuleType.Fixed  -> "${p.name}:F:${r.fixedHour12},${r.fixedMinute},${if (r.fixedIsPm) "PM" else "AM"}"
            }
        }

    operator fun get(p: Prayer): JamaatRule = rules[p] ?: default().rules.getValue(p)
}