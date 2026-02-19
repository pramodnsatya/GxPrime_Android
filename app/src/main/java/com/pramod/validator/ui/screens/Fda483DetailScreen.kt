package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Fda483Assessment
import com.pramod.validator.data.models.RiskArea
import com.pramod.validator.data.models.ChecklistItem
import com.pramod.validator.viewmodel.Fda483ViewModel
import com.pramod.validator.viewmodel.CustomAssessmentViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fda483DetailScreen(
    assessmentId: String,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483History: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToCustomAssessment: () -> Unit,
    viewModel: Fda483ViewModel = viewModel()
) {
    val assessment by viewModel.currentAssessment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(assessmentId) {
        viewModel.getAssessmentById(assessmentId)
    }
    
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "FDA 483 Analysis",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateToFda483History) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to History",
                                tint = Color(0xFF0F172A) // slate-900
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A), // slate-900
                        navigationIconContentColor = Color(0xFF0F172A) // slate-900
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "fda483",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> onNavigateToHistory()
                        "fda483" -> { /* Already on FDA 483 - do nothing */ }
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
            if (isLoading && assessment == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (assessment == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFEF4444) // red-500
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Assessment not found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                // Header Section - File Name and Date
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // File Name
                        Text(
                            text = assessment!!.fileName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        
                        // Upload Date
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF64748B) // slate-500
                            )
                            Text(
                                text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                                    .format(Date(assessment!!.uploadedAt)),
                                fontSize = 15.sp,
                                color = Color(0xFF64748B) // slate-500
                            )
                        }
                    }
                }
                
                // Status indicator if processing
                if (assessment!!.status == "processing") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEF3C7) // amber-50
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFF59E0B) // amber-500
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Processing Analysis",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A) // slate-900
                                    )
                                    Text(
                                        text = "Your FDA 483 document is being analyzed. Please check back in a few moments.",
                                        fontSize = 14.sp,
                                        color = Color(0xFF64748B), // slate-500
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Summary Card
                if (assessment!!.summary.isNotEmpty()) {
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
                                    text = "Summary",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A) // slate-900
                                )
                                
                                Text(
                                    text = assessment!!.summary,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A), // slate-900
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                } else if (assessment!!.status == "completed" && assessment!!.summary.isEmpty()) {
                    // Show message if completed but no summary
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
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF64748B) // slate-500
                                )
                                Text(
                                    text = "Summary not available",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B) // slate-500
                                )
                            }
                        }
                    }
                }
                
                // Risk Areas Section
                if (assessment!!.riskAreas.isNotEmpty()) {
                    item {
                        Text(
                            text = "Identified Risk Areas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A), // slate-900
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(assessment!!.riskAreas.size) { index ->
                        val riskArea = assessment!!.riskAreas[index]
                        RiskAreaCard(riskArea = riskArea, index = index + 1)
                    }
                } else if (assessment!!.status == "completed" && assessment!!.riskAreas.isEmpty()) {
                    // Show message if completed but no risk areas
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
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Identified Risk Areas",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A), // slate-900
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF64748B) // slate-500
                                )
                                Text(
                                    text = "No risk areas identified",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B) // slate-500
                                )
                            }
                        }
                    }
                }
                
                // Checklist Section
                if (assessment!!.checklist.isNotEmpty()) {
                    item {
                        Text(
                            text = "Action Checklist",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A), // slate-900
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(assessment!!.checklist.size) { index ->
                        val checklistItem = assessment!!.checklist[index]
                        ChecklistItemCard(checklistItem = checklistItem, index = index + 1)
                    }
                    
                    // Create Assessment Button
                    item {
                        CreateAssessmentFromChecklistButton(
                            checklistItems = assessment!!.checklist,
                            fda483AssessmentId = assessment!!.id,
                            onNavigateToCustomAssessment = onNavigateToCustomAssessment,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else if (assessment!!.status == "completed" && assessment!!.checklist.isEmpty()) {
                    // Show message if completed but no checklist
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
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Action Checklist",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A), // slate-900
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF64748B) // slate-500
                                )
                                Text(
                                    text = "No checklist items available",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B) // slate-500
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskAreaCard(
    riskArea: RiskArea,
    index: Int
) {
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
            // Header with number and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Numbered circle - 48x48
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFFF1F5F9), // slate-100
                            shape = RoundedCornerShape(24.dp) // Full circle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569) // slate-600
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = riskArea.area,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                }
            }
            
            // Description
            Text(
                text = riskArea.description,
                fontSize = 15.sp,
                color = Color(0xFF0F172A), // slate-900
                lineHeight = 22.sp
            )
            
            // Specific Details
            if (riskArea.specificDetails.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC) // slate-50
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF3B82F6) // blue-500
                            )
                            Text(
                                text = "Specific Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3B82F6) // blue-500
                            )
                        }
                        
                        Text(
                            text = riskArea.specificDetails,
                            fontSize = 14.sp,
                            color = Color(0xFF64748B), // slate-500
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemCard(
    checklistItem: ChecklistItem,
    index: Int
) {
    val priorityColor = when (checklistItem.priority.lowercase()) {
        "high" -> Color(0xFFEF4444) // red-500
        "medium" -> Color(0xFF3B82F6) // blue-500
        "low" -> Color(0xFF10B981) // green-500
        else -> Color(0xFF64748B) // slate-500
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Numbered circle - 48x48
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFF1F5F9), // slate-100
                        shape = RoundedCornerShape(24.dp) // Full circle
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569) // slate-600
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = checklistItem.item,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A) // slate-900
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Priority badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = checklistItem.priority.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = priorityColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAssessmentFromChecklistButton(
    checklistItems: List<ChecklistItem>,
    fda483AssessmentId: String,
    onNavigateToCustomAssessment: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val customAssessmentViewModel: CustomAssessmentViewModel = viewModel()
    val successMessage by customAssessmentViewModel.successMessage.collectAsState()
    val errorMessage by customAssessmentViewModel.errorMessage.collectAsState()
    val isLoading by customAssessmentViewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Show feedback via Snackbar with "View" action for success
    LaunchedEffect(successMessage) {
        successMessage?.let { msg ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "View",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNavigateToCustomAssessment()
                }
                customAssessmentViewModel.clearMessages()
            }
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                customAssessmentViewModel.clearMessages()
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Snackbars for feedback
            SnackbarHost(hostState = snackbarHostState)

            Column {
                Text(
                    text = "Create Assessment from Checklist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Convert ${checklistItems.size} checklist items into an assessment",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B) // slate-500
                )
            }

            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6) // blue-500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Add, "Create", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create Assessment",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    
    if (showDialog) {
        CreateAssessmentDialog(
            onDismiss = { showDialog = false },
            onCreate = { assessmentName ->
                customAssessmentViewModel.createAssessmentFromChecklist(
                    checklistItems = checklistItems,
                    fda483AssessmentId = fda483AssessmentId,
                    assessmentName = assessmentName
                )
                showDialog = false
            }
        )
    }
    
    // Show success/error messages
    successMessage?.let { message ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    
    errorMessage?.let { message ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun CreateAssessmentDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var assessmentName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Assessment from Checklist") },
        text = {
            OutlinedTextField(
                value = assessmentName,
                onValueChange = { assessmentName = it },
                label = { Text("Assessment Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g., FDA 483 Follow-up Assessment") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (assessmentName.isNotBlank()) {
                        onCreate(assessmentName)
                    }
                },
                enabled = assessmentName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

