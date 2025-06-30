package com.neski.pennypincher.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Income
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import com.neski.pennypincher.data.models.IncomeSource
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.CurrencyRepository
import com.neski.pennypincher.data.repository.IncomeSourceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeDialog(
    userId: String,
    income: Income,
    onDismiss: () -> Unit,
    onUpdate: (Income) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var amount by remember { mutableStateOf(income.amount.toString()) }
    var sourceId by remember { mutableStateOf(income.incomeSourceId) }
    var currencyId by remember { mutableStateOf(income.currencyId) }
    var date by remember { mutableStateOf(income.date) }
    val showDatePicker = remember { mutableStateOf(false) }

    var sources by remember { mutableStateOf<List<IncomeSource>>(emptyList()) }
    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    LaunchedEffect(userId) {
        coroutineScope.launch {
            sources = IncomeSourceRepository.getAllIncomeSources(userId)
            currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)
        }
    }

    // Date picker logic (like AddExpenseDialog)
    if (showDatePicker.value) {
        val calendar = Calendar.getInstance().apply { time = date }
        DisposableEffect(Unit) {
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
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }

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
                        Text("Edit Income", style = MaterialTheme.typography.titleLarge)
                        Text("Update the details of your income.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = sources.find { it.id == sourceId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source") },
                        placeholder = { Text("Select source") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sourceDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    sourceId = source.id
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = currencies.find { it.id == currencyId }?.let { "${it.code} - ${it.name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        placeholder = { Text("Select currency") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.code} - ${currency.name}") },
                                onClick = {
                                    currencyId = currency.id
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = dateFormat.format(date),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker.value = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker.value = true }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val updatedIncome = income.copy(
                            amount = amount.toDoubleOrNull() ?: income.amount,
                            date = date,
                            incomeSourceId = sourceId,
                            currencyId = currencyId
                        )
                        onUpdate(updatedIncome)
                    },
                    enabled = amount.isNotBlank() && sourceId.isNotBlank() && currencyId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Income")
                }
            }
        }
    }
}