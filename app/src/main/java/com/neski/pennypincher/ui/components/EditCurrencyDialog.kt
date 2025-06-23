package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Currency

@Composable
fun EditCurrencyDialog(
    currency: Currency,
    onDismiss: () -> Unit,
    onUpdate: (Currency) -> Unit
) {
    var code by remember { mutableStateOf(currency.code) }
    var name by remember { mutableStateOf(currency.name) }
    var symbol by remember { mutableStateOf(currency.symbol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(currency.copy(code = code, name = name, symbol = symbol))
                },
                enabled = code.isNotBlank() && name.isNotBlank() && symbol.isNotBlank()
            ) {
                Text("Update Currency")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Edit Currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") })
                OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") })
            }
        }
    )
}
