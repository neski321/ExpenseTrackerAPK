package com.neski.pennypincher.ui.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.IncomeSource
import com.neski.pennypincher.data.repository.IncomeSourceRepository
import kotlinx.coroutines.launch
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberDismissState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IncomeSourcesScreen(userId: String) {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<IncomeSource>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var sourceToEdit by remember { mutableStateOf<IncomeSource?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var sourceToDelete by remember { mutableStateOf<IncomeSource?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }

    fun loadSources() {
        scope.launch {
            isLoading = true
            sources = IncomeSourceRepository.getAllIncomeSources(userId)
            isLoading = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                sources = IncomeSourceRepository.getAllIncomeSources(userId)
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(userId) { loadSources() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Income Sources") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Income Source")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sources, key = { it.id }) { source ->
                        val dismissState = dismissStates.getOrPut(source.id) { rememberDismissState() }
                        LaunchedEffect(dismissState.currentValue) {
                            if (
                                dismissState.isDismissed(DismissDirection.EndToStart) ||
                                dismissState.isDismissed(DismissDirection.StartToEnd)
                            ) {
                                sourceToDelete = source
                                showConfirmDialog = true
                            }
                        }
                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
                            background = {},
                            dismissContent = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sourceToEdit = source; showEditDialog = true },
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(source.name, style = MaterialTheme.typography.titleMedium)
                                            Text(source.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { sourceToEdit = source; showEditDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                    }
                                }
                            }
                        )
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
    }

    if (showAddDialog) {
        AddIncomeSourceDialog(
            userId = userId,
            onDismiss = { showAddDialog = false },
            onAdd = {
                showAddDialog = false
                loadSources()
            }
        )
    }
    if (showEditDialog && sourceToEdit != null) {
        EditIncomeSourceDialog(
            userId = userId,
            source = sourceToEdit!!,
            onDismiss = {
                showEditDialog = false
                sourceToEdit = null
            },
            onUpdate = {
                showEditDialog = false
                sourceToEdit = null
                loadSources()
            }
        )
    }
    if (showConfirmDialog && sourceToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                sourceToDelete = null
                //dismissStates[sourceToDelete?.id]?.reset()
            },
            title = { Text("Delete Income Source") },
            text = { Text("Are you sure you want to delete this income source?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        IncomeSourceRepository.deleteIncomeSource(userId, sourceToDelete!!.id)
                        showConfirmDialog = false
                        sourceToDelete = null
                        loadSources()
                        snackbarHostState.showSnackbar("Income source deleted")
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        showConfirmDialog = false
                        dismissStates[sourceToDelete?.id]?.reset()
                        sourceToDelete = null
                    }
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddIncomeSourceDialog(userId: String, onDismiss: () -> Unit, onAdd: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                    Column {
                        Text("Add New Income Source", style = MaterialTheme.typography.titleLarge)
                        Text("Create a new source for your income.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source Name") },
                    placeholder = { Text("e.g., Salary, Freelance Project") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            IncomeSourceRepository.addIncomeSource(userId, IncomeSource(name = name))
                            onAdd()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Source")
                }
            }
        }
    }
}

@Composable
fun EditIncomeSourceDialog(userId: String, source: IncomeSource, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    var name by remember { mutableStateOf(source.name) }
    val scope = rememberCoroutineScope()
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                    Column {
                        Text("Edit Income Source", style = MaterialTheme.typography.titleLarge)
                        Text("Update the details of this income source.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            IncomeSourceRepository.updateIncomeSource(userId, source.copy(name = name))
                            onUpdate()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Source")
                }
            }
        }
    }
} 