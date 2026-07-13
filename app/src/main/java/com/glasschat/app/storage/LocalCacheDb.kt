package com.glasschat.app.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Pure cache of events already seen from [StorageProvider]. Deleting this
 * database must never lose data permanently — on next launch,
 * [EventStoreService] re-fetches from the channel and rebuilds it. It also
 * stores each event's provider reference (e.g. Bale message_id) so we keep a
 * durable index even if getUpdates history has since expired upstream.
 */
class LocalCacheDb(context: Context) : SQLiteOpenHelper(context, "glasschat_cache.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE events (
                uuid TEXT PRIMARY KEY,
                version INTEGER NOT NULL,
                type TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                metadata TEXT NOT NULL,
                encrypted_payload TEXT,
                provider_ref TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema evolution goes here in future versions; for now, no-op.
    }

    /** Returns false if this event uuid is already cached (duplicate detection). */
    fun insertIfNew(event: Event, providerRef: String?): Boolean {
        val db = writableDatabase
        val cursor = db.query("events", arrayOf("uuid"), "uuid = ?", arrayOf(event.uuid), null, null, null)
        val exists = cursor.moveToFirst()
        cursor.close()
        if (exists) return false

        val values = ContentValues().apply {
            put("uuid", event.uuid)
            put("version", event.version)
            put("type", event.type.name)
            put("created_at", event.createdAt)
            put("metadata", event.metadata.toString())
            put("encrypted_payload", event.encryptedPayload)
            put("provider_ref", providerRef)
        }
        db.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return true
    }

    fun loadAll(): List<Event> {
        val db = readableDatabase
        val cursor = db.query("events", null, null, null, null, null, "created_at ASC")
        val list = mutableListOf<Event>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Event(
                        uuid = it.getString(it.getColumnIndexOrThrow("uuid")),
                        version = it.getInt(it.getColumnIndexOrThrow("version")),
                        type = EventType.valueOf(it.getString(it.getColumnIndexOrThrow("type"))),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        metadata = org.json.JSONObject(it.getString(it.getColumnIndexOrThrow("metadata"))),
                        encryptedPayload = it.getString(it.getColumnIndexOrThrow("encrypted_payload"))
                    )
                )
            }
        }
        return list
    }
}
