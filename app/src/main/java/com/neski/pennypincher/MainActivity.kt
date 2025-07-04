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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neski.pennypincher.data.repository.SessionManager
import com.neski.pennypincher.ui.auth.LoginScreen
import com.neski.pennypincher.ui.auth.SignupScreen
import com.neski.pennypincher.ui.dashboard.DashboardScreen
import com.neski.pennypincher.ui.expenses.ExpensesScreen
import com.neski.pennypincher.ui.navigation.AppSidebar
import com.neski.pennypincher.ui.theme.PennyPincherTheme
import com.neski.pennypincher.ui.welcome.WelcomeScreen
import com.neski.pennypincher.ui.categories.CategoriesScreen
import com.neski.pennypincher.ui.expenses.SearchExpensesScreen
import com.neski.pennypincher.ui.income.IncomeScreen
import com.neski.pennypincher.ui.payment.PaymentMethodsScreen
import com.neski.pennypincher.ui.settings.SettingsScreen
import com.neski.pennypincher.ui.expenses.FilteredExpensesScreen
import com.neski.pennypincher.ui.income.IncomeSourcesScreen
import com.neski.pennypincher.ui.income.FilteredIncomeScreen
import com.neski.pennypincher.ui.components.SplashScreen
import com.neski.pennypincher.ui.navigation.Screen
import com.neski.pennypincher.ui.navigation.NavigationEvent
import com.neski.pennypincher.ui.state.AppEvent
import com.neski.pennypincher.ui.state.AppStateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SessionManager
        SessionManager.initialize(this)

        setContent {
            val appStateManager: AppStateManager = viewModel()
            val appState by appStateManager.appState.collectAsState()
            
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            PennyPincherTheme(useDarkTheme = appState.isDarkTheme) {
                when {
                    appState.isLoading -> {
                        SplashScreen()
                    }
                    
                    !appState.isLoggedIn -> {
                        when (appState.navigationState.currentRoute) {
                            "welcome" -> WelcomeScreen(
                                onGetStarted = { 
                                    appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute("login")))
                                }
                            )
                            "login" -> LoginScreen(
                                onLoginSuccess = { 
                                    appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute("dashboard")))
                                },
                                onNavigateToSignup = { 
                                    appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute("signup")))
                                }
                            )
                            "signup" -> SignupScreen(
                                onSignupSuccess = { 
                                    appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute("dashboard")))
                                },
                                onNavigateToLogin = { 
                                    appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute("login")))
                                }
                            )
                            else -> WelcomeScreen(
                                onGetStarted = { 
                                    appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute("login")))
                                }
                            )
                        }
                    }
                    
                    else -> {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                AppSidebar(
                                    selectedRoute = appState.navigationState.currentRoute,
                                    onItemSelected = { route ->
                                        appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute(route)))
                                        scope.launch { drawerState.close() }
                                    },
                                    onToggleTheme = {
                                        appStateManager.handleEvent(AppEvent.ToggleTheme)
                                    },
                                    onLogout = {
                                        appStateManager.handleEvent(AppEvent.Logout)
                                    },
                                    isDarkTheme = appState.isDarkTheme
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
                                            )
                                        },
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
                                    val screen = Screen.fromRoute(appState.navigationState.currentRoute)
                                    
                                    when (screen) {
                                        is Screen.Dashboard -> DashboardScreen(
                                            userId = appState.currentUser,
                                            onNavigate = { route ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute(route)))
                                            },
                                            onNavigateToExpensesByMonth = { month ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToMonth(month)))
                                            },
                                            categoryStack = appState.navigationState.categoryStack,
                                            setCategoryOriginRoute = { /* Handled by navigation manager */ }
                                        )
                                        
                                        is Screen.Expenses -> ExpensesScreen(
                                            userId = appState.currentUser,
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByMonth -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            month = screen.month,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByCategory -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            categoryId = screen.categoryId,
                                            categoryName = screen.categoryName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByPaymentMethod -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            paymentMethodId = screen.paymentMethodId,
                                            paymentMethodName = screen.paymentMethodName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByMonthAndCategory -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            month = screen.month,
                                            categoryId = screen.categoryId,
                                            categoryName = screen.categoryName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByMonthAndPaymentMethod -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            month = screen.month,
                                            paymentMethodId = screen.paymentMethodId,
                                            paymentMethodName = screen.paymentMethodName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByCategoryAndPaymentMethod -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            categoryId = screen.categoryId,
                                            categoryName = screen.categoryName,
                                            paymentMethodId = screen.paymentMethodId,
                                            paymentMethodName = screen.paymentMethodName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.ExpensesByMonthAndCategoryAndPaymentMethod -> FilteredExpensesScreen(
                                            userId = appState.currentUser,
                                            month = screen.month,
                                            categoryId = screen.categoryId,
                                            categoryName = screen.categoryName,
                                            paymentMethodId = screen.paymentMethodId,
                                            paymentMethodName = screen.paymentMethodName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.Income -> IncomeScreen(
                                            userId = appState.currentUser,
                                            onNavigateToFilteredIncome = { incomeSourceId, incomeSourceName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToIncomeSource(incomeSourceId, incomeSourceName)))
                                            }
                                        )
                                        
                                        is Screen.Categories -> CategoriesScreen(
                                            userId = appState.currentUser,
                                            onCategoryClick = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            }
                                        )
                                        
                                        is Screen.PaymentMethods -> PaymentMethodsScreen(
                                            userId = appState.currentUser,
                                            onPaymentMethodClick = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.Search -> SearchExpensesScreen(
                                            userId = appState.currentUser,
                                            onNavigateToCategory = { categoryId, categoryName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToCategory(categoryId, categoryName)))
                                            },
                                            onNavigateToFilteredExpenses = { paymentMethodId, paymentMethodName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToPaymentMethod(paymentMethodId, paymentMethodName)))
                                            }
                                        )
                                        
                                        is Screen.Settings -> SettingsScreen(userId = appState.currentUser)
                                        
                                        is Screen.IncomeSources -> IncomeSourcesScreen(
                                            userId = appState.currentUser,
                                            onIncomeSourceClick = { incomeSourceId, incomeSourceName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToIncomeSource(incomeSourceId, incomeSourceName)))
                                            }
                                        )
                                        
                                        is Screen.IncomeBySource -> FilteredIncomeScreen(
                                            userId = appState.currentUser,
                                            incomeSourceId = screen.incomeSourceId,
                                            incomeSourceName = screen.incomeSourceName,
                                            onBack = {
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateBack))
                                            },
                                            onNavigateToFilteredIncome = { incomeSourceId, incomeSourceName ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToIncomeSource(incomeSourceId, incomeSourceName)))
                                            }
                                        )
                                        
                                        else -> DashboardScreen(
                                            userId = appState.currentUser,
                                            onNavigate = { route ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToRoute(route)))
                                            },
                                            onNavigateToExpensesByMonth = { month ->
                                                appStateManager.handleEvent(AppEvent.Navigate(NavigationEvent.NavigateToMonth(month)))
                                            },
                                            categoryStack = appState.navigationState.categoryStack,
                                            setCategoryOriginRoute = { /* Handled by navigation manager */ }
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
