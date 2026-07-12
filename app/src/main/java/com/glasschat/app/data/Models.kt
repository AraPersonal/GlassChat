package com.glasschat.app.data

data class ChatMessage(
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiscoveredDevice(
    val ip: String,
    val name: String = ip
)
