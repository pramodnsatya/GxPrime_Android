package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.Enterprise
import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.data.models.Department
import com.pramod.validator.data.models.DepartmentType
import com.pramod.validator.data.models.Facility
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.data.repository.PermissionRepository
import com.pramod.validator.data.repository.FacilityRepository
import com.pramod.validator.data.repository.InvitationRepository
import com.pramod.validator.data.models.Invitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EnterpriseAdminViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val permissionRepository = PermissionRepository()
    private val facilityRepository = FacilityRepository()
    private val invitationRepository = InvitationRepository()
    
    private val _enterprise = MutableStateFlow<Enterprise?>(null)
    val enterprise: StateFlow<Enterprise?> = _enterprise.asStateFlow()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _facilities = MutableStateFlow<List<Facility>>(emptyList())
    val facilities: StateFlow<List<Facility>> = _facilities.asStateFlow()
    
    private val _invitations = MutableStateFlow<List<Invitation>>(emptyList())
    val invitations: StateFlow<List<Invitation>> = _invitations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _canShowAddButton = MutableStateFlow(false)
    val canShowAddButton: StateFlow<Boolean> = _canShowAddButton.asStateFlow()
    
    // Search functionality
    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()
    
    // Filtered data
    val filteredUsers: StateFlow<List<User>> = combine(
        _users,
        _userSearchQuery
    ) { users, query ->
        if (query.isBlank()) {
            users
        } else {
            users.filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.department.contains(query, ignoreCase = true) ||
                it.jobTitle.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun initialize(enterpriseId: String) {
        android.util.Log.d("EnterpriseAdminVM", "========== INITIALIZING ==========")
        android.util.Log.d("EnterpriseAdminVM", "Received enterpriseId: '$enterpriseId'")
        android.util.Log.d("EnterpriseAdminVM", "Enterprise ID is empty: ${enterpriseId.isEmpty()}")
        
        if (enterpriseId.isEmpty()) {
            android.util.Log.e("EnterpriseAdminVM", "❌ Cannot initialize with empty enterprise ID!")
            _errorMessage.value = "Enterprise ID not found"
            return
        }
        
        loadEnterprise(enterpriseId)
        loadUsers(enterpriseId)
        // Don't call loadFacilities here - it will be called after enterprise loads
    }
    
    private fun loadEnterprise(enterpriseId: String) {
        viewModelScope.launch {
            android.util.Log.d("EnterpriseAdminVM", "Loading enterprise: $enterpriseId")
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.getEnterpriseById(enterpriseId)
            result.fold(
                onSuccess = { 
                    android.util.Log.d("EnterpriseAdminVM", "Enterprise loaded successfully: ${it?.companyName}")
                    _enterprise.value = it
                    // Update add button visibility after enterprise loads
                    updateAddButtonVisibility()
                    // NOW load facilities after enterprise is loaded
                    loadFacilities()
                },
                onFailure = { 
                    android.util.Log.e("EnterpriseAdminVM", "Failed to load enterprise: ${it.message}", it)
                    _errorMessage.value = "Failed to load enterprise: ${it.message}"
                    _canShowAddButton.value = false
                }
            )
            
            _isLoading.value = false
        }
    }
    
    private fun updateAddButtonVisibility() {
        val ent = _enterprise.value
        _canShowAddButton.value = ent != null && ent.currentUserCount < ent.userLimit
    }
    
    private fun loadUsers(enterpriseId: String) {
        viewModelScope.launch {
            android.util.Log.d("EnterpriseAdminVM", "========== LOADING USERS ==========")
            android.util.Log.d("EnterpriseAdminVM", "Enterprise ID: $enterpriseId")
            android.util.Log.d("EnterpriseAdminVM", "Current user: ${repository.getCurrentUser()?.uid}")
            // Get current user's role from Firestore
            val currentUserRole = try {
                val currentUserDoc = FirebaseFirestore.getInstance().collection("users")
                    .document(repository.getCurrentUser()?.uid ?: "")
                    .get()
                    .await()
                currentUserDoc.toObject(User::class.java)?.role ?: "UNKNOWN"
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
            android.util.Log.d("EnterpriseAdminVM", "Current user role: $currentUserRole")
            
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.getEnterpriseUsers(enterpriseId)
            result.fold(
                onSuccess = { 
                    android.util.Log.d("EnterpriseAdminVM", "✅ Users loaded successfully: ${it.size} users")
                    _users.value = it
                },
                onFailure = { 
                    android.util.Log.e("EnterpriseAdminVM", "❌ Failed to load users: ${it.message}", it)
                    _errorMessage.value = "Failed to load users: ${it.message}"
                }
            )
            
            _isLoading.value = false
        }
    }
    
    suspend fun createUser(
        email: String,
        password: String,
        displayName: String,
        department: String,
        jobTitle: String
    ): Result<User> {
        val enterpriseId = _enterprise.value?.id ?: return Result.failure(Exception("Enterprise not loaded"))
        
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.createEnterpriseUser(
            email = email,
            password = password,
            displayName = displayName,
            enterpriseId = enterpriseId,
            department = department,
            jobTitle = jobTitle
        )
        
        result.fold(
            onSuccess = { createdUser ->
                // Send credentials email to enterprise user
                viewModelScope.launch {
                    val emailService = com.pramod.validator.services.EmailService()
                    val enterpriseName = _enterprise.value?.companyName ?: "Validator"
                    emailService.sendCredentialsEmail(
                        toEmail = email,
                        recipientName = displayName,
                        tempPassword = password,
                        enterpriseName = enterpriseName
                    )
                }
                
                _successMessage.value = "User created successfully! Login credentials have been sent to $email"
                // Reload both enterprise (for updated count) and users
                loadEnterprise(enterpriseId)
                loadUsers(enterpriseId)
                // Update button visibility after creating user
                updateAddButtonVisibility()
            },
            onFailure = {
                _errorMessage.value = "Failed to create user: ${it.message}"
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    fun canCreateMoreUsers(): Boolean {
        val ent = _enterprise.value ?: return false
        return ent.currentUserCount < ent.userLimit
    }
    
    fun getRemainingSlots(): Int {
        val ent = _enterprise.value ?: return 0
        return maxOf(0, ent.userLimit - ent.currentUserCount)
    }
    
    fun getCurrentUserCount(): Int {
        return _enterprise.value?.currentUserCount ?: 0
    }
    
    fun getUserLimit(): Int {
        return _enterprise.value?.userLimit ?: 50
    }
    
    suspend fun updateUser(user: User): Result<Unit> {
        android.util.Log.d("EnterpriseAdminVM", "Starting user update for ${user.displayName}")
        android.util.Log.d("EnterpriseAdminVM", "User data: $user")
        
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        return try {
            // First verify the user exists
            val existingUser = repository.getUserById(user.uid).getOrNull()
            if (existingUser == null) {
                android.util.Log.e("EnterpriseAdminVM", "❌ User not found in database: ${user.uid}")
                _errorMessage.value = "User not found in database"
                _isLoading.value = false
                return Result.failure(Exception("User not found"))
            }
            
            android.util.Log.d("EnterpriseAdminVM", "Found existing user: ${existingUser.displayName}")
            
            // Update the user
            val result = repository.updateUser(user)
            
            result.fold(
                onSuccess = {
                    android.util.Log.d("EnterpriseAdminVM", "✅ User updated successfully")
                    _successMessage.value = "User updated successfully!"
                    
                    // Update local state
                    _users.value = _users.value.map { u ->
                        if (u.uid == user.uid) user else u
                    }
                    
                    // Also reload from backend to ensure consistency
                    _enterprise.value?.id?.let { enterpriseId ->
                        android.util.Log.d("EnterpriseAdminVM", "Reloading users list...")
                        val reloadResult = repository.getEnterpriseUsers(enterpriseId)
                        reloadResult.fold(
                            onSuccess = { freshUsers ->
                                android.util.Log.d("EnterpriseAdminVM", "✅ Users reloaded: ${freshUsers.size} users")
                                _users.value = freshUsers
                            },
                            onFailure = { error ->
                                android.util.Log.w("EnterpriseAdminVM", "⚠️ Failed to reload users: ${error.message}")
                            }
                        )
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("EnterpriseAdminVM", "❌ Failed to update user: ${error.message}", error)
                    _errorMessage.value = "Failed to update user: ${error.message}"
                }
            )
            
            result
        } catch (e: Exception) {
            android.util.Log.e("EnterpriseAdminVM", "❌ Error updating user: ${e.message}", e)
            _errorMessage.value = "Error updating user: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun deleteUser(userId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        
        val result = repository.deleteUser(userId)
        
        result.fold(
            onSuccess = {
                _successMessage.value = "User deleted successfully!"
                // Reload users and enterprise to reflect changes
                _enterprise.value?.id?.let { enterpriseId ->
                    // Force reload both users and enterprise data
                    val usersResult = repository.getEnterpriseUsers(enterpriseId)
                    val enterpriseResult = repository.getEnterpriseById(enterpriseId)
                    
                    usersResult.fold(
                        onSuccess = { freshUsers ->
                            _users.value = freshUsers
                        },
                        onFailure = {
                            android.util.Log.w("EnterpriseAdminVM", "Failed to reload users: ${it.message}")
                        }
                    )
                    
                    enterpriseResult.fold(
                        onSuccess = { freshEnterprise ->
                            freshEnterprise?.let { enterprise ->
                                _enterprise.value = enterprise
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.w("EnterpriseAdminVM", "Failed to reload enterprise: ${error.message}")
                        }
                    )
                }
            },
            onFailure = {
                _errorMessage.value = "Failed to delete user: ${it.message}"
            }
        )
        
        _isLoading.value = false
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    // Search functions
    fun updateUserSearchQuery(query: String) {
        _userSearchQuery.value = query
    }
    
    // Permission Management Functions
    
    /**
     * Create invitation with permissions
     */
    @Suppress("UNUSED_PARAMETER")
    fun createInvitationWithPermissions(
        email: String,
        displayName: String,
        department: String,
        jobTitle: String,
        canViewDepartmentAssessments: Boolean,
        canViewAllAssessments: Boolean,
        canAccessFda483Analysis: Boolean,
        createdBy: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                // Check if enterprise is loaded
                val enterprise = _enterprise.value
                if (enterprise == null) {
                    android.util.Log.e("EnterpriseAdminVM", "Enterprise not found - _enterprise.value is null")
                    _errorMessage.value = "Failed to create user: Enterprise not found"
                    _isLoading.value = false
                    return@launch
                }
                
                android.util.Log.d("EnterpriseAdminVM", "Creating user for enterprise: ${enterprise.companyName} (${enterprise.id})")
                android.util.Log.d("EnterpriseAdminVM", "User email: $email")
                
                // Create permissions object with simplified permissions
                val permissions = UserPermission(
                    userId = "", // Will be set when user is created
                    enterpriseId = enterprise.id,
                    department = department,
                    canCreateAssessments = true, // Always enabled
                    canViewOwnAssessments = true, // Always enabled
                    canViewDepartmentAssessments = canViewDepartmentAssessments,
                    canViewAllAssessments = canViewAllAssessments,
                    canAccessFda483Analysis = canAccessFda483Analysis,
                    createdBy = createdBy
                )
                
                android.util.Log.d("EnterpriseAdminVM", "Created permissions object")
                
                // Generate temporary password
                val tempPassword = generateTemporaryPassword()
                android.util.Log.d("EnterpriseAdminVM", "Generated temporary password")
                
                // Create the user account directly in Firebase Auth
                val userResult = repository.createEnterpriseUser(
                    email = email,
                    password = tempPassword,
                    displayName = displayName,
                    enterpriseId = enterprise.id,
                    department = department,
                    jobTitle = jobTitle
                )
                
                userResult.fold(
                    onSuccess = { user ->
                        android.util.Log.d("EnterpriseAdminVM", "User created successfully: ${user.uid}")
                        
                        // Set the userId in permissions
                        val permissionsWithUserId = permissions.copy(userId = user.uid)
                        
                        // Save user permissions
                        viewModelScope.launch {
                            val permissionResult = permissionRepository.createUserPermission(permissionsWithUserId)
                            permissionResult.fold(
                                onSuccess = {
                                    android.util.Log.d("EnterpriseAdminVM", "Permissions created successfully")
                                    
                                    // Send credentials email
                                    viewModelScope.launch {
                                        val emailService = com.pramod.validator.services.EmailService()
                                        emailService.sendCredentialsEmail(
                                            toEmail = email,
                                            recipientName = displayName,
                                            tempPassword = tempPassword,
                                            enterpriseName = enterprise.companyName
                                        )
                                    }
                                    
                                    _successMessage.value = "User created successfully! Login credentials have been sent to $email"
                                    loadUsers(enterprise.id)
                                },
                                onFailure = { error ->
                                    android.util.Log.e("EnterpriseAdminVM", "Failed to create permissions: ${error.message}")
                                    _errorMessage.value = "User created but failed to set permissions: ${error.message}"
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("EnterpriseAdminVM", "Failed to create user: ${error.message}")
                        _errorMessage.value = "Failed to create user: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EnterpriseAdminVM", "Exception in createInvitationWithPermissions: ${e.message}", e)
                _errorMessage.value = "Error creating invitation: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    private fun getInvitationLink(token: String): String {
        // In a real app, this would be your app's deep link or web URL
        return "https://yourapp.com/invite?token=$token"
    }
    
    /**
     * Generate a secure temporary password
     */
    private fun generateTemporaryPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
    }

    
    /**
     * Update user permissions
     */
    fun updateUserPermissions(
        userId: String,
        department: String,
        canViewDepartmentAssessments: Boolean,
        canViewAllAssessments: Boolean,
        canAccessFda483Analysis: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                // First get existing permission
                val permissionResult = permissionRepository.getUserPermissions(userId)
                permissionResult.fold(
                    onSuccess = { existingPermission ->
                        if (existingPermission != null) {
                            // Update existing permission with simplified fields
                            val updatedPermission = existingPermission.copy(
                                department = department,
                                canCreateAssessments = true, // Always enabled
                                canViewOwnAssessments = true, // Always enabled
                                canViewDepartmentAssessments = canViewDepartmentAssessments,
                                canViewAllAssessments = canViewAllAssessments,
                                canAccessFda483Analysis = canAccessFda483Analysis,
                                updatedAt = System.currentTimeMillis()
                            )
                            
                            val updateResult = permissionRepository.updateUserPermission(updatedPermission)
                            updateResult.fold(
                                onSuccess = {
                                    _successMessage.value = "User permissions updated successfully"
                                },
                                onFailure = { error ->
                                    _errorMessage.value = "Failed to update permissions: ${error.message}"
                                }
                            )
                        } else {
                            // Create new permission with simplified fields
                            val newPermission = UserPermission(
                                userId = userId,
                                enterpriseId = _enterprise.value?.id ?: "",
                                department = department,
                                canCreateAssessments = true, // Always enabled
                                canViewOwnAssessments = true, // Always enabled
                                canViewDepartmentAssessments = canViewDepartmentAssessments,
                                canViewAllAssessments = canViewAllAssessments,
                                canAccessFda483Analysis = canAccessFda483Analysis,
                                createdBy = _enterprise.value?.adminUid ?: ""
                            )
                            
                            val createResult = permissionRepository.createUserPermission(newPermission)
                            createResult.fold(
                                onSuccess = {
                                    _successMessage.value = "User permissions created successfully"
                                },
                                onFailure = { error ->
                                    _errorMessage.value = "Failed to create permissions: ${error.message}"
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to get user permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error updating permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Get user permissions
     */
    suspend fun getUserPermissions(userId: String): UserPermission? {
        return try {
            val result = permissionRepository.getUserPermissions(userId)
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get default domains for department type
     */
    fun getDefaultDomainsForDepartment(departmentType: DepartmentType): List<String> {
        return permissionRepository.getDefaultDomainsForDepartment(departmentType)
    }
    
    /**
     * Get all department types
     */
    fun getDepartmentTypes(): List<DepartmentType> {
        return DepartmentType.values().toList()
    }
    
    /**
     * Get all available domains
     */
    fun getAvailableDomains(): List<String> {
        return listOf("qu_", "pl_", "pr_", "mt_", "lab_", "fe_")
    }

    // Facility Management Functions

    /**
     * Load facilities for the enterprise
     */
    fun loadFacilities() {
        viewModelScope.launch {
            android.util.Log.d("EnterpriseAdminVM", "=== LOADING FACILITIES ===")
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val enterpriseId = _enterprise.value?.id
                android.util.Log.d("EnterpriseAdminVM", "Enterprise object: ${_enterprise.value}")
                android.util.Log.d("EnterpriseAdminVM", "Enterprise ID from object: $enterpriseId")
                
                if (enterpriseId != null) {
                    android.util.Log.d("EnterpriseAdminVM", "Querying facilities for enterpriseId: $enterpriseId")
                    val result = facilityRepository.getFacilitiesByEnterprise(enterpriseId)
                    result.fold(
                        onSuccess = { facilities ->
                            android.util.Log.d("EnterpriseAdminVM", "✅ Got ${facilities.size} facilities from repository")
                            facilities.forEach { f ->
                                android.util.Log.d("EnterpriseAdminVM", "  - Facility: ${f.name}, enterpriseId: ${f.enterpriseId}, isActive: ${f.isActive}")
                            }
                            _facilities.value = facilities
                            android.util.Log.d("EnterpriseAdminVM", "✅ Facilities state updated: ${_facilities.value.size} facilities")
                        },
                        onFailure = { error ->
                            android.util.Log.e("EnterpriseAdminVM", "❌ Failed to load facilities: ${error.message}", error)
                            _errorMessage.value = "Failed to load facilities: ${error.message}"
                        }
                    )
                } else {
                    android.util.Log.e("EnterpriseAdminVM", "❌ Enterprise ID is NULL - cannot load facilities")
                    _errorMessage.value = "Enterprise ID not found"
                }
            } catch (e: Exception) {
                android.util.Log.e("EnterpriseAdminVM", "❌ Error loading facilities: ${e.message}", e)
                _errorMessage.value = "Error loading facilities: ${e.message}"
            }

            _isLoading.value = false
            android.util.Log.d("EnterpriseAdminVM", "=== LOADING FACILITIES COMPLETE ===")
        }
    }

    /**
     * Create a new facility
     */
    suspend fun createFacility(
        name: String,
        description: String,
        createdBy: String
    ): Result<Unit> {
        android.util.Log.d("EnterpriseAdminVM", "Starting facility creation: $name")
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        return try {
            val enterpriseId = _enterprise.value?.id
            android.util.Log.d("EnterpriseAdminVM", "Creating facility for enterprise: $enterpriseId")
            android.util.Log.d("EnterpriseAdminVM", "Enterprise object: ${_enterprise.value}")
            
            if (enterpriseId == null) {
                android.util.Log.e("EnterpriseAdminVM", "❌ Enterprise not found")
                _errorMessage.value = "Enterprise not found"
                return Result.failure(Exception("Enterprise not found"))
            }

            val facility = Facility(
                name = name,
                description = description,
                enterpriseId = enterpriseId,
                createdBy = createdBy,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            android.util.Log.d("EnterpriseAdminVM", "Facility object before saving: $facility")

            val result = facilityRepository.createFacility(facility)
            result.onSuccess { newFacility ->
                android.util.Log.d("EnterpriseAdminVM", "✅ Facility created successfully: ${newFacility.name}")
                _successMessage.value = "Facility created successfully"
                
                // Update local state
                _facilities.value = _facilities.value + newFacility
            }.onFailure { error ->
                android.util.Log.e("EnterpriseAdminVM", "❌ Failed to create facility: ${error.message}", error)
                _errorMessage.value = "Failed to create facility: ${error.message}"
            }
            
            result.map { Unit }
        } catch (e: Exception) {
            android.util.Log.e("EnterpriseAdminVM", "❌ Error creating facility: ${e.message}", e)
            _errorMessage.value = "Error creating facility: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update an existing facility
     */
    suspend fun updateFacility(
        facilityId: String,
        name: String,
        description: String,
        updatedBy: String
    ): Result<Unit> {
        android.util.Log.d("EnterpriseAdminVM", "Starting facility update for ID: $facilityId")
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        return try {
            val existingFacility = _facilities.value.find { it.id == facilityId }
            if (existingFacility == null) {
                android.util.Log.e("EnterpriseAdminVM", "❌ Facility not found: $facilityId")
                _errorMessage.value = "Facility not found"
                _isLoading.value = false
                return Result.failure(Exception("Facility not found"))
            }

            android.util.Log.d("EnterpriseAdminVM", "Found existing facility: ${existingFacility.name}")
            
            // Keep all existing fields and only update what changed
            val updatedFacility = existingFacility.copy(
                name = name,
                description = description,
                updatedBy = updatedBy,
                updatedAt = System.currentTimeMillis(),
                // Preserve all other fields
                id = existingFacility.id,
                enterpriseId = existingFacility.enterpriseId,
                createdBy = existingFacility.createdBy,
                createdAt = existingFacility.createdAt,
                isActive = existingFacility.isActive
            )

            android.util.Log.d("EnterpriseAdminVM", "Updating facility with data: $updatedFacility")
            val result = facilityRepository.updateFacility(updatedFacility)
            
            result.fold(
                onSuccess = {
                    android.util.Log.d("EnterpriseAdminVM", "✅ Facility updated successfully")
                    // Update local state
                    _facilities.value = _facilities.value.map { facility ->
                        if (facility.id == facilityId) updatedFacility else facility
                    }
                    _successMessage.value = "Facility updated successfully"
                },
                onFailure = { error ->
                    android.util.Log.e("EnterpriseAdminVM", "❌ Failed to update facility: ${error.message}", error)
                    _errorMessage.value = "Failed to update facility: ${error.message}"
                }
            )
            
            result
        } catch (e: Exception) {
            android.util.Log.e("EnterpriseAdminVM", "❌ Error updating facility: ${e.message}", e)
            _errorMessage.value = "Error updating facility: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Delete a facility
     */
    fun deleteFacility(facilityId: String, deletedBy: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val facility = _facilities.value.find { it.id == facilityId }
                if (facility != null) {
                    // First perform the delete operation (returns affected assessments count)
                    val result = facilityRepository.deleteFacility(facilityId)
                    result.fold(
                        onSuccess = { affectedCount ->
                            // Update local state only after successful deletion
                            _facilities.value = _facilities.value.filter { it.id != facilityId }
                            val message = if (affectedCount > 0) {
                                "Facility deleted successfully. $affectedCount assessment(s) were associated with this facility."
                            } else {
                                "Facility deleted successfully"
                            }
                            _successMessage.value = message
                        },
                        onFailure = { error ->
                            _errorMessage.value = "Failed to delete facility: ${error.message}"
                        }
                    )
                } else {
                    _errorMessage.value = "Facility not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting facility: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    /**
     * Get facility name by ID
     */
    suspend fun getFacilityName(facilityId: String): String {
        return try {
            val result = facilityRepository.getFacilityName(facilityId)
            result.getOrNull() ?: "Unknown Facility"
        } catch (e: Exception) {
            "Unknown Facility"
        }
    }

    /**
     * Load user permissions for a specific user
     */
    fun loadUserPermissions(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = permissionRepository.getUserPermissions(userId)
                result.fold(
                    onSuccess = { permission ->
                        android.util.Log.d("EnterpriseAdminVM", "✅ Loaded permissions for user: $userId")
                        // Update user with permissions
                        val updatedUsers = _users.value.map { user ->
                            if (user.uid == userId) {
                                user.copy(permissions = permission)
                            } else {
                                user
                            }
                        }
                        _users.value = updatedUsers
                    },
                    onFailure = { error ->
                        android.util.Log.e("EnterpriseAdminVM", "❌ Failed to load permissions: ${error.message}", error)
                        _errorMessage.value = "Failed to load permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EnterpriseAdminVM", "❌ Error loading permissions: ${e.message}", e)
                _errorMessage.value = "Error loading permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Update user permissions
     */
    fun updateUserPermissions(permission: UserPermission) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = permissionRepository.updateUserPermission(permission)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("EnterpriseAdminVM", "✅ Updated permissions for user: ${permission.userId}")
                        _successMessage.value = "Permissions updated successfully"
                        
                        // Update local user list
                        val updatedUsers = _users.value.map { user ->
                            if (user.uid == permission.userId) {
                                user.copy(permissions = permission)
                            } else {
                                user
                            }
                        }
                        _users.value = updatedUsers
                    },
                    onFailure = { error ->
                        android.util.Log.e("EnterpriseAdminVM", "❌ Failed to update permissions: ${error.message}", error)
                        _errorMessage.value = "Failed to update permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EnterpriseAdminVM", "❌ Error updating permissions: ${e.message}", e)
                _errorMessage.value = "Error updating permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Create user with enhanced permissions (Simplified)
     */
    fun createUserWithEnhancedPermissions(
        email: String,
        password: String,
        displayName: String,
        department: String,
        jobTitle: String,
        canViewDepartmentAssessments: Boolean,
        canViewAllAssessments: Boolean,
        canAccessFda483Analysis: Boolean,
        createdBy: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                // First create the enterprise user
                val userResult = repository.createEnterpriseUser(
                    email = email,
                    password = password,
                    displayName = displayName,
                    enterpriseId = _enterprise.value?.id ?: "",
                    department = department,
                    jobTitle = jobTitle
                )
                
                userResult.fold(
                    onSuccess = { user ->
                        // Create simplified permissions for the user
                        val permission = UserPermission(
                            userId = user.uid,
                            enterpriseId = _enterprise.value?.id ?: "",
                            department = department,
                            canCreateAssessments = true, // Always enabled
                            canViewOwnAssessments = true, // Always enabled
                            canViewDepartmentAssessments = canViewDepartmentAssessments,
                            canViewAllAssessments = canViewAllAssessments,
                            canAccessFda483Analysis = canAccessFda483Analysis,
                            createdBy = createdBy
                        )
                        
                        val permissionResult = permissionRepository.createUserPermission(permission)
                        permissionResult.fold(
                            onSuccess = {
                                android.util.Log.d("EnterpriseAdminVM", "✅ Created user with enhanced permissions: ${user.displayName}")
                                _successMessage.value = "User created with enhanced permissions successfully"
                                
                                // Update enterprise user count
                                _enterprise.value?.let { enterprise ->
                                    val updatedEnterprise = enterprise.copy(
                                        currentUserCount = enterprise.currentUserCount + 1
                                    )
                                    repository.updateEnterprise(updatedEnterprise)
                                    _enterprise.value = updatedEnterprise
                                }
                                
                                // Reload users to include the new user
                                loadUsers(_enterprise.value?.id ?: "")
                            },
                            onFailure = { error ->
                                android.util.Log.e("EnterpriseAdminVM", "❌ Failed to create permissions: ${error.message}", error)
                                _errorMessage.value = "User created but failed to set permissions: ${error.message}"
                            }
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("EnterpriseAdminVM", "❌ Failed to create user: ${error.message}", error)
                        _errorMessage.value = "Failed to create user: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EnterpriseAdminVM", "❌ Error creating user with enhanced permissions: ${e.message}", e)
                _errorMessage.value = "Error creating user: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Load invitations for the enterprise
     */
    fun loadInvitations(enterpriseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = invitationRepository.getInvitationsByEnterprise(enterpriseId)
                result.fold(
                    onSuccess = { invitations ->
                        _invitations.value = invitations
                    },
                    onFailure = { error ->
                        android.util.Log.e("EnterpriseAdminVM", "Failed to load invitations: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EnterpriseAdminVM", "Error loading invitations: ${e.message}")
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Delete an invitation
     */
    fun deleteInvitation(invitationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = invitationRepository.deleteInvitation(invitationId)
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Invitation deleted successfully"
                        loadInvitations(_enterprise.value?.id ?: "")
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to delete invitation: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting invitation: ${e.message}"
            }
            _isLoading.value = false
        }
    }
}

