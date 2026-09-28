package com.jarvisauth.app.ui.pair

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvisauth.app.ui.components.BenchScaffold
import com.jarvisauth.app.ui.components.CodeField
import com.jarvisauth.app.ui.components.GhostButton
import com.jarvisauth.app.ui.components.PrimaryButton
import com.jarvisauth.app.ui.components.QuietTextButton
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.Mute
import com.jarvisauth.app.ui.theme.Night
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.Refuse

@Composable
fun PairScreen(
    viewModel: PairViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PairEvent.Success -> onClose()
            }
        }
    }
    BackHandler {
        viewModel.onAbandon()
        onClose()
    }
    when (val current = state) {
        PairUiState.Scanner ->
            ScannerPane(
                onQr = viewModel::onQrScanned,
                onEnterCode = viewModel::onEnterCode,
                onClose = {
                    viewModel.onAbandon()
                    onClose()
                },
                modifier = modifier,
            )
        PairUiState.Code ->
            CodePane(
                onConnect = viewModel::onCodeSubmit,
                onClose = {
                    viewModel.onAbandon()
                    onClose()
                },
                modifier = modifier,
            )
        PairUiState.Connecting ->
            ConnectingPane(
                onCancel = {
                    viewModel.onAbandon()
                    onClose()
                },
                modifier = modifier,
            )
        is PairUiState.Error ->
            ErrorPane(
                message = current.message,
                onRetry = viewModel::onRetry,
                onClose = {
                    viewModel.onAbandon()
                    onClose()
                },
                modifier = modifier,
            )
    }
}

@Composable
private fun ScannerPane(
    onQr: (String) -> Unit,
    onEnterCode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Night),
    ) {
        QrCameraPreview(
            enabled = true,
            onQr = onQr,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Night.copy(alpha = 0.35f)),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(240.dp)
                .border(1.5.dp, Hairline, RoundedCornerShape(8.dp)),
        )
        QuietTextButton(
            text = "Close",
            onClick = onClose,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Point at the QR on your PC.",
                style = MaterialTheme.typography.bodyLarge,
                color = Paper,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Don’t leave the QR on screen after pairing.",
                style = MaterialTheme.typography.labelSmall,
                color = Mute,
            )
            Spacer(Modifier.height(24.dp))
            GhostButton(
                text = "Enter code instead",
                onClick = onEnterCode,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CodePane(
    onConnect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    BenchScaffold(
        modifier = modifier,
        dock = {
            PrimaryButton(
                text = "Connect",
                onClick = { onConnect(code) },
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        QuietTextButton(text = "Close", onClick = onClose)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Enter pairing code",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Paste the code from the PC. Don’t leave the QR on screen after pairing.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        CodeField(value = code, onValueChange = { code = it })
    }
}

@Composable
private fun ConnectingPane(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BenchScaffold(
        modifier = modifier,
        dock = {
            GhostButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "Connecting to PC…",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ErrorPane(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BenchScaffold(
        modifier = modifier,
        dock = {
            PrimaryButton(
                text = "Retry",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(
                text = "Close",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        QuietTextButton(text = "Close", onClick = onClose)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Pairing failed",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Refuse,
        )
    }
}
