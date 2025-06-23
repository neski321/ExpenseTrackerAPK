package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Income
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IncomeRow(
    income: Income,
    sourceName: String,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(income.date)

    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(dateString, style = MaterialTheme.typography.bodyMedium)
                    Text(income.description, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                AssistChip(
                    onClick = {},
                    label = { Text(sourceName) }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "$${"%.2f".format(income.amount)}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row {
                    onEdit?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Income")
                        }
                    }
                }
            }

            HorizontalDivider()
        }
    }
}
