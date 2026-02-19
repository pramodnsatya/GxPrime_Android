package com.pramod.validator.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Enterprise
import com.pramod.validator.viewmodel.SuperAdminViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    onSignOut: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToCreateEnterprise: () -> Unit,
    onNavigateToCreateUser: () -> Unit,
    viewModel: SuperAdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showEditEnterpriseDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var showDeleteEnterpriseDialog by remember { mutableStateOf(false) }
    var showDeleteUserDialog by remember { mutableStateOf(false) }
    var selectedEnterprise by remember { mutableStateOf<Enterprise?>(null) }
    var selectedUser by remember { mutableStateOf<com.pramod.validator.data.models.User?>(null) }
    val scope = rememberCoroutineScope()
    
    val enterprises by viewModel.filteredEnterprises.collectAsState()
    val allUsers by viewModel.filteredUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Local search query states
    var enterpriseSearchQuery by remember { mutableStateOf("") }
    var userSearchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Super Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
                currentRoute = "super_admin",
                onNavigate = { route ->
                    when (route) {
                        "home" -> { /* Stay on Super Admin Dashboard - do nothing */ }
                        "history" -> onNavigateToHistory()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> onNavigateToResources()
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
                    viewModel.clearMessages()
                }
            }
            
            // Show compact notification messages
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = message, 
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = message, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Modern Tab Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { 
                            Text(
                                "Enterprises",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        icon = { 
                            Icon(
                                Icons.Default.Settings, 
                                contentDescription = null,
                                tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { 
                            Text(
                                "Users",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        icon = { 
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null,
                                tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { 
                            Text(
                                "Statistics",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        icon = { 
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = null,
                                tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }
            
            // Tab Content
            when (selectedTab) {
                0 ->                 EnterprisesTab(
                    enterprises = enterprises,
                    isLoading = isLoading,
                    onCreateEnterprise = { onNavigateToCreateEnterprise() },
                    onEditEnterprise = { enterprise ->
                        selectedEnterprise = enterprise
                        showEditEnterpriseDialog = true
                    },
                    onDeleteEnterprise = { enterprise ->
                        selectedEnterprise = enterprise
                        showDeleteEnterpriseDialog = true
                    },
                    onRefresh = { viewModel.loadEnterprises() },
                    onSearchQueryChange = { viewModel.updateEnterpriseSearchQuery(it) },
                    searchQuery = enterpriseSearchQuery,
                    onSearchQueryUpdate = { enterpriseSearchQuery = it }
                )
                1 -> UsersTab(
                    users = allUsers,
                    isLoading = isLoading,
                    onCreateUser = { onNavigateToCreateUser() },
                    onEditUser = { user ->
                        selectedUser = user
                        showEditUserDialog = true
                    },
                    onDeleteUser = { user ->
                        selectedUser = user
                        showDeleteUserDialog = true
                    },
                    onRefresh = { viewModel.loadAllUsers() },
                    onSearchQueryChange = { viewModel.updateUserSearchQuery(it) },
                    searchQuery = userSearchQuery,
                    onSearchQueryUpdate = { userSearchQuery = it }
                )
                2 -> StatisticsTab(
                    statistics = viewModel.getStatistics(),
                    enterprises = enterprises,
                    viewModel = viewModel
                )
            }
        }
    }
    
    
    if (showEditEnterpriseDialog && selectedEnterprise != null) {
        EditEnterpriseDialog(
            enterprise = selectedEnterprise!!,
            onDismiss = {
                showEditEnterpriseDialog = false
                selectedEnterprise = null
            },
            onUpdate = { enterprise, expiresAt ->
                scope.launch {
                    viewModel.updateEnterprise(enterprise.copy(expiresAt = expiresAt))
                    showEditEnterpriseDialog = false
                    selectedEnterprise = null
                }
            }
        )
    }
    
    if (showEditUserDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showEditUserDialog = false
                selectedUser = null
            },
            onUpdate = { user ->
                scope.launch {
                    viewModel.updateUser(user)
                    showEditUserDialog = false
                    selectedUser = null
                }
            }
        )
    }
    
    if (showDeleteEnterpriseDialog && selectedEnterprise != null) {
        DeleteEnterpriseDialog(
            enterprise = selectedEnterprise!!,
            onDismiss = {
                showDeleteEnterpriseDialog = false
                selectedEnterprise = null
            },
            onDelete = { enterpriseId ->
                scope.launch {
                    viewModel.deleteEnterprise(enterpriseId)
                    showDeleteEnterpriseDialog = false
                    selectedEnterprise = null
                }
            }
        )
    }
    
    if (showDeleteUserDialog && selectedUser != null) {
        DeleteUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showDeleteUserDialog = false
                selectedUser = null
            },
            onDelete = { userId ->
                scope.launch {
                    viewModel.deleteUser(userId)
                    showDeleteUserDialog = false
                    selectedUser = null
                }
            }
        )
    }
}

@Composable
private fun EnterprisesTab(
    enterprises: List<Enterprise>,
    isLoading: Boolean,
    onCreateEnterprise: () -> Unit,
    onEditEnterprise: (Enterprise) -> Unit,
    onDeleteEnterprise: (Enterprise) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryUpdate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Total Enterprises",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${enterprises.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh enterprises",
                            tint = MaterialTheme.colorScheme.primary
                        )
                }
                OutlinedButton(
                    onClick = onCreateEnterprise,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                        border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Create Enterprise",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    }
                }
            }
        }
        
        // Modern Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                onSearchQueryUpdate(it)
                onSearchQueryChange(it)
            },
            label = { Text("Search enterprises...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        onSearchQueryUpdate("")
                        onSearchQueryChange("") 
                    }) {
                        Icon(
                            Icons.Default.Clear, 
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading enterprises...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else if (enterprises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No enterprises found for \"$searchQuery\""
                            } else {
                                "No enterprises yet"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try a different search term"
                            } else {
                                "Create your first enterprise to get started"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(enterprises) { enterprise ->
                    EnterpriseCard(
                        enterprise = enterprise,
                        onEdit = { onEditEnterprise(enterprise) },
                        onDelete = { onDeleteEnterprise(enterprise) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterpriseCard(
    enterprise: Enterprise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = enterprise.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            if (enterprise.isActive) "Active" else "Inactive",
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            if (enterprise.isActive) Icons.Default.Info else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        ) 
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (enterprise.isActive) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer,
                        labelColor = if (enterprise.isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer,
                        leadingIconContentColor = if (enterprise.isActive) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Contact Information
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        enterprise.adminEmail, 
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        enterprise.contactPhone, 
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Users", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${enterprise.currentUserCount}/${enterprise.userLimit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Admin", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        enterprise.adminName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Created", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(enterprise.createdAt)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (enterprise.industry.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Industry: ${enterprise.industry}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun UsersTab(
    users: List<com.pramod.validator.data.models.User>,
    isLoading: Boolean,
    onCreateUser: () -> Unit,
    onEditUser: (com.pramod.validator.data.models.User) -> Unit,
    onDeleteUser: (com.pramod.validator.data.models.User) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryUpdate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Modern Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Total Users",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${users.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh users",
                            tint = MaterialTheme.colorScheme.primary
                        )
                }
                FilledTonalButton(
                    onClick = onCreateUser,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create User")
                    }
                }
            }
        }
        
        // Modern Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                onSearchQueryUpdate(it)
                onSearchQueryChange(it)
            },
            label = { Text("Search users...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        onSearchQueryUpdate("")
                        onSearchQueryChange("") 
                    }) {
                        Icon(
                            Icons.Default.Clear, 
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading users...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No users found for \"$searchQuery\""
                            } else {
                                "No users yet"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try a different search term"
                            } else {
                                "Create your first user to get started"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    UserCard(
                        user = user,
                        onEdit = { onEditUser(user) },
                        onDelete = { onDeleteUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: com.pramod.validator.data.models.User,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
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
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = when (user.role) {
                    "SUPER_ADMIN" -> MaterialTheme.colorScheme.primaryContainer
                    "ENTERPRISE_ADMIN" -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (user.role) {
                            "SUPER_ADMIN" -> Icons.Default.Star
                            "ENTERPRISE_ADMIN" -> Icons.Default.AccountCircle
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = when (user.role) {
                            "SUPER_ADMIN" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "ENTERPRISE_ADMIN" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.displayName, 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    user.email, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (user.companyName.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        user.companyName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (user.department.isNotEmpty() || user.jobTitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${user.jobTitle}${if (user.department.isNotEmpty() && user.jobTitle.isNotEmpty()) " • " else ""}${user.department}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit, 
                        "Edit User", 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, 
                        "Delete User", 
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        when (user.role) {
                            "SUPER_ADMIN" -> "Super Admin"
                            "ENTERPRISE_ADMIN" -> "Ent. Admin"
                            else -> if (user.isActive) "Active" else "Inactive"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = when (user.role) {
                    "SUPER_ADMIN" -> AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    "ENTERPRISE_ADMIN" -> AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    else -> AssistChipDefaults.assistChipColors(
                        containerColor = if (user.isActive) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer,
                        labelColor = if (user.isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            )
        }
    }
}

@Composable
private fun StatisticsTab(
    statistics: Map<String, Int>,
    enterprises: List<Enterprise>,
    viewModel: SuperAdminViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("System Overview", style = MaterialTheme.typography.titleLarge)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Enterprises",
                    value = statistics["totalEnterprises"]?.toString() ?: "0",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active",
                    value = statistics["activeEnterprises"]?.toString() ?: "0",
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active",
                    value = statistics["activeUsers"]?.toString() ?: "0",
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Regular Users",
                    value = statistics["regularUsers"]?.toString() ?: "0",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Spacer(Modifier.height(16.dp))
            Text("Enterprise User Limits", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        
        // Individual enterprise capacity cards
        enterprises.forEach { enterprise ->
            item {
                EnterpriseCapacityCard(enterprise = enterprise)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, 
                        contentDescription = null, 
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                value, 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                title, 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnterpriseCapacityCard(
    enterprise: Enterprise
) {
    val capacityPercentage = if (enterprise.userLimit > 0) {
        (enterprise.currentUserCount.toFloat() / enterprise.userLimit * 100).toInt()
    } else 0
    
    val progressColor = when {
        capacityPercentage >= 90 -> MaterialTheme.colorScheme.error
        capacityPercentage >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = enterprise.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer,
                        labelColor = if (enterprise.isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Users: ${enterprise.currentUserCount}/${enterprise.userLimit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$capacityPercentage% capacity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { if (enterprise.userLimit > 0) enterprise.currentUserCount.toFloat() / enterprise.userLimit else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            if (enterprise.adminName.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Admin: ${enterprise.adminName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun EditEnterpriseDialog(
    enterprise: Enterprise,
    onDismiss: () -> Unit,
    onUpdate: (Enterprise, Long) -> Unit
) {
    var companyName by remember(enterprise.id) { mutableStateOf(enterprise.companyName) }
    var contactEmail by remember(enterprise.id) { mutableStateOf(enterprise.contactEmail) }
    var contactPhone by remember(enterprise.id) { mutableStateOf(enterprise.contactPhone) }
    var address by remember(enterprise.id) { mutableStateOf(enterprise.address) }
    var industry by remember(enterprise.id) { mutableStateOf(enterprise.industry) }
    var isActive by remember(enterprise.id) { mutableStateOf(enterprise.isActive) }
    var subscriptionMonths by remember { mutableStateOf("12") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Enterprise") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Contact Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = industry,
                        onValueChange = { industry = it },
                        label = { Text("Industry") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = subscriptionMonths,
                        onValueChange = { subscriptionMonths = it },
                        label = { Text("Extend Subscription (months) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Add months to current expiration (e.g., 12 to extend by 1 year)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Enterprise")
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (companyName.isNotBlank() && subscriptionMonths.isNotBlank()) {
                        val months = subscriptionMonths.toIntOrNull() ?: 12
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = if (enterprise.expiresAt > 0) enterprise.expiresAt else System.currentTimeMillis()
                        calendar.add(Calendar.MONTH, months)
                        val newExpiresAt = calendar.timeInMillis
                        
                        val updatedEnterprise = enterprise.copy(
                            companyName = companyName,
                            contactEmail = contactEmail,
                            contactPhone = contactPhone,
                            address = address,
                            industry = industry,
                            isActive = isActive
                        )
                        onUpdate(updatedEnterprise, newExpiresAt)
                    }
                },
                enabled = companyName.isNotBlank() && subscriptionMonths.isNotBlank()
            ) {
                Text("Update")
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
private fun EditUserDialog(
    user: com.pramod.validator.data.models.User,
    onDismiss: () -> Unit,
    onUpdate: (com.pramod.validator.data.models.User) -> Unit
) {
    var displayName by remember(user.uid) { mutableStateOf(user.displayName) }
    var department by remember(user.uid) { mutableStateOf(user.department) }
    var jobTitle by remember(user.uid) { mutableStateOf(user.jobTitle) }
    var isActive by remember(user.uid) { mutableStateOf(user.isActive) }
    var subscriptionMonths by remember { mutableStateOf("12") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = { },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }
                item {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = subscriptionMonths,
                        onValueChange = { subscriptionMonths = it },
                        label = { Text("Extend Subscription (months) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Add months to current expiration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active User")
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.isNotBlank() && subscriptionMonths.isNotBlank()) {
                        val months = subscriptionMonths.toIntOrNull() ?: 12
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = if (user.expiresAt > 0) user.expiresAt else System.currentTimeMillis()
                        calendar.add(Calendar.MONTH, months)
                        val newExpiresAt = calendar.timeInMillis
                        
                        val updatedUser = user.copy(
                            displayName = displayName,
                            department = department,
                            jobTitle = jobTitle,
                            isActive = isActive,
                            expiresAt = newExpiresAt
                        )
                        onUpdate(updatedUser)
                    }
                },
                enabled = displayName.isNotBlank() && subscriptionMonths.isNotBlank()
            ) {
                Text("Update")
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
private fun DeleteEnterpriseDialog(
    enterprise: Enterprise,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Enterprise") },
        text = {
            Column {
                Text("Are you sure you want to delete this enterprise?")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enterprise: ${enterprise.companyName}",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ This will delete ALL ${enterprise.currentUserCount} users in this enterprise!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDelete(enterprise.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Enterprise")
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
private fun DeleteUserDialog(
    user: com.pramod.validator.data.models.User,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete User") },
        text = {
            Column {
                Text("Are you sure you want to delete this user?")
                Spacer(Modifier.height(8.dp))
                Text(
                    "User: ${user.displayName}",
                    fontWeight = FontWeight.Bold
                )
                Text("Email: ${user.email}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDelete(user.uid) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

