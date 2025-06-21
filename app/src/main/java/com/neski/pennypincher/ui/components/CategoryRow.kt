package com.neski.pennypincher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Category

@Composable
fun CategoryRow(
    category: Category,
    categoryNameMap: Map<String, String> = emptyMap(), // Map of id -> name
    onEdit: () -> Unit = {},
    //onDelete: () -> Unit = {}
) {
    val parentName = category.parentId?.let { categoryNameMap[it] }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium
                )

                if (!parentName.isNullOrBlank()) {
                    Text(
                        text = "Subcategory of: $parentName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { onEdit() }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Category")
            }
        }
    }
}
