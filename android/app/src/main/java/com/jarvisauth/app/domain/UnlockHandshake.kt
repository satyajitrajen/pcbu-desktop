package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketUnlockRequest
import com.jarvisauth.app.protocol.PacketUnlockRequestData
import com.jarvisauth.app.protocol.PacketUnlockResponse
import com.jarvisauth.app.protocol.PacketUnlockResponseData
import com.jarvisauth.app.protocol.ProtocolJson
import com.jarvisauth.app.protocol.SystemEpochClock

object UnlockHandshake {
    const val PROTO_VERSION = "3.0.0"

    sealed class Parse {
        data class Prompt(
            val prompt: UnlockPrompt,
            val pairing: Pairing,
            val tsIP: String = "",
        ) : Parse()

        data class Fail(
            val error: UnlockError,
        ) : Parse()
    }

    fun parseRequest(
        payload: ByteArray,
        pairingsById: Map<String, Pairing>,
        clock: EpochClock = SystemEpochClock,
    ): Parse {
        val request =
            ProtocolJson.decode<PacketUnlockRequest>(payload.toString(Charsets.UTF_8))
                ?: return Parse.Fail(UnlockError.DataError)
        return parseRequest(payload, pairingsById[request.deviceId], clock)
    }

    fun parseRequest(
        payload: ByteArray,
        pairing: Pairing?,
        clock: EpochClock = SystemEpochClock,
    ): Parse {
        val request =
            ProtocolJson.decode<PacketUnlockRequest>(payload.toString(Charsets.UTF_8))
                ?: return Parse.Fail(UnlockError.DataError)
        if (pairing == null || request.deviceId != pairing.deviceId) {
            return Parse.Fail(UnlockError.NotPaired)
        }
        if (request.protoVersion != PROTO_VERSION) {
            return Parse.Fail(UnlockError.ProtocolError)
        }
        val blob = Hex.decode(request.encData) ?: return Parse.Fail(UnlockError.DataError)
        return when (val decrypted = AesGcmPacket.decrypt(blob, pairing.encryptionKey, clock)) {
            CryptResult.InvalidTimestamp -> Parse.Fail(UnlockError.TimeSkew)
            CryptResult.OtherError -> Parse.Fail(UnlockError.DataError)
            is CryptResult.Ok -> {
                val data =
                    ProtocolJson.decode<PacketUnlockRequestData>(
                        decrypted.data.toString(Charsets.UTF_8),
                    ) ?: return Parse.Fail(UnlockError.DataError)
                if (data.unlockToken.isBlank()) {
                    return Parse.Fail(UnlockError.DataError)
                }
                Parse.Prompt(
                    UnlockPrompt(
                        deviceId = pairing.deviceId,
                        pcName = pairing.pcName,
                        user = data.user.ifBlank { pairing.userName },
                        program = formatUnlockProgram(data.program),
                        unlockToken = data.unlockToken,
                    ),
                    pairing,
                    tsIP = request.tsIP,
                )
            }
        }
    }

    fun errorPayload(error: UnlockError): ByteArray =
        ProtocolJson.encode(
            PacketUnlockResponse(error = error.wireCode, encData = ""),
        ).toByteArray(Charsets.UTF_8)

    fun successPayload(
        unlockToken: String,
        passwordKey: String,
        encryptionKey: String,
        clock: EpochClock = SystemEpochClock,
        tsIP: String = "",
    ): ByteArray? {
        val inner =
            ProtocolJson.encode(
                PacketUnlockResponseData(
                    unlockToken = unlockToken,
                    passwordKey = passwordKey,
                ),
            ).toByteArray(Charsets.UTF_8)
        val encrypted =
            when (val result = AesGcmPacket.encrypt(inner, encryptionKey, clock)) {
                is CryptResult.Ok -> result.data
                else -> return null
            }
        return ProtocolJson.encode(
            PacketUnlockResponse(error = "", encData = Hex.encode(encrypted), tsIP = tsIP),
        ).toByteArray(Charsets.UTF_8)
    }
}
