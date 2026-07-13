package com.glasschat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.glasschat.app.auth.AuthService
import com.glasschat.app.services.ConversationInfo
import com.glasschat.app.services.ConversationService
import com.glasschat.app.services.MessageService
import com.glasschat.app.services.SyncService
import com.glasschat.app.services.UserService
import com.glasschat.app.session.Session
import com.glasschat.app.session.SessionStore
import com.glasschat.app.storage.EventStoreService
import com.glasschat.app.ui.screens.ChatScreen
import com.glasschat.app.ui.screens.ContactsScreen
import com.glasschat.app.ui.screens.LoginScreen
import com.glasschat.app.ui.screens.RegisterScreen
import com.glasschat.app.ui.theme.GlassChatTheme

private sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Contacts : Screen()
    data class Chat(val conversation: ConversationInfo, val peerUsername: String) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventStore = EventStoreService(applicationContext)
        val authService = AuthService(applicationContext, eventStore)
        val userService = UserService(eventStore)
        val conversationService = ConversationService(applicationContext, eventStore, userService)
        val messageService = MessageService(eventStore)
        val syncService = SyncService(eventStore)
        val sessionStore = SessionStore(applicationContext)

        setContent {
            GlassChatTheme {
                val existingSession = remember { sessionStore.load() }
                var screen by remember {
                    mutableStateOf<Screen>(if (existingSession != null) Screen.Contacts else Screen.Login)
                }
                var myUserId by remember { mutableStateOf(existingSession?.userId ?: "") }
                var myUsername by remember { mutableStateOf(existingSession?.username ?: "") }

                LaunchedEffect(Unit) {
                    syncService.start()
                }

                when (val s = screen) {
                    is Screen.Login -> LoginScreen(
                        authService = authService,
                        onLoggedIn = { userId, username ->
                            sessionStore.save(Session(userId, username))
                            myUserId = userId
                            myUsername = username
                            screen = Screen.Contacts
                        },
                        onGoToRegister = { screen = Screen.Register }
                    )

                    is Screen.Register -> RegisterScreen(
                        authService = authService,
                        onRegistered = { userId, username ->
                            sessionStore.save(Session(userId, username))
                            myUserId = userId
                            myUsername = username
                            screen = Screen.Contacts
                        },
                        onGoToLogin = { screen = Screen.Login }
                    )

                    is Screen.Contacts -> ContactsScreen(
                        myUserId = myUserId,
                        myUsername = myUsername,
                        userService = userService,
                        conversationService = conversationService,
                        onOpenChat = { conversation, peerUsername ->
                            screen = Screen.Chat(conversation, peerUsername)
                        },
                        onLogout = {
                            sessionStore.clear()
                            screen = Screen.Login
                        }
                    )

                    is Screen.Chat -> ChatScreen(
                        conversation = s.conversation,
                        peerUsername = s.peerUsername,
                        myUserId = myUserId,
                        eventStore = eventStore,
                        conversationService = conversationService,
                        messageService = messageService,
                        onBack = { screen = Screen.Contacts }
                    )
                }
            }
        }
    }
}
