package com.neski.pennypincher.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search

@Composable
fun AppSidebar(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    isDarkTheme: Boolean
) {
    val colors = MaterialTheme.colorScheme

    val items = listOf(
        SidebarItem("Dashboard", "dashboard", Icons.Default.Home),
        SidebarItem("Expenses", "expenses", Icons.Default.AttachMoney),
        SidebarItem("Search Expenses", "search", Icons.Default.Search),
        SidebarItem("Income", "income", Icons.AutoMirrored.Filled.TrendingUp),
        SidebarItem("Categories", "categories", Icons.Default.Category),
        SidebarItem("Income Sources", "incomeSources", Icons.Default.Money),
        SidebarItem("Payment Methods", "paymentMethods", Icons.Default.CreditCard),
        SidebarItem("Settings", "settings", Icons.Default.Settings)
    )

    Surface(
        color = colors.surface,
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = "App Icon",
                    tint = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PennyPincher",
                    fontSize = 30.sp,
                    color = colors.primary,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label, fontSize = 16.sp, color = colors.onSurface) },
                    selected = item.route == selectedRoute,
                    onClick = { onItemSelected(item.route) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = colors.onSurface
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)

            NavigationDrawerItem(
                label = { Text("Toggle Theme", color = colors.onSurface) },
                selected = false,
                onClick = onToggleTheme,
                icon = {
                    val themeIcon = if (isDarkTheme) Icons.Default.Brightness7 else Icons.Default.DarkMode
                    Icon(themeIcon, contentDescription = "Toggle Theme", tint = colors.onSurface)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Logout", color = colors.onSurface) },
                selected = false,
                onClick = onLogout,
                icon = {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = colors.onSurface)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
