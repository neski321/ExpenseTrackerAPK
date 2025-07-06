package com.neski.pennypincher.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
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
import com.neski.pennypincher.ui.theme.getTextColor

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
    val expandedParents = remember { mutableStateMapOf<String, Boolean>() }

    var currencyExpanded by remember { mutableStateOf(false) }

    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }
    //val currencyMap = currencies.associateBy { it.id }
    //val selectedCurrencyLabel = currencyMap[currency]?.let { "${it.symbol} ${it.code}" } ?: "Select Currency"

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
        DisposableEffect(showDatePicker.value) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    date = calendar.time
                    showDatePicker.value = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnCancelListener {
                showDatePicker.value = false
            }
            dialog.setOnDismissListener {
                showDatePicker.value = false
            }
            dialog.show()
            onDispose {
                dialog.dismiss()
            }
        }
    }

    // Date picker for next due date
    if (showNextDueDatePicker.value) {
        val calendar = Calendar.getInstance().apply {
            time = nextDueDate ?: Date()
        }
        DisposableEffect(showNextDueDatePicker.value) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    nextDueDate = calendar.time
                    showNextDueDatePicker.value = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnCancelListener {
                showNextDueDatePicker.value = false
            }
            dialog.setOnDismissListener {
                showNextDueDatePicker.value = false
            }
            dialog.show()
            onDispose {
                dialog.dismiss()
            }
        }
    }

    // Mirror EditExpenseDialog layout and logic
    val selectedCategoryName = categories.find { it.id == category }?.name ?: "Select Category"
    val selectedPaymentMethodName = paymentMethods.find { it.id == paymentMethod }?.name ?: "Select Payment Method"
    val selectedCurrency = currencies.find { it.id == currency }?.let {
        if (it.symbol.isNotBlank()) "${it.code} (${it.symbol})" else it.code
    } ?: "Select Currency"

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                        Text("Add New Expense", style = MaterialTheme.typography.titleLarge, color = getTextColor())
                        Text("Enter the details of your new expense.", style = MaterialTheme.typography.bodySmall, color = getTextColor())
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
                                } else {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = formattedDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date of Expense") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                showDatePicker.value = true
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                        }
                    }

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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // Show nested parent/child structure
                            val parents = categories.filter { it.parentId == null }.sortedBy { it.name }
                            val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }
                            parents.forEach { parent ->
                                val children = childrenByParent[parent.id] ?: emptyList()
                                if (children.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                expandedParents[parent.id] = !(expandedParents[parent.id] ?: true)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (expandedParents[parent.id] ?: true) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                                contentDescription = if (expandedParents[parent.id] ?: true) "Collapse" else "Expand"
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(parent.name) },
                                            onClick = {
                                                category = parent.id
                                                expanded = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (expandedParents[parent.id] ?: true) {
                                        children.sortedBy { it.name }.forEach { child ->
                                            DropdownMenuItem(
                                                text = { Row { Spacer(Modifier.width(48.dp)); Text(child.name) } },
                                                onClick = {
                                                    category = child.id
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(parent.name) },
                                        onClick = {
                                            category = parent.id
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
                        Text("Next Due Date", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = formattedNextDueDate,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Pick next due date") },
                                label = { Text("Next Due Date") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    showNextDueDatePicker.value = true
                                }
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        }
                        Text("Set the date for the next billing cycle.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = getTextColor())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                        enabled = description.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && currency.isNotBlank()
                    ) {
                        Text("Add Expense", color = getTextColor())
                    }
                }
            }
        }
    }
}