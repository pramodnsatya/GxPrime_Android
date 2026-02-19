package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Report
import com.pramod.validator.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    onBack: () -> Unit,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val reports by historyViewModel.reports.collectAsState()
    val report = reports.find { it.id == reportId }
    
    // Filter state for detailed responses
    var selectedFilter by remember { mutableStateOf("All") }
    
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color(0xFF1E3A8A) // blue-900 (same as GxPrime header)
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "Assessment Report",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Navigate to home */ }) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E3A8A), // blue-900
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Report not found", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Section - Domain > Subdomain, Assessment Name, Facility
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Domain > Subdomain
                        Text(
                            text = if (report.subDomainName.isNotEmpty()) {
                                "${report.domainName} > ${report.subDomainName}"
                            } else {
                                report.domainName
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B) // slate-500
                        )
                        
                        // Assessment Name
                        Text(
                            text = report.assessmentName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        
                        // Facility Name
                        if (report.facilityName.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF64748B)
                                )
                                Text(
                                    text = report.facilityName,
                                    fontSize = 15.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
                
                // Compliance Score Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Compliance Score",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            
                            val compliancePercentage = if (report.compliantCount + report.nonCompliantCount > 0) {
                                (report.compliantCount.toFloat() / (report.compliantCount + report.nonCompliantCount)) * 100
                            } else 0f
                            
                            Text(
                                text = "${compliancePercentage.toInt()}%",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
                
                // Responses Overview (Pie Chart)
                item {
                    com.pramod.validator.ui.components.ResponsesPieChart(
                        totalQuestions = report.totalQuestions,
                        compliantCount = report.compliantCount,
                        nonCompliantCount = report.nonCompliantCount,
                        notApplicableCount = report.notApplicableCount
                    )
                }
                
                // Assessment Summary
                if (report.aiSummary.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Assessment Summary",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                
                                // Parse and render AI summary with proper formatting
                                RenderAISummaryWithFormatting(report.aiSummary)
                            }
                        }
                    }
                }
                
                // Detailed Responses Section
                if (report.responses.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Detailed Responses",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            
                            IconButton(onClick = { /* Toggle collapse */ }) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Collapse",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    
                    // Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChipButton(
                                label = "All (${report.totalQuestions})",
                                isSelected = selectedFilter == "All",
                                onClick = { selectedFilter = "All" }
                            )
                            FilterChipButton(
                                label = "Compliant (${report.compliantCount})",
                                isSelected = selectedFilter == "Compliant",
                                onClick = { selectedFilter = "Compliant" }
                            )
                            FilterChipButton(
                                label = "Non-Compliant (${report.nonCompliantCount})",
                                isSelected = selectedFilter == "Non-Compliant",
                                onClick = { selectedFilter = "Non-Compliant" }
                            )
                        }
                    }
                    
                    // Second row for N/A filter
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChipButton(
                                label = "N/A (${report.notApplicableCount})",
                                isSelected = selectedFilter == "N/A",
                                onClick = { selectedFilter = "N/A" }
                            )
                        }
                    }
                    
                    // Filter responses - responses are stored as enum names: "COMPLIANT", "NON_COMPLIANT", "NOT_APPLICABLE"
                    val filteredResponses = when (selectedFilter) {
                        "Compliant" -> report.responses.filter { it.value == "COMPLIANT" }
                        "Non-Compliant" -> report.responses.filter { it.value == "NON_COMPLIANT" }
                        "N/A" -> report.responses.filter { it.value == "NOT_APPLICABLE" }
                        else -> report.responses
                    }
                    
                    android.util.Log.d("ReportDetail", "Total responses: ${report.responses.size}")
                    android.util.Log.d("ReportDetail", "Selected filter: $selectedFilter")
                    android.util.Log.d("ReportDetail", "Filtered responses: ${filteredResponses.size}")
                    
                    // Display filtered responses
                    items(filteredResponses.toList().sortedBy { it.first }) { (questionId, answer) ->
                        val index = filteredResponses.toList().sortedBy { it.first }.indexOfFirst { it.first == questionId }
                        val questionText = report.questionTexts[questionId] ?: "Question text not available"
                        
                        com.pramod.validator.ui.components.QuestionResponseCard(
                            questionNumber = index + 1,
                            questionText = questionText,
                            answer = answer,
                            modifier = Modifier.padding(bottom = 12.dp),
                            containerColor = Color.White
                        )
                    }
                    
                    // Show message if no results after filtering
                    if (filteredResponses.isEmpty() && selectedFilter != "All") {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "No responses match the selected filter",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                )
                            }
                        }
                    }
                }
                
                // Back to Home Button
                item {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Back to Home",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF1F5F9),
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun ModernQuestionCard(
    questionNumber: Int,
    questionText: String,
    answer: String
) {
    // Large gray background card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC) // Light gray background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question with number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Q$questionNumber",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = questionText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Status badge with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val (statusColor, statusText) = when (answer) {
                    "Compliant" -> Pair(Color(0xFF64748B), "COMPLIANT")
                    "Non-Compliant" -> Pair(Color(0xFF64748B), "NON_COMPLIANT")
                    else -> Pair(Color(0xFF64748B), "NOT APPLICABLE")
                }
                
                // Small circular icon
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(statusColor, shape = CircleShape)
                )
                
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


@Composable
private fun RenderAISummaryWithFormatting(aiSummary: String) {
    // Use the existing FormattedAISummary component which already works
    FormattedAISummary(
        aiSummary = aiSummary,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}
