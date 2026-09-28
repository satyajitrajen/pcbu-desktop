package com.jarvisauth.app.domain

import java.net.Inet4Address
import java.net.InetAddress

object Ipv4 {
    private val DOTTED =
        Regex(
            """^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$""",
        )

    fun parse(raw: String): String? {
        val host = raw.substringBefore('%').removePrefix("::ffff:").removePrefix("::FFFF:")
        return if (DOTTED.matches(host)) host else null
    }

    fun from(address: InetAddress): String? {
        if (address is Inet4Address) {
            return parse(address.hostAddress.orEmpty())
        }
        return parse(address.hostAddress.orEmpty())
    }
}
