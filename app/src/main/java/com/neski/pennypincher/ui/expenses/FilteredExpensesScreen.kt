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
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import androidx.compose.material.icons.Icons


//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.SwipeToDismiss
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.automirrored.filled.ArrowBack
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditExpenseDialog
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor
import com.neski.pennypincher.ui.components.DateFilters
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilteredExpensesScreen(
    userId: String,
    month: String? = null,
    categoryId: String? = null,
    categoryName: String? = null,
    paymentMethodId: String? = null,
    paymentMethodName: String? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToCategory: ((String, String) -> Unit)? = null,
    onNavigateToFilteredExpenses: ((String, String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    val categoryMap = categories.associateBy({ it.id }, { it.name })
    val paymentMethodMap = paymentMethods.associateBy({ it.id }, { it.name })

    // Find current category and its parent if it's a child category
    val currentCategory = categories.find { it.id == categoryId }
    val parentCategory = currentCategory?.parentId?.let { parentId ->
        categories.find { it.id == parentId }
    }

    //val formatter = DateTimeFormatter.ofPattern("MMMM")

    var showConfirmDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }
    var showEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Date filter state
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var availableYears by remember { mutableStateOf<List<Int>>(emptyList()) }
    var availableMonths by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun filterExpenses(
        allExpenses: List<Expense>,
        categories: List<Category>,
        month: String?,
        categoryId: String?,
        paymentMethodId: String?
    ): List<Expense> {
        return allExpenses.filter { expense ->
            val matchesMonth = month?.let {
                val parts = it.split(" ")
                val monthNum = try {
                    java.text.DateFormatSymbols().months.indexOfFirst { m -> m.startsWith(parts[0], ignoreCase = true) }
                } catch (e: Exception) { -1 }
                val yearNum = parts.getOrNull(1)?.toIntOrNull() ?: -1
                if (monthNum >= 0 && yearNum > 0) {
                    val cal = java.util.Calendar.getInstance().apply { time = expense.date }
                    cal.get(java.util.Calendar.MONTH) == monthNum && cal.get(java.util.Calendar.YEAR) == yearNum
                } else false
            } ?: true
            val matchesCategory = categoryId?.let {
                val allCategoryIds = mutableSetOf(it)
                fun findChildren(parentId: String) {
                    categories.filter { c -> c.parentId == parentId }.forEach { c ->
                        allCategoryIds.add(c.id)
                        findChildren(c.id)
                    }
                }
                findChildren(it)
                allCategoryIds.contains(expense.categoryId)
            } ?: true
            val matchesPayment = paymentMethodId?.let { expense.paymentMethodId == it } ?: true
            matchesMonth && matchesCategory && matchesPayment
        }
    }

    LaunchedEffect(userId, month, categoryId, paymentMethodId) {
        scope.launch {
            val allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
            categories = CategoryRepository.getAllCategories(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            expenses = filterExpenses(allExpenses, categories, month, categoryId, paymentMethodId)
            isLoading = false

            // Build available years from loaded/filtered expenses
            availableYears = expenses.map {
                val cal = java.util.Calendar.getInstance()
                cal.time = it.date
                cal.get(java.util.Calendar.YEAR)
            }.distinct().sortedDescending()
        }
    }

    // Update available months when selected year or expenses change
    LaunchedEffect(selectedYear, expenses) {
        scope.launch {
            selectedYear?.let { year ->
                availableMonths = expenses.filter {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it.date
                    cal.get(java.util.Calendar.YEAR) == year
                }.map {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it.date
                    cal.get(java.util.Calendar.MONTH)
                }.distinct().sorted()
            } ?: run {
                availableMonths = emptyList()
            }
        }
    }

    // Filter expenses by selected year/month
    val filteredExpenses = expenses.filter { expense ->
        val calendar = java.util.Calendar.getInstance()
        calendar.time = expense.date
        val expenseYear = calendar.get(java.util.Calendar.YEAR)
        val expenseMonth = calendar.get(java.util.Calendar.MONTH)
        val yearMatches = selectedYear == null || expenseYear == selectedYear
        val monthMatches = selectedMonth == null || expenseMonth == selectedMonth
        yearMatches && monthMatches
    }

    fun deleteExpense(expense: Expense) {
        scope.launch {
            try {
                ExpenseRepository.deleteExpense(userId, expense.id)
                // Force refresh all data to ensure consistency
                snackbarHostState.showSnackbar("Expense deleted successfully")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to delete expense")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                // Breadcrumb and back button
                if (onBack != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back to Expenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Breadcrumb for category navigation
                if (categoryName != null && parentCategory != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = parentCategory.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onNavigateToCategory?.invoke(parentCategory.id, parentCategory.name)
                            }
                        )
                        Text(
                            text = " > ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Title and subtitle (optional, can be dynamic based on filter)
                Text(
                    text = when {
                        month != null -> "Expenses for $month"
                        categoryName != null -> "Expenses for $categoryName"
                        paymentMethodName != null -> "Expenses for $paymentMethodName"
                        else -> "Filtered Expenses"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = getTextColor()
                )
                Text(
                    text = when {
                        month != null -> "View your expenses for this period."
                        categoryName != null -> "Showing expenses recorded for this category and its sub-categories across all time."
                        paymentMethodName != null -> "View your expenses for this payment method."
                        else -> "Track your filtered spending."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = getTextColor()
                )
                Spacer(Modifier.height(12.dp))
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingSpinner(size = 80, showText = true, loadingText = "Loading filtered expenses...")
                    }
                } else if (filteredExpenses.isEmpty()) {
                    Text("No expenses found.", color = getTextColor())
                } else {
                    // Date filter UI
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
                        label = "Filter Expenses"
                    )
                    // Table header row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Description\n/Category", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Payment\nMethod", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Amount", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Actions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 50.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExpenses, key = { it.id }) { expense ->
                            val dismissState = dismissStates.getOrPut(expense.id) { rememberDismissState() }
                            val categoryNameResolved = categoryMap[expense.categoryId] ?: "Unknown"
                            val paymentMethodNameResolved = paymentMethodMap[expense.paymentMethodId] ?: "N/A"

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
                                        categoryName = categoryNameResolved,
                                        paymentMethodName = paymentMethodNameResolved,
                                        onEdit = {
                                            expenseToEdit = expense
                                            showEditDialog = true
                                        },
                                        onDelete = { deleteExpense(expense) },
                                        onCategoryClick = {
                                            onNavigateToCategory?.invoke(expense.categoryId, categoryNameResolved)
                                        },
                                        onPaymentMethodClick = {
                                            onNavigateToFilteredExpenses?.invoke(expense.paymentMethodId ?: "", paymentMethodNameResolved)
                                        }
                                    )
                                }
                            )
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
                    title = { Text("Delete Expense", color = getTextColor()) },
                    text = { Text("Are you sure you want to delete this expense?", color = getTextColor()) },
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
                            Text("Cancel", color = getTextColor())
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
                            ExpenseRepository.updateExpense(userId, updatedExpense)
                            val allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                            categories = CategoryRepository.getAllCategories(userId, forceRefresh = true)
                            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId, forceRefresh = true)
                            expenses = filterExpenses(allExpenses, categories, month, categoryId, paymentMethodId)
                            // Rebuild availableYears and availableMonths from new expenses
                            availableYears = expenses.map {
                                val cal = java.util.Calendar.getInstance()
                                cal.time = it.date
                                cal.get(java.util.Calendar.YEAR)
                            }.distinct().sortedDescending()
                            selectedYear?.let { year ->
                                availableMonths = expenses.filter {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.time = it.date
                                    cal.get(java.util.Calendar.YEAR) == year
                                }.map {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.time = it.date
                                    cal.get(java.util.Calendar.MONTH)
                                }.distinct().sorted()
                            } ?: run {
                                availableMonths = emptyList()
                            }
                            showEditDialog = false
                            expenseToEdit = null
                            snackbarHostState.showSnackbar("Expense updated successfully")
                        }
                    }
                )
            }
        }
    }
}

