package com.crk.timeforsalah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.crk.timeforsalah.ui.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crk.timeforsalah.ui.SettingsScreen
import com.crk.timeforsalah.ui.SettingsViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TimeForSalahApp() }
    }
}

private enum class Screen { Home, Jamaat, Settings, Alarms }

@Composable
fun TimeForSalahApp() {
    var current by remember { mutableStateOf(Screen.Home) }
    val snackbar = remember { SnackbarHostState() }

    MaterialTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = current == Screen.Home,
                        onClick = { current = Screen.Home },
                        icon = { Text("🏠") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = current == Screen.Jamaat,
                        onClick = { current = Screen.Jamaat },
                        icon = { Text("🕌") },
                        label = { Text("Jamaat") }
                    )
                    NavigationBarItem(
                        selected = current == Screen.Settings,
                        onClick = { current = Screen.Settings },
                        icon = { Text("⚙️") },
                        label = { Text("Settings") }
                    )
                    NavigationBarItem(
                        selected = current == Screen.Alarms,
                        onClick = { current = Screen.Alarms },
                        icon = { Text("⏰") },
                        label = { Text("Alarms") }
                    )
                }
            }
        ) { padding ->
            Surface(Modifier.padding(padding)) @Composable {
                when (current) {
                    Screen.Home -> HomeScreen(snackbar)
                    Screen.Jamaat -> JamaatScreen()
                    Screen.Settings -> SettingsHost(onDone = { current = Screen.Home })
                    Screen.Alarms -> AlarmsScreen(snackbar)
                }
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
        onSave = { vm.save(it, onDone) }
    )
}
