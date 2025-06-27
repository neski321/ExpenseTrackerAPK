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
import androidx.compose.ui.window.Dialog
import com.neski.pennypincher.data.models.Income
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeDialog(
    userId: String,
    income: Income,
    onDismiss: () -> Unit,
    onUpdate: (Income) -> Unit
) {
    val context = LocalContext.current

    var amount by remember { mutableStateOf(income.amount.toString()) }
    var sourceId by remember { mutableStateOf(income.incomeSourceId) }
    var currencyId by remember { mutableStateOf(income.currencyId) }
    var date by remember { mutableStateOf(income.date) }
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
                    Text("Edit Income", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                Button(
                    onClick = {
                        val updatedIncome = income.copy(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            incomeSourceId = sourceId,
                            currencyId = currencyId,
                            date = date
                        )
                        onUpdate(updatedIncome)
                    },
                    enabled = amount.isNotBlank() && sourceId.isNotBlank()
                ) {
                    Text("Update Income")
                }
                }
            }
        }
    }
}