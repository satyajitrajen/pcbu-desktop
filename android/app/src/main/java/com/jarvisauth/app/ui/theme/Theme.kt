package com.jarvisauth.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val JarvisAuthColorScheme =
    darkColorScheme(
        primary = Brass,
        onPrimary = Night,
        primaryContainer = Steel,
        onPrimaryContainer = Paper,
        secondary = Brass,
        onSecondary = Night,
        background = Night,
        onBackground = Paper,
        surface = Night,
        onSurface = Paper,
        surfaceVariant = Steel,
        onSurfaceVariant = Mute,
        outline = Hairline,
        error = Refuse,
        onError = Paper,
    )

private val JarvisAuthTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                color = Paper,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                color = Paper,
            ),
        titleLarge =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = Paper,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = Paper,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = Mute,
            ),
        labelLarge =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Mute,
            ),
    )

@Composable
fun JarvisAuthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisAuthColorScheme,
        typography = JarvisAuthTypography,
        content = content,
    )
}
