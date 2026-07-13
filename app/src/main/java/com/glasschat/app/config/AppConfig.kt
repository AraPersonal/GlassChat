package com.glasschat.app.config

/**
 * Single source of truth for every secret/endpoint the app needs.
 * No other file in the project may hardcode a token, chat id, or base URL —
 * everything routes through this object.
 *
 * Fill these in yourself; they are intentionally left blank.
 */
object AppConfig {
    /** Bot token from @Bot_Father on Bale. Format: "123456789:abcdEF...". */
    const val BALE_BOT_TOKEN: String = ""

    /** Numeric id of the private Bale channel used as the event store. */
    const val BALE_CHANNEL_ID: String = ""

    /** Base URL for the Bale Bot API. */
    const val BALE_API_BASE_URL: String = "https://tapi.bale.ai/bot"
}
