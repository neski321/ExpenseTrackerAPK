package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Income
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IncomeCard(
    income: Income,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(income.date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = income.incomeSourceId, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Amount: ${income.amount}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32)) // green
                    Text(text = "Date: $dateString", style = MaterialTheme.typography.bodySmall)
                }

                Row {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}