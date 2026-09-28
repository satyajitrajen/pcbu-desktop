package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketUnlockRequest
import com.jarvisauth.app.protocol.PacketUnlockRequestData
import com.jarvisauth.app.protocol.PacketUnlockResponse
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.protocol.ProtocolJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockHandshakeTest {
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
        )

    @Test
    fun unknownDeviceIdIsNotPaired() {
        val payload = requestPayload("other-id", "3.0.0", "tok")
        val parsed = UnlockHandshake.parseRequest(payload, pairing, clock)
        assertEquals(UnlockError.NotPaired, (parsed as UnlockHandshake.Parse.Fail).error)
    }

    @Test
    fun twoPcsMatchOnlyRequestedDeviceId() {
        val other =
            pairing.copy(
                deviceId = "pc-2",
                encryptionKey = "other-enc",
                passwordKey = "other-pw",
                pcName = "LAPTOP",
                pairingMethod = PairingMethod.TCP,
            )
        val pairings = mapOf(pairing.deviceId to pairing, other.deviceId to other)
        val parsed =
            UnlockHandshake.parseRequest(
                requestPayload("pc-2", "3.0.0", "tok-2", other.encryptionKey),
                pairings,
                clock,
            ) as UnlockHandshake.Parse.Prompt
        assertEquals("pc-2", parsed.prompt.deviceId)
        assertEquals("LAPTOP", parsed.prompt.pcName)
        assertEquals(other, parsed.pairing)
    }

    @Test
    fun unknownDeviceIdAmongPairingsIsNotPaired() {
        val parsed =
            UnlockHandshake.parseRequest(
                requestPayload("pc-missing", "3.0.0", "tok"),
                mapOf(pairing.deviceId to pairing),
                clock,
            )
        assertEquals(UnlockError.NotPaired, (parsed as UnlockHandshake.Parse.Fail).error)
    }

    @Test
    fun protoMismatchIsProtocolError() {
        val payload = requestPayload("pc-1", "2.0.0", "tok")
        val parsed = UnlockHandshake.parseRequest(payload, pairing, clock)
        assertEquals(UnlockError.ProtocolError, (parsed as UnlockHandshake.Parse.Fail).error)
    }

    @Test
    fun successEchoesTokenAndCancelHasNoPasswordKey() {
        val payload = requestPayload("pc-1", "3.0.0", "token-abc")
        val parsed = UnlockHandshake.parseRequest(payload, pairing, clock) as UnlockHandshake.Parse.Prompt
        assertEquals("token-abc", parsed.prompt.unlockToken)
        assertEquals("Windows login", parsed.prompt.program)
        assertEquals("", parsed.tsIP)

        val withTs =
            UnlockHandshake.parseRequest(
                requestPayload("pc-1", "3.0.0", "token-abc", tsIP = "100.64.9.1"),
                pairing,
                clock,
            ) as UnlockHandshake.Parse.Prompt
        assertEquals("100.64.9.1", withTs.tsIP)

        val success = UnlockHandshake.successPayload("token-abc", pairing.passwordKey, pairing.encryptionKey, clock)!!
        val successJson = ProtocolJson.decode<PacketUnlockResponse>(success.toString(Charsets.UTF_8))!!
        assertEquals("", successJson.error)
        val inner =
            AesGcmPacket.decrypt(Hex.decode(successJson.encData)!!, pairing.encryptionKey, clock) as CryptResult.Ok
        assertTrue(inner.data.toString(Charsets.UTF_8).contains("token-abc"))
        assertTrue(inner.data.toString(Charsets.UTF_8).contains("pw-key"))

        assertEquals("", successJson.tsIP)

        val successWithTs =
            UnlockHandshake.successPayload(
                "token-abc",
                pairing.passwordKey,
                pairing.encryptionKey,
                clock,
                tsIP = "100.64.1.2",
            )!!
        val successWithTsJson =
            ProtocolJson.decode<PacketUnlockResponse>(successWithTs.toString(Charsets.UTF_8))!!
        assertEquals("100.64.1.2", successWithTsJson.tsIP)

        val cancel = UnlockHandshake.errorPayload(UnlockError.Cancel)
        val cancelJson = ProtocolJson.decode<PacketUnlockResponse>(cancel.toString(Charsets.UTF_8))!!
        assertEquals("CANCEL", cancelJson.error)
        assertEquals("", cancelJson.encData)
        assertFalse(cancel.toString(Charsets.UTF_8).contains("pw-key"))
    }

    private fun requestPayload(
        deviceId: String,
        proto: String,
        token: String,
        encryptionKey: String = pairing.encryptionKey,
        tsIP: String = "",
    ): ByteArray {
        val inner =
            ProtocolJson.encode(
                PacketUnlockRequestData(user = "satya", program = "Windows-Login", unlockToken = token),
            ).toByteArray()
        val blob = (AesGcmPacket.encrypt(inner, encryptionKey, clock) as CryptResult.Ok).data
        return ProtocolJson.encode(
            PacketUnlockRequest(
                protoVersion = proto,
                deviceId = deviceId,
                encData = Hex.encode(blob),
                tsIP = tsIP,
            ),
        ).toByteArray()
    }
}
