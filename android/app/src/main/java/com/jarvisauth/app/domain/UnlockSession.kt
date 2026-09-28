package com.jarvisauth.app.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class UnlockSession {
    private val mutex = Mutex()
    private val _prompt = MutableStateFlow<UnlockPrompt?>(null)
    val prompt: StateFlow<UnlockPrompt?> = _prompt.asStateFlow()
    private var waiter: CompletableDeferred<UnlockDecision>? = null

    suspend fun awaitDecision(
        prompt: UnlockPrompt,
        onPresented: () -> Unit = {},
    ): UnlockDecision {
        val deferred = CompletableDeferred<UnlockDecision>()
        mutex.withLock {
            waiter?.complete(UnlockDecision.Cancel)
            waiter = deferred
            _prompt.value = prompt
        }
        onPresented()
        return try {
            withTimeout(90_000) { deferred.await() }
        } catch (_: TimeoutCancellationException) {
            UnlockDecision.Cancel
        } finally {
            mutex.withLock {
                if (waiter === deferred) {
                    waiter = null
                    _prompt.value = null
                }
            }
        }
    }

    fun complete(decision: UnlockDecision) {
        waiter?.complete(decision)
    }

    fun promptFor(deviceId: String): UnlockPrompt? =
        _prompt.value?.takeIf { it.deviceId == deviceId }
}

class ListenerState {
    private val _armed = MutableStateFlow(false)
    val armed: StateFlow<Boolean> = _armed.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setArmed(value: Boolean) {
        _armed.value = value
        _error.value = null
    }

    fun setError(message: String?) {
        _error.value = message
    }
}
