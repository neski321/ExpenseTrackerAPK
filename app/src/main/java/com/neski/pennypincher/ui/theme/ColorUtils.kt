package com.neski.pennypincher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun getTextColor(): Color {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (isLight) Color.Black else MaterialTheme.colorScheme.onBackground
} 