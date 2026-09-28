package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.Ipv4
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.SystemEpochClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class TcpUnlockServer(
    private val clock: EpochClock = SystemEpochClock,
    private val soTimeoutMs: Int = 125_000,
) {
    suspend fun listen(
        port: Int,
        pairingsById: suspend () -> Map<String, Pairing>,
        onListening: (Int) -> Unit = {},
        onPeer: suspend (deviceId: String, ipv4: String) -> Unit = { _, _ -> },
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ) = withContext(Dispatchers.IO) {
        ServerSocket().use { server ->
            server.reuseAddress = true
            server.soTimeout = 1_000
            server.bind(allIpv4Interfaces(port))
            onListening(server.localPort)
            coroutineScope {
                while (currentCoroutineContext().isActive) {
                    val client =
                        try {
                            server.accept()
                        } catch (_: SocketTimeoutException) {
                            continue
                        } catch (_: SocketException) {
                            if (!currentCoroutineContext().isActive) break else continue
                        }
                    launch {
                        try {
                            handleClient(client, pairingsById, onPeer, decide)
                        } catch (_: Exception) {
                            // Closed or truncated session; keep accepting.
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleClient(
        socket: Socket,
        pairingsById: suspend () -> Map<String, Pairing>,
        onPeer: suspend (deviceId: String, ipv4: String) -> Unit,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ) {
        socket.use {
            it.tcpNoDelay = true
            it.soTimeout = soTimeoutMs
            val ipv4 = Ipv4.from(it.inetAddress)
            UnlockInboundSession.handle(
                input = it.getInputStream(),
                output = it.getOutputStream(),
                pairingsById = pairingsById(),
                decide = decide,
                clock = clock,
                stillOpen = { !it.isClosed && it.isConnected },
                onMatched = { deviceId, tsIP ->
                    if (ipv4 != null) onPeer(deviceId, ipv4)
                    if (tsIP.isNotBlank()) onPeer(deviceId, tsIP)
                },
            )
        }
    }
}
