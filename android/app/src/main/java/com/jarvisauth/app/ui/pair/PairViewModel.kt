package com.jarvisauth.app.ui.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvisauth.app.domain.PairError
import com.jarvisauth.app.domain.PairPc
import com.jarvisauth.app.domain.PairResult
import com.jarvisauth.app.domain.userMessage
import com.jarvisauth.app.protocol.PairingQrParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface PairUiState {
    data object Scanner : PairUiState

    data object Code : PairUiState

    data object Connecting : PairUiState

    data class Error(
        val message: String,
    ) : PairUiState
}

sealed interface PairEvent {
    data object Success : PairEvent
}

class PairViewModel(
    private val pairPc: PairPc,
) : ViewModel() {
    private val _state = MutableStateFlow<PairUiState>(PairUiState.Scanner)
    val state: StateFlow<PairUiState> = _state.asStateFlow()

    private val eventsChannel = Channel<PairEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private var pairJob: Job? = null
    private var payloadAccepted = false

    fun onQrScanned(raw: String) {
        if (payloadAccepted) return
        if (PairingQrParser.parse(raw) == null) return
        startPair(raw, fromScanner = true)
    }

    fun onCodeSubmit(raw: String) {
        startPair(raw, fromScanner = false)
    }

    fun onEnterCode() {
        if (_state.value is PairUiState.Connecting) return
        payloadAccepted = false
        pairJob?.cancel()
        _state.value = PairUiState.Code
    }

    fun onRetry() {
        payloadAccepted = false
        pairJob?.cancel()
        _state.value = PairUiState.Scanner
    }

    fun onAbandon() {
        payloadAccepted = false
        pairJob?.cancel()
    }

    private fun startPair(
        raw: String,
        fromScanner: Boolean,
    ) {
        if (pairJob?.isActive == true) return
        pairJob =
            viewModelScope.launch {
                payloadAccepted = true
                _state.value = PairUiState.Connecting
                when (val result = pairPc(raw)) {
                    is PairResult.Ok -> eventsChannel.send(PairEvent.Success)
                    is PairResult.Err -> {
                        payloadAccepted = false
                        if (fromScanner && result.error == PairError.InvalidPayload) {
                            _state.value = PairUiState.Scanner
                        } else {
                            _state.value = PairUiState.Error(result.error.userMessage())
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        onAbandon()
        super.onCleared()
    }
}
