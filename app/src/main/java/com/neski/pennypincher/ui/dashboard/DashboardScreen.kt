package com.neski.pennypincher.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.StatCard
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import com.neski.pennypincher.ui.components.SpendingBarChartSection


@Composable
fun DashboardScreen(
    userId: String,
    onNavigateToExpenses: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var totalSpentThisMonth by remember { mutableStateOf(0.0) }
    var weeklyTransactionCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var monthlySpending by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }


    LaunchedEffect(userId) {
        scope.launch {
            totalSpentThisMonth = ExpenseRepository.getThisMonthTotal(userId)
            weeklyTransactionCount = ExpenseRepository.getThisWeekCount(userId)
            monthlySpending = ExpenseRepository.getMonthlySpending(userId)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

            SpendingBarChartSection(data = monthlySpending)
        }
    }
}