package com.jarvisauth.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvisauth.app.ui.theme.Brass
import com.jarvisauth.app.ui.theme.Hairline

@Composable
fun ArmedRail(
    armed: Boolean,
    modifier: Modifier = Modifier,
) {
    val railColor by animateColorAsState(
        targetValue = if (armed) Brass else Hairline,
        animationSpec = tween(180),
        label = "rail",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(railColor),
    )
}
