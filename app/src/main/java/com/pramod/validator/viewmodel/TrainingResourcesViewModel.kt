package com.pramod.validator.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.TrainingResource
import com.pramod.validator.data.models.TrainingResourceType
import com.pramod.validator.data.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingResourceFormState(
    val resourceId: String = "",
    val title: String = "",
    val description: String = "",
    val type: TrainingResourceType = TrainingResourceType.ARTICLE,
    val linkUrl: String = "",
    val selectedFileName: String? = null,
    val selectedFileMimeType: String? = null,
    val selectedFileUri: Uri? = null,
    val existingResourceUrl: String = "",
    val existingStoragePath: String = "",
    val createdAt: Long = 0L,
    val createdBy: String = "",
    val isEditing: Boolean = false
)

class TrainingResourcesViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    
    private val _resources = MutableStateFlow<List<TrainingResource>>(emptyList())
    val resources: StateFlow<List<TrainingResource>> = _resources.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _formState = MutableStateFlow(TrainingResourceFormState())
    val formState: StateFlow<TrainingResourceFormState> = _formState.asStateFlow()
    
    private var observeJob: Job? = null
    
    init {
        observeResources()
    }
    
    private fun observeResources() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeTrainingResources().collect { resources ->
                _resources.value = resources
                _isLoading.value = false
            }
        }
    }
    
    fun refreshResources() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTrainingResources()
                .onSuccess { _resources.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Failed to load resources" }
            _isLoading.value = false
        }
    }
    
    fun updateTitle(value: String) {
        _formState.update { it.copy(title = value) }
    }
    
    fun updateDescription(value: String) {
        _formState.update { it.copy(description = value) }
    }
    
    fun updateType(type: TrainingResourceType) {
        _formState.update { current ->
            current.copy(
                type = type,
                linkUrl = "",
                selectedFileName = null,
                selectedFileMimeType = null,
                selectedFileUri = null
            )
        }
    }
    
    fun updateLinkUrl(value: String) {
        _formState.update { it.copy(linkUrl = value) }
    }
    
    fun setSelectedFile(uri: Uri?, name: String?, mimeType: String?) {
        val currentType = _formState.value.type
        
        // Validate file type for PDF and FDA483 - only accept PDFs
        if (uri != null && (currentType == TrainingResourceType.PDF || currentType == TrainingResourceType.FDA483)) {
            val isPdf = mimeType?.equals("application/pdf", ignoreCase = true) == true ||
                       name?.endsWith(".pdf", ignoreCase = true) == true
            
            if (!isPdf) {
                _errorMessage.value = "Only PDF files are allowed for ${currentType.name} resources"
                return
            }
        }
        
        _formState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFileName = name,
                selectedFileMimeType = mimeType
            )
        }
    }
    
    fun editResource(resource: TrainingResource) {
        _formState.value = TrainingResourceFormState(
            resourceId = resource.id,
            title = resource.title,
            description = resource.description,
            type = resource.type,
            linkUrl = when (resource.type) {
                TrainingResourceType.VIDEO,
                TrainingResourceType.ARTICLE -> resource.resourceUrl
                else -> ""
            },
            selectedFileName = resource.fileName.takeIf { it.isNotBlank() },
            selectedFileMimeType = resource.mimeType.takeIf { it.isNotBlank() },
            selectedFileUri = null,
            existingResourceUrl = resource.resourceUrl,
            existingStoragePath = resource.storagePath,
            createdAt = resource.createdAt,
            createdBy = resource.createdBy,
            isEditing = true
        )
    }
    
    fun clearForm() {
        _formState.value = TrainingResourceFormState()
    }
    
    fun saveResource(currentUserId: String?) {
        val state = _formState.value
        if (state.title.isBlank()) {
            _errorMessage.value = "Please provide a title"
            return
        }
        
        val requiresLink = state.type == TrainingResourceType.VIDEO || state.type == TrainingResourceType.ARTICLE
        if (requiresLink && state.linkUrl.isBlank()) {
            _errorMessage.value = "Please provide a valid link"
            return
        }
        
        val requiresFile = !requiresLink
        if (requiresFile && state.selectedFileUri == null && state.existingResourceUrl.isBlank()) {
            _errorMessage.value = "Please select a file to upload"
            return
        }
        
        // Validate file type for PDF and FDA483 - only accept PDFs
        if (requiresFile && state.selectedFileUri != null && 
            (state.type == TrainingResourceType.PDF || state.type == TrainingResourceType.FDA483)) {
            val isPdf = state.selectedFileMimeType?.equals("application/pdf", ignoreCase = true) == true ||
                       state.selectedFileName?.endsWith(".pdf", ignoreCase = true) == true
            
            if (!isPdf) {
                _errorMessage.value = "Only PDF files are allowed for ${state.type.name} resources"
                return
            }
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            val trimmedLink = state.linkUrl.trim()
            var resourceUrl = if (requiresLink) trimmedLink else state.existingResourceUrl
            var storagePath = state.existingStoragePath
            var fileName = state.selectedFileName ?: ""
            var mimeType = state.selectedFileMimeType ?: ""
            var shouldDeleteOldFile = false
            
            if (requiresFile && state.selectedFileUri != null) {
                val uploadResult = repository.uploadTrainingResourceFile(
                    uri = state.selectedFileUri,
                    fileName = state.selectedFileName,
                    mimeType = state.selectedFileMimeType
                )
                
                if (uploadResult.isFailure) {
                    _errorMessage.value = uploadResult.exceptionOrNull()?.message ?: "Failed to upload file"
                    _isLoading.value = false
                    return@launch
                }
                
                val uploaded = uploadResult.getOrNull()
                resourceUrl = uploaded?.downloadUrl ?: ""
                storagePath = uploaded?.storagePath ?: ""
                fileName = state.selectedFileName ?: ""
                mimeType = state.selectedFileMimeType ?: ""
                shouldDeleteOldFile = state.isEditing && state.existingStoragePath.isNotBlank()
            }
            
            if (!requiresFile) {
                storagePath = ""
                fileName = ""
                mimeType = ""
            }
            
            if (requiresFile && resourceUrl.isBlank()) {
                _errorMessage.value = "Unable to determine file URL. Please try uploading again."
                _isLoading.value = false
                return@launch
            }
            
            val resource = TrainingResource(
                id = state.resourceId,
                title = state.title.trim(),
                description = state.description.trim(),
                type = state.type,
                resourceUrl = resourceUrl,
                videoUrl = if (state.type == TrainingResourceType.VIDEO) trimmedLink else "",
                fileName = if (requiresFile) fileName else "",
                mimeType = if (requiresFile) mimeType else "",
                storagePath = storagePath,
                createdAt = if (state.isEditing && state.createdAt > 0) state.createdAt else System.currentTimeMillis(),
                createdBy = state.createdBy.ifBlank { currentUserId ?: "" },
                tags = emptyList()
            )
            
            val result = repository.saveTrainingResource(resource)
            if (result.isSuccess) {
                if (shouldDeleteOldFile) {
                    repository.deleteTrainingResourceFile(state.existingStoragePath)
                }
                _successMessage.value = if (state.isEditing) {
                    "Resource updated successfully"
                } else {
                    "Resource uploaded successfully"
                }
                clearForm()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save resource"
            }
            
            _isLoading.value = false
        }
    }
    
    fun deleteResource(resource: TrainingResource) {
        if (resource.id.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteTrainingResource(resource.id, resource.storagePath)
            if (result.isSuccess) {
                _successMessage.value = "Resource deleted successfully"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete resource"
            }
            _isLoading.value = false
        }
    }
    
    fun consumeError() {
        _errorMessage.value = null
    }
    
    fun consumeSuccess() {
        _successMessage.value = null
    }
}

