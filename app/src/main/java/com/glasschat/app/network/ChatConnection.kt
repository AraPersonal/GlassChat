package com.glasschat.app.network

import com.glasschat.app.data.ChatMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

enum class ConnectionState { DISCONNECTED, LISTENING, CONNECTING, CONNECTED }

/**
 * Handles a single chat session over a plain TCP socket on the local
 * hotspot/Wi-Fi network. One side calls [startListening] (waits for the
 * other phone to connect), the other side calls [connectTo] with the IP
 * discovered by [NetworkScanner].
 */
object ChatConnection {
    const val CHAT_PORT = 8988

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var writer: PrintWriter? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _peerIp = MutableStateFlow<String?>(null)
    val peerIp: StateFlow<String?> = _peerIp.asStateFlow()

    /** Wait for another GlassChat phone on the hotspot to connect to us. */
    fun startListening() {
        reset()
        _state.value = ConnectionState.LISTENING
        scope.launch {
            try {
                val server = ServerSocket(CHAT_PORT)
                serverSocket = server
                val client = server.accept()
                attachSocket(client)
            } catch (_: Exception) {
                _state.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /** Actively connect to a device found by [NetworkScanner]. */
    fun connectTo(ip: String) {
        reset()
        _state.value = ConnectionState.CONNECTING
        scope.launch {
            try {
                val client = Socket()
                client.connect(java.net.InetSocketAddress(ip, CHAT_PORT), 3000)
                attachSocket(client)
            } catch (_: Exception) {
                _state.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private fun attachSocket(client: Socket) {
        socket = client
        writer = PrintWriter(client.getOutputStream(), true)
        _peerIp.value = client.inetAddress?.hostAddress
        _state.value = ConnectionState.CONNECTED

        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                while (true) {
                    val line = reader.readLine() ?: break
                    _messages.value = _messages.value + ChatMessage(text = line, isMine = false)
                }
            } catch (_: Exception) {
                // connection dropped
            } finally {
                _state.value = ConnectionState.DISCONNECTED
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.value = _messages.value + ChatMessage(text = text, isMine = true)
        scope.launch {
            try {
                writer?.println(text)
            } catch (_: Exception) {
            }
        }
    }

    fun reset() {
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        writer = null
        socket = null
        serverSocket = null
        _state.value = ConnectionState.DISCONNECTED
        _peerIp.value = null
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
