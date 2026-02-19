package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.User
import com.pramod.validator.viewmodel.SuperAdminViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.ui.components.DatePickerField
import com.pramod.validator.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminUsersScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToCreateUser: () -> Unit,
    superAdminViewModel: SuperAdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    // Initialize ViewModel
    LaunchedEffect(Unit) {
        superAdminViewModel.loadAllUsers()
    }
    
    val allUsers by superAdminViewModel.filteredUsers.collectAsState()
    val isLoading by superAdminViewModel.isLoading.collectAsState()
    val errorMessage by superAdminViewModel.errorMessage.collectAsState()
    val successMessage by superAdminViewModel.successMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var showDeleteUserDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()
    
    // Update search query in ViewModel
    LaunchedEffect(searchQuery) {
        superAdminViewModel.updateUserSearchQuery(searchQuery)
    }
    
    // Auto-dismiss messages
    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null || successMessage != null) {
            kotlinx.coroutines.delay(3000)
            superAdminViewModel.clearMessages()
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
                            "Users",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateToHome) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A) // slate-900
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToCreateUser) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add User",
                                tint = Color(0xFF1E3A8A) // blue-900
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A), // slate-900
                        navigationIconContentColor = Color(0xFF0F172A), // slate-900
                        actionIconContentColor = Color(0xFF1E3A8A) // blue-900
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
        ) {
            // Error/Success messages
            errorMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2) // red-100
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626), // red-600
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = Color(0xFFDC2626), // red-600
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            successMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1FAE5) // green-100
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981), // green-600
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = Color(0xFF10B981), // green-600
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { 
                    Text(
                        "Search users...",
                        color = Color(0xFF94A3B8) // slate-400
                    ) 
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8) // slate-400
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8) // slate-400
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                    unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                )
            )
            
            // Users list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E3A8A) // blue-900
                    )
                }
            } else if (allUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF94A3B8) // slate-400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No users found"
                            } else {
                                "No users yet"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try a different search term"
                            } else {
                                "Create your first user to get started"
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allUsers) { user ->
                        ModernUserCard(
                            user = user,
                            onEditClick = {
                                selectedUser = user
                                showEditUserDialog = true
                            },
                            onDeleteClick = {
                                selectedUser = user
                                showDeleteUserDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Edit User Dialog
    if (showEditUserDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showEditUserDialog = false
                selectedUser = null
            },
            onUpdate = { user ->
                scope.launch {
                    superAdminViewModel.updateUser(user)
                    // Reload users to reflect the updated status immediately
                    superAdminViewModel.loadAllUsers()
                    showEditUserDialog = false
                    selectedUser = null
                }
            }
        )
    }
    
    // Delete User Dialog
    if (showDeleteUserDialog && selectedUser != null) {
        DeleteUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showDeleteUserDialog = false
                selectedUser = null
            },
            onDelete = { userId ->
                scope.launch {
                    superAdminViewModel.deleteUser(userId)
                    showDeleteUserDialog = false
                    selectedUser = null
                }
            }
        )
    }
}

@Composable
private fun ModernUserCard(
    user: User,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            // Color bar on top - blue-500 matching "Users" metric
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color(0xFF3B82F6), // blue-500
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.email,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                    if (user.companyName.isNotEmpty() || user.department.isNotEmpty() || user.jobTitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                if (user.companyName.isNotEmpty()) append(user.companyName)
                                if (user.department.isNotEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(user.department)
                                }
                                if (user.jobTitle.isNotEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(user.jobTitle)
                                }
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8) // slate-400
                        )
                    }
                    
                    // Show subscription end date
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                    val expirationText = if (user.expiresAt > 0) {
                        dateFormat.format(Date(user.expiresAt))
                    } else {
                        "No expiration"
                    }
                    Text(
                        text = "Subscription: $expirationText",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8) // slate-400
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color(0xFF64748B) // slate-500
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
                            onDismissRequest = { showMenu = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .wrapContentHeight(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                showMenu = false
                                                onEditClick()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color(0xFF2563EB), // blue-600
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "Edit",
                                            color = Color(0xFF2563EB), // blue-600
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                showMenu = false
                                                onDeleteClick()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFDC2626), // red-600
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "Delete",
                                            color = Color(0xFFDC2626), // red-600
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onUpdate: (User) -> Unit
) {
    var displayName by remember(user.uid) { mutableStateOf(user.displayName) }
    var department by remember(user.uid) { mutableStateOf(user.department) }
    var jobTitle by remember(user.uid) { mutableStateOf(user.jobTitle) }
    var isActive by remember(user.uid) { mutableStateOf(user.isActive) }
    var expirationDate by remember(user.uid) { 
        mutableStateOf<Long?>(if (user.expiresAt > 0) user.expiresAt else null)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { 
            Text(
                "Edit User",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A) // slate-900
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
                }
                item {
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = { },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
                }
                item {
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
                item {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
                }
                item {
                    DatePickerField(
                        selectedDate = expirationDate,
                        onDateSelected = { expirationDate = it },
                        label = "Subscription Expiration Date *",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Subscription will expire at 11:59 PM on the selected date",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Active User",
                            color = Color(0xFF0F172A) // slate-900
                        )
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
                    if (displayName.isNotBlank() && expirationDate != null) {
                        val updatedUser = user.copy(
                            displayName = displayName,
                            department = department,
                            jobTitle = jobTitle,
                            isActive = isActive,
                            expiresAt = expirationDate!!
                        )
                        onUpdate(updatedUser)
                    }
                },
                enabled = displayName.isNotBlank() && expirationDate != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A), // blue-900
                    disabledContainerColor = Color(0xFFE2E8F0) // slate-200
                )
            ) {
                Text(
                    "Update",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cancel",
                    color = Color(0xFF0F172A) // slate-900
                )
            }
        }
    )
}

@Composable
private fun DeleteUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { 
            Text(
                "Delete User",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A) // slate-900
            )
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to delete this user?",
                    color = Color(0xFF64748B), // slate-500
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "User: ${user.displayName}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
                Text(
                    "Email: ${user.email}",
                    color = Color(0xFF64748B) // slate-500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDC2626) // red-600
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDelete(user.uid) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626) // red-600
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Delete",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cancel",
                    color = Color(0xFF0F172A) // slate-900
                )
            }
        }
    )
}

