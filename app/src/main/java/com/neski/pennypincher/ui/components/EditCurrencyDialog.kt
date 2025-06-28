package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.neski.pennypincher.data.models.Currency
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@Composable
fun EditCurrencyDialog(
    currency: Currency,
    onDismiss: () -> Unit,
    onUpdate: (Currency) -> Unit
) {
    var code by remember { mutableStateOf(currency.code) }
    var name by remember { mutableStateOf(currency.name) }
    var symbol by remember { mutableStateOf(currency.symbol) }

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
                    Text("Edit Currency", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") })
                    OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                Button(
                    onClick = {
                        onUpdate(currency.copy(code = code, name = name, symbol = symbol))
                    },
                    enabled = code.isNotBlank() && name.isNotBlank() && symbol.isNotBlank(),

                ) {
                    Text("Update Currency")
                }
                }
            }
        }
    }
}
