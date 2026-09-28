package com.jarvisauth.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvisauth.app.domain.LockError
import com.jarvisauth.app.domain.LockPc
import com.jarvisauth.app.domain.LockResult
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.domain.PairingStore
import com.jarvisauth.app.domain.UnlockPc
import com.jarvisauth.app.domain.UnlockPcResult
import com.jarvisauth.app.domain.UnlockSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailStatus {
    data object Idle : DetailStatus

    data object Unlocking : DetailStatus

    data object Locking : DetailStatus

    data object Locked : DetailStatus

    data class Powering(
        val action: String,
    ) : DetailStatus

    data class Powered(
        val action: String,
    ) : DetailStatus

    data class Failed(
        val message: String,
    ) : DetailStatus
}

class PcDetailViewModel(
    private val deviceId: String,
    private val store: PairingStore,
    private val lockPc: LockPc,
    private val unlockPc: UnlockPc,
    private val session: UnlockSession,
    private val onEmpty: () -> Unit,
) : ViewModel() {
    val pairing: StateFlow<Pairing?> =
        store.observe()
            .map { list -> list.find { it.deviceId == deviceId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plateUp: StateFlow<Boolean> =
        session.prompt
            .map { it?.deviceId == deviceId }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _status = MutableStateFlow<DetailStatus>(DetailStatus.Idle)
    val status: StateFlow<DetailStatus> = _status.asStateFlow()

    private var lockJob: Job? = null
    private var unlockJob: Job? = null
    private var clearJob: Job? = null

    fun unlock(showPlate: () -> Unit) {
        if (unlockJob?.isActive == true || lockJob?.isActive == true) return
        clearJob?.cancel()
        if (session.promptFor(deviceId) != null) {
            viewModelScope.launch { unlockPc(deviceId, showPlate) }
            return
        }
        unlockJob =
            viewModelScope.launch {
                _status.value = DetailStatus.Unlocking
                when (val result = unlockPc(deviceId, showPlate)) {
                    UnlockPcResult.Resumed -> _status.value = DetailStatus.Idle
                    UnlockPcResult.Unlocked -> _status.value = DetailStatus.Idle
                    UnlockPcResult.Cancelled -> _status.value = DetailStatus.Idle
                    is UnlockPcResult.Err ->
                        _status.value = DetailStatus.Failed(result.error.userMessage)
                }
            }
    }

    fun cancelUnlock() {
        unlockJob?.cancel()
        unlockJob = null
        if (_status.value is DetailStatus.Unlocking) {
            _status.value = DetailStatus.Idle
        }
    }

    fun lock() {
        command("")
    }

    fun shutdown() {
        command("shutdown")
    }

    fun restart() {
        command("restart")
    }

    fun cancelCommand() {
        lockJob?.cancel()
        lockJob = null
        when (_status.value) {
            DetailStatus.Locking, is DetailStatus.Powering -> _status.value = DetailStatus.Idle
            else -> Unit
        }
    }

    private fun command(action: String) {
        if (unlockJob?.isActive == true || lockJob?.isActive == true || plateUp.value) return
        clearJob?.cancel()
        lockJob =
            viewModelScope.launch {
                _status.value =
                    if (action.isEmpty()) {
                        DetailStatus.Locking
                    } else {
                        DetailStatus.Powering(action)
                    }
                when (val result = lockPc(deviceId, action)) {
                    LockResult.Ok -> {
                        _status.value =
                            if (action.isEmpty()) {
                                DetailStatus.Locked
                            } else {
                                DetailStatus.Powered(action)
                            }
                        clearJob =
                            launch {
                                delay(1_200)
                                _status.value = DetailStatus.Idle
                            }
                    }
                    is LockResult.Err ->
                        _status.value = DetailStatus.Failed(result.error.userMessage)
                }
            }
    }

    fun missingAddressMessage(): String = LockError.NoAddress.userMessage

    fun remove(onRemoved: () -> Unit) {
        viewModelScope.launch {
            store.remove(deviceId)
            if (store.snapshot().pairings.isEmpty()) {
                onEmpty()
            }
            onRemoved()
        }
    }
}
