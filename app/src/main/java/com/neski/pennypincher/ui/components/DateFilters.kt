package com.neski.pennypincher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.neski.pennypincher.ui.theme.getTextColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilters(
    availableYears: List<Int>,
    availableMonths: List<Int>,
    selectedYear: Int?,
    selectedMonth: Int?,
    onYearSelected: (Int?) -> Unit,
    onMonthSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Filter Records"
) {
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = getTextColor()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year Filter
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedYear?.toString() ?: "All Years",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        // "All Years" option
                        DropdownMenuItem(
                            text = { Text("All Years") },
                            onClick = {
                                onYearSelected(null)
                                onMonthSelected(null) // Reset month when year changes
                                yearExpanded = false
                            }
                        )
                        
                        // Available years
                        availableYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    onYearSelected(year)
                                    onMonthSelected(null) // Reset month when year changes
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Month Filter
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (selectedMonth != null) monthNames[selectedMonth] else "All Months",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        enabled = selectedYear != null
                    )
                    
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        // "All Months" option
                        DropdownMenuItem(
                            text = { Text("All Months") },
                            onClick = {
                                onMonthSelected(null)
                                monthExpanded = false
                            }
                        )
                        
                        // Available months for selected year
                        availableMonths.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(monthNames[month]) },
                                onClick = {
                                    onMonthSelected(month)
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Clear filters button
            if (selectedYear != null || selectedMonth != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onYearSelected(null)
                        onMonthSelected(null)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear Filters")
                }
            }
        }
    }
}