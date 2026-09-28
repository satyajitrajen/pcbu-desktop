package com.jarvisauth.app.data

import android.content.Context

class DebugPrefs(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences("jarvisauth", Context.MODE_PRIVATE)

    var debugLogging: Boolean
        get() = prefs.getBoolean(KEY, false)
        set(value) {
            prefs.edit().putBoolean(KEY, value).apply()
        }

    companion object {
        private const val KEY = "debug_logging"
    }
}
