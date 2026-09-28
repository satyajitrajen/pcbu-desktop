package com.jarvisauth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.ui.theme.Hairline
import com.jarvisauth.app.ui.theme.Night

@Composable
fun BenchScaffold(
    modifier: Modifier = Modifier,
    armed: Boolean = false,
    showRail: Boolean = true,
    dock: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding(),
    ) {
        if (showRail) {
            ArmedRail(armed = armed)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
            content = content,
        )
        if (dock != null) {
            HorizontalDivider(color = Hairline, thickness = 1.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 20.dp),
                content = dock,
            )
        }
    }
}
