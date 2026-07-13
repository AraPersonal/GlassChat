package com.glasschat.app.auth

import android.content.Context
import android.util.Base64
import com.glasschat.app.crypto.CryptoManager
import com.glasschat.app.crypto.IdentityKeyStore
import com.glasschat.app.storage.Event
import com.glasschat.app.storage.EventStoreService
import com.glasschat.app.storage.EventType
import org.json.JSONObject
import java.util.UUID

data class AuthResult(val success: Boolean, val userId: String? = null, val username: String? = null, val message: String)

/**
 * Registration and login, entirely from inside the app — no separate bot
 * conversation flow, no server. Both operations talk to Bale only through
 * [EventStoreService] (which in turn only talks to it through
 * [com.glasschat.app.storage.BaleStorageProvider]).
 *
 * Passwords are never stored or compared as plaintext — only Argon2id
 * hashes ever leave the device (as part of the UserRegistered event).
 */
class AuthService(context: Context, private val eventStore: EventStoreService) {
    private val identity = IdentityKeyStore(context)

    suspend fun register(username: String, password: String): AuthResult {
        val trimmed = username.trim()
        if (trimmed.isEmpty() || password.isEmpty()) {
            return AuthResult(false, message = "نام کاربری و رمز عبور نمی‌توانند خالی باشند")
        }

        eventStore.sync()
        val taken = eventStore.currentEvents()
            .filter { it.type == EventType.UserRegistered }
            .any { it.metadata.optString("username").equals(trimmed, ignoreCase = true) }
        if (taken) {
            return AuthResult(false, message = "این نام کاربری قبلاً ثبت شده است")
        }

        val userId = UUID.randomUUID().toString()
        val salt = CryptoManager.randomSalt()
        val hash = CryptoManager.deriveKeyFromPassphrase(password.toCharArray(), salt)
        val publicKey = identity.publicKeyBase64() // generated & persisted on first call

        val metadata = JSONObject().apply {
            put("user_id", userId)
            put("username", trimmed)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("password_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            put("public_key", publicKey)
        }

        eventStore.append(Event(type = EventType.UserRegistered, metadata = metadata))
        return AuthResult(true, userId = userId, username = trimmed, message = "ثبت‌نام موفق")
    }

    suspend fun login(username: String, password: String): AuthResult {
        eventStore.sync()
        val trimmed = username.trim()

        val registration = eventStore.currentEvents()
            .filter { it.type == EventType.UserRegistered }
            .lastOrNull { it.metadata.optString("username").equals(trimmed, ignoreCase = true) }
            ?: return AuthResult(false, message = "کاربری با این نام کاربری پیدا نشد")

        val salt = Base64.decode(registration.metadata.getString("salt"), Base64.NO_WRAP)
        val expectedHash = Base64.decode(registration.metadata.getString("password_hash"), Base64.NO_WRAP)
        val actualHash = CryptoManager.deriveKeyFromPassphrase(password.toCharArray(), salt)

        return if (actualHash.contentEquals(expectedHash)) {
            AuthResult(
                true,
                userId = registration.metadata.getString("user_id"),
                username = registration.metadata.getString("username"),
                message = "ورود موفق"
            )
        } else {
            AuthResult(false, message = "رمز عبور اشتباه است")
        }
    }
}
