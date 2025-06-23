package com.neski.pennypincher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.CurrencyRepository
import com.neski.pennypincher.ui.components.AddCurrencyDialog
import com.neski.pennypincher.ui.components.CurrencyCard
import com.neski.pennypincher.ui.components.EditCurrencyDialog
import kotlinx.coroutines.launch

@Composable
fun CurrencyManagementSection(userId: String) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editCurrency by remember { mutableStateOf<Currency?>(null) }

    // Fetch existing currencies
    LaunchedEffect(userId) {
        currencies = CurrencyRepository.getAllCurrencies(userId, true)
    }

    Box {
        SnackbarHost(hostState = snackbarHostState)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Manage Currencies", style = MaterialTheme.typography.titleMedium)
                Text("Base currency: US Dollar ($). Add or edit other currencies.")


                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Currency")
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currencies) { currency ->
                        CurrencyCard(
                            currency = currency,
                            onEdit = { editCurrency = currency },
                            onDelete = {
                                scope.launch {
                                    if (currency.code.uppercase() == "USD") {
                                        snackbarHostState.showSnackbar("Cannot delete base currency.")
                                    } else {
                                        CurrencyRepository.deleteCurrency(userId, currency.id)
                                        currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)
                                        snackbarHostState.showSnackbar("Currency deleted.")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AddCurrencyDialog(
                userId = userId,
                onDismiss = { showAddDialog = false },
                onAdded = {
                    scope.launch {
                        currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)
                        snackbarHostState.showSnackbar("Currency added.")
                    }
                }
            )
        }

        // Edit Dialog
        editCurrency?.let { currencyToEdit ->
            EditCurrencyDialog(
                currency = currencyToEdit,
                onDismiss = { editCurrency = null },
                onUpdate = { updated ->
                    scope.launch {
                        CurrencyRepository.updateCurrency(userId, updated)
                        currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)
                        snackbarHostState.showSnackbar("Currency updated.")
                        editCurrency = null
                    }
                }
            )
        }
    }
}
