package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.repository.CategoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    userId: String,
    category: Category,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(category.name) }
    var selectedParentId by remember { mutableStateOf(category.parentId ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    val parentOptions = allCategories.filter { it.id != category.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val updated = category.copy(
                            name = name,
                            parentId = if (selectedParentId.isBlank()) null else selectedParentId
                        )
                        CategoryRepository.addCategory(userId, updated)
                        isLoading = false
                        onUpdated()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (isLoading) "Saving..." else "Update Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Column {
                Text("Edit Category", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Update the details of this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Category Name", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }

                Text("Parent Category (Optional)", style = MaterialTheme.typography.labelSmall)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedName = parentOptions.find { it.id == selectedParentId }?.name
                        ?: "None (Top-level category)"

                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent Category (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor() // ✅ NEW SYNTAX
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Option for top-level (no parent)
                        DropdownMenuItem(
                            text = { Text("None (Top-level category)") },
                            onClick = {
                                selectedParentId = ""
                                expanded = false
                            }
                        )

                        // Group categories by parent (or "Uncategorized")
                        val grouped = parentOptions
                            .filter { it.id != category.id } // prevent selecting itself
                            .sortedBy { it.name }
                            .groupBy { parentOptions.find { p -> p.id == it.parentId }?.name ?: "Parent Categories" }

                        grouped.forEach { (groupName, children) ->
                            // Group Header
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                enabled = false, // Make it unclickable
                                onClick = {}
                            )

                            // Items in that group
                            children.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(parent.name) },
                                    onClick = {
                                        selectedParentId = parent.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                }
            }
        }
    )
}
