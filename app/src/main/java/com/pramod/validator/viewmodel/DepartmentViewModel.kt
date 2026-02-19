package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.Department
import com.pramod.validator.data.repository.DepartmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DepartmentViewModel : ViewModel() {
    private val departmentRepository = DepartmentRepository()

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * Load departments for a specific enterprise
     * Ensures enterprise isolation
     */
    fun loadDepartments(enterpriseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                android.util.Log.d("DepartmentViewModel", "Loading departments for enterprise: $enterpriseId")
                val result = departmentRepository.getDepartmentsByEnterprise(enterpriseId)
                result.fold(
                    onSuccess = { departments ->
                        _departments.value = departments
                        android.util.Log.d("DepartmentViewModel", "Loaded ${departments.size} departments")
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load departments: ${error.message}"
                        android.util.Log.e("DepartmentViewModel", "Error loading departments", error)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading departments: ${e.message}"
                android.util.Log.e("DepartmentViewModel", "Exception loading departments", e)
            }

            _isLoading.value = false
        }
    }

    /**
     * Create a new department
     */
    fun createDepartment(
        name: String,
        description: String,
        enterpriseId: String,
        allowedDomains: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Validate input
                if (name.isBlank()) {
                    _errorMessage.value = "Department name is required"
                    _isLoading.value = false
                    return@launch
                }

                // Check for duplicate department name in this enterprise
                val existingDept = _departments.value.find { 
                    it.name.equals(name, ignoreCase = true) && it.enterpriseId == enterpriseId 
                }
                if (existingDept != null) {
                    _errorMessage.value = "A department with this name already exists"
                    _isLoading.value = false
                    return@launch
                }

                val department = Department(
                    name = name.trim(),
                    description = description.trim(),
                    enterpriseId = enterpriseId,
                    allowedDomains = allowedDomains,
                    isActive = true
                )

                android.util.Log.d("DepartmentViewModel", "Creating department: ${department.name} for enterprise: $enterpriseId")
                val result = departmentRepository.createDepartment(department)
                result.fold(
                    onSuccess = { createdDepartment ->
                        _departments.value = _departments.value + createdDepartment
                        _successMessage.value = "Department created successfully"
                        android.util.Log.d("DepartmentViewModel", "Department created: ${createdDepartment.id}")
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to create department: ${error.message}"
                        android.util.Log.e("DepartmentViewModel", "Error creating department", error)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error creating department: ${e.message}"
                android.util.Log.e("DepartmentViewModel", "Exception creating department", e)
            }

            _isLoading.value = false
        }
    }

    /**
     * Update an existing department
     * Cascades changes to all related entities
     */
    fun updateDepartment(department: Department) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Validate input
                if (department.name.isBlank()) {
                    _errorMessage.value = "Department name is required"
                    _isLoading.value = false
                    return@launch
                }

                // Check for duplicate department name (excluding current)
                val existingDept = _departments.value.find { 
                    it.name.equals(department.name, ignoreCase = true) && 
                    it.enterpriseId == department.enterpriseId &&
                    it.id != department.id
                }
                if (existingDept != null) {
                    _errorMessage.value = "A department with this name already exists"
                    _isLoading.value = false
                    return@launch
                }

                android.util.Log.d("DepartmentViewModel", "Updating department: ${department.name}")
                val result = departmentRepository.updateDepartment(department)
                result.fold(
                    onSuccess = {
                        _departments.value = _departments.value.map { 
                            if (it.id == department.id) department else it 
                        }
                        _successMessage.value = "Department updated successfully. All related data has been updated."
                        android.util.Log.d("DepartmentViewModel", "Department updated with cascading changes")
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to update department: ${error.message}"
                        android.util.Log.e("DepartmentViewModel", "Error updating department", error)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error updating department: ${e.message}"
                android.util.Log.e("DepartmentViewModel", "Exception updating department", e)
            }

            _isLoading.value = false
        }
    }

    /**
     * Delete a department
     * Returns count of affected assessments for warning
     */
    fun deleteDepartment(departmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val department = _departments.value.find { it.id == departmentId }
                if (department != null) {
                    android.util.Log.d("DepartmentViewModel", "Deleting department: ${department.name}")
                    val result = departmentRepository.deleteDepartment(departmentId)
                    result.fold(
                        onSuccess = { affectedCount ->
                            _departments.value = _departments.value.filter { it.id != departmentId }
                            val message = if (affectedCount > 0) {
                                "Department deleted successfully. $affectedCount assessment(s) were associated with this department."
                            } else {
                                "Department deleted successfully"
                            }
                            _successMessage.value = message
                            android.util.Log.d("DepartmentViewModel", "Department deleted: $affectedCount assessments affected")
                        },
                        onFailure = { error ->
                            _errorMessage.value = "Failed to delete department: ${error.message}"
                            android.util.Log.e("DepartmentViewModel", "Error deleting department", error)
                        }
                    )
                } else {
                    _errorMessage.value = "Department not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting department: ${e.message}"
                android.util.Log.e("DepartmentViewModel", "Exception deleting department", e)
            }

            _isLoading.value = false
        }
    }

    /**
     * Get count of assessments for a department (for delete warning)
     */
    suspend fun getAssessmentCount(enterpriseId: String, departmentName: String): Int {
        return try {
            departmentRepository.countAssessmentsByDepartment(enterpriseId, departmentName)
        } catch (e: Exception) {
            android.util.Log.e("DepartmentViewModel", "Error counting assessments", e)
            0
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}


