package com.pramod.validator.ui.components

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pramod.validator.data.models.DepartmentType
import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission

@Composable
fun PermissionManagementDialog(
    user: User?,
    currentPermission: UserPermission?,
    onDismiss: () -> Unit,
    onSave: (UserPermission) -> Unit
) {
    if (user == null) return
    
    var canViewDepartmentAssessments by remember { 
        mutableStateOf(currentPermission?.canViewDepartmentAssessments ?: false)
    }
    var canViewAllAssessments by remember { 
        mutableStateOf(currentPermission?.canViewAllAssessments ?: false)
    }
    var canAccessFda483Analysis by remember { 
        mutableStateOf(currentPermission?.canAccessFda483Analysis ?: false)
    }
    
    // Auto-select logic for hierarchical permissions
    LaunchedEffect(canViewAllAssessments) {
        if (canViewAllAssessments) {
            canViewDepartmentAssessments = true
        }
    }
    
    LaunchedEffect(canViewDepartmentAssessments) {
        if (!canViewDepartmentAssessments && canViewAllAssessments) {
            canViewAllAssessments = false
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage Permissions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // User Info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${user.department} • ${user.jobTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Simplified Permissions Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Core Permissions Section (Always Enabled)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Core Permissions (Always Enabled)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                PermissionToggle(
                                    title = "1. Create Assessments",
                                    description = "Main feature - user can create new assessments (always enabled)",
                                    checked = true,
                                    onCheckedChange = { },
                                    enabled = false
                                )
                                
                                PermissionToggle(
                                    title = "2. View Own Assessments",
                                    description = "User can view their own assessment history (always enabled)",
                                    checked = true,
                                    onCheckedChange = { },
                                    enabled = false
                                )
                            }
                        }
                    }
                    
                    // Optional Permissions Section
                    item {
                        Text(
                            "Optional Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    item {
                        PermissionToggle(
                            title = "3. View Department Assessments",
                            description = "View assessments from same department (includes own assessments)",
                            checked = canViewDepartmentAssessments,
                            onCheckedChange = { 
                                canViewDepartmentAssessments = it
                                if (!it) canViewAllAssessments = false
                            }
                        )
                    }
                    
                    item {
                        PermissionToggle(
                            title = "4. View All Assessments",
                            description = "View all enterprise assessments (auto-selects department + own)",
                            checked = canViewAllAssessments,
                            onCheckedChange = { 
                                canViewAllAssessments = it
                                if (it) canViewDepartmentAssessments = true
                            }
                        )
                    }
                    
                    item {
                        PermissionToggle(
                            title = "5. FDA 483 Analysis Access",
                            description = "Show FDA 483 Analysis tab in user's bottom navigation bar",
                            checked = canAccessFda483Analysis,
                            onCheckedChange = { canAccessFda483Analysis = it }
                        )
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { 
                            val updatedPermission = (currentPermission ?: UserPermission(
                                userId = user.uid,
                                enterpriseId = user.enterpriseId,
                                department = user.department
                            )).copy(
                                canCreateAssessments = true, // Always enabled
                                canViewOwnAssessments = true, // Always enabled
                                canViewDepartmentAssessments = canViewDepartmentAssessments,
                                canViewAllAssessments = canViewAllAssessments,
                                canAccessFda483Analysis = canAccessFda483Analysis,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updatedPermission)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Permissions")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && enabled) 
                MaterialTheme.colorScheme.primaryContainer 
            else if (!enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) Modifier.clickable { onCheckedChange(!checked) }
                    else Modifier
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
