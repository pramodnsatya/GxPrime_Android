package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Domain
import com.pramod.validator.data.models.SubDomain
import com.pramod.validator.data.models.Facility
import com.pramod.validator.data.repository.FacilityRepository
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.viewmodel.SubDomainViewModel
import com.pramod.validator.viewmodel.AuthViewModel
import androidx.compose.material3.MenuAnchorType
import kotlinx.coroutines.launch
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.ui.components.AbstractBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubDomainScreen(
    domain: Domain,
    onSubDomainSelected: (SubDomain, String, String, String) -> Unit, // subdomain, assessment name, facility id, facility name
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    viewModel: SubDomainViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    val subDomains by viewModel.subDomains.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Facility-related state
    val facilityRepository = remember { FacilityRepository() }
    val firebaseRepository = remember { FirebaseRepository() }
    var facilities by remember { mutableStateOf<List<Facility>>(emptyList()) }
    var isLoadingFacilities by remember { mutableStateOf(false) }
    
    // Load sub-domains when domain changes
    LaunchedEffect(domain.id) {
        viewModel.loadSubDomains(domain.id)
    }
    
    // Load current user and facilities
    LaunchedEffect(Unit) {
        isLoadingFacilities = true
        try {
            // Get current user from Firebase
            val firebaseUser = firebaseRepository.getCurrentUser()
            if (firebaseUser != null) {
                // Get user details from Firestore
                val userResult = firebaseRepository.getUserById(firebaseUser.uid)
                val user = userResult.getOrNull()
                
                // Load facilities if user is from an enterprise
                if (user?.enterpriseId?.isNotEmpty() == true) {
                    val facilitiesResult = facilityRepository.getFacilitiesByEnterprise(user.enterpriseId)
                    facilitiesResult.fold(
                        onSuccess = { facilityList ->
                            facilities = facilityList
                        },
                        onFailure = { error ->
                            android.util.Log.e("SubDomainScreen", "Failed to load facilities: ${error.message}")
                        }
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SubDomainScreen", "Error loading user/facilities: ${e.message}", e)
        }
        isLoadingFacilities = false
    }
    
    var showAssessmentDialog by remember { mutableStateOf(false) }
    var selectedSubDomain by remember { mutableStateOf<SubDomain?>(null) }
    var assessmentName by remember { mutableStateOf("") }
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }
    var customFacilityName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            text = domain.name, 
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
        containerColor = Color(0xFFF8FAFC) // slate-50 - light gray background
    ) { paddingValues ->
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            // Simplified header with better spacing
            Text(
                text = "Select a sub-domain to assess",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A), // slate-900
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            )

            // Sub-domain list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (subDomains.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No sub-domains available",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(subDomains) { subDomain ->
                        SubDomainCard(
                            subDomain = subDomain,
                            domain = domain,
                            onClick = {
                                selectedSubDomain = subDomain
                                showAssessmentDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Assessment Name Dialog
    if (showAssessmentDialog && selectedSubDomain != null) {
        AlertDialog(
            onDismissRequest = {
                showAssessmentDialog = false
                assessmentName = ""
                selectedFacility = null
                customFacilityName = ""
            },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Name this Assessment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Give a unique name to this ${selectedSubDomain!!.name} assessment.",
                        fontSize = 15.sp,
                        color = Color(0xFF64748B), // slate-500
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = assessmentName,
                        onValueChange = { assessmentName = it },
                        label = { Text("Assessment Name") },
                        placeholder = { Text("e.g., Q1 2025 - ${selectedSubDomain!!.name}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Facility Selection
                    if (currentUser?.enterpriseId?.isNotEmpty() == true) {
                        // Enterprise user - show dropdown
                        if (isLoadingFacilities) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading facilities...")
                            }
                        } else if (facilities.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedFacility?.name ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Select Facility *") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    facilities.forEach { facility ->
                                        DropdownMenuItem(
                                            text = { Text(facility.name) },
                                            onClick = {
                                                selectedFacility = facility
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // No facilities available - show text input
                            OutlinedTextField(
                                value = customFacilityName,
                                onValueChange = { customFacilityName = it },
                                label = { Text("Facility Name *") },
                                placeholder = { Text("Enter facility name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // Regular user - show text input
                        OutlinedTextField(
                            value = customFacilityName,
                            onValueChange = { customFacilityName = it },
                            label = { Text("Facility Name *") },
                            placeholder = { Text("Enter facility name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            if (assessmentName.isNotBlank() && selectedSubDomain != null) {
                                // Check if facility is selected/entered
                                val facilityName = if (currentUser?.enterpriseId?.isNotEmpty() == true) {
                                    // Enterprise user
                                    if (facilities.isNotEmpty()) {
                                        selectedFacility?.name ?: ""
                                    } else {
                                        customFacilityName
                                    }
                                } else {
                                    // Regular user
                                    customFacilityName
                                }
                                
                                if (facilityName.isNotBlank()) {
                                    val facilityId = selectedFacility?.id ?: ""
                                    onSubDomainSelected(selectedSubDomain!!, assessmentName.trim(), facilityId, facilityName)
                                    showAssessmentDialog = false
                                    assessmentName = ""
                                    selectedFacility = null
                                    customFacilityName = ""
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SubDomainScreen", "Error starting assessment: ${e.message}", e)
                        }
                    },
                    enabled = assessmentName.isNotBlank() && selectedSubDomain != null && 
                             ((currentUser?.enterpriseId?.isNotEmpty() == true && 
                               ((facilities.isNotEmpty() && selectedFacility != null) || 
                                (facilities.isEmpty() && customFacilityName.isNotBlank()))) ||
                              (currentUser?.enterpriseId?.isEmpty() != false && customFacilityName.isNotBlank())),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A) // blue-900
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Start Assessment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAssessmentDialog = false
                        assessmentName = ""
                        selectedFacility = null
                        customFacilityName = ""
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
fun SubDomainCard(
    subDomain: SubDomain,
    domain: Domain,
    onClick: () -> Unit
) {
    // Define color gradients for different domains based on domain ID or name
    val (gradientStart, gradientEnd) = when {
        // Match by ID prefix
        domain.id.contains("qu_", ignoreCase = true) || 
        domain.name.contains("Quality", ignoreCase = true) -> {
            android.util.Log.d("SubDomainCard", "Quality Unit - Indigo: ${domain.id} / ${domain.name}")
            Pair(Color(0xFF6366F1), Color(0xFF4F46E5)) // Quality Unit - indigo
        }
        domain.id.contains("pr_", ignoreCase = true) || 
        domain.name.contains("Production", ignoreCase = true) || 
        domain.name.contains("Manufacturing", ignoreCase = true) -> {
            android.util.Log.d("SubDomainCard", "Production - Violet: ${domain.id} / ${domain.name}")
            Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)) // Production - violet
        }
        domain.id.contains("mt_", ignoreCase = true) || 
        domain.name.contains("Material", ignoreCase = true) -> {
            android.util.Log.d("SubDomainCard", "Materials - Pink: ${domain.id} / ${domain.name}")
            Pair(Color(0xFFEC4899), Color(0xFFDB2777)) // Materials - pink
        }
        domain.id.contains("lab_", ignoreCase = true) || 
        domain.name.contains("Laboratory", ignoreCase = true) -> {
            android.util.Log.d("SubDomainCard", "Laboratory - Emerald: ${domain.id} / ${domain.name}")
            Pair(Color(0xFF10B981), Color(0xFF059669)) // Laboratory - emerald
        }
        domain.id.contains("fe_", ignoreCase = true) || 
        domain.name.contains("Facilities", ignoreCase = true) || 
        domain.name.contains("Equipment", ignoreCase = true) -> {
            android.util.Log.d("SubDomainCard", "Facilities - Amber: ${domain.id} / ${domain.name}")
            Pair(Color(0xFFF59E0B), Color(0xFFD97706)) // Facilities - amber
        }
        domain.id.contains("pl_", ignoreCase = true) || 
        domain.name.contains("Packaging", ignoreCase = true) || 
        domain.name.contains("Labeling", ignoreCase = true) -> {
            android.util.Log.d("SubDomainCard", "Packaging/Labeling - Rose: ${domain.id} / ${domain.name}")
            Pair(Color(0xFFF43F5E), Color(0xFFE11D48)) // Packaging/Labeling - rose
        }
        else -> {
            android.util.Log.w("SubDomainCard", "No match - Default Slate: ${domain.id} / ${domain.name}")
            Pair(Color(0xFF64748B), Color(0xFF475569)) // Default - slate
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored gradient top bar - 6px height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(gradientStart, gradientEnd)
                        )
                    )
            )
            
            // Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Numbered circle - 48x48
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFFF1F5F9), // slate-100
                            shape = RoundedCornerShape(24.dp) // Full circle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subDomain.order.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569) // slate-600
                    )
                }

                // Sub-domain info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subDomain.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    if (subDomain.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subDomain.description,
                            fontSize = 14.sp,
                            color = Color(0xFF64748B), // slate-500
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

