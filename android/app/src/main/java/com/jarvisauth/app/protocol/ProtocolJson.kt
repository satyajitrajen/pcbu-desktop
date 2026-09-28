package com.jarvisauth.app.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProtocolJson {
    val format: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    inline fun <reified T> encode(value: T): String = format.encodeToString(value)

    inline fun <reified T> decode(text: String): T? =
        try {
            format.decodeFromString(text)
        } catch (_: Exception) {
            null
        }
}
