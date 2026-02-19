package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.Enterprise
import com.pramod.validator.data.models.User
import com.pramod.validator.data.repository.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuperAdminViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    
    private val _enterprises = MutableStateFlow<List<Enterprise>>(emptyList())
    val enterprises: StateFlow<List<Enterprise>> = _enterprises.asStateFlow()
    
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Search functionality
    private val _enterpriseSearchQuery = MutableStateFlow("")
    val enterpriseSearchQuery: StateFlow<String> = _enterpriseSearchQuery.asStateFlow()
    
    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()
    
    // Filtered data
    val filteredEnterprises: StateFlow<List<Enterprise>> = combine(
        _enterprises,
        _enterpriseSearchQuery
    ) { enterprises, query ->
        if (query.isBlank()) {
            enterprises
        } else {
            enterprises.filter { 
                it.companyName.contains(query, ignoreCase = true) ||
                it.adminName.contains(query, ignoreCase = true) ||
                it.adminEmail.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val filteredUsers: StateFlow<List<User>> = combine(
        _allUsers,
        _userSearchQuery
    ) { users, query ->
        // Filter to only show standalone users (no enterpriseId)
        val standaloneUsers = users.filter { it.enterpriseId.isEmpty() }
        
        if (query.isBlank()) {
            standaloneUsers
        } else {
            standaloneUsers.filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.companyName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        loadEnterprises()
        // Add a small delay before loading users to ensure user document is fully propagated
        viewModelScope.launch {
            delay(500)
        loadAllUsers()
        }
    }
    
    fun loadEnterprises() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.getAllEnterprises()
            result.fold(
                onSuccess = { 
                    _enterprises.value = it
                    // Clear any previous error messages on success
                    _errorMessage.value = null
                },
                onFailure = { error ->
                    // Only show error if it's a permission error, not if it's just an empty collection
                    val errorMessage = error.message ?: "Unknown error"
                    if (errorMessage.contains("PERMISSION_DENIED", ignoreCase = true)) {
                        _errorMessage.value = "Failed to load enterprises: ${error.message}"
                    } else {
                        // For other errors, log but don't show to user (might be temporary network issues)
                        android.util.Log.e("SuperAdminViewModel", "Error loading enterprises: ${error.message}", error)
                        // Set empty list instead of showing error
                        _enterprises.value = emptyList()
                    }
                }
            )
            
            _isLoading.value = false
        }
    }
    
    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Retry up to 3 times with exponential backoff for permission errors
            var lastError: Throwable? = null
            var success = false
            
            for (attempt in 0 until 3) {
                if (success) break
            
            val result = repository.getAllUsers()
            result.fold(
                onSuccess = { 
                    _allUsers.value = it
                    // Clear any previous error messages on success
                    _errorMessage.value = null
                        success = true
                },
                onFailure = { error ->
                        lastError = error
                    val errorMessage = error.message ?: "Unknown error"
                    if (errorMessage.contains("PERMISSION_DENIED", ignoreCase = true)) {
                            if (attempt < 2) {
                                // Retry with exponential backoff
                                val delayMs = 500L * (attempt + 1)
                                android.util.Log.w("SuperAdminViewModel", "Permission denied, retrying in ${delayMs}ms (attempt ${attempt + 2}/3)")
                                delay(delayMs)
                            } else {
                                // Final attempt failed
                        _errorMessage.value = "Failed to load users: ${error.message}"
                                android.util.Log.e("SuperAdminViewModel", "Failed to load users after 3 attempts: ${error.message}", error)
                                success = true // Stop retrying
                            }
                    } else {
                        // For other errors, log but don't show to user (might be temporary network issues)
                        android.util.Log.e("SuperAdminViewModel", "Error loading users: ${error.message}", error)
                        // Set empty list instead of showing error
                        _allUsers.value = emptyList()
                            success = true // Don't retry for non-permission errors
                    }
                }
            )
            }
            
            if (!success && lastError != null) {
                // All retries failed
                _allUsers.value = emptyList()
            }
            
            _isLoading.value = false
        }
    }
    
    suspend fun createEnterprise(enterprise: Enterprise, adminPassword: String, expiresAt: Long): Result<Enterprise> {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.createEnterprise(enterprise, adminPassword, expiresAt)
        
        result.fold(
            onSuccess = { createdEnterprise ->
                // Send credentials email to enterprise admin
                viewModelScope.launch {
                    val emailService = com.pramod.validator.services.EmailService()
                    emailService.sendCredentialsEmail(
                        toEmail = enterprise.adminEmail,
                        recipientName = enterprise.adminName,
                        tempPassword = adminPassword,
                        enterpriseName = enterprise.companyName
                    )
                }
                
                _successMessage.value = "Enterprise created successfully! Login credentials have been sent to ${enterprise.adminEmail}"
                loadEnterprises()
                loadAllUsers()
            },
            onFailure = {
                _errorMessage.value = "Failed to create enterprise: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    suspend fun createUser(
        email: String,
        password: String,
        displayName: String,
        expiresAt: Long,
        department: String = "",
        jobTitle: String = ""
    ): Result<User> {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.createUser(email, password, displayName, expiresAt, department, jobTitle)
        
        result.fold(
            onSuccess = { createdUser ->
                // Send credentials email to standalone user
                viewModelScope.launch {
                    val emailService = com.pramod.validator.services.EmailService()
                    emailService.sendCredentialsEmail(
                        toEmail = email,
                        recipientName = displayName,
                        tempPassword = password,
                        enterpriseName = "Validator" // Standalone user, no enterprise
                    )
                }
                
                _successMessage.value = "User created successfully! Login credentials have been sent to $email"
                loadAllUsers()
            },
            onFailure = {
                _errorMessage.value = "Failed to create user: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    suspend fun updateUser(user: User): Result<Unit> {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.updateUser(user)
        
        result.fold(
            onSuccess = {
                _successMessage.value = "User updated successfully!"
                loadAllUsers()
            },
            onFailure = {
                _errorMessage.value = "Failed to update user: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    suspend fun deleteUser(userId: String): Result<Unit> {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.deleteUser(userId)
        
        result.fold(
            onSuccess = {
                _successMessage.value = "User deleted successfully!"
                // Reload both users and enterprises to reflect any count changes
                loadAllUsers()
                loadEnterprises()
            },
            onFailure = {
                _errorMessage.value = "Failed to delete user: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    suspend fun deleteEnterprise(enterpriseId: String): Result<Unit> {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.deleteEnterprise(enterpriseId)
        
        result.fold(
            onSuccess = {
                _successMessage.value = "Enterprise and all its users deleted successfully!"
                loadEnterprises()
                loadAllUsers()
            },
            onFailure = {
                _errorMessage.value = "Failed to delete enterprise: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    suspend fun updateEnterprise(enterprise: Enterprise): Result<Unit> {
        _isLoading.value = true
        _errorMessage.value = null
        
        val result = repository.updateEnterprise(enterprise)
        
        result.fold(
            onSuccess = {
                _successMessage.value = "Enterprise updated successfully!"
                // Reload enterprises and wait for it to complete
                loadEnterprises()
                // Wait a bit for the reload to complete
                kotlinx.coroutines.delay(200)
            },
            onFailure = {
                _errorMessage.value = "Failed to update enterprise: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    // Search functions
    fun updateEnterpriseSearchQuery(query: String) {
        _enterpriseSearchQuery.value = query
    }
    
    fun updateUserSearchQuery(query: String) {
        _userSearchQuery.value = query
    }
    
    fun getStatistics(): Map<String, Int> {
        return mapOf(
            "totalEnterprises" to _enterprises.value.size,
            "activeEnterprises" to _enterprises.value.count { it.isActive },
            "totalUsers" to _allUsers.value.size,
            "activeUsers" to _allUsers.value.count { it.isActive },
            "enterpriseAdmins" to _allUsers.value.count { it.role == "ENTERPRISE_ADMIN" },
            "regularUsers" to _allUsers.value.count { it.role == "USER" }
        )
    }
}

