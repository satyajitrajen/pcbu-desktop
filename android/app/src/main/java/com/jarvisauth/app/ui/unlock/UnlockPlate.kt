package com.jarvisauth.app.ui.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.domain.UnlockPrompt
import com.jarvisauth.app.ui.components.PrimaryButton
import com.jarvisauth.app.ui.components.QuietTextButton
import com.jarvisauth.app.ui.components.RefuseTextButton
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.JarvisAuthTheme
import com.jarvisauth.app.ui.theme.Mute
import com.jarvisauth.app.ui.theme.Night
import com.jarvisauth.app.ui.theme.Ok
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.Refuse

@Composable
fun UnlockPlate(
    state: PlateState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        when (state) {
            PlateState.Loading -> {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "Connecting to PC…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Mute,
                )
                Spacer(Modifier.height(1.dp))
            }
            is PlateState.Ask -> AskContent(state.prompt, onConfirm, onCancel)
            is PlateState.Unlocked ->
                OutcomeContent(
                    hostname = state.pcName,
                    caption = "Unlocked.",
                    captionColor = Ok,
                    onDismiss = null,
                )
            is PlateState.Denied ->
                OutcomeContent(
                    hostname = state.pcName,
                    caption = "Not unlocked.",
                    detail = state.reason.takeIf { it.isNotBlank() && it != "Not unlocked." },
                    captionColor = Refuse,
                    onDismiss = onDismiss,
                )
            PlateState.Gone -> Unit
        }
    }
}

@Composable
private fun AskContent(
    prompt: UnlockPrompt,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        Spacer(Modifier.height(24.dp))
        Text(
            text = prompt.pcName,
            style = MaterialTheme.typography.headlineLarge,
            color = Paper,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Hairline, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = prompt.user,
            style = MaterialTheme.typography.bodyLarge,
            color = Paper,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = prompt.program,
            style = MaterialTheme.typography.bodyMedium,
            color = Mute,
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrimaryButton(
            text = "Confirm",
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
        RefuseTextButton(
            text = "Cancel",
            onClick = onCancel,
        )
    }
}

@Composable
private fun OutcomeContent(
    hostname: String,
    caption: String,
    captionColor: Color,
    onDismiss: (() -> Unit)?,
    detail: String? = null,
) {
    Column {
        Spacer(Modifier.height(24.dp))
        if (hostname.isNotBlank()) {
            Text(
                text = hostname,
                style = MaterialTheme.typography.headlineLarge,
                color = Paper,
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Hairline, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.titleLarge,
            color = captionColor,
        )
        if (detail != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Mute,
            )
        }
    }
    if (onDismiss != null) {
        QuietTextButton(text = "Dismiss", onClick = onDismiss)
    } else {
        Spacer(Modifier.height(48.dp))
    }
}

@Preview
@Composable
private fun UnlockPlatePreview() {
    JarvisAuthTheme {
        UnlockPlate(
            state =
                PlateState.Ask(
                    UnlockPrompt("id", "WORKSTATION-01", "satya", "Windows login", "tok"),
                ),
            onConfirm = {},
            onCancel = {},
            onDismiss = {},
        )
    }
}
