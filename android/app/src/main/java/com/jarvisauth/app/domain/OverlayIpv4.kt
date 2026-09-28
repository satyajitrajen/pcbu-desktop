package com.jarvisauth.app.domain

import java.net.NetworkInterface

/** Tailscale.com CGNAT range (RFC 6598 / 100.64.0.0/10). Not RFC1918 LAN. */
object OverlayIpv4 {
    fun isOverlay(ip: String): Boolean {
        val parsed = Ipv4.parse(ip) ?: return false
        val parts = parsed.split('.')
        if (parts.size != 4) return false
        val a = parts[0].toIntOrNull() ?: return false
        val b = parts[1].toIntOrNull() ?: return false
        return a == 100 && b in 64..127
    }

    fun isLan(ip: String): Boolean {
        val parsed = Ipv4.parse(ip) ?: return false
        if (isOverlay(parsed)) return false
        val parts = parsed.split('.')
        if (parts.size != 4) return false
        val a = parts[0].toIntOrNull() ?: return false
        val b = parts[1].toIntOrNull() ?: return false
        return a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31)
    }

    fun local(addresses: Sequence<String> = localIpv4s()): String? =
        addresses.mapNotNull { Ipv4.parse(it) }.firstOrNull { isOverlay(it) }

    fun localLan(addresses: Sequence<String> = localIpv4s()): String? =
        addresses.mapNotNull { Ipv4.parse(it) }.firstOrNull { isLan(it) }

    fun localIpv4s(): Sequence<String> =
        sequence {
            val en =
                runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull() ?: return@sequence
            for (nif in en) {
                val usable =
                    runCatching { nif.isUp && !nif.isLoopback }.getOrDefault(false)
                if (!usable) continue
                for (addr in nif.inetAddresses) {
                    Ipv4.from(addr)?.let { yield(it) }
                }
            }
        }
}

fun Pairing.rememberAddress(ip: String): Pairing? {
    val parsed = Ipv4.parse(ip) ?: return null
    return if (OverlayIpv4.isOverlay(parsed)) {
        if (lastTailscaleIpv4 == parsed) this else copy(lastTailscaleIpv4 = parsed)
    } else {
        if (lastIpv4 == parsed) this else copy(lastIpv4 = parsed)
    }
}
