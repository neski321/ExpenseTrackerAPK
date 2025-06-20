package com.neski.pennypincher.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Expense
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.remember
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

@Composable
fun ExpenseCard(expense: Expense) {
    val dateFormatted = remember(expense.date) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(expense.date)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = expense.description, style = MaterialTheme.typography.titleMedium)
            Text(text = "$${expense.amount}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}