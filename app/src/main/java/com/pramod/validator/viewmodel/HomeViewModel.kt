package com.pramod.validator.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.DomainData
import com.pramod.validator.data.models.Domain
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.data.repository.PermissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val permissionRepository = PermissionRepository()

    private val _domains = MutableStateFlow<List<Domain>>(emptyList())
    val domains: StateFlow<List<Domain>> = _domains.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _completedReportsCount = MutableStateFlow(0)
    val completedReportsCount: StateFlow<Int> = _completedReportsCount.asStateFlow()

    init {
        loadDomains()
    }
    
    /**
     * Load domains with permission filtering for enterprise users
     */
    fun loadDomainsForUser(userId: String, userRole: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                if (userRole == "SUPER_ADMIN") {
                    // Super admin sees all domains
                    loadDomains()
                } else if (userRole == "ENTERPRISE_ADMIN") {
                    // Enterprise admin sees all domains
                    loadDomains()
                } else {
                    // Regular user - filter based on permissions
                    val allowedDomainsResult = permissionRepository.getFilteredDomainsForUser(userId)
                    allowedDomainsResult.fold(
                        onSuccess = { allowedDomains ->
                            if (allowedDomains.isNotEmpty()) {
                                // Filter domains based on user permissions
                                val allDomains = DomainData.getDomains()
                                val filteredDomains = allDomains.filter { domain ->
                                    val subDomainsForDomain = DomainData.getSubDomains(domain.id)
                                    subDomainsForDomain.any { subdomain ->
                                        allowedDomains.any { allowedDomain ->
                                            subdomain.id.startsWith(allowedDomain)
                                        }
                                    }
                                }
                                _domains.value = filteredDomains
                                Log.d("HomeViewModel", "✅ Filtered ${filteredDomains.size} domains for user $userId")
                            } else {
                                // No permissions set - show all domains (fallback)
                                loadDomains()
                            }
                        },
                        onFailure = { error ->
                            Log.e("HomeViewModel", "❌ Error getting user permissions: ${error.message}")
                            // Fallback to all domains
                            loadDomains()
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Exception loading domains for user: ${e.message}", e)
                _errorMessage.value = "Error loading domains: ${e.message}"
                // Fallback to all domains
                loadDomains()
            }
            
            _isLoading.value = false
        }
    }

    private fun loadDomains() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Load from Firebase first
                val result = repository.getDomains()
                
                if (result.isSuccess) {
                    val firebaseDomains = result.getOrNull() ?: emptyList()
                    if (firebaseDomains.isNotEmpty()) {
                        Log.d("HomeViewModel", "✅ Loaded ${firebaseDomains.size} domains from Firebase")
                        _domains.value = firebaseDomains
                    } else {
                        // Firebase returned empty - use local fallback
                        Log.w("HomeViewModel", "⚠️ Firebase returned empty domains, using local fallback")
                        _domains.value = DomainData.getDomains()
                        _errorMessage.value = "Using local data. Please check Firebase setup."
                    }
                } else {
                    // Firebase error - use local fallback
                    val error = result.exceptionOrNull()
                    Log.e("HomeViewModel", "❌ Firebase error: ${error?.message}", error)
                    _domains.value = DomainData.getDomains()
                    _errorMessage.value = "Firebase connection issue. Using local data."
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Exception loading domains: ${e.message}", e)
                _domains.value = DomainData.getDomains()
                _errorMessage.value = "Error loading data. Using local fallback."
            }
            
            _isLoading.value = false
        }
    }

    fun getCurrentUserId(): String? {
        return repository.getCurrentUser()?.uid
    }
    
    /**
     * Load completed reports count for current user
     */
    fun loadCompletedReportsCount() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId != null) {
                    val result = repository.getUserReports(userId)
                    if (result.isSuccess) {
                        val reports = result.getOrNull() ?: emptyList()
                        _completedReportsCount.value = reports.size
                        Log.d("HomeViewModel", "✅ Loaded ${reports.size} completed reports")
                    } else {
                        Log.e("HomeViewModel", "❌ Error loading reports: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Exception loading reports count: ${e.message}", e)
            }
        }
    }
}

