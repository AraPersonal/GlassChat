package com.glasschat.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.glasschat.app.data.DiscoveredDevice
import com.glasschat.app.network.ChatConnection
import com.glasschat.app.network.NetworkScanner
import com.glasschat.app.ui.components.GlassIconBadge
import com.glasschat.app.ui.components.GlassSurface
import com.glasschat.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    onBack: () -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    val devices = remember { mutableStateListOf<DiscoveredDevice>() }
    var myIp by remember { mutableStateOf<String?>(null) }

    fun startScan() {
        devices.clear()
        isScanning = true
        myIp = NetworkScanner.getLocalIpAddress(context)
        scope.launch {
            NetworkScanner.scanForDevices(context) { found ->
                if (devices.none { it.ip == found.ip }) devices.add(found)
            }
            isScanning = false
        }
    }

    // also open a listening socket so other phones can find & connect to us
    LaunchedEffect(Unit) {
        ChatConnection.startListening()
        startScan()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconBadge(modifier = Modifier.clickable { onBack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("اتصال به گوشی دیگر", color = TextPrimary, fontSize = 19.sp())
                    Text(
                        myIp?.let { "آی‌پی من: $it" } ?: "به هات‌اسپات یا وای‌فای مشترک وصل شوید",
                        color = TextSecondary,
                        fontSize = 12.sp()
                    )
                }
            }

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Wifi, contentDescription = null, tint = AccentBluePale)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isScanning) "در حال جستجوی گوشی‌های متصل به هات‌اسپات..."
                        else "${devices.size} دستگاه پیدا شد",
                        color = TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentBluePale
                        )
                    } else {
                        TextButton(onClick = { startScan() }) {
                            Text("بروزرسانی", color = AccentBluePale)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices) { device ->
                    GlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ChatConnection.reset()
                                    ChatConnection.connectTo(device.ip)
                                    onDeviceSelected(device.ip)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Brush.linearGradient(listOf(AccentBlue, AccentPurple)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("گوشی ${device.ip}", color = TextPrimary)
                                Text("برای شروع چت ضربه بزنید", color = TextSecondary, fontSize = 12.sp())
                            }
                        }
                    }
                }

                if (!isScanning && devices.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("هیچ گوشی‌ای پیدا نشد", color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "مطمئن شوید هر دو گوشی به یک هات‌اسپات/وای‌فای وصل هستند و اپ روی هر دو باز است",
                                color = TextSecondary,
                                fontSize = 12.sp(),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// small helper so we don't need an extra import for .sp everywhere
private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
