package com.crk.timeforsalah.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.crk.timeforsalah.R
import com.crk.timeforsalah.alarms.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState = SettingsUiState(),
    onSave: (SettingsUiState) -> Unit = {},
    onChangeLocationClick: (LocationMode) -> Unit = {} // kept for compatibility
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Working copy before "Save"
    var editable by remember(state) { mutableStateOf(state) }

    // --- Voice selection ---
    val voiceOptions = listOf(
        "adhan_full_1" to "Abdul Basit Abdul Samad",
        "adhan_full_2" to "Mishary Rashid Alafasy",
        "adhan_short"  to "Short Adhan",
        "adhan_short_2" to "Short Adhan 2",
        "azaan_common" to "Classic Adhan"
    )
    var voiceExpanded by remember { mutableStateOf(false) }
    var selectedVoiceKey by remember { mutableStateOf(state.alarmSoundKey.ifBlank { "azaan_common" }) }

    var pushEnabled by remember { mutableStateOf(true) }

    // Location method
    val locationOptions = listOf(
        LocationMode.GPS to "Auto-detect (GPS)",
        LocationMode.MANUAL to "Manual city"
    )
    var locExpanded by remember { mutableStateOf(false) }

    // Language
    val languageOptions = listOf(
        AppLanguage.EN to "English",
        AppLanguage.UR to "اردو (Urdu)",
        AppLanguage.AR to "العربية (Arabic)"
    )
    var langExpanded by remember { mutableStateOf(false) }

    // NEW: Calculation method + Asr method
    val calcOptions = listOf(
        CalculationMethod.MWL to "Muslim World League (MWL)",
        CalculationMethod.UmmAlQura to "Umm al-Qura (Makkah)",
        CalculationMethod.ISNA to "ISNA (North America)",
        CalculationMethod.Egyptian to "Egyptian",
        CalculationMethod.Karachi to "Karachi",
        CalculationMethod.Tehran to "University of Tehran"
    )
    var calcExpanded by remember { mutableStateOf(false) }

    val asrOptions = listOf(
        AsrMethod.Shafii to "Shafi’i / Maliki / Hanbali",
        AsrMethod.Hanafi to "Hanafi"
    )
    var asrExpanded by remember { mutableStateOf(false) }

    // City picker overlay + status for location
    var showCityPicker by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf<String?>(null) }

    // Permissions (GPS)
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            scope.launch {
                detectAndSetCity(
                    context,
                    onStart = { locating = true; locationStatus = "Detecting current location..." },
                    onCity = { city ->
                        locating = false
                        editable = editable.copy(manualCity = city)
                        locationStatus = "Detected: $city"
                    },
                    onError = { locating = false; locationStatus = it }
                )
            }
        } else {
            locationStatus = "Location permission denied"
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun handleChangeLocation() {
        when (editable.locationMode) {
            LocationMode.MANUAL -> showCityPicker = true
            LocationMode.GPS -> {
                if (hasLocationPermission()) {
                    scope.launch {
                        detectAndSetCity(
                            context,
                            onStart = { locating = true; locationStatus = "Detecting current location..." },
                            onCity = { city ->
                                locating = false
                                editable = editable.copy(manualCity = city)
                                locationStatus = "Detected: $city"
                            },
                            onError = { locating = false; locationStatus = it }
                        )
                    }
                } else {
                    permLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            }
        }
        onChangeLocationClick(editable.locationMode) // optional callback
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFA8FF78), Color(0xFF78FFD6))))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------- Azan Settings --------------------
            SectionCard(title = "Azan Settings") {
                Text("Muezzin Voice", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = voiceExpanded,
                    onExpandedChange = { voiceExpanded = !voiceExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = voiceOptions.find { it.first == selectedVoiceKey }?.second ?: "Choose voice",
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    ExposedDropdownMenu(
                        expanded = voiceExpanded,
                        onDismissRequest = { voiceExpanded = false }
                    ) {
                        voiceOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { selectedVoiceKey = key; voiceExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Test the currently selected sound by posting a one-off notification on its channel
                OutlinedButton(
                    onClick = {
                        val channelId = NotificationHelper.getOrCreateChannelForSound(context, selectedVoiceKey)
                        val testId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                        val notif = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setContentTitle("Test Adhan Notification")
                            .setContentText("This uses your selected Adhan sound.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .build()
                        NotificationManagerCompat.from(context).notify(testId, notif)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD7FFD9))
                ) {
                    Text("Test Notification Sound")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Push Notifications", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Receive prayer time reminders", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                    }
                    Switch(checked = pushEnabled, onCheckedChange = { pushEnabled = it })
                }
            }

            // -------------------- Location Settings --------------------
            SectionCard(title = "Location Settings") {
                Text("Location Method", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = locExpanded,
                    onExpandedChange = { locExpanded = !locExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = locationOptions.first { it.first == editable.locationMode }.second,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    ExposedDropdownMenu(
                        expanded = locExpanded,
                        onDismissRequest = { locExpanded = false }
                    ) {
                        locationOptions.forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    editable = editable.copy(locationMode = mode)
                                    locExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF152018), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = editable.manualCity.ifBlank { "New York, NY" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (locating) "Detecting…" else (locationStatus ?: "Tap Change Location to update"),
                        color = Color(0xFFB2F3B7),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { handleChangeLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD7FFD9))
                ) { Text("Change Location") }
            }

            // -------------------- Prayer Time Calculation (NEW) --------------------
            SectionCard(title = "Prayer Time Calculation") {
                // Calculation Method
                Text("Calculation Method", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = calcExpanded,
                    onExpandedChange = { calcExpanded = !calcExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = calcOptions.first { it.first == editable.calcMethod }.second,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calcExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    ExposedDropdownMenu(
                        expanded = calcExpanded,
                        onDismissRequest = { calcExpanded = false }
                    ) {
                        calcOptions.forEach { (m, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    editable = editable.copy(calcMethod = m)
                                    calcExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Asr Method
                Text("Asr Method", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = asrExpanded,
                    onExpandedChange = { asrExpanded = !asrExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = asrOptions.first { it.first == editable.asrMethod }.second,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = asrExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    ExposedDropdownMenu(
                        expanded = asrExpanded,
                        onDismissRequest = { asrExpanded = false }
                    ) {
                        asrOptions.forEach { (m, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    editable = editable.copy(asrMethod = m)
                                    asrExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // -------------------- App Settings --------------------
            SectionCard(title = "App Settings") {
                Text("Language", color = Color(0xFFB2F3B7), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        readOnly = true,
                        value = languageOptions.firstOrNull { it.first == editable.language }?.second ?: "English",
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languageOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { editable = editable.copy(language = code); langExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    onSave(
                        editable.copy(
                            alarmSoundKey = selectedVoiceKey
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6C74A),
                    contentColor = Color(0xFF0E1A12)
                ),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Save Settings", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))
        }

        // ---------- City Picker Overlay (Manual mode) ----------
        if (showCityPicker) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                color = Color.Transparent
            ) {
                CityPickerScreen(
                    onPick = { city, country ->
                        editable = editable.copy(
                            locationMode = LocationMode.MANUAL,
                            manualCity = "$city, $country"
                        )
                        locationStatus = "Selected: $city, $country"
                        showCityPicker = false
                    },
                    onBack = { showCityPicker = false }
                )
            }
        }
    }
}

/* ---------- Shared UI helpers ---------- */

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun detectAndSetCity(
    context: android.content.Context,
    onStart: () -> Unit = {},
    onCity: (String) -> Unit,
    onError: (String) -> Unit
) {
    onStart()

    val lm = context.getSystemService(LocationManager::class.java)
    val loc: Location? = try {
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } catch (_: SecurityException) {
        onError("Location permission missing"); return
    } catch (_: Exception) {
        null
    }

    val finalLoc: Location? = loc ?: if (Build.VERSION.SDK_INT >= 30) {
        suspendCancellableCoroutine { cont ->
            lm.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                null,
                context.mainExecutor
            ) { l -> cont.resume(l) }
        }
    } else null

    if (finalLoc == null) { onError("Could not get location"); return }

    val cityText = reverseGeocodeCity(context, finalLoc.latitude, finalLoc.longitude)
    if (cityText != null) onCity(cityText) else onError("Could not resolve city")
}

private suspend fun reverseGeocodeCity(
    context: android.content.Context,
    lat: Double,
    lon: Double
): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    return if (Build.VERSION.SDK_INT >= 33) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(lat, lon, 1) { list ->
                val a = list.firstOrNull()
                val city = a?.locality ?: a?.subAdminArea ?: a?.adminArea
                val country = a?.countryName
                cont.resume(if (city != null && country != null) "$city, $country" else country ?: city)
            }
        }
    } else {
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lon, 1)
                val a = list?.firstOrNull()
                val city = a?.locality ?: a?.subAdminArea ?: a?.adminArea
                val country = a?.countryName
                if (city != null && country != null) "$city, $country" else country ?: city
            } catch (_: Exception) { null }
        }
    }
}
