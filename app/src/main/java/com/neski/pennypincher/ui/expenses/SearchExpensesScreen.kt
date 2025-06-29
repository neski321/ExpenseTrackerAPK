package com.neski.pennypincher.ui.expenses

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.EditExpenseDialog
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import kotlinx.coroutines.launch

// Material 2 for swipe to dismiss only
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDismissState
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.ExpenseRow
import com.neski.pennypincher.ui.components.LoadingSpinner

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.neski.pennypincher.ui.theme.getTextColor

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchExpensesScreen(userId: String, onNavigateToCategory: ((String, String) -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    var allExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }

    var showConfirmDialog by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var selectedPaymentMethodId by remember { mutableStateOf<String?>(null) }
    var allPaymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var paymentExpanded by remember { mutableStateOf(false) }

    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var allCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val categoryMap = allCategories.associateBy({ it.id }, { it.name })
    val paymentMethodMap = allPaymentMethods.associateBy({ it.id }, { it.name })

    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }

    val snackbarHostState = remember { SnackbarHostState() }

    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    var query by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        scope.launch {
            try {
                isLoading = true
                allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                allCategories = CategoryRepository.getAllCategories(userId)
                allPaymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Helper to get all descendant category IDs (including the selected one)
    fun getAllDescendantCategoryIds(categoryId: String, categories: List<Category>): Set<String> {
        val directChildren = categories.filter { it.parentId == categoryId }
        return directChildren.fold(mutableSetOf(categoryId)) { acc, child ->
            acc.addAll(getAllDescendantCategoryIds(child.id, categories))
            acc
        }
    }

    val filteredExpenses = allExpenses.filter { expense ->
        val matchesText = query.isBlank() || expense.description.contains(query, ignoreCase = true)
        val selectedCategoryValue = selectedCategory
        val categoryIdsToMatch = if (selectedCategoryValue == null) null else getAllDescendantCategoryIds(selectedCategoryValue, allCategories)
        val matchesCat = categoryIdsToMatch == null || categoryIdsToMatch.contains(expense.categoryId)
        val matchesPay = selectedPaymentMethodId == null || expense.paymentMethodId == selectedPaymentMethodId

        val matchesAmount = runCatching {
            (minAmount.isBlank() || expense.amount >= minAmount.toDouble()) &&
                    (maxAmount.isBlank() || expense.amount <= maxAmount.toDouble())
        }.getOrDefault(true)

        val matchesDate = runCatching {
            val localDate = expense.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            (startDate == null || !localDate.isBefore(startDate)) &&
                    (endDate == null || !localDate.isAfter(endDate))
        }.getOrDefault(true)

        matchesText && matchesCat && matchesPay && matchesAmount && matchesDate
    }

    fun deleteExpense(expense: Expense) {
        scope.launch {
            try {
                ExpenseRepository.deleteExpense(userId, expense.id)
                allExpenses = allExpenses.filterNot { it.id == expense.id }
                snackbarHostState.showSnackbar("Expense deleted")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to delete expense")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(top = 2.dp, bottom = 2.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Search & Filter Expenses",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = getTextColor()
            )
            Text(
                text = "Refine your expense list using the filters below.",
                style = MaterialTheme.typography.bodyMedium,
                color = getTextColor()
            )
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingSpinner(size = 80, showText = true, loadingText = "Loading search results...")
                }
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    val selectedName = allCategories.find { it.id == selectedCategory }?.name
                        ?: "All Categories"

                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                selectedCategory = null
                                categoryExpanded = false
                            }
                        )

                        val grouped = allCategories
                            .sortedBy { it.name }
                            .groupBy { parent ->
                                allCategories.find { it.id == parent.parentId }?.name ?: "Top-Level"
                            }

                        grouped.forEach { (groupName, children) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                enabled = false,
                                onClick = {}
                            )

                            children.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded }
                ) {
                    val selectedPaymentName = allPaymentMethods.find { it.id == selectedPaymentMethodId }?.name
                        ?: "All Payments"

                    OutlinedTextField(
                        value = selectedPaymentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(paymentExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Payments") },
                            onClick = {
                                selectedPaymentMethodId = null
                                paymentExpanded = false
                            }
                        )
                        allPaymentMethods.sortedBy { it.name }.forEach {
                            DropdownMenuItem(
                                text = { Text(it.name) },
                                onClick = {
                                    selectedPaymentMethodId = it.id
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { minAmount = it },
                    label = { Text("Min $") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { maxAmount = it },
                    label = { Text("Max $") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showStartPicker = true }) {
                    Text(startDate?.format(dateFormatter) ?: "Start Date")
                }

                OutlinedButton(onClick = { showEndPicker = true }) {
                    Text(endDate?.format(dateFormatter) ?: "End Date")
                }
            }

            if (showStartPicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            startDatePickerState.selectedDateMillis?.let {
                                startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            showStartPicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartPicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(
                        state = startDatePickerState,
                        title = { Text("Select Start Date") }
                    )
                }
            }

            if (showEndPicker) {
                DatePickerDialog(
                    onDismissRequest = { showEndPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            endDatePickerState.selectedDateMillis?.let {
                                endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            showEndPicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndPicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(
                        state = endDatePickerState,
                        title = { Text("Select End Date") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = {
                query = ""
                selectedCategory = null
                selectedPaymentMethodId = null
                minAmount = ""
                maxAmount = ""
                startDate = null
                endDate = null
                startDatePickerState.selectedDateMillis = null
                endDatePickerState.selectedDateMillis = null
            }) {
                Text("Clear All Filters")
            }

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
                    Text("Date", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    Text("Description\n/Category", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    Text("Payment\nMethod", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    Text("Amount", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    Text("Actions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                                onDelete = {
                                    expenseToDelete = expense
                                    showConfirmDialog = true
                                },
                                onCategoryClick = {
                                    if (onNavigateToCategory != null && expense.categoryId.isNotBlank()) {
                                        onNavigateToCategory(expense.categoryId, categoryName)
                                    }
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
                showEditDialog = false
                expenseToEdit = null
                scope.launch {
                    ExpenseRepository.updateExpense(userId, updatedExpense)
                    allExpenses = ExpenseRepository.getAllExpenses(userId, forceRefresh = true)
                    snackbarHostState.showSnackbar("Expense updated")
                }
            }
        )
    }
}
