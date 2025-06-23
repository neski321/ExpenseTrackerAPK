package com.neski.pennypincher.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Income
import com.neski.pennypincher.data.repository.IncomeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    userId: String,
    onDismiss: () -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("") }
    var sourceId by remember { mutableStateOf("") }
    var currencyId by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val income = Income(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        date = date,
                        incomeSourceId = sourceId,
                        currencyId = currencyId
                    )
                    scope.launch {
                        IncomeRepository.addIncome(userId, income)
                        onAdd()
                    }
                },
                enabled = amount.isNotBlank() && sourceId.isNotBlank()
            ) {
                Text("Add Income")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add New Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sourceId,
                    onValueChange = { sourceId = it },
                    label = { Text("Source") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currencyId,
                    onValueChange = { currencyId = it },
                    label = { Text("Currency ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateFormat.format(date),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )
            }
        }
    )
}