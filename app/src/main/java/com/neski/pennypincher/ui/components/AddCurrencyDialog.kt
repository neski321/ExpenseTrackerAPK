package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.CurrencyRepository
import kotlinx.coroutines.launch
import java.util.*
import com.neski.pennypincher.ui.theme.getTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCurrencyDialog(
    userId: String,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Text("Add New Currency", style = MaterialTheme.typography.headlineSmall, color = getTextColor())
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Currency Name", style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            showValidationError = false
                        },
                        isError = showValidationError && name.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Currency Code (3 letters)", style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it
                            showValidationError = false
                        },
                        isError = showValidationError && code.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Currency Symbol", style = MaterialTheme.typography.labelMedium, color = getTextColor())
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = {
                            symbol = it
                            showValidationError = false
                        },
                        isError = showValidationError && symbol.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (showValidationError) {
                    Text(
                        text = "All fields are required.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (code.isBlank() || name.isBlank() || symbol.isBlank()) {
                            showValidationError = true
                            return@Button
                        }

                        val newCurrency = Currency(
                            id = UUID.randomUUID().toString(),
                            code = code.trim().uppercase(),
                            name = name.trim(),
                            symbol = symbol.trim()
                        )

                        scope.launch {
                            CurrencyRepository.addCurrency(userId, newCurrency)
                            onAdded()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Currency")
                }
            }
        }
    )
}
