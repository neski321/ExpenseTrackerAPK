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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

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

@Composable
fun WebVersionSection() {
    val context = LocalContext.current
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Web Version", style = MaterialTheme.typography.titleMedium)
            Text("Access PennyPincher on the web for a full desktop experience.")
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pennypincherbyneski.vercel.app/dashboard"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Language, contentDescription = "Web Version")
                Spacer(Modifier.width(8.dp))
                Text("Open Web Version")
            }
        }
    }
}

    val netBalance = totalIncome - totalExpenses

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Text("Manage application currencies, account, financial overview, and data.")
        }
        item {
            AccountSettingsSection()
        }
        item {
            FinancialOverviewSection(userId = userId)
        }
        item {
            CurrencyManagementSection(userId = userId)
        }
        item {
            WebVersionSection()
        }
    }
}
