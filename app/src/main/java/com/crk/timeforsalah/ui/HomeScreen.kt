package com.crk.timeforsalah.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign // Added import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crk.timeforsalah.alarms.PrayerAlarmScheduler
import com.crk.timeforsalah.core.JamaatCalculator
import com.crk.timeforsalah.core.PrayerTimes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(snackbarHostState: SnackbarHostState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))

    val settings = vm.settings.collectAsState().value
    val city = settings.manualCity.ifBlank { "Your City" }
    val times = vm.prayerTimes.collectAsState().value
    val jamaat = vm.jamaatTimes.collectAsState().value

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }

    val prayers = listOfNotNull(
        "Fajr" to times?.fajr,
        "Sunrise" to times?.sunrise,
        "Dhuhr" to times?.dhuhr,
        "Asr" to times?.asr,
        "Maghrib" to times?.maghrib,
        "Isha" to times?.isha
    )
    val prayersJamat = listOfNotNull(
        "Fajr" to times?.fajr,
        "Sunrise" to times?.sunrise,
        "Dhuhr" to times?.dhuhr,
        "Asr" to times?.asr,
        "Maghrib" to times?.maghrib,
        "Isha" to times?.isha
    )
    val currentPair = prayers.lastOrNull { it.second != null && !it.second!!.isAfter(now) }
    // Try to find next prayer after now
// Find the next prayer normally
    var nextPair = prayersJamat.firstOrNull { it.second != null && it.second!!.isAfter(now) }

// If none found (after Isha), wrap to tomorrow's Fajr
    if (nextPair == null && currentPair?.first == "Isha") {
        prayersJamat.firstOrNull { it.first == "Fajr" }?.let { fajrPair ->
            nextPair = fajrPair.first to fajrPair.second?.plusDays(1) // shift +1 day
        }
    }
    if (currentPair?.first == "Fajr") {
        nextPair = prayersJamat.firstOrNull { it.first == "Dhuhr" }
    }
    val currentPairJamat = prayersJamat.lastOrNull { it.second != null && !it.second!!.isAfter(now) }
    var nextPairJamat = prayersJamat.firstOrNull { it.second != null && it.second!!.isAfter(now) }

    // If none found (after Isha), wrap to tomorrow's Fajr
    if (nextPair == null && currentPair?.first == "Isha") {
        prayersJamat.firstOrNull { it.first == "Fajr" }?.let { fajrPair ->
            nextPairJamat = fajrPair.first to fajrPair.second?.plusDays(1) // shift +1 day
        }
    }
    if (currentPair?.first == "Fajr") {
        nextPairJamat = prayersJamat.firstOrNull { it.first == "Dhuhr" }
    }




    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            doScheduleNow(context, times, jamaat)
            scope.launch { snackbarHostState.showSnackbar("Scheduled today’s alarms") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Notifications permission is required to schedule") }
        }
    }

    fun scheduleTodayAlarms() {
        if (times == null) {
            scope.launch { snackbarHostState.showSnackbar("Prayer times not ready yet") }
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS); return }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val am = context.getSystemService(AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms()) {
                val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
                scope.launch { snackbarHostState.showSnackbar("Enable \"Allow exact alarms\" then tap again") }
                return
            }
        }
        doScheduleNow(context, times, jamaat)
        scope.launch { snackbarHostState.showSnackbar("Scheduled today’s alarms") }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF6EDC7E), Color(0xFF3CC67A))))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = city,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0E1A12)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            ColorChip(backgroundColor = Color(0xFF212121)) {
                val today = LocalDate.now()
                val greg = today.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMMM yyyy", Locale.getDefault()))
                val hijri = formatHijri(settings.hijriAdjust)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(greg, color = Color(0xFFFFFFFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Text(hijri, color = Color(0xFFFFFFFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }


            }

            // ——— Current Prayer card (two circular countdowns: Start & Jamaat) ———
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallBlackCard(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularCard(
                            titleTop = "Current Start",
                            prayerName = currentPair?.first ?: "--",
                            target = currentPair?.second, // start time
                            ringColor = Color(0xFF4BE38B)
                        )
                        CircularCard(
                            titleTop = "Current Jamaat",
                            prayerName = currentPairJamat?.first ?: "--",
                            target = currentPairJamat?.first?.let { pname ->
                                jamaatTimeFor(pname, jamaat, now)
                            }, // jamaat time
                            ringColor = Color(0xFF4BE38B)
                        )
                    }
                }
            }

            // ——— Next Prayer card (two circular countdowns: Start & Jamaat) ———
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallBlackCard(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularCard(
                            titleTop = "Next Start",
                            prayerName = nextPair?.first ?: "--",
                            target = nextPair?.second, // start time
                            ringColor = Color(0xFFFEA12E)
                        )
                        CircularCard(
                            titleTop = "Next Jamaat",
                            prayerName = nextPairJamat?.first ?: "--",
                            target = nextPairJamat?.first?.let { pname ->
                                jamaatTimeFor(pname, jamaat, now)
                            }, // jamaat time
                            ringColor = Color(0xFFFEA12E)
                        )
                    }
                }
            }

            BlackTableCard {
                Text(
                    text = "Today Prayer Times",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 24.sp   // increase font size
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(0.4f)) {
                        TableHeader("Prayer")
                    }
                    Box(Modifier.weight(0.3f), contentAlignment = Alignment.Center) {
                        TableHeader("Azan")
                    }
                    Box(Modifier.weight(0.3f), contentAlignment = Alignment.Center) {
                        TableHeader("Jamaat")
                    }
                }
                Spacer(Modifier.height(6.dp)); DividerTiny()

                @Composable
                fun RowScope.TableCell(text: String, weight: Float, color: Color = Color.White, align: TextAlign = TextAlign.Start) {
                    Text(
                        text = text,
                        color = color,
                        modifier = Modifier.weight(weight).padding(horizontal = 4.dp),
                        textAlign = align,
                        fontSize = 18.sp
                    )
                }

                @Composable
                fun PrayerRow(name: String, nameColor: Color, azanTime: LocalDateTime?, jamaatTime: LocalDateTime?) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(text = name, weight = 0.4f, color = nameColor)
                        TableCell(text = formatTime(azanTime), weight = 0.3f, align = TextAlign.Center)
                        TableCell(text = jamaatTime?.let { formatTime(it) } ?: "—", weight = 0.3f, align = TextAlign.Center)
                    }
                    DividerTiny()
                }

                PrayerRow("Fajr", Color(0xFFFFA726), times?.fajr, jamaat?.fajr)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = "Sunrise", weight = 0.4f, color = Color(0xFFFFC107))
                    TableCell(text = formatTime(times?.sunrise), weight = 0.3f, align = TextAlign.Center)
                    TableCell(text = "—", weight = 0.3f, align = TextAlign.Center, color = Color.White) // Ensure color is set for "—"
                }
                DividerTiny()

                PrayerRow("Zuhr", Color(0xFF7CB342), times?.dhuhr, jamaat?.dhuhr)
                PrayerRow("Asr", Color(0xFFFF7043), times?.asr, jamaat?.asr)
                PrayerRow("Maghrib", Color(0xFFFF5252), times?.maghrib, jamaat?.maghrib)
                PrayerRow("Isha", Color(0xFF42A5F5), times?.isha, jamaat?.isha)
                // Last DividerTiny is part of PrayerRow, so no extra one needed here.
            }


        }
    }
}

