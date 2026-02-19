package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.data.repository.PermissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val permissionRepository = PermissionRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _currentUserPermissions = MutableStateFlow<UserPermission?>(null)
    val currentUserPermissions: StateFlow<UserPermission?> = _currentUserPermissions.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        _authState.value = AuthState.Loading
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val result = repository.getUserById(currentUser.uid)
                    result.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                _currentUser.value = user
                                // Load user permissions
                                loadUserPermissions(user.uid)
                                _authState.value = AuthState.Authenticated
                            } else {
                                _authState.value = AuthState.Unauthenticated
                            }
                        },
                        onFailure = {
                            _authState.value = AuthState.Unauthenticated
                        }
                    )
                } catch (e: Exception) {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    private suspend fun loadUserPermissions(userId: String) {
        try {
            val permissionResult = permissionRepository.getUserPermissions(userId)
            permissionResult.fold(
                onSuccess = { permissions ->
                    _currentUserPermissions.value = permissions
                    android.util.Log.d("AuthViewModel", "Loaded permissions for user: $permissions")
                },
                onFailure = { error ->
                    android.util.Log.w("AuthViewModel", "No permissions found for user: ${error.message}")
                    _currentUserPermissions.value = null
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error loading permissions: ${e.message}", e)
            _currentUserPermissions.value = null
        }
    }
    
    /**
     * Refresh user permissions - call this when permissions are updated by admin
     */
    fun refreshUserPermissions() {
        val currentUser = _currentUser.value
        if (currentUser != null) {
            viewModelScope.launch {
                loadUserPermissions(currentUser.uid)
                android.util.Log.d("AuthViewModel", "Refreshed permissions for user: ${currentUser.uid}")
            }
        }
    }
    
    private fun loadCurrentUserData() {
        viewModelScope.launch {
            val firebaseUser = repository.getCurrentUser()
            if (firebaseUser != null) {
                val result = repository.getUserById(firebaseUser.uid)
                _currentUser.value = result.getOrNull()
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signUp(email, password, displayName)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                
                // Load permissions for new user
                user?.let {
                    loadUserPermissions(it.uid)
                }
                
                // Set user ID and context in Crashlytics
                user?.let {
                    com.pramod.validator.utils.CrashReporting.setUserId(it.uid)
                    com.pramod.validator.utils.CrashReporting.setCustomKey("user_email", it.email ?: "")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("user_role", it.role)
                    it.enterpriseId?.let { enterpriseId ->
                        com.pramod.validator.utils.CrashReporting.setCustomKey("enterprise_id", enterpriseId)
                    }
                }
                
                _authState.value = AuthState.Authenticated
            } else {
                val error = result.exceptionOrNull()
                error?.let {
                    com.pramod.validator.utils.CrashReporting.logException(it, "Sign up failed")
                }
                _authState.value = AuthState.Error(error?.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            android.util.Log.d("AuthViewModel", "🔐 Attempting sign in for: $email")
            
            try {
                val result = repository.signIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        android.util.Log.d("AuthViewModel", "✅ Sign in successful for: ${user.email}, role: ${user.role}, enterpriseId: ${user.enterpriseId}")
                        _currentUser.value = user
                        
                        // Load permissions for signed in user
                        loadUserPermissions(user.uid)
                        
                        _authState.value = AuthState.Authenticated
                        
                        // Set user ID and context in Crashlytics
                        com.pramod.validator.utils.CrashReporting.setUserId(user.uid)
                        com.pramod.validator.utils.CrashReporting.setCustomKey("user_email", user.email ?: "")
                        com.pramod.validator.utils.CrashReporting.setCustomKey("user_role", user.role)
                        user.enterpriseId?.let { 
                            com.pramod.validator.utils.CrashReporting.setCustomKey("enterprise_id", it)
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("AuthViewModel", "❌ Sign in failed: ${error.message}", error)
                        
                        // Provide user-friendly error messages
                        val userMessage = when {
                            error.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your internet connection and try again."
                            error.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Connection timeout. Please check your internet connection and try again."
                            error.message?.contains("password", ignoreCase = true) == true -> 
                                "Invalid email or password. Please try again."
                            error.message?.contains("user not found", ignoreCase = true) == true -> 
                                "No account found with this email."
                            error.message?.contains("too many requests", ignoreCase = true) == true -> 
                                "Too many failed attempts. Please try again later."
                            else -> error.message ?: "Sign in failed. Please try again."
                        }
                        
                        com.pramod.validator.utils.CrashReporting.logException(error, "Sign in failed")
                        _authState.value = AuthState.Error(userMessage)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ Unexpected error during sign in: ${e.message}", e)
                val userMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection and try again."
                    else -> "An unexpected error occurred. Please try again."
                }
                _authState.value = AuthState.Error(userMessage)
            }
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return repository.sendPasswordResetEmail(email)
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Idle
        _currentUser.value = null
        _currentUserPermissions.value = null
        
        // Clear user ID from Crashlytics
        com.pramod.validator.utils.CrashReporting.setUserId("")
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
    
    suspend fun getUserRole(): String? {
        val firebaseUser = repository.getCurrentUser() ?: return null
        val user = repository.getUserById(firebaseUser.uid).getOrNull()
        return user?.role
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

