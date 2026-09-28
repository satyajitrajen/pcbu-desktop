package com.jarvisauth.app.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.SystemEpochClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

sealed class BluetoothListenError {
    data object Unavailable : BluetoothListenError()

    data object Disabled : BluetoothListenError()

    data object Permission : BluetoothListenError()

    data object Failed : BluetoothListenError()

    val userMessage: String
        get() =
            when (this) {
                Unavailable -> "This phone has no Bluetooth."
                Disabled -> "Turn Bluetooth on."
                Permission -> "Allow Bluetooth to listen for this PC."
                Failed -> "Bluetooth listen failed."
            }
}

class RfcommUnlockServer(
    context: Context,
    private val clock: EpochClock = SystemEpochClock,
) {
    private val adapter: BluetoothAdapter? =
        context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter

    @SuppressLint("MissingPermission")
    suspend fun listen(
        pairingsById: suspend () -> Map<String, Pairing>,
        onPeer: suspend (deviceId: String, ipv4: String) -> Unit = { _, _ -> },
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ): BluetoothListenError? =
        withContext(Dispatchers.IO) {
            val bluetooth = adapter ?: return@withContext BluetoothListenError.Unavailable
            if (!bluetooth.isEnabled) return@withContext BluetoothListenError.Disabled
            val server =
                try {
                    bluetooth.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                } catch (_: SecurityException) {
                    return@withContext BluetoothListenError.Permission
                } catch (_: IOException) {
                    return@withContext BluetoothListenError.Failed
                }
            try {
                acceptLoop(server, pairingsById, onPeer, decide)
                null
            } catch (_: BluetoothListenFailed) {
                BluetoothListenError.Failed
            } finally {
                try {
                    server.close()
                } catch (_: IOException) {
                }
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun acceptLoop(
        server: BluetoothServerSocket,
        pairingsById: suspend () -> Map<String, Pairing>,
        onPeer: suspend (deviceId: String, ipv4: String) -> Unit,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ) {
        coroutineScope {
            val closer =
                launch {
                    try {
                        awaitCancellation()
                    } finally {
                        try {
                            server.close()
                        } catch (_: IOException) {
                        }
                    }
                }
            while (currentCoroutineContext().isActive) {
                val client =
                    try {
                        server.accept()
                    } catch (_: IOException) {
                        if (!currentCoroutineContext().isActive) break
                        throw BluetoothListenFailed()
                    }
                launch {
                    try {
                        handleClient(client, pairingsById, onPeer, decide)
                    } catch (_: Exception) {
                        // Closed or truncated session; do not retry this socket.
                    }
                }
            }
            closer.cancel()
        }
    }

    private suspend fun handleClient(
        socket: BluetoothSocket,
        pairingsById: suspend () -> Map<String, Pairing>,
        onPeer: suspend (deviceId: String, ipv4: String) -> Unit,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ) {
        socket.use {
            UnlockInboundSession.handle(
                input = it.inputStream,
                output = it.outputStream,
                pairingsById = pairingsById(),
                decide = decide,
                clock = clock,
                stillOpen = { it.isConnected },
                onMatched = { deviceId, tsIP ->
                    if (tsIP.isNotBlank()) onPeer(deviceId, tsIP)
                },
            )
        }
    }

    companion object {
        const val SERVICE_NAME = "JarvisAuth"
        val SERVICE_UUID: UUID = UUID.fromString("62182bf7-97c8-45f9-aa2c-53c5f2008bdf")
    }
}

private class BluetoothListenFailed : IOException()
