package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.pramod.validator.viewmodel.SuperAdminViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminStatisticsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToResources: () -> Unit,
    superAdminViewModel: SuperAdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        superAdminViewModel.loadEnterprises()
        superAdminViewModel.loadAllUsers()
    }
    
    val enterprises by superAdminViewModel.enterprises.collectAsState()
    val statistics = superAdminViewModel.getStatistics()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "Statistics",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateToHome) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
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
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
                    }
                },
                user = currentUser,
                permissions = null
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)), // slate-50
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "System Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Enterprises",
                        value = statistics["totalEnterprises"]?.toString() ?: "0",
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF10B981) // emerald-500
                    )
                    StatCard(
                        title = "Active",
                        value = statistics["activeEnterprises"]?.toString() ?: "0",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF10B981) // emerald-500
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Users",
                        value = statistics["totalUsers"]?.toString() ?: "0",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF3B82F6) // blue-500
                    )
                    StatCard(
                        title = "Active",
                        value = statistics["activeUsers"]?.toString() ?: "0",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF3B82F6) // blue-500
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Ent. Admins",
                        value = statistics["enterpriseAdmins"]?.toString() ?: "0",
                        icon = Icons.Default.AccountCircle,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF8B5CF6) // violet-500
                    )
                    StatCard(
                        title = "Regular Users",
                        value = statistics["regularUsers"]?.toString() ?: "0",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF8B5CF6) // violet-500
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Enterprise User Limits",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Individual enterprise capacity cards
            enterprises.forEach { enterprise ->
                item {
                    EnterpriseCapacityCard(enterprise = enterprise)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFF1F5F9), // slate-100
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title, 
                fontSize = 14.sp,
                color = Color(0xFF64748B) // slate-500
            )
        }
    }
}

@Composable
private fun EnterpriseCapacityCard(
    enterprise: com.pramod.validator.data.models.Enterprise
) {
    val capacityPercentage = if (enterprise.userLimit > 0) {
        (enterprise.currentUserCount.toFloat() / enterprise.userLimit * 100).toInt()
    } else 0
    
    val progressColor = when {
        capacityPercentage >= 90 -> Color(0xFFDC2626) // red-600
        capacityPercentage >= 70 -> Color(0xFFF59E0B) // amber-500
        else -> Color(0xFF10B981) // emerald-500
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = enterprise.companyName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            if (enterprise.isActive) "Active" else "Inactive",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (enterprise.isActive) 
                            Color(0xFFD1FAE5) // green-100
                        else 
                            Color(0xFFFEE2E2), // red-100
                        labelColor = if (enterprise.isActive) 
                            Color(0xFF10B981) // green-600
                        else 
                            Color(0xFFDC2626) // red-600
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Users: ${enterprise.currentUserCount}/${enterprise.userLimit}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A) // slate-900
                )
                Text(
                    "$capacityPercentage% capacity",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B) // slate-500
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { if (enterprise.userLimit > 0) enterprise.currentUserCount.toFloat() / enterprise.userLimit else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = Color(0xFFE2E8F0) // slate-200
            )
            
            if (enterprise.adminName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Admin: ${enterprise.adminName}",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B) // slate-500
                )
            }
        }
    }
}
