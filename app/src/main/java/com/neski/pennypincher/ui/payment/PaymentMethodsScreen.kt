package com.neski.pennypincher.ui.payment

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.repository.PaymentMethodRepository
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.rememberDismissState
import com.neski.pennypincher.ui.components.EditPaymentMethodDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.neski.pennypincher.ui.components.AddPaymentMethodDialog

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PaymentMethodsScreen(userId: String) {
    val scope = rememberCoroutineScope()
    var methods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var methodToEdit by remember { mutableStateOf<PaymentMethod?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var methodToDelete by remember { mutableStateOf<PaymentMethod?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }

    LaunchedEffect(userId) {
        scope.launch {
            methods = PaymentMethodRepository.getAllPaymentMethods(userId)
            isLoading = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                methods = PaymentMethodRepository.getAllPaymentMethods(userId, forceRefresh = true)
                isRefreshing = false
            }
        }
    )

    fun deleteMethod(method: PaymentMethod) {
        scope.launch {
            try {
                val db = PaymentMethodRepository
                db.deletePaymentMethod(userId, method.id)
                methods = methods.filterNot { it.id == method.id }
            } catch (e: Exception) {}
        }
    }

    fun updateMethod(method: PaymentMethod, newName: String) {
        scope.launch {
            try {
                val updated = method.copy(name = newName)
                PaymentMethodRepository.updatePaymentMethod(userId, updated)
                methods = methods.map { if (it.id == method.id) updated else it }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment Method")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Payment Methods", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (methods.isEmpty()) {
                    Text("No payment methods found.")
                } else {
                    methods.forEach { method ->
                        val dismissState = dismissStates.getOrPut(method.id) { rememberDismissState() }
                        LaunchedEffect(dismissState.currentValue) {
                            if (
                                dismissState.isDismissed(DismissDirection.EndToStart) ||
                                dismissState.isDismissed(DismissDirection.StartToEnd)
                            ) {
                                methodToDelete = method
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
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = method.name, style = MaterialTheme.typography.bodyLarge)
                                        Row {
                                            IconButton(onClick = {
                                                methodToEdit = method
                                                showEditDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = {
                                                methodToDelete = method
                                                showConfirmDialog = true
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
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
    if (showEditDialog && methodToEdit != null) {
        EditPaymentMethodDialog(
            paymentMethod = methodToEdit!!,
            onDismiss = { showEditDialog = false },
            onUpdate = { newName ->
                updateMethod(methodToEdit!!, newName)
                showEditDialog = false
            }
        )
    }
    if (showConfirmDialog && methodToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                methodToDelete = null
            },
            title = { Text("Delete Payment Method") },
            text = { Text("Are you sure you want to delete this payment method?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteMethod(methodToDelete!!)
                    showConfirmDialog = false
                    methodToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    methodToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showAddDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name ->
                scope.launch {
                    val newMethod = PaymentMethod(id = UUID.randomUUID().toString(), name = name)
                    PaymentMethodRepository.addPaymentMethod(userId, newMethod)
                    methods = PaymentMethodRepository.getAllPaymentMethods(userId, forceRefresh = true)
                    showAddDialog = false
                }
            }
        )
    }
}
