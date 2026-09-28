package com.jarvisauth.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.ui.theme.Brass
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.Night
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.Refuse

private val ButtonShape = RoundedCornerShape(6.dp)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .height(48.dp)
                .defaultMinSize(minWidth = 48.dp),
        shape = ButtonShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Brass,
                contentColor = Night,
                disabledContainerColor = Brass.copy(alpha = 0.4f),
                disabledContentColor = Night.copy(alpha = 0.4f),
            ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .height(48.dp)
                .defaultMinSize(minWidth = 48.dp),
        shape = ButtonShape,
        border = BorderStroke(1.dp, Hairline.copy(alpha = if (enabled) 1f else 0.4f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = Paper,
                disabledContentColor = Paper.copy(alpha = 0.4f),
            ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text)
    }
}

@Composable
fun QuietTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Paper),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Text(text)
    }
}

@Composable
fun RefuseTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Refuse),
    ) {
        Text(text)
    }
}
