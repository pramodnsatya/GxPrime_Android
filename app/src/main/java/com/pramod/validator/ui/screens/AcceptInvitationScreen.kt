package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pramod.validator.data.models.Invitation
import com.pramod.validator.data.repository.InvitationRepository
import com.pramod.validator.data.repository.FirebaseRepository
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun AcceptInvitationScreen(
    token: String? = null,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val actualToken = token ?: getStoredInvitationToken(context)
    var invitation by remember { mutableStateOf<Invitation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) }
    
    val invitationRepository = InvitationRepository()
    val firebaseRepository = FirebaseRepository()
    val scope = rememberCoroutineScope()
    
    // Load invitation on startup
    LaunchedEffect(actualToken) {
        if (actualToken.isNullOrBlank()) {
            error = "No invitation token found"
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val result = invitationRepository.getInvitationByToken(actualToken)
            result.fold(
                onSuccess = { inv ->
                    if (inv != null && !inv.isUsed && inv.expiresAt > System.currentTimeMillis()) {
                        invitation = inv
                        isLoading = false
                    } else {
                        error = "Invitation not found, expired, or already used"
                        isLoading = false
                    }
                },
                onFailure = { e ->
                    error = "Failed to load invitation: ${e.message}"
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            error = "Error: ${e.message}"
            isLoading = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading invitation...")
                }
            }
            
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            invitation != null -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Accept Invitation",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Invitation Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "You've been invited to join:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Name: ${invitation!!.displayName}")
                                Text("Email: ${invitation!!.email}")
                                Text("Department: ${invitation!!.department}")
                                Text("Job Title: ${invitation!!.jobTitle}")
                            }
                        }
                        
                        // Password Setup
                        Text(
                            text = "Set up your account password:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onError("Invitation declined") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Decline")
                            }
                            
                            Button(
                                onClick = {
                                    if (password.length < 6) {
                                        onError("Password must be at least 6 characters")
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        onError("Passwords do not match")
                                        return@Button
                                    }
                                    
                                    scope.launch {
                                        isCreatingAccount = true
                                        try {
                                            // Create the user account with permissions
                                            val userResult = firebaseRepository.createEnterpriseUser(
                                                email = invitation!!.email,
                                                password = password,
                                                displayName = invitation!!.displayName,
                                                enterpriseId = invitation!!.enterpriseId,
                                                department = invitation!!.department,
                                                jobTitle = invitation!!.jobTitle,
                                                permissions = invitation!!.permissions
                                            )
                                            
                                            userResult.fold(
                                                onSuccess = { user ->
                                                    // Mark invitation as used
                                                    actualToken?.let { 
                                                        invitationRepository.markInvitationAsUsed(it)
                                                    }
                                                    onSuccess()
                                                },
                                                onFailure = { e ->
                                                    onError("Failed to create account: ${e.message}")
                                                }
                                            )
                                        } catch (e: Exception) {
                                            onError("Error: ${e.message}")
                                        } finally {
                                            isCreatingAccount = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isCreatingAccount && password.isNotBlank() && confirmPassword.isNotBlank()
                            ) {
                                if (isCreatingAccount) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Accept & Create Account")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getStoredInvitationToken(context: Context): String? {
    val sharedPref = context.getSharedPreferences("invitation", Context.MODE_PRIVATE)
    return sharedPref.getString("invitation_token", null)
}
