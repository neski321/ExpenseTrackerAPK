package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.repository.CategoryRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import com.neski.pennypincher.ui.theme.getTextColor

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

    val expandedParents = remember { mutableStateMapOf<String, Boolean>() }

    Dialog(onDismissRequest = onDismiss) {
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
                        Text("Edit Category", style = MaterialTheme.typography.titleLarge, color = getTextColor())
                        Text("Update the details of this category.", style = MaterialTheme.typography.bodySmall, color = getTextColor())
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
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
                                .menuAnchor()
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
                            // Nested parent/child structure
                            val parents = parentOptions.filter { it.parentId == null }.sortedBy { it.name }
                            val childrenByParent = parentOptions.filter { it.parentId != null }.groupBy { it.parentId }
                            parents.forEach { parent ->
                                val children = childrenByParent[parent.id] ?: emptyList()
                                if (children.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                expandedParents[parent.id] = !(expandedParents[parent.id] ?: true)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (expandedParents[parent.id] ?: true) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                                contentDescription = if (expandedParents[parent.id] ?: true) "Collapse" else "Expand"
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(parent.name) },
                                            onClick = {
                                                selectedParentId = parent.id
                                                expanded = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (expandedParents[parent.id] ?: true) {
                                        children.sortedBy { it.name }.forEach { child ->
                                            DropdownMenuItem(
                                                text = { Row { Spacer(Modifier.width(48.dp)); Text(child.name) } },
                                                onClick = {
                                                    selectedParentId = child.id
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                } else {
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val updated = category.copy(
                                name = name,
                                parentId = if (selectedParentId.isBlank()) null else selectedParentId
                            )
                            CategoryRepository.updateCategory(userId, updated)
                            isLoading = false
                            onUpdated()
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(if (isLoading) "Saving..." else "Update Category")
                }
                }
            }
        }
    }
}
