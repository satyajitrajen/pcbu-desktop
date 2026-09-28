package com.jarvisauth.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvisauth.app.data.DebugPrefs
import com.jarvisauth.app.domain.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel(
    private val appLog: AppLog,
    private val prefs: DebugPrefs,
) : ViewModel() {
    val lines: StateFlow<List<String>> = appLog.lines
    private val _debugEnabled = MutableStateFlow(appLog.debugEnabled)
    val debugEnabled: StateFlow<Boolean> = _debugEnabled.asStateFlow()

    fun setDebugEnabled(value: Boolean) {
        appLog.debugEnabled = value
        prefs.debugLogging = value
        _debugEnabled.value = value
    }

    fun clear() {
        viewModelScope.launch { appLog.clear() }
    }
}
