package com.pramod.validator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.viewmodel.EnterpriseAdminViewModel
import com.pramod.validator.viewmodel.DepartmentViewModel
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseAdminHomeScreen(
    enterpriseId: String,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToFacilities: () -> Unit,
    onNavigateToDepartments: () -> Unit,
    enterpriseAdminViewModel: EnterpriseAdminViewModel = viewModel(),
    departmentViewModel: DepartmentViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    // Initialize ViewModels
    LaunchedEffect(enterpriseId) {
        enterpriseAdminViewModel.initialize(enterpriseId)
        departmentViewModel.loadDepartments(enterpriseId)
    }
    
    val users by enterpriseAdminViewModel.users.collectAsState()
    val facilities by enterpriseAdminViewModel.facilities.collectAsState()
    val departments by departmentViewModel.departments.collectAsState()
    val isLoading by enterpriseAdminViewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
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
                permissions = null
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
                    contentPadding = PaddingValues(bottom = 0.dp) // Scaffold paddingValues already accounts for bottom bar
                ) {
                    // Welcome banner with gradient - overlapping with metrics card
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Gradient banner - extended to allow overlap
                            EnterpriseAdminWelcomeBanner(
                                modifier = Modifier.padding(bottom = 70.dp)
                            )
                            
                            // Enterprise Summary Card - positioned to overlap at bottom
                            EnterpriseSummaryCard(
                                totalUsers = users.size,
                                totalFacilities = facilities.size,
                                totalDepartments = departments.size,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                    
                    // Management Options Section Header
                    item {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp) // Reduced spacing to give more room for grid
                        ) {
                            Text(
                                text = "Management",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    
                    // Management option cards - 2 column grid
                    // Calculation: 2 rows of square cards
                    // Card width ≈ (screen width - 32dp padding - 12dp spacing) / 2 ≈ 158dp
                    // Card height = 158dp (square)
                    // 2 rows = 158dp * 2 = 316dp
                    // Plus 8dp spacing between rows + 8dp top padding = 332dp total
                    // Adding extra buffer for safety: 360dp
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp), // Increased further to ensure departments tile edge is fully visible
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false
                        ) {
                            // Users tile
                            item {
                                ManagementOptionCard(
                                    title = "Users",
                                    description = "Manage enterprise users",
                                    icon = Icons.Default.People,
                                    onClick = onNavigateToUsers
                                )
                            }
                            
                            // Facilities tile
                            item {
                                ManagementOptionCard(
                                    title = "Facilities",
                                    description = "Manage facilities",
                                    icon = Icons.Default.Home,
                                    onClick = onNavigateToFacilities
                                )
                            }
                            
                            // Departments tile
                            item {
                                ManagementOptionCard(
                                    title = "Departments",
                                    description = "Manage departments",
                                    icon = Icons.Default.Business,
                                    onClick = onNavigateToDepartments
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
private fun EnterpriseAdminWelcomeBanner(
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
            .padding(horizontal = 16.dp, vertical = 40.dp)
    ) {
        Column {
            // Date and icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
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
                text = "Welcome back, Admin!",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Subtitle
            Text(
                text = "Manage your enterprise resources",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EnterpriseSummaryCard(
    totalUsers: Int,
    totalFacilities: Int,
    totalDepartments: Int,
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
            // Total Users
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$totalUsers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6) // blue-500
                )
                Text(
                    text = "Users",
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
            
            // Total Facilities
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$totalFacilities",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981) // emerald-500
                )
                Text(
                    text = "Facilities",
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
            
            // Total Departments
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$totalDepartments",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6) // violet-500
                )
                Text(
                    text = "Departments",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ManagementOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square cards as per design
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Container - 48x48 with neutral background
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF64748B), // slate-500 - neutral gray
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A) // slate-900
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF64748B) // slate-500
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE2E8F0)) // slate-200
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Start link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage",
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

