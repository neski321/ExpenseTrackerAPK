package com.neski.pennypincher.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.rememberDismissState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor


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
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    // Helper function to update categories with consistent sorting
    fun updateCategoriesWithSorting(fetched: List<Category>) {
        categories = fetched.sortedBy { it.name }
        groupedCategories = fetched
            .sortedBy { it.name }
            .groupBy { parent ->
                fetched.find { it.id == parent.parentId }?.name ?: "Parent"
            }
        categoryNameMap = categories.associateBy({ it.id }, { it.name })
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                val fetched = CategoryRepository.getAllCategories(userId, forceRefresh = true)
                updateCategoriesWithSorting(fetched)
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(userId) {
        scope.launch {
            val fetched = CategoryRepository.getAllCategories(userId)
            updateCategoriesWithSorting(fetched)
            isLoading = false
        }
    }

    fun deleteCategory(category: Category) {
        scope.launch {
            CategoryRepository.deleteCategory(userId, category.id)
            // Force refresh from Firebase after deletion
            val fetched = CategoryRepository.getAllCategories(userId, forceRefresh = true)
            updateCategoriesWithSorting(fetched)
        }
    }

    // Helper function to recursively display categories in a layered system
    fun displayCategoryTree(
        category: Category,
        allCategories: List<Category>,
        categoryNameMap: Map<String, String>,
        dismissStates: MutableMap<String, androidx.compose.material.DismissState>,
        onEdit: (Category) -> Unit,
        onDelete: (Category) -> Unit,
        onClick: (Category) -> Unit,
        indentLevel: Int = 0
    ): List<@Composable () -> Unit> {
        val composables = mutableListOf<@Composable () -> Unit>()
        val children = allCategories.filter { it.parentId == category.id }.sortedBy { it.name }
        val hasChildren = children.isNotEmpty()
        composables.add {
            val dismissState = dismissStates.getOrPut(category.id) { rememberDismissState() }
            LaunchedEffect(dismissState.currentValue) {
                if (
                    dismissState.isDismissed(DismissDirection.EndToStart) ||
                    dismissState.isDismissed(DismissDirection.StartToEnd)
                ) {
                    onDelete(category)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasChildren) {
                            IconButton(
                                onClick = { expandedMap[category.id] = !(expandedMap[category.id] ?: true) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (expandedMap[category.id] ?: true) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = if (expandedMap[category.id] ?: true) "Collapse" else "Expand"
                                )
                            }
                        } else {
                            Spacer(Modifier.width(24.dp))
                        }
                        CategoryRow(
                            category = category,
                            categoryNameMap = categoryNameMap,
                            onEdit = { onEdit(category) },
                            onClick = { onClick(category) },
                            indentLevel = indentLevel
                        )
                    }
                }
            )
        }
        if (!hasChildren || (expandedMap[category.id] ?: true)) {
            children.forEach { child ->
                composables.addAll(
                    displayCategoryTree(
                        child,
                        allCategories,
                        categoryNameMap,
                        dismissStates,
                        onEdit,
                        onDelete,
                        onClick,
                        indentLevel + 1
                    )
                )
            }
        }
        return composables
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        },
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
                    text = "Manage Categories",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = getTextColor()
                )
                Text(
                    text = "Organize your expenses by creating and managing categories and sub-categories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getTextColor()
                )
                Spacer(Modifier.height(12.dp))
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingSpinner(size = 80, showText = true, loadingText = "Loading categories...")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        val parentCategories = categories.filter { it.parentId == null }.sortedBy { it.name }
                        parentCategories.forEach { parent ->
                            val composables = displayCategoryTree(
                                parent,
                                categories,
                                categoryNameMap,
                                dismissStates,
                                onEdit = {
                                    categoryToEdit = it
                                    showEditDialog = true
                                },
                                onDelete = {
                                    categoryToDelete = it
                                    showConfirmDialog = true
                                },
                                onClick = {
                                    onCategoryClick(it.id, it.name)
                                }
                            )
                            composables.forEach { item { it() } }
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
            categories = categories,
            onDismiss = { showDialog = false },
            onAdded = {
                scope.launch {
                    val fetched = CategoryRepository.getAllCategories(userId, forceRefresh = true)
                    updateCategoriesWithSorting(fetched)
                    showDialog = false
                }
            }
        )
    }

    if (showEditDialog && categoryToEdit != null) {
        EditCategoryDialog(
            userId = userId,
            category = categoryToEdit!!,
            allCategories = categories,
            onDismiss = { showEditDialog = false },
            onUpdated = {
                scope.launch {
                    val fetched = CategoryRepository.getAllCategories(userId, forceRefresh = true)
                    updateCategoriesWithSorting(fetched)
                    showEditDialog = false
                }
            }
        )
    }


    if (showConfirmDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                scope.launch {
                    dismissStates[categoryToDelete?.id]?.reset()
                    showConfirmDialog = false
                    categoryToDelete = null
                }
            },
            title = { Text("Delete Category", color = getTextColor()) },
            text = { Text("Are you sure you want to delete this category?", color = getTextColor()) },
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
                    Text("Cancel", color = getTextColor())
                }
            }
        )
    }
}
