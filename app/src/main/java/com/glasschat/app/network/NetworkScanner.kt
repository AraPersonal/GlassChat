package com.glasschat.app.network

import android.content.Context
import android.net.wifi.WifiManager
import com.glasschat.app.data.DiscoveredDevice
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Finds other phones on the same hotspot/Wi-Fi subnet that are running
 * GlassChat and listening on CHAT_PORT.
 */
object NetworkScanner {

    /** Returns this device's IPv4 address on the current Wi-Fi/hotspot network, e.g. 192.168.43.12 */
    fun getLocalIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val ipInt = wifiManager.connectionInfo.ipAddress
        if (ipInt == 0) return null
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    /**
     * Scans every host (1-254) on the local /24 subnet in parallel, trying to
     * open a TCP connection on CHAT_PORT. Any host that responds is considered
     * a device running GlassChat.
     */
    suspend fun scanForDevices(
        context: Context,
        onDeviceFound: (DiscoveredDevice) -> Unit
    ) = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress(context) ?: return@withContext
        val subnet = localIp.substringBeforeLast(".")
        val myLastOctet = localIp.substringAfterLast(".")

        val jobs = (1..254).map { host ->
            async {
                if (host.toString() == myLastOctet) return@async
                val targetIp = "$subnet.$host"
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(targetIp, ChatConnection.CHAT_PORT), 200)
                        onDeviceFound(DiscoveredDevice(ip = targetIp))
                    }
                } catch (_: Exception) {
                    // host not reachable / not running GlassChat, ignore
                }
            }
        }
        jobs.awaitAll()
    }
}
