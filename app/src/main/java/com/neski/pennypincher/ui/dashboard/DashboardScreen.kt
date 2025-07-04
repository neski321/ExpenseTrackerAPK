package com.neski.pennypincher.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.StatCard
import com.neski.pennypincher.ui.components.SpendingBarChartSection
import kotlinx.coroutines.launch
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.graphics.Color
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.models.Category
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import com.neski.pennypincher.ui.components.AddExpenseDialog
import com.neski.pennypincher.ui.components.SpendingPieChartSection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.graphics.toColorInt
import com.neski.pennypincher.ui.components.LoadingSpinner
import java.util.UUID
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    userId: String,
    onNavigate: (String) -> Unit,
    onNavigateToExpensesByMonth: (String) -> Unit = {},
    categoryStack: List<Pair<String, String>> = emptyList(),
    setCategoryOriginRoute: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    var totalSpentThisMonth by remember { mutableDoubleStateOf(0.0) }
    var weeklyTransactionCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var monthlySpending by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                totalSpentThisMonth = ExpenseRepository.getThisMonthTotal(userId, forceRefresh = true)
                weeklyTransactionCount = ExpenseRepository.getThisWeekCount(userId, forceRefresh = true)
                monthlySpending = ExpenseRepository.getMonthlySpending(userId, forceRefresh = true)
                categories = CategoryRepository.getAllCategories(userId, forceRefresh = true)
                expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                isRefreshing = false
            }
        }
    )

    fun loadData(forceRefresh: Boolean = false) {
        scope.launch {
            if (forceRefresh) isRefreshing = true
            totalSpentThisMonth = ExpenseRepository.getThisMonthTotal(userId, forceRefresh)
            weeklyTransactionCount = ExpenseRepository.getThisWeekCount(userId, forceRefresh)
            monthlySpending = ExpenseRepository.getMonthlySpending(userId, forceRefresh)
            categories = CategoryRepository.getAllCategories(userId, forceRefresh)
            expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh)
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(userId) {
        loadData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingSpinner(size = 80, showText = true, loadingText = "Loading dashboard...")
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Quick Actions Card
                var showAddExpenseDialog by remember { mutableStateOf(false) }
                if (showAddExpenseDialog) {
                    AddExpenseDialog(
                        userId = userId,
                        onDismiss = { showAddExpenseDialog = false },
                        onAdd = { description, amount, currency, date, category, paymentMethod, isSubscription, nextDueDate ->
                            scope.launch {
                                val newExpense = Expense(
                                    id = UUID.randomUUID().toString(),
                                    description = description,
                                    amount = amount,
                                    currencyId = currency,
                                    date = date,
                                    categoryId = category,
                                    paymentMethodId = paymentMethod,
                                    isSubscription = isSubscription,
                                    nextDueDate = nextDueDate ?: Date()
                                )
                                ExpenseRepository.addExpense(userId, newExpense)
                                // Force refresh all dashboard data
                                loadData(forceRefresh = true)
                                showAddExpenseDialog = false
                            }
                        }
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Manage your finances efficiently.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { showAddExpenseDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text("Add New Expense", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onNavigate("categories") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text("Manage Categories", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "This Month",
                        value = "$${"%.2f".format(totalSpentThisMonth)}"
                    )
                    StatCard(
                        title = "This Week",
                        value = "$weeklyTransactionCount transactions",
                        onClick = { onNavigate("expenses") }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Bar chart first
                Text("Spending Overview", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SpendingBarChartSection(
                    data = monthlySpending,
                    modifier = Modifier.fillMaxWidth(),
                    onBarClick = { month ->
                        onNavigateToExpensesByMonth(month)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Pie chart data prep
                val categorySpending = remember(expenses, categories) {
                    expenses.groupBy { it.categoryId }
                        .mapNotNull { (catId, exps) ->
                            val cat = categories.find { it.id == catId }
                            val total = exps.sumOf { it.amount }
                            if (cat != null && total > 0) cat.id to total else null
                        }
                        .toMap()
                }
                val categoryColorMap = remember(categories) {
                    val defaultColors = listOf(
                        Color(0xFF957DAD), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
                        Color(0xFFE57373), Color(0xFFFFD54F), Color(0xFF4DD0E1), Color(0xFFBA68C8),
                        Color(0xFFA1887F), Color(0xFF90A4AE), Color(0xFF388E3C), Color(0xFFD32F2F),
                        Color(0xFF00B8D4), Color(0xFF8D6E63), Color(0xFF43A047), Color(0xFF6D4C41),
                        Color(0xFF0288D1), Color(0xFFAD1457), Color(0xFF0097A7), Color(0xFF7B1FA2),
                        Color(0xFFFBC02D), Color(0xFF388E3C), Color(0xFF1976D2), Color(0xFF5D4037)
                    )
                    var colorIdx = 0
                    buildMap {
                        categories.forEach { cat ->
                            val color = try {
                                if (cat.color.isNotBlank()) Color(cat.color.toColorInt())
                                else defaultColors[colorIdx++ % defaultColors.size]
                            } catch (_: Exception) {
                                defaultColors[colorIdx++ % defaultColors.size]
                            }
                            put(cat.id, color)
                        }
                    }
                }
                SpendingPieChartSection(
                    data = categorySpending,
                    colorMap = categoryColorMap,
                    modifier = Modifier.fillMaxWidth(),
                    onSliceClick = { categoryId ->
                        val cat = categories.find { it.id == categoryId }
                        if (cat != null) {
                            setCategoryOriginRoute?.invoke("dashboard")
                            onNavigate("expensesByCategory:${cat.id}:${cat.name}")
                        }
                    },
                    categoryNames = categories.associate { it.id to it.name }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            scale = true,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
