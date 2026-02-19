package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.DepartmentType
import com.pramod.validator.viewmodel.EnterpriseAdminViewModel
import com.pramod.validator.viewmodel.DepartmentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvitationScreen(
    onBack: () -> Unit,
    onCreateInvitationSuccess: () -> Unit,
    viewModel: EnterpriseAdminViewModel = viewModel()
) {
    val departmentViewModel: DepartmentViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val departments by departmentViewModel.departments.collectAsState()
    val enterprise by viewModel.enterprise.collectAsState()
    
    // Load departments when enterprise is available
    LaunchedEffect(enterprise) {
        enterprise?.let {
            departmentViewModel.loadDepartments(it.id)
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
                            "Create Invitation",
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
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
                // Form Content
                CreateInvitationForm(
                    departments = departments,
                    onCreateInvitation = { invitationData ->
                        scope.launch {
                            viewModel.createInvitationWithPermissions(
                                email = invitationData.email,
                                displayName = invitationData.displayName,
                                department = invitationData.department,
                                jobTitle = invitationData.jobTitle,
                                canViewDepartmentAssessments = invitationData.canViewDepartmentAssessments,
                                canViewAllAssessments = invitationData.canViewAllAssessments,
                                canAccessFda483Analysis = invitationData.canAccessFda483Analysis,
                                createdBy = "admin"
                            )
                        }
                    },
                    onCancel = onBack,
                    isLoading = isLoading
                )
                
            // Error message
            LaunchedEffect(errorMessage, successMessage) {
                if (errorMessage != null || successMessage != null) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearMessages()
                }
            }
            
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2) // red-100
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFDC2626) // red-600
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626) // red-600
                        )
                    }
                }
            }
            
            // Success message
            LaunchedEffect(successMessage) {
                if (successMessage != null) {
                    kotlinx.coroutines.delay(2000) // Show success message for 2 seconds
                    onCreateInvitationSuccess()
                }
            }
            
            if (successMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1FAE5) // green-100
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981) // green-600
                        )
                        Text(
                            text = successMessage ?: "",
                            color = Color(0xFF10B981) // green-600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateInvitationForm(
    departments: List<com.pramod.validator.data.models.Department>,
    onCreateInvitation: (CreateInvitationData) -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    
    // Simplified Permission states
    // Core permissions - always true
    val canCreateAssessments = true
    val canViewOwnAssessments = true
    
    // Optional permissions
    var canViewDepartmentAssessments by remember { mutableStateOf(false) }
    var canViewAllAssessments by remember { mutableStateOf(false) }
    var canAccessFda483Analysis by remember { mutableStateOf(false) }
    
    // Auto-select logic for hierarchical permissions
    LaunchedEffect(canViewAllAssessments) {
        if (canViewAllAssessments) {
            // Selecting "View All" automatically selects "View Department"
            canViewDepartmentAssessments = true
        }
    }
    
    LaunchedEffect(canViewDepartmentAssessments) {
        // If user manually unchecks "View Department", also uncheck "View All"
        if (!canViewDepartmentAssessments && canViewAllAssessments) {
            canViewAllAssessments = false
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Basic Information Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionSection(
                    title = "Basic Information",
                    icon = Icons.Default.Person
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
                    
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
            
                    // Department Dropdown
                    var departmentExpanded by remember { mutableStateOf(false) }
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = departmentExpanded,
                        onExpandedChange = { departmentExpanded = !departmentExpanded }
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Department") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true,
                            placeholder = { Text("Select department") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                                unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = departmentExpanded,
                            onDismissRequest = { departmentExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            if (departments.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFF3F4F6) // slate-100
                                ) {
                                    Text(
                                        text = "No departments available",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF64748B) // slate-500
                                    )
                                }
                            } else {
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(
                                                    dept.name,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF0F172A) // slate-900
                                                )
                                                if (dept.description.isNotEmpty()) {
                                                    Text(
                                                        dept.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF64748B) // slate-500
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            department = dept.name
                                            departmentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
                }
            }
        }
        
        // User Permissions Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PermissionSection(
                    title = "User Permissions",
                    icon = Icons.Default.Security
                ) {
                    // Core permissions - Always enabled (non-interactive)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1F5F9) // slate-100
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Core Permissions (Always Enabled)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF1E3A8A), // blue-900
                                fontWeight = FontWeight.Bold
                            )
                            
                            PermissionCheckbox(
                                checked = canCreateAssessments,
                                onCheckedChange = { }, // Cannot be changed
                                text = "1. Create Assessments",
                                description = "Main feature - user can create new assessments (always enabled)",
                                enabled = false
                            )
                            
                            PermissionCheckbox(
                                checked = canViewOwnAssessments,
                                onCheckedChange = { }, // Cannot be changed
                                text = "2. View Own Assessments",
                                description = "User can view their own assessment history (always enabled)",
                                enabled = false
                            )
                        }
                    }
                    
                    // Optional permissions
                    Text(
                        "Optional Permissions",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1E3A8A), // blue-900
                        fontWeight = FontWeight.Bold
                    )
                    
                    PermissionCheckbox(
                        checked = canViewDepartmentAssessments,
                        onCheckedChange = { 
                            canViewDepartmentAssessments = it
                            // If unchecking, also uncheck "View All"
                            if (!it) {
                                canViewAllAssessments = false
                            }
                        },
                        text = "3. View Department Assessments",
                        description = "View assessments from same department (includes own assessments)"
                    )
                    
                    PermissionCheckbox(
                        checked = canViewAllAssessments,
                        onCheckedChange = { 
                            canViewAllAssessments = it
                            // Auto-select "View Department" when checking "View All"
                            if (it) {
                                canViewDepartmentAssessments = true
                            }
                        },
                        text = "4. View All Assessments",
                        description = "View all enterprise assessments (auto-selects department + own)"
                    )
                    
                    PermissionCheckbox(
                        checked = canAccessFda483Analysis,
                        onCheckedChange = { canAccessFda483Analysis = it },
                        text = "5. FDA 483 Analysis Access",
                        description = "Show FDA 483 Analysis tab in user's bottom navigation bar"
                    )
                }
            }
        }
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0F172A) // slate-900
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            Button(
                onClick = {
                    val invitationData = CreateInvitationData(
                        email = email,
                        displayName = displayName,
                        department = department,
                        jobTitle = jobTitle,
                        canViewDepartmentAssessments = canViewDepartmentAssessments,
                        canViewAllAssessments = canViewAllAssessments,
                        canAccessFda483Analysis = canAccessFda483Analysis
                    )
                    onCreateInvitation(invitationData)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = email.isNotBlank() && displayName.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A) // blue-900
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Send Invitation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1E3A8A), // blue-900
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A) // slate-900
            )
        }
        
        content()
    }
}

@Composable
private fun PermissionCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    description: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF1E3A8A), // blue-900
                disabledCheckedColor = Color(0xFF1E3A8A).copy(alpha = 0.6f)
            )
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Color(0xFF0F172A) else Color(0xFF64748B) // slate-900 or slate-500
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B) // slate-500
            )
        }
    }
}

data class CreateInvitationData(
    val email: String,
    val displayName: String,
    val department: String,
    val jobTitle: String,
    val canViewDepartmentAssessments: Boolean,
    val canViewAllAssessments: Boolean,
    val canAccessFda483Analysis: Boolean
)
