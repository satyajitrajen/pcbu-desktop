package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketLockRequest
import com.jarvisauth.app.protocol.PacketLockRequestData
import com.jarvisauth.app.protocol.PacketLockResponse
import com.jarvisauth.app.protocol.PacketLockResponseData
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.protocol.ProtocolJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockHandshakeTest {
    private val clock = EpochClock { 1_700_000_000_000L }
    private val pairing =
        Pairing(
            deviceId = "pc-1",
            encryptionKey = "enc-key",
            passwordKey = "pw-key",
            pairingMethod = PairingMethod.UDP,
            pcName = "WORKSTATION",
            pcOs = "Windows",
            userName = "satya",
            unlockServerPort = 43296,
            pairedAtEpochMs = 1L,
            lastIpv4 = "10.0.0.8",
        )

    @Test
    fun requestHasNoPasswordKey() {
        val payload = LockHandshake.requestPayload(pairing, "nonce-1", clock)!!
        val json = payload.toString(Charsets.UTF_8)
        assertFalse(json.contains("pw-key"))
        assertFalse(json.contains("passwordKey"))
        val request = ProtocolJson.decode<PacketLockRequest>(json)!!
        assertEquals("1.0.0", request.protoVersion)
        assertEquals("pc-1", request.deviceId)
        assertEquals("", request.tsIP)
        val withTs = LockHandshake.requestPayload(pairing, "nonce-1", clock, tsIP = "100.64.1.2")!!
        assertEquals(
            "100.64.1.2",
            ProtocolJson.decode<PacketLockRequest>(withTs.toString(Charsets.UTF_8))!!.tsIP,
        )
        val inner =
            (
                AesGcmPacket.decrypt(Hex.decode(request.encData)!!, pairing.encryptionKey, clock)
                    as CryptResult.Ok
            ).data
        val data = ProtocolJson.decode<PacketLockRequestData>(inner.toString(Charsets.UTF_8))!!
        assertEquals("pc-1", data.pcDeviceId)
        assertEquals("nonce-1", data.nonce)
        assertEquals("", data.action)
        assertFalse(inner.toString(Charsets.UTF_8).contains("\"action\""))
        assertFalse(inner.toString(Charsets.UTF_8).contains("passwordKey"))
    }

    @Test
    fun powerRequestUsesProto11AndInnerAction() {
        val payload =
            LockHandshake.requestPayload(
                pairing,
                "nonce-1",
                clock,
                action = "shutdown",
            )!!
        val json = payload.toString(Charsets.UTF_8)
        assertFalse(json.contains("passwordKey"))
        assertFalse(json.contains("\"action\""))
        val request = ProtocolJson.decode<PacketLockRequest>(json)!!
        assertEquals("1.1.0", request.protoVersion)
        val inner =
            (
                AesGcmPacket.decrypt(Hex.decode(request.encData)!!, pairing.encryptionKey, clock)
                    as CryptResult.Ok
            ).data
        val text = inner.toString(Charsets.UTF_8)
        assertFalse(text.contains("passwordKey"))
        val data = ProtocolJson.decode<PacketLockRequestData>(text)!!
        assertEquals("shutdown", data.action)
        assertTrue(text.contains("\"action\""))
    }

    @Test
    fun successEchoesNonceAndHasNoPasswordKey() {
        val inner =
            ProtocolJson.encode(PacketLockResponseData(nonce = "nonce-1")).toByteArray()
        val blob = (AesGcmPacket.encrypt(inner, pairing.encryptionKey, clock) as CryptResult.Ok).data
        val payload =
            ProtocolJson.encode(
                PacketLockResponse(error = "", encData = Hex.encode(blob)),
            ).toByteArray()
        assertFalse(payload.toString(Charsets.UTF_8).contains("passwordKey"))
        assertEquals(LockResult.Ok, LockHandshake.parseResponse(payload, pairing, "nonce-1", clock))
    }

    @Test
    fun protoMismatchWireIsProtocolError() {
        val payload =
            ProtocolJson.encode(
                PacketLockResponse(error = "PROTOCOL_ERROR", encData = ""),
            ).toByteArray()
        val parsed = LockHandshake.parseResponse(payload, pairing, "n", clock) as LockResult.Err
        assertEquals(LockError.ProtocolError, parsed.error)
    }

    @Test
    fun successBodyWithPasswordKeyIsRejected() {
        val inner = """{"nonce":"nonce-1","passwordKey":"pw-key"}""".toByteArray()
        val blob = (AesGcmPacket.encrypt(inner, pairing.encryptionKey, clock) as CryptResult.Ok).data
        val payload =
            ProtocolJson.encode(
                PacketLockResponse(error = "", encData = Hex.encode(blob)),
            ).toByteArray()
        val parsed = LockHandshake.parseResponse(payload, pairing, "nonce-1", clock) as LockResult.Err
        assertEquals(LockError.AppError, parsed.error)
    }
}
