package com.neski.pennypincher.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.ExpenseRow
import com.neski.pennypincher.ui.components.AddExpenseDialog
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository

import java.util.UUID
import kotlinx.coroutines.launch

// Material 2 for swipe to dismiss only
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
//import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDismissState
import java.util.Date

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@Composable
fun ExpensesScreen(userId: String) {
    val scope = rememberCoroutineScope()
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val categoryMap = categories.associateBy({ it.id }, { it.name })
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    val paymentMethodMap = paymentMethods.associateBy({ it.id }, { it.name })
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }

    var page by remember { mutableStateOf(0) }
    val pageSize = 20
    var isLoadingMore by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        scope.launch {
            categories = CategoryRepository.getAllCategories(userId)
            expenses = ExpenseRepository.getAllExpenses(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            val pageOne = ExpenseRepository.getExpensesByPage(userId, pageSize, 0)
            expenses = pageOne
            page = 0
            isLoading = false
        }
    }

    fun deleteExpense(expense: Expense) {
        scope.launch {
            // 🔥 Replace with actual Firestore delete if needed
            expenses = expenses.filterNot { it.id == expense.id }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Manage Expenses",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (expenses.isEmpty()) {
                Text("No expenses found.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                                    onEdit = { /* TODO */ },
                                    onDelete = { deleteExpense(expense) }
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        if (!isLoadingMore) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoadingMore = true
                                        val nextPage = ExpenseRepository.getExpensesByPage(userId, pageSize, page + 1)
                                        if (nextPage.isNotEmpty()) {
                                            expenses = expenses + nextPage
                                            page += 1
                                        }
                                        isLoadingMore = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text("Load More")
                            }
                        }
                    }
                }
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

    if (showDialog) {
        AddExpenseDialog(
            userId = userId,
            onDismiss = { showDialog = false },
            onAdd = { desc, amt, curr, date, cat, method, isSub, nextDue ->
                val newExpense = Expense(
                    id = UUID.randomUUID().toString(),
                    description = desc,
                    amount = amt,
                    currencyId = curr,
                    date = date,
                    categoryId = cat,
                    paymentMethodId = method,
                    isSubscription = isSub,
                    nextDueDate = nextDue ?: Date() // ✅ add this
                )
                scope.launch {
                    ExpenseRepository.addExpense(newExpense)
                    expenses = ExpenseRepository.getAllExpenses(userId)
                }
                showDialog = false
            }
        )
    }

}
