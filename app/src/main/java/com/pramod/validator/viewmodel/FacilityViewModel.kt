package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.Facility
import com.pramod.validator.data.repository.FacilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FacilityViewModel : ViewModel() {
    private val facilityRepository = FacilityRepository()

    private val _facilities = MutableStateFlow<List<Facility>>(emptyList())
    val facilities: StateFlow<List<Facility>> = _facilities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * Load facilities for a specific enterprise
     */
    fun loadFacilities(enterpriseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = facilityRepository.getFacilitiesByEnterprise(enterpriseId)
                result.fold(
                    onSuccess = { facilities ->
                        _facilities.value = facilities
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load facilities: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading facilities: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    /**
     * Create a new facility
     */
    fun createFacility(
        name: String,
        description: String,
        enterpriseId: String,
        createdBy: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val facility = Facility(
                    name = name,
                    description = description,
                    enterpriseId = enterpriseId,
                    createdBy = createdBy
                )

                val result = facilityRepository.createFacility(facility)
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Facility created successfully"
                        loadFacilities(enterpriseId) // Reload the list
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to create facility: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error creating facility: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    /**
     * Update an existing facility
     */
    fun updateFacility(
        facilityId: String,
        name: String,
        description: String,
        updatedBy: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val existingFacility = _facilities.value.find { it.id == facilityId }
                if (existingFacility != null) {
                    val updatedFacility = existingFacility.copy(
                        name = name,
                        description = description,
                        updatedBy = updatedBy
                    )

                    val result = facilityRepository.updateFacility(updatedFacility)
                    result.fold(
                        onSuccess = {
                            _successMessage.value = "Facility updated successfully"
                            loadFacilities(existingFacility.enterpriseId) // Reload the list
                        },
                        onFailure = { error ->
                            _errorMessage.value = "Failed to update facility: ${error.message}"
                        }
                    )
                } else {
                    _errorMessage.value = "Facility not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating facility: ${e.message}"
            }

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
                    val result = facilityRepository.deleteFacility(facilityId)
                    result.fold(
                        onSuccess = { affectedCount ->
                            val message = if (affectedCount > 0) {
                                "Facility deleted successfully. $affectedCount assessment(s) were associated with this facility."
                            } else {
                                "Facility deleted successfully"
                            }
                            _successMessage.value = message
                            loadFacilities(facility.enterpriseId) // Reload the list
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
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
