package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.LockClient
import com.jarvisauth.app.domain.LockError
import com.jarvisauth.app.domain.LockHandshake
import com.jarvisauth.app.domain.LockResult
import com.jarvisauth.app.domain.OverlayIpv4
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.SystemEpochClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class TcpLockClient(
    private val clock: EpochClock = SystemEpochClock,
    private val connectTimeoutMs: Int = 8_000,
    private val soTimeoutMs: Int = 8_000,
    private val localOverlayIpv4: () -> String = { OverlayIpv4.local().orEmpty() },
) : LockClient {
    override suspend fun lock(
        host: String,
        port: Int,
        pairing: Pairing,
        action: String,
    ): LockResult =
        withContext(Dispatchers.IO) {
            val nonce = LockHandshake.newNonce()
            val request =
                LockHandshake.requestPayload(pairing, nonce, clock, localOverlayIpv4(), action)
                    ?: return@withContext LockResult.Err(LockError.AppError)
            try {
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                    socket.soTimeout = soTimeoutMs
                    PacketStream.write(socket.getOutputStream(), PacketIds.LOCK_REQUEST, request)
                    val framed = PacketStream.read(socket.getInputStream())
                    if (framed.id != PacketIds.LOCK_RESPONSE) {
                        return@use LockResult.Err(LockError.DataError)
                    }
                    LockHandshake.parseResponse(framed.payload, pairing, nonce, clock)
                }
            } catch (_: Exception) {
                LockResult.Err(LockError.Unreachable)
            }
        }
}
