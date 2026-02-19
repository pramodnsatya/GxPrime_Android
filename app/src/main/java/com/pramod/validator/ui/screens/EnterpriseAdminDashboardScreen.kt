package com.pramod.validator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Facility
import com.pramod.validator.data.models.Department
import com.pramod.validator.data.models.User
import com.pramod.validator.viewmodel.EnterpriseAdminViewModel
import com.pramod.validator.viewmodel.DepartmentViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.ui.components.DepartmentsManagementTab
import com.pramod.validator.ui.components.CreateDepartmentDialog
import com.pramod.validator.ui.components.EditDepartmentDialog
import com.pramod.validator.ui.components.DeleteDepartmentDialog
import com.pramod.validator.ui.components.DeleteFacilityDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseAdminDashboardScreen(
    enterpriseId: String,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToCreateInvitation: () -> Unit,
    onSignOut: () -> Unit
) {
    val enterpriseAdminViewModel: EnterpriseAdminViewModel = viewModel()
    val departmentViewModel: DepartmentViewModel = viewModel()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateFacilityDialog by remember { mutableStateOf(false) }
    var showCreateDepartmentDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var showEditFacilityDialog by remember { mutableStateOf(false) }
    var showEditDepartmentDialog by remember { mutableStateOf(false) }
    var showDeleteUserDialog by remember { mutableStateOf(false) }
    var showDeleteFacilityDialog by remember { mutableStateOf(false) }
    var showDeleteDepartmentDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    var selectedDepartment by remember { mutableStateOf<Department?>(null) }
    val scope = rememberCoroutineScope()
    
    // Initialize ViewModels
    LaunchedEffect(enterpriseId) {
        enterpriseAdminViewModel.initialize(enterpriseId)
        departmentViewModel.loadDepartments(enterpriseId)
    }
    
    val users by enterpriseAdminViewModel.users.collectAsState()
    val facilities by enterpriseAdminViewModel.facilities.collectAsState()
    val departments by departmentViewModel.departments.collectAsState()
    val isLoading by enterpriseAdminViewModel.isLoading.collectAsState()
    val errorMessage by enterpriseAdminViewModel.errorMessage.collectAsState()
    val successMessage by enterpriseAdminViewModel.successMessage.collectAsState()
    val deptErrorMessage by departmentViewModel.errorMessage.collectAsState()
    val deptSuccessMessage by departmentViewModel.successMessage.collectAsState()
    
    // Local search query states
    var userSearchQuery by remember { mutableStateOf("") }
    var facilitySearchQuery by remember { mutableStateOf("") }
    var departmentSearchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Enterprise Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "enterprise_admin",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> onNavigateToHistory()
                        "fda483" -> onNavigateToFda483()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
                        "sign_out" -> onSignOut()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Auto-dismissing notifications
            LaunchedEffect(errorMessage, successMessage) {
                if (errorMessage != null || successMessage != null) {
                    kotlinx.coroutines.delay(3000) // Auto-dismiss after 3 seconds
                    enterpriseAdminViewModel.clearMessages()
                }
            }
            
            // Error message
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
            
            // Success message
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
            
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Users") },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Facilities") },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Departments") },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Users Tab
                    UsersManagementTab(
                        users = users,
                        isLoading = isLoading,
                        searchQuery = userSearchQuery,
                        onSearchQueryChange = { userSearchQuery = it },
                        onAddUserClick = { onNavigateToCreateInvitation() },
                        onEditUserClick = { user ->
                            selectedUser = user
                            showEditUserDialog = true
                        },
                        onDeleteUserClick = { user ->
                            selectedUser = user
                            showDeleteUserDialog = true
                        }
                    )
                }
                1 -> {
                    // Facilities Tab
                    FacilitiesManagementTab(
                        facilities = facilities,
                        isLoading = isLoading,
                        searchQuery = facilitySearchQuery,
                        onSearchQueryChange = { facilitySearchQuery = it },
                        onAddFacilityClick = { showCreateFacilityDialog = true },
                        onEditFacilityClick = { facility ->
                            selectedFacility = facility
                            showEditFacilityDialog = true
                        },
                        onDeleteFacilityClick = { facility ->
                            selectedFacility = facility
                            showDeleteFacilityDialog = true
                        }
                    )
                }
                2 -> {
                    // Departments Tab
                    DepartmentsManagementTab(
                        departments = departments,
                        isLoading = isLoading,
                        searchQuery = departmentSearchQuery,
                        onSearchQueryChange = { departmentSearchQuery = it },
                        onAddDepartmentClick = { showCreateDepartmentDialog = true },
                        onEditDepartmentClick = { department ->
                            selectedDepartment = department
                            showEditDepartmentDialog = true
                        },
                        onDeleteDepartmentClick = { department ->
                            selectedDepartment = department
                            showDeleteDepartmentDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Create Facility Dialog
    if (showCreateFacilityDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showCreateFacilityDialog = false },
            modifier = Modifier.background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
            title = { 
                Text(
                    "Create Facility",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Facility Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val result = enterpriseAdminViewModel.createFacility(
                                    name = name,
                                    description = description,
                                    createdBy = "admin"
                                )
                                result.fold(
                                    onSuccess = {
                                        showCreateFacilityDialog = false
                                    },
                                    onFailure = { error ->
                                        android.util.Log.e("EnterpriseAdminScreen", "Failed to create facility: ${error.message}", error)
                                        // Keep dialog open on error
                                    }
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("EnterpriseAdminScreen", "Error creating facility: ${e.message}", e)
                                // Keep dialog open on error
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateFacilityDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    // Edit User Dialog
    if (showEditUserDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            onDismiss = { 
                showEditUserDialog = false
                selectedUser = null
            },
            onSave = { name, email, department, jobTitle ->
                scope.launch {
                    try {
                        // Keep all existing fields and only update the ones that changed
                        val updatedUser = selectedUser!!.copy(
                            displayName = name,
                            department = department,
                            jobTitle = jobTitle,
                            // Preserve all other fields
                            role = selectedUser!!.role,
                            enterpriseId = selectedUser!!.enterpriseId,
                            companyName = selectedUser!!.companyName,
                            permissions = selectedUser!!.permissions,
                            createdAt = selectedUser!!.createdAt,
                            createdBy = selectedUser!!.createdBy,
                            isActive = selectedUser!!.isActive,
                            expiresAt = selectedUser!!.expiresAt
                        )
                        
                        val result = enterpriseAdminViewModel.updateUser(updatedUser)
                        result.fold(
                            onSuccess = {
                                showEditUserDialog = false
                                selectedUser = null
                            },
                            onFailure = { error ->
                                android.util.Log.e("EnterpriseAdminScreen", "Failed to update user: ${error.message}", error)
                                // Keep dialog open on error
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("EnterpriseAdminScreen", "Error updating user: ${e.message}", e)
                        // Keep dialog open on error
                    }
                }
            }
        )
    }

    // Delete User Confirmation Dialog
    if (showDeleteUserDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteUserDialog = false
                selectedUser = null
            },
            title = { Text("Delete User") },
            text = { 
                Text("Are you sure you want to delete ${selectedUser?.displayName}? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                selectedUser?.let { user ->
                                    enterpriseAdminViewModel.deleteUser(user.uid)
                                }
                                showDeleteUserDialog = false
                                selectedUser = null
                            } catch (e: Exception) {
                                android.util.Log.e("EnterpriseAdminScreen", "Error deleting user: ${e.message}", e)
                                // Keep dialog open on error
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteUserDialog = false
                        selectedUser = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Facility Confirmation Dialog
    if (showDeleteFacilityDialog && selectedFacility != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteFacilityDialog = false
                selectedFacility = null
            },
            title = { Text("Delete Facility") },
            text = { 
                Text("Are you sure you want to delete ${selectedFacility?.name}? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                selectedFacility?.let { facility ->
                                    enterpriseAdminViewModel.deleteFacility(facility.id, "admin")
                                }
                                showDeleteFacilityDialog = false
                                selectedFacility = null
                            } catch (e: Exception) {
                                android.util.Log.e("EnterpriseAdminScreen", "Error deleting facility: ${e.message}", e)
                                // Keep dialog open on error
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteFacilityDialog = false
                        selectedFacility = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Facility Dialog
    if (showEditFacilityDialog && selectedFacility != null) {
        EditFacilityDialog(
            facility = selectedFacility!!,
            onDismiss = { 
                showEditFacilityDialog = false
                selectedFacility = null
            },
            onSave = { name, description ->
                scope.launch {
                    try {
                        val result = enterpriseAdminViewModel.updateFacility(
                            selectedFacility!!.id,
                            name,
                            description,
                            "admin"
                        )
                        result.fold(
                            onSuccess = {
                                showEditFacilityDialog = false
                                selectedFacility = null
                            },
                            onFailure = { error ->
                                android.util.Log.e("EnterpriseAdminScreen", "Failed to update facility: ${error.message}", error)
                                // Keep dialog open on error
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("EnterpriseAdminScreen", "Error updating facility: ${e.message}", e)
                        // Keep dialog open on error
                    }
                }
            }
        )
    }
    
    // Department Dialogs
    CreateDepartmentDialog(
        showDialog = showCreateDepartmentDialog,
        onDismiss = { 
            showCreateDepartmentDialog = false
        },
        onCreate = { name, description ->
            departmentViewModel.createDepartment(name, description, enterpriseId)
        },
        isLoading = departmentViewModel.isLoading.collectAsState().value,
        errorMessage = departmentViewModel.errorMessage.collectAsState().value,
        successMessage = departmentViewModel.successMessage.collectAsState().value
    )
    
    // Track success to close create dialog
    LaunchedEffect(departmentViewModel.successMessage.collectAsState().value) {
        val successMsg = departmentViewModel.successMessage.value
        if (successMsg?.isNotEmpty() == true && showCreateDepartmentDialog) {
            kotlinx.coroutines.delay(1000)
            showCreateDepartmentDialog = false
        }
    }
    
    EditDepartmentDialog(
        showDialog = showEditDepartmentDialog,
        onDismiss = { 
            showEditDepartmentDialog = false
        },
        department = selectedDepartment,
        onSave = { name, description ->
            selectedDepartment?.let {
                val updatedDepartment = it.copy(name = name, description = description)
                departmentViewModel.updateDepartment(updatedDepartment)
            }
        },
        isLoading = departmentViewModel.isLoading.collectAsState().value,
        errorMessage = departmentViewModel.errorMessage.collectAsState().value,
        successMessage = departmentViewModel.successMessage.collectAsState().value
    )
    
    // Track success to close edit dialog
    LaunchedEffect(departmentViewModel.successMessage.collectAsState().value) {
        val successMsg = departmentViewModel.successMessage.value
        if (successMsg?.isNotEmpty() == true && showEditDepartmentDialog) {
            kotlinx.coroutines.delay(1000)
            showEditDepartmentDialog = false
        }
    }
    
    DeleteDepartmentDialog(
        showDialog = showDeleteDepartmentDialog,
        onDismiss = { 
            showDeleteDepartmentDialog = false
        },
        department = selectedDepartment,
        onConfirm = {
            selectedDepartment?.let {
                departmentViewModel.deleteDepartment(it.id)
            }
        },
        affectedAssessmentsCount = null,
        isLoading = departmentViewModel.isLoading.collectAsState().value,
        errorMessage = departmentViewModel.errorMessage.collectAsState().value,
        successMessage = departmentViewModel.successMessage.collectAsState().value
    )
    
    // Track success/error to close delete dialog
    LaunchedEffect(departmentViewModel.successMessage.collectAsState().value, departmentViewModel.errorMessage.collectAsState().value) {
        val successMsg = departmentViewModel.successMessage.value
        val errorMsg = departmentViewModel.errorMessage.value
        if (showDeleteDepartmentDialog) {
            if (successMsg?.isNotEmpty() == true) {
                kotlinx.coroutines.delay(1000)
                showDeleteDepartmentDialog = false
            } else if (errorMsg?.contains("Cannot delete") == true) {
                // Keep dialog open for "Cannot delete" errors (shows warning)
            }
        }
    }
}

@Composable
private fun UsersManagementTab(
    users: List<User>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddUserClick: () -> Unit,
    onEditUserClick: (User) -> Unit,
    onDeleteUserClick: (User) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "User Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = onAddUserClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search users...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Users List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val filteredUsers = users.filter { user ->
                    searchQuery.isEmpty() || 
                    user.displayName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.department.contains(searchQuery, ignoreCase = true)
                }
                
                items(filteredUsers) { user ->
                    UserCard(
                        user = user,
                        onEditClick = { onEditUserClick(user) },
                        onDeleteClick = { onDeleteUserClick(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FacilitiesManagementTab(
    facilities: List<Facility>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddFacilityClick: () -> Unit,
    onEditFacilityClick: (Facility) -> Unit,
    onDeleteFacilityClick: (Facility) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Facility Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
                    FloatingActionButton(
                        onClick = onAddFacilityClick,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Facility")
                    }
        }
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search facilities...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Facilities List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val filteredFacilities = facilities.filter { facility ->
                    searchQuery.isEmpty() || 
                    facility.name.contains(searchQuery, ignoreCase = true) ||
                    facility.description.contains(searchQuery, ignoreCase = true)
                }
                
                items(filteredFacilities) { facility ->
                    FacilityCard(
                        facility = facility,
                        onEditClick = { onEditFacilityClick(facility) },
                        onDeleteClick = { onDeleteFacilityClick(facility) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${user.department} • ${user.jobTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit User",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete User",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FacilityCard(
    facility: Facility,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = facility.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = facility.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Facility",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Facility",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreateUser: (String, String, String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            ),
            shape = RoundedCornerShape(28.dp)
        ),
        title = { 
            Text(
                "Create User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )
                
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                    onClick = { 
                        onCreateUser(email, password, displayName, department, jobTitle)
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && displayName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var email by remember { mutableStateOf(user.email) }
    var department by remember { mutableStateOf(user.department) }
    var jobTitle by remember { mutableStateOf(user.jobTitle) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false // Email cannot be changed
                )
                
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, email, department, jobTitle) },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Save")
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
private fun EditFacilityDialog(
    facility: Facility,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(facility.name) }
    var description by remember { mutableStateOf(facility.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            ),
            shape = RoundedCornerShape(28.dp)
        ),
        title = { 
            Text(
                "Edit Facility",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Facility Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text("Cancel")
            }
        }
    )
}