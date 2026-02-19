package com.pramod.validator.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Enterprise
import com.pramod.validator.viewmodel.SuperAdminViewModel
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.ui.components.DatePickerField
import com.pramod.validator.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminEnterprisesScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToCreateEnterprise: () -> Unit,
    superAdminViewModel: SuperAdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        superAdminViewModel.loadEnterprises()
    }
    
    val enterprises by superAdminViewModel.filteredEnterprises.collectAsState()
    val isLoading by superAdminViewModel.isLoading.collectAsState()
    val errorMessage by superAdminViewModel.errorMessage.collectAsState()
    val successMessage by superAdminViewModel.successMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showEditEnterpriseDialog by remember { mutableStateOf(false) }
    var showDeleteEnterpriseDialog by remember { mutableStateOf(false) }
    var selectedEnterprise by remember { mutableStateOf<Enterprise?>(null) }
    var enterprisesState by remember { mutableStateOf<List<Enterprise>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // Update local state when enterprises change
    LaunchedEffect(enterprises) {
        enterprisesState = enterprises
    }
    
    LaunchedEffect(searchQuery) {
        superAdminViewModel.updateEnterpriseSearchQuery(searchQuery)
    }
    
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
                            "Enterprises",
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
                        IconButton(onClick = onNavigateToCreateEnterprise) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Enterprise",
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
                        "Search enterprises...",
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
            
            // Enterprises list - reuse EnterpriseCard from SuperAdminDashboardScreen
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E3A8A) // blue-900
                    )
                }
            } else if (enterprises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF94A3B8) // slate-400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No enterprises found"
                            } else {
                                "No enterprises yet"
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
                                "Create your first enterprise to get started"
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
                    items(enterprises) { enterprise ->
                        ModernEnterpriseCard(
                            enterprise = enterprise,
                            onEditClick = {
                                                    selectedEnterprise = enterprise
                                                    showEditEnterpriseDialog = true
                                                },
                            onDeleteClick = {
                                                    selectedEnterprise = enterprise
                                                    showDeleteEnterpriseDialog = true
                                                },
                            colorBarColor = Color(0xFF8B5CF6) // violet-500
                        )
                    }
                }
            }
        }
    }
    
    // Edit Enterprise Dialog
    if (showEditEnterpriseDialog && selectedEnterprise != null) {
        // Get the latest enterprise from the list to ensure we have the most up-to-date data
        val latestEnterprise = enterprises.find { it.id == selectedEnterprise!!.id } ?: selectedEnterprise!!
        
        // Use key to force recomposition when enterprise data changes
        key(latestEnterprise.id, latestEnterprise.isActive, latestEnterprise.expiresAt) {
            EditEnterpriseDialog(
                enterprise = latestEnterprise,
                onDismiss = {
                    showEditEnterpriseDialog = false
                    selectedEnterprise = null
                },
            onUpdate = { updatedEnterprise: Enterprise, expiresAtValue: Long ->
                scope.launch {
                    val finalEnterprise = updatedEnterprise.copy(expiresAt = expiresAtValue)
                    val result = superAdminViewModel.updateEnterprise(finalEnterprise)
                    
                    // Wait for the update to complete and enterprises to reload
                    result.fold(
                        onSuccess = {
                            // The ViewModel's updateEnterprise already calls loadEnterprises()
                            // Wait a bit more to ensure Firestore has propagated and UI has updated
                            kotlinx.coroutines.delay(500)
                            showEditEnterpriseDialog = false
                            selectedEnterprise = null
                        },
                        onFailure = {
                            // Keep dialog open on error so user can see the error message
                            // Don't close the dialog
                        }
                    )
                }
            },
            superAdminViewModel = superAdminViewModel
            )
        }
    }
    
    // Delete Enterprise Dialog
    if (showDeleteEnterpriseDialog && selectedEnterprise != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteEnterpriseDialog = false
                selectedEnterprise = null
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { 
                Text(
                    "Delete Enterprise",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to delete this enterprise?",
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enterprise: ${selectedEnterprise!!.companyName}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ This will delete ALL ${selectedEnterprise!!.currentUserCount} users in this enterprise!",
                        fontSize = 14.sp,
                        color = Color(0xFFDC2626), // red-600
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        fontSize = 13.sp,
                        color = Color(0xFFDC2626) // red-600
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            superAdminViewModel.deleteEnterprise(selectedEnterprise!!.id)
                            showDeleteEnterpriseDialog = false
                            selectedEnterprise = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626) // red-600
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Delete Enterprise",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteEnterpriseDialog = false
                        selectedEnterprise = null
                    },
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
}

