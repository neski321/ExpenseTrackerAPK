package com.neski.pennypincher.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

data class NavigationState(
    val currentRoute: String,
    val categoryStack: List<Pair<String, String>> = emptyList(),
    val navigationStack: List<String> = emptyList(),
    val categoryOriginRoute: String? = null,
    val paymentMethodOriginRoute: String? = null,
    val nestedOriginRoute: String? = null,
    val incomeSourceOriginRoute: String? = null
)

sealed class NavigationEvent {
    data class NavigateToRoute(val route: String) : NavigationEvent()
    data class NavigateToCategory(val categoryId: String, val categoryName: String) : NavigationEvent()
    data class NavigateToPaymentMethod(val paymentMethodId: String, val paymentMethodName: String) : NavigationEvent()
    data class NavigateToMonth(val month: String) : NavigationEvent()
    data class NavigateToIncomeSource(val incomeSourceId: String, val incomeSourceName: String) : NavigationEvent()
    object NavigateBack : NavigationEvent()
    object ClearNavigationStack : NavigationEvent()
}

class ExpenseNavigationManager {
    private var _navigationState = mutableStateOf(NavigationState("dashboard"))
    val navigationState: State<NavigationState> = _navigationState

    fun handleNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToRoute -> {
                val isRoot = isRootScreen(event.route)
                if (isRoot) {
                    // Clear stack and all origins when entering a root page
                    _navigationState.value = NavigationState(event.route)
                } else {
                    val currentState = _navigationState.value
                    val newNavigationStack = currentState.navigationStack.toMutableList()
                    // Don't add root screens to the navigation stack
                    if (!isRootScreen(currentState.currentRoute)) {
                        newNavigationStack.add(currentState.currentRoute)
                    }
                    _navigationState.value = currentState.copy(
                        currentRoute = event.route,
                        navigationStack = newNavigationStack.toList()
                    )
                }
            }
            
            is NavigationEvent.NavigateToCategory -> {
                val currentState = _navigationState.value
                val isFromRoot = isRootScreen(currentState.currentRoute)
                val newCategoryStack = if (isFromRoot) mutableListOf<Pair<String, String>>() else currentState.categoryStack.toMutableList()
                newCategoryStack.add(event.categoryId to event.categoryName)
                
                val newRoute = when {
                    currentState.currentRoute.startsWith("expensesByMonth:") -> {
                        val month = currentState.currentRoute.removePrefix("expensesByMonth:")
                        "expensesByMonthAndCategory:$month:${event.categoryId}:${event.categoryName}"
                    }
                    currentState.currentRoute.startsWith("expensesByPaymentMethod:") -> {
                        val parts = currentState.currentRoute.removePrefix("expensesByPaymentMethod:").split(":")
                        val paymentMethodId = parts.getOrNull(0) ?: ""
                        val paymentMethodName = parts.drop(1).joinToString(":")
                        "expensesByCategoryAndPaymentMethod:${event.categoryId}:${event.categoryName}:$paymentMethodId:$paymentMethodName"
                    }
                    currentState.currentRoute.startsWith("expensesByMonthAndPaymentMethod:") -> {
                        val parts = currentState.currentRoute.removePrefix("expensesByMonthAndPaymentMethod:").split(":")
                        val month = parts.getOrNull(0) ?: ""
                        val paymentMethodId = parts.getOrNull(1) ?: ""
                        val paymentMethodName = parts.drop(2).joinToString(":")
                        "expensesByMonthAndCategoryAndPaymentMethod:$month:${event.categoryId}:${event.categoryName}:$paymentMethodId:$paymentMethodName"
                    }
                    else -> "expensesByCategory:${event.categoryId}:${event.categoryName}"
                }
                
                // Prevent duplicate navigation if already on this filtered page
                if (newRoute == currentState.currentRoute) return
                
                // Determine the category origin route
                val categoryOriginRoute = when {
                    isFromRoot -> currentState.currentRoute
                    currentState.categoryOriginRoute != null -> currentState.categoryOriginRoute
                    else -> currentState.currentRoute
                }
                
                val newNavigationStack = if (isFromRoot) mutableListOf<String>() else currentState.navigationStack.toMutableList()
                if (!isRootScreen(currentState.currentRoute)) {
                    newNavigationStack.add(currentState.currentRoute)
                }
                
                _navigationState.value = currentState.copy(
                    currentRoute = newRoute,
                    categoryStack = newCategoryStack.toList(),
                    navigationStack = newNavigationStack.toList(),
                    categoryOriginRoute = categoryOriginRoute,
                    nestedOriginRoute = when {
                        currentState.currentRoute.startsWith("expensesByMonth:") -> currentState.nestedOriginRoute ?: "dashboard"
                        currentState.currentRoute.startsWith("expensesByPaymentMethod:") -> currentState.nestedOriginRoute ?: currentState.paymentMethodOriginRoute
                        else -> currentState.nestedOriginRoute
                    }
                )
            }
            
