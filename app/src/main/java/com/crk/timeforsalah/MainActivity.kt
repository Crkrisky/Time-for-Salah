package com.crk.timeforsalah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crk.timeforsalah.ui.*
import com.crk.timeforsalah.ui.SettingsScreen
import com.crk.timeforsalah.ui.SettingsViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TimeForSalahApp() }
    }
}

private enum class Screen { Home, Jamaat, Settings, Alarms, LocationPrompt }

@Composable
fun TimeForSalahApp() {
    var currentScreen by remember { mutableStateOf(Screen.LocationPrompt) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))
    val settingsState by settingsViewModel.uiState.collectAsState()

    // Observe settingsState and navigate to Home if a city is selected
    LaunchedEffect(settingsState.manualCity) {
        if (settingsState.manualCity.isNotEmpty()) {
            if (currentScreen == Screen.LocationPrompt) { // Only navigate if currently on prompt
                currentScreen = Screen.Home
            }
        } else {
            currentScreen = Screen.LocationPrompt // Ensure prompt is shown if city is cleared
        }
    }

    // State to track if notification permission is granted
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true) // No runtime permission needed before Android 13
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    // Request permission if on Android 13+ and not yet granted
    LaunchedEffect(key1 = Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MaterialTheme {
        if (hasNotificationPermission) {
            if (currentScreen == Screen.LocationPrompt) {
                LocationPromptScreen { currentScreen = Screen.Settings }
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == Screen.Home,
                                onClick = { currentScreen = Screen.Home },
                                icon = { Text("🏠") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Jamaat,
                                onClick = { currentScreen = Screen.Jamaat },
                                icon = { Text("🕌") },
                                label = { Text("Jamaat") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Settings,
                                onClick = { currentScreen = Screen.Settings },
                                icon = { Text("⚙️") },
                                label = { Text("Settings") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.Alarms,
                                onClick = { currentScreen = Screen.Alarms },
                                icon = { Text("⏰") },
                                label = { Text("Alarms") }
                            )
                        }
                    }
                ) { padding ->
                    Surface(Modifier.padding(padding)) @Composable {
                        when (currentScreen) {
                            Screen.Home -> HomeScreen(snackbarHostState)
                            Screen.Jamaat -> JamaatScreen()
                            Screen.Settings -> SettingsHost(onDone = {
                                if (settingsState.manualCity.isNotEmpty()) {
                                    currentScreen = Screen.Home
                                } else {
                                    currentScreen = Screen.LocationPrompt // Go back to prompt if no city set
                                }
                            })
                            Screen.Alarms -> AlarmsScreen(snackbarHostState)
                            Screen.LocationPrompt -> {} // Should be handled by the outer if
                        }
                    }
                }
            }
        } else {
            // Show a screen requesting permission
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Notification Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "This app needs notification permission to send you prayer time alerts. Please grant the permission to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        }
                    }) {
                        Text("Grant Permission in Settings")
                    }
                     Button(onClick = {
                         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                         }
                    }, modifier = Modifier.padding(top=8.dp)) {
                        Text("Retry Permission Request")
                    }
                }
            }
        }
    }
}

@Composable
fun LocationPromptScreen(onGoToSettingsClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Location Not Set",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Please select your location in settings to see prayer times.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(onClick = onGoToSettingsClick) {
                Text("Go to Settings")
            }
        }
    }
}

/** Keeps your existing Settings VM+Screen wiring isolated from the shell above. */
@Composable
private fun SettingsHost(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val vm: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(ctx))
    val ui = vm.uiState.collectAsState().value
    SettingsScreen(
        state = ui,
        onSave = { settingsUiState -> vm.save(settingsUiState, onDone) } // Pass onDone to save
    )
}