/** Return the Jamaat time for a given prayer name (uses Jumuah for Dhuhr on Fridays). */
private fun jamaatTimeFor(
    prayerName: String,
    jamaat: JamaatCalculator.Result?,
    now: LocalDateTime
): LocalDateTime? {
    val isFriday = now.toLocalDate().dayOfWeek == DayOfWeek.FRIDAY
    return when (prayerName) {
        "Fajr"    -> jamaat?.fajr
        "Dhuhr"   -> if (isFriday) jamaat?.jumuah else jamaat?.dhuhr
        "Zuhr"    -> if (isFriday) jamaat?.jumuah else jamaat?.dhuhr // handle alternate label
        "Asr"     -> jamaat?.asr
        "Maghrib" -> jamaat?.maghrib
        "Isha"    -> jamaat?.isha
        else      -> null
    }
}

/** Schedules only for today using your existing scheduler. */
private fun doScheduleNow(
    context: android.content.Context,
    times: PrayerTimes?,
    jamaat: JamaatCalculator.Result?
) {
    val pt = times ?: return
    PrayerAlarmScheduler.scheduleForToday(
        context = context,
        times = pt,
        jamaat = jamaat,
        enableStart = mapOf("Fajr" to true, "Dhuhr" to true, "Asr" to true, "Maghrib" to true, "Isha" to true),
        enableJamaat = mapOf("Fajr" to true, "Dhuhr" to true, "Asr" to true, "Maghrib" to true, "Isha" to true),
        minutesBeforeJamaat = mapOf("Dhuhr" to 10, "Asr" to 10)
    )
}

// Helper functions (assuming they exist elsewhere or should be added if not)
// @Composable fun TableHeader(text: String) { Text(text, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp) }
// @Composable fun DividerTiny() { Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp) }
// fun formatTime(time: LocalDateTime?): String { return time?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: "—" }
// fun formatHijri(adjust: Int): String { /* Placeholder for Hijri date formatting */ return "1 Ramadan 1445 AH" }
// @Composable fun BlackChip(content: @Composable ColumnScope.() -> Unit) { /* Placeholder */ }
// @Composable fun SmallBlackCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) { /* Placeholder */ }
// @Composable fun CircularCard(titleTop: String, prayerName: String, target: LocalDateTime?, ringColor: Color) { /* Placeholder */ }
// @Composable fun BlackTableCard(content: @Composable ColumnScope.() -> Unit) { /* Placeholder */ }

