package com.neski.pennypincher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neski.pennypincher.data.models.Expense
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseRow(
    expense: Expense,
    categoryName: String,
    modifier: Modifier = Modifier,
    paymentMethodName: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    onPaymentMethodClick: () -> Unit = {}
) {
    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(expense.date)

    Card(modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = dateFormatted,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(modifier = Modifier.weight(2f)) {
                    Text(text = expense.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = categoryName,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onCategoryClick() }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        if (expense.isSubscription) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• Subscription",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                }

                Text(
                    text = paymentMethodName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1.8f)
                        .clickable { onPaymentMethodClick() },
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "$${"%.2f".format(expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }

            HorizontalDivider()
        }
    }
}
