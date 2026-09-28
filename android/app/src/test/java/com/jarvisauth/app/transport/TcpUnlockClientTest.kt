package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.PacketUnlockRequest
import com.jarvisauth.app.protocol.PacketUnlockRequestData
import com.jarvisauth.app.protocol.PacketUnlockResponse
import com.jarvisauth.app.protocol.PacketUnlockResponseData
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.protocol.ProtocolJson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class TcpUnlockClientTest {
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
    fun udpClientSendsDeviceIdThenSuccess() {
        val server = ServerSocket(0)
        val port = server.localPort
        val done = CountDownLatch(1)
        val seenDeviceId = AtomicReference("")
        val responseError = AtomicReference("unset")
        val responseToken = AtomicReference("")
        Executors.newSingleThreadExecutor().execute {
            try {
                server.use { listener ->
                    listener.accept().use { socket ->
                        val idPacket = PacketStream.read(socket.getInputStream())
                        seenDeviceId.set(idPacket.payload.toString(Charsets.UTF_8))
                        val inner =
                            ProtocolJson.encode(
                                PacketUnlockRequestData("satya", "Windows-Login", "tok-1"),
                            ).toByteArray()
                        val blob =
                            (AesGcmPacket.encrypt(inner, pairing.encryptionKey, clock) as CryptResult.Ok).data
                        PacketStream.write(
                            socket.getOutputStream(),
                            PacketIds.UNLOCK_REQUEST,
                            ProtocolJson.encode(
                                PacketUnlockRequest("3.0.0", pairing.deviceId, Hex.encode(blob)),
                            ).toByteArray(),
                        )
                        val resp = PacketStream.read(socket.getInputStream())
                        val parsed =
                            ProtocolJson.decode<PacketUnlockResponse>(resp.payload.toString(Charsets.UTF_8))!!
                        responseError.set(parsed.error)
                        val data =
                            (
                                AesGcmPacket.decrypt(
                                    Hex.decode(parsed.encData)!!,
                                    pairing.encryptionKey,
                                    clock,
                                ) as CryptResult.Ok
                            ).data
                        val innerResp =
                            ProtocolJson.decode<PacketUnlockResponseData>(data.toString(Charsets.UTF_8))!!
                        responseToken.set(innerResp.unlockToken)
                    }
                }
            } finally {
                done.countDown()
            }
        }
        runBlocking {
            TcpUnlockClient(clock = clock, localOverlayIpv4 = { "" }).run("127.0.0.1", port, pairing) { UnlockDecision.Confirm }
        }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertEquals("pc-1", seenDeviceId.get())
        assertEquals("", responseError.get())
        assertEquals("tok-1", responseToken.get())
    }

    @Test
    fun cancelDoesNotSendPasswordKey() {
        val server = ServerSocket(0)
        val port = server.localPort
        val done = CountDownLatch(1)
        val encData = AtomicReference("unset")
        val error = AtomicReference("")
        Executors.newSingleThreadExecutor().execute {
            try {
                server.use { listener ->
                    listener.accept().use { socket ->
                        PacketStream.read(socket.getInputStream())
                        val inner =
                            ProtocolJson.encode(
                                PacketUnlockRequestData("satya", "sudo", "tok-2"),
                            ).toByteArray()
                        val blob =
                            (AesGcmPacket.encrypt(inner, pairing.encryptionKey, clock) as CryptResult.Ok).data
                        PacketStream.write(
                            socket.getOutputStream(),
                            PacketIds.UNLOCK_REQUEST,
                            ProtocolJson.encode(
                                PacketUnlockRequest("3.0.0", pairing.deviceId, Hex.encode(blob)),
                            ).toByteArray(),
                        )
                        val resp = PacketStream.read(socket.getInputStream())
                        val parsed =
                            ProtocolJson.decode<PacketUnlockResponse>(resp.payload.toString(Charsets.UTF_8))!!
                        error.set(parsed.error)
                        encData.set(parsed.encData)
                    }
                }
            } finally {
                done.countDown()
            }
        }
        runBlocking {
            TcpUnlockClient(clock = clock, localOverlayIpv4 = { "" }).run("127.0.0.1", port, pairing) { UnlockDecision.Cancel }
        }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertEquals("CANCEL", error.get())
        assertEquals("", encData.get())
    }
}
