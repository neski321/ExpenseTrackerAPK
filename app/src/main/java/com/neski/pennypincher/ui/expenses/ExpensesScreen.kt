package com.neski.pennypincher.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Snackbar
//import androidx.compose.material3.SnackbarData

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
import com.neski.pennypincher.ui.components.EditExpenseDialog
import java.util.Date
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ExpensesScreen(userId: String, filterMonth: String? = null, onBack: (() -> Unit)? = null) {
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
    var showEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseBeingEdited by remember { mutableStateOf<Expense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var page by remember { mutableStateOf(0) }
    val pageSize = 20
    var isLoadingMore by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                categories = CategoryRepository.getAllCategories(userId)
                expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
                val pageOne = ExpenseRepository.getExpensesByPage(userId, pageSize, 0)
                expenses = pageOne
                page = 0
                isRefreshing = false
            }
        }
    )

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

    val filteredExpenses = if (filterMonth != null) {
        expenses.filter { expense ->
            val month = java.text.SimpleDateFormat("MMM").format(expense.date)
            month == filterMonth
        }
    } else {
        expenses
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
            if (filterMonth != null && onBack != null) {
                TopAppBar(
                    title = { Text("Expenses for $filterMonth") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Manage Expenses") }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Expense")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                } else if (filteredExpenses.isEmpty()) {
                    Text("No expenses found.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredExpenses, key = { it.id }) { expense ->
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
                                directions = setOf(DismissDirection.EndToStart),
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
                    ExpenseRepository.addExpense(userId, newExpense)
                    expenses = ExpenseRepository.getAllExpenses(userId)
                    snackbarHostState.showSnackbar("Expense added successfully")
                }
                showDialog = false
            }
        )
    }

    expenseBeingEdited?.let { expense ->
        EditExpenseDialog(
            userId = userId,
            expense = expense,
            onDismiss = { expenseBeingEdited = null },
            onUpdate = { updatedExpense ->
                scope.launch {
                    ExpenseRepository.updateExpense(userId, updatedExpense)
                    expenses = ExpenseRepository.getAllExpenses(userId)
                    expenseBeingEdited = null
                    snackbarHostState.showSnackbar("Expense updated successfully")
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
            onUpdate = { updatedExpense ->
                scope.launch {
                    ExpenseRepository.addExpense(userId, updatedExpense)
                    expenses = ExpenseRepository.getExpensesByPage(userId, 20, 0)
                    showEditDialog = false
                    expenseToEdit = null
                    snackbarHostState.showSnackbar("Expense updated successfully")
                }
            }
        )
    }
}
