package com.neski.pennypincher.ui.income

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Income
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.IncomeRepository
import com.neski.pennypincher.data.repository.CurrencyRepository
import com.neski.pennypincher.data.repository.IncomeSourceRepository
import com.neski.pennypincher.data.models.IncomeSource
import com.neski.pennypincher.ui.components.IncomeRow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditIncomeDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilteredIncomeScreen(
    userId: String,
    incomeSourceId: String? = null,
    incomeSourceName: String? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToFilteredIncome: ((String, String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var incomeSources by remember { mutableStateOf<List<IncomeSource>>(emptyList()) }
    var currencies by remember { mutableStateOf<List<Currency>>(emptyList()) }
    val incomeSourceMap = incomeSources.associateBy({ it.id }, { it.name })
    val currencyMap = currencies.associateBy({ it.id }, { it })

    var showConfirmDialog by remember { mutableStateOf(false) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }
    var showEditDialog by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<Income?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId, incomeSourceId) {
        scope.launch {
            val allIncomes = IncomeRepository.getAllIncome(userId, forceRefresh = true)
            incomeSources = IncomeSourceRepository.getAllIncomeSources(userId)
            currencies = CurrencyRepository.getAllCurrencies(userId)
            
            val filtered = when {
                incomeSourceId != null -> {
                    allIncomes.filter { it.incomeSourceId == incomeSourceId }
                }
                else -> emptyList()
            }
            incomes = filtered
            isLoading = false
        }
    }

    fun deleteIncome(income: Income) {
        scope.launch {
            try {
                IncomeRepository.deleteIncome(userId, income.id)
                // Force refresh all data to ensure consistency
                snackbarHostState.showSnackbar("Income deleted successfully")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to delete income")
            }
        }
    }

    Scaffold(
        topBar = {
            val titleComposable: @Composable () -> Unit = when {
                incomeSourceName != null -> {
                    @Composable {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            Text("Income for $incomeSourceName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = getTextColor())
                            Text(
                                "View your income for this source.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                else -> {
                    @Composable { Text("Filtered Income", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = getTextColor()) }
                }
            }
            if (onBack != null) {
                TopAppBar(
                    title = { titleComposable() },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(title = { titleComposable() })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingSpinner(size = 80, showText = true, loadingText = "Loading filtered income...")
                }
            } else if (incomes.isEmpty()) {
                val filterType = when {
                    incomeSourceName != null -> "this income source"
                    else -> "the selected filter"
                }
                Text("No income found for $filterType.", color = getTextColor())
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(incomes, key = { it.id }) { income ->
                        val dismissState = dismissStates.getOrPut(income.id) { rememberDismissState() }
                        val sourceName = incomeSourceMap[income.incomeSourceId] ?: "Unknown"

                        LaunchedEffect(dismissState.currentValue) {
                            if (
                                dismissState.isDismissed(DismissDirection.EndToStart) ||
                                dismissState.isDismissed(DismissDirection.StartToEnd)
                            ) {
                                incomeToDelete = income
                                showConfirmDialog = true
                            }
                        }

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                            background = {},
                            dismissContent = {
                                IncomeRow(
                                    income = income,
                                    sourceName = sourceName,
                                    onEdit = {
                                        incomeToEdit = income
                                        showEditDialog = true
                                    },
                                    onDelete = { deleteIncome(income) },
                                    onSourceClick = {
                                        onNavigateToFilteredIncome?.invoke(income.incomeSourceId, sourceName)
                                    }
                                )
                            }
                        )
                    }
                }
            }
            if (showConfirmDialog && incomeToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmDialog = false
                        incomeToDelete = null
                    },
                    title = { Text("Delete Income", color = getTextColor()) },
                    text = { Text("Are you sure you want to delete this income?", color = getTextColor()) },
                    confirmButton = {
                        TextButton(onClick = {
                            deleteIncome(incomeToDelete!!)
                            showConfirmDialog = false
                            incomeToDelete = null
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            scope.launch {
                                dismissStates[incomeToDelete?.id]?.reset()
                                showConfirmDialog = false
                                incomeToDelete = null
                            }
                        }) {
                            Text("Cancel", color = getTextColor())
                        }
                    }
                )
            }
            if (showEditDialog && incomeToEdit != null) {
                EditIncomeDialog(
                    userId = userId,
                    income = incomeToEdit!!,
                    onDismiss = {
                        showEditDialog = false
                        incomeToEdit = null
                    },
                    onUpdate = { updatedIncome ->
                        scope.launch {
                            try {
                                IncomeRepository.updateIncome(userId, updatedIncome)
                                // Force refresh all data to ensure consistency
                                incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true)
                                incomeSources = IncomeSourceRepository.getAllIncomeSources(userId, forceRefresh = true)
                                currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)
                                showEditDialog = false
                                incomeToEdit = null
                                snackbarHostState.showSnackbar("Income updated successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to update income")
                            }
                        }
                    }
                )
            }
        }
    }
} 