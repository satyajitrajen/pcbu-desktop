package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.OverlayIpv4
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockError
import com.jarvisauth.app.domain.UnlockHandshake
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.SystemEpochClock
import java.io.InputStream
import java.io.OutputStream

object UnlockInboundSession {
    suspend fun handle(
        input: InputStream,
        output: OutputStream,
        pairingsById: Map<String, Pairing>,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
        clock: EpochClock = SystemEpochClock,
        stillOpen: () -> Boolean = { true },
        onMatched: suspend (deviceId: String, tsIP: String) -> Unit = { _, _ -> },
    ) {
        // Desktop TCPUnlockClient / BTUnlockClient already know the pairing and send 0xB1
        // immediately (BaseUnlockConnection::PerformAuthFlow needsDeviceID=false). Do not send 0xB0.
        val framed = PacketStream.read(input)
        if (framed.id != PacketIds.UNLOCK_REQUEST) {
            writeError(output, UnlockError.DataError)
            return
        }
        when (val parsed = UnlockHandshake.parseRequest(framed.payload, pairingsById, clock)) {
            is UnlockHandshake.Parse.Fail -> writeError(output, parsed.error)
            is UnlockHandshake.Parse.Prompt -> {
                onMatched(parsed.pairing.deviceId, parsed.tsIP)
                val decision = decide(parsed.prompt)
                if (!stillOpen()) return
                when (decision) {
                    UnlockDecision.Cancel -> writeError(output, UnlockError.Cancel)
                    UnlockDecision.Confirm -> {
                        val payload =
                            UnlockHandshake.successPayload(
                                unlockToken = parsed.prompt.unlockToken,
                                passwordKey = parsed.pairing.passwordKey,
                                encryptionKey = parsed.pairing.encryptionKey,
                                clock = clock,
                                tsIP = OverlayIpv4.local().orEmpty(),
                            )
                        if (payload == null) {
                            writeError(output, UnlockError.AppError)
                        } else {
                            PacketStream.write(output, PacketIds.UNLOCK_RESPONSE, payload)
                        }
                    }
                }
            }
        }
    }

    private fun writeError(
        output: OutputStream,
        error: UnlockError,
    ) {
        PacketStream.write(output, PacketIds.UNLOCK_RESPONSE, UnlockHandshake.errorPayload(error))
    }
}
