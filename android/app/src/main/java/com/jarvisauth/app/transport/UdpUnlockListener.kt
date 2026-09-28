package com.jarvisauth.app.transport

import android.content.Context
import android.net.wifi.WifiManager
import com.jarvisauth.app.protocol.PacketUdpBroadcast
import com.jarvisauth.app.protocol.ProtocolJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

class UdpUnlockListener(
    context: Context,
) {
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    suspend fun listen(
        ports: Set<Int>,
        onBroadcast: suspend (PacketUdpBroadcast) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val lock =
            wifi?.createMulticastLock("jarvisauth-unlock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        try {
            coroutineScope {
                ports.forEach { port ->
                    launch { bindPort(port, onBroadcast) }
                }
            }
        } finally {
            if (lock?.isHeld == true) lock.release()
        }
    }

    private suspend fun bindPort(
        port: Int,
        onBroadcast: suspend (PacketUdpBroadcast) -> Unit,
    ) {
        DatagramSocket(null).use { socket ->
            socket.reuseAddress = true
            socket.bind(allIpv4Interfaces(port))
            socket.soTimeout = 1_000
            val buf = ByteArray(4096)
            while (currentCoroutineContext().isActive) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val broadcast = ProtocolJson.decode<PacketUdpBroadcast>(text) ?: continue
                if (broadcast.deviceId.isBlank() || broadcast.pcbuIP.isBlank() ||
                    broadcast.pcbuPort !in 1..65535
                ) {
                    continue
                }
                onBroadcast(broadcast)
            }
        }
    }
}
