package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.ui.components.QuestionResponseCard
import com.pramod.validator.ui.components.ResponseFilterChips
import com.pramod.validator.viewmodel.ReportViewModel
import com.pramod.validator.utils.NetworkMonitor
import org.json.JSONObject
import org.json.JSONArray

// Data classes for parsed summary data
data class ParsedSummaryData(
    val strengths: List<String>,
    val issues: List<ParsedIssue>,
    val nextSteps: List<String>,
    val parseError: Exception?
)

data class ParsedIssue(
    val area: String,
    val problem: String,
    val improvement: String,
    val where: String,
    val how: String
)

// Function to parse JSON summary and format as readable text
private fun formatAISummary(jsonSummary: String): String {
    return try {
        android.util.Log.d("formatAISummary", "Raw AI summary: $jsonSummary")
        
        if (jsonSummary.isBlank()) {
            android.util.Log.w("formatAISummary", "AI summary is blank")
            return "Summary is being generated. Please wait..."
        }
        
        val json = JSONObject(jsonSummary)
        val summary = StringBuilder()
        
        // Summary title
        summary.append("Assessment Summary\n\n")
        
        // Key strengths
        if (json.has("strengths")) {
            summary.append("Key Strengths:\n")
            val strengths = json.getJSONArray("strengths")
            for (i in 0 until strengths.length()) {
                summary.append("• ${strengths.getString(i)}\n")
            }
            summary.append("\n")
        }
        
        // Issues/Areas for improvement
        if (json.has("issues")) {
            summary.append("Critical Areas for Improvement:\n")
            val issues = json.getJSONArray("issues")
            for (i in 0 until issues.length()) {
                val item = issues.getJSONObject(i)
                summary.append("• ${item.getString("area")}: ${item.getString("problem")}\n")
                summary.append("  How to improve: ${item.getString("improvement")}\n")
                summary.append("  Where: ${item.getString("where")}\n")
                summary.append("  How: ${item.getString("how")}\n\n")
            }
        }
        
        val result = summary.toString()
        android.util.Log.d("formatAISummary", "Formatted result: $result")
        
        if (result.isBlank() || result == "Assessment Summary\n\n") {
            android.util.Log.w("formatAISummary", "Result is empty or only has title")
            "AI summary is being generated. Please wait..."
        } else {
            result
        }
    } catch (e: Exception) {
        android.util.Log.e("formatAISummary", "Error parsing summary: ${e.message}", e)
        android.util.Log.e("formatAISummary", "Raw summary that failed to parse: $jsonSummary")
        
        // Check if this is an AI generation failure message (happens when offline)
        if (jsonSummary.contains("AI Summary generation failed", ignoreCase = true) || 
            jsonSummary.contains("Failed to connect", ignoreCase = true)) {
            android.util.Log.w("formatAISummary", "Detected AI generation failure, showing user-friendly message")
            return "Summary will be generated once internet connection is restored. Please check back later."
        }
        
        // If JSON parsing fails for other reasons, show generic message
        if (jsonSummary.isNotBlank()) {
            android.util.Log.w("formatAISummary", "Non-empty summary but failed to parse, showing generic message")
            "Summary is being generated. Please wait..."
        } else {
            "Summary is being generated. Please wait..."
        }
    }
}

