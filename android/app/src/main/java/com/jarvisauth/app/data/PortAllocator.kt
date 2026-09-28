package com.jarvisauth.app.data

import java.net.DatagramSocket
import java.net.ServerSocket

object PortAllocator {
    const val DEFAULT_TCP = 43310
    const val DEFAULT_UDP = 43311
    const val DEFAULT_UDP_MANUAL = 43312

    fun firstFreeTcp(start: Int = DEFAULT_TCP): Int = firstFree(start, udp = false)

    fun firstFreeUdp(start: Int): Int = firstFree(start, udp = true)

    private fun firstFree(
        start: Int,
        udp: Boolean,
    ): Int {
        repeat(20) { i ->
            val port = start + i
            val free =
                runCatching {
                    if (udp) {
                        DatagramSocket(port).close()
                    } else {
                        ServerSocket(port).close()
                    }
                    true
                }.getOrDefault(false)
            if (free) return port
        }
        return start
    }
}
