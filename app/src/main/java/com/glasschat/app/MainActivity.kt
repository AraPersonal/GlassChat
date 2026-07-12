package com.glasschat.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.glasschat.app.ui.screens.ChatScreen
import com.glasschat.app.ui.screens.DeviceDiscoveryScreen
import com.glasschat.app.ui.theme.GlassChatTheme

private enum class Screen { CHAT, DISCOVERY }

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result not critical to app flow, scanning still tries best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            GlassChatTheme {
                var screen by remember { mutableStateOf(Screen.CHAT) }

                when (screen) {
                    Screen.CHAT -> ChatScreen(
                        onOpenDiscovery = { screen = Screen.DISCOVERY }
                    )
                    Screen.DISCOVERY -> DeviceDiscoveryScreen(
                        onBack = { screen = Screen.CHAT },
                        onDeviceSelected = { screen = Screen.CHAT }
                    )
                }
            }
        }
    }
}
