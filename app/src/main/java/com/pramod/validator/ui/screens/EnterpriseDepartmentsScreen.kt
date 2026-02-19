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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Department
import com.pramod.validator.viewmodel.DepartmentViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.ui.components.CreateDepartmentDialog
import com.pramod.validator.ui.components.EditDepartmentDialog
import com.pramod.validator.ui.components.DeleteDepartmentDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseDepartmentsScreen(
    enterpriseId: String,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    departmentViewModel: DepartmentViewModel = viewModel()
) {
    // Initialize ViewModel
    LaunchedEffect(enterpriseId) {
        departmentViewModel.loadDepartments(enterpriseId)
    }
    
    val departments by departmentViewModel.departments.collectAsState()
    val isLoading by departmentViewModel.isLoading.collectAsState()
    val errorMessage by departmentViewModel.errorMessage.collectAsState()
    val successMessage by departmentViewModel.successMessage.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDepartmentDialog by remember { mutableStateOf(false) }
    var showEditDepartmentDialog by remember { mutableStateOf(false) }
    var showDeleteDepartmentDialog by remember { mutableStateOf(false) }
    var selectedDepartment by remember { mutableStateOf<Department?>(null) }
    val scope = rememberCoroutineScope()
    
    // Filter departments based on search query
    val filteredDepartments = departments.filter { department ->
        searchQuery.isEmpty() ||
        department.name.contains(searchQuery, ignoreCase = true) ||
        department.description.contains(searchQuery, ignoreCase = true)
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
                            "Departments",
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
                    actions = {
                        IconButton(onClick = { showCreateDepartmentDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Department",
                                tint = Color(0xFF1E3A8A) // blue-900
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
                .background(Color(0xFFF8FAFC)) // slate-50
        ) {
            // Error/Success messages
            LaunchedEffect(errorMessage, successMessage) {
                if (errorMessage != null || successMessage != null) {
                    kotlinx.coroutines.delay(3000)
                    departmentViewModel.clearMessages()
                }
            }
            
            errorMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            text = message,
                            color = Color(0xFFDC2626) // red-600
                        )
                    }
                }
            }
            
            successMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            text = message,
                            color = Color(0xFF10B981) // green-600
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
                    .padding(20.dp),
                placeholder = { Text("Search departments...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                    unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                )
            )
            
            // Departments list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E3A8A) // blue-900
                    )
                }
            } else if (filteredDepartments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No departments found" else "No departments yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B) // slate-500
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Add your first department to get started",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8) // slate-400
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDepartments) { department ->
                        ModernDepartmentCard(
                            department = department,
                            onEditClick = {
                                selectedDepartment = department
                                showEditDepartmentDialog = true
                            },
                            onDeleteClick = {
                                selectedDepartment = department
                                showDeleteDepartmentDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Create Department Dialog
    CreateDepartmentDialog(
        showDialog = showCreateDepartmentDialog,
        onDismiss = { showCreateDepartmentDialog = false },
        onCreate = { name, description ->
            departmentViewModel.createDepartment(name, description, enterpriseId)
        },
        isLoading = isLoading,
        errorMessage = errorMessage,
        successMessage = successMessage
    )
    
    // Track success to close create dialog
    LaunchedEffect(successMessage) {
        if (successMessage?.isNotEmpty() == true && showCreateDepartmentDialog) {
            kotlinx.coroutines.delay(1000)
            showCreateDepartmentDialog = false
        }
    }
    
    // Edit Department Dialog
    EditDepartmentDialog(
        showDialog = showEditDepartmentDialog,
        onDismiss = { 
            showEditDepartmentDialog = false
            selectedDepartment = null
        },
        department = selectedDepartment,
        onSave = { name, description ->
            selectedDepartment?.let {
                val updatedDepartment = it.copy(name = name, description = description)
                departmentViewModel.updateDepartment(updatedDepartment)
            }
        },
        isLoading = isLoading,
        errorMessage = errorMessage,
        successMessage = successMessage
    )
    
    // Track success to close edit dialog
    LaunchedEffect(successMessage) {
        if (successMessage?.isNotEmpty() == true && showEditDepartmentDialog) {
            kotlinx.coroutines.delay(1000)
            showEditDepartmentDialog = false
            selectedDepartment = null
        }
    }
    
    // Delete Department Dialog
    DeleteDepartmentDialog(
        showDialog = showDeleteDepartmentDialog,
        onDismiss = { 
            showDeleteDepartmentDialog = false
            selectedDepartment = null
        },
        department = selectedDepartment,
        onConfirm = {
            selectedDepartment?.let {
                departmentViewModel.deleteDepartment(it.id)
            }
        },
        affectedAssessmentsCount = null,
        isLoading = isLoading,
        errorMessage = errorMessage,
        successMessage = successMessage
    )
    
    // Track success/error to close delete dialog
    LaunchedEffect(successMessage, errorMessage) {
        if (showDeleteDepartmentDialog) {
            if (successMessage?.isNotEmpty() == true) {
                kotlinx.coroutines.delay(1000)
                showDeleteDepartmentDialog = false
                selectedDepartment = null
            } else if (errorMessage?.contains("Cannot delete") == true) {
                // Keep dialog open for "Cannot delete" errors (shows warning)
            }
        }
    }
}

@Composable
private fun ModernDepartmentCard(
    department: Department,
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
            // Color bar on top - violet-500 matching "Departments" metric
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color(0xFF8B5CF6), // violet-500
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
                    text = department.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A) // slate-900
                )
                if (department.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = department.description,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
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
                        properties = PopupProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
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
                            // Edit option
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
                            
                            // Delete option
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

