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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    userId: String,
    onNavigateToExpenses: () -> Unit,
    onNavigateToExpensesByMonth: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var totalSpentThisMonth by remember { mutableDoubleStateOf(0.0) }
    var weeklyTransactionCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var monthlySpending by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                totalSpentThisMonth = ExpenseRepository.getThisMonthTotal(userId, forceRefresh = true)
                weeklyTransactionCount = ExpenseRepository.getThisWeekCount(userId, forceRefresh = true)
                monthlySpending = ExpenseRepository.getMonthlySpending(userId, forceRefresh = true)
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
                CircularProgressIndicator()
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
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
                        onClick = onNavigateToExpenses // 🔗 click to expenses page
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Spending Overview", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SpendingBarChartSection(
                    data = monthlySpending,
                    modifier = Modifier.fillMaxWidth(),
                    onBarClick = { month ->
                        onNavigateToExpensesByMonth(month)
                    }
                )
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
