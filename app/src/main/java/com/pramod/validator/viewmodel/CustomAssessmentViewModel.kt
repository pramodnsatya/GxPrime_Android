package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pramod.validator.data.models.CustomAssessment
import com.pramod.validator.data.models.CustomQuestion
import com.pramod.validator.data.models.ChecklistItem
import com.pramod.validator.data.repository.CustomAssessmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CustomAssessmentViewModel : ViewModel() {
    private val repository = CustomAssessmentRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _assessments = MutableStateFlow<List<CustomAssessment>>(emptyList())
    val assessments: StateFlow<List<CustomAssessment>> = _assessments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _currentAssessment = MutableStateFlow<CustomAssessment?>(null)
    val currentAssessment: StateFlow<CustomAssessment?> = _currentAssessment.asStateFlow()
    
    private val _shouldShowChecklistTab = MutableStateFlow(false)
    val shouldShowChecklistTab: StateFlow<Boolean> = _shouldShowChecklistTab.asStateFlow()
    
    init {
        loadUserCustomAssessments()
    }
    
    fun loadUserCustomAssessments() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getUserCustomAssessments(currentUser.uid)
                result.fold(
                    onSuccess = { assessments ->
                        _assessments.value = assessments
                    },
                    onFailure = { error ->
                        android.util.Log.e("CustomAssessmentViewModel", "Error loading assessments: ${error.message}", error)
                        _errorMessage.value = "Failed to load assessments: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CustomAssessmentViewModel", "Error loading assessments: ${e.message}", e)
                _errorMessage.value = "Failed to load assessments: ${e.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun createCustomAssessment(name: String, questions: List<CustomQuestion>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        if (name.isBlank()) {
            _errorMessage.value = "Assessment name cannot be empty"
            return
        }
        
        if (questions.isEmpty()) {
            _errorMessage.value = "Assessment must have at least one question"
            return
        }
        
        val assessment = CustomAssessment(
            userId = currentUser.uid,
            name = name,
            questions = questions,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val createResult = repository.createCustomAssessment(assessment)
                createResult.fold(
                    onSuccess = { createdAssessment ->
                        _successMessage.value = "Assessment created successfully"
                        loadUserCustomAssessments()
                    },
                    onFailure = { error ->
                        android.util.Log.e("CustomAssessmentViewModel", "Error creating assessment: ${error.message}", error)
                        _errorMessage.value = "Failed to create assessment: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CustomAssessmentViewModel", "Error creating assessment: ${e.message}", e)
                _errorMessage.value = "Failed to create assessment: ${e.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun getCustomAssessmentById(assessmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getCustomAssessmentById(assessmentId)
                result.fold(
                    onSuccess = { assessment ->
                        _currentAssessment.value = assessment
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load assessment: ${error.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading assessment: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun updateCustomAssessment(assessmentId: String, name: String, questions: List<CustomQuestion>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        if (name.isBlank()) {
            _errorMessage.value = "Assessment name cannot be empty"
            return
        }
        
        if (questions.isEmpty()) {
            _errorMessage.value = "Assessment must have at least one question"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                // Get existing assessment to preserve other fields
                val existingResult = repository.getCustomAssessmentById(assessmentId)
                existingResult.fold(
                    onSuccess = { existingAssessment ->
                        if (existingAssessment == null) {
                            _errorMessage.value = "Assessment not found"
                            _isLoading.value = false
                            return@launch
                        }
                        
                        val updatedAssessment = existingAssessment.copy(
                            name = name,
                            questions = questions,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        val updateResult = repository.updateCustomAssessment(updatedAssessment)
                        updateResult.fold(
                            onSuccess = { updated ->
                                _successMessage.value = "Assessment updated successfully"
                                loadUserCustomAssessments()
                            },
                            onFailure = { error ->
                                android.util.Log.e("CustomAssessmentViewModel", "Error updating assessment: ${error.message}", error)
                                _errorMessage.value = "Failed to update assessment: ${error.message}"
                            }
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("CustomAssessmentViewModel", "Error loading assessment: ${error.message}", error)
                        _errorMessage.value = "Failed to load assessment: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CustomAssessmentViewModel", "Error updating assessment: ${e.message}", e)
                _errorMessage.value = "Failed to update assessment: ${e.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun deleteCustomAssessment(assessmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteCustomAssessment(assessmentId)
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        _successMessage.value = "Assessment deleted successfully"
                        loadUserCustomAssessments()
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        _errorMessage.value = "Failed to delete assessment: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error deleting assessment: ${e.message}"
            }
        }
    }
    
    fun createAssessmentFromChecklist(
        checklistItems: List<ChecklistItem>,
        fda483AssessmentId: String,
        assessmentName: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        if (checklistItems.isEmpty()) {
            _errorMessage.value = "Checklist is empty"
            return
        }
        
        if (assessmentName.isBlank()) {
            _errorMessage.value = "Assessment name cannot be empty"
            return
        }
        
        // Convert checklist items to questions
        val questions = checklistItems.mapIndexed { index, checklistItem ->
            CustomQuestion(
                id = UUID.randomUUID().toString(),
                questionText = checklistItem.item,
                order = index + 1
            )
        }
        
        val assessment = CustomAssessment(
            userId = currentUser.uid,
            name = assessmentName,
            description = "Assessment created from FDA 483 checklist",
            questions = questions,
            isFromChecklist = true,
            sourceFda483AssessmentId = fda483AssessmentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val createResult = repository.createCustomAssessment(assessment)
                createResult.fold(
                    onSuccess = { createdAssessment ->
                        _successMessage.value = "Assessment created successfully from checklist"
                        _shouldShowChecklistTab.value = true // Set flag to show FDA 483 Checklist tab
                        loadUserCustomAssessments()
                    },
                    onFailure = { error ->
                        android.util.Log.e("CustomAssessmentViewModel", "Error creating checklist assessment: ${error.message}", error)
                        _errorMessage.value = "Failed to create assessment: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CustomAssessmentViewModel", "Error creating checklist assessment: ${e.message}", e)
                _errorMessage.value = "Failed to create assessment: ${e.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun getChecklistAssessments(): List<CustomAssessment> {
        return _assessments.value.filter { it.isFromChecklist }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    fun setShouldShowChecklistTab(show: Boolean = true) {
        _shouldShowChecklistTab.value = show
    }
    
    fun clearShouldShowChecklistTab() {
        _shouldShowChecklistTab.value = false
    }
    
    // Check if an assessment is from checklist by ID
    suspend fun isAssessmentFromChecklist(assessmentId: String): Boolean {
        return try {
            val result = repository.getCustomAssessmentById(assessmentId)
            result.fold(
                onSuccess = { assessment -> assessment?.isFromChecklist == true },
                onFailure = { false }
            )
        } catch (e: Exception) {
            false
        }
    }
}

