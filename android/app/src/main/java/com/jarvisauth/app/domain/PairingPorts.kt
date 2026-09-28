package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.PacketPairInit
import com.jarvisauth.app.protocol.PacketPairResponse
import kotlinx.coroutines.flow.Flow

data class DiscoveredHost(
    val ip: String,
    val port: Int,
)

sealed class PairResult {
    data class Ok(
        val pairing: Pairing,
    ) : PairResult()

    data class Err(
        val error: PairError,
    ) : PairResult()
}

interface PairingStore {
    fun observe(): Flow<List<Pairing>>

    suspend fun reload()

    suspend fun snapshot(): PairingsSnapshot

    suspend fun ensureLocalDevice(displayName: String): LocalDevice

    suspend fun upsert(pairing: Pairing)

    suspend fun remove(deviceId: String)

    suspend fun rememberIpv4(
        deviceId: String,
        ip: String,
    )
}

fun interface PairingDiscovery {
    suspend fun find(
        serverId: String,
        discoveryPort: Int,
        encKey: String,
    ): DiscoveredHost?
}

fun interface PairingClient {
    suspend fun pair(
        host: String,
        port: Int,
        encKey: String,
        init: PacketPairInit,
    ): PacketPairResponse
}
