package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketLockRequest
import com.jarvisauth.app.protocol.PacketLockRequestData
import com.jarvisauth.app.protocol.PacketLockResponse
import com.jarvisauth.app.protocol.PacketLockResponseData
import com.jarvisauth.app.protocol.ProtocolJson
import com.jarvisauth.app.protocol.SystemEpochClock
import java.security.SecureRandom

object LockHandshake {
    const val PROTO_VERSION = "1.0.0"
    const val POWER_PROTO_VERSION = "1.1.0"
    const val PORT = 43298

    fun newNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Hex.encode(bytes)
    }

    fun requestPayload(
        pairing: Pairing,
        nonce: String,
        clock: EpochClock = SystemEpochClock,
        tsIP: String = "",
        action: String = "",
    ): ByteArray? {
        val powerAction = if (action == "shutdown" || action == "restart") action else ""
        val inner =
            ProtocolJson.encode(
                PacketLockRequestData(
                    pcDeviceId = pairing.deviceId,
                    nonce = nonce,
                    action = powerAction,
                ),
            ).toByteArray(Charsets.UTF_8)
        val encrypted =
            when (val result = AesGcmPacket.encrypt(inner, pairing.encryptionKey, clock)) {
                is CryptResult.Ok -> result.data
                else -> return null
            }
        return ProtocolJson.encode(
            PacketLockRequest(
                protoVersion = if (powerAction.isEmpty()) PROTO_VERSION else POWER_PROTO_VERSION,
                deviceId = pairing.deviceId,
                encData = Hex.encode(encrypted),
                tsIP = tsIP,
            ),
        ).toByteArray(Charsets.UTF_8)
    }

    fun parseResponse(
        payload: ByteArray,
        pairing: Pairing,
        nonce: String,
        clock: EpochClock = SystemEpochClock,
    ): LockResult {
        val json = payload.toString(Charsets.UTF_8)
        if (json.contains("passwordKey", ignoreCase = true)) {
            return LockResult.Err(LockError.AppError)
        }
        val response =
            ProtocolJson.decode<PacketLockResponse>(json)
                ?: return LockResult.Err(LockError.DataError)
        if (response.error.isNotBlank()) {
            return LockResult.Err(LockError.fromWire(response.error))
        }
        val blob = Hex.decode(response.encData) ?: return LockResult.Err(LockError.DataError)
        return when (val decrypted = AesGcmPacket.decrypt(blob, pairing.encryptionKey, clock)) {
            CryptResult.InvalidTimestamp -> LockResult.Err(LockError.TimeSkew)
            CryptResult.OtherError -> LockResult.Err(LockError.DataError)
            is CryptResult.Ok -> {
                val text = decrypted.data.toString(Charsets.UTF_8)
                if (text.contains("passwordKey", ignoreCase = true)) {
                    return LockResult.Err(LockError.AppError)
                }
                val data =
                    ProtocolJson.decode<PacketLockResponseData>(text)
                        ?: return LockResult.Err(LockError.DataError)
                if (data.nonce != nonce) {
                    LockResult.Err(LockError.DataError)
                } else {
                    LockResult.Ok
                }
            }
        }
    }
}
