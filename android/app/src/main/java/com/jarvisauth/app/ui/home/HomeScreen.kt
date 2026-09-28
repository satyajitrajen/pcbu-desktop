package com.jarvisauth.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.ui.components.BenchScaffold
import com.jarvisauth.app.ui.components.GhostButton
import com.jarvisauth.app.ui.components.PrimaryButton
import com.jarvisauth.app.ui.components.QuietTextButton
import com.jarvisauth.app.ui.theme.Brass
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.JarvisAuthTheme
import com.jarvisauth.app.ui.theme.Mute
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.PlexMono
import com.jarvisauth.app.ui.theme.Refuse

@Composable
fun HomeScreen(
    pcs: List<PcRowUi>,
    armed: Boolean,
    onPairPc: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onBatteryHelp: () -> Unit,
    onLogs: () -> Unit = {},
    onPcClick: (String) -> Unit = {},
    listenerError: String? = null,
    onOpenBluetooth: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (pcs.isEmpty()) {
        EmptyHome(
            onPairPc = onPairPc,
            onLogs = onLogs,
            modifier = modifier,
        )
    } else {
        PairedHome(
            pcs = pcs,
            armed = armed,
            listenerError = listenerError,
            onPairPc = onPairPc,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onBatteryHelp = onBatteryHelp,
            onLogs = onLogs,
            onPcClick = onPcClick,
            onOpenBluetooth = onOpenBluetooth,
            modifier = modifier,
        )
    }
}

@Composable
private fun EmptyHome(
    onPairPc: () -> Unit,
    onLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BenchScaffold(
        modifier = modifier,
        dock = {
            PrimaryButton(
                text = "Pair PC",
                onClick = onPairPc,
                modifier = Modifier.fillMaxWidth(),
            )
            QuietTextButton(
                text = "Logs",
                onClick = onLogs,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        },
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "No PCs yet",
            style = MaterialTheme.typography.labelSmall,
            color = Mute,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "JarvisAuth",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pair a PC to unlock it from this phone.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PairedHome(
    pcs: List<PcRowUi>,
    armed: Boolean,
    listenerError: String?,
    onPairPc: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onBatteryHelp: () -> Unit,
    onLogs: () -> Unit,
    onPcClick: (String) -> Unit,
    onOpenBluetooth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BenchScaffold(
        modifier = modifier,
        armed = armed,
        dock = {
            if (listenerError != null) {
                Text(
                    text = listenerError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Refuse,
                )
                Spacer(Modifier.height(12.dp))
                GhostButton(
                    text = "Open Bluetooth",
                    onClick = onOpenBluetooth,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(
                text = if (armed) "Stop listening" else "Start listening",
                onClick = if (armed) onStopListening else onStartListening,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                QuietTextButton(text = "Logs", onClick = onLogs)
                QuietTextButton(text = "Battery help", onClick = onBatteryHelp)
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "JarvisAuth",
                    style = MaterialTheme.typography.labelSmall,
                    color = Mute,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (armed) "Listening" else "Idle",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (armed) Brass else Paper,
                )
            }
            QuietTextButton(text = "Pair PC", onClick = onPairPc)
        }
        Spacer(Modifier.height(24.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(pcs, key = { it.deviceId }) { pc ->
                PcRow(pc, onClick = { onPcClick(pc.deviceId) })
                HorizontalDivider(color = Hairline, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun PcRow(
    pc: PcRowUi,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(onClick = onClick)
                .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = pc.name,
                style = MaterialTheme.typography.titleLarge,
                color = Paper,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pc.state,
                style = MaterialTheme.typography.labelSmall,
                color = Mute,
            )
        }
        Text(
            text = pc.methodLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
            color = Mute,
        )
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    JarvisAuthTheme {
        HomeScreen(
            pcs = emptyList(),
            armed = false,
            onPairPc = {},
            onStartListening = {},
            onStopListening = {},
            onBatteryHelp = {},
        )
    }
}

@Preview
@Composable
private fun HomeScreenPairedPreview() {
    JarvisAuthTheme {
        HomeScreen(
            pcs =
                listOf(
                    PcRowUi("abc", "WORKSTATION-01", "UDP", "Idle"),
                    PcRowUi("def", "LAPTOP", "Bluetooth", "Idle"),
                ),
            armed = false,
            onPairPc = {},
            onStartListening = {},
            onStopListening = {},
            onBatteryHelp = {},
        )
    }
}
