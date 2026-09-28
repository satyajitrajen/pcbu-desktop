package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockPrompt
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TcpUnlockServerTest {
    private val clock = EpochClock { 1_700_000_000_000L }
    private val pc1 =
        Pairing(
            deviceId = "pc-1",
            encryptionKey = "enc-1",
            passwordKey = "pw-1",
            pairingMethod = PairingMethod.TCP,
            pcName = "WORKSTATION",
            pcOs = "Windows",
            userName = "satya",
            unlockServerPort = 43296,
            pairedAtEpochMs = 1L,
        )
    private val pc2 =
        pc1.copy(
            deviceId = "pc-2",
            encryptionKey = "enc-2",
            passwordKey = "pw-2",
            pcName = "LAPTOP",
        )
    private val pairings = mapOf(pc1.deviceId to pc1, pc2.deviceId to pc2)

    @Test
    fun pcConnectsAndGetsSuccessWithoutDeviceIdPacket() {
        runServer { port, job ->
            Socket("127.0.0.1", port).use { socket ->
                socket.tcpNoDelay = true
                writeRequest(socket, pc1, "tok-tcp")
                val resp = PacketStream.read(socket.getInputStream())
                assertEquals(PacketIds.UNLOCK_RESPONSE, resp.id)
                val parsed =
                    ProtocolJson.decode<PacketUnlockResponse>(resp.payload.toString(Charsets.UTF_8))!!
                assertEquals("", parsed.error)
                val inner =
                    (
                        AesGcmPacket.decrypt(
                            Hex.decode(parsed.encData)!!,
                            pc1.encryptionKey,
                            clock,
                        ) as CryptResult.Ok
                    ).data
                val data =
                    ProtocolJson.decode<PacketUnlockResponseData>(inner.toString(Charsets.UTF_8))!!
                assertEquals("tok-tcp", data.unlockToken)
                assertEquals("pw-1", data.passwordKey)
            }
            job.cancelAndJoin()
        }
    }

    @Test
    fun unknownDeviceIdIsNotPaired() {
        runServer { port, job ->
            Socket("127.0.0.1", port).use { socket ->
                socket.tcpNoDelay = true
                writeRequest(socket, pc1.copy(deviceId = "pc-missing"), "tok")
                val resp = PacketStream.read(socket.getInputStream())
                val parsed =
                    ProtocolJson.decode<PacketUnlockResponse>(resp.payload.toString(Charsets.UTF_8))!!
                assertEquals("NOT_PAIRED", parsed.error)
                assertEquals("", parsed.encData)
            }
            job.cancelAndJoin()
        }
    }

    @Test
    fun twoPcsUseMatchingPasswordKey() {
        runServer(decide = { UnlockDecision.Confirm }) { port, job ->
            Socket("127.0.0.1", port).use { socket ->
                socket.tcpNoDelay = true
                writeRequest(socket, pc2, "tok-2")
                val resp = PacketStream.read(socket.getInputStream())
                val parsed =
                    ProtocolJson.decode<PacketUnlockResponse>(resp.payload.toString(Charsets.UTF_8))!!
                val inner =
                    (
                        AesGcmPacket.decrypt(
                            Hex.decode(parsed.encData)!!,
                            pc2.encryptionKey,
                            clock,
                        ) as CryptResult.Ok
                    ).data
                val data =
                    ProtocolJson.decode<PacketUnlockResponseData>(inner.toString(Charsets.UTF_8))!!
                assertEquals("pw-2", data.passwordKey)
                assertEquals("tok-2", data.unlockToken)
            }
            job.cancelAndJoin()
        }
    }

    @Test
    fun cancelDoesNotSendPasswordKey() {
        runServer(decide = { UnlockDecision.Cancel }) { port, job ->
            Socket("127.0.0.1", port).use { socket ->
                socket.tcpNoDelay = true
                writeRequest(socket, pc1, "tok-cancel")
                val resp = PacketStream.read(socket.getInputStream())
                val parsed =
                    ProtocolJson.decode<PacketUnlockResponse>(resp.payload.toString(Charsets.UTF_8))!!
                assertEquals("CANCEL", parsed.error)
                assertEquals("", parsed.encData)
                assertTrue(!parsed.encData.contains("pw-1"))
            }
            job.cancelAndJoin()
        }
    }

    private fun writeRequest(
        socket: Socket,
        pairing: Pairing,
        token: String,
    ) {
        val inner =
            ProtocolJson.encode(
                PacketUnlockRequestData("satya", "Windows-Login", token),
            ).toByteArray()
        val blob = (AesGcmPacket.encrypt(inner, pairing.encryptionKey, clock) as CryptResult.Ok).data
        PacketStream.write(
            socket.getOutputStream(),
            PacketIds.UNLOCK_REQUEST,
            ProtocolJson.encode(
                PacketUnlockRequest("3.0.0", pairing.deviceId, Hex.encode(blob)),
            ).toByteArray(),
        )
    }

    private fun runServer(
        decide: (UnlockPrompt) -> UnlockDecision = { UnlockDecision.Confirm },
        block: suspend (Int, Job) -> Unit,
    ) {
        runBlocking {
            val bound = CountDownLatch(1)
            val port = AtomicInteger(0)
            val job =
                launch(Dispatchers.IO) {
                    TcpUnlockServer(clock = clock).listen(
                        port = 0,
                        pairingsById = { pairings },
                        onListening = {
                            port.set(it)
                            bound.countDown()
                        },
                    ) { decide(it) }
                }
            assertTrue(bound.await(5, TimeUnit.SECONDS))
            withContext(Dispatchers.IO) { block(port.get(), job) }
        }
    }
}