            is NavigationEvent.NavigateToPaymentMethod -> {
                val currentState = _navigationState.value
                val newRoute = when {
                    currentState.currentRoute.startsWith("expensesByMonth:") -> {
                        val month = currentState.currentRoute.removePrefix("expensesByMonth:")
                        "expensesByMonthAndPaymentMethod:$month:${event.paymentMethodId}:${event.paymentMethodName}"
                    }
                    currentState.currentRoute.startsWith("expensesByCategory:") -> {
                        val parts = currentState.currentRoute.removePrefix("expensesByCategory:").split(":")
                        val categoryId = parts.getOrNull(0) ?: ""
                        val categoryName = parts.drop(1).joinToString(":")
                        "expensesByCategoryAndPaymentMethod:$categoryId:$categoryName:${event.paymentMethodId}:${event.paymentMethodName}"
                    }
                    currentState.currentRoute.startsWith("expensesByMonthAndCategory:") -> {
                        val parts = currentState.currentRoute.removePrefix("expensesByMonthAndCategory:").split(":")
                        val month = parts.getOrNull(0) ?: ""
                        val categoryId = parts.getOrNull(1) ?: ""
                        val categoryName = parts.drop(2).joinToString(":")
                        "expensesByMonthAndCategoryAndPaymentMethod:$month:$categoryId:$categoryName:${event.paymentMethodId}:${event.paymentMethodName}"
                    }
                    else -> "expensesByPaymentMethod:${event.paymentMethodId}:${event.paymentMethodName}"
                }
                
                val newNavigationStack = currentState.navigationStack.toMutableList()
                // Don't add root screens to the navigation stack
                if (!isRootScreen(currentState.currentRoute)) {
                    newNavigationStack.add(currentState.currentRoute)
                }
                
                _navigationState.value = currentState.copy(
                    currentRoute = newRoute,
                    navigationStack = newNavigationStack.toList(),
                    paymentMethodOriginRoute = currentState.paymentMethodOriginRoute ?: currentState.currentRoute
                )
            }
            
            is NavigationEvent.NavigateToMonth -> {
                val currentState = _navigationState.value
                val newNavigationStack = currentState.navigationStack.toMutableList()
                // Don't add root screens to the navigation stack
                if (!isRootScreen(currentState.currentRoute)) {
                    newNavigationStack.add(currentState.currentRoute)
                }
                
                _navigationState.value = currentState.copy(
                    currentRoute = "expensesByMonth:${event.month}",
                    navigationStack = newNavigationStack.toList(),
                    nestedOriginRoute = currentState.nestedOriginRoute ?: currentState.currentRoute
                )
            }
            
            is NavigationEvent.NavigateToIncomeSource -> {
                val currentState = _navigationState.value
                val newRoute = "incomeBySource:${event.incomeSourceId}:${event.incomeSourceName}"
                // Prevent duplicate navigation if already on this filtered page
                if (newRoute == currentState.currentRoute) return
                val newNavigationStack = currentState.navigationStack.toMutableList()
                // Don't add root screens to the navigation stack
                if (!isRootScreen(currentState.currentRoute)) {
                    newNavigationStack.add(currentState.currentRoute)
                }
                _navigationState.value = currentState.copy(
                    currentRoute = newRoute,
                    navigationStack = newNavigationStack.toList(),
                    incomeSourceOriginRoute = currentState.incomeSourceOriginRoute ?: currentState.currentRoute
                )
            }
            
            is NavigationEvent.NavigateBack -> {
                handleBackNavigation()
            }
            
