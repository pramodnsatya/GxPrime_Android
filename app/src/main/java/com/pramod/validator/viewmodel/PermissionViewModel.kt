package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.data.models.Department
import com.pramod.validator.data.models.DepartmentType
import com.pramod.validator.data.repository.PermissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionViewModel : ViewModel() {
    private val permissionRepository = PermissionRepository()
    
    private val _permissions = MutableStateFlow<List<UserPermission>>(emptyList())
    val permissions: StateFlow<List<UserPermission>> = _permissions.asStateFlow()
    
    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    /**
     * Load permissions for an enterprise
     */
    fun loadEnterprisePermissions(enterpriseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = permissionRepository.getEnterprisePermissions(enterpriseId)
                result.fold(
                    onSuccess = { permissions ->
                        _permissions.value = permissions
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Load departments for an enterprise
     */
    fun loadEnterpriseDepartments(enterpriseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = permissionRepository.getEnterpriseDepartments(enterpriseId)
                result.fold(
                    onSuccess = { departments ->
                        _departments.value = departments
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load departments: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading departments: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Create user permissions
     */
    fun createUserPermission(permission: UserPermission) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = permissionRepository.createUserPermission(permission)
                result.fold(
                    onSuccess = { createdPermission ->
                        _successMessage.value = "Permissions created successfully"
                        // Reload permissions
                        permission.enterpriseId.let { enterpriseId ->
                            loadEnterprisePermissions(enterpriseId)
                        }
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to create permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error creating permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Update user permissions
     */
    fun updateUserPermission(permission: UserPermission) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = permissionRepository.updateUserPermission(permission)
                result.fold(
                    onSuccess = { updatedPermission ->
                        _successMessage.value = "Permissions updated successfully"
                        // Reload permissions
                        permission.enterpriseId.let { enterpriseId ->
                            loadEnterprisePermissions(enterpriseId)
                        }
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to update permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error updating permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Delete user permissions
     */
    fun deleteUserPermission(permissionId: String, enterpriseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = permissionRepository.deleteUserPermission(permissionId)
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Permissions deleted successfully"
                        loadEnterprisePermissions(enterpriseId)
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to delete permissions: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting permissions: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Create department
     */
    fun createDepartment(department: Department) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = permissionRepository.createDepartment(department)
                result.fold(
                    onSuccess = { createdDepartment ->
                        _successMessage.value = "Department created successfully"
                        // Reload departments
                        department.enterpriseId.let { enterpriseId ->
                            loadEnterpriseDepartments(enterpriseId)
                        }
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to create department: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error creating department: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Get default domains for department type
     */
    fun getDefaultDomainsForDepartment(departmentType: DepartmentType): List<String> {
        return permissionRepository.getDefaultDomainsForDepartment(departmentType)
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    /**
     * Get permission by user ID
     */
    suspend fun getUserPermission(userId: String): UserPermission? {
        return try {
            val result = permissionRepository.getUserPermissions(userId)
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if user has domain access
     */
    suspend fun hasDomainAccess(userId: String, domainId: String): Boolean {
        return try {
            val result = permissionRepository.hasDomainAccess(userId, domainId)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if user can create assessments
     */
    suspend fun canCreateAssessments(userId: String): Boolean {
        return try {
            val result = permissionRepository.canCreateAssessments(userId)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if user can view department assessments
     */
    suspend fun canViewDepartmentAssessments(userId: String): Boolean {
        return try {
            val result = permissionRepository.canViewDepartmentAssessments(userId)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get filtered domains for user
     */
    suspend fun getFilteredDomainsForUser(userId: String): List<String> {
        return try {
            val result = permissionRepository.getFilteredDomainsForUser(userId)
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
