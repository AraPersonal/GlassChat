package com.glasschat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.glasschat.app.network.ChatConnection
import com.glasschat.app.network.ConnectionState
import com.glasschat.app.ui.components.GlassIconBadge
import com.glasschat.app.ui.components.GlassSurface
import com.glasschat.app.ui.theme.*

private fun sp(v: Int) = TextUnit(v.toFloat(), TextUnitType.Sp)

@Composable
fun ChatScreen(onOpenDiscovery: () -> Unit) {
    val messages by ChatConnection.messages.collectAsState()
    val state by ChatConnection.state.collectAsState()
    val peerIp by ChatConnection.peerIp.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Top bar (glass) ----
            GlassSurface(
                cornerRadius = 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(AccentBlue, AccentPurple)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = sp(18))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("گفتگوی محلی", color = TextPrimary, fontSize = sp(17), fontWeight = FontWeight.SemiBold)
                        val statusText = when (state) {
                            ConnectionState.CONNECTED -> "متصل به ${peerIp ?: ""}"
                            ConnectionState.CONNECTING -> "در حال اتصال..."
                            ConnectionState.LISTENING -> "منتظر اتصال..."
                            ConnectionState.DISCONNECTED -> "متصل نیست"
                        }
                        Text(statusText, color = TextSecondary, fontSize = sp(12))
                    }
                    GlassIconBadge(modifier = Modifier.clickable { onOpenDiscovery() }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "اتصال به گوشی دیگر", tint = AccentBluePale)
                    }
                }
            }

            // ---- Messages ----
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "هنوز پیامی نیست.\nبرای شروع، از منوی بالا به یک گوشی دیگر وصل شوید.",
                                color = TextSecondary,
                                fontSize = sp(13),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    if (msg.isMine)
                                        Brush.horizontalGradient(listOf(BubbleMine, AccentPurple.copy(alpha = 0.85f)))
                                    else
                                        Brush.verticalGradient(listOf(GlassWhiteStrong, GlassWhite)),
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (msg.isMine) 18.dp else 4.dp,
                                        bottomEnd = if (msg.isMine) 4.dp else 18.dp
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(msg.text, color = TextPrimary, fontSize = sp(15))
                        }
                    }
                }
            }

            // ---- Input bar (glass) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSurface(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 26
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("پیام...", color = TextSecondary) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            Brush.linearGradient(listOf(AccentBlue, AccentPurple)),
                            CircleShape
                        )
                        .clickable(enabled = state == ConnectionState.CONNECTED) {
                            ChatConnection.sendMessage(input.trim())
                            input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "ارسال", tint = Color.White)
                }
            }
        }
    }
}
