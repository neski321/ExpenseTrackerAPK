package com.neski.pennypincher.ui.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.rememberDismissState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.AddIncomeSourceDialog
import com.neski.pennypincher.ui.components.EditIncomeSourceDialog

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IncomeSourcesScreen(
    userId: String,
    onIncomeSourceClick: (String, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<IncomeSource>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var sourceToEdit by remember { mutableStateOf<IncomeSource?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var sourceToDelete by remember { mutableStateOf<IncomeSource?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }

    LaunchedEffect(userId) {
        scope.launch {
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

    fun deleteSource(source: IncomeSource) {
        scope.launch {
            try {
                IncomeSourceRepository.deleteIncomeSource(userId, source.id)
                sources = sources.filterNot { it.id == source.id }
            } catch (e: Exception) {}
        }
    }

    fun updateSource(source: IncomeSource, newName: String) {
        scope.launch {
            try {
                val updated = source.copy(name = newName)
                IncomeSourceRepository.updateIncomeSource(userId, updated)
                sources = sources.map { if (it.id == source.id) updated else it }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Income Source")
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Manage Income Sources",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Add, edit, or remove your income sources",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (sources.isEmpty()) {
                    Text("No income sources found.")
                } else {
                    sources.forEach { source ->
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
                            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                            background = {},
                            dismissContent = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { onIncomeSourceClick(source.id, source.name) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(source.name, style = MaterialTheme.typography.bodyLarge)
                                            Text(source.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = {
                                            sourceToEdit = source
                                            showEditDialog = true
                                        }) {
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

    if (showEditDialog && sourceToEdit != null) {
        EditIncomeSourceDialog(
            userId = userId,
            source = sourceToEdit!!,
            onDismiss = { showEditDialog = false },
            onUpdate = { newName ->
                updateSource(sourceToEdit!!, newName)
                showEditDialog = false
            }
        )
    }

    if (showConfirmDialog && sourceToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                sourceToDelete = null
            },
            title = { Text("Delete Income Source") },
            text = { Text("Are you sure you want to delete this income source?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteSource(sourceToDelete!!)
                    showConfirmDialog = false
                    sourceToDelete = null
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

    if (showAddDialog) {
        AddIncomeSourceDialog(
            userId = userId,
            onDismiss = { showAddDialog = false },
            onAdd = {
                scope.launch {
                    sources = IncomeSourceRepository.getAllIncomeSources(userId, forceRefresh = true)
                    showAddDialog = false
                }
            }
        )
    }
} 