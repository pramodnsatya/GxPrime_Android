package com.pramod.validator.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.DomainData
import com.pramod.validator.data.models.SubDomain
import com.pramod.validator.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubDomainViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _subDomains = MutableStateFlow<List<SubDomain>>(emptyList())
    val subDomains: StateFlow<List<SubDomain>> = _subDomains.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadSubDomains(domainId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Load from Firebase first
                val result = repository.getSubDomains(domainId)
                
                if (result.isSuccess) {
                    val firebaseSubDomains = result.getOrNull() ?: emptyList()
                    if (firebaseSubDomains.isNotEmpty()) {
                        Log.d("SubDomainViewModel", "✅ Loaded ${firebaseSubDomains.size} sub-domains from Firebase")
                        _subDomains.value = firebaseSubDomains
                    } else {
                        // Firebase returned empty - use local fallback
                        Log.w("SubDomainViewModel", "⚠️ Firebase returned empty sub-domains, using local fallback")
                        _subDomains.value = DomainData.getSubDomains(domainId)
                        _errorMessage.value = "Using local data. Please check Firebase setup."
                    }
                } else {
                    // Firebase error - use local fallback
                    val error = result.exceptionOrNull()
                    Log.e("SubDomainViewModel", "❌ Firebase error: ${error?.message}", error)
                    _subDomains.value = DomainData.getSubDomains(domainId)
                    _errorMessage.value = "Firebase connection issue. Using local data."
                }
            } catch (e: Exception) {
                Log.e("SubDomainViewModel", "❌ Exception loading sub-domains: ${e.message}", e)
                _subDomains.value = DomainData.getSubDomains(domainId)
                _errorMessage.value = "Error loading data. Using local fallback."
            }
            
            _isLoading.value = false
        }
    }
}


