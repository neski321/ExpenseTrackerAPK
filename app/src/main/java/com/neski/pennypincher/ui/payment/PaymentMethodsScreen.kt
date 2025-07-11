package com.neski.pennypincher.ui.payment

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.AddPaymentMethodDialog
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor
import com.neski.pennypincher.ui.components.PaymentMethodRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    userId: String,
    onPaymentMethodClick: (String, String) -> Unit = { _, _ -> }
    ) {
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
            },
                containerColor = MaterialTheme.colorScheme.primary
                ) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment Method")
            }
        },
        contentWindowInsets = WindowInsets(top = 2.dp, bottom = 2.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp )
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Manage Payment Methods",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = getTextColor()
                )
                Text(
                    text = "Add, edit, ot remove your payment methods",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getTextColor()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingSpinner(size = 80, showText = true, loadingText = "Loading payment methods...")
                    }
                } else if (methods.isEmpty()) {
                    Text("No payment methods found.", color = getTextColor())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(methods, key = { it.id }) { method ->
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
                                    PaymentMethodRow(
                                        paymentMethod = method,
                                        onEdit = {
                                            methodToEdit = method
                                            showEditDialog = true
                                        },
                                        onClick = { onPaymentMethodClick(method.id, method.name) }
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
    }
    if (showEditDialog && methodToEdit != null) {
        EditPaymentMethodDialog(
            paymentMethod = methodToEdit!!,
            onDismiss = { showEditDialog = false },
            onUpdate = { newName ->
                updateMethod(methodToEdit!!, newName)
                scope.launch {
                    methods = PaymentMethodRepository.getAllPaymentMethods(userId, forceRefresh = true)
                    showEditDialog = false
                }
            }
        )
    }
    if (showConfirmDialog && methodToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                //dismissStates[methodToDelete?.id]?.reset()
                methodToDelete = null
            },
            title = { Text("Delete Payment Method", color = getTextColor()) },
            text = { Text("Are you sure you want to delete this payment method?", color = getTextColor()) },
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
                    scope.launch {
                        showConfirmDialog = false
                        dismissStates[methodToDelete?.id]?.reset()
                        methodToDelete = null
                    }
                }) {
                    Text("Cancel", color = getTextColor())
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
