package com.neski.pennypincher.ui.expenses

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.ExpenseRow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilteredExpensesScreen(userId: String, month: String) {
    val scope = rememberCoroutineScope()
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    val categoryMap = categories.associateBy({ it.id }, { it.name })
    val paymentMethodMap = paymentMethods.associateBy({ it.id }, { it.name })

    val formatter = DateTimeFormatter.ofPattern("MMMM")

    LaunchedEffect(userId, month) {
        scope.launch {
            val allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
            val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            expenses = allExpenses.filter {
                format.format(it.date) == month
            }
            categories = CategoryRepository.getAllCategories(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Expenses for $month") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (expenses.isEmpty()) {
                Text("No expenses found for $month.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenses) { expense ->
                        val categoryName = categoryMap[expense.categoryId] ?: "Unknown"
                        val paymentMethodName = paymentMethodMap[expense.paymentMethodId] ?: "N/A"
                        ExpenseRow(
                            expense = expense,
                            categoryName = categoryName,
                            paymentMethodName = paymentMethodName
                        )
                    }
                }
            }
        }
    }
}
