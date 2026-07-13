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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasschat.app.services.ChatMessage
import com.glasschat.app.services.ConversationInfo
import com.glasschat.app.services.ConversationService
import com.glasschat.app.services.MessageService
import com.glasschat.app.storage.EventStoreService
import com.glasschat.app.ui.components.GlassIconBadge
import com.glasschat.app.ui.components.GlassSurface
import com.glasschat.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    conversation: ConversationInfo,
    peerUsername: String,
    myUserId: String,
    eventStore: EventStoreService,
    conversationService: ConversationService,
    messageService: MessageService,
    onBack: () -> Unit
) {
    val events by eventStore.events.collectAsState()
    var conversationKey by remember { mutableStateOf<ByteArray?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Derive the AES key once (needs the peer's public key to already be known).
    LaunchedEffect(conversation.conversationId) {
        conversationKey = conversationService.deriveConversationKey(conversation, myUserId)
    }

    // Re-decrypt/refresh history whenever the underlying event cache changes
    // (SyncService merges new events from the channel in the background).
    LaunchedEffect(events, conversationKey) {
        val key = conversationKey ?: return@LaunchedEffect
        messages = messageService.history(conversation.conversationId, myUserId, key)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(Modifier.fillMaxSize()) {
            GlassSurface(cornerRadius = 0, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconBadge(modifier = Modifier.clickable { onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(peerUsername.take(1).uppercase(), color = Color.White, fontWeight = FontWe
ight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(peerUsername, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (conversationKey != null) "رمزگذاری‌شده سرتاسری" else "در حال آماده‌سازی رمزنگاری...",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Text("هنوز پیامی نیست. اولین پیام رو بفرست!", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                items(messages, key = { it.messageId }) { msg ->
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
                                        topStart = 18.dp, topEnd = 18.dp,
                                        bottomStart = if (msg.isMine) 18.dp else 4.dp,
                                        bottomEnd = if (msg.isMine) 4.dp else 18.dp
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(msg.text, color = TextPrimary, fontSize = 15.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSurface(modifier = Modifier.weight(1f), cornerRadius = 26) {
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)), CircleShape)
                        .clickable(enabled = conversationKey != null &&
 !sending && input.isNotBlank()) {
                            val key = conversationKey ?: return@clickable
                            val text = input.trim()
                            val peerId = conversation.participantIds.first { it != myUserId }
                            input = ""
                            sending = true
                            scope.launch {
                                messageService.send(conversation.conversationId, myUserId, peerId, text, key)
                                messages = messageService.history(conversation.conversationId, myUserId, key)
                                sending = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "ارسال", tint = Color.White)
                }
            }
        }
    }
}
