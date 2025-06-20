package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun SpendingBarChartSection(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val months = data.keys.toList()
    val values = data.values.map { it.toFloat() }
    val entryModel = entryModelOf(*values.toTypedArray())

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier.padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending Overview",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your spending trends over the last few months. Click a month for details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Chart(
                chart = columnChart(), // ✅ no ColumnComponent required
                model = entryModel,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ -> "$${value.toInt()}" }
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _ ->
                        months.getOrNull(value.toInt()) ?: ""
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }
    }
}
