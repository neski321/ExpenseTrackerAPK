package com.neski.pennypincher.ui.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neski.pennypincher.ui.navigation.AppSidebar

@Composable
fun PennyPincherLayout(
    selectedRoute: String,
    onRouteChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            AppSidebar(
                selectedRoute = selectedRoute,
                onItemSelected = onRouteChange,
                onToggleTheme = onToggleTheme,
                onLogout = onLogout
            )
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}