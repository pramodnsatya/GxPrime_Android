package com.pramod.validator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Domain
import com.pramod.validator.data.models.InProgressAssessment
import com.pramod.validator.viewmodel.HomeViewModel
import com.pramod.validator.viewmodel.InProgressAssessmentViewModel
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.ui.components.OfflineIndicator
import com.pramod.validator.ui.components.AbstractBackground
import com.pramod.validator.utils.NetworkMonitor
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDomainSelected: (Domain) -> Unit, // Navigate to subdomain selection
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToCustomAssessment: () -> Unit,
    onResumeAssessment: (String) -> Unit, // assessmentId
    onSignOut: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    inProgressViewModel: InProgressAssessmentViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val domains by homeViewModel.domains.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val inProgressAssessments by inProgressViewModel.assessments.collectAsState()
    val inProgressLoading by inProgressViewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    val completedReportsCount by homeViewModel.completedReportsCount.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionDialogMessage by remember { mutableStateOf("") }
    
    // Network connectivity monitoring
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())
    
    // Extract first name from displayName
    val firstName = remember(currentUser?.displayName) {
        currentUser?.displayName?.split(" ")?.firstOrNull() ?: "User"
    }
    
    // Reload in-progress assessments and completed reports when screen appears
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "🔄 Loading in-progress assessments...")
        inProgressViewModel.loadUserInProgressAssessments()
        homeViewModel.loadCompletedReportsCount()
    }
    
    // Log when assessments list changes
    LaunchedEffect(inProgressAssessments.size) {
        android.util.Log.d("HomeScreen", "📊 In-progress assessments count: ${inProgressAssessments.size}")
        inProgressAssessments.forEachIndexed { index, assessment ->
            android.util.Log.d("HomeScreen", "   [$index] ${assessment.assessmentName} (${assessment.id})")
        }
    }

    Scaffold(
        topBar = {
            // Gradient top app bar with logo - from-slate-900 via-blue-900 to-slate-900
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A), // slate-900
                                Color(0xFF1E3A8A), // blue-900
                                Color(0xFF0F172A)  // slate-900
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Logo square with "Gx" - gradient from-blue-400 to-purple-500
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF60A5FA), // blue-400
                                            Color(0xFFA855F7)  // purple-500
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                    ) {
                        Text(
                                text = "Gx",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "GxPrime",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "home" -> { /* Already on home - do nothing */ }
                        "history" -> onNavigateToHistory()
                        "fda483" -> onNavigateToFda483()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
                    }
                },
                user = currentUser,
                permissions = currentUserPermissions
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Offline indicator
                    item {
                        OfflineIndicator(
                            isOnline = isOnline,
                    modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    
                    // Welcome banner with gradient - overlapping with metrics card
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Gradient banner - extended to allow overlap
                            WelcomeBanner(
                                firstName = firstName,
                                onViewHistory = onNavigateToHistory,
                                modifier = Modifier.padding(bottom = 70.dp) // More space between button and metrics
                            )
                            
                            // Assessment Summary Card - positioned to overlap at bottom
                            AssessmentSummaryCard(
                                completedCount = completedReportsCount,
                                inProgressCount = inProgressAssessments.size,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                    
                    // Assessment Domains Section Header
                    item {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
                        ) {
                    Text(
                                text = "Assessment Domains",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    
                    // Domain cards - 2 column grid
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(((domains.size + 1 + 1) / 2 * 200).dp), // +1 for custom tile, calculate proper height
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false // Disable internal scrolling since we're in a LazyColumn
                        ) {
                            items(domains.size) { index ->
                                ModernDomainCard(
                                    domain = domains[index],
                                    onClick = {
                                        if (com.pramod.validator.utils.PermissionChecker.canCreateAssessments(currentUser, currentUserPermissions)) {
                                            onDomainSelected(domains[index])
                                    } else {
                                            permissionDialogMessage = "You don't have access to create assessments. Please contact your IT admin to grant you assessment creation permissions."
                                            showPermissionDialog = true
                                        }
                                    }
                                )
                            }
                            
                            // Custom assessment tile
                            item {
                                ModernCustomAssessmentTile(
                                    onClick = {
                                        if (com.pramod.validator.utils.PermissionChecker.canCreateAssessments(currentUser, currentUserPermissions)) {
                                            onNavigateToCustomAssessment()
                                        } else {
                                            permissionDialogMessage = "You don't have access to create assessments. Please contact your IT admin to grant you assessment creation permissions."
                                            showPermissionDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    // In Progress Section Header
                    item {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
                        ) {
                        Text(
                                text = "In Progress",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Continue where you left off",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                        
                    // In Progress Assessments
                    if (inProgressLoading) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Loading your in-progress assessments...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else if (inProgressAssessments.isNotEmpty()) {
                        items(inProgressAssessments) { assessment ->
                            ModernInProgressAssessmentCard(
                                    assessment = assessment,
                                    onResume = { onResumeAssessment(assessment.id) },
                                onDelete = { showDeleteDialog = assessment.id },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Text(
                                        "No in-progress assessments",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Start a new assessment to see it appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        showDeleteDialog?.let { assessmentId ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                containerColor = Color.White,
                title = { 
                    Text(
                        "Delete Assessment?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    ) 
                },
                text = { 
                    Text(
                        "Are you sure you want to delete this in-progress assessment?",
                        fontSize = 15.sp,
                        color = Color(0xFF64748B) // slate-500
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            inProgressViewModel.deleteInProgressAssessment(assessmentId)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A) // blue-900
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
                        onClick = { showDeleteDialog = null },
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
    
    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Permission Denied",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { 
                Text(
                    "Access Denied",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    permissionDialogMessage,
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                Button(
                    onClick = { showPermissionDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
}

// Modern Components - defined here for proper scope resolution

@Composable
private fun ModernCustomAssessmentTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientStart = Color(0xFF6366F1) // indigo-500
    val gradientEnd = Color(0xFF4F46E5) // indigo-600
    
    Box(
        modifier = modifier.height(180.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Icon Container - 48x48 with neutral background (no color coding)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFFF1F5F9), // slate-100 - neutral gray
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF64748B), // slate-500 - neutral gray
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Title only (no description)
                    Text(
                        text = "Custom",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                }
                
                Column {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0)) // slate-200
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Start assessment link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start assessment",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernInProgressAssessmentCard(
    assessment: InProgressAssessment,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val totalQuestions = assessment.totalQuestions.takeIf { it > 0 } ?: assessment.questionTexts.size
    val currentProgress = assessment.currentQuestionIndex + 1
    val progressPercentage = if (totalQuestions > 0) {
        (currentProgress.toFloat() / totalQuestions.toFloat()) * 100f
    } else {
        0f
    }
    
    // Format time ago
    val timeAgo = remember(assessment.updatedAt) {
        val diff = System.currentTimeMillis() - assessment.updatedAt
        when {
            diff < 3600000L -> "${diff / 60000L} minutes ago"
            diff < 86400000L -> "${diff / 3600000L} hours ago"
            else -> "${diff / 86400000L} days ago"
        }
    }
    
    // Use darker purple gradient for all in-progress tiles
    val (gradientStart, gradientEnd) = Pair(Color(0xFF7C3AED), Color(0xFF6D28D9)) // purple-600 to purple-700 - darker purple
    
    // Single color for badge
    val domainColor = gradientStart
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Progress bar at top - 4px height with multi-color gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFFF1F5F9)) // slate-100
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercentage / 100f)
                        .height(4.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(gradientStart, gradientEnd)
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top row: Domain badge and menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Domain badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = domainColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = assessment.domainName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
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
                                        onDelete()
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
                
                // Assessment name
                Text(
                    text = assessment.assessmentName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A) // slate-900
                )
                
                // Location if available
                if (assessment.facilityName.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF64748B), // slate-500
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = assessment.facilityName,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }
                
                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0)) // slate-200
                )
                
                // Bottom row: Progress, time, and Resume button - all in ONE LINE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Progress and time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Progress section - stacked layout
                        Column {
                            Text(
                                text = "Progress",
                                fontSize = 13.sp,
                                color = Color(0xFF7C3AED), // purple-600 - darker purple
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$currentProgress / $totalQuestions",
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A), // slate-900 - black
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF64748B), // slate-500
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = timeAgo,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B) // slate-500
                            )
                        }
                    }
                    
                    // Right side: Resume button
                    Button(
                        onClick = onResume,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB) // blue-600
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resume",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainCard(
    domain: Domain,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = domain.icon,
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = domain.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Tap to start",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomAssessmentTile(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📝",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Custom",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Tap to start",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InProgressAssessmentCard(
    assessment: InProgressAssessment,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = assessment.assessmentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (assessment.facilityName.isNotEmpty()) {
                    Text(
                        text = "Facility: ${assessment.facilityName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "Domain: ${assessment.domainName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Progress: ${assessment.currentQuestionIndex + 1} / ${assessment.totalQuestions.takeIf { it > 0 } ?: assessment.questionTexts.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Resume", fontSize = 14.sp)
                }
                
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// New Modern Components - moved here for proper scope

@Composable
private fun WelcomeBanner(
    firstName: String,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val currentDate = dateFormat.format(Date())
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2563EB), // blue-600
                        Color(0xFF1E40AF), // blue-700
                        Color(0xFF7C3AED)  // purple-700
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 32.dp)
    ) {
        Column {
            // Date and icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = currentDate,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            
            // Welcome message
            Text(
                text = "Welcome back, $firstName!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Subtitle
            Text(
                text = "Select a domain to start your assessment",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // View History Button - white/20 background, white/30 border
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "View History",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AssessmentSummaryCard(
    completedCount: Int,
    inProgressCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completed
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$completedCount",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6) // blue-500 for completed
                )
                Text(
                    text = "Completed",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color(0xFFE2E8F0)) // slate-200
            )
            
            // In Progress
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$inProgressCount",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA855F7) // purple-500 for in progress
                )
                Text(
                    text = "In Progress",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernDomainCard(
    domain: Domain,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get solid colors based on domain name - matching actual app domain names
    val domainColor = when {
        domain.name.contains("Quality Unit", ignoreCase = true) -> Color(0xFF6366F1) // indigo-500
        domain.name.contains("Packaging", ignoreCase = true) -> Color(0xFFA855F7) // purple-500
        domain.name.contains("Production", ignoreCase = true) -> Color(0xFF10B981) // green-500
        domain.name.contains("Materials", ignoreCase = true) -> Color(0xFF06B6D4) // cyan-500
        domain.name.contains("Laboratory", ignoreCase = true) -> Color(0xFF14B8A6) // teal-500
        domain.name.contains("Facilities", ignoreCase = true) -> Color(0xFF6366F1) // indigo-500
        else -> Color(0xFF3B82F6) // default blue
    }
    
    // Gradient for icon background
    val (gradientStart, gradientEnd) = when {
        domain.name.contains("Quality Unit", ignoreCase = true) -> Pair(Color(0xFF6366F1), Color(0xFF4F46E5)) // indigo-500 to indigo-600
        domain.name.contains("Packaging", ignoreCase = true) -> Pair(Color(0xFFF43F5E), Color(0xFFE11D48)) // rose-500 to rose-600
        domain.name.contains("Production", ignoreCase = true) -> Pair(Color(0xFF10B981), Color(0xFF059669)) // green-500 to green-600
        domain.name.contains("Materials", ignoreCase = true) -> Pair(Color(0xFF06B6D4), Color(0xFF0891B2)) // cyan-500 to cyan-600
        domain.name.contains("Laboratory", ignoreCase = true) -> Pair(Color(0xFF14B8A6), Color(0xFF0D9488)) // teal-500 to teal-600
        domain.name.contains("Facilities", ignoreCase = true) -> Pair(Color(0xFF6366F1), Color(0xFF4F46E5)) // indigo-500 to indigo-600
        else -> Pair(Color(0xFF3B82F6), Color(0xFF2563EB)) // default blue gradient
    }
    
    Box(
        modifier = modifier.height(180.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Icon Container - 48x48 with neutral background (no color coding)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFFF1F5F9), // slate-100 - neutral gray
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = domain.icon,
                            fontSize = 24.sp,
                            color = Color(0xFF64748B) // slate-500 - neutral gray text
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Domain name only (no description)
                    Text(
                        text = domain.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                }
                
                Column {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0)) // slate-200
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Start assessment link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start assessment",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        
        // Multi-color gradient top border - 4px height, positioned at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        )
    }
}

}
