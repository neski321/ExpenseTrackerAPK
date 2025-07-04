package com.neski.pennypincher.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object Income : Screen("income")
    object Categories : Screen("categories")
    object PaymentMethods : Screen("paymentMethods")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object IncomeSources : Screen("incomeSources")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Loading : Screen("loading")
    
    // Filtered expense screens
    data class ExpensesByMonth(val month: String) : Screen("expensesByMonth:$month")
    data class ExpensesByCategory(val categoryId: String, val categoryName: String) : Screen("expensesByCategory:$categoryId:$categoryName")
    data class ExpensesByPaymentMethod(val paymentMethodId: String, val paymentMethodName: String) : Screen("expensesByPaymentMethod:$paymentMethodId:$paymentMethodName")
    
    // Combined filtered screens
    data class ExpensesByMonthAndCategory(val month: String, val categoryId: String, val categoryName: String) : Screen("expensesByMonthAndCategory:$month:$categoryId:$categoryName")
    data class ExpensesByMonthAndPaymentMethod(val month: String, val paymentMethodId: String, val paymentMethodName: String) : Screen("expensesByMonthAndPaymentMethod:$month:$paymentMethodId:$paymentMethodName")
    data class ExpensesByCategoryAndPaymentMethod(val categoryId: String, val categoryName: String, val paymentMethodId: String, val paymentMethodName: String) : Screen("expensesByCategoryAndPaymentMethod:$categoryId:$categoryName:$paymentMethodId:$paymentMethodName")
    data class ExpensesByMonthAndCategoryAndPaymentMethod(val month: String, val categoryId: String, val categoryName: String, val paymentMethodId: String, val paymentMethodName: String) : Screen("expensesByMonthAndCategoryAndPaymentMethod:$month:$categoryId:$categoryName:$paymentMethodId:$paymentMethodName")
    
    // Income filtered screens
    data class IncomeBySource(val incomeSourceId: String, val incomeSourceName: String) : Screen("incomeBySource:$incomeSourceId:$incomeSourceName")
    
    companion object {
        fun fromRoute(route: String): Screen {
            return when {
                route == "dashboard" -> Dashboard
                route == "expenses" -> Expenses
                route == "income" -> Income
                route == "categories" -> Categories
                route == "paymentMethods" -> PaymentMethods
                route == "search" -> Search
                route == "settings" -> Settings
                route == "incomeSources" -> IncomeSources
                route == "welcome" -> Welcome
                route == "login" -> Login
                route == "signup" -> Signup
                route == "loading" -> Loading
                
                route.startsWith("expensesByMonth:") -> {
                    val month = route.removePrefix("expensesByMonth:")
                    ExpensesByMonth(month)
                }
                
                route.startsWith("expensesByCategory:") -> {
                    val parts = route.removePrefix("expensesByCategory:").split(":")
                    val categoryId = parts.getOrNull(0) ?: ""
                    val categoryName = parts.drop(1).joinToString(":")
                    ExpensesByCategory(categoryId, categoryName)
                }
                
                route.startsWith("expensesByPaymentMethod:") -> {
                    val parts = route.removePrefix("expensesByPaymentMethod:").split(":")
                    val paymentMethodId = parts.getOrNull(0) ?: ""
                    val paymentMethodName = parts.drop(1).joinToString(":")
                    ExpensesByPaymentMethod(paymentMethodId, paymentMethodName)
                }
                
                route.startsWith("expensesByMonthAndCategory:") -> {
                    val parts = route.removePrefix("expensesByMonthAndCategory:").split(":")
                    val month = parts.getOrNull(0) ?: ""
                    val categoryId = parts.getOrNull(1) ?: ""
                    val categoryName = parts.drop(2).joinToString(":")
                    ExpensesByMonthAndCategory(month, categoryId, categoryName)
                }
                
                route.startsWith("expensesByMonthAndPaymentMethod:") -> {
                    val parts = route.removePrefix("expensesByMonthAndPaymentMethod:").split(":")
                    val month = parts.getOrNull(0) ?: ""
                    val paymentMethodId = parts.getOrNull(1) ?: ""
                    val paymentMethodName = parts.drop(2).joinToString(":")
                    ExpensesByMonthAndPaymentMethod(month, paymentMethodId, paymentMethodName)
                }
                
                route.startsWith("expensesByCategoryAndPaymentMethod:") -> {
                    val parts = route.removePrefix("expensesByCategoryAndPaymentMethod:").split(":")
                    val categoryId = parts.getOrNull(0) ?: ""
                    val categoryName = parts.getOrNull(1) ?: ""
                    val paymentMethodId = parts.getOrNull(2) ?: ""
                    val paymentMethodName = parts.drop(3).joinToString(":")
                    ExpensesByCategoryAndPaymentMethod(categoryId, categoryName, paymentMethodId, paymentMethodName)
                }
                
                route.startsWith("expensesByMonthAndCategoryAndPaymentMethod:") -> {
                    val parts = route.removePrefix("expensesByMonthAndCategoryAndPaymentMethod:").split(":")
                    val month = parts.getOrNull(0) ?: ""
                    val categoryId = parts.getOrNull(1) ?: ""
                    val categoryName = parts.getOrNull(2) ?: ""
                    val paymentMethodId = parts.getOrNull(3) ?: ""
                    val paymentMethodName = parts.drop(4).joinToString(":")
                    ExpensesByMonthAndCategoryAndPaymentMethod(month, categoryId, categoryName, paymentMethodId, paymentMethodName)
                }
                
                route.startsWith("incomeBySource:") -> {
                    val parts = route.removePrefix("incomeBySource:").split(":")
                    val incomeSourceId = parts.getOrNull(0) ?: ""
                    val incomeSourceName = parts.drop(1).joinToString(":")
                    IncomeBySource(incomeSourceId, incomeSourceName)
                }
                
                else -> Dashboard
            }
        }
    }
} 