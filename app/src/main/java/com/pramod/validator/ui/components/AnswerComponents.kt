package com.pramod.validator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable components and utilities for displaying answer types
 */

// Answer type utilities
object AnswerUtils {
    fun getAnswerColor(answer: String, colorScheme: androidx.compose.material3.ColorScheme): Color {
        return when (answer) {
            "COMPLIANT" -> colorScheme.primary
            "NON_COMPLIANT" -> colorScheme.error
            "NOT_APPLICABLE" -> colorScheme.secondary
            else -> colorScheme.onSurface
        }
    }
    
    fun getAnswerIcon(answer: String): String {
        return when (answer) {
            "COMPLIANT" -> "✓"
            "NON_COMPLIANT" -> "✗"
            "NOT_APPLICABLE" -> "—"
            else -> "?"
        }
    }
    
    fun getAnswerDisplayText(answer: String): String {
        return when (answer) {
            "COMPLIANT" -> "Compliant"
            "NON_COMPLIANT" -> "Non-Compliant"
            "NOT_APPLICABLE" -> "Not Applicable"
            else -> answer
        }
    }
}

/**
 * Reusable Answer Badge component
 */
@Composable
fun AnswerBadge(
    answer: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = AnswerUtils.getAnswerColor(answer, colorScheme).copy(alpha = 0.15f)
    val textColor = AnswerUtils.getAnswerColor(answer, colorScheme)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AnswerUtils.getAnswerIcon(answer),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AnswerUtils.getAnswerDisplayText(answer),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * Reusable Question Response Card
 */
@Composable
fun QuestionResponseCard(
    questionNumber: Int,
    questionText: String,
    answer: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Question $questionNumber",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = questionText,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AnswerBadge(answer = answer)
        }
    }
}

/**
 * Filter for answer types - shows All, Compliant, Non-Compliant, Not Applicable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseFilterChips(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    compliantCount: Int,
    nonCompliantCount: Int,
    notApplicableCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = compliantCount + nonCompliantCount + notApplicableCount
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All filter
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { 
                    Text(
                        text = "All ($totalCount)",
                        fontSize = 13.sp
                    ) 
                },
                leadingIcon = if (selectedFilter == null) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            
            // Compliant filter
            FilterChip(
                selected = selectedFilter == "COMPLIANT",
                onClick = { onFilterSelected(if (selectedFilter == "COMPLIANT") null else "COMPLIANT") },
                label = { 
                    Text(
                        text = "Compliant ($compliantCount)",
                        fontSize = 13.sp
                    ) 
                },
                leadingIcon = if (selectedFilter == "COMPLIANT") {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            // Non-Compliant filter
            FilterChip(
                selected = selectedFilter == "NON_COMPLIANT",
                onClick = { onFilterSelected(if (selectedFilter == "NON_COMPLIANT") null else "NON_COMPLIANT") },
                label = { 
                    Text(
                        text = "Non-Compliant ($nonCompliantCount)",
                        fontSize = 13.sp
                    ) 
                },
                leadingIcon = if (selectedFilter == "NON_COMPLIANT") {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.error
                )
            )
        }
        
        // Not Applicable filter (second row if needed)
        FilterChip(
            selected = selectedFilter == "NOT_APPLICABLE",
            onClick = { onFilterSelected(if (selectedFilter == "NOT_APPLICABLE") null else "NOT_APPLICABLE") },
            label = { 
                Text(
                    text = "Not Applicable ($notApplicableCount)",
                    fontSize = 13.sp
                ) 
            },
            leadingIcon = if (selectedFilter == "NOT_APPLICABLE") {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.secondary
            )
        )
    }
}

