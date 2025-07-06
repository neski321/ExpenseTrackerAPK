package com.neski.pennypincher.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt

@Composable
fun CategoryIcon(name: String, color: String, size: Int = 24) {
    val icon = when (name.lowercase()) {
        "food", "restaurant" -> Icons.Default.Restaurant
        "shopping", "store" -> Icons.Default.ShoppingCart
        "home", "rent" -> Icons.Default.Home
        "travel", "flight" -> Icons.Default.Flight
        "health", "fitness" -> Icons.Default.Favorite
        else -> Icons.AutoMirrored.Filled.Label
    }

    val parsedColor = try {
        Color(color.toColorInt())
    } catch (e: Exception) {
        Color.Gray
    }

    Icon(imageVector = icon, contentDescription = name, tint = parsedColor)
}
