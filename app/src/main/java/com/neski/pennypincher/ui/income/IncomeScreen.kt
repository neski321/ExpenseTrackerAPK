package com.neski.pennypincher.ui.income

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.neski.pennypincher.data.models.IncomeSource
import com.neski.pennypincher.data.repository.IncomeSourceRepository
import com.neski.pennypincher.ui.components.AddIncomeDialog
import com.neski.pennypincher.ui.components.EditIncomeDialog
import com.neski.pennypincher.ui.components.IncomeRow
import kotlinx.coroutines.launch

// Material 2 for swipe to dismiss only
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.SwipeToDismiss
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberDismissState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor
import com.neski.pennypincher.ui.components.DateFilters

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun IncomeScreen(
    userId: String,
    onNavigateToFilteredIncome: ((String, String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }

    var currencyMap by remember { mutableStateOf<Map<String, Currency>>(emptyMap()) }
    var incomeSources by remember { mutableStateOf<List<IncomeSource>>(emptyList()) }
    val incomeSourceMap = incomeSources.associateBy({ it.id }, { it.name })

    // Date filter state
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var availableYears by remember { mutableStateOf<List<Int>>(emptyList()) }
    var availableMonths by remember { mutableStateOf<List<Int>>(emptyList()) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true).sortedByDescending { it.date }
                currencyMap = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true).associateBy { it.id }
                incomeSources = IncomeSourceRepository.getAllIncomeSources(userId, forceRefresh = true)
                // Build available years from loaded incomes
                availableYears = incomes.map {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it.date
                    cal.get(java.util.Calendar.YEAR)
                }.distinct().sortedDescending()
                isRefreshing = false
            }
        }
    )

    fun loadData() {
        scope.launch {
            isLoading = true
            incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true).sortedByDescending { it.date }
            currencyMap = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true).associateBy { it.id }
            incomeSources = IncomeSourceRepository.getAllIncomeSources(userId, forceRefresh = true)
            // Build available years from loaded incomes
            availableYears = incomes.map {
                val cal = java.util.Calendar.getInstance()
                cal.time = it.date
                cal.get(java.util.Calendar.YEAR)
            }.distinct().sortedDescending()
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        loadData()
    }

    // Update available months when selected year or incomes change
    LaunchedEffect(selectedYear, incomes) {
        scope.launch {
            selectedYear?.let { year ->
                availableMonths = incomes.filter {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it.date
                    cal.get(java.util.Calendar.YEAR) == year
                }.map {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it.date
                    cal.get(java.util.Calendar.MONTH)
                }.distinct().sorted()
            } ?: run {
                availableMonths = emptyList()
            }
        }
    }

    // Filter incomes by selected year/month
    val filteredIncomes = incomes.filter { income ->
        val calendar = java.util.Calendar.getInstance()
        calendar.time = income.date
        val incomeYear = calendar.get(java.util.Calendar.YEAR)
        val incomeMonth = calendar.get(java.util.Calendar.MONTH)
        val yearMatches = selectedYear == null || incomeYear == selectedYear
        val monthMatches = selectedMonth == null || incomeMonth == selectedMonth
        yearMatches && monthMatches
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
                ) {
                Icon(Icons.Default.Add, contentDescription = "Add Income")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(top = 2.dp, bottom = 2.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Manage Income",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = getTextColor()
                )
                Text(
                    text = "Track your earnings and keep your finances in order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getTextColor()
                )
                Spacer(Modifier.height(12.dp))
                // Date filter UI
                DateFilters(
                    availableYears = availableYears,
                    availableMonths = availableMonths,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onYearSelected = { year ->
                        selectedYear = year
                        selectedMonth = null // Reset month when year changes
                    },
                    onMonthSelected = { month ->
                        selectedMonth = month
                    },
                    label = "Filter Income"
                )
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingSpinner(size = 80, showText = true, loadingText = "Loading income...")
                    }
                } else if (incomes.isEmpty()) {
                    Text("No income records found.", color = getTextColor())
                } else {

                    // Table header row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        shape = MaterialTheme.shapes.medium,
                        //color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp,
                        //border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium)
                            Text("Income Source", modifier = Modifier.weight(2.3f), style = MaterialTheme.typography.labelMedium)
                            Text("Amount", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                            Text("Actions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredIncomes, key = { it.id }) { income ->
                            val dismissState = dismissStates.getOrPut(income.id) { rememberDismissState() }

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
                                directions = setOf(DismissDirection.EndToStart),
                                background = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
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
                                        sourceName = incomeSourceMap[income.incomeSourceId] ?: income.incomeSourceId,
                                        onEdit = { editingIncome = income },
                                        onDelete = {
                                            scope.launch {
                                                IncomeRepository.deleteIncome(userId, income.id)
                                                loadData()
                                                snackbarHostState.showSnackbar("Income deleted")
                                            }
                                        },
                                        onSourceClick = {
                                            onNavigateToFilteredIncome?.invoke(income.incomeSourceId, incomeSourceMap[income.incomeSourceId] ?: income.incomeSourceId)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                scale = true,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }

        if (showAddDialog) {
            AddIncomeDialog(
                userId = userId,
                onDismiss = { showAddDialog = false },
                onAdd = {
                    scope.launch {
                        incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true)
                    }
                    showAddDialog = false
                }
            )
        }

        editingIncome?.let { income ->
            EditIncomeDialog(
                userId = userId,
                income = income,
                onDismiss = {
                    editingIncome = null
                    showAddDialog = false
                },
                onUpdate = { updatedIncome ->
                    scope.launch {
                        IncomeRepository.updateIncome(userId, updatedIncome)
                        incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true)
                    }
                    editingIncome = null
                    showAddDialog = false
                }
            )
        }
    }

    if (showConfirmDialog && incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                incomeToDelete?.let { income ->
                    scope.launch { dismissStates[income.id]?.reset() }
                }
                showConfirmDialog = false
                incomeToDelete = null
                               },
            title = { Text("Delete Income?", color = getTextColor()) },
            text = { Text("Are you sure you want to delete this income record? This action cannot be undone.", color = getTextColor()) },
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


}
