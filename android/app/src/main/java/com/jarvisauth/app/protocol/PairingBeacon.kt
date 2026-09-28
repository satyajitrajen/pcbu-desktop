package com.jarvisauth.app.protocol

object PairingBeacon {
    fun decrypt(
        blob: ByteArray,
        encKey: String,
        clock: EpochClock = SystemEpochClock,
    ): PacketUdpPairBeacon? {
        val plain =
            when (val result = AesGcmPacket.decrypt(blob, encKey, clock)) {
                is CryptResult.Ok -> result.data
                else -> return null
            }
        return ProtocolJson.decode<PacketUdpPairBeacon>(plain.toString(Charsets.UTF_8))
    }
}
