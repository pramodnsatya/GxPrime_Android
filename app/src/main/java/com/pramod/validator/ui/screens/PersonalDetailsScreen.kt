package com.pramod.validator.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.ui.components.InfoRow
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onNavigateToResources: () -> Unit,
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    // Use AuthViewModel for bottom nav to avoid flash of all items
    val currentUserData by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    
    var firestoreUser by remember { mutableStateOf<User?>(null) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Load user data from Firestore - fresh data every time
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            android.util.Log.e("PersonalDetailsScreen", "No Firebase Auth user found")
            return@LaunchedEffect
        }
        
        currentUser.uid.let { uid ->
            try {
                android.util.Log.d("PersonalDetailsScreen", "Loading user data for uid: $uid")
                val userDoc = firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()
                
                val user = userDoc.toObject(User::class.java)
                android.util.Log.d("PersonalDetailsScreen", "Loaded user: ${user?.displayName}, role: ${user?.role}, email: ${user?.email}")
                
                // Only update if this is actually the current logged-in user
                if (user?.uid == uid) {
                    firestoreUser = user
                } else {
                    android.util.Log.e("PersonalDetailsScreen", "User UID mismatch! Expected: $uid, Got: ${user?.uid}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PersonalDetailsScreen", "Error loading user data: ${e.message}")
            }
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
                            "Personal Details",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A) // slate-900
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "profile",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> onNavigateToHistory()
                        "fda483" -> onNavigateToFda483()
                        "profile" -> { /* Already on profile - do nothing */ }
                        "resources" -> onNavigateToResources()
                    }
                },
                user = currentUserData,
                permissions = currentUserPermissions
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Account Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Account Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0)) // slate-200

                    InfoRow(
                        label = "Name",
                        value = firestoreUser?.displayName?.ifBlank { 
                            currentUser?.displayName?.ifBlank { "Not set" } ?: "Not set"
                        } ?: currentUser?.displayName ?: "Not set"
                    )

                    InfoRow(
                        label = "Email",
                        value = currentUser?.email ?: "Not available"
                    )

                    if (firestoreUser?.enterpriseId?.isNotBlank() == true) {
                        InfoRow(
                            label = "Enterprise",
                            value = firestoreUser?.companyName?.ifBlank { "Not set" } ?: "Not set"
                        )
                    }

                    // Only show department and job title for regular users, not for enterprise admins
                    if (firestoreUser?.role != "ENTERPRISE_ADMIN" && firestoreUser?.department?.isNotBlank() == true) {
                        InfoRow(
                            label = "Department",
                            value = firestoreUser?.department ?: ""
                        )
                    }

                    if (firestoreUser?.role != "ENTERPRISE_ADMIN" && firestoreUser?.jobTitle?.isNotBlank() == true) {
                        InfoRow(
                            label = "Job Title",
                            value = firestoreUser?.jobTitle ?: ""
                        )
                    }
                    
                    // Show subscription end date for standalone users and enterprise admins
                    firestoreUser?.let { user ->
                        val shouldShowSubscription = user.enterpriseId.isEmpty() || user.role == "ENTERPRISE_ADMIN"
                        if (shouldShowSubscription) {
                            if (user.expiresAt > 0) {
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                val expirationDate = dateFormat.format(Date(user.expiresAt))
                                InfoRow(
                                    label = "Subscription End Date",
                                    value = expirationDate
                                )
                            } else {
                                InfoRow(
                                    label = "Subscription End Date",
                                    value = "No expiration"
                                )
                            }
                        }
                    }
                }
            }

            // Change Password Button
            Button(
                onClick = { showChangePassword = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A) // blue-900
                )
            ) {
                Text(
                    "Change Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            // Sign Out Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Account Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    
                    Button(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444) // red-500
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Sign Out",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    // Change Password Modal Bottom Sheet
    if (showChangePassword) {
        ModalBottomSheet(
            onDismissRequest = { showChangePassword = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Change Password",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        message = null
                    },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        message = null
                    },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        message = null
                    },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (message != null) {
                    Text(
                        text = message ?: "",
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        when {
                            currentPassword.isBlank() -> {
                                message = "Please enter current password"
                                isError = true
                            }
                            newPassword.isBlank() -> {
                                message = "Please enter new password"
                                isError = true
                            }
                            newPassword.length < 6 -> {
                                message = "Password must be at least 6 characters"
                                isError = true
                            }
                            newPassword != confirmPassword -> {
                                message = "Passwords do not match"
                                isError = true
                            }
                            else -> {
                                scope.launch {
                                    isChangingPassword = true
                                    try {
                                        val user = auth.currentUser
                                        val email = user?.email
                                        if (user != null && email != null) {
                                            val credential = EmailAuthProvider.getCredential(email, currentPassword)
                                            user.reauthenticate(credential).await()
                                            user.updatePassword(newPassword).await()
                                            message = "Password changed successfully!"
                                            isError = false
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmPassword = ""
                                            showChangePassword = false
                                        }
                                    } catch (e: Exception) {
                                        message = "Error: ${e.message}"
                                        isError = true
                                    }
                                    isChangingPassword = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A) // blue-900
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "Change Password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    
    // Sign Out Modal Bottom Sheet
    if (showSignOutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sign Out",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )

                Text(
                    text = "Are you sure you want to sign out? You will need to sign in again to access your account.",
                    color = Color(0xFF64748B), // slate-500
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444) // red-500
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Sign Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { showSignOutDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444) // red-500
                    )
                ) {
                    Text(
                        "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