@Composable
private fun ModernEnterpriseCard(
    enterprise: Enterprise,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    colorBarColor: Color
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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color bar at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = colorBarColor,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = enterprise.companyName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = enterprise.adminEmail,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                    
                    if (enterprise.contactPhone.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = enterprise.contactPhone,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8) // slate-400
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Users: ${enterprise.currentUserCount}/${enterprise.userLimit}",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8) // slate-400
                    )
                    
                    // Show subscription end date
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                    val expirationText = if (enterprise.expiresAt > 0) {
                        dateFormat.format(Date(enterprise.expiresAt))
                    } else {
                        "No expiration"
                    }
                    Text(
                        text = "Subscription: $expirationText",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8) // slate-400
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
}

@Composable
private fun EditEnterpriseDialog(
    enterprise: Enterprise,
    onDismiss: () -> Unit,
    onUpdate: (Enterprise, Long) -> Unit,
    superAdminViewModel: SuperAdminViewModel
) {
    var companyName by remember { mutableStateOf(enterprise.companyName) }
    var contactEmail by remember { mutableStateOf(enterprise.contactEmail) }
    var contactPhone by remember { mutableStateOf(enterprise.contactPhone) }
    var address by remember { mutableStateOf(enterprise.address) }
    var industry by remember { mutableStateOf(enterprise.industry) }
    var isActive by remember { mutableStateOf(enterprise.isActive) }
    var expirationDate by remember { 
        mutableStateOf<Long?>(if (enterprise.expiresAt > 0) enterprise.expiresAt else null)
    }
    
    // Update state when enterprise prop changes
    LaunchedEffect(enterprise.id) {
        companyName = enterprise.companyName
        contactEmail = enterprise.contactEmail
        contactPhone = enterprise.contactPhone
        address = enterprise.address
        industry = enterprise.industry
        isActive = enterprise.isActive
        expirationDate = if (enterprise.expiresAt > 0) enterprise.expiresAt else null
    }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { 
            Text(
                "Edit Enterprise",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A) // slate-900
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name *") },
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
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Contact Email") },
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
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact Phone") },
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
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
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
                        value = industry,
                        onValueChange = { industry = it },
                        label = { Text("Industry") },
                        modifier = Modifier.fillMaxWidth(),
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
                        Column {
                            Text(
                                "Active Enterprise",
                                color = Color(0xFF0F172A), // slate-900
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (isActive) "Users can access the app" else "Users cannot access the app",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B) // slate-500
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { newValue ->
                                isActive = newValue
                                // Don't update immediately - wait for Save button
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val expirationDateValue = expirationDate
                    if (companyName.isNotBlank() && expirationDateValue != null) {
                        val updatedEnterprise = enterprise.copy(
                            companyName = companyName,
                            contactEmail = contactEmail,
                            contactPhone = contactPhone,
                            address = address,
                            industry = industry,
                            isActive = isActive, // Use the current state value
                            expiresAt = expirationDateValue
                        )
                        onUpdate(updatedEnterprise, expirationDateValue)
                    }
                },
                enabled = companyName.isNotBlank() && expirationDate != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A), // blue-900
                    disabledContainerColor = Color(0xFFE2E8F0) // slate-200
                )
            ) {
                Text(
                    "Save",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
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
