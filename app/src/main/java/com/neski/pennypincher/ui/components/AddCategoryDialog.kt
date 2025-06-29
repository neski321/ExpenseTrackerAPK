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
                        value = selectedParentName,
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
                                expanded = false
                            }
                        )

                        // Group categories by parent name
                        val grouped = categories
                            .sortedBy { it.name }
                            .groupBy { cat ->
                                categories.find { it.id == cat.parentId }?.name ?: "Parent Categories"
                            }

                        grouped.forEach { (groupName, children) ->
                            // Group label (unclickable)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                enabled = false,
                                onClick = {}
                            )

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
