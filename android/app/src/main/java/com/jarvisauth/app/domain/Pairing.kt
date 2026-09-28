package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.PairingMethod
import kotlinx.serialization.Serializable

@Serializable
data class Pairing(
    val deviceId: String,
    val encryptionKey: String,
    val passwordKey: String,
    val pairingMethod: PairingMethod,
    val pcName: String,
    val pcOs: String,
    val userName: String,
    val unlockServerPort: Int,
    val macAddresses: List<String> = emptyList(),
    val pairedAtEpochMs: Long,
    val lastIpv4: String = "",
    val lastTailscaleIpv4: String = "",
) {
    override fun toString(): String =
        "Pairing(deviceId=${deviceId.take(8)}, pcName=$pcName, method=$pairingMethod)"
}

@Serializable
data class LocalDevice(
    val uuid: String,
    val displayName: String,
    val tcpPort: Int,
    val udpPort: Int,
    val udpManualPort: Int,
)

@Serializable
data class PairingsSnapshot(
    val localDevice: LocalDevice? = null,
    val pairings: List<Pairing> = emptyList(),
)
