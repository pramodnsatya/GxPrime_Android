package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.Report
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.data.repository.PermissionRepository
import com.pramod.validator.utils.AISummaryGenerator
import com.pramod.validator.utils.PermissionChecker
import com.pramod.validator.utils.AssessmentViewScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class HistoryViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val permissionRepository = PermissionRepository()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _filteredReports = MutableStateFlow<List<Report>>(emptyList())
    val filteredReports: StateFlow<List<Report>> = _filteredReports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _selectedDomainFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedDomainFilters: StateFlow<Set<String>> = _selectedDomainFilters.asStateFlow()
    
    private val _selectedStatusFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedStatusFilters: StateFlow<Set<String>> = _selectedStatusFilters.asStateFlow()
    
    private val _selectedDateRange = MutableStateFlow<String>("All Time")
    val selectedDateRange: StateFlow<String> = _selectedDateRange.asStateFlow()
    
    private val _selectedSortBy = MutableStateFlow<String>("Date (Newest)")
    val selectedSortBy: StateFlow<String> = _selectedSortBy.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Keep for backward compatibility
    private val _selectedDomainFilter = MutableStateFlow<String?>(null)
    val selectedDomainFilter: StateFlow<String?> = _selectedDomainFilter.asStateFlow()
    
    // Pagination state
    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private var hasMoreData = true
    private val pageSize = 20
    private var currentUserId: String? = null
    private var currentEnterpriseId: String? = null
    private var currentUserRole: String? = null
    private var currentViewScope: AssessmentViewScope? = null
    
    // Search state
    private var isSearchMode = false
    private var isLoadingAllForSearch = false
    private var allReportsLoaded = false  // Track if all reports are already loaded (from search)
    private var hasInitialLoad = false  // Track if initial load has been done

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            resetPagination() // Reset pagination state for fresh load
            allReportsLoaded = false  // Reset flag when explicitly loading (user refresh or initial load)
            try {
                val currentAuthUser = repository.getCurrentUser()
                if (currentAuthUser == null) {
                    android.util.Log.e("HistoryViewModel", "No Firebase Auth user - cannot load reports")
                    _reports.value = emptyList()
                    _filteredReports.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                val userId = currentAuthUser.uid
                currentUserId = userId // Store for pagination
                // Get current user to check their role
                val userResult = repository.getUserById(userId)
                val user = userResult.getOrNull()
                
                android.util.Log.d("HistoryViewModel", "Loading reports for user role: '${user?.role}', enterpriseId: '${user?.enterpriseId}'")
                android.util.Log.d("HistoryViewModel", "Role comparison - SUPER_ADMIN: ${user?.role == "SUPER_ADMIN"}, ENTERPRISE_ADMIN: ${user?.role == "ENTERPRISE_ADMIN"}")
                android.util.Log.d("HistoryViewModel", "User object: $user")
                
                // Store user info for pagination
                currentUserRole = user?.role
                currentEnterpriseId = user?.enterpriseId
                
                when (user?.role) {
                    "SUPER_ADMIN" -> {
                        // Super admin sees all reports - query directly from Firestore
                        try {
                            android.util.Log.d("HistoryViewModel", "SUPER_ADMIN: Loading ALL reports from Firestore")
                            val snapshot = FirebaseFirestore.getInstance()
                                .collection("reports")
                                .orderBy("completedAt", Query.Direction.DESCENDING)
                                .get()
                                .await()
                            val reportsList = snapshot.documents.mapNotNull { it.toObject(Report::class.java) }
                            android.util.Log.d("HistoryViewModel", "SUPER_ADMIN: Loaded ${reportsList.size} total reports")
                            
                            // Log date range of reports
                            if (reportsList.isNotEmpty()) {
                                val oldest = reportsList.minByOrNull { it.completedAt }
                                val newest = reportsList.maxByOrNull { it.completedAt }
                                android.util.Log.d("HistoryViewModel", "SUPER_ADMIN: Date range - Oldest: ${oldest?.completedAt?.let { java.util.Date(it) }}, Newest: ${newest?.completedAt?.let { java.util.Date(it) }}")
                            }
                            
                            // Deduplicate by ID to prevent duplicates
                            val uniqueReports = reportsList.distinctBy { it.id }
                            android.util.Log.d("HistoryViewModel", "SUPER_ADMIN: After deduplication: ${uniqueReports.size} unique reports")
                            _reports.value = uniqueReports
                            applyFilter()
                        } catch (e: Exception) {
                            android.util.Log.e("HistoryViewModel", "Error fetching all reports: ${e.message}", e)
                        }
                    }
                    "ENTERPRISE_ADMIN" -> {
                        // Enterprise admin sees all reports from their enterprise
                        android.util.Log.d("HistoryViewModel", "Processing ENTERPRISE_ADMIN case")
                        if (user?.enterpriseId?.isNotEmpty() == true) {
                            android.util.Log.d("HistoryViewModel", "Loading enterprise reports for enterpriseId: '${user.enterpriseId}'")
                            try {
                                // First, let's check if there are ANY reports in the database
                                val allReportsSnapshot = FirebaseFirestore.getInstance()
                                    .collection("reports")
                                    .limit(5)
                                    .get()
                                    .await()
                                
                                android.util.Log.d("HistoryViewModel", "DEBUG: Found ${allReportsSnapshot.documents.size} total reports in database")
                                allReportsSnapshot.documents.forEach { doc ->
                                    android.util.Log.d("HistoryViewModel", "DEBUG: Report ${doc.id} - enterpriseId: ${doc.data?.get("enterpriseId")}, userId: ${doc.data?.get("userId")}")
                                }
                                
                                // Now query for enterprise reports (simplified query to avoid index requirement)
                                val snapshot = FirebaseFirestore.getInstance()
                                    .collection("reports")
                                    .whereEqualTo("enterpriseId", user.enterpriseId)
                                    .get()
                                    .await()
                                
                                // Sort in memory to avoid index requirement
                                val sortedReports = snapshot.documents
                                    .mapNotNull { doc ->
                                        doc.toObject(Report::class.java)
                                    }
                                    .sortedByDescending { it.completedAt }
                                
                                android.util.Log.d("HistoryViewModel", "Query returned ${snapshot.documents.size} documents")
                                
                                android.util.Log.d("HistoryViewModel", "Successfully loaded ${sortedReports.size} reports for enterprise")
                                
                                // Log date range of reports
                                if (sortedReports.isNotEmpty()) {
                                    val oldest = sortedReports.minByOrNull { it.completedAt }
                                    val newest = sortedReports.maxByOrNull { it.completedAt }
                                    android.util.Log.d("HistoryViewModel", "ENTERPRISE_ADMIN: Date range - Oldest: ${oldest?.completedAt?.let { java.util.Date(it) }}, Newest: ${newest?.completedAt?.let { java.util.Date(it) }}")
                                }
                                
                                // Deduplicate by ID to prevent duplicates
                                val uniqueReports = sortedReports.distinctBy { it.id }
                                android.util.Log.d("HistoryViewModel", "ENTERPRISE_ADMIN: After deduplication: ${uniqueReports.size} unique reports")
                                _reports.value = uniqueReports
                                applyFilter()
                            } catch (e: Exception) {
                                android.util.Log.e("HistoryViewModel", "Failed to load enterprise reports: ${e.message}", e)
                                _reports.value = emptyList()
                                applyFilter()
                            }
                        } else {
                            android.util.Log.e("HistoryViewModel", "Enterprise admin has no enterpriseId or it's empty")
                            _reports.value = emptyList()
                            applyFilter()
                        }
                    }
                    else -> {
                        // Fallback: If role doesn't match exactly, try to handle as enterprise admin if they have enterpriseId
                        android.util.Log.d("HistoryViewModel", "Role '${user?.role}' not recognized, checking if user has enterpriseId")
                        if (user?.enterpriseId?.isNotEmpty() == true) {
                            android.util.Log.d("HistoryViewModel", "User has enterpriseId '${user.enterpriseId}', treating as enterprise admin")
                            try {
                                val snapshot = FirebaseFirestore.getInstance()
                                    .collection("reports")
                                    .whereEqualTo("enterpriseId", user.enterpriseId)
                                    .get()
                                    .await()
                                
                                // Sort in memory to avoid index requirement
                                val reportsList = snapshot.documents
                                    .mapNotNull { doc ->
                                        doc.toObject(Report::class.java)
                                    }
                                    .sortedByDescending { it.completedAt }
                                
                                android.util.Log.d("HistoryViewModel", "Fallback: Loaded ${reportsList.size} reports for enterprise")
                                // Deduplicate by ID to prevent duplicates
                                val uniqueReports = reportsList.distinctBy { it.id }
                                _reports.value = uniqueReports
                                applyFilter()
                            } catch (e: Exception) {
                                android.util.Log.e("HistoryViewModel", "Fallback: Failed to load enterprise reports: ${e.message}", e)
                                _reports.value = emptyList()
                                applyFilter()
                            }
                        } else {
                            android.util.Log.d("HistoryViewModel", "No enterpriseId found, treating as regular user")
                            // Regular user - check permissions and filter accordingly
                            val permissionResult = permissionRepository.getUserPermissions(userId)
                            permissionResult.fold(
                                onSuccess = { permission ->
                                    android.util.Log.d("HistoryViewModel", "User permissions: $permission")
                                    if (permission != null) {
                                        // Use centralized PermissionChecker
                                        val viewScope = PermissionChecker.getAssessmentViewScope(user, permission)
                                        android.util.Log.d("HistoryViewModel", "Assessment view scope: $viewScope")
                                        
                                        when (viewScope) {
                                            AssessmentViewScope.ALL -> {
                                                // Can view all assessments in enterprise - use pagination
                                                currentViewScope = AssessmentViewScope.ALL
                                                if (permission.enterpriseId.isNotEmpty()) {
                                                    android.util.Log.d("HistoryViewModel", "Loading all enterprise reports for enterpriseId: ${permission.enterpriseId} with pagination")
                                                    val result = repository.getPaginatedEnterpriseReports(
                                                        enterpriseId = permission.enterpriseId,
                                                        pageSize = pageSize,
                                                        lastDocument = null // First page
                                                    )
                                                    if (result.isSuccess) {
                                                        val paginatedResult = result.getOrNull()
                                                        if (paginatedResult != null) {
                                                            _reports.value = paginatedResult.items.distinctBy { it.id }
                                                            lastDocument = paginatedResult.lastDocument
                                                            hasMoreData = paginatedResult.hasMore
                                                            android.util.Log.d("HistoryViewModel", "Loaded ${paginatedResult.items.size} reports (ALL scope), hasMore=${paginatedResult.hasMore}")
                                                            applyFilter()
                                                        }
                                                    } else {
                                                        android.util.Log.e("HistoryViewModel", "Failed to load enterprise reports: ${result.exceptionOrNull()?.message}")
                                                        _reports.value = emptyList()
                                                        applyFilter()
                                                    }
                                                } else {
                                                    android.util.Log.w("HistoryViewModel", "No enterpriseId in permission, loading user's own reports with pagination")
                                                    currentViewScope = AssessmentViewScope.OWN
                                                    val result = repository.getPaginatedUserReports(
                                                        userId = userId,
                                                        pageSize = pageSize,
                                                        lastDocument = null // First page
                                                    )
                                                    if (result.isSuccess) {
                                                        val paginatedResult = result.getOrNull()
                                                        if (paginatedResult != null) {
                                                            _reports.value = paginatedResult.items.distinctBy { it.id }
                                                            lastDocument = paginatedResult.lastDocument
                                                            hasMoreData = paginatedResult.hasMore
                                                            applyFilter()
                                                        }
                                                    }
                                                }
                                            }
                                            AssessmentViewScope.DEPARTMENT -> {
                                                // Can view assessments in their department
                                                val userDepartment = PermissionChecker.getUserDepartment(user, permission)
                                                android.util.Log.d("HistoryViewModel", "Loading department reports for department: $userDepartment")
                                                if (permission.enterpriseId.isNotEmpty() && userDepartment.isNotEmpty()) {
                                                    val result = repository.getEnterpriseReports(permission.enterpriseId)
                                                    if (result.isSuccess) {
                                                        val reportsList = result.getOrNull() ?: emptyList()
                                                        // Filter by department
                                                        val departmentReports = reportsList.filter { report ->
                                                            report.userDepartment.equals(userDepartment, ignoreCase = true)
                                                        }
                                                        val uniqueReports = departmentReports.distinctBy { it.id }
                                                        android.util.Log.d("HistoryViewModel", "Loaded ${uniqueReports.size} department reports")
                                                        _reports.value = uniqueReports
                                                        applyFilter()
                                                    } else {
                                                        android.util.Log.e("HistoryViewModel", "Failed to load enterprise reports: ${result.exceptionOrNull()?.message}")
                                                        _reports.value = emptyList()
                                                        applyFilter()
                                                    }
                                                } else {
                                                    android.util.Log.w("HistoryViewModel", "Missing enterpriseId or department, loading user's own reports")
                                                    val result = repository.getUserReports(userId)
                                                if (result.isSuccess) {
                                                    val reportsList = result.getOrNull() ?: emptyList()
                                                    val uniqueReports = reportsList.distinctBy { it.id }
                                                    _reports.value = uniqueReports
                                                    applyFilter()
                                                    }
                                                }
                                            }
                                            AssessmentViewScope.OWN -> {
                                                // Can only view own assessments - use pagination
                                                android.util.Log.d("HistoryViewModel", "Loading user's own reports (OWN scope) with pagination")
                                                currentViewScope = AssessmentViewScope.OWN
                                                val result = repository.getPaginatedUserReports(
                                                    userId = userId,
                                                    pageSize = pageSize,
                                                    lastDocument = null // First page
                                                )
                                                if (result.isSuccess) {
                                                    val paginatedResult = result.getOrNull()
                                                    if (paginatedResult != null) {
                                                        _reports.value = paginatedResult.items.distinctBy { it.id }
                                                        lastDocument = paginatedResult.lastDocument
                                                        hasMoreData = paginatedResult.hasMore
                                                        android.util.Log.d("HistoryViewModel", "Loaded ${paginatedResult.items.size} own reports, hasMore=${paginatedResult.hasMore}")
                                                        applyFilter()
                                                    }
                                                } else {
                                                    android.util.Log.e("HistoryViewModel", "Failed to load user reports: ${result.exceptionOrNull()?.message}")
                                                    _reports.value = emptyList()
                                                    applyFilter()
                                                }
                                            }
                                        }
                                    } else {
                                        // No permissions set - can only view own reports with pagination
                                        android.util.Log.d("HistoryViewModel", "No permissions set, loading user's own reports with pagination")
                                        currentViewScope = AssessmentViewScope.OWN
                                        val result = repository.getPaginatedUserReports(
                                            userId = userId,
                                            pageSize = pageSize,
                                            lastDocument = null // First page
                                        )
                                            if (result.isSuccess) {
                                            val paginatedResult = result.getOrNull()
                                            if (paginatedResult != null) {
                                                _reports.value = paginatedResult.items.distinctBy { it.id }
                                                lastDocument = paginatedResult.lastDocument
                                                hasMoreData = paginatedResult.hasMore
                                                android.util.Log.d("HistoryViewModel", "Loaded ${paginatedResult.items.size} own reports (no permissions), hasMore=${paginatedResult.hasMore}")
                                                applyFilter()
                                            }
                                        } else {
                                            android.util.Log.e("HistoryViewModel", "Failed to load user reports: ${result.exceptionOrNull()?.message}")
                                            _reports.value = emptyList()
                                            applyFilter()
                                        }
                                    }
                                },
                                onFailure = { error ->
                                    // No permissions found - can only view own reports with pagination
                                    android.util.Log.w("HistoryViewModel", "No permissions found: ${error.message}, loading only user's own reports with pagination")
                                    currentViewScope = AssessmentViewScope.OWN
                                    val result = repository.getPaginatedUserReports(
                                        userId = userId,
                                        pageSize = pageSize,
                                        lastDocument = null // First page
                                    )
                                    if (result.isSuccess) {
                                        val paginatedResult = result.getOrNull()
                                        if (paginatedResult != null) {
                                            _reports.value = paginatedResult.items.distinctBy { it.id }
                                            lastDocument = paginatedResult.lastDocument
                                            hasMoreData = paginatedResult.hasMore
                                            android.util.Log.d("HistoryViewModel", "Loaded ${paginatedResult.items.size} own reports (permissions error), hasMore=${paginatedResult.hasMore}")
                                            applyFilter()
                                        }
                                    } else {
                                        android.util.Log.e("HistoryViewModel", "Failed to load user reports: ${result.exceptionOrNull()?.message}")
                                        _reports.value = emptyList()
                                        applyFilter()
                                    }
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error loading reports", e)
                _reports.value = emptyList()
                _filteredReports.value = emptyList()
            } finally {
                _isLoading.value = false
                // Mark initial load as done (even if it failed, to prevent infinite retries)
                hasInitialLoad = true
            }
        }
    }
    
    /**
     * Load more reports using pagination
     * Only loads if there's more data and not currently loading
     */
    fun loadMoreReports() {
        // Don't load if in search mode (all reports already loaded)
        if (isSearchMode) {
            android.util.Log.d("HistoryViewModel", "Skipping loadMore: in search mode (all reports already loaded)")
            return
        }
        
        // Don't load if all reports are already loaded (from previous search)
        if (allReportsLoaded) {
            android.util.Log.d("HistoryViewModel", "Skipping loadMore: all reports already loaded")
            return
        }
        
        // Don't load if already loading or no more data
        if (_isLoadingMore.value || !hasMoreData || lastDocument == null) {
            android.util.Log.d("HistoryViewModel", "Skipping loadMore: isLoading=${_isLoadingMore.value}, hasMore=$hasMoreData, lastDoc=${lastDocument != null}")
            return
        }
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val userId = currentUserId
                val enterpriseId = currentEnterpriseId
                val role = currentUserRole
                val viewScope = currentViewScope
                
                if (userId == null) {
                    android.util.Log.e("HistoryViewModel", "Cannot load more: no user ID")
                    _isLoadingMore.value = false
                    return@launch
                }
                
                android.util.Log.d("HistoryViewModel", "Loading more reports... lastDoc exists, role=$role, scope=$viewScope")
                
                // Load more based on role and scope
                when (role) {
                    "SUPER_ADMIN" -> {
                        // For super admin, we don't use pagination yet (loads all)
                        hasMoreData = false
                    }
                    "ENTERPRISE_ADMIN" -> {
                        if (enterpriseId != null) {
                            val result = repository.getPaginatedEnterpriseReports(
                                enterpriseId = enterpriseId,
                                pageSize = pageSize,
                                lastDocument = lastDocument
                            )
                            
                            if (result.isSuccess) {
                                val paginatedResult = result.getOrNull()
                                if (paginatedResult != null) {
                                    // Append new reports
                                    val newReports = _reports.value + paginatedResult.items
                                    _reports.value = newReports.distinctBy { it.id }
                                    
                                    // Update pagination state
                                    lastDocument = paginatedResult.lastDocument
                                    hasMoreData = paginatedResult.hasMore
                                    
                                    android.util.Log.d("HistoryViewModel", "Loaded ${paginatedResult.items.size} more reports, hasMore=${paginatedResult.hasMore}")
                                    
                                    applyFilter()
                                }
                            }
                        }
                    }
                    else -> {
                        // Regular user with permissions
                        when (viewScope) {
                            AssessmentViewScope.ALL -> {
                                if (enterpriseId != null) {
                                    val result = repository.getPaginatedEnterpriseReports(
                                        enterpriseId = enterpriseId,
                                        pageSize = pageSize,
                                        lastDocument = lastDocument
                                    )
                                    
                                    if (result.isSuccess) {
                                        val paginatedResult = result.getOrNull()
                                        if (paginatedResult != null) {
                                            val newReports = _reports.value + paginatedResult.items
                                            _reports.value = newReports.distinctBy { it.id }
                                            lastDocument = paginatedResult.lastDocument
                                            hasMoreData = paginatedResult.hasMore
                                            applyFilter()
                                        }
                                    }
                                }
                            }
                            AssessmentViewScope.DEPARTMENT -> {
                                // For department view, we still load all enterprise reports 
                                // and filter by department (pagination doesn't help much here)
                                hasMoreData = false
                            }
                            AssessmentViewScope.OWN -> {
                                val result = repository.getPaginatedUserReports(
                                    userId = userId,
                                    pageSize = pageSize,
                                    lastDocument = lastDocument
                                )
                                
                                if (result.isSuccess) {
                                    val paginatedResult = result.getOrNull()
                                    if (paginatedResult != null) {
                                        val newReports = _reports.value + paginatedResult.items
                                        _reports.value = newReports.distinctBy { it.id }
                                        lastDocument = paginatedResult.lastDocument
                                        hasMoreData = paginatedResult.hasMore
                                        applyFilter()
                                    }
                                }
                            }
                            else -> {
                                hasMoreData = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error loading more reports", e)
            }
            _isLoadingMore.value = false
        }
    }
    
    /**
     * Reset pagination state (call before loadReports)
     */
    private fun resetPagination() {
        lastDocument = null
        hasMoreData = true
    }
    
    /**
     * Check if reports should be loaded (only if not already loaded)
     */
    fun shouldLoadReports(): Boolean {
        return !hasInitialLoad || _reports.value.isEmpty()
    }
    
    /**
     * Force reload reports (for pull-to-refresh scenarios)
     */
    fun forceReloadReports() {
        hasInitialLoad = false
        loadReports()
    }
    
    /**
     * Load ALL reports (non-paginated) for search functionality
     * This ensures search works across all user reports, not just loaded pages
     */
    private fun loadAllReportsForSearch() {
        if (isLoadingAllForSearch) {
            android.util.Log.d("HistoryViewModel", "Already loading all reports for search, skipping")
            return
        }
        
        viewModelScope.launch {
            isLoadingAllForSearch = true
            _isLoading.value = true
            try {
                val currentAuthUser = repository.getCurrentUser()
                if (currentAuthUser == null) {
                    android.util.Log.e("HistoryViewModel", "No Firebase Auth user - cannot load all reports")
                    _isLoading.value = false
                    isLoadingAllForSearch = false
                    return@launch
                }
                
                val userId = currentAuthUser.uid
                val userResult = repository.getUserById(userId)
                val user = userResult.getOrNull()
                
                android.util.Log.d("HistoryViewModel", "Loading ALL reports for search, role: ${user?.role}")
                
                when (user?.role) {
                    "SUPER_ADMIN" -> {
                        // Super admin - load all reports
                        val snapshot = FirebaseFirestore.getInstance()
                            .collection("reports")
                            .orderBy("completedAt", Query.Direction.DESCENDING)
                            .get()
                            .await()
                        val reportsList = snapshot.documents.mapNotNull { it.toObject(Report::class.java) }
                        _reports.value = reportsList.distinctBy { it.id }
                    }
                    "ENTERPRISE_ADMIN" -> {
                        // Enterprise admin - load all enterprise reports
                        if (user.enterpriseId?.isNotEmpty() == true) {
                            val result = repository.getEnterpriseReports(user.enterpriseId)
                            if (result.isSuccess) {
                                val reportsList = result.getOrNull() ?: emptyList()
                                _reports.value = reportsList.distinctBy { it.id }
                            }
                        }
                    }
                    else -> {
                        // Regular user - check permissions
                        val permissionResult = permissionRepository.getUserPermissions(userId)
                        permissionResult.fold(
                            onSuccess = { permission ->
                                if (permission != null) {
                                    val viewScope = PermissionChecker.getAssessmentViewScope(user, permission)
                                    when (viewScope) {
                                        AssessmentViewScope.ALL -> {
                                            if (permission.enterpriseId.isNotEmpty()) {
                                                val result = repository.getEnterpriseReports(permission.enterpriseId)
                                                if (result.isSuccess) {
                                                    val reportsList = result.getOrNull() ?: emptyList()
                                                    _reports.value = reportsList.distinctBy { it.id }
                                                }
                                            }
                                        }
                                        AssessmentViewScope.DEPARTMENT -> {
                                            if (permission.enterpriseId.isNotEmpty()) {
                                                val result = repository.getEnterpriseReports(permission.enterpriseId)
                                                if (result.isSuccess) {
                                                    val allReports = result.getOrNull() ?: emptyList()
                                                    val userDepartment = PermissionChecker.getUserDepartment(user, permission)
                                                    val departmentReports = if (userDepartment.isNotEmpty()) {
                                                        allReports.filter { it.userDepartment == userDepartment }
                                                    } else {
                                                        allReports
                                                    }
                                                    _reports.value = departmentReports.distinctBy { it.id }
                                                }
                                            }
                                        }
                                        AssessmentViewScope.OWN -> {
                                            // Load all user reports
                                            val result = repository.getUserReports(userId)
                                            if (result.isSuccess) {
                                                val reportsList = result.getOrNull() ?: emptyList()
                                                _reports.value = reportsList.distinctBy { it.id }
                                            }
                                        }
                                        else -> {
                                            val result = repository.getUserReports(userId)
                                            if (result.isSuccess) {
                                                val reportsList = result.getOrNull() ?: emptyList()
                                                _reports.value = reportsList.distinctBy { it.id }
                                            }
                                        }
                                    }
                                } else {
                                    // No permissions - load own reports
                                    val result = repository.getUserReports(userId)
                                    if (result.isSuccess) {
                                        val reportsList = result.getOrNull() ?: emptyList()
                                        _reports.value = reportsList.distinctBy { it.id }
                                    }
                                }
                            },
                            onFailure = { error ->
                                android.util.Log.w("HistoryViewModel", "Failed to get permissions for search: ${error.message}")
                                val result = repository.getUserReports(userId)
                                if (result.isSuccess) {
                                    val reportsList = result.getOrNull() ?: emptyList()
                                    _reports.value = reportsList.distinctBy { it.id }
                                }
                            }
                        )
                    }
                }
                
                applyFilter()
                // Mark that all reports are loaded and reset pagination state
                allReportsLoaded = true
                hasMoreData = false
                lastDocument = null
                android.util.Log.d("HistoryViewModel", "Loaded ${_reports.value.size} reports for search")
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error loading all reports for search", e)
            }
            _isLoading.value = false
            isLoadingAllForSearch = false
        }
    }

    fun applyFilter() {
        val domainFilters = _selectedDomainFilters.value
        val statusFilters = _selectedStatusFilters.value
        val dateRange = _selectedDateRange.value
        val sortBy = _selectedSortBy.value
        val searchQuery = _searchQuery.value.trim().lowercase()
        val allReports = _reports.value
        
        // Deduplicate reports by ID to prevent showing duplicates
        var filtered = allReports.distinctBy { it.id }
        
        // Apply domain filter (multiple domains)
        if (domainFilters.isNotEmpty()) {
            filtered = filtered.filter { report ->
                // Check if report's domain matches any selected domain
                domainFilters.any { domainName ->
                    matchesDomain(report.domainId, report.domainName, domainName)
                }
            }
        }
        
        // Apply status filter
        // Note: All reports in history are "Completed". "In Progress" would need to check InProgressAssessments
        // For now, if "In Progress" is selected, show empty (or we could combine with in-progress assessments)
        if (statusFilters.isNotEmpty()) {
            filtered = filtered.filter { report ->
                if (statusFilters.contains("Completed")) {
                    true // All reports are completed
                } else if (statusFilters.contains("In Progress")) {
                    false // Reports are not in-progress (they're completed)
        } else {
                    true
                }
            }
        }
        
        // Apply date range filter
        if (dateRange != "All Time") {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            
            val startTime = when (dateRange) {
                "Today" -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                "This Week" -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                "This Month" -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                else -> 0L
            }
            
            filtered = filtered.filter { it.completedAt >= startTime }
        }
        
        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { report ->
                // Search in assessment name
                report.assessmentName.lowercase().contains(searchQuery) ||
                // Search in facility name
                report.facilityName.lowercase().contains(searchQuery) ||
                // Search in user name
                report.userName.lowercase().contains(searchQuery) ||
                // Search in domain name
                report.domainName.lowercase().contains(searchQuery) ||
                report.subDomainName.lowercase().contains(searchQuery)
            }
        }
        
        // Apply sorting
        val sorted = when (sortBy) {
            "Date (Newest)" -> filtered.sortedByDescending { it.completedAt }
            "Date (Oldest)" -> filtered.sortedBy { it.completedAt }
            "Name (A-Z)" -> filtered.sortedBy { it.assessmentName.lowercase() }
            "Name (Z-A)" -> filtered.sortedByDescending { it.assessmentName.lowercase() }
            "Domain (A-Z)" -> filtered.sortedBy { it.domainName.lowercase() }
            "Domain (Z-A)" -> filtered.sortedByDescending { it.domainName.lowercase() }
            "Status" -> filtered.sortedByDescending { it.completedAt } // All are completed, sort by date
            else -> filtered.sortedByDescending { it.completedAt }
        }
        
        _filteredReports.value = sorted
    }
    
    // Helper function to match domain by ID or name
    private fun matchesDomain(domainId: String, domainName: String, filterDomain: String): Boolean {
        return when (filterDomain) {
            "Quality Unit" -> domainId.contains("qu_", ignoreCase = true) || 
                            domainName.contains("Quality", ignoreCase = true)
            "Packaging & Labeling" -> domainId.contains("pl_", ignoreCase = true) || 
                                     domainName.contains("Packaging", ignoreCase = true) ||
                                     domainName.contains("Labeling", ignoreCase = true)
            "Production" -> domainId.contains("pr_", ignoreCase = true) || 
                          domainName.contains("Production", ignoreCase = true) ||
                          domainName.contains("Manufacturing", ignoreCase = true)
            "Materials" -> domainId.contains("mt_", ignoreCase = true) || 
                         domainName.contains("Material", ignoreCase = true)
            "Laboratory Systems" -> domainId.contains("lab_", ignoreCase = true) || 
                                   domainName.contains("Laboratory", ignoreCase = true)
            "Facilities & Equipment" -> domainId.contains("fe_", ignoreCase = true) || 
                                       domainName.contains("Facilities", ignoreCase = true) ||
                                       domainName.contains("Equipment", ignoreCase = true)
            "Custom" -> domainId == "custom" || domainName.contains("Custom", ignoreCase = true)
            else -> false
        }
    }
    
    private var searchDebounceJob: kotlinx.coroutines.Job? = null
    
    fun setSearchQuery(query: String) {
        val trimmedQuery = query.trim()
        val previousQuery = _searchQuery.value.trim()
        
        // Update search query immediately for UI responsiveness
        _searchQuery.value = query
        
        // Cancel previous debounce job
        searchDebounceJob?.cancel()
        
        // If search query changed from empty to non-empty, enter search mode
        if (previousQuery.isEmpty() && trimmedQuery.isNotEmpty()) {
            android.util.Log.d("HistoryViewModel", "Entering search mode, loading all reports")
            isSearchMode = true
            loadAllReportsForSearch()
        }
        // If search query changed from non-empty to empty, exit search mode
        else if (previousQuery.isNotEmpty() && trimmedQuery.isEmpty()) {
            android.util.Log.d("HistoryViewModel", "Exiting search mode, keeping already-loaded reports")
            isSearchMode = false
            // DON'T call loadReports() - we already have all reports loaded!
            // Just apply filter to show all reports (no search filter)
            applyFilter()
        }
        // If search query is non-empty, debounce filter application
        else if (trimmedQuery.isNotEmpty()) {
            // Debounce filter application to avoid excessive filtering while typing
            searchDebounceJob = viewModelScope.launch {
                delay(300) // Wait 300ms after user stops typing
                if (isSearchMode) {
                    android.util.Log.d("HistoryViewModel", "Applying debounced search filter")
                    applyFilter()
                }
            }
            // Apply filter immediately for responsive UI (will be refined after debounce)
            applyFilter()
        }
        // If search is empty, just apply filter
        else {
            applyFilter()
        }
    }

    fun setDomainFilters(domains: Set<String>) {
        _selectedDomainFilters.value = domains
        // Update backward compatibility
        _selectedDomainFilter.value = domains.firstOrNull()
        applyFilter()
    }
    
    fun setStatusFilters(statuses: Set<String>) {
        _selectedStatusFilters.value = statuses
        applyFilter()
    }
    
    fun setDateRange(dateRange: String) {
        _selectedDateRange.value = dateRange
        applyFilter()
    }
    
    fun setSortBy(sortBy: String) {
        _selectedSortBy.value = sortBy
        applyFilter()
    }

    // Backward compatibility
    fun setDomainFilter(domainId: String?) {
        _selectedDomainFilter.value = domainId
        _selectedDomainFilters.value = if (domainId != null) setOf(domainId) else emptySet()
        applyFilter()
    }

    fun clearFilter() {
        _selectedDomainFilters.value = emptySet()
        _selectedStatusFilters.value = emptySet()
        _selectedDateRange.value = "All Time"
        _selectedSortBy.value = "Date (Newest)"
        _selectedDomainFilter.value = null
        applyFilter()
    }
}