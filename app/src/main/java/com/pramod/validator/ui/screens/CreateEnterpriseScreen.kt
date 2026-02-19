package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.Enterprise
import com.pramod.validator.viewmodel.SuperAdminViewModel
import com.pramod.validator.ui.components.DatePickerField
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEnterpriseScreen(
    onBack: () -> Unit,
    onCreateEnterpriseSuccess: () -> Unit,
    viewModel: SuperAdminViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Clear loading-related errors when screen is displayed
    // Only show errors related to creating enterprises, not loading them
    LaunchedEffect(Unit) {
        val currentError = viewModel.errorMessage.value
        if (currentError != null && currentError.contains("Failed to load", ignoreCase = true)) {
            viewModel.clearMessages()
        }
    }
    
    // Filter error message to only show create-related errors
    val displayError = remember(errorMessage) {
        if (errorMessage != null && !errorMessage!!.contains("Failed to load", ignoreCase = true)) {
            errorMessage
        } else {
            null
        }
    }
    
    var companyName by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf<Long?>(null) }
    
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "Create Enterprise",
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Admin Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = { adminEmail = it },
                    label = { Text("Admin Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    label = { Text("Admin Password *") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Contact Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Contact Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error message (only show create-related errors, not loading errors)
                if (displayError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = displayError,
                                color = Color(0xFFDC2626), // red-600
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Success message
                LaunchedEffect(successMessage) {
                    if (successMessage != null) {
                        kotlinx.coroutines.delay(2000) // Show success message for 2 seconds
                        onCreateEnterpriseSuccess()
                    }
                }
                
                if (successMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = successMessage ?: "",
                                color = Color(0xFF10B981), // green-600
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0F172A) // slate-900
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) // slate-200
                    ) {
                        Text(
                            "Cancel",
                            color = Color(0xFF0F172A) // slate-900
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (companyName.isNotBlank() && adminName.isNotBlank() && 
                                adminEmail.isNotBlank() && adminPassword.isNotBlank() &&
                                expirationDate != null) {
                                
                                val enterprise = Enterprise(
                                    id = UUID.randomUUID().toString(),
                                    companyName = companyName,
                                    adminName = adminName,
                                    adminEmail = adminEmail,
                                    contactEmail = contactEmail.ifBlank { adminEmail },
                                    contactPhone = contactPhone,
                                    address = address,
                                    industry = industry
                                )
                                
                                scope.launch {
                                    viewModel.createEnterprise(enterprise, adminPassword, expirationDate!!)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = companyName.isNotBlank() && adminName.isNotBlank() && 
                                 adminEmail.isNotBlank() && adminPassword.isNotBlank() &&
                                 expirationDate != null && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A), // blue-900
                            disabledContainerColor = Color(0xFFE2E8F0) // slate-200
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Create",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

