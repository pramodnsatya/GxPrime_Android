package com.pramod.validator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pramod.validator.data.models.DepartmentType
import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.viewmodel.EnterpriseAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPermissionManagementScreen(
    userId: String,
    onBack: () -> Unit,
    enterpriseAdminViewModel: EnterpriseAdminViewModel = viewModel()
) {
    val users by enterpriseAdminViewModel.users.collectAsState()
    val isLoading by enterpriseAdminViewModel.isLoading.collectAsState()
    val errorMessage by enterpriseAdminViewModel.errorMessage.collectAsState()
    val successMessage by enterpriseAdminViewModel.successMessage.collectAsState()
    
    val selectedUser = users.find { it.uid == userId }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentPermission by remember { mutableStateOf<UserPermission?>(null) }
    
    // Lottie animation for loading - using a placeholder for now
    // val lottieComposition by rememberLottieComposition(
    //     LottieCompositionSpec.RawRes(android.R.raw.loading) // You'll need to add a loading.json file
    // )
    
    LaunchedEffect(userId) {
        if (selectedUser != null) {
            // Load user's current permissions
            enterpriseAdminViewModel.loadUserPermissions(userId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Manage Permissions",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Enhanced loading with Lottie animation
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp)
                        )
                        Text(
                            text = "Loading permissions...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // User Info Card
                    item {
                        UserInfoCard(
                            user = selectedUser,
                            onEditPermissions = {
                                currentPermission = selectedUser?.permissions
                                showPermissionDialog = true
                            }
                        )
                    }
                    
                    // Current Permissions Card
                    item {
                        CurrentPermissionsCard(
                            permission = selectedUser?.permissions,
                            onEdit = {
                                currentPermission = selectedUser?.permissions
                                showPermissionDialog = true
                            }
                        )
                    }
                    
                    // Permission Categories
                    item {
                        Text(
                            text = "Permission Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    item {
                        PermissionCategoriesCard(
                            permission = selectedUser?.permissions,
                            onEdit = {
                                currentPermission = selectedUser?.permissions
                                showPermissionDialog = true
                            }
                        )
                    }
                }
            }
            
            // Success/Error Messages
            AnimatedVisibility(
                visible = successMessage != null || errorMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = if (successMessage != null) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = successMessage ?: errorMessage ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = if (successMessage != null) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Permission Management Dialog
        if (showPermissionDialog) {
            PermissionManagementDialog(
                user = selectedUser,
                currentPermission = currentPermission,
                onDismiss = { showPermissionDialog = false },
                onSave = { permission ->
                    enterpriseAdminViewModel.updateUserPermissions(permission)
                    showPermissionDialog = false
                }
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    user: User?,
    onEditPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = onEditPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Permissions")
                }
            }
            
            if (user != null) {
                SimpleInfoRow("Name", user.displayName)
                SimpleInfoRow("Email", user.email)
                SimpleInfoRow("Department", user.department)
                SimpleInfoRow("Job Title", user.jobTitle)
                SimpleInfoRow("Role", user.role)
            }
        }
    }
}

@Composable
private fun CurrentPermissionsCard(
    permission: UserPermission?,
    @Suppress("UNUSED_PARAMETER") onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Current Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (permission != null) {
                PermissionItem(
                    title = "Create Assessments",
                    enabled = permission.canCreateAssessments,
                    description = "Can create new assessments"
                )
                
                PermissionItem(
                    title = "View Own Assessments",
                    enabled = permission.canViewOwnAssessments,
                    description = "Can view their own assessment history"
                )
                
                PermissionItem(
                    title = "View Department Assessments",
                    enabled = permission.canViewDepartmentAssessments,
                    description = "Can view department assessment history"
                )
                
                PermissionItem(
                    title = "View All Assessments",
                    enabled = permission.canViewAllAssessments,
                    description = "Can view all enterprise assessments"
                )
                
                PermissionItem(
                    title = "FDA 483 Analysis Access",
                    enabled = permission.canAccessFda483Analysis,
                    description = "Can access FDA 483 Analysis feature"
                )
            } else {
                Text(
                    text = "No permissions set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoriesCard(
    permission: UserPermission?,
    @Suppress("UNUSED_PARAMETER") onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Permission Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Assessment Permissions
            PermissionCategory(
                title = "Assessment Permissions",
                icon = Icons.Default.Assessment,
                permissions = listOf(
                    "Create Assessments" to (permission?.canCreateAssessments ?: false),
                    "View Own Assessments" to (permission?.canViewOwnAssessments ?: false),
                    "View Department Assessments" to (permission?.canViewDepartmentAssessments ?: false),
                    "View All Assessments" to (permission?.canViewAllAssessments ?: false)
                )
            )
            
            // FDA 483 Access
            PermissionCategory(
                title = "FDA 483 Access",
                icon = Icons.Default.Assignment,
                permissions = listOf(
                    "FDA 483 Analysis" to (permission?.canAccessFda483Analysis ?: false)
                )
            )
        }
    }
}

@Composable
private fun PermissionCategory(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    permissions: List<Pair<String, Boolean>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        permissions.forEach { (permissionName, isEnabled) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.Info else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isEnabled) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = permissionName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    enabled: Boolean,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.Info else Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionManagementDialog(
    @Suppress("UNUSED_PARAMETER") user: User?,
    @Suppress("UNUSED_PARAMETER") currentPermission: UserPermission?,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSave: (UserPermission) -> Unit
) {
    // This would be a comprehensive dialog for editing permissions
    // Implementation would include checkboxes for all permission types
    // and domain selection for specific permissions
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Permissions") },
        text = { Text("Permission editing dialog would go here") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SimpleInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