@Composable
fun ReportRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun FormattedAISummary(
    aiSummary: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    // Parse JSON outside of composable using remember
    val parsedData = remember(aiSummary) {
        try {
            val json = JSONObject(aiSummary)
            ParsedSummaryData(
                strengths = if (json.has("strengths")) {
                    val arr = json.getJSONArray("strengths")
                    (0 until arr.length()).map { arr.getString(it) }
                } else emptyList(),
                issues = if (json.has("issues")) {
                    val arr = json.getJSONArray("issues")
                    (0 until arr.length()).map {
                        val item = arr.getJSONObject(it)
                        ParsedIssue(
                            area = item.getString("area"),
                            problem = item.getString("problem"),
                            improvement = item.getString("improvement"),
                            where = item.getString("where"),
                            how = item.getString("how")
                        )
                    }
                } else emptyList(),
                nextSteps = if (json.has("next_steps")) {
                    val arr = json.getJSONArray("next_steps")
                    (0 until arr.length()).map { arr.getString(it) }
                } else emptyList(),
                parseError = null
            )
        } catch (e: Exception) {
            android.util.Log.e("FormattedAISummary", "Error parsing summary: ${e.message}", e)
            ParsedSummaryData(
                strengths = emptyList(),
                issues = emptyList(),
                nextSteps = emptyList(),
                parseError = e
            )
        }
    }
    
    Column {
        if (parsedData.parseError != null) {
            // Fallback to plain text if JSON parsing fails
            Text(
                text = formatAISummary(aiSummary),
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = Color(0xFF0F172A), // slate-900
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            // Key Strengths Section
            if (parsedData.strengths.isNotEmpty()) {
                Text(
                    text = "Key Strengths:",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981) // emerald-500
                )
                Spacer(modifier = Modifier.height(8.dp))
                parsedData.strengths.forEach { strength ->
                    Row(
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "• ",
                            fontSize = fontSize,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Text(
                            text = strength,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A), // slate-900
                            lineHeight = lineHeight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Critical Areas for Improvement Section
            if (parsedData.issues.isNotEmpty()) {
                Text(
                    text = "Critical Areas for Improvement:",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444) // red-500
                )
                Spacer(modifier = Modifier.height(8.dp))
                parsedData.issues.forEach { issue ->
                    Column(
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    ) {
                        Row {
                            Text(
                                text = "• ",
                                fontSize = fontSize,
                                color = Color(0xFF0F172A) // slate-900
                            )
                            Text(
                                text = "${issue.area}: ${issue.problem}",
                                fontSize = fontSize,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF0F172A), // slate-900
                                lineHeight = lineHeight
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "How to improve: ${issue.improvement}",
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A), // slate-900
                            lineHeight = lineHeight,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Where: ${issue.where}",
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A), // slate-900
                            lineHeight = lineHeight,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "How: ${issue.how}",
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A), // slate-900
                            lineHeight = lineHeight,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            
            // Next Steps Section (if exists)
            if (parsedData.nextSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next Steps:",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
                Spacer(modifier = Modifier.height(8.dp))
                parsedData.nextSteps.forEach { step ->
                    Row(
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "• ",
                            fontSize = fontSize,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Text(
                            text = step,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A), // slate-900
                            lineHeight = lineHeight
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    domainId: String,
    domainName: String,
    subDomainId: String,
    subDomainName: String,
    assessmentName: String,
    facilityId: String,
    facilityName: String,
    responses: Map<String, AnswerType>,
    questionTexts: Map<String, String> = emptyMap(), // Kept for navigation compatibility
    onBackToHome: () -> Unit,
    reportViewModel: ReportViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val report by reportViewModel.report.collectAsState()
    val isLoading by reportViewModel.isLoading.collectAsState()
    val aiSummaryPending by reportViewModel.aiSummaryPending.collectAsState()
    
    // Filter state for detailed responses
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    // Collapse state for detailed responses
    var isResponsesExpanded by remember { mutableStateOf(true) }

    // Network monitor to check connectivity
    val networkMonitor = remember { com.pramod.validator.utils.NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())
    
    // Monitor connectivity changes and retry AI summary generation for pending reports
    LaunchedEffect(isOnline) {
        if (isOnline) {
            val currentReport = report
            if (currentReport != null && currentReport.aiSummaryStatus == "pending") {
                android.util.Log.d("ReportScreen", "🌐 Internet restored! Retrying AI summary for pending report: ${currentReport.id}")
                
                // Retry generating the AI summary for this specific report
                if (domainId == "custom" && questionTexts.isNotEmpty()) {
                    reportViewModel.generateCustomAssessmentReport(
                        assessmentId = subDomainId,
                        assessmentName = assessmentName,
                        responses = responses,
                        questionTexts = questionTexts,
                        context = context
                    )
                } else {
                    reportViewModel.generateReport(
                        domainId,
                        domainName,
                        subDomainId,
                        subDomainName,
                        assessmentName,
                        facilityId,
                        facilityName,
                        responses,
                        questionTexts = questionTexts,
                        context = context
                    )
                }
            }
        }
    }
    
    LaunchedEffect(domainId, subDomainId, responses, questionTexts) {
        // Check if report already exists and matches current assessment
        val existingReport = report
        val reportMatches = existingReport != null && 
                           existingReport.domainId == domainId && 
                           existingReport.subDomainId == subDomainId
        
        // If report already exists and matches, don't regenerate
        if (reportMatches) {
            android.util.Log.d("ReportScreen", "✅ Report already exists, skipping generation")
            return@LaunchedEffect
        }
        
        // Check network status
        val currentlyOnline = networkMonitor.isCurrentlyOnline()
        
        // If offline and we don't have a matching report, generate one (will be fast, marks AI as pending)
        // If online, generate normally
        if (domainId == "custom" && questionTexts.isNotEmpty()) {
            // Use the provided question texts for custom assessments
            reportViewModel.generateCustomAssessmentReport(
                assessmentId = subDomainId,
                assessmentName = assessmentName,
                responses = responses,
                questionTexts = questionTexts,
                context = context
            )
        } else {
            reportViewModel.generateReport(
                domainId,
                domainName,
                subDomainId,
                subDomainName,
                assessmentName,
                facilityId,
                facilityName,
                responses,
                questionTexts = questionTexts, // Pass question texts already loaded during assessment
                context = context
            )
        }
    }

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
                        IconButton(onClick = onBackToHome) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onBackToHome) {
                            Icon(
                                imageVector = Icons.Default.Home,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
        ) {
            // Only show loading if we don't have a report yet
            // If report exists (even with pending status), show the report content
            val shouldShowLoading = isLoading && report == null
            
            if (shouldShowLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF1E3A8A) // blue-900
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Generating Summary...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Analyzing your responses and identifying key areas. This may take a moment.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B), // slate-500
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else if (report != null) {
                report?.let { currentReport ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Section - Assessment Name
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Assessment Name
                            Text(
                                text = currentReport.assessmentName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A) // slate-900
                            )
                            
                            // Facility Name (if available)
                            if (currentReport.facilityName.isNotEmpty()) {
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
                                        text = currentReport.facilityName,
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
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
                                    color = Color(0xFF64748B) // slate-500
                                )

                                Text(
                                    text = "${reportViewModel.getCompliancePercentage().toInt()}%",
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A) // slate-900
                                )
                            }
                        }
                    }
                    
                    // Responses Pie Chart
                    item {
                        com.pramod.validator.ui.components.ResponsesPieChart(
                            totalQuestions = currentReport.totalQuestions,
                            compliantCount = currentReport.compliantCount,
                            nonCompliantCount = currentReport.nonCompliantCount,
                            notApplicableCount = currentReport.notApplicableCount,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // AI Summary Section
                    if (currentReport.aiSummaryStatus == "pending" || aiSummaryPending) {
                        item {
                            // Show message when AI summary is pending
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Assessment Saved Successfully",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A) // slate-900
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "Your assessment has been saved to history. Please connect to the internet and check the Assessment History section later to view the AI-generated summary.",
                                        fontSize = 14.sp,
                                        color = Color(0xFF64748B), // slate-500
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    } else if (currentReport.aiSummary.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Assessment Summary title
                                    Text(
                                        text = "Assessment Summary",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A) // slate-900
                                    )
                                    
                                    // Render formatted AI summary with proper styling
                                    FormattedAISummary(
                                        aiSummary = currentReport.aiSummary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    // Detailed Responses Section
                    if (currentReport.responses.isNotEmpty() && currentReport.questionTexts.isNotEmpty()) {
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
                                    color = Color(0xFF0F172A) // slate-900
                                )
                                
                                IconButton(onClick = { isResponsesExpanded = !isResponsesExpanded }) {
                                    Icon(
                                        imageVector = if (isResponsesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isResponsesExpanded) "Collapse" else "Expand",
                                        tint = Color(0xFF64748B) // slate-500
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
                                    label = "All (${currentReport.totalQuestions})",
                                    isSelected = selectedFilter == null,
                                    onClick = { selectedFilter = null }
                                )
                                FilterChipButton(
                                    label = "Compliant (${currentReport.compliantCount})",
                                    isSelected = selectedFilter == "COMPLIANT",
                                    onClick = { selectedFilter = "COMPLIANT" }
                                )
                                FilterChipButton(
                                    label = "Non-Compliant (${currentReport.nonCompliantCount})",
                                    isSelected = selectedFilter == "NON_COMPLIANT",
                                    onClick = { selectedFilter = "NON_COMPLIANT" }
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
                                    label = "N/A (${currentReport.notApplicableCount})",
                                    isSelected = selectedFilter == "NOT_APPLICABLE",
                                    onClick = { selectedFilter = "NOT_APPLICABLE" }
                                )
                            }
                        }
                        
                        // Filter responses - responses are stored as enum names: "COMPLIANT", "NON_COMPLIANT", "NOT_APPLICABLE"
                        val filteredResponses = when (selectedFilter) {
                            "COMPLIANT" -> currentReport.responses.filter { it.value == "COMPLIANT" }
                            "NON_COMPLIANT" -> currentReport.responses.filter { it.value == "NON_COMPLIANT" }
                            "NOT_APPLICABLE" -> currentReport.responses.filter { it.value == "NOT_APPLICABLE" }
                            else -> currentReport.responses
                        }
                        
                        // Display filtered responses (only if expanded)
                        if (isResponsesExpanded) {
                            items(filteredResponses.toList().sortedBy { it.first }) { (questionId, answer) ->
                                val index = filteredResponses.toList().sortedBy { it.first }.indexOfFirst { it.first == questionId }
                                val questionText = currentReport.questionTexts[questionId] ?: "Question text not available"
                                
                                com.pramod.validator.ui.components.QuestionResponseCard(
                                    questionNumber = index + 1,
                                    questionText = questionText,
                                    answer = answer,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    containerColor = Color.White
                                )
                            }
                            
                            // Show message if no results after filtering
                            if (filteredResponses.isEmpty() && selectedFilter != null) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Text(
                                            text = "No responses match the selected filter",
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B), // slate-500
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Back to Home Button
                    item {
                        Button(
                            onClick = onBackToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E3A8A) // blue-900
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Back to Home",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
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
        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFF1F5F9), // blue-900 or slate-100
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
                color = if (isSelected) Color.White else Color(0xFF64748B) // white or slate-500
            )
        }
    }
}

