package com.neski.pennypincher

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import com.neski.pennypincher.ui.auth.LoginScreen
import com.neski.pennypincher.ui.auth.SignupScreen
import com.neski.pennypincher.ui.dashboard.DashboardScreen
import com.neski.pennypincher.ui.expenses.ExpensesScreen
import com.neski.pennypincher.ui.navigation.AppSidebar
import com.neski.pennypincher.ui.theme.PennyPincherTheme
import com.neski.pennypincher.ui.welcome.WelcomeScreen
import com.neski.pennypincher.data.repository.AuthRepository
import com.neski.pennypincher.ui.categories.CategoriesScreen
import com.neski.pennypincher.ui.expenses.SearchExpensesScreen
import com.google.firebase.auth.FirebaseAuth
import com.neski.pennypincher.ui.income.IncomeScreen
import com.neski.pennypincher.ui.payment.PaymentMethodsScreen
import com.neski.pennypincher.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import com.neski.pennypincher.ui.expenses.FilteredExpensesScreen

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var selectedRoute by remember { mutableStateOf("welcome") } // Start on welcome screen
            var isDarkTheme by remember { mutableStateOf(false) }
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            // Firebase user ID
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            PennyPincherTheme(useDarkTheme = isDarkTheme) {
                when (selectedRoute) {
                    "welcome" -> WelcomeScreen(
                        onGetStarted = { selectedRoute = "login" }
                    )

                    "login" -> LoginScreen(
                        onLoginSuccess = { selectedRoute = "dashboard" },
                        onNavigateToSignup = { selectedRoute = "signup" }
                    )

                    "signup" -> SignupScreen(
                        onSignupSuccess = { selectedRoute = "dashboard" },
                        onNavigateToLogin = { selectedRoute = "login" }
                    )

                    else -> {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                AppSidebar(
                                    selectedRoute = selectedRoute,
                                    onItemSelected = {
                                        selectedRoute = it
                                        scope.launch { drawerState.close() }
                                    },
                                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                                    onLogout = {
                                        AuthRepository.signOut()
                                        selectedRoute = "login"
                                    }
                                )
                            }
                        ) {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("PennyPincher") },
                                        navigationIcon = {
                                            IconButton(onClick = {
                                                scope.launch { drawerState.open() }
                                            }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                                            }
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    when {
                                        selectedRoute == "dashboard" -> DashboardScreen(
                                            userId = userId,
                                            onNavigateToExpenses = { selectedRoute = "expenses" },
                                            onNavigateToExpensesByMonth = { month ->
                                                selectedRoute = "expensesByMonth:$month"
                                            }
                                        )
                                        selectedRoute == "expenses" -> ExpensesScreen(userId = userId)
                                        selectedRoute.startsWith("expensesByMonth:") -> {
                                            val month = selectedRoute.removePrefix("expensesByMonth:")
                                            FilteredExpensesScreen(
                                                userId = userId,
                                                month = month,
                                                onBack = { selectedRoute = "dashboard" }
                                            )
                                        }
                                        selectedRoute == "income" -> IncomeScreen(userId = userId)
                                        selectedRoute == "categories" -> CategoriesScreen(userId = userId)
                                        selectedRoute == "paymentMethods" -> PaymentMethodsScreen(userId = userId)
                                        selectedRoute == "search" -> SearchExpensesScreen(userId = userId)
                                        selectedRoute == "settings" -> SettingsScreen(userId = userId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
