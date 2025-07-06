package com.neski.pennypincher.ui.expenses

import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.ExpenseRow
import com.neski.pennypincher.ui.components.AddExpenseDialog
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository

import java.util.UUID
import java.util.Calendar
import kotlinx.coroutines.launch

// Material 2 for swipe to dismiss only
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.SwipeToDismiss
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Delete
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditExpenseDialog
import java.util.Date
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.components.DateFilters
import com.neski.pennypincher.ui.theme.getTextColor
import android.util.Log

@SuppressLint("SimpleDateFormat")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ExpensesScreen(
    userId: String, 
    filterMonth: String? = null, 
    onBack: (() -> Unit)? = null, 
    onNavigateToCategory: ((String, String) -> Unit)? = null,
    onNavigateToFilteredExpenses: ((String, String) -> Unit)? = null
) {
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

    var isRefreshing by remember { mutableStateOf(false) }
    
    // Filter state variables
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var availableYears by remember { mutableStateOf<List<Int>>(emptyList()) }
    var availableMonths by remember { mutableStateOf<List<Int>>(emptyList()) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                categories = CategoryRepository.getAllCategories(userId)
                expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
                // Refresh available years and months
                availableYears = expenses.map {
                    val cal = Calendar.getInstance()
                    cal.time = it.date
                    cal.get(Calendar.YEAR)
                }.distinct().sortedDescending()
                selectedYear?.let { year ->
                    availableMonths = expenses.filter {
                        val cal = Calendar.getInstance()
                        cal.time = it.date
                        cal.get(Calendar.YEAR) == year
                    }.map {
                        val cal = Calendar.getInstance()
                        cal.time = it.date
                        cal.get(Calendar.MONTH)
                    }.distinct().sorted()
                }
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(userId) {
        scope.launch {
            categories = CategoryRepository.getAllCategories(userId)
            expenses = ExpenseRepository.getAllExpenses(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            // Build available years from loaded expenses
            availableYears = expenses.map {
                val cal = Calendar.getInstance()
                cal.time = it.date
                cal.get(Calendar.YEAR)
            }.distinct().sortedDescending()
            isLoading = false

            // Debug: Print all years in loaded expenses
            val years = expenses.map {
                val cal = Calendar.getInstance()
                cal.time = it.date
                cal.get(Calendar.YEAR)
            }
            Log.d("ExpensesDebug", "Loaded expense years: $years")
            Log.d("ExpensesDebug", "Available years for filter: $availableYears")
        }
    }
    
    // Update available months when selected year changes
    LaunchedEffect(selectedYear, expenses) {
        scope.launch {
            selectedYear?.let { year ->
                availableMonths = expenses.filter {
                    val cal = Calendar.getInstance()
                    cal.time = it.date
                    cal.get(Calendar.YEAR) == year
                }.map {
                    val cal = Calendar.getInstance()
                    cal.time = it.date
                    cal.get(Calendar.MONTH)
                }.distinct().sorted()
            } ?: run {
                availableMonths = emptyList()
            }
        }
    }

    val year = selectedYear
    val month = selectedMonth
    val filteredExpenses = expenses.filter { expense ->
        val calendar = Calendar.getInstance()
        calendar.time = expense.date
        val expenseYear = calendar.get(Calendar.YEAR)
        val expenseMonth = calendar.get(Calendar.MONTH)
        // Apply year filter
        val yearMatches = year == null || expenseYear == year
        // Apply month filter
        val monthMatches = month == null || expenseMonth == month
        // Apply legacy filterMonth filter if provided
        val legacyMonthMatches = if (filterMonth != null) {
            val m = java.text.SimpleDateFormat("MMM").format(expense.date)
            m == filterMonth
        } else {
            true
        }
        yearMatches && monthMatches && legacyMonthMatches
    }

    // Debug: Print count of filtered expenses for selected year/month
    Log.d("ExpensesDebug", "Filtered expenses for year=$year, month=$month: ${filteredExpenses.size}")

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Expense")
            }
        },
        contentWindowInsets = WindowInsets(top = 2.dp, bottom = 2.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Manage Expenses",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = getTextColor()
                )
                Text(
                    text = "Track your daily spending and keep your finances in order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getTextColor()
                )
                Spacer(Modifier.height(12.dp))
                
                // Expense Filters
                DateFilters(
                    availableYears = availableYears,
                    availableMonths = availableMonths,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onYearSelected = { year ->
                        selectedYear = year
                        selectedMonth = null // Reset month when year changes
                    },
                    onMonthSelected = { month ->
                        selectedMonth = month
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingSpinner(size = 80, showText = true, loadingText = "Loading expenses...")
                    }
                } else if (filteredExpenses.isEmpty()) {
                    Text("No expenses found.", color = getTextColor())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Table header row
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                shape = MaterialTheme.shapes.medium,
                                //color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 1.dp,
                                //border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                    Text("Description\n/Category", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium)
                                    Text("Payment\nMethod", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium)
                                    Text("Amount", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium)
                                    Text("Actions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
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
                                        background = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.Red.copy(alpha = 0.2f))
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        dismissContent = {
                                            ExpenseRow(
                                                expense = expense,
                                                categoryName = categoryName,
                                                paymentMethodName = paymentMethodName,
                                                onEdit = {
                                                    expenseToEdit = expense
                                                    showEditDialog = true
                                                },
                                                onDelete = {
                                                    expenseToDelete = expense
                                                    showConfirmDialog = true
                                                },
                                                onCategoryClick = {
                                                    if (onNavigateToCategory != null && expense.categoryId.isNotBlank()) {
                                                        onNavigateToCategory(expense.categoryId, categoryName)
                                                    }
                                                },
                                                onPaymentMethodClick = {
                                                    if (onNavigateToFilteredExpenses != null && expense.paymentMethodId != null) {
                                                        onNavigateToFilteredExpenses(expense.paymentMethodId, paymentMethodName)
                                                    }
                                                }
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
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
            }
        }
    }
    if (showConfirmDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                expenseToDelete?.let { expense ->
                    scope.launch { dismissStates[expense.id]?.reset() }
                }
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
            onAdd = { description, amount, currency, date, category, paymentMethod, isSubscription, nextDueDate ->
                scope.launch {
                    val newExpense = Expense(
                        id = UUID.randomUUID().toString(),
                        description = description,
                        amount = amount,
                        currencyId = currency,
                        date = date,
                        categoryId = category,
                        paymentMethodId = paymentMethod,
                        isSubscription = isSubscription,
                        nextDueDate = nextDueDate ?: Date()
                    )
                    ExpenseRepository.addExpense(userId, newExpense)
                    expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                    // Rebuild availableYears and availableMonths from new expenses
                    availableYears = expenses.map {
                        val cal = Calendar.getInstance()
                        cal.time = it.date
                        cal.get(Calendar.YEAR)
                    }.distinct().sortedDescending()
                    selectedYear?.let { year ->
                        availableMonths = expenses.filter {
                            val cal = Calendar.getInstance()
                            cal.time = it.date
                            cal.get(Calendar.YEAR) == year
                        }.map {
                            val cal = Calendar.getInstance()
                            cal.time = it.date
                            cal.get(Calendar.MONTH)
                        }.distinct().sorted()
                    } ?: run {
                        availableMonths = emptyList()
                    }
                    // Reset filter if selected year/month are no longer available
                    if (selectedYear != null && selectedYear !in availableYears) {
                        selectedYear = null
                        selectedMonth = null
                    }
                    if (selectedMonth != null && selectedMonth !in availableMonths) {
                        selectedMonth = null
                    }
                    snackbarHostState.showSnackbar("Expense added successfully")
                    Log.d("ExpensesDebug", "After add: years=" + expenses.map {
                        val cal = Calendar.getInstance()
                        cal.time = it.date
                        cal.get(Calendar.YEAR)
                    } + " selectedYear=$selectedYear selectedMonth=$selectedMonth")
                    showDialog = false
                }
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
                    expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
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
                    expenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                    // Rebuild availableYears and availableMonths from new expenses
                    availableYears = expenses.map {
                        val cal = Calendar.getInstance()
                        cal.time = it.date
                        cal.get(Calendar.YEAR)
                    }.distinct().sortedDescending()
                    selectedYear?.let { year ->
                        availableMonths = expenses.filter {
                            val cal = Calendar.getInstance()
                            cal.time = it.date
                            cal.get(Calendar.YEAR) == year
                        }.map {
                            val cal = Calendar.getInstance()
                            cal.time = it.date
                            cal.get(Calendar.MONTH)
                        }.distinct().sorted()
                    } ?: run {
                        availableMonths = emptyList()
                    }
                    // Reset filter if selected year/month are no longer available
                    if (selectedYear != null && selectedYear !in availableYears) {
                        selectedYear = null
                        selectedMonth = null
                    }
                    if (selectedMonth != null && selectedMonth !in availableMonths) {
                        selectedMonth = null
                    }
                    // Debug logging
                    Log.d("ExpensesDebug", "After update: years=" + expenses.map {
                        val cal = Calendar.getInstance()
                        cal.time = it.date
                        cal.get(Calendar.YEAR)
                    } + " availableYears=$availableYears selectedYear=$selectedYear selectedMonth=$selectedMonth")
                    Log.d("ExpensesDebug", "Expense details: " + expenses.joinToString { e ->
                        val cal = Calendar.getInstance()
                        cal.time = e.date
                        "date=" + cal.get(Calendar.YEAR).toString() + "-" + (cal.get(Calendar.MONTH)+1).toString() + " paymentMethod=" + (e.paymentMethodId ?: "null")
                    })
                    showEditDialog = false
                    expenseToEdit = null
                    snackbarHostState.showSnackbar("Expense updated successfully")
                }
            }
        )
    }
}
