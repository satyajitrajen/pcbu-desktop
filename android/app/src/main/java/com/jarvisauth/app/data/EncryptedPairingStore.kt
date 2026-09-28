package com.jarvisauth.app.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.jarvisauth.app.domain.Ipv4
import com.jarvisauth.app.domain.LocalDevice
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.PairingStore
import com.jarvisauth.app.domain.PairingsSnapshot
import com.jarvisauth.app.domain.rememberAddress
import com.jarvisauth.app.protocol.ProtocolJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class EncryptedPairingStore(
    context: Context,
) : PairingStore {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "pairings.json")
    private val mutex = Mutex()
    private val snapshot = MutableStateFlow(PairingsSnapshot())
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    override fun observe(): Flow<List<Pairing>> = snapshot.map { it.pairings }

    override suspend fun reload() {
        mutex.withLock {
            snapshot.value = readUnlocked()
        }
    }

    override suspend fun snapshot(): PairingsSnapshot = mutex.withLock { snapshot.value }

    override suspend fun ensureLocalDevice(displayName: String): LocalDevice =
        mutex.withLock {
            val current = snapshot.value.localDevice
            if (current != null) {
                if (current.displayName != displayName) {
                    val updated = current.copy(displayName = displayName)
                    writeUnlocked(snapshot.value.copy(localDevice = updated))
                    return@withLock updated
                }
                return@withLock current
            }
            val created =
                LocalDevice(
                    uuid = UUID.randomUUID().toString(),
                    displayName = displayName,
                    tcpPort = PortAllocator.firstFreeTcp(),
                    udpPort = PortAllocator.firstFreeUdp(PortAllocator.DEFAULT_UDP),
                    udpManualPort = PortAllocator.firstFreeUdp(PortAllocator.DEFAULT_UDP_MANUAL),
                )
            writeUnlocked(snapshot.value.copy(localDevice = created))
            created
        }

    override suspend fun upsert(pairing: Pairing) {
        mutex.withLock {
            val rest = snapshot.value.pairings.filterNot { it.deviceId == pairing.deviceId }
            writeUnlocked(snapshot.value.copy(pairings = rest + pairing))
        }
    }

    override suspend fun remove(deviceId: String) {
        mutex.withLock {
            val rest = snapshot.value.pairings.filterNot { it.deviceId == deviceId }
            writeUnlocked(snapshot.value.copy(pairings = rest))
        }
    }

    override suspend fun rememberIpv4(
        deviceId: String,
        ip: String,
    ) {
        val parsed = Ipv4.parse(ip) ?: return
        mutex.withLock {
            val current = snapshot.value.pairings.find { it.deviceId == deviceId } ?: return@withLock
            val next = current.rememberAddress(parsed) ?: return@withLock
            if (next === current || next == current) return@withLock
            val rest = snapshot.value.pairings.filterNot { it.deviceId == deviceId }
            writeUnlocked(snapshot.value.copy(pairings = rest + next))
        }
    }

    private fun encryptedFile(): EncryptedFile =
        EncryptedFile.Builder(
            appContext,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()

    private fun readUnlocked(): PairingsSnapshot {
        if (!file.exists()) return PairingsSnapshot()
        return runCatching {
            encryptedFile().openFileInput().use { input ->
                ProtocolJson.decode<PairingsSnapshot>(input.readBytes().toString(Charsets.UTF_8))
                    ?: PairingsSnapshot()
            }
        }.getOrDefault(PairingsSnapshot())
    }

    private fun writeUnlocked(next: PairingsSnapshot) {
        if (file.exists()) {
            file.delete()
        }
        encryptedFile().openFileOutput().use { output ->
            output.write(ProtocolJson.encode(next).toByteArray(Charsets.UTF_8))
        }
        snapshot.value = next
    }
}
