package com.jarvisauth.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvisauth.app.domain.ListenerState
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.PairingStore
import com.jarvisauth.app.protocol.PairingMethod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PcRowUi(
    val deviceId: String,
    val name: String,
    val methodLabel: String,
    val state: String,
)

class HomeViewModel(
    private val store: PairingStore,
    listenerState: ListenerState,
) : ViewModel() {
    val armed: StateFlow<Boolean> = listenerState.armed
    val listenerError: StateFlow<String?> = listenerState.error

    val needsBluetooth: StateFlow<Boolean> =
        store.observe()
            .map { list -> list.any { it.pairingMethod == PairingMethod.BLUETOOTH } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pcs: StateFlow<List<PcRowUi>> =
        combine(store.observe(), listenerState.armed) { list, isArmed ->
            list.map { it.toRow(if (isArmed) "Listening" else "Idle") }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { store.reload() }
    }
}

private fun Pairing.toRow(state: String): PcRowUi =
    PcRowUi(
        deviceId = deviceId,
        name = pcName,
        methodLabel = pairingMethod.label(),
        state = state,
    )

fun PairingMethod.label(): String =
    when (this) {
        PairingMethod.UDP -> "UDP"
        PairingMethod.TCP -> "TCP"
        PairingMethod.BLUETOOTH -> "Bluetooth"
        PairingMethod.MANUAL_UDP -> "Manual UDP"
    }