            is NavigationEvent.ClearNavigationStack -> {
                _navigationState.value = NavigationState("dashboard", navigationStack = emptyList())
            }
        }
    }
    
    private fun handleBackNavigation() {
        val currentState = _navigationState.value
        
        // If we have a navigation stack, pop the last route from it
        if (currentState.navigationStack.isNotEmpty()) {
            val newNavigationStack = currentState.navigationStack.toMutableList()
            val previousRoute = newNavigationStack.removeAt(newNavigationStack.lastIndex)
            
            // Handle category stack updates for category-related routes
            val newCategoryStack = if (currentState.currentRoute.startsWith("expensesByCategory") || 
                                      currentState.currentRoute.startsWith("expensesByMonthAndCategory") ||
                                      currentState.currentRoute.startsWith("expensesByCategoryAndPaymentMethod") ||
                                      currentState.currentRoute.startsWith("expensesByMonthAndCategoryAndPaymentMethod")) {
                if (currentState.categoryStack.isNotEmpty()) {
                    currentState.categoryStack.toMutableList().apply { removeAt(lastIndex) }
                } else {
                    currentState.categoryStack
                }
            } else {
                currentState.categoryStack
            }
            
            _navigationState.value = currentState.copy(
                currentRoute = previousRoute,
                navigationStack = newNavigationStack.toList(),
                categoryStack = newCategoryStack.toList()
            )
        } else {
            // Fallback to default navigation if no stack
            val currentRoute = currentState.currentRoute
            val newRoute = when {
                currentRoute.startsWith("expensesByCategory:") -> {
                    if (currentState.categoryStack.size > 1) {
                        // Pop current, go to previous category
                        val newCategoryStack = currentState.categoryStack.toMutableList()
                        newCategoryStack.removeAt(newCategoryStack.lastIndex)
                        val (prevId, prevName) = newCategoryStack.last()
                        
                        _navigationState.value = currentState.copy(
                            currentRoute = "expensesByCategory:$prevId:$prevName",
                            categoryStack = newCategoryStack.toList()
                        )
                        return
                    } else {
                        currentState.categoryOriginRoute ?: "categories"
                    }
                }
                
                currentRoute.startsWith("expensesByPaymentMethod:") -> {
                    currentState.paymentMethodOriginRoute ?: "expenses"
                }
                
                currentRoute.startsWith("expensesByMonth:") -> {
                    currentState.nestedOriginRoute ?: "dashboard"
                }
                
                currentRoute.startsWith("expensesByMonthAndCategory:") -> {
                    if (currentState.categoryStack.size > 1) {
                        // Pop current, go to previous category within same month
                        val parts = currentRoute.removePrefix("expensesByMonthAndCategory:").split(":")
                        val month = parts.getOrNull(0) ?: ""
                        val newCategoryStack = currentState.categoryStack.toMutableList()
                        newCategoryStack.removeAt(newCategoryStack.lastIndex)
                        val (prevId, prevName) = newCategoryStack.last()
                        
                        _navigationState.value = currentState.copy(
                            currentRoute = "expensesByMonthAndCategory:$month:$prevId:$prevName",
                            categoryStack = newCategoryStack.toList()
                        )
                        return
                    } else {
                        currentState.categoryOriginRoute ?: "expensesByMonth:${currentRoute.removePrefix("expensesByMonthAndCategory:").split(":").getOrNull(0) ?: ""}"
                    }
                }
                
                currentRoute.startsWith("expensesByCategoryAndPaymentMethod:") -> {
                    currentState.paymentMethodOriginRoute ?: "expensesByCategory:${currentState.categoryStack.lastOrNull()?.first ?: ""}:${currentState.categoryStack.lastOrNull()?.second ?: ""}"
                }
                
                currentRoute.startsWith("expensesByMonthAndPaymentMethod:") -> {
                    currentState.paymentMethodOriginRoute ?: "expensesByMonth:${currentRoute.removePrefix("expensesByMonthAndPaymentMethod:").split(":").getOrNull(0) ?: ""}"
                }
                
                currentRoute.startsWith("expensesByMonthAndCategoryAndPaymentMethod:") -> {
                    currentState.paymentMethodOriginRoute ?: "expensesByMonthAndCategory:${currentRoute.removePrefix("expensesByMonthAndCategoryAndPaymentMethod:").split(":").take(3).joinToString(":")}"
                }
                
                currentRoute.startsWith("incomeBySource:") -> {
                    currentState.incomeSourceOriginRoute ?: "income"
                }
                
                else -> "dashboard"
            }
            
            _navigationState.value = currentState.copy(currentRoute = newRoute)
        }
    }

    private fun isRootScreen(route: String): Boolean {
        val rootScreens = listOf("dashboard", "search", "expenses", "income", "categories", "paymentMethods", "incomeSources")
        return rootScreens.contains(route)
    }
} 