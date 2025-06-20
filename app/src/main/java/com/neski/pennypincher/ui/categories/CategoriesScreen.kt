package com.neski.pennypincher.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.repository.CategoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(userId: String) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val expandedParents = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        scope.launch {
            categories = CategoryRepository.getAllCategories(userId)
            isLoading = false
        }
    }

    val parentCategories = categories.filter { it.parentId == null }
    val grouped = parentCategories.map { parent ->
        parent to categories.filter { it.parentId == parent.id }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Open Add Category dialog */ },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp)
        ) {
            Text("Manage Categories", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (categories.isEmpty()) {
                Text("No categories found.")
            } else {
                println("✅ Loaded ${categories.size} categories")

                grouped.forEach { (parent, children) ->
                    val isExpanded = expandedParents[parent.id] ?: true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedParents[parent.id] = !isExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIcon(name = parent.icon, color = parent.color)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parent.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (isExpanded) {
                        children.forEach { child ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = try {
                                        Color(android.graphics.Color.parseColor(child.color))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    CategoryIcon(name = child.icon, color = child.color)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = child.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
