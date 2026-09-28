package com.jarvisauth.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.jarvisauth.app.JarvisAuthApp
import com.jarvisauth.app.R
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.transport.BluetoothListenError
import com.jarvisauth.app.transport.RfcommUnlockServer
import com.jarvisauth.app.transport.TcpUnlockClient
import com.jarvisauth.app.transport.TcpUnlockServer
import com.jarvisauth.app.transport.UdpUnlockListener
import com.jarvisauth.app.ui.unlock.UnlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UnlockForegroundService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + job)
    private var listenJob: Job? = null
    private val handshakeMutex = Mutex()
    private val inFlight = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels()
        scope.launch {
            graph().unlockSession.prompt.collect { prompt ->
                if (prompt != null) showPlate(prompt)
            }
        }
        scope.launch {
            graph().pairingStore.observe().drop(1).collect {
                val current = listenJob ?: return@collect
                if (!current.isActive) return@collect
                current.cancelAndJoin()
                listenJob = scope.launch(Dispatchers.IO) { listen() }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        graph().listenerState.setArmed(true)
        if (listenJob?.isActive != true) {
            listenJob = scope.launch(Dispatchers.IO) { listen() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        graph().listenerState.setArmed(false)
        listenJob?.cancel()
        job.cancel()
        super.onDestroy()
    }

    private suspend fun listen() {
        val store = graph().pairingStore
        store.reload()
        val snap = store.snapshot()
        val local = snap.localDevice
        if (local == null || snap.pairings.isEmpty()) {
            stopSelf()
            return
        }
        val ports = linkedSetOf<Int>()
        if (snap.pairings.any { it.pairingMethod == PairingMethod.UDP }) {
            ports += local.udpPort
        }
        if (snap.pairings.any { it.pairingMethod == PairingMethod.MANUAL_UDP }) {
            ports += local.udpManualPort
        }
        val tcpPairings =
            snap.pairings.filter { it.pairingMethod == PairingMethod.TCP }.associateBy { it.deviceId }
        val btPairings =
            snap.pairings.filter { it.pairingMethod == PairingMethod.BLUETOOTH }.associateBy { it.deviceId }
        if (ports.isEmpty() && tcpPairings.isEmpty() && btPairings.isEmpty()) {
            return
        }
        coroutineScope {
            if (ports.isNotEmpty()) {
                launch {
                    graph().appLog.append("UDP unlock listening")
                    UdpUnlockListener(this@UnlockForegroundService).listen(ports) { broadcast ->
                        launch {
                            handleBroadcast(
                                broadcast.deviceId,
                                broadcast.pcbuIP,
                                broadcast.pcbuPort,
                                broadcast.isManual,
                                broadcast.tsIP,
                            )
                        }
                    }
                }
            }
            if (tcpPairings.isNotEmpty()) {
                launch {
                    try {
                        graph().appLog.append("TCP unlock listening")
                        TcpUnlockServer().listen(
                            port = local.tcpPort,
                            pairingsById = {
                                graph().pairingStore.snapshot().pairings
                                    .filter { it.pairingMethod == PairingMethod.TCP }
                                    .associateBy { it.deviceId }
                            },
                            onPeer = { id, ip -> graph().pairingStore.rememberIpv4(id, ip) },
                            decide = ::decidePrompt,
                        )
                    } catch (ex: Exception) {
                        graph().appLog.append("TCP unlock server failed: ${ex.javaClass.simpleName}")
                    }
                }
            }
            if (btPairings.isNotEmpty()) {
                launch {
                    graph().appLog.append("Bluetooth unlock listening")
                    val error =
                        try {
                            RfcommUnlockServer(this@UnlockForegroundService).listen(
                                pairingsById = {
                                    graph().pairingStore.snapshot().pairings
                                        .filter { it.pairingMethod == PairingMethod.BLUETOOTH }
                                        .associateBy { it.deviceId }
                                },
                                onPeer = { id, ip -> graph().pairingStore.rememberIpv4(id, ip) },
                                decide = ::decidePrompt,
                            )
                        } catch (ex: Exception) {
                            graph().appLog.append("BT unlock server failed: ${ex.javaClass.simpleName}")
                            BluetoothListenError.Failed
                        }
                    if (error != null) {
                        graph().appLog.append(error.userMessage)
                        graph().listenerState.setError(error.userMessage)
                    }
                }
            }
        }
    }

    private suspend fun decidePrompt(prompt: UnlockPrompt): UnlockDecision {
        handshakeMutex.withLock {
            if (!inFlight.add(prompt.deviceId)) return UnlockDecision.Cancel
        }
        return try {
            graph().unlockSession.awaitDecision(prompt)
        } finally {
            handshakeMutex.withLock { inFlight.remove(prompt.deviceId) }
        }
    }

    private suspend fun handleBroadcast(
        deviceId: String,
        host: String,
        port: Int,
        isManual: Boolean,
        tsIP: String,
    ) {
        val pairing =
            graph().pairingStore.snapshot().pairings.find { it.deviceId == deviceId } ?: return
        graph().pairingStore.rememberIpv4(deviceId, host)
        if (tsIP.isNotBlank()) graph().pairingStore.rememberIpv4(deviceId, tsIP)
        val expectsManual = pairing.pairingMethod == PairingMethod.MANUAL_UDP
        if (isManual != expectsManual) return
        handshakeMutex.withLock {
            if (!inFlight.add(pairing.deviceId)) return
        }
        try {
            graph().appLog.append("UDP unlock from ${pairing.deviceId.take(8)}")
            TcpUnlockClient().run(
                host = host,
                port = port,
                pairing = pairing,
                onLearnIp = { ip -> graph().pairingStore.rememberIpv4(pairing.deviceId, ip) },
            ) { prompt ->
                graph().unlockSession.awaitDecision(prompt)
            }
        } catch (ex: Exception) {
            graph().appLog.append("unlock handshake failed: ${ex.javaClass.simpleName}")
        } finally {
            handshakeMutex.withLock { inFlight.remove(pairing.deviceId) }
        }
    }

    private fun showPlate(prompt: UnlockPrompt) {
        val fullScreen =
            PendingIntent.getActivity(
                this,
                2,
                UnlockActivity.intent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(this, CHANNEL_UNLOCK)
                .setSmallIcon(R.drawable.ic_stat_key)
                .setContentTitle(prompt.pcName)
                .setContentText("Unlock request")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreen, true)
                .setAutoCancel(true)
                .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(UNLOCK_NOTIFICATION_ID, notification)
        startActivity(
            UnlockActivity.intent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun startAsForeground() {
        val stop =
            PendingIntent.getService(
                this,
                1,
                Intent(this, UnlockForegroundService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(this, CHANNEL_LISTENER)
                .setSmallIcon(R.drawable.ic_stat_key)
                .setContentTitle(getString(R.string.listener_notification_title))
                .setContentText(getString(R.string.listener_notification_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, getString(R.string.listener_stop), stop)
                .build()
        val type =
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
        ServiceCompat.startForeground(this, LISTENER_NOTIFICATION_ID, notification, type)
    }

    private fun ensureChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LISTENER,
                "Listener",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UNLOCK,
                "Unlock",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private fun graph() = (application as JarvisAuthApp).graph

    companion object {
        const val ACTION_START = "com.jarvisauth.app.START_LISTENER"
        const val ACTION_STOP = "com.jarvisauth.app.STOP_LISTENER"
        private const val CHANNEL_LISTENER = "listener"
        private const val CHANNEL_UNLOCK = "unlock"
        private const val LISTENER_NOTIFICATION_ID = 1
        private const val UNLOCK_NOTIFICATION_ID = 2

        fun start(context: Context) {
            val intent =
                Intent(context, UnlockForegroundService::class.java).setAction(ACTION_START)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, UnlockForegroundService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
