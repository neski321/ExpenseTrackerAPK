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
import java.time.format.DateTimeFormatter
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditExpenseDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilteredExpensesScreen(userId: String, month: String, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    val categoryMap = categories.associateBy({ it.id }, { it.name })
    val paymentMethodMap = paymentMethods.associateBy({ it.id }, { it.name })

    //val formatter = DateTimeFormatter.ofPattern("MMMM")

    var showConfirmDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }
    var showEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId, month) {
        scope.launch {
            val allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
            // Parsing month and year from the bar label ('Jan 2024')
            val parts = month.split(" ")
            val monthNum = try {
                java.text.DateFormatSymbols().months.indexOfFirst { it.startsWith(parts[0], ignoreCase = true) }
            } catch (e: Exception) { -1 }
            val yearNum = parts.getOrNull(1)?.toIntOrNull() ?: -1
            expenses = if (monthNum >= 0 && yearNum > 0) {
                allExpenses.filter {
                    val cal = java.util.Calendar.getInstance().apply { time = it.date }
                    cal.get(java.util.Calendar.MONTH) == monthNum && cal.get(java.util.Calendar.YEAR) == yearNum
                }
            } else emptyList()
            categories = CategoryRepository.getAllCategories(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            isLoading = false
        }
    }

    fun deleteExpense(expense: Expense) {
        scope.launch {
            try {
                ExpenseRepository.deleteExpense(userId, expense.id)
                expenses = expenses.filterNot { it.id == expense.id }
                snackbarHostState.showSnackbar("Expense deleted")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to delete expense")
            }
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Expenses for $month") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(title = { Text("Expenses for $month") })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (expenses.isEmpty()) {
                Text("No expenses found for $month.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenses, key = { it.id }) { expense ->
                        val dismissState = dismissStates.getOrPut(expense.id) { rememberDismissState() }
                        val categoryName = categoryMap[expense.categoryId] ?: "Unknown"
                        val paymentMethodName = paymentMethodMap[expense.paymentMethodId] ?: "N/A"

                        LaunchedEffect(dismissState.currentValue) {
                            if (
                                dismissState.isDismissed(DismissDirection.EndToStart) ||
                                dismissState.isDismissed(DismissDirection.StartToEnd)
                            ) {
                                expenseToDelete = expense
                                showConfirmDialog = true
                            }
                        }

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                            background = {},
                            dismissContent = {
                                ExpenseRow(
                                    expense = expense,
                                    categoryName = categoryName,
                                    paymentMethodName = paymentMethodName,
                                    onEdit = {
                                        expenseToEdit = expense
                                        showEditDialog = true
                                    },
                                    onDelete = { deleteExpense(expense) }
                                )
                            }
                        )
                    }
                }
            }
            if (showConfirmDialog && expenseToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmDialog = false
                        expenseToDelete = null
                    },
                    title = { Text("Delete Expense") },
                    text = { Text("Are you sure you want to delete this expense?") },
                    confirmButton = {
                        TextButton(onClick = {
                            deleteExpense(expenseToDelete!!)
                            showConfirmDialog = false
                            expenseToDelete = null
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            scope.launch {
                                dismissStates[expenseToDelete?.id]?.reset()
                                showConfirmDialog = false
                                expenseToDelete = null
                            }
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showEditDialog && expenseToEdit != null) {
                EditExpenseDialog(
                    userId = userId,
                    expense = expenseToEdit!!,
                    onDismiss = {
                        showEditDialog = false
                        expenseToEdit = null
                    },
                    onUpdate = {
                        showEditDialog = false
                        expenseToEdit = null
                        scope.launch {
                            expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                        }
                    }
                )
            }
        }
    }
}
