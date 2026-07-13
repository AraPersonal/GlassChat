package com.glasschat.app.session

import android.content.Context

data class Session(val userId: String, val username: String)

/** Persists the logged-in user locally so the app doesn't ask for
 *  username/password again on every launch. Purely local — not an event. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("glasschat_session", Context.MODE_PRIVATE)

    fun save(session: Session) {
        prefs.edit()
            .putString("user_id", session.userId)
            .putString("username", session.username)
            .apply()
    }

    fun load(): Session? {
        val id = prefs.getString("user_id", null) ?: return null
        val name = prefs.getString("username", null) ?: return null
        return Session(id, name)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
