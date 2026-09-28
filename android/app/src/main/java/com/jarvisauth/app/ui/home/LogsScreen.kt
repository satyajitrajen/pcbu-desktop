package com.jarvisauth.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.ui.components.BenchScaffold
import com.jarvisauth.app.ui.components.GhostButton
import com.jarvisauth.app.ui.components.QuietTextButton
import com.jarvisauth.app.ui.components.RefuseTextButton
import com.jarvisauth.app.ui.theme.Brass
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.Mute
import com.jarvisauth.app.ui.theme.Night
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.PlexMono

@Composable
fun LogsScreen(
    lines: List<String>,
    debugEnabled: Boolean,
    onDebugChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BenchScaffold(
        modifier = modifier,
        dock = {
            GhostButton(
                text = "Copy",
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("JarvisAuth logs", lines.joinToString("\n")))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            RefuseTextButton(
                text = "Clear",
                onClick = onClear,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        },
    ) {
        QuietTextButton(text = "Close", onClick = onClose)
        Spacer(Modifier.height(8.dp))
        Text(text = "Logs", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Secrets are stripped. Copy is for this phone only.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mute,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Debug logging",
                style = MaterialTheme.typography.bodyMedium,
                color = Paper,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = debugEnabled,
                onCheckedChange = onDebugChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = Night,
                        checkedTrackColor = Brass,
                        uncheckedThumbColor = Paper,
                        uncheckedTrackColor = Hairline,
                        uncheckedBorderColor = Mute,
                        checkedBorderColor = Brass,
                    ),
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            if (lines.isEmpty()) {
                item {
                    Text("No lines yet.", style = MaterialTheme.typography.bodyMedium, color = Mute)
                }
            } else {
                items(lines) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                        color = Paper,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
