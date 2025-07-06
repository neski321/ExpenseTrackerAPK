package com.neski.pennypincher.ui.income

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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


//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.SwipeToDismiss
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Delete
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditIncomeDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor
import com.neski.pennypincher.ui.components.DateFilters

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
    //val currencyMap = currencies.associateBy({ it.id }, { it })

    var showConfirmDialog by remember { mutableStateOf(false) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }
    var showEditDialog by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<Income?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Date filter state
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var availableYears by remember { mutableStateOf<List<Int>>(emptyList()) }
    var availableMonths by remember { mutableStateOf<List<Int>>(emptyList()) }

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

            // Build available years from loaded/filtered incomes
            availableYears = incomes.map {
                val cal = java.util.Calendar.getInstance()
                cal.time = it.date
                cal.get(java.util.Calendar.YEAR)
            }.distinct().sortedDescending()
        }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(top = 2.dp, bottom = 2.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Breadcrumb and back button (always at the top)
                if (onBack != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back to Income",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = when {
                        incomeSourceName != null -> "Income for $incomeSourceName"
                        else -> "Filtered Income"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = getTextColor()
                )
                Text(
                    text = when {
                        incomeSourceName != null -> "View your income for this source."
                        else -> "Track your filtered income."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = getTextColor()
                )
                Spacer(Modifier.height(12.dp))
                // Date filter UI (consistent placement)
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
                        LoadingSpinner(size = 80, showText = true, loadingText = "Loading filtered income...")
                    }
                } else if (filteredIncomes.isEmpty()) {
                    val filterType = when {
                        incomeSourceName != null -> "this income source"
                        else -> "the selected filter"
                    }
                    Text("No income found for $filterType.", color = getTextColor())
                } else {
                    // Table header row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Income Source", modifier = Modifier.weight(2.3f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Amount", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                            Text("Actions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = getTextColor())
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 50.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredIncomes, key = { it.id }) { income ->
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
                            Text("Cancel")
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
                                incomes = IncomeRepository.getAllIncome(userId, forceRefresh = true)
                                incomeSources = IncomeSourceRepository.getAllIncomeSources(userId, forceRefresh = true)
                                currencies = CurrencyRepository.getAllCurrencies(userId, forceRefresh = true)
                                // Rebuild availableYears and availableMonths from new incomes
                                availableYears = incomes.map {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.time = it.date
                                    cal.get(java.util.Calendar.YEAR)
                                }.distinct().sortedDescending()
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
                                // Reset filter if selected year/month are no longer available
                                if (selectedYear != null && selectedYear !in availableYears) {
                                    selectedYear = null
                                    selectedMonth = null
                                }
                                if (selectedMonth != null && selectedMonth !in availableMonths) {
                                    selectedMonth = null
                                }
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