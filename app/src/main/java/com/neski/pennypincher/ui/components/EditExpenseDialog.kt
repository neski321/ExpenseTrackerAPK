package com.neski.pennypincher.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.neski.pennypincher.data.models.Expense
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import com.neski.pennypincher.data.repository.CurrencyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    userId: String,
    expense: Expense,
    onDismiss: () -> Unit,
    onUpdate: (Expense) -> Unit
) {
    var description by remember { mutableStateOf(expense.description) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var currency by remember { mutableStateOf(expense.currencyId) }
    var date by remember { mutableStateOf(expense.date) }
    var category by remember { mutableStateOf(expense.categoryId) }
    var paymentMethod by remember { mutableStateOf(expense.paymentMethodId ?: "") }
    var isSubscription by remember { mutableStateOf(expense.isSubscription) }
    var nextDueDate by remember { mutableStateOf(expense.nextDueDate) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showNextDueDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(date)
    val formattedNextDueDate = nextDueDate?.let { dateFormatter.format(it) } ?: "Pick next due date"

    LaunchedEffect(userId) {
        coroutineScope.launch {
            categories = CategoryRepository.getAllCategories(userId)
            paymentMethods = PaymentMethodRepository.getAllPaymentMethods(userId)
            currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = false)
        }
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance().apply { time = date }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                date = calendar.time
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showNextDueDatePicker) {
        val calendar = Calendar.getInstance().apply { time = nextDueDate ?: Date() }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                nextDueDate = calendar.time
                showNextDueDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val selectedCategoryName = categories.find { it.id == category }?.name ?: "Select Category"
    val selectedPaymentMethodName = paymentMethods.find { it.id == paymentMethod }?.name ?: "Select Payment Method"
    val selectedCurrency = currencies.find { it.id == currency }?.let {
        if (it.symbol.isNotBlank()) "${it.code} (${it.symbol})" else it.code
    } ?: "Select Currency"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Edit Expense", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Update the details of your expense.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
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

                        // Currency Dropdown
                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = !currencyExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = selectedCurrency,
                                onValueChange = {},
                                label = { Text("Currency") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                if (currencies.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No currencies available") },
                                        onClick = { currencyExpanded = false }
                                    )
                                }else {
                                    currencies.sortedBy { it.code }.forEach { curr ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (curr.symbol.isNotBlank())
                                                        "${curr.code} (${curr.symbol}) - ${curr.name}"
                                                    else
                                                        "${curr.code} - ${curr.name}"
                                                )
                                            },
                                            onClick = {
                                                currency = curr.id
                                                currencyExpanded = false
                                            }
                                        )
                                    }
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
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedCategoryName,
                            onValueChange = {},
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            val grouped = categories.groupBy { cat ->
                                categories.find { it.id == cat.parentId }?.name ?: "Parent"
                            }.toSortedMap()

                            grouped.forEach { (parentName, subcats) ->
                                DropdownMenuItem(
                                    text = { Text(parentName.uppercase(), style = MaterialTheme.typography.labelMedium) },
                                    onClick = {},
                                    enabled = false
                                )
                                subcats.sortedBy { it.name }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            category = cat.id
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payment Method Dropdown
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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
                        Column {
                            Text("Next Due Date", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = formattedNextDueDate,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Pick next due date") },
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNextDueDatePicker = true }
                            )
                            Text("Set the date for the next billing cycle.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onUpdate(
                                expense.copy(
                                    description = description,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    currencyId = currency,
                                    date = date,
                                    categoryId = category,
                                    paymentMethodId = if (paymentMethod.isBlank()) null else paymentMethod,
                                    isSubscription = isSubscription,
                                    nextDueDate = nextDueDate
                                )
                            )
                        },
                        enabled = description.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && currency.isNotBlank()
                    ) {
                        Text("Update Expense")
                    }
                }
            }
        }
    }
}