package com.glasschat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasschat.app.auth.AuthService
import com.glasschat.app.ui.components.GlassSurface
import com.glasschat.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authService: AuthService,
    onLoggedIn: (userId: String, username: String) -> Unit,
    onGoToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GlassChat", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("ورود به حساب کاربری", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorText = null },
                        label = { Text("نام کاربری") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBluePale,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = AccentBluePale,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = AccentBluePale
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorText = null },
                        label = { Text("رمز عبور") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBluePale,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = AccentBluePale,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = AccentBluePale
                        )
                    )

                    errorText?.let {
                     
   Spacer(Modifier.height(10.dp))
                        Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                Brush.horizontalGradient(listOf(AccentBlue, AccentPurple)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    errorText = "نام کاربری و رمز عبور را وارد کنید"
                                    return@TextButton
                                }
                                loading = true
                                scope.launch {
                                    val result = authService.login(username, password)
                                    loading = false
                                    if (result.success) {
                                        onLoggedIn(result.userId!!, result.username!!)
                                    } else {
                                        errorText = result.message
                                    }
                                }
                            }) {
                                Text("ورود", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onGoToRegister) {
                Text("حساب نداری؟ ثبت‌نام کن", color = AccentBluePale)
            }
        }
    }
}
