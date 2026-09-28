package com.jarvisauth.app.transport

import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketPairInit
import com.jarvisauth.app.protocol.PacketPairResponse
import com.jarvisauth.app.protocol.PacketPairResponseData
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.ProtocolJson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.Executors

class TcpPairClientTest {
    private val encKey = "tcp-pair-enc-key"
    private val clock = EpochClock { 1_700_000_000_000L }

    @Test
    fun handshakeRoundTripAgainstLocalServer() {
        val server = ServerSocket(0)
        val port = server.localPort
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            server.use { listener ->
                listener.accept().use { socket ->
                    val framed = PacketStream.read(socket.getInputStream())
                    val plain =
                        (AesGcmPacket.decrypt(framed.payload, encKey, clock) as CryptResult.Ok).data
                    val init = ProtocolJson.decode<PacketPairInit>(plain.toString(Charsets.UTF_8))!!
                    assertEquals("4.0.0", init.protoVersion)
                    val response =
                        PacketPairResponse(
                            data =
                                PacketPairResponseData(
                                    pairingMethod = "TCP",
                                    deviceId = "dev-1",
                                    deviceName = "PC",
                                    passwordKey = "pw",
                                ),
                        )
                    val blob =
                        (
                            AesGcmPacket.encrypt(
                                ProtocolJson.encode(response).toByteArray(),
                                encKey,
                                clock,
                            ) as CryptResult.Ok
                        ).data
                    PacketStream.write(socket.getOutputStream(), PacketIds.PAIR_RESPONSE, blob)
                }
            }
        }
        val result =
            runBlocking {
                TcpPairClient(clock = clock).pair(
                    host = "127.0.0.1",
                    port = port,
                    encKey = encKey,
                    init =
                        PacketPairInit(
                            deviceUUID = "uuid",
                            deviceName = "Pixel",
                            tcpPort = 43310,
                            udpPort = 43311,
                            udpManualPort = 43312,
                        ),
                )
            }
        executor.shutdown()
        assertEquals("dev-1", result.data.deviceId)
        assertEquals("pw", result.data.passwordKey)
        assertEquals("TCP", result.data.pairingMethod)
    }
}
