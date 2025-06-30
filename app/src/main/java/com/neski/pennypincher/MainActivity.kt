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
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.neski.pennypincher.ui.auth.LoginScreen
import com.neski.pennypincher.ui.auth.SignupScreen
import com.neski.pennypincher.ui.dashboard.DashboardScreen
import com.neski.pennypincher.ui.expenses.ExpensesScreen
import com.neski.pennypincher.ui.navigation.AppSidebar
import com.neski.pennypincher.ui.theme.PennyPincherTheme
import com.neski.pennypincher.ui.welcome.WelcomeScreen
import com.neski.pennypincher.data.repository.AuthRepository
import com.neski.pennypincher.data.repository.SessionManager
import com.neski.pennypincher.ui.categories.CategoriesScreen
import com.neski.pennypincher.ui.expenses.SearchExpensesScreen
import com.neski.pennypincher.ui.income.IncomeScreen
import com.neski.pennypincher.ui.payment.PaymentMethodsScreen
import com.neski.pennypincher.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import com.neski.pennypincher.ui.expenses.FilteredExpensesScreen
import com.neski.pennypincher.ui.income.IncomeSourcesScreen
import com.neski.pennypincher.ui.income.FilteredIncomeScreen
import com.neski.pennypincher.ui.components.SplashScreen

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SessionManager
        SessionManager.initialize(this)

        setContent {
            var selectedRoute by remember { mutableStateOf("loading") } // Start with loading
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            // Observe authentication state
            val isLoggedIn by SessionManager.isLoggedIn.collectAsState()
            val currentUser by SessionManager.currentUser.collectAsState()
            
            // Observe theme state from SessionManager
            val isDarkTheme by SessionManager.isDarkTheme.collectAsState()

            // Firebase user ID
            val userId = currentUser?.uid ?: ""

            // --- Category navigation stack for breadcrumbs ---
            val categoryStack = remember { mutableStateListOf<Pair<String, String>>() }
            var categoryOriginRoute by remember { mutableStateOf<String?>(null) }
            
            // --- Income source navigation origin tracking ---
            var incomeSourceOriginRoute by remember { mutableStateOf<String?>(null) }
            
            // --- Payment method navigation origin tracking ---
            var paymentMethodOriginRoute by remember { mutableStateOf<String?>(null) }

            // Check authentication state and set initial route
            LaunchedEffect(isLoggedIn) {
                selectedRoute = when {
                    isLoggedIn -> "dashboard"
                    else -> "welcome"
                }
            }

            PennyPincherTheme(useDarkTheme = isDarkTheme) {
                when (selectedRoute) {
                    "loading" -> {
                        // Show loading screen while checking authentication
                        SplashScreen()
                    }
                    
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
                                    onToggleTheme = {
                                        SessionManager.toggleTheme()
                                    },
                                    onLogout = {
                                        AuthRepository.signOut()
                                        selectedRoute = "welcome"
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        ) {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text("PennyPincher",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 25.sp
                                                ) },
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
                                            onNavigate = { selectedRoute = it },
                                            onNavigateToExpensesByMonth = { month ->
                                                selectedRoute = "expensesByMonth:$month"
                                            },
                                            categoryStack = categoryStack,
                                            setCategoryOriginRoute = { categoryOriginRoute = it }
                                        )
                                        selectedRoute == "expenses" -> ExpensesScreen(
                                            userId = userId,
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                categoryStack.clear()
                                                categoryStack.add(categoryId to categoryName)
                                                categoryOriginRoute = "expenses"
                                                selectedRoute = "expensesByCategory:$categoryId:$categoryName"
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                paymentMethodOriginRoute = "expenses"
                                                selectedRoute = "expensesByPaymentMethod:$paymentMethodId:$paymentMethodName"
                                            }
                                        )
                                        selectedRoute.startsWith("expensesByMonth:") -> {
                                            val month = selectedRoute.removePrefix("expensesByMonth:")
                                            FilteredExpensesScreen(
                                                userId = userId,
                                                month = month,
                                                onBack = { selectedRoute = "dashboard" }
                                            )
                                        }
                                        selectedRoute == "income" -> IncomeScreen(
                                            userId = userId,
                                            onNavigateToFilteredIncome = { incomeSourceId, incomeSourceName ->
                                                incomeSourceOriginRoute = "income"
                                                selectedRoute = "incomeBySource:$incomeSourceId:$incomeSourceName"
                                            }
                                        )
                                        selectedRoute == "categories" -> CategoriesScreen(
                                            userId = userId,
                                            onCategoryClick = { categoryId, categoryName ->
                                                categoryStack.clear()
                                                categoryStack.add(categoryId to categoryName)
                                                categoryOriginRoute = "categories"
                                                selectedRoute = "expensesByCategory:$categoryId:$categoryName"
                                            }
                                        )
                                        selectedRoute == "paymentMethods" -> PaymentMethodsScreen(
                                            userId = userId,
                                            onPaymentMethodClick = { paymentMethodId, paymentMethodName ->
                                                paymentMethodOriginRoute = "paymentMethods"
                                                selectedRoute = "expensesByPaymentMethod:$paymentMethodId:$paymentMethodName"
                                            }
                                        )
                                        selectedRoute == "search" -> SearchExpensesScreen(
                                            userId = userId,
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                categoryStack.clear()
                                                categoryStack.add(categoryId to categoryName)
                                                categoryOriginRoute = "search"
                                                selectedRoute = "expensesByCategory:$categoryId:$categoryName"
                                            }
                                        )
                                        selectedRoute == "settings" -> SettingsScreen(userId = userId)
                                        selectedRoute.startsWith("expensesByCategory:") -> {
                                            val parts = selectedRoute.removePrefix("expensesByCategory:").split(":")
                                            val categoryId = parts.getOrNull(0) ?: ""
                                            val categoryName = parts.drop(1).joinToString(":")
                                            FilteredExpensesScreen(
                                                userId = userId,
                                                categoryId = categoryId,
                                                categoryName = categoryName,
                                                onBack = {
                                                    if (categoryStack.size > 1) {
                                                        // Pop current, go to previous
                                                        categoryStack.removeAt(categoryStack.lastIndex)
                                                        val (prevId, prevName) = categoryStack.last()
                                                        selectedRoute = "expensesByCategory:$prevId:$prevName"
                                                    } else {
                                                        // Back to origin (expenses or categories)
                                                        categoryStack.clear()
                                                        selectedRoute = categoryOriginRoute ?: "categories"
                                                        categoryOriginRoute = null
                                                    }
                                                },
                                                onNavigateToCategory = { newCategoryId, newCategoryName ->
                                                    categoryStack.add(newCategoryId to newCategoryName)
                                                    selectedRoute = "expensesByCategory:$newCategoryId:$newCategoryName"
                                                }
                                            )
                                        }
                                        selectedRoute.startsWith("expensesByPaymentMethod:") -> {
                                            val parts = selectedRoute.removePrefix("expensesByPaymentMethod:").split(":")
                                            val paymentMethodId = parts.getOrNull(0) ?: ""
                                            val paymentMethodName = parts.drop(1).joinToString(":")
                                            FilteredExpensesScreen(
                                                userId = userId,
                                                paymentMethodId = paymentMethodId,
                                                paymentMethodName = paymentMethodName,
                                                onBack = { 
                                                    selectedRoute = paymentMethodOriginRoute ?: "expenses"
                                                    paymentMethodOriginRoute = null
                                                },
                                                onNavigateToCategory = { categoryId, categoryName ->
                                                    categoryStack.clear()
                                                    categoryStack.add(categoryId to categoryName)
                                                    categoryOriginRoute = "expensesByPaymentMethod"
                                                    selectedRoute = "expensesByCategory:$categoryId:$categoryName"
                                                },
                                                onNavigateToFilteredExpenses = { newPaymentMethodId, newPaymentMethodName ->
                                                    selectedRoute = "expensesByPaymentMethod:$newPaymentMethodId:$newPaymentMethodName"
                                                }
                                            )
                                        }
                                        selectedRoute == "incomeSources" -> IncomeSourcesScreen(
                                            userId = userId,
                                            onIncomeSourceClick = { incomeSourceId, incomeSourceName ->
                                                incomeSourceOriginRoute = "incomeSources"
                                                selectedRoute = "incomeBySource:$incomeSourceId:$incomeSourceName"
                                            }
                                        )
                                        selectedRoute.startsWith("incomeBySource:") -> {
                                            val parts = selectedRoute.removePrefix("incomeBySource:").split(":")
                                            val incomeSourceId = parts.getOrNull(0) ?: ""
                                            val incomeSourceName = parts.drop(1).joinToString(":")
                                            FilteredIncomeScreen(
                                                userId = userId,
                                                incomeSourceId = incomeSourceId,
                                                incomeSourceName = incomeSourceName,
                                                onBack = { 
                                                    selectedRoute = incomeSourceOriginRoute ?: "income"
                                                    incomeSourceOriginRoute = null
                                                },
                                                onNavigateToFilteredIncome = { newIncomeSourceId, newIncomeSourceName ->
                                                    selectedRoute = "incomeBySource:$newIncomeSourceId:$newIncomeSourceName"
                                                }
                                            )
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
}
