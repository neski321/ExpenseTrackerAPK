package com.neski.pennypincher.ui.income

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Income
import com.neski.pennypincher.data.models.Currency
import com.neski.pennypincher.data.repository.IncomeRepository
import com.neski.pennypincher.data.repository.CurrencyRepository
import com.neski.pennypincher.ui.components.AddIncomeDialog
import com.neski.pennypincher.ui.components.EditIncomeDialog
import com.neski.pennypincher.ui.components.IncomeRow
import kotlinx.coroutines.launch

// Material 2 for swipe to dismiss only
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDismissState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun IncomeScreen(userId: String) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }


    var currencyMap by remember { mutableStateOf<Map<String, Currency>>(emptyMap()) }

    fun loadData() {
        scope.launch {
            isLoading = true
            incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true)
            currencyMap = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true).associateBy { it.id }
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        loadData()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Income Records") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Income")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {

            if (isLoading) {
                CircularProgressIndicator()
            } else if (incomes.isEmpty()) {
                Text("No income records found.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(incomes, key = { it.id }) { income ->
                        val dismissState = rememberDismissState(
                            confirmStateChange = {
                                if (it == DismissValue.DismissedToStart) {
                                    incomeToDelete = income
                                    showConfirmDialog = true
                                    false // Don’t auto-dismiss
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Red.copy(alpha = 0.2f))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            dismissContent = {
                                IncomeRow(
                                    income = income,
                                    sourceName = income.incomeSourceId, // This will show the ID unless mapped
                                    onEdit = { editingIncome = income },
                                    onDelete = {
                                        scope.launch {
                                            IncomeRepository.deleteIncome(userId, income.id)
                                            loadData()
                                            snackbarHostState.showSnackbar("Income deleted")
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddIncomeDialog(
                userId = userId,
                onDismiss = { showAddDialog = false },
                onAdd = {
                    showAddDialog = false
                    loadData()
                }
            )
        }

        editingIncome?.let { income ->
            EditIncomeDialog(
                userId = userId,
                income = income,
                onDismiss = { editingIncome = null },
                onUpdate = { updated ->
                    scope.launch {
                        IncomeRepository.updateIncome(userId, updated)
                        loadData()
                        snackbarHostState.showSnackbar("Income updated")
                    }
                    editingIncome = null
                }
            )
        }
    }

    if (showConfirmDialog && incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete Income?") },
            text = { Text("Are you sure you want to delete this income record? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        IncomeRepository.deleteIncome(userId, incomeToDelete!!.id)
                        loadData()
                        snackbarHostState.showSnackbar("Income deleted")
                        showConfirmDialog = false
                        incomeToDelete = null
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    incomeToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }


}
