package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.LockResult
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketLockRequest
import com.jarvisauth.app.protocol.PacketLockRequestData
import com.jarvisauth.app.protocol.PacketLockResponse
import com.jarvisauth.app.protocol.PacketLockResponseData
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.protocol.ProtocolJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TcpLockClientTest {
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
            lastIpv4 = "127.0.0.1",
        )

    @Test
    fun loopbackSuccessHasNoPasswordKey() {
        runBlocking {
            ServerSocket(0).use { server ->
                val port = server.localPort
                val ready = CountDownLatch(1)
                val job =
                    launch(Dispatchers.IO) {
                        ready.countDown()
                        val client = server.accept()
                        client.use { socket ->
                            socket.tcpNoDelay = true
                            val framed = PacketStream.read(socket.getInputStream())
                            assertEquals(PacketIds.LOCK_REQUEST, framed.id)
                            val json = framed.payload.toString(Charsets.UTF_8)
                            assertFalse(json.contains("passwordKey"))
                            val request = ProtocolJson.decode<PacketLockRequest>(json)!!
                            assertEquals("1.0.0", request.protoVersion)
                            val inner =
                                (
                                    AesGcmPacket.decrypt(
                                        Hex.decode(request.encData)!!,
                                        pairing.encryptionKey,
                                        clock,
                                    ) as CryptResult.Ok
                                ).data
                            assertFalse(inner.toString(Charsets.UTF_8).contains("passwordKey"))
                            assertFalse(inner.toString(Charsets.UTF_8).contains("\"action\""))
                            val data =
                                ProtocolJson.decode<PacketLockRequestData>(
                                    inner.toString(Charsets.UTF_8),
                                )!!
                            assertEquals("", data.action)
                            val replyInner =
                                ProtocolJson.encode(
                                    PacketLockResponseData(nonce = data.nonce),
                                ).toByteArray()
                            val blob =
                                (
                                    AesGcmPacket.encrypt(
                                        replyInner,
                                        pairing.encryptionKey,
                                        clock,
                                    ) as CryptResult.Ok
                                ).data
                            val reply =
                                ProtocolJson.encode(
                                    PacketLockResponse(error = "", encData = Hex.encode(blob)),
                                ).toByteArray()
                            assertFalse(reply.toString(Charsets.UTF_8).contains("passwordKey"))
                            PacketStream.write(
                                socket.getOutputStream(),
                                PacketIds.LOCK_RESPONSE,
                                reply,
                            )
                        }
                    }
                assertTrue(ready.await(5, TimeUnit.SECONDS))
                val result =
                    withContext(Dispatchers.IO) {
                        TcpLockClient(clock = clock, localOverlayIpv4 = { "" }).lock(
                            "127.0.0.1",
                            port,
                            pairing,
                            "",
                        )
                    }
                assertEquals(LockResult.Ok, result)
                job.cancelAndJoin()
            }
        }
    }

    @Test
    fun shutdownLoopbackSendsProto11() {
        runBlocking {
            ServerSocket(0).use { server ->
                val port = server.localPort
                val ready = CountDownLatch(1)
                val job =
                    launch(Dispatchers.IO) {
                        ready.countDown()
                        val client = server.accept()
                        client.use { socket ->
                            socket.tcpNoDelay = true
                            val framed = PacketStream.read(socket.getInputStream())
                            val request =
                                ProtocolJson.decode<PacketLockRequest>(
                                    framed.payload.toString(Charsets.UTF_8),
                                )!!
                            assertEquals("1.1.0", request.protoVersion)
                            val inner =
                                (
                                    AesGcmPacket.decrypt(
                                        Hex.decode(request.encData)!!,
                                        pairing.encryptionKey,
                                        clock,
                                    ) as CryptResult.Ok
                                ).data
                            val data =
                                ProtocolJson.decode<PacketLockRequestData>(
                                    inner.toString(Charsets.UTF_8),
                                )!!
                            assertEquals("shutdown", data.action)
                            val replyInner =
                                ProtocolJson.encode(
                                    PacketLockResponseData(nonce = data.nonce),
                                ).toByteArray()
                            val blob =
                                (
                                    AesGcmPacket.encrypt(
                                        replyInner,
                                        pairing.encryptionKey,
                                        clock,
                                    ) as CryptResult.Ok
                                ).data
                            PacketStream.write(
                                socket.getOutputStream(),
                                PacketIds.LOCK_RESPONSE,
                                ProtocolJson.encode(
                                    PacketLockResponse(error = "", encData = Hex.encode(blob)),
                                ).toByteArray(),
                            )
                        }
                    }
                assertTrue(ready.await(5, TimeUnit.SECONDS))
                val result =
                    withContext(Dispatchers.IO) {
                        TcpLockClient(clock = clock, localOverlayIpv4 = { "" }).lock(
                            "127.0.0.1",
                            port,
                            pairing,
                            "shutdown",
                        )
                    }
                assertEquals(LockResult.Ok, result)
                job.cancelAndJoin()
            }
        }
    }
}
