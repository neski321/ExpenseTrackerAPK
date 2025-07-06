package com.neski.pennypincher.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.repository.CategoryRepository
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import com.neski.pennypincher.ui.theme.getTextColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    userId: String,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var icon by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val parentOptions = categories.sortedBy { it.name }
    val expandedParents = remember { mutableStateMapOf<String, Boolean>() }

    val selectedParentName = parentOptions.find { it.id == parentId }?.name ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val newCategory = Category(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        parentId = parentId,
                        icon = icon.trim(),
                        color = color.trim()
                    )
                    scope.launch {
                        CategoryRepository.addCategory(userId, newCategory)
                        onAdded()
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = getTextColor())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = getTextColor())
            }
        },
        title = { Text("Add New Category", color = getTextColor()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = parentOptions.find { it.id == selectedParentId }?.name ?: "None",
                        onValueChange = {},
                        label = { Text("Parent Category (Optional)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Option for no parent (top-level category)
                        DropdownMenuItem(
                            text = { Text("None (Top-level category)") },
                            onClick = {
                                selectedParentId = ""
                                parentId = null
                                expanded = false
                            }
                        )
                        // Show nested parent/child structure
                        val parents = categories.filter { it.parentId == null }.sortedBy { it.name }
                        val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }
                        parents.forEach { parent ->
                            val children = childrenByParent[parent.id] ?: emptyList()
                            if (children.isNotEmpty()) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                                                parentId = parent.id
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
                                                parentId = child.id
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
                                        parentId = parent.id
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
