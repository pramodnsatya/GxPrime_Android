package com.pramod.validator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.viewmodel.AuthState
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.ui.components.AbstractBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit, // (userRole, enterpriseId)
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Get user data and route based on role
                val user = currentUser
                if (user != null) {
                    onLoginSuccess(user.role, user.enterpriseId)
                } else {
                    // Fallback to regular user if data not loaded
                    onLoginSuccess("USER", "")
                }
            }
            is AuthState.Error -> errorMessage = (authState as AuthState.Error).message
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Decorative abstract background
        AbstractBackground(
            modifier = Modifier
                .fillMaxSize()
                .matchParentSize()
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
        // Sign-in card with solid background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo and title with icon
                Text(
                    text = "GxPrime",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Sign in to continue",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = { passwordVisible = !passwordVisible }
                        ) {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                fontSize = 12.sp
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showForgotPasswordDialog = true }
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            authViewModel.signIn(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = "Contact your administrator for account access",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        }
        
        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                authViewModel = authViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel
) {
    var resetEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { 
            if (!isLoading) onDismiss()
        },
        title = {
            Text(
                text = "Reset Password",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isSuccess) {
                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { 
                            resetEmail = it
                            statusMessage = null
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isLoading
                    )
                }

                if (statusMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSuccess) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = if (isSuccess)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isSuccess) {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            isLoading = true
                            scope.launch {
                                val result = authViewModel.sendPasswordResetEmail(resetEmail)
                                isLoading = false
                                if (result.isSuccess) {
                                    isSuccess = true
                                    statusMessage = "Password reset email sent! Please check your inbox."
                                } else {
                                    statusMessage = result.exceptionOrNull()?.message 
                                        ?: "Failed to send reset email. Please try again."
                                }
                            }
                        }
                    },
                    enabled = !isLoading && resetEmail.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Reset Link")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

