package com.crk.timeforsalah.ui

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

import com.crk.timeforsalah.data.AlarmPrefs
import com.crk.timeforsalah.data.AlarmPrefsStore
import com.crk.timeforsalah.alarms.RescheduleReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(snackbar: SnackbarHostState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val alarmPrefsStore = remember { AlarmPrefsStore(context.applicationContext) }

    val prayers = listOf(
        PrayerCardUi("Fajr",    Color(0xFFFFA726)),
        PrayerCardUi("Zuhr",    Color(0xFF7CB342)),
        PrayerCardUi("Asr",     Color(0xFFFF7043)),
        PrayerCardUi("Maghrib", Color(0xFFFF5252)),
        PrayerCardUi("Isha",    Color(0xFF42A5F5))
    )

    val enabledMap           = remember { mutableStateMapOf<String, Boolean>() }
    val startCheckedMap      = remember { mutableStateMapOf<String, Boolean>() }
    val startOffsetLabelMap  = remember { mutableStateMapOf<String, String>() }
    val jamaatCheckedMap     = remember { mutableStateMapOf<String, Boolean>() }
    val jamaatOffsetLabelMap = remember { mutableStateMapOf<String, String>() }
    val soundLabelMap        = remember { mutableStateMapOf<String, String>() }

    fun keyForStore(name: String) = if (name == "Zuhr") "Dhuhr" else name

    fun canonicalSoundKey(label: String): String = when (label) {
        "Adhan Traditional" -> "adhan_traditional"
        "Adhan Makkah"      -> "adhan_makkah"
        "Adhan Madinah"     -> "adhan_madinah"
        "Adhan Short"       -> "adhan_short"
        else                -> "adhan_traditional" // Default
    }

    fun labelForCanonicalSoundKey(key: String): String = when (key) {
        "adhan_traditional" -> "Adhan Traditional"
        "adhan_makkah"      -> "Adhan Makkah"
        "adhan_madinah"     -> "Adhan Madinah"
        "adhan_short"       -> "Adhan Short"
        else                -> "Adhan Traditional" // Default
    }

    fun toMinutes(label: String): Int = when (label.lowercase(Locale.getDefault())) {
        "at prayer time"    -> 0
        "2 minutes before"  -> 2
        "5 minutes before"  -> 5
        "10 minutes before" -> 10
        "15 minutes before" -> 15
        "20 minutes before" -> 20
        "25 minutes before" -> 25
        "30 minutes before" -> 30
        else                -> 0 // Default
    }

    fun labelForOffsetMinutes(minutes: Int): String = when (minutes) {
        0  -> "At prayer time"
        2  -> "2 minutes before"
        5  -> "5 minutes before"
        10 -> "10 minutes before"
        15 -> "15 minutes before"
        20 -> "20 minutes before"
        25 -> "25 minutes before"
        30 -> "30 minutes before"
        else -> "5 minutes before" // Default
    }

    LaunchedEffect(Unit) {
        val savedPrefs = alarmPrefsStore.prefs.first()
        prayers.forEach { prayer ->
            val storeKey = keyForStore(prayer.name)

            enabledMap[prayer.name] = if (savedPrefs.startEnabled.isEmpty() && savedPrefs.jamaatEnabled.isEmpty() && savedPrefs.perPrayerSoundKey.isEmpty()) {
                true // Default for a fresh install or completely empty prefs
            } else {
                (savedPrefs.startEnabled[storeKey] ?: false) ||
                        (savedPrefs.jamaatEnabled[storeKey] ?: false) ||
                        savedPrefs.perPrayerSoundKey.containsKey(storeKey)
            }

            startCheckedMap[prayer.name] = savedPrefs.startEnabled[storeKey] ?: true
            startOffsetLabelMap[prayer.name] = labelForOffsetMinutes(savedPrefs.startPreMinutes[storeKey] ?: 5)

            jamaatCheckedMap[prayer.name] = savedPrefs.jamaatEnabled[storeKey] ?: false
            jamaatOffsetLabelMap[prayer.name] = labelForOffsetMinutes(savedPrefs.jamaatPreMinutes[storeKey] ?: 5)

            soundLabelMap[prayer.name] = labelForCanonicalSoundKey(
                savedPrefs.perPrayerSoundKey[storeKey] ?: canonicalSoundKey("Adhan Traditional")
            )
        }
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
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            prayers.forEach { base ->
                key(base.name) {
                    var soundExpanded by remember { mutableStateOf(false) }
                    var startOffsetExpanded by remember { mutableStateOf(false) }
                    var jamaatOffsetExpanded by remember { mutableStateOf(false) }

                    AlarmCard(
                        name = base.name,
                        accent = base.color,
                        enabled = enabledMap[base.name] ?: true,
                        onEnabledChange = { enabledMap[base.name] = it },

                        soundLabel = soundLabelMap[base.name] ?: labelForCanonicalSoundKey("adhan_traditional"),
                        onSoundClick = { soundExpanded = !soundExpanded },
                        soundExpanded = soundExpanded,
                        onSoundDismiss = { soundExpanded = false },
                        onSoundSelect = { soundLabelMap[base.name] = it },

                        startChecked = startCheckedMap[base.name] ?: true,
                        onStartCheckedChange = { startCheckedMap[base.name] = it },
                        startOffsetLabel = startOffsetLabelMap[base.name] ?: labelForOffsetMinutes(5),
                        startOffsetExpanded = startOffsetExpanded,
                        onStartOffsetClick = { startOffsetExpanded = !startOffsetExpanded },
                        onStartOffsetDismiss = { startOffsetExpanded = false },
                        onStartOffsetSelect = { startOffsetLabelMap[base.name] = it },

                        jamaatChecked = jamaatCheckedMap[base.name] ?: false,
                        onJamaatCheckedChange = { jamaatCheckedMap[base.name] = it },
                        jamaatOffsetLabel = jamaatOffsetLabelMap[base.name] ?: labelForOffsetMinutes(5),
                        jamaatOffsetExpanded = jamaatOffsetExpanded,
                        onJamaatOffsetClick = { jamaatOffsetExpanded = !jamaatOffsetExpanded },
                        onJamaatOffsetDismiss = { jamaatOffsetExpanded = false },
                        onJamaatOffsetSelect = { jamaatOffsetLabelMap[base.name] = it }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        val store = AlarmPrefsStore(context.applicationContext)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val am = context.getSystemService(AlarmManager::class.java)
                            if (am != null && !am.canScheduleExactAlarms()) {
                                val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = "package:${context.packageName}".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(i)
                                snackbar.showSnackbar("Turn on \"Allow exact alarms\", then tap Save again")
                                return@launch
                            }
                        }

                        val daily = listOf("Fajr","Zuhr","Asr","Maghrib","Isha")

                        val startEnabled = daily.associate { uiName ->
                            keyForStore(uiName) to (enabledMap[uiName] == true && startCheckedMap[uiName] == true)
                        }
                        val jamaatEnabled = buildMap {
                            daily.forEach { uiName ->
                                put(keyForStore(uiName), enabledMap[uiName] == true && jamaatCheckedMap[uiName] == true)
                            }
                            val existingPrefs = alarmPrefsStore.prefs.first()
                            put("Jumuah", existingPrefs.jamaatEnabled["Jumuah"] ?: true)
                        }

                        val startPre = daily.associate { uiName ->
                            keyForStore(uiName) to toMinutes(startOffsetLabelMap[uiName] ?: labelForOffsetMinutes(0))
                        }

                        val jamaatPre = buildMap {
                            daily.forEach { uiName ->
                                put(keyForStore(uiName), toMinutes(jamaatOffsetLabelMap[uiName] ?: labelForOffsetMinutes(0)))
                            }
                            val existingPrefs = alarmPrefsStore.prefs.first()
                            put("Jumuah", existingPrefs.jamaatPreMinutes["Jumuah"] ?: 0)
                        }

                        val perPrayerSounds = daily.associate { uiName ->
                            keyForStore(uiName) to canonicalSoundKey(
                                soundLabelMap[uiName] ?: labelForCanonicalSoundKey("adhan_traditional")
                            )
                        }

                        val existingGlobalSoundKey = alarmPrefsStore.prefs.first().soundKey

                        val newPrefs = AlarmPrefs(
                            startEnabled = startEnabled,
                            jamaatEnabled = jamaatEnabled,
                            startPreMinutes = startPre,
                            jamaatPreMinutes = jamaatPre,
                            perPrayerSoundKey = perPrayerSounds,
                            soundKey = existingGlobalSoundKey
                        )
                        store.save(newPrefs)

                        RescheduleReceiver.kickoff(context.applicationContext)
                        snackbar.showSnackbar("Alarms saved and rescheduled ✅")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6C74A),
                    contentColor = Color(0xFF0E1A12)
                ),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Save Alarms") }
        }
    }
}

