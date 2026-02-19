package com.pramod.validator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.pramod.validator.data.models.Fda483Assessment
import com.pramod.validator.viewmodel.Fda483ViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun Fda483MainScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToAssessmentDetail: (String) -> Unit,
    viewModel: Fda483ViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Upload 483", "History")
    
    val assessments by viewModel.assessments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val completedAssessmentId by viewModel.completedAssessmentId.collectAsState()
    val shouldShowHistoryTab by viewModel.shouldShowHistoryTab.collectAsState()
    
    // Switch to History tab when coming back from detail screen
    LaunchedEffect(shouldShowHistoryTab) {
        if (shouldShowHistoryTab) {
            selectedTab = 1 // Switch to History tab
            viewModel.clearShouldShowHistoryTab()
        }
    }
    
    // Navigate to detail screen when assessment completes
    LaunchedEffect(completedAssessmentId) {
        completedAssessmentId?.let { assessmentId ->
            viewModel.clearCompletedAssessmentId()
            onNavigateToAssessmentDetail(assessmentId)
        }
    }
    
    // Clear messages after showing
    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null || successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A)
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
                        "fda483" -> { /* Already on FDA 483 */ }
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Modern Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF3B82F6) // blue-500
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF3B82F6) else Color(0xFF64748B)
                            ) 
                        }
                    )
                }
            }
            
            // Messages
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2) // red-100
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFDC2626) // red-600
                    )
                }
            }
            
            successMessage?.let { success ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7) // green-100
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF16A34A) // green-600
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> ModernFda483UploadTab(
                    viewModel = viewModel,
                    isLoading = isLoading
                )
                1 -> ModernFda483HistoryTab(
                    assessments = assessments,
                    isLoading = isLoading,
                    onAssessmentClick = onNavigateToAssessmentDetail,
                    onDeleteAssessment = { viewModel.deleteAssessment(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ModernFda483UploadTab(
    viewModel: Fda483ViewModel,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val storagePermissionState = rememberPermissionState(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            selectedFileUri = it
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        selectedFileName = cursor.getString(nameIndex)
                    }
                }
            }
            selectedFileName = selectedFileName ?: "fda483.pdf"
        }
    }
    
    val isAndroid13Plus = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Upload icon with blue background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF3B82F6)), // blue-500
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = "Upload",
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Upload FDA 483 Document",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A) // slate-900
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Upload your FDA 483 inspection report as a PDF and receive instant AI-powered analysis to help you understand and address findings.",
            fontSize = 15.sp,
            color = Color(0xFF64748B), // slate-500
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // What you'll get section
        Text(
            text = "What you'll get:",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Feature items
        FeatureItem(
            icon = Icons.Default.Warning,
            iconColor = Color(0xFF3B82F6), // blue
            title = "Risk areas identified",
            description = "Critical findings highlighted with severity assessment"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureItem(
            icon = Icons.Default.Description,
            iconColor = Color(0xFF8B5CF6), // violet
            title = "Detailed explanations",
            description = "Plain-language breakdown of regulatory requirements"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureItem(
            icon = Icons.Default.CheckCircle,
            iconColor = Color(0xFF10B981), // emerald
            title = "Actionable checklist",
            description = "Step-by-step remediation plan for each observation"
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Select PDF button
        Button(
            onClick = {
                if (isAndroid13Plus) {
                    filePickerLauncher.launch("application/pdf")
                } else {
                    if (storagePermissionState.status.isGranted) {
                        filePickerLauncher.launch("application/pdf")
                    } else if (storagePermissionState.status.shouldShowRationale) {
                        storagePermissionState.launchPermissionRequest()
                    } else {
                        storagePermissionState.launchPermissionRequest()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6) // blue-500
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Upload, "Upload", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select PDF File", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Supported format: PDF (max 10MB)",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8) // slate-400
        )
        
        // Process button when file is selected
        selectedFileUri?.let { uri ->
            Spacer(modifier = Modifier.height(24.dp))
            
            // Selected file card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF), // blue-50
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)) // blue-200
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedFileName ?: "fda483.pdf",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Ready to process",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = {
                        selectedFileUri = null
                        selectedFileName = null
                    }) {
                        Icon(Icons.Default.Close, "Remove", tint = Color(0xFF64748B))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    selectedFileName?.let { fileName ->
                        viewModel.uploadAndProcessPdf(context, uri, fileName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.Send, "Process", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process FDA 483", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ModernFda483HistoryTab(
    assessments: List<Fda483Assessment>,
    isLoading: Boolean,
    onAssessmentClick: (String) -> Unit,
    onDeleteAssessment: (String) -> Unit
) {
    if (isLoading && assessments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
    } else if (assessments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "No history",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF94A3B8) // slate-400
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No FDA 483 assessments yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upload a FDA 483 document to get started",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(assessments) { assessment ->
                ModernFda483AssessmentCard(
                    assessment = assessment,
                    onClick = { onAssessmentClick(assessment.id) },
                    onDelete = { onDeleteAssessment(assessment.id) }
                )
            }
        }
    }
}

@Composable
private fun ModernFda483AssessmentCard(
    assessment: Fda483Assessment,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Determine color based on status
    val (gradientStart, gradientEnd) = when (assessment.status) {
        "completed" -> Pair(Color(0xFF10B981), Color(0xFF059669)) // emerald
        "processing" -> Pair(Color(0xFF3B82F6), Color(0xFF2563EB)) // blue
        "failed" -> Pair(Color(0xFFEF4444), Color(0xFFDC2626)) // red
        else -> Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)) // violet
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored gradient top bar - 6px height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(gradientStart, gradientEnd)
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with title and menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = assessment.fileName.removeSuffix(".pdf"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color(0xFF64748B)
                            )
                        }
                        
                        if (showMenu) {
                            val density = LocalDensity.current
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(
                                    with(density) { (-8).dp.roundToPx() },
                                    with(density) { 48.dp.roundToPx() }
                                ),
                                onDismissRequest = { showMenu = false }
                            ) {
                                Button(
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626) // red-600
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Delete",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Date and time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF64748B)
                    )
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(Date(assessment.uploadedAt)),
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
                
                // View Analysis button
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Analysis",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            title = { 
                Text(
                    "Delete Assessment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to delete this FDA 483 assessment?",
                    fontSize = 15.sp,
                    color = Color(0xFF64748B) // slate-500
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444) // red-500
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Delete",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF0F172A) // slate-900
                    )
                ) {
                    Text(
                        "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
