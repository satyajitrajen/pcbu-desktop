package com.jarvisauth.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.domain.Ipv4
import com.jarvisauth.app.domain.OverlayIpv4
import com.jarvisauth.app.domain.Pairing
import com.jarvisauth.app.ui.components.BenchScaffold
import com.jarvisauth.app.ui.components.GhostButton
import com.jarvisauth.app.ui.components.PrimaryButton
import com.jarvisauth.app.ui.components.QuietTextButton
import com.jarvisauth.app.ui.components.RefuseTextButton
import com.jarvisauth.app.ui.components.SpecRow
import com.jarvisauth.app.ui.theme.Mute
import com.jarvisauth.app.ui.theme.Ok
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.Refuse

@Composable
fun PcDetailScreen(
    pairing: Pairing?,
    status: DetailStatus,
    plateUp: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onShutdown: () -> Unit,
    onRestart: () -> Unit,
    onCancelUnlock: () -> Unit,
    onCancelCommand: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
    missingAddressMessage: String,
    modifier: Modifier = Modifier,
) {
    var confirming by remember { mutableStateOf(false) }
    val hasAddress =
        pairing != null &&
            (Ipv4.parse(pairing.lastIpv4) != null || OverlayIpv4.isOverlay(pairing.lastTailscaleIpv4))
    val unlocking = status is DetailStatus.Unlocking
    val locking = status is DetailStatus.Locking
    val powering = status is DetailStatus.Powering
    val actionsLocked = unlocking || locking || powering || plateUp
    BackHandler(enabled = unlocking || locking || powering) {
        if (unlocking) onCancelUnlock() else onCancelCommand()
    }
    BenchScaffold(
        modifier = modifier,
        dock =
            if (pairing == null) {
                null
            } else {
                {
                    StatusLine(
                        hasAddress = hasAddress,
                        status = status,
                        missingAddressMessage = missingAddressMessage,
                    )
                    PrimaryButton(
                        text = "Unlock PC",
                        onClick = onUnlock,
                        enabled = !actionsLocked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    GhostButton(
                        text = "Lock PC",
                        onClick = onLock,
                        enabled = !actionsLocked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GhostButton(
                            text = "Shut down",
                            onClick = onShutdown,
                            enabled = !actionsLocked,
                            modifier = Modifier.weight(1f),
                        )
                        GhostButton(
                            text = "Restart",
                            onClick = onRestart,
                            enabled = !actionsLocked,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (confirming) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "This PC will not be able to unlock from this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Mute,
                        )
                        RefuseTextButton(
                            text = "Remove PC",
                            onClick = onRemove,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        GhostButton(
                            text = "Keep",
                            onClick = { confirming = false },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        RefuseTextButton(
                            text = "Remove PC",
                            onClick = { confirming = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            },
    ) {
        QuietTextButton(text = "Close", onClick = onClose)
        Spacer(Modifier.height(16.dp))
        if (pairing == null) {
            Text("This PC is gone.", style = MaterialTheme.typography.bodyMedium, color = Mute)
        } else {
            Text(
                text = pairing.pcName,
                style = MaterialTheme.typography.headlineLarge,
                color = Paper,
            )
            Spacer(Modifier.height(28.dp))
            SpecRow("OS", pairing.pcOs.ifBlank { "Unknown" })
            SpecRow("Method", pairing.pairingMethod.label())
            SpecRow("User", pairing.userName.ifBlank { "—" })
            SpecRow("Device", pairing.deviceId.take(12), mono = true)
            val tailscale = pairing.lastTailscaleIpv4.takeIf { OverlayIpv4.isOverlay(it) }
            SpecRow(
                label = "Tailscale",
                value = tailscale ?: "Not seen",
                mono = tailscale != null,
                muted = tailscale == null,
            )
        }
    }
}

@Composable
private fun StatusLine(
    hasAddress: Boolean,
    status: DetailStatus,
    missingAddressMessage: String,
) {
    val (text, color) =
        when {
            status is DetailStatus.Unlocking -> "Unlocking…" to Mute
            !hasAddress -> missingAddressMessage to Mute
            status is DetailStatus.Locking -> "Locking…" to Mute
            status is DetailStatus.Locked -> "Locked." to Ok
            status is DetailStatus.Powering && status.action == "restart" -> "Restarting…" to Mute
            status is DetailStatus.Powering -> "Shutting down…" to Mute
            status is DetailStatus.Powered && status.action == "restart" -> "Restarting." to Ok
            status is DetailStatus.Powered -> "Shutting down." to Ok
            status is DetailStatus.Failed -> status.message to Refuse
            else -> return
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Spacer(Modifier.height(12.dp))
}
