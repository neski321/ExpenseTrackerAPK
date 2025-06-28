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

import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditExpenseDialog
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

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
    onNavigateToCategory: ((String, String) -> Unit)? = null
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

    LaunchedEffect(userId, month, categoryId, paymentMethodId) {
        scope.launch {
            val allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
            categories = CategoryRepository.getAllCategories(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            val filtered = when {
                month != null -> {
                    val parts = month.split(" ")
                    val monthNum = try {
                        java.text.DateFormatSymbols().months.indexOfFirst { it.startsWith(parts[0], ignoreCase = true) }
                    } catch (e: Exception) { -1 }
                    val yearNum = parts.getOrNull(1)?.toIntOrNull() ?: -1
                    if (monthNum >= 0 && yearNum > 0) {
                        allExpenses.filter {
                            val cal = java.util.Calendar.getInstance().apply { time = it.date }
                            cal.get(java.util.Calendar.MONTH) == monthNum && cal.get(java.util.Calendar.YEAR) == yearNum
                        }
                    } else emptyList()
                }
                categoryId != null -> {
                    val allCategoryIds = mutableSetOf(categoryId)
                    fun findChildren(parentId: String) {
                        categories.filter { it.parentId == parentId }.forEach {
                            allCategoryIds.add(it.id)
                            findChildren(it.id)
                        }
                    }
                    findChildren(categoryId)
                    allExpenses.filter { it.categoryId in allCategoryIds }
                }
                paymentMethodId != null -> {
                    allExpenses.filter { it.paymentMethodId == paymentMethodId }
                }
                else -> emptyList()
            }
            expenses = filtered
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
            // Build the path from root to selected category (outside composable lambda)
            val categoryPath = if (categoryId != null && categoryName != null) {
                val path = mutableListOf<Category>()
                var cat = categories.find { it.id == categoryId }
                while (cat != null) {
                    path.add(cat)
                    cat = cat.parentId?.let { parentId -> categories.find { it.id == parentId } }
                }
                path.reverse()
                path
            } else null

            val titleComposable: @Composable () -> Unit = when {
                categoryPath != null && categoryPath.isNotEmpty() -> {
                    @Composable {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Expenses for ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                categoryPath.forEachIndexed { idx, c ->
                                    if (idx > 0) {
                                        Text(" > ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                                    }
                                    if (idx < categoryPath.size - 1 && onNavigateToCategory != null) {
                                        Text(
                                            text = c.name,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.clickable { onNavigateToCategory(c.id, c.name) }
                                        )
                                    } else {
                                        Text(
                                            text = c.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                            Text(
                                "Showing expenses recorded for this category and its sub-categories across all time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                month != null -> {
                    @Composable {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            Text("Expenses for $month", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "View your expenses for this period.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                paymentMethodName != null -> {
                    @Composable {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            Text("Expenses for $paymentMethodName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "View your expenses for this payment method.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                else -> {
                    @Composable { Text("Filtered Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                }
            }
            if (onBack != null) {
                TopAppBar(
                    title = { titleComposable() },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(title = { titleComposable() })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (expenses.isEmpty()) {
                val filterType = when {
                    month != null -> "this month"
                    categoryName != null -> "this category"
                    paymentMethodName != null -> "this payment method"
                    else -> "the selected filter"
                }
                Text("No expenses found for $filterType.")
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
