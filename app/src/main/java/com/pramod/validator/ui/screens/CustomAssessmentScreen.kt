package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import java.util.UUID
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.CustomAssessment
import com.pramod.validator.data.models.CustomQuestion
import com.pramod.validator.viewmodel.CustomAssessmentViewModel
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAssessmentScreen(
    onBack: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToCustomQuestionnaire: (String, String) -> Unit, // assessmentId, assessmentName
    onNavigateToCreateAssessment: () -> Unit,
    onNavigateToEditAssessment: (String) -> Unit, // assessmentId
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    initialTabIndex: Int = 0, // 0 = Create/View, 1 = FDA 483 Checklist
    viewModel: CustomAssessmentViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    var selectedTab by remember { mutableIntStateOf(initialTabIndex) }
    val tabs = listOf("Create/View", "FDA 483 Checklist")
    
    // Handle initial tab selection
    LaunchedEffect(initialTabIndex) {
        if (initialTabIndex in 0..1) {
            selectedTab = initialTabIndex
        }
    }
    
    val assessments by viewModel.assessments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val shouldShowChecklistTab by viewModel.shouldShowChecklistTab.collectAsState()
    
    // Switch to FDA 483 Checklist tab when flag is set (e.g., after creating assessment from checklist or returning from questionnaire)
    LaunchedEffect(shouldShowChecklistTab) {
        if (shouldShowChecklistTab) {
            selectedTab = 1 // Switch to FDA 483 Checklist tab
            viewModel.clearShouldShowChecklistTab()
        }
    }
    
    // Also check flag when screen becomes visible (in case flag was set before navigation)
    LaunchedEffect(Unit) {
        viewModel.loadUserCustomAssessments()
        // Check flag on screen entry
        if (shouldShowChecklistTab) {
            selectedTab = 1
            viewModel.clearShouldShowChecklistTab()
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
                            "Custom Assessments",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
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
                currentRoute = "custom",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> onNavigateToHistory()
                        "fda483" -> onNavigateToFda483()
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
                    0 -> CustomAssessmentCreateViewTab(
                        assessments = assessments.filter { !it.isFromChecklist },
                        isLoading = isLoading,
                        onAssessmentClick = { assessmentId ->
                            // Find assessment name from list
                            val assessment = assessments.find { it.id == assessmentId }
                            onNavigateToCustomQuestionnaire(assessmentId, assessment?.name ?: "Custom Assessment")
                        },
                        onDeleteAssessment = { viewModel.deleteCustomAssessment(it) },
                        onEditAssessment = { assessmentId ->
                            onNavigateToEditAssessment(assessmentId)
                        },
                        onCreateAssessment = { name, questions ->
                            viewModel.createCustomAssessment(name, questions)
                        },
                        viewModel = viewModel,
                        onNavigateToCreateAssessment = onNavigateToCreateAssessment
                    )
                    1 -> CustomAssessmentChecklistTab(
                        assessments = assessments.filter { it.isFromChecklist },
                        isLoading = isLoading,
                        onAssessmentClick = { assessmentId, assessmentName ->
                            // Set flag to show Checklist tab when returning
                            viewModel.setShouldShowChecklistTab(true)
                            onNavigateToCustomQuestionnaire(assessmentId, assessmentName)
                        },
                        onDeleteAssessment = { viewModel.deleteCustomAssessment(it) },
                        onEditAssessment = { assessmentId ->
                            // Set flag to show Checklist tab when returning
                            viewModel.setShouldShowChecklistTab(true)
                            onNavigateToEditAssessment(assessmentId)
                        }
                    )
                }
            }
        }
    }

