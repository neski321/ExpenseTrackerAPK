package com.neski.pennypincher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.CurrencyRepository
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.data.repository.IncomeRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(userId: String) {
    val scope = rememberCoroutineScope()

    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }
    var totalIncome by remember { mutableStateOf(0.0) }
    var totalExpenses by remember { mutableStateOf(0.0) }

    LaunchedEffect(userId) {
        scope.launch {
            currencies = CurrencyRepository.getAllCurrencies(userId, true)
            totalIncome = IncomeRepository.getTotalIncome(userId)
            totalExpenses = ExpenseRepository.getTotalExpenses(userId)
        }
    }

    val netBalance = totalIncome - totalExpenses

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Manage application currencies, account, financial overview, and data.")

        AccountSettingsSection()
        FinancialOverviewSection(userId = userId)
        CurrencyManagementSection(userId = userId)
    }
}
