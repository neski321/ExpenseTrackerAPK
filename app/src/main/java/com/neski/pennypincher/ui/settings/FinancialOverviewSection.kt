package com.neski.pennypincher.ui.settings

import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.data.repository.IncomeRepository
import kotlinx.coroutines.launch

@SuppressLint("DefaultLocale")
@Composable
fun FinancialOverviewSection(userId: String) {
    val scope = rememberCoroutineScope()

    var totalIncome by remember { mutableStateOf(0.0) }
    var totalExpenses by remember { mutableStateOf(0.0) }

    LaunchedEffect(userId) {
        scope.launch {
            totalIncome = IncomeRepository.getTotalIncome(userId)
            totalExpenses = ExpenseRepository.getTotalExpenses(userId)
        }
    }

    val netBalance = totalIncome - totalExpenses

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("All-Time Financial Overview", style = MaterialTheme.typography.titleMedium)
            Text("Your total income, expenses, and net balance (in base currency).")

            Text(
                text = "Total Income: $${String.format("%.2f", totalIncome)}",
                color = Color(0xFF4CAF50) // Green
            )

            Text(
                text = "Total Expenses: $${String.format("%.2f", totalExpenses)}",
                color = Color(0xFFF44336) // Red
            )

            Text(
                text = "Net Balance: $${String.format("%.2f", netBalance)}",
                color = if (netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