@Composable
fun CustomAssessmentCreateViewTab(
    assessments: List<CustomAssessment>,
    isLoading: Boolean,
    onAssessmentClick: (String) -> Unit,
    onDeleteAssessment: (String) -> Unit,
    onEditAssessment: (String) -> Unit,
    onCreateAssessment: (String, List<CustomQuestion>) -> Unit,
    viewModel: CustomAssessmentViewModel,
    onNavigateToCreateAssessment: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Create Assessment Button (navigate to full-screen create page)
        Button(
            onClick = onNavigateToCreateAssessment,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 20.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6) // blue-500
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, "Create", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Create Assessment",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (isLoading && assessments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (assessments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "No assessments",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No custom assessments yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create your first custom assessment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(assessments) { assessment ->
                    ModernCustomAssessmentCard(
                        assessment = assessment,
                        onClick = { onAssessmentClick(assessment.id) },
                        onDelete = { onDeleteAssessment(assessment.id) },
                        onEdit = { onEditAssessment(assessment.id) }
                    )
                }
            }
        }
    }
    
    // Dialog removed in favor of full-screen page
}

@Composable
fun CreateCustomAssessmentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, List<CustomQuestion>) -> Unit
) {
    var assessmentName by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf<List<CustomQuestion>>(emptyList()) }
    var currentQuestionText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Assessment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Assessment Name
                OutlinedTextField(
                    value = assessmentName,
                    onValueChange = { assessmentName = it },
                    label = { Text("Assessment Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Divider()
                
                // Question Input
                OutlinedTextField(
                    value = currentQuestionText,
                    onValueChange = { currentQuestionText = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter a question...") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (currentQuestionText.isNotBlank()) {
                                    val newQuestion = CustomQuestion(
                                        id = UUID.randomUUID().toString(),
                                        questionText = currentQuestionText.trim(),
                                        order = questions.size + 1
                                    )
                                    questions = questions + newQuestion
                                    currentQuestionText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, "Add Question")
                        }
                    }
                )
                
                // Questions List
                if (questions.isNotEmpty()) {
                    Text(
                        text = "Questions (${questions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    questions.forEachIndexed { index, question ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = question.questionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        questions = questions.filter { it.id != question.id }
                                            .mapIndexed { idx, q -> q.copy(order = idx + 1) }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (assessmentName.isNotBlank() && questions.isNotEmpty()) {
                        onCreate(assessmentName, questions)
                    }
                },
                enabled = assessmentName.isNotBlank() && questions.isNotEmpty()
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

@Composable
fun ModernCustomAssessmentCard(
    assessment: CustomAssessment,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Use indigo gradient for custom assessments
    val gradientStart = Color(0xFF6366F1) // indigo-500
    val gradientEnd = Color(0xFF4F46E5) // indigo-600
    
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Numbered circle - 48x48 (using question count or index)
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
                        text = "${assessment.questions.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569) // slate-600
                    )
                }
                
                // Assessment info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = assessment.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Date and time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF64748B) // slate-500
                        )
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                                .format(Date(assessment.createdAt)),
                            fontSize = 14.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${assessment.questions.size} questions",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
                
                // Menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            "Menu",
                            tint = Color(0xFF64748B), // slate-500
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Update/Add Questions") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF64748B))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        )
                    }
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
                    "Are you sure you want to delete this custom assessment?",
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

@Composable
fun CustomAssessmentChecklistTab(
    assessments: List<CustomAssessment>,
    isLoading: Boolean,
    onAssessmentClick: (String, String) -> Unit,
    onDeleteAssessment: (String) -> Unit,
    onEditAssessment: (String) -> Unit
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
                    imageVector = Icons.Default.Checklist,
                    contentDescription = "No assessments",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF94A3B8) // slate-400
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No checklist assessments yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A) // slate-900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create assessments from FDA 483 checklists",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B), // slate-500
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
            item {
                Text(
                    text = "FDA 483 Checklist Assessments",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A), // slate-900
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(assessments) { assessment ->
                ModernCustomAssessmentCard(
                    assessment = assessment,
                    onClick = { onAssessmentClick(assessment.id, assessment.name) },
                    onDelete = { onDeleteAssessment(assessment.id) },
                    onEdit = { onEditAssessment(assessment.id) }
                )
            }
        }
    }
}

