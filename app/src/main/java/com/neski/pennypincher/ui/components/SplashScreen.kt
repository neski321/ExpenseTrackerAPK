package com.neski.pennypincher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.AttachMoney,
                contentDescription = "PennyPincher Icon",
                tint = colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "PennyPincher",
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CircularProgressIndicator(
                color = colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
} 