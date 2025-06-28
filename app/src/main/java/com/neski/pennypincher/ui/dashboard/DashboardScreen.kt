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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import com.google.firebase.auth.FirebaseAuth
import com.neski.pennypincher.ui.components.AddExpenseDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    userId: String,
    onNavigate: (String) -> Unit,
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
                // Quick Actions Card
                var showAddExpenseDialog by remember { mutableStateOf(false) }
                if (showAddExpenseDialog) {
                    AddExpenseDialog(
                        userId = userId,
                        onDismiss = { showAddExpenseDialog = false },
                        onAdd = { _, _, _, _, _, _, _, _ -> showAddExpenseDialog = false }
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
                            Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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
