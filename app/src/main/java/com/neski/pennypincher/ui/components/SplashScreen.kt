package com.neski.pennypincher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    val colorScheme = MaterialTheme.colorScheme
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgGradient = if (isLight) {
        Brush.verticalGradient(listOf(colorScheme.primary.copy(alpha = 0.1f), colorScheme.primaryContainer))
    } else {
        Brush.verticalGradient(listOf(colorScheme.surface, colorScheme.background))
    }

    // Create infinite rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Use the new LoadingSpinner component
            LoadingSpinner(
                size = 120,
                showText = true,
                loadingText = "Loading..."
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "PennyPincher",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.primary
            )
        }
    }
} 