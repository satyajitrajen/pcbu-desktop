package com.jarvisauth.app.ui.unlock

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvisauth.app.JarvisAuthApp
import com.jarvisauth.app.ui.theme.JarvisAuthTheme

class UnlockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        if (Build.VERSION.SDK_INT < 27) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        val app = application as JarvisAuthApp
        setContent {
            val vm: UnlockViewModel = viewModel(factory = app.graph.viewModelFactory)
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state) {
                if (state is PlateState.Gone) finish()
            }
            JarvisAuthTheme {
                UnlockPlate(
                    state = state,
                    onConfirm = { authenticate(vm) },
                    onCancel = { vm.onCancel() },
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun authenticate(vm: UnlockViewModel) {
        val prompt =
            BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        vm.onConfirmed()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        vm.onCancel()
                    }
                },
            )
        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm unlock")
        if (Build.VERSION.SDK_INT >= 30) {
            info.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            info.setDeviceCredentialAllowed(true)
        }
        prompt.authenticate(info.build())
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, UnlockActivity::class.java)
    }
}
