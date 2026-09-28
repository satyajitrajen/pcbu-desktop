package com.jarvisauth.app.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvisauth.app.domain.UnlockDecision
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.domain.UnlockSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlateState {
    data object Loading : PlateState

    data class Ask(
        val prompt: UnlockPrompt,
    ) : PlateState

    data class Unlocked(
        val pcName: String,
    ) : PlateState

    data class Denied(
        val pcName: String,
        val reason: String,
    ) : PlateState

    data object Gone : PlateState
}

class UnlockViewModel(
    private val session: UnlockSession,
) : ViewModel() {
    private val _state = MutableStateFlow<PlateState>(PlateState.Loading)
    val state: StateFlow<PlateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            session.prompt.collect { prompt ->
                if (prompt != null && _state.value is PlateState.Loading) {
                    _state.value = PlateState.Ask(prompt)
                }
            }
        }
        val current = session.prompt.value
        if (current != null) {
            _state.value = PlateState.Ask(current)
        }
    }

    fun onConfirmed() {
        val name = (session.prompt.value?.pcName ?: (_state.value as? PlateState.Ask)?.prompt?.pcName).orEmpty()
        session.complete(UnlockDecision.Confirm)
        _state.value = PlateState.Unlocked(name)
        viewModelScope.launch {
            delay(800)
            _state.value = PlateState.Gone
        }
    }

    fun onCancel() {
        val name = (session.prompt.value?.pcName ?: (_state.value as? PlateState.Ask)?.prompt?.pcName).orEmpty()
        session.complete(UnlockDecision.Cancel)
        _state.value = PlateState.Denied(name, "Not unlocked.")
    }
}
