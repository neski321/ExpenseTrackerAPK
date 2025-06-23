package com.neski.pennypincher.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.repository.CurrencyRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    userId: String,
    onDismiss: () -> Unit,
    onAdd: (
        description: String,
        amount: Double,
        currency: String,
        date: Date,
        category: String,
        paymentMethod: String?,
        isSubscription: Boolean,
        nextDueDate: Date?
    ) -> Unit
) {
    // State variables
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD ($)") }
    var date by remember { mutableStateOf(Date()) }
    var category by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var isSubscription by remember { mutableStateOf(false) }
    var nextDueDate by remember { mutableStateOf<Date?>(null) }

    // Date picker states
    val showDatePicker = remember { mutableStateOf(false) }
    val showNextDueDatePicker = remember { mutableStateOf(false) }

    // Formatted dates
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val formattedDate by remember(date) { derivedStateOf { dateFormatter.format(date) } }
    val formattedNextDueDate by remember(nextDueDate) {
        derivedStateOf { nextDueDate?.let { dateFormatter.format(it) } ?: "Pick next due date" }
    }

    // Other state
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    var currencyExpanded by remember { mutableStateOf(false) }

    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }
    val currencyMap = currencies.associateBy { it.id }
    val selectedCurrencyLabel = currencyMap[currency]?.let { "${it.symbol} ${it.code}" } ?: "Select Currency"





    // Load categories
    LaunchedEffect(userId) {
        coroutineScope.launch {
            categories = CategoryRepository.getAllCategories(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)

        }
    }

    // Date picker for expense date
    if (showDatePicker.value) {
        val calendar = Calendar.getInstance().apply { time = date }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                date = calendar.time
                showDatePicker.value = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            LaunchedEffect(Unit) { show() }
            DisposableEffect(Unit) {
                onDispose { dismiss() }
            }
        }
    }

    // Date picker for next due date
    if (showNextDueDatePicker.value) {
        val calendar = Calendar.getInstance().apply {
            time = nextDueDate ?: Date()
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                nextDueDate = calendar.time
                showNextDueDatePicker.value = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            LaunchedEffect(Unit) { show() }
            DisposableEffect(Unit) {
                onDispose { dismiss() }
            }
        }
    }

    val selectedCategoryName = categories.find { it.id == category }?.name ?: "Select Category"
    val selectedPaymentMethodName = paymentMethods.find { it.id == paymentMethod }?.name ?: "Select Payment Method"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        description,
                        amount.toDoubleOrNull() ?: 0.0,
                        currency,
                        date,
                        category,
                        if (paymentMethod.isBlank()) null else paymentMethod,
                        isSubscription,
                        nextDueDate
                    )
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && category.isNotBlank()
            ) {
                Text("Add Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Column {
                Text("Add New Expense", style = MaterialTheme.typography.headlineSmall)
                Text("Enter the details of your new expense.", style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Groceries, Netflix") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedCurrencyLabel,
                            onValueChange = {},
                            label = { Text("Currency") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                            },
                            modifier = Modifier.menuAnchor().weight(1f)
                        )

                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.sortedBy { it.code }.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.symbol} ${item.code}") },
                                    onClick = {
                                        currency = item.id
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                }

                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Expense") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            modifier = Modifier.clickable { showDatePicker.value = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker.value = true }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedCategoryName,
                        onValueChange = {},
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val grouped = categories.groupBy { cat ->
                            categories.find { it.id == cat.parentId }?.name ?: "Parent"
                        }.toSortedMap()

                        grouped.forEach { (parentName, subcats) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        parentName.uppercase(),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            subcats.sortedBy { it.name }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        category = cat.id
                                        expanded = false
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
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedPaymentMethodName,
                        onValueChange = {},
                        label = { Text("Payment Method (Optional)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        paymentMethods.sortedBy { it.name }.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.name) },
                                onClick = {
                                    paymentMethod = method.id
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSubscription,
                        onCheckedChange = { isSubscription = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Is this a recurring subscription?")
                }

                if (isSubscription) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Next Due Date", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = formattedNextDueDate,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Pick next due date") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    modifier = Modifier.clickable { showNextDueDatePicker.value = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNextDueDatePicker.value = true }
                        )
                        Text(
                            "Set the date for the next billing cycle.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}