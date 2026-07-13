package com.glasschat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasschat.app.services.ConversationService
import com.glasschat.app.services.UserInfo
import com.glasschat.app.services.UserService
import com.glasschat.app.ui.components.GlassIconBadge
import com.glasschat.app.ui.components.GlassSurface
import com.glasschat.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    myUserId: String,
    myUsername: String,
    userService: UserService,
    conversationService: ConversationService,
    onOpenChat: (conversation: com.glasschat.app.services.ConversationInfo, peerUsername: String) -> Unit,
    onLogout: () -> Unit
) {
    var contacts by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        loading = true
        contacts = userService.listUsers().filter { it.userId != myUserId }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

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
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(myUsername.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("مخاطبین", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(myUsername, color = TextSecondary, fontSize = 12.sp)
                    }
                    GlassIconBadge(modifier = Modifier.clickable { onLogout() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "خروج", tint = TextPrimary)
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBluePale)
                }
            } else if (contacts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "هنوز کاربر دیگری ثبت‌نام نکرده",
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    
verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(contacts) { contact ->
                        GlassSurface(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val conv = conversationService.findOrCreate(myUserId, contact.userId)
                                            onOpenChat(conv, contact.username)
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(contact.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(contact.username, color = TextPrimary, fontSize = 16.sp)
                                    Text(
                                        if (contact.publicKeyBase64 != null) "برای چت ضربه بزن" else "هنوز کلید رمزنگاری منتشر نشده",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
