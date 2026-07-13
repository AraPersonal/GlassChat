package com.glasschat.app.storage

import com.glasschat.app.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * StorageProvider backed by a private Bale channel, using the Bale Bot API
 * (documented at https://docs.bale.ai — HTTP, Telegram-Bot-API-compatible).
 *
 * Each [Event] is serialized to JSON and sent as one text message via
 * sendMessage. [fetchNew] polls getUpdates for messages the bot has received
 * since the last call — see the class-level caveat in StorageProvider.kt about
 * update retention; this is a live feed, not guaranteed full history.
 *
 * This is the ONLY class in the project that talks to the Bale API directly.
 */
class BaleStorageProvider : StorageProvider {

    private val base get() = "${AppConfig.BALE_API_BASE_URL}${AppConfig.BALE_BOT_TOKEN}"
    private var lastUpdateId: Long = 0

    override suspend fun publish(event: Event): String = withContext(Dispatchers.IO) {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < 3) {
            try {
                val body = JSONObject().apply {
                    put("chat_id", AppConfig.BALE_CHANNEL_ID)
                    put("text", event.serialize())
                }
                val response = post("$base/sendMessage", body)
                return@withContext response
                    .getJSONObject("result")
                    .getInt("message_id")
                    .toString()
            } catch (e: Exception) {
                lastError = e
                attempt++
                if (attempt < 3) delay(300L * attempt)
            }
        }
        throw lastError ?: IllegalStateException("publish failed")
    }

    override suspend fun fetchNew(): List<Event> = withContext(Dispatchers.IO) {
        val url = "$base/getUpdates?offset=${lastUpdateId + 1}&timeout=0"
        val response = get(url)
        val results = response.getJSONArray("result")
        val events = mutableListOf<Event>()
        for (i in 0 until results.length()) {
            val update = results.getJSONObject(i)
            lastUpdateId = maxOf(lastUpdateId, update.getLong("update_id"))
            // Channel posts arrive as "channel_post" (Telegram-style); fall back to "message".
            val messageObj = when {
                update.has("channel_post") -> update.getJSONObject("channel_post")
                update.has("message") -> update.getJSONObject("message")
                else -> null
            } ?: continue
            val text = messageObj.optString("text", null) ?: continue
            runCatching { events.add(Event.deserialize(text)) }
        }
        events
    }

    private fun post(urlStr: String, body: JSONObject): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw java.io.IOException("Bale API error $code: $text")
        return JSONObject(text)
    }

    private fun get(urlStr: String): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw java.io.IOException("Bale API error $code: $text")
        return JSONObject(text)
    }
}
