package com.jarvisauth.app.transport

import android.content.Context
import android.net.wifi.WifiManager
import com.jarvisauth.app.domain.DiscoveredHost
import com.jarvisauth.app.domain.PairingDiscovery
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.PairingBeacon
import com.jarvisauth.app.protocol.SystemEpochClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

class UdpPairingDiscovery(
    context: Context? = null,
    private val clock: EpochClock = SystemEpochClock,
    private val timeoutMs: Long = 25_000,
) : PairingDiscovery {
    private val wifi = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    override suspend fun find(
        serverId: String,
        discoveryPort: Int,
        encKey: String,
    ): DiscoveredHost? =
        withContext(Dispatchers.IO) {
            val lock =
                wifi?.createMulticastLock("jarvisauth-pair")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            try {
                DatagramSocket(null).use { socket ->
                    socket.reuseAddress = true
                    socket.bind(InetSocketAddress(discoveryPort))
                    socket.soTimeout = 1_000
                    val buf = ByteArray(4096)
                    val deadline = System.currentTimeMillis() + timeoutMs
                    while (System.currentTimeMillis() < deadline) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            socket.receive(packet)
                        } catch (_: SocketTimeoutException) {
                            continue
                        }
                        val blob = packet.data.copyOf(packet.length)
                        val beacon = PairingBeacon.decrypt(blob, encKey, clock) ?: continue
                        if (beacon.serverId == serverId &&
                            beacon.ip.isNotBlank() &&
                            beacon.port in 1..65535
                        ) {
                            return@withContext DiscoveredHost(beacon.ip, beacon.port)
                        }
                    }
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                if (lock?.isHeld == true) lock.release()
            }
        }
}
