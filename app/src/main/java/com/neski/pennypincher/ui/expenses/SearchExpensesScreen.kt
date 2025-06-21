package com.neski.pennypincher.ui.expenses

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.repository.ExpenseRepository
import com.neski.pennypincher.ui.components.ExpenseCard
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import kotlinx.coroutines.launch

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchExpensesScreen(userId: String) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    var allExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }

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

    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    var allCategories by remember { mutableStateOf<List<Category>>(emptyList()) }

    var query by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        scope.launch {
            try {
                isLoading = true
                allExpenses = ExpenseRepository.getAllExpenses(userId)
                allCategories = CategoryRepository.getAllCategories(userId)
                allPaymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val filteredExpenses = allExpenses.filter { expense ->
        val matchesText = query.isBlank() || expense.description.contains(query, ignoreCase = true)
        val matchesCat = selectedCategory == null || selectedCategory == expense.categoryId
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Expenses") },
                actions = {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            )
        }
    ) { innerPadding ->

        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
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

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredExpenses) { expense ->
                    ExpenseCard(expense = expense)
                }
            }
        }
    }
}
