package com.jarvisauth.app.transport

import com.jarvisauth.app.domain.PairDecryptException
import com.jarvisauth.app.domain.PairTimeSkewException
import com.jarvisauth.app.domain.PairingClient
import com.jarvisauth.app.domain.cryptFailure
import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.PacketIds
import com.jarvisauth.app.protocol.PacketPairInit
import com.jarvisauth.app.protocol.PacketPairResponse
import com.jarvisauth.app.protocol.PacketStream
import com.jarvisauth.app.protocol.ProtocolJson
import com.jarvisauth.app.protocol.SystemEpochClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

class TcpPairClient(
    private val clock: EpochClock = SystemEpochClock,
    private val connectTimeoutMs: Int = 8_000,
    private val soTimeoutMs: Int = 20_000,
) : PairingClient {
    override suspend fun pair(
        host: String,
        port: Int,
        encKey: String,
        init: PacketPairInit,
    ): PacketPairResponse =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                socket.soTimeout = soTimeoutMs
                val localIp =
                    (socket.localAddress as? Inet4Address)?.hostAddress
                        ?: socket.localAddress.hostAddress?.substringBefore('%')
                        ?: "0.0.0.0"
                val toSend = if (init.ipAddress.isBlank()) init.copy(ipAddress = localIp) else init
                val plain = ProtocolJson.encode(toSend).toByteArray(Charsets.UTF_8)
                val encrypted =
                    when (val result = AesGcmPacket.encrypt(plain, encKey, clock)) {
                        is CryptResult.Ok -> result.data
                        else -> throw cryptFailure(result)
                    }
                PacketStream.write(socket.getOutputStream(), PacketIds.PAIR_INIT, encrypted)
                val framed = PacketStream.read(socket.getInputStream())
                if (framed.id != PacketIds.PAIR_RESPONSE) {
                    throw PairDecryptException()
                }
                val decrypted =
                    when (val result = AesGcmPacket.decrypt(framed.payload, encKey, clock)) {
                        is CryptResult.Ok -> result.data
                        CryptResult.InvalidTimestamp -> throw PairTimeSkewException()
                        CryptResult.OtherError -> throw PairDecryptException()
                    }
                ProtocolJson.decode<PacketPairResponse>(decrypted.toString(Charsets.UTF_8))
                    ?: throw PairDecryptException()
            }
        }
}
