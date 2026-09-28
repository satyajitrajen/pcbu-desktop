package com.jarvisauth.app

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jarvisauth.app.data.DebugPrefs
import com.jarvisauth.app.data.EncryptedPairingStore
import com.jarvisauth.app.domain.AppLog
import com.jarvisauth.app.domain.ListenerState
import com.jarvisauth.app.domain.LockPc
import com.jarvisauth.app.domain.PairPc
import com.jarvisauth.app.domain.PairingStore
import com.jarvisauth.app.domain.UnlockPc
import com.jarvisauth.app.domain.UnlockSession
import com.jarvisauth.app.transport.TcpLockClient
import com.jarvisauth.app.transport.TcpPairClient
import com.jarvisauth.app.transport.TcpUnlockClient
import com.jarvisauth.app.transport.UdpPairingDiscovery
import com.jarvisauth.app.ui.home.HomeViewModel
import com.jarvisauth.app.ui.home.LogsViewModel
import com.jarvisauth.app.ui.pair.PairViewModel
import com.jarvisauth.app.ui.unlock.UnlockViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisAuthApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.applicationScope.launch {
            graph.pairingStore.reload()
        }
    }
}

class AppGraph(
    app: Application,
) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val pairingStore: PairingStore = EncryptedPairingStore(app)
    val debugPrefs = DebugPrefs(app)
    val appLog =
        AppLog(logcat = { Log.i("JarvisAuth", it) }).also { log ->
            log.debugEnabled = debugPrefs.debugLogging
        }
    val unlockSession = UnlockSession()
    val listenerState = ListenerState()
    val pairPc: PairPc =
        PairPc(
            store = pairingStore,
            discovery = UdpPairingDiscovery(app),
            client = TcpPairClient(),
            deviceName = Build.MODEL.ifBlank { "Android" },
        )
    val lockPc: LockPc =
        LockPc(
            store = pairingStore,
            client = TcpLockClient(),
            appLog = appLog,
        )
    val unlockPc: UnlockPc =
        UnlockPc(
            store = pairingStore,
            session = unlockSession,
            dialer = TcpUnlockClient(connectTimeoutMs = 8_000, soTimeoutMs = 8_000),
            appLog = appLog,
        )
    val viewModelFactory: ViewModelProvider.Factory = GraphViewModelFactory(this)
}

class GraphViewModelFactory(
    private val graph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(graph.pairingStore, graph.listenerState) as T
            modelClass.isAssignableFrom(PairViewModel::class.java) ->
                PairViewModel(graph.pairPc) as T
            modelClass.isAssignableFrom(UnlockViewModel::class.java) ->
                UnlockViewModel(graph.unlockSession) as T
            modelClass.isAssignableFrom(LogsViewModel::class.java) ->
                LogsViewModel(graph.appLog, graph.debugPrefs) as T
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}
