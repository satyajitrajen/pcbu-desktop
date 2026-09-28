package com.jarvisauth.app.protocol

object PairingQrParser {
    fun parse(raw: String): PairingQrData? {
        val json = extractJson(raw) ?: return null
        val data = ProtocolJson.decode<PairingQrData>(json) ?: return null
        if (data.encKey.isBlank()) return null
        if (data.port !in 1..65535) return null
        if (data.serverId.isBlank() && data.ip.isBlank()) return null
        return data
    }

    fun isCloudMethod(method: String): Boolean =
        method == "CLOUD_TCP" || method == "CLOUD_BT"

    private fun extractJson(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{")) return trimmed
        val decoded = Base32.decode(trimmed) ?: return null
        return decoded.toString(Charsets.UTF_8).trim().takeIf { it.startsWith("{") }
    }
}
