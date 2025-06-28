package com.neski.pennypincher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun SpendingBarChartSection(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    onBarClick: (String) -> Unit = {}
) {
    val months = data.keys.toList()
    val values = data.values.map { it.toFloat() }
    //val entryModel = entryModelOf(*values.toTypedArray())
    val barColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(2.dp, borderColor),
        color = backgroundColor,
        modifier = modifier
            .padding(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spending Overview",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your spending trends over the last few months. Click a month for details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Custom clickable bar chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                // Draw bars manually for click support
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val maxValue = values.maxOrNull() ?: 1f
                    months.forEachIndexed { i, month ->
                        val value = values.getOrNull(i) ?: 0f
                        val barHeightRatio = if (maxValue > 0) value / maxValue else 0f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height((150.dp * barHeightRatio).coerceAtLeast(8.dp))
                                    .width(32.dp)
                                    .background(
                                        color = barColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onBarClick(month) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$${value.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = month,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (months.isNotEmpty()) {
                Button(
                    onClick = { onBarClick(months.last()) },
                    colors = ButtonDefaults.buttonColors(containerColor = barColor)
                ) {
                    Text("View Details for ${months.last()}")
                }
            }
        }
    }
}
