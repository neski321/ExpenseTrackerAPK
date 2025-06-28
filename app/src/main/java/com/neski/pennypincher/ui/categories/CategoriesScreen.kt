package com.neski.pennypincher.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.ui.components.AddCategoryDialog
import com.neski.pennypincher.data.repository.CategoryRepository
import com.neski.pennypincher.ui.components.CategoryRow
import com.neski.pennypincher.ui.components.EditCategoryDialog
import kotlinx.coroutines.launch

// Material 2 for swipe to dismiss only
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberDismissState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(userId: String, onCategoryClick: (String, String) -> Unit = { _, _ -> }) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var groupedCategories by remember { mutableStateOf<Map<String, List<Category>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val dismissStates = remember { mutableStateMapOf<String, androidx.compose.material.DismissState>() }
    var showEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryNameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                val fetched = CategoryRepository.getAllCategories(userId, forceRefresh = true)
                categories = fetched.sortedBy { it.name }
                groupedCategories = fetched
                    .sortedBy { it.name }
                    .groupBy { parent ->
                        fetched.find { it.id == parent.parentId }?.name ?: "Parent"
                    }
                categoryNameMap = categories.associateBy({ it.id }, { it.name })
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(userId) {
        scope.launch {
            val fetched = CategoryRepository.getAllCategories(userId)
            categories = fetched.sortedBy { it.name }
            groupedCategories = fetched
                .sortedBy { it.name }
                .groupBy { parent ->
                    fetched.find { it.id == parent.parentId }?.name ?: "Parent"
                }
            categoryNameMap = categories.associateBy({ it.id }, { it.name })
            isLoading = false
        }
    }

    fun deleteCategory(category: Category) {
        scope.launch {
            CategoryRepository.deleteCategory(userId, category.id)
            val updated = categories.filterNot { it.id == category.id }
            categories = updated
            groupedCategories = updated
                .sortedBy { it.name }
                .groupBy { parent ->
                    updated.find { it.id == parent.parentId }?.name ?: "Parent"
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Categories") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedCategories
                            .toSortedMap() // alphabetically by parent name
                            .forEach { (parentName, children) ->
                                item {
                                    Text(
                                        text = parentName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                items(children.sortedBy { it.name }, key = { it.id }) { category ->
                                    val dismissState = dismissStates.getOrPut(category.id) {
                                        rememberDismissState()
                                    }

                                    LaunchedEffect(dismissState.currentValue) {
                                        if (
                                            dismissState.isDismissed(DismissDirection.EndToStart) ||
                                            dismissState.isDismissed(DismissDirection.StartToEnd)
                                        ) {
                                            categoryToDelete = category
                                            showConfirmDialog = true
                                        }
                                    }

                                    SwipeToDismiss(
                                        state = dismissState,
                                        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                                        background = {},
                                        dismissContent = {
                                            CategoryRow(
                                                category = category,
                                                categoryNameMap = categoryNameMap,
                                                onEdit = {
                                                    categoryToEdit = category
                                                    showEditDialog = true
                                                },
                                                onClick = {
                                                    onCategoryClick(category.id, category.name)
                                                }
                                            )
                                        }
                                    )
                                }
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

    if (showDialog) {
        AddCategoryDialog(
            userId = userId,
            onDismiss = { showDialog = false },
            categories = categories,
            onAdded = {
                showDialog = false
            // Refresh after add
            scope.launch {
                val refreshed = CategoryRepository.getAllCategories(userId)
                categories = refreshed.sortedBy { it.name }
                groupedCategories = refreshed
                    .sortedBy { it.name }
                    .groupBy { parent ->
                        refreshed.find { it.id == parent.parentId }?.name ?: "Parent"
                    }
                }
            }
        )
    }

    if (showEditDialog && categoryToEdit != null) {
        EditCategoryDialog(
            userId = userId,
            category = categoryToEdit!!,
            allCategories = categories,
            onDismiss = {
                showEditDialog = false
                categoryToEdit = null
            },
            onUpdated = {
                showEditDialog = false
                categoryToEdit = null
                scope.launch {
                    val refreshed = CategoryRepository.getAllCategories(userId)
                    categories = refreshed.sortedBy { it.name }
                    groupedCategories = refreshed
                        .sortedBy { it.name }
                        .groupBy { parent ->
                            refreshed.find { it.id == parent.parentId }?.name ?: "Uncategorized"
                        }
                }
            }
        )
    }


    if (showConfirmDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                categoryToDelete = null
            },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete this category?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteCategory(categoryToDelete!!)
                    showConfirmDialog = false
                    categoryToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        dismissStates[categoryToDelete?.id]?.reset()
                        showConfirmDialog = false
                        categoryToDelete = null
                    }
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
