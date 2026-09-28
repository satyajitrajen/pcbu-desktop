package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.Ipv4
import com.jarvisauth.app.domain.OverlayIpv4
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockDialResult
import com.jarvisauth.app.domain.UnlockDialer
import com.jarvisauth.app.domain.UnlockError
import com.jarvisauth.app.domain.UnlockHandshake
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.SystemEpochClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class TcpUnlockClient(
    private val clock: EpochClock = SystemEpochClock,
    private val connectTimeoutMs: Int = 8_000,
    private val soTimeoutMs: Int = 125_000,
    private val localOverlayIpv4: () -> String = { OverlayIpv4.local().orEmpty() },
) : UnlockDialer {
    suspend fun run(
        host: String,
        port: Int,
        pairing: Pairing,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ): UnlockDialResult = run(host, port, pairing, { _ -> }, decide)

    override suspend fun run(
        host: String,
        port: Int,
        pairing: Pairing,
        onLearnIp: suspend (String) -> Unit,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ): UnlockDialResult =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    try {
                        socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                    } catch (_: Exception) {
                        return@withContext UnlockDialResult.Unreachable
                    }
                    socket.soTimeout = soTimeoutMs
                    PacketStream.write(
                        socket.getOutputStream(),
                        PacketIds.DEVICE_ID,
                        pairing.deviceId.toByteArray(Charsets.UTF_8),
                    )
                    val framed =
                        try {
                            PacketStream.read(socket.getInputStream())
                        } catch (_: Exception) {
                            return@withContext UnlockDialResult.Unreachable
                        }
                    if (framed.id != PacketIds.UNLOCK_REQUEST) {
                        writeError(socket, UnlockError.DataError)
                        return@use UnlockDialResult.ProtocolFail
                    }
                    when (val parsed = UnlockHandshake.parseRequest(framed.payload, pairing, clock)) {
                        is UnlockHandshake.Parse.Fail -> {
                            writeError(socket, parsed.error)
                            UnlockDialResult.ProtocolFail
                        }
                        is UnlockHandshake.Parse.Prompt -> {
                            if (parsed.tsIP.isNotBlank()) onLearnIp(parsed.tsIP)
                            Ipv4.from(socket.inetAddress)?.let { onLearnIp(it) }
                            val decision = decide(parsed.prompt)
                            if (socket.isClosed || !socket.isConnected) {
                                return@use UnlockDialResult.Finished(decision)
                            }
                            when (decision) {
                                UnlockDecision.Cancel -> writeError(socket, UnlockError.Cancel)
                                UnlockDecision.Confirm -> {
                                    val payload =
                                        UnlockHandshake.successPayload(
                                            unlockToken = parsed.prompt.unlockToken,
                                            passwordKey = pairing.passwordKey,
                                            encryptionKey = pairing.encryptionKey,
                                            clock = clock,
                                            tsIP = localOverlayIpv4(),
                                        )
                                    if (payload == null) {
                                        writeError(socket, UnlockError.AppError)
                                    } else {
                                        PacketStream.write(
                                            socket.getOutputStream(),
                                            PacketIds.UNLOCK_RESPONSE,
                                            payload,
                                        )
                                    }
                                }
                            }
                            UnlockDialResult.Finished(decision)
                        }
                    }
                }
            } catch (_: Exception) {
                UnlockDialResult.Unreachable
            }
        }

    private fun writeError(
        socket: Socket,
        error: UnlockError,
    ) {
        if (socket.isClosed) return
        PacketStream.write(
            socket.getOutputStream(),
            PacketIds.UNLOCK_RESPONSE,
            UnlockHandshake.errorPayload(error),
        )
    }
}
