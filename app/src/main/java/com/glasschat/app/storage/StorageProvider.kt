package com.glasschat.app.storage

import org.json.JSONObject
import java.util.UUID

/**
 * One immutable fact about the system. Never updated or deleted — only new
 * events are appended. Current state is rebuilt by replaying events in order.
 *
 * `encryptedPayload` holds message-level plaintext (base64 ciphertext) when
 * relevant (e.g. MessageSent). `metadata` is never encrypted: conversation id,
 * sender, receiver, timestamp, event type — needed to route/replay events
 * without decrypting everything.
 */
data class Event(
    val uuid: String = UUID.randomUUID().toString(),
    val version: Int = 1,
    val type: EventType,
    val createdAt: Long = System.currentTimeMillis(),
    val metadata: JSONObject,
    val encryptedPayload: String? = null
) {
    fun serialize(): String = JSONObject().apply {
        put("uuid", uuid)
        put("version", version)
        put("type", type.name)
        put("created_at", createdAt)
        put("metadata", metadata)
        if (encryptedPayload != null) put("encrypted_payload", encryptedPayload)
    }.toString()

    companion object {
        fun deserialize(json: String): Event {
            val o = JSONObject(json)
            return Event(
                uuid = o.getString("uuid"),
                version = o.getInt("version"),
                type = EventType.valueOf(o.getString("type")),
                createdAt = o.getLong("created_at"),
                metadata = o.getJSONObject("metadata"),
                encryptedPayload = if (o.has("encrypted_payload")) o.getString("encrypted_payload") else null
            )
        }
    }
}

enum class EventType {
    UserRegistered,
    UsernameChanged,
    ConversationCreated,
    MessageSent,
    MessageDeleted,
    DeviceRegistered
}

/**
 * Storage abstraction. No part of the app besides an implementation of this
 * interface may know how/where events are actually persisted. Swappable with
 * FirebaseStorageProvider, PostgreSQLStorageProvider, LocalStorageProvider,
 * MockStorageProvider, etc. without touching any other layer.
 */
interface StorageProvider {
    /** Appends one event to the store. Returns the provider-assigned reference
     *  (e.g. Bale message_id) used later for local indexing/retry. */
    suspend fun publish(event: Event): String

    /** Returns every event currently retrievable from the backend, oldest first.
     *  NOTE: depending on the provider, this may not be the *complete* history
     *  (see BaleStorageProvider docs) — always combine with the local cache. */
    suspend fun fetchNew(): List<Event>
}
