package com.jarvisauth.app.protocol

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

enum class PairingMethod {
    TCP,
    BLUETOOTH,
    UDP,
    MANUAL_UDP,
    ;

    companion object {
        fun fromWire(value: String): PairingMethod? =
            entries.find { it.name == value }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PacketPairInit(
    val protoVersion: String = "4.0.0",
    val deviceUUID: String = "",
    val deviceName: String = "",
    val ipAddress: String = "",
    val tcpPort: Int = 0,
    val udpPort: Int = 0,
    val udpManualPort: Int = 0,
    val cloudToken: String = "",
    // Optional Tailscale overlay IPv4. Omitted when empty so old peers see unchanged JSON. Do not JSON-wrap 0xB0.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tsIP: String = "",
)

@Serializable
data class PacketPairResponseData(
    val pairingMethod: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceOS: String = "",
    val unlockServerPort: Int = 0,
    val macAddresses: List<String> = emptyList(),
    val userName: String = "",
    val passwordKey: String = "",
)

@Serializable
data class PacketPairResponse(
    val errMsg: String = "",
    val data: PacketPairResponseData = PacketPairResponseData(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PacketUnlockRequest(
    val protoVersion: String = "3.0.0",
    val deviceId: String = "",
    val encData: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tsIP: String = "",
)

@Serializable
data class PacketUnlockRequestData(
    val user: String = "",
    val program: String = "",
    val unlockToken: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PacketUnlockResponse(
    val error: String = "",
    val encData: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tsIP: String = "",
)

@Serializable
data class PacketUnlockResponseData(
    val unlockToken: String = "",
    val passwordKey: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PacketUdpBroadcast(
    val deviceId: String = "",
    val pcbuIP: String = "",
    val pcbuPort: Int = 0,
    val isManual: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tsIP: String = "",
)

@Serializable
data class PacketUdpPairBeacon(
    val serverId: String = "",
    val ip: String = "",
    val port: Int = 0,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PacketLockRequest(
    val protoVersion: String = "1.0.0",
    val deviceId: String = "",
    val encData: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tsIP: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PacketLockRequestData(
    val pcDeviceId: String = "",
    val nonce: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val action: String = "",
)

@Serializable
data class PacketLockResponse(
    val error: String = "",
    val encData: String = "",
)

@Serializable
data class PacketLockResponseData(
    val nonce: String = "",
)

@Serializable
data class PairingQrData(
    val serverId: String = "",
    val ip: String = "",
    val port: Int = 0,
    val method: String = "",
    val encKey: String = "",
)
