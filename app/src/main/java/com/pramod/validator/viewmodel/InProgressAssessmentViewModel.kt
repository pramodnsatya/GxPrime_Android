package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pramod.validator.data.models.InProgressAssessment
import com.pramod.validator.data.repository.InProgressAssessmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InProgressAssessmentViewModel : ViewModel() {
    private val repository = InProgressAssessmentRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _assessments = MutableStateFlow<List<InProgressAssessment>>(emptyList())
    val assessments: StateFlow<List<InProgressAssessment>> = _assessments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Temporary storage for assessment being resumed
    private val _assessmentToRestore = MutableStateFlow<InProgressAssessment?>(null)
    val assessmentToRestore: StateFlow<InProgressAssessment?> = _assessmentToRestore.asStateFlow()
    
    // Store the ID of the last saved assessment for auto-save
    private val _lastSavedAssessmentId = MutableStateFlow<String?>(null)
    val lastSavedAssessmentId: StateFlow<String?> = _lastSavedAssessmentId.asStateFlow()
    
    init {
        loadUserInProgressAssessments()
    }
    
    fun setAssessmentToRestore(assessment: InProgressAssessment?) {
        _assessmentToRestore.value = assessment
    }
    
    fun clearAssessmentToRestore() {
        _assessmentToRestore.value = null
    }
    
    fun loadUserInProgressAssessments() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.getUserInProgressAssessments(currentUser.uid)
                result.fold(
                    onSuccess = { assessments ->
                        android.util.Log.d("InProgressAssessmentViewModel", "✅ Loaded ${assessments.size} in-progress assessments")
                        _assessments.value = assessments
                    },
                    onFailure = { error ->
                        android.util.Log.e("InProgressAssessmentViewModel", "❌ Error loading assessments: ${error.message}", error)
                        com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "load_in_progress_assessments")
                        com.pramod.validator.utils.CrashReporting.logException(error, "Failed to load in-progress assessments")
                        _errorMessage.value = "Couldn't load your saved assessments. Please try again."
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("InProgressAssessmentViewModel", "Error loading assessments: ${e.message}", e)
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "load_in_progress_assessments")
                com.pramod.validator.utils.CrashReporting.logException(e, "Exception loading in-progress assessments")
                _errorMessage.value = "Couldn't load your saved assessments. Please try again."
            }
            
            _isLoading.value = false
        }
    }
    
    fun saveInProgressAssessment(assessment: InProgressAssessment) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val assessmentWithUserId = assessment.copy(userId = currentUser.uid)
                
                val result = if (assessmentWithUserId.id.isNotEmpty()) {
                    // If ID is provided, fetch existing to preserve createdAt, then update
                    android.util.Log.d("InProgressAssessmentViewModel", "🔄 Updating existing assessment with ID: ${assessmentWithUserId.id}")
                    val existingResult = repository.getInProgressAssessmentById(assessmentWithUserId.id)
                    existingResult.fold(
                        onSuccess = { existing ->
                            if (existing != null) {
                                val updated = assessmentWithUserId.copy(createdAt = existing.createdAt)
                                repository.updateInProgressAssessment(updated)
                            } else {
                                // If not found, create new
                                repository.saveInProgressAssessment(assessmentWithUserId)
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("InProgressAssessmentViewModel", "Error fetching existing assessment: ${error.message}", error)
                            repository.updateInProgressAssessment(assessmentWithUserId)
                        }
                    )
                } else {
                    // Check if an existing assessment with the same criteria exists
                    val existingResult = repository.findExistingAssessment(
                        userId = currentUser.uid,
                        assessmentName = assessmentWithUserId.assessmentName,
                        facilityId = assessmentWithUserId.facilityId,
                        subDomainId = assessmentWithUserId.subDomainId,
                        isCustomAssessment = assessmentWithUserId.isCustomAssessment
                    )
                    
                    existingResult.fold(
                        onSuccess = { existingAssessment ->
                            if (existingAssessment != null) {
                                // Update existing assessment
                                android.util.Log.d("InProgressAssessmentViewModel", "🔄 Updating existing assessment: ${existingAssessment.id}")
                                val updatedAssessment = assessmentWithUserId.copy(
                                    id = existingAssessment.id,
                                    createdAt = existingAssessment.createdAt // Preserve original creation time
                                )
                                repository.updateInProgressAssessment(updatedAssessment)
                            } else {
                                // Create new assessment
                                android.util.Log.d("InProgressAssessmentViewModel", "➕ Creating new assessment")
                                repository.saveInProgressAssessment(assessmentWithUserId)
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("InProgressAssessmentViewModel", "Error finding existing assessment: ${error.message}", error)
                            // If finding fails, just create a new one
                            repository.saveInProgressAssessment(assessmentWithUserId)
                        }
                    )
                }
                
                result.fold(
                    onSuccess = { savedAssessment ->
                        android.util.Log.d("InProgressAssessmentViewModel", "✅ Assessment saved successfully: ${savedAssessment.id}")
                        android.util.Log.d("InProgressAssessmentViewModel", "   - Responses: ${savedAssessment.responses.size}")
                        android.util.Log.d("InProgressAssessmentViewModel", "   - Current index: ${savedAssessment.currentQuestionIndex}")
                        _successMessage.value = "Progress saved successfully"
                        _lastSavedAssessmentId.value = savedAssessment.id // Store ID for auto-save
                        // Reload list after a short delay to ensure Firestore has updated
                        kotlinx.coroutines.delay(300)
                        loadUserInProgressAssessments() // Reload list
                    },
                    onFailure = { error ->
                        android.util.Log.e("InProgressAssessmentViewModel", "❌ Error saving assessment: ${error.message}", error)
                        com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "save_in_progress_assessment")
                        com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", assessmentWithUserId.id.takeIf { it.isNotEmpty() } ?: "new")
                        com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_name", assessmentWithUserId.assessmentName)
                        com.pramod.validator.utils.CrashReporting.logException(error, "Failed to save in-progress assessment")
                        _errorMessage.value = "Couldn't save your progress. Please check your connection and try again."
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("InProgressAssessmentViewModel", "Error saving assessment: ${e.message}", e)
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "save_in_progress_assessment")
                com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", assessment.id.takeIf { it.isNotEmpty() } ?: "new")
                com.pramod.validator.utils.CrashReporting.logException(e, "Exception saving in-progress assessment")
                _errorMessage.value = "Couldn't save your progress. Please check your connection and try again."
            }
            
            _isLoading.value = false
        }
    }
    
    fun deleteInProgressAssessment(assessmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = repository.deleteInProgressAssessment(assessmentId)
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Assessment deleted successfully"
                        loadUserInProgressAssessments() // Reload list
                    },
                    onFailure = { error ->
                        android.util.Log.e("InProgressAssessmentViewModel", "Error deleting assessment: ${error.message}", error)
                        _errorMessage.value = "Failed to delete assessment: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("InProgressAssessmentViewModel", "Error deleting assessment: ${e.message}", e)
                _errorMessage.value = "Failed to delete assessment: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    fun updateInProgressAssessment(assessment: InProgressAssessment) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val result = repository.updateInProgressAssessment(assessment)
                result.fold(
                    onSuccess = { updatedAssessment ->
                        _successMessage.value = "Progress updated successfully"
                        loadUserInProgressAssessments() // Reload list
                    },
                    onFailure = { error ->
                        android.util.Log.e("InProgressAssessmentViewModel", "Error updating assessment: ${error.message}", error)
                        _errorMessage.value = "Failed to update progress: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("InProgressAssessmentViewModel", "Error updating assessment: ${e.message}", e)
                _errorMessage.value = "Failed to update progress: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    fun getInProgressAssessmentById(assessmentId: String, onResult: (InProgressAssessment?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.getInProgressAssessmentById(assessmentId)
                result.fold(
                    onSuccess = { assessment ->
                        onResult(assessment)
                    },
                    onFailure = { error ->
                        android.util.Log.e("InProgressAssessmentViewModel", "Error getting assessment: ${error.message}", error)
                        onResult(null)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("InProgressAssessmentViewModel", "Error getting assessment: ${e.message}", e)
                onResult(null)
            }
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

