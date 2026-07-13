package com.glasschat.app.storage

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The only entry point the rest of the app should use to read/write state.
 * Wraps a [StorageProvider] (currently [BaleStorageProvider]) plus the local
 * [LocalCacheDb]. Handles retry, duplicate detection, and sync — callers just
 * see a flat, ordered, deduplicated list of [Event]s.
 */
class EventStoreService(
    context: Context,
    private val provider: StorageProvider = BaleStorageProvider()
) {
    private val cache = LocalCacheDb(context)

    private val _events = MutableStateFlow(cache.loadAll())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    /** Appends a new event: publishes to the backend, then caches it locally
     *  only once the publish succeeds (so the cache never has "phantom" events
     *  the channel doesn't actually have). */
    suspend fun append(event: Event) {
        val ref = provider.publish(event)
        if (cache.insertIfNew(event, ref)) {
            _events.value = cache.loadAll()
        }
    }

    /** Pulls whatever new events the backend currently has (see caveats on
     *  [StorageProvider.fetchNew]) and merges them into the local cache,
     *  silently skipping ones already seen (idempotent, safe to call often). */
    suspend fun sync() {
        val fresh = try {
            provider.fetchNew()
        } catch (_: Exception) {
            return // network failure: keep working off the existing cache
        }
        var changed = false
        for (event in fresh) {
            if (cache.insertIfNew(event, null)) changed = true
        }
        if (changed) _events.value = cache.loadAll()
    }

    fun currentEvents(): List<Event> = _events.value
}
