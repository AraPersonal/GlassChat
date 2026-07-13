package com.glasschat.app.auth

import android.util.Base64
import com.glasschat.app.crypto.CryptoManager
import com.glasschat.app.storage.Event
import com.glasschat.app.storage.EventStoreService
import com.glasschat.app.storage.EventType
import org.json.JSONObject

data class AuthResult(val success: Boolean, val userId: String? = null, val message: String)

/**
 * Verifies a Username/Password login against the replayed UserRegistered
 * events. Registration itself happens inside the Bale bot conversation (per
 * spec), not in this app — the bot is expected to publish a UserRegistered
 * event with `metadata.username`, `metadata.salt` (base64) and
 * `metadata.password_hash` (base64, Argon2id output) once signup completes.
 *
 * Passwords are NEVER stored or compared as plaintext, only as Argon2id
 * hashes, matching the spec.
 */
class AuthService(private val eventStore: EventStoreService) {

    suspend fun login(username: String, password: String): AuthResult {
        eventStore.sync()

        // Replay: find the most recent UserRegistered event for this username.
        val registration = eventStore.currentEvents()
            .filter { it.type == EventType.UserRegistered }
            .lastOrNull { it.metadata.optString("username") == username }
            ?: return AuthResult(false, message = "کاربری با این نام کاربری پیدا نشد")

        val salt = Base64.decode(registration.metadata.getString("salt"), Base64.NO_WRAP)
        val expectedHash = Base64.decode(registration.metadata.getString("password_hash"), Base64.NO_WRAP)

        val actualHash = CryptoManager.deriveKeyFromPassphrase(password.toCharArray(), salt)

        return if (actualHash.contentEquals(expectedHash)) {
            AuthResult(true, userId = registration.metadata.getString("user_id"), message = "ورود موفق")
        } else {
            AuthResult(false, message = "رمز عبور اشتباه است")
        }
    }

    /** Helper for the bot-side registration flow (or an admin tool) to build
     *  the event that should be published — kept here so the hashing logic
     *  lives in exactly one place. */
    fun buildRegistrationEvent(userId: String, username: String, password: String): Event {
        val salt = CryptoManager.randomSalt()
        val hash = CryptoManager.deriveKeyFromPassphrase(password.toCharArray(), salt)
        val metadata = JSONObject().apply {
            put("user_id", userId)
            put("username", username)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("password_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
        }
        return Event(type = EventType.UserRegistered, metadata = metadata)
    }
}
