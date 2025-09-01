package com.crk.timeforsalah.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
// Explicitly import types from data package to avoid ambiguity
import com.crk.timeforsalah.data.Prayer
import com.crk.timeforsalah.data.JamaatRule
import com.crk.timeforsalah.data.JamaatRules
import com.crk.timeforsalah.data.RuleType
// Keep the wildcard for other things like formatTime, fixedToDateTime, if they are from there
import com.crk.timeforsalah.data.*
import kotlinx.coroutines.launch
// import java.time.LocalDate // Removed unused import
import java.time.LocalDateTime

@Composable
fun JamaatScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))
    val times = homeVm.prayerTimes.collectAsState().value

    val store = remember { JamaatRulesStore(context.applicationContext) }
    val rulesState = store.rules.collectAsState(initial = JamaatRules.default())
    val rules = rulesState.value

    val greenBg = Brush.verticalGradient(listOf(Color(0xFF6EDC7E), Color(0xFF3CC67A)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(greenBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Prayer here should now refer to com.crk.timeforsalah.data.Prayer due to explicit import
            listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha, Prayer.Jumuah).forEach { p ->
                val r = rules[p] // p is data.Prayer, rules expects data.Prayer. r is data.JamaatRule.
                val azan = when (p) {
                    Prayer.Fajr -> times?.fajr
                    Prayer.Dhuhr -> times?.dhuhr
                    Prayer.Jumuah -> times?.dhuhr // Assuming Jumuah uses Dhuhr time from 'times'
                    Prayer.Asr -> times?.asr
                    Prayer.Maghrib -> times?.maghrib
                    Prayer.Isha -> times?.isha
                    // else -> null // Not needed if the list is exhaustive for Prayer type from data
                }
                JamaatRuleCard(
                    prayer = p, // p is data.Prayer
                    azanTime = azan,
                    rule = r, // r is data.JamaatRule
                    onChange = { newRule -> // newRule will be data.JamaatRule
                        val newMap = rules.rules.toMutableMap()
                        newMap[p] = newRule // p is data.Prayer, newRule is data.JamaatRule
                        scope.launch { store.save(JamaatRules(newMap)) }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun JamaatRuleCard(
    prayer: Prayer, // This should now be com.crk.timeforsalah.data.Prayer
    azanTime: LocalDateTime?,
    rule: JamaatRule, // This should now be com.crk.timeforsalah.data.JamaatRule
    onChange: (JamaatRule) -> Unit // This should now be (com.crk.timeforsalah.data.JamaatRule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dot = when (prayer) { // prayer is data.Prayer
                    Prayer.Fajr -> Color(0xFFFFA726)
                    Prayer.Dhuhr -> Color(0xFF7CB342)
                    Prayer.Jumuah -> Color(0xFFF6C74A)
                    Prayer.Asr -> Color(0xFFFF7043)
                    Prayer.Maghrib -> Color(0xFFFF5252)
                    Prayer.Isha -> Color(0xFF42A5F5)
                    // else -> Color(0xFFD7FFD9) // If Prayer is an enum and all cases covered, else might not be needed
                }
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(prayer.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Azan: ${formatTime(azanTime)}", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (rule.type != RuleType.Offset) onChange(rule.copy(type = RuleType.Offset)) }
            ) {
                RadioButton(
                    selected = rule.type == RuleType.Offset, // RuleType should be data.RuleType
                    onClick = { onChange(rule.copy(type = RuleType.Offset)) }
                )
                Spacer(Modifier.width(6.dp))
                Text("Minutes after start time", color = Color.White, fontWeight = FontWeight.Medium)
            }

            if (rule.type == RuleType.Offset) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("–") { onChange(rule.copy(offsetMinutes = (rule.offsetMinutes - 1).coerceAtLeast(0))) }
                    Text("${rule.offsetMinutes}", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
                    StepButton("+") { onChange(rule.copy(offsetMinutes = (rule.offsetMinutes + 1).coerceAtMost(240))) }
                    Spacer(Modifier.width(6.dp))
                    Text("minutes", color = Color(0xFFB2F3B7))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (rule.type != RuleType.Fixed) onChange(rule.copy(type = RuleType.Fixed)) }
            ) {
                RadioButton(
                    selected = rule.type == RuleType.Fixed,
                    onClick = { onChange(rule.copy(type = RuleType.Fixed)) }
                )
                Spacer(Modifier.width(6.dp))
                Text("Set fixed time", color = Color.White, fontWeight = FontWeight.Medium)
            }

            if (rule.type == RuleType.Fixed) {
                Spacer(Modifier.height(8.dp))
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val hourInit = rule.fixedHour12.coerceIn(1, 12)
                val minuteInit = rule.fixedMinute.coerceIn(0, 59)
                val isPmInit = rule.fixedIsPm
                val hour24 = (hourInit % 12) + if (isPmInit) 12 else 0

                OutlinedButton(
                    onClick = {
                        TimePickerDialog(ctx, { _, h24, m ->
                            val pm = h24 >= 12
                            val h12 = ((h24 + 11) % 12) + 1
                            onChange(rule.copy(fixedHour12 = h12, fixedMinute = m, fixedIsPm = pm))
                        }, hour24, minuteInit, false).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD7FFD9))
                ) { Text(formatFixed(rule.fixedHour12, rule.fixedMinute, rule.fixedIsPm)) }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF26352B)) // Changed Divider to HorizontalDivider

            val jamaatTime = when (rule.type) {
                RuleType.Offset -> azanTime?.plusMinutes(rule.offsetMinutes.toLong())
                RuleType.Fixed -> fixedToDateTime(rule)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Jamaat Time:", color = Color(0xFFB2F3B7))
                Text(formatTime(jamaatTime), color = Color(0xFFF6C74A), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD7FFD9))
    ) { Text(label) }
}
