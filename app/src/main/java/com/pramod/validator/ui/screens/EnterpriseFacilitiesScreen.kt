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
import com.pramod.validator.data.models.Facility
import com.pramod.validator.viewmodel.EnterpriseAdminViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseFacilitiesScreen(
    enterpriseId: String,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    enterpriseAdminViewModel: EnterpriseAdminViewModel = viewModel()
) {
    // Initialize ViewModel
    LaunchedEffect(enterpriseId) {
        enterpriseAdminViewModel.initialize(enterpriseId)
    }
    
    val facilities by enterpriseAdminViewModel.facilities.collectAsState()
    val isLoading by enterpriseAdminViewModel.isLoading.collectAsState()
    val errorMessage by enterpriseAdminViewModel.errorMessage.collectAsState()
    val successMessage by enterpriseAdminViewModel.successMessage.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFacilityDialog by remember { mutableStateOf(false) }
    var showEditFacilityDialog by remember { mutableStateOf(false) }
    var showDeleteFacilityDialog by remember { mutableStateOf(false) }
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    val scope = rememberCoroutineScope()
    
    // Filter facilities based on search query
    val filteredFacilities = facilities.filter { facility ->
        searchQuery.isEmpty() ||
        facility.name.contains(searchQuery, ignoreCase = true) ||
        facility.description.contains(searchQuery, ignoreCase = true)
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
                            "Facilities",
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
                        IconButton(onClick = { showCreateFacilityDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Facility",
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
                    enterpriseAdminViewModel.clearMessages()
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
                placeholder = { Text("Search facilities...") },
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
            
            // Facilities list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E3A8A) // blue-900
                    )
                }
            } else if (filteredFacilities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No facilities found" else "No facilities yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B) // slate-500
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Add your first facility to get started",
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
                    items(filteredFacilities) { facility ->
                        ModernFacilityCard(
                            facility = facility,
                            onEditClick = {
                                selectedFacility = facility
                                showEditFacilityDialog = true
                            },
                            onDeleteClick = {
                                selectedFacility = facility
                                showDeleteFacilityDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Create Facility Dialog
    if (showCreateFacilityDialog) {
        CreateFacilityDialog(
            onDismiss = { showCreateFacilityDialog = false },
            onCreate = { name, description ->
                scope.launch {
                    enterpriseAdminViewModel.createFacility(
                        name = name,
                        description = description,
                        createdBy = "admin"
                    )
                    showCreateFacilityDialog = false
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
                    enterpriseAdminViewModel.updateFacility(
                        selectedFacility!!.id,
                        name,
                        description,
                        "admin"
                    )
                    showEditFacilityDialog = false
                    selectedFacility = null
                }
            }
        )
    }
    
    // Delete Facility Dialog
    if (showDeleteFacilityDialog && selectedFacility != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteFacilityDialog = false
                selectedFacility = null
            },
            containerColor = Color.White,
            title = { 
                Text(
                    "Delete Facility?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to delete ${selectedFacility?.name}? This action cannot be undone.",
                    fontSize = 15.sp,
                    color = Color(0xFF64748B) // slate-500
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            selectedFacility?.let { facility ->
                                enterpriseAdminViewModel.deleteFacility(facility.id, "admin")
                            }
                            showDeleteFacilityDialog = false
                            selectedFacility = null
                        }
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
                    onClick = { 
                        showDeleteFacilityDialog = false
                        selectedFacility = null
                    },
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

@Composable
private fun ModernFacilityCard(
    facility: Facility,
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
            // Color bar on top - emerald-500 matching "Facilities" metric
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color(0xFF10B981), // emerald-500
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
                    text = facility.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A) // slate-900
                )
                if (facility.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = facility.description,
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

@Composable
private fun CreateFacilityDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { 
            Text(
                "Create Facility",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A) // slate-900
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
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A) // blue-900
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Create",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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
        containerColor = Color.White,
        title = { 
            Text(
                "Edit Facility",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A) // slate-900
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
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A) // blue-900
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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

