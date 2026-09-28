package com.jarvisauth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvisauth.app.ui.theme.Brass
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.Mute
import com.jarvisauth.app.ui.theme.Paper
import com.jarvisauth.app.ui.theme.PlexMono
import com.jarvisauth.app.ui.theme.Refuse
import com.jarvisauth.app.ui.theme.Steel

@Composable
fun CodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val border =
        when {
            isError -> Refuse
            focused -> Brass
            else -> Hairline
        }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(Steel, RoundedCornerShape(6.dp))
                .border(1.dp, border, RoundedCornerShape(6.dp))
                .padding(16.dp)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = "Pairing code" },
        textStyle =
            TextStyle(
                fontFamily = PlexMono,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = Paper,
            ),
        cursorBrush = SolidColor(Brass),
    )
}

@Composable
fun SpecRow(
    label: String,
    value: String,
    mono: Boolean = false,
    muted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val talkBack = "$label, $value"
    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = talkBack },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Mute,
            )
            Text(
                text = value,
                style =
                    if (mono) {
                        MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                color = if (muted) Mute else Paper,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        HorizontalDivider(color = Hairline, thickness = 1.dp)
    }
}
