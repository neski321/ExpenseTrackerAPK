package com.neski.pennypincher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items

@Composable
fun SpendingPieChartSection(
    data: Map<String, Double>,
    colorMap: Map<String, Color>,
    modifier: Modifier = Modifier,
    onSliceClick: (String) -> Unit = {},
    categoryNames: Map<String, String> = emptyMap()
) {
    val total = data.values.sum()
    val sortedData = data.entries.sortedByDescending { it.value }
    val colors = remember { colorMap }
    val sliceAngles = remember(data) {
        val angles = mutableListOf<Pair<String, Float>>()
        var startAngle = 0f
        for ((category, value) in sortedData) {
            val sweep = if (total > 0) (value / total * 360f).toFloat() else 0f
            angles.add(category to sweep)
            startAngle += sweep
        }
        angles
    }
    val legendItems = sortedData.map { it.key }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Breakdown of your expenses. Click a category for details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    sortedData.forEach { (categoryId, value) ->
                        val sweep = if (total > 0) (value / total * 360f).toFloat() else 0f
                        val color = colors[categoryId] ?: Color.Gray
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                        // Draw percentage label
                        if (sweep > 10f) {
                            val angleRad = Math.toRadians((startAngle + sweep / 2).toDouble())
                            val labelRadius = radius * 0.65f
                            val labelX = center.x + (labelRadius * cos(angleRad)).toFloat()
                            val labelY = center.y + (labelRadius * sin(angleRad)).toFloat()
                            val percent = if (total > 0) (value / total * 100).toInt() else 0
                            drawContext.canvas.nativeCanvas.apply {
                                drawText(
                                    "$percent%",
                                    labelX,
                                    labelY,
                                    android.graphics.Paint().apply {
                                        setColor(android.graphics.Color.WHITE)
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        textSize = 32f
                                        isFakeBoldText = true
                                    }
                                )
                            }
                        }
                        startAngle += sweep
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Legend
            LazyHorizontalGrid(
                rows = GridCells.Adaptive(minSize = 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(legendItems) { categoryId ->
                    val color = colors[categoryId] ?: Color.Gray
                    val name = categoryNames[categoryId] ?: "Other"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clickable { onSliceClick(categoryId) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, shape = RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
} 