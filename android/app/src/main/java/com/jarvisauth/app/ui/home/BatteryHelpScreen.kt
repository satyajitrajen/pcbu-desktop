package com.jarvisauth.app.ui.home

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.jarvisauth.app.ui.components.BenchScaffold
import com.jarvisauth.app.ui.components.PrimaryButton
import com.jarvisauth.app.ui.components.QuietTextButton
import com.jarvisauth.app.ui.theme.Mute

@Composable
fun BatteryHelpScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BenchScaffold(
        modifier = modifier,
        dock = {
            PrimaryButton(
                text = "Open battery settings",
                onClick = {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                "package:${context.packageName}".toUri(),
                            )
                        } else {
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        QuietTextButton(text = "Close", onClick = onClose)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Battery help",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text =
                "Some phones stop apps in the background. If unlock requests never appear, " +
                    "allow JarvisAuth to ignore battery optimizations. This does not override every OEM killer, " +
                    "but it is the setting Android actually exposes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mute,
        )
    }
}