private data class PrayerCardUi(val name: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmCard(
    name: String,
    accent: Color,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,

    soundLabel: String,
    onSoundClick: () -> Unit,
    soundExpanded: Boolean,
    onSoundDismiss: () -> Unit,
    onSoundSelect: (String) -> Unit,

    startChecked: Boolean,
    onStartCheckedChange: (Boolean) -> Unit,
    startOffsetLabel: String,
    startOffsetExpanded: Boolean,
    onStartOffsetClick: () -> Unit,
    onStartOffsetDismiss: () -> Unit,
    onStartOffsetSelect: (String) -> Unit,

    jamaatChecked: Boolean,
    onJamaatCheckedChange: (Boolean) -> Unit,
    jamaatOffsetLabel: String,
    jamaatOffsetExpanded: Boolean,
    onJamaatOffsetClick: () -> Unit,
    onJamaatOffsetDismiss: () -> Unit,
    onJamaatOffsetSelect: (String) -> Unit
) {
    val soundOptions = listOf("Adhan Traditional", "Adhan Makkah", "Adhan Madinah", "Adhan Short")
    val offsetOptions = listOf(
        "At prayer time", "2 minutes before", "5 minutes before", "10 minutes before",
        "15 minutes before", "20 minutes before", "25 minutes before", "30 minutes before"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(if (enabled) "Enabled" else "Disabled", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(Modifier.height(14.dp))

                Text("Sound Selection", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = soundExpanded,
                    onExpandedChange = { onSoundClick() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = soundLabel,
                        onValueChange = { /* handled by onSoundSelect */ },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color(0xFF152018),
                            unfocusedContainerColor = Color(0xFF152018),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(expanded = soundExpanded, onDismissRequest = onSoundDismiss) {
                        soundOptions.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { onSoundSelect(it); onSoundDismiss() })
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = startChecked, onCheckedChange = onStartCheckedChange)
                    Spacer(Modifier.width(6.dp))
                    Text("Start Time Notification", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                if (startChecked) {
                    Text("Notify me:", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                    ExposedDropdownMenuBox(
                        expanded = startOffsetExpanded,
                        onExpandedChange = { onStartOffsetClick() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            readOnly = true,
                            value = startOffsetLabel,
                            onValueChange = { /* handled by onStartOffsetSelect */ },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startOffsetExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors(
                                focusedContainerColor = Color(0xFF152018),
                                unfocusedContainerColor = Color(0xFF152018),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = startOffsetExpanded, onDismissRequest = onStartOffsetDismiss) {
                            offsetOptions.forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { onStartOffsetSelect(it); onStartOffsetDismiss() })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = jamaatChecked, onCheckedChange = onJamaatCheckedChange)
                    Spacer(Modifier.width(6.dp))
                    Text("Jamaat Time Notification", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                if (jamaatChecked) {
                    Text("Notify me:", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                    ExposedDropdownMenuBox(
                        expanded = jamaatOffsetExpanded,
                        onExpandedChange = { onJamaatOffsetClick() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            readOnly = true,
                            value = jamaatOffsetLabel,
                            onValueChange = { /* handled by onJamaatOffsetSelect */ },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jamaatOffsetExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors(
                                focusedContainerColor = Color(0xFF152018),
                                unfocusedContainerColor = Color(0xFF152018),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = jamaatOffsetExpanded, onDismissRequest = onJamaatOffsetDismiss) {
                            offsetOptions.forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { onJamaatOffsetSelect(it); onJamaatOffsetDismiss() })
                            }
                        }
                    }
                }
            }
        }
    }
}
