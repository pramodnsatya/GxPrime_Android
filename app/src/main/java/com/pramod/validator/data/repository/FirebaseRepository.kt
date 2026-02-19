package com.pramod.validator.data.repository

import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.StorageException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.pramod.validator.data.FirestoreCollections
import com.pramod.validator.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import java.util.UUID
import java.util.Calendar

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Authentication
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            // Update Firebase Auth profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                createdAt = System.currentTimeMillis()
            )
            
            // Save user to Firestore
            firestore.collection(FirestoreCollections.USERS)
                .document(user.uid)
                .set(user)
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed")
            
            // Get user from Firestore
            val userDoc = firestore.collection(FirestoreCollections.USERS)
                .document(firebaseUser.uid)
                .get()
                .await()
            
            // If user document doesn't exist, check if it's a super admin and create it
            var user = userDoc.toObject(User::class.java)
            if (user == null) {
                // Check if this is a super admin email
                if (isSuperAdminEmail(email)) {
                    // Create super admin user document
                    user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        displayName = firebaseUser.displayName ?: "Super Admin",
                        role = UserRole.SUPER_ADMIN.name,
                        enterpriseId = "",
                        companyName = "",
                        createdBy = "",
                        isActive = true,
                        expiresAt = 0L // Super admin never expires
                    )
                    
                    // Save to Firestore
                    firestore.collection(FirestoreCollections.USERS)
                        .document(firebaseUser.uid)
                        .set(user)
                        .await()
                    
                    android.util.Log.d("FirebaseRepository", "✅ Created super admin user document in Firestore")
                } else {
                    throw Exception("User not found")
                }
            }
            
            // Check if user is active
            if (!user.isActive) {
                // Sign out the user immediately
                auth.signOut()
                throw Exception("Account has been deactivated. Please contact your administrator.")
            }
            
            // Check if user belongs to an enterprise and if that enterprise is active
            if (user.enterpriseId.isNotEmpty()) {
                val enterpriseDoc = firestore.collection(FirestoreCollections.ENTERPRISES)
                    .document(user.enterpriseId)
                    .get()
                    .await()
                
                val enterprise = enterpriseDoc.toObject(Enterprise::class.java)
                if (enterprise != null && !enterprise.isActive) {
                    // Sign out the user immediately
                    auth.signOut()
                    throw Exception("Your organization's account has been deactivated. Please contact your administrator.")
                }
                
                // Check if enterprise has expired (at 11:59 PM on expiration date)
                if (enterprise != null && enterprise.expiresAt != 0L) {
                    val expirationEndOfDay = getEndOfDayTimestamp(enterprise.expiresAt)
                    if (System.currentTimeMillis() > expirationEndOfDay) {
                        // Sign out the user immediately
                        auth.signOut()
                        throw Exception("Subscription is inactive, please reach out to customer care.")
                    }
                }
            }
            
            // Check if user account has expired (at 11:59 PM on expiration date)
            if (user.expiresAt != 0L) {
                val expirationEndOfDay = getEndOfDayTimestamp(user.expiresAt)
                if (System.currentTimeMillis() > expirationEndOfDay) {
                    // Sign out the user immediately
                    auth.signOut()
                    throw Exception("Subscription is inactive, please reach out to customer care.")
                }
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // Firebase Auth will only send reset emails to registered accounts
            // No need to query Firestore - this avoids permission issues
            auth.sendPasswordResetEmail(email).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Firebase Auth returns specific errors for invalid emails
            val errorMessage = when {
                e.message?.contains("no user record", ignoreCase = true) == true -> 
                    "No account found with this email address."
                e.message?.contains("invalid email", ignoreCase = true) == true -> 
                    "Please enter a valid email address."
                else -> e.message ?: "Failed to send reset email. Please try again."
            }
            Result.failure(Exception(errorMessage))
        }
    }

    fun signOut() {
        auth.signOut()
    }
    
    suspend fun getUserDisplayName(userId: String): String {
        return try {
            // Try Firebase Auth first
            val authDisplayName = auth.currentUser?.displayName
            if (!authDisplayName.isNullOrBlank()) {
                return authDisplayName
            }
            
            // Fallback to Firestore
            val userDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .get()
                .await()
            
            val user = userDoc.toObject(User::class.java)
            user?.displayName ?: auth.currentUser?.email ?: "Unknown User"
        } catch (e: Exception) {
            auth.currentUser?.email ?: "Unknown User"
        }
    }
    
    // Get question text by ID from Firestore (fallback for old reports)
    suspend fun getQuestionText(questionId: String): String? {
        return try {
            val questionDoc = firestore.collection(FirestoreCollections.QUESTIONS)
                .document(questionId)
                .get()
                .await()
            
            questionDoc.toObject(Question::class.java)?.text
        } catch (e: Exception) {
            null
        }
    }

    // Domains - Scalable methods
    suspend fun getDomains(): Result<List<Domain>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.DOMAINS)
                .get()
                .await()
            
            val domains = snapshot.documents
                .mapNotNull { it.toObject(Domain::class.java) }
                .sortedBy { it.order } // Sort in memory
            
            Result.success(domains)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveDomain(domain: Domain): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.DOMAINS)
                .document(domain.id)
                .set(domain)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Sub-Domains - Scalable methods
    suspend fun getSubDomains(domainId: String): Result<List<SubDomain>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.SUBDOMAINS)
                .whereEqualTo("domainId", domainId)
                // Removed .orderBy() to avoid needing composite index
                // Sort in memory instead
                .get()
                .await()
            
            val subDomains = snapshot.documents
                .mapNotNull { it.toObject(SubDomain::class.java) }
                .sortedBy { it.order } // Sort in memory
            
            Result.success(subDomains)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveSubDomain(subDomain: SubDomain): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.SUBDOMAINS)
                .document(subDomain.id)
                .set(subDomain)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Questions - Scalable methods (now uses subDomainId)
    suspend fun getQuestionsBySubDomain(subDomainId: String, forceServer: Boolean = false): Result<List<Question>> {
        return try {
            android.util.Log.d("FirebaseRepository", "📥 Loading questions for subDomainId: $subDomainId (forceServer: $forceServer)")
            
            val source = if (forceServer) {
                com.google.firebase.firestore.Source.SERVER
            } else {
                com.google.firebase.firestore.Source.DEFAULT
            }
            
            // Add timeout for server fetches to prevent hanging
            // Try both "domainId" and "subDomainId" fields to handle different data structures
            val snapshot = if (forceServer) {
                try {
                    kotlinx.coroutines.withTimeout(15000) { // 15 second timeout for server fetch
                        // First try with domainId field
                        var result = firestore.collection(FirestoreCollections.QUESTIONS)
                            .whereEqualTo("domainId", subDomainId)
                            .get(source)
                            .await()
                        
                        // If no results, try with subDomainId field
                        if (result.isEmpty) {
                            android.util.Log.d("FirebaseRepository", "🔍 No results with 'domainId' field, trying 'subDomainId' field...")
                            result = firestore.collection(FirestoreCollections.QUESTIONS)
                                .whereEqualTo("subDomainId", subDomainId)
                                .get(source)
                                .await()
                            
                            if (!result.isEmpty) {
                                android.util.Log.d("FirebaseRepository", "✅ Found questions using 'subDomainId' field")
                            }
                        }
                        
                        result
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.w("FirebaseRepository", "⚠️ Server fetch timeout for subDomainId: $subDomainId, trying cache...")
                    // Try cache as fallback on timeout - try both fields
                    var cacheResult = firestore.collection(FirestoreCollections.QUESTIONS)
                        .whereEqualTo("domainId", subDomainId)
                        .get(com.google.firebase.firestore.Source.CACHE)
                        .await()
                    
                    if (cacheResult.isEmpty) {
                        cacheResult = firestore.collection(FirestoreCollections.QUESTIONS)
                            .whereEqualTo("subDomainId", subDomainId)
                            .get(com.google.firebase.firestore.Source.CACHE)
                            .await()
                    }
                    
                    cacheResult
                }
            } else {
                // Try both fields for non-forced queries too
                var result = firestore.collection(FirestoreCollections.QUESTIONS)
                    .whereEqualTo("domainId", subDomainId)
                    .get(source)
                    .await()
                
                if (result.isEmpty) {
                    android.util.Log.d("FirebaseRepository", "🔍 No results with 'domainId' field, trying 'subDomainId' field...")
                    result = firestore.collection(FirestoreCollections.QUESTIONS)
                        .whereEqualTo("subDomainId", subDomainId)
                        .get(source)
                        .await()
                    
                    if (!result.isEmpty) {
                        android.util.Log.d("FirebaseRepository", "✅ Found questions using 'subDomainId' field")
                    }
                }
                
                result
            }
            
            val questions = snapshot.documents
                .mapNotNull { it.toObject(Question::class.java) }
                .sortedBy { it.order } // Sort in memory by order field
            
            // Log if we got questions from cache (metadata will indicate this)
            if (snapshot.metadata.isFromCache) {
                android.util.Log.w("FirebaseRepository", "⚠️ Loaded ${questions.size} questions from cache for subDomainId: $subDomainId")
            } else {
                android.util.Log.d("FirebaseRepository", "✅ Loaded ${questions.size} questions from server for subDomainId: $subDomainId")
            }
            
            if (questions.isEmpty() && !snapshot.metadata.isFromCache) {
                // Got empty result from server - this means no questions exist in database
                android.util.Log.w("FirebaseRepository", "⚠️ Server returned empty result for subDomainId: $subDomainId - no questions exist in database")
                
                // Diagnostic: Try to see what questions exist (limit to 5 for debugging)
                try {
                    val diagnosticSnapshot = firestore.collection(FirestoreCollections.QUESTIONS)
                        .limit(5)
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .await()
                    
                    val sampleQuestions = diagnosticSnapshot.documents.mapNotNull { doc ->
                        val question = doc.toObject(Question::class.java)
                        question?.let { "id=${it.id}, domainId=${it.domainId}, text=${it.text.take(50)}" }
                    }
                    
                    android.util.Log.w("FirebaseRepository", "🔍 Diagnostic: Sample questions in database (first 5):")
                    sampleQuestions.forEach { android.util.Log.w("FirebaseRepository", "  - $it") }
                    
                    // Also check if there are any questions with similar domainId
                    val allDomainIds = diagnosticSnapshot.documents.mapNotNull { 
                        it.toObject(Question::class.java)?.domainId 
                    }.distinct()
                    android.util.Log.w("FirebaseRepository", "🔍 Diagnostic: Sample domainIds found: $allDomainIds")
                    android.util.Log.w("FirebaseRepository", "🔍 Diagnostic: Looking for subDomainId: $subDomainId")
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRepository", "❌ Diagnostic query failed: ${e.message}")
                }
            }
            
            Result.success(questions)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            // Check if it's a network-related error
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ||
                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                android.util.Log.w("FirebaseRepository", "⚠️ Network error loading questions for subDomainId: $subDomainId - ${e.message}")
                // Try cache as fallback if not forcing server
                if (!forceServer) {
                    try {
                        android.util.Log.d("FirebaseRepository", "🔄 Attempting to load from cache as fallback...")
                        // Try both fields in cache
                        var cacheSnapshot = firestore.collection(FirestoreCollections.QUESTIONS)
                            .whereEqualTo("domainId", subDomainId)
                            .get(com.google.firebase.firestore.Source.CACHE)
                            .await()
                        
                        if (cacheSnapshot.isEmpty) {
                            cacheSnapshot = firestore.collection(FirestoreCollections.QUESTIONS)
                                .whereEqualTo("subDomainId", subDomainId)
                                .get(com.google.firebase.firestore.Source.CACHE)
                                .await()
                        }
                        
                        val cachedQuestions = cacheSnapshot.documents
                            .mapNotNull { it.toObject(Question::class.java) }
                            .sortedBy { it.order }
                        
                        if (cachedQuestions.isNotEmpty()) {
                            android.util.Log.w("FirebaseRepository", "✅ Loaded ${cachedQuestions.size} questions from cache as fallback")
                            return Result.success(cachedQuestions)
                        } else {
                            android.util.Log.w("FirebaseRepository", "⚠️ Cache also empty for subDomainId: $subDomainId")
                        }
                    } catch (cacheException: Exception) {
                        android.util.Log.e("FirebaseRepository", "❌ Cache fallback also failed: ${cacheException.message}")
                    }
                }
            }
            android.util.Log.e("FirebaseRepository", "❌ Error loading questions for subDomainId: $subDomainId - ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "❌ Unexpected error loading questions for subDomainId: $subDomainId - ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun saveQuestion(question: Question): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.QUESTIONS)
                .document(question.id)
                .set(question)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveQuestions(questions: List<Question>): Result<Unit> {
        return try {
            // Firestore batches can handle max 500 operations
            questions.chunked(500).forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { question ->
                    val docRef = firestore.collection(FirestoreCollections.QUESTIONS).document(question.id)
                    writeBatch.set(docRef, question)
                }
                writeBatch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Responses
    suspend fun saveResponse(response: Response): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.RESPONSES)
                .document(response.id)
                .set(response)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getResponsesForDomain(userId: String, domainId: String): Result<List<Response>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.RESPONSES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("domainId", domainId)
                .get()
                .await()
            
            val responses = snapshot.documents.mapNotNull { it.toObject(Response::class.java) }
            Result.success(responses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Reports
    suspend fun saveReport(report: Report): Result<Unit> {
        return try {
            android.util.Log.d("FirebaseRepository", "Saving report with enterpriseId: ${report.enterpriseId}")
            firestore.collection(FirestoreCollections.REPORTS)
                .document(report.id)
                .set(report)
                .await()
            
            android.util.Log.d("FirebaseRepository", "Successfully saved report")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Failed to save report: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "save_report")
            com.pramod.validator.utils.CrashReporting.setCustomKey("report_id", report.id)
            com.pramod.validator.utils.CrashReporting.setCustomKey("user_id", report.userId)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to save report")
            Result.failure(e)
        }
    }
    
    /**
     * Find an existing report by userId, assessmentName, subDomainId, and facilityId
     * This is used to prevent duplicate reports when generating AI summaries
     */
    suspend fun findExistingReport(
        userId: String,
        assessmentName: String,
        subDomainId: String,
        facilityId: String = "",
        domainId: String = ""
    ): Result<Report?> {
        return try {
            var query = firestore.collection(FirestoreCollections.REPORTS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("assessmentName", assessmentName)
                .whereEqualTo("subDomainId", subDomainId)
            
            // For non-custom assessments, also match facilityId and domainId
            if (domainId != "custom" && facilityId.isNotEmpty()) {
                query = query.whereEqualTo("facilityId", facilityId)
            }
            if (domainId.isNotEmpty()) {
                query = query.whereEqualTo("domainId", domainId)
            }
            
            val snapshot = query
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                android.util.Log.d("FirebaseRepository", "No existing report found")
                return Result.success(null)
            }
            
            val doc = snapshot.documents.first()
            val existingReport = doc.toObject(Report::class.java)
            android.util.Log.d("FirebaseRepository", "Found existing report: ${existingReport?.id}")
            Result.success(existingReport)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error finding existing report: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserReports(userId: String): Result<List<Report>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.REPORTS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { it.toObject(Report::class.java) }
                .sortedByDescending { it.completedAt }
            
            Result.success(reports)
        } catch (e: Exception) {
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "get_user_reports")
            com.pramod.validator.utils.CrashReporting.setCustomKey("user_id", userId)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to get user reports")
            Result.failure(e)
        }
    }
    
    suspend fun getEnterpriseReports(enterpriseId: String): Result<List<Report>> {
        return try {
            android.util.Log.d("FirebaseRepository", "Loading reports for enterprise: $enterpriseId")
            val snapshot = firestore.collection(FirestoreCollections.REPORTS)
                .whereEqualTo("enterpriseId", enterpriseId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { doc ->
                android.util.Log.d("FirebaseRepository", "Report data: ${doc.data}")
                doc.toObject(Report::class.java)
            }.sortedByDescending { it.completedAt }
            
            android.util.Log.d("FirebaseRepository", "Found ${reports.size} reports")
            Result.success(reports)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error loading enterprise reports: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "get_enterprise_reports")
            com.pramod.validator.utils.CrashReporting.setCustomKey("enterprise_id", enterpriseId)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to get enterprise reports")
            Result.failure(e)
        }
    }
    
    /**
     * Get user reports with pagination
     * @param userId User ID to filter reports
     * @param pageSize Number of reports per page (default 20)
     * @param lastDocument Last document from previous page (null for first page)
     * @return PaginatedResult with reports and pagination info
     */
    suspend fun getPaginatedUserReports(
        userId: String,
        pageSize: Int = 20,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Result<PaginatedResult<Report>> {
        return try {
            android.util.Log.d("FirebaseRepository", "Loading paginated reports for user: $userId, pageSize: $pageSize")
            
            var query = firestore.collection(FirestoreCollections.REPORTS)
                .whereEqualTo("userId", userId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(pageSize.toLong() + 1) // Load one extra to check if there are more
            
            // If not first page, start after last document
            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }
            
            val snapshot = query.get().await()
            val documents = snapshot.documents
            
            // Check if there are more pages
            val hasMore = documents.size > pageSize
            
            // Take only the requested page size
            val reportDocuments = if (hasMore) {
                documents.dropLast(1)
            } else {
                documents
            }
            
            val reports = reportDocuments.mapNotNull { it.toObject(Report::class.java) }
            val newLastDocument = if (reportDocuments.isNotEmpty()) {
                reportDocuments.last()
            } else null
            
            android.util.Log.d("FirebaseRepository", "Loaded ${reports.size} reports, hasMore: $hasMore")
            
            Result.success(PaginatedResult(
                items = reports,
                lastDocument = newLastDocument,
                hasMore = hasMore
            ))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error loading paginated user reports: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "get_paginated_user_reports")
            com.pramod.validator.utils.CrashReporting.setCustomKey("user_id", userId)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to get paginated user reports")
            Result.failure(e)
        }
    }
    
    /**
     * Get enterprise reports with pagination
     * @param enterpriseId Enterprise ID to filter reports
     * @param pageSize Number of reports per page (default 20)
     * @param lastDocument Last document from previous page (null for first page)
     * @return PaginatedResult with reports and pagination info
     */
    suspend fun getPaginatedEnterpriseReports(
        enterpriseId: String,
        pageSize: Int = 20,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Result<PaginatedResult<Report>> {
        return try {
            android.util.Log.d("FirebaseRepository", "Loading paginated reports for enterprise: $enterpriseId, pageSize: $pageSize")
            
            var query = firestore.collection(FirestoreCollections.REPORTS)
                .whereEqualTo("enterpriseId", enterpriseId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(pageSize.toLong() + 1) // Load one extra to check if there are more
            
            // If not first page, start after last document
            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }
            
            val snapshot = query.get().await()
            val documents = snapshot.documents
            
            // Check if there are more pages
            val hasMore = documents.size > pageSize
            
            // Take only the requested page size
            val reportDocuments = if (hasMore) {
                documents.dropLast(1)
            } else {
                documents
            }
            
            val reports = reportDocuments.mapNotNull { it.toObject(Report::class.java) }
            val newLastDocument = if (reportDocuments.isNotEmpty()) {
                reportDocuments.last()
            } else null
            
            android.util.Log.d("FirebaseRepository", "Loaded ${reports.size} reports, hasMore: $hasMore")
            
            Result.success(PaginatedResult(
                items = reports,
                lastDocument = newLastDocument,
                hasMore = hasMore
            ))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error loading paginated enterprise reports: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "get_paginated_enterprise_reports")
            com.pramod.validator.utils.CrashReporting.setCustomKey("enterprise_id", enterpriseId)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to get paginated enterprise reports")
            Result.failure(e)
        }
    }
    
    // ========== ENTERPRISE MANAGEMENT ==========
    
    /**
     * Create a new enterprise account
     * Only Super Admin can call this
     */
    suspend fun createEnterprise(enterprise: Enterprise, adminPassword: String, expiresAt: Long): Result<Enterprise> {
        return try {
            android.util.Log.d("FirebaseRepository", "Creating enterprise admin for: ${enterprise.adminEmail}")
            
            // Create Firebase Auth account for enterprise admin
            val authResult = auth.createUserWithEmailAndPassword(enterprise.adminEmail, adminPassword).await()
            val adminUid = authResult.user?.uid ?: throw Exception("Failed to create admin account")
            
            android.util.Log.d("FirebaseRepository", "Setting display name for enterprise admin: ${enterprise.adminName}")
            
            // Update Firebase Auth profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(enterprise.adminName)
                .build()
            authResult.user?.updateProfile(profileUpdates)?.await()
            
            // Update enterprise with admin UID and expiration
            val updatedEnterprise = enterprise.copy(
                adminUid = adminUid,
                currentUserCount = 1, // Admin counts as first user
                expiresAt = expiresAt
            )
            
            // Save enterprise to Firestore
            firestore.collection(FirestoreCollections.ENTERPRISES)
                .document(updatedEnterprise.id)
                .set(updatedEnterprise)
                .await()
            
            // Create enterprise admin user with same expiration as enterprise
            val adminUser = User(
                uid = adminUid,
                email = enterprise.adminEmail,
                displayName = enterprise.adminName,
                role = UserRole.ENTERPRISE_ADMIN.name,
                enterpriseId = enterprise.id,
                companyName = enterprise.companyName,
                createdBy = getCurrentUser()?.uid ?: "",
                isActive = true,
                expiresAt = expiresAt
            )
            
            // Save admin user to Firestore
            firestore.collection(FirestoreCollections.USERS)
                .document(adminUid)
                .set(adminUser)
                .await()
            
            android.util.Log.d("FirebaseRepository", "Enterprise admin created successfully")
            Result.success(updatedEnterprise)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error creating enterprise: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all enterprises
     * Only Super Admin can call this
     */
    suspend fun getAllEnterprises(): Result<List<Enterprise>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.ENTERPRISES)
                .get()
                .await()
            
            val enterprises = snapshot.documents
                .mapNotNull { it.toObject(Enterprise::class.java) }
                .sortedByDescending { it.createdAt }
            
            Result.success(enterprises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get enterprise by ID
     */
    suspend fun getEnterpriseById(enterpriseId: String): Result<Enterprise?> {
        return try {
            val doc = firestore.collection(FirestoreCollections.ENTERPRISES)
                .document(enterpriseId)
                .get()
                .await()
            
            Result.success(doc.toObject(Enterprise::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update enterprise details
     * When enterprise expiration changes, update all its users' expiration too
     */
    suspend fun updateEnterprise(enterprise: Enterprise): Result<Unit> {
        return try {
            val oldEnterpriseResult = getEnterpriseById(enterprise.id)
            val oldEnterprise = oldEnterpriseResult.getOrNull()
            
            // Save enterprise to Firestore
            firestore.collection(FirestoreCollections.ENTERPRISES)
                .document(enterprise.id)
                .set(enterprise)
                .await()
            
            // If expiration changed, update all users in this enterprise
            if (oldEnterprise != null && oldEnterprise.expiresAt != enterprise.expiresAt) {
                val usersResult = getEnterpriseUsers(enterprise.id)
                val users = usersResult.getOrNull() ?: emptyList()
                
                // Update each user's expiration
                users.forEach { user ->
                    val updatedUser = user.copy(expiresAt = enterprise.expiresAt)
                    firestore.collection(FirestoreCollections.USERS)
                        .document(user.uid)
                        .set(updatedUser)
                        .await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete enterprise (Super Admin only)
     * Also deletes all users in the enterprise
     */
    suspend fun deleteEnterprise(enterpriseId: String): Result<Unit> {
        return try {
            // Get all users in this enterprise
            val usersResult = getEnterpriseUsers(enterpriseId)
            val users = usersResult.getOrNull() ?: emptyList()
            
            // Delete all users
            users.forEach { user ->
                firestore.collection(FirestoreCollections.USERS)
                    .document(user.uid)
                    .delete()
                    .await()
            }
            
            // Delete enterprise
            firestore.collection(FirestoreCollections.ENTERPRISES)
                .document(enterpriseId)
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a standalone user (Super Admin only)
     * This is for individual users not part of any enterprise
     */
    suspend fun createUser(
        email: String,
        password: String,
        displayName: String,
        expiresAt: Long,
        department: String = "",
        jobTitle: String = ""
    ): Result<User> {
        return try {
            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user account")
            val userUid = firebaseUser.uid
            
            // Update Firebase Auth profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Create standalone user
            val user = User(
                uid = userUid,
                email = email,
                displayName = displayName,
                role = UserRole.USER.name,
                enterpriseId = "", // No enterprise
                companyName = "",
                department = department,
                jobTitle = jobTitle,
                createdBy = getCurrentUser()?.uid ?: "",
                isActive = true,
                expiresAt = expiresAt
            )
            
            // Save user to Firestore
            firestore.collection(FirestoreCollections.USERS)
                .document(userUid)
                .set(user)
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a user within an enterprise
     * Can be called by Enterprise Admin or Super Admin
     */
    suspend fun createEnterpriseUser(
        email: String,
        password: String,
        displayName: String,
        enterpriseId: String,
        department: String = "",
        jobTitle: String = "",
        permissions: UserPermission? = null
    ): Result<User> {
        // Save the current user to restore session
        val currentAdmin = getCurrentUser()
        
        return try {
            // Get enterprise to check user limit
            val enterpriseResult = getEnterpriseById(enterpriseId)
            val enterprise = enterpriseResult.getOrNull() ?: throw Exception("Enterprise not found")
            
            // Check if user limit reached
            if (enterprise.currentUserCount >= enterprise.userLimit) {
                throw Exception("User limit reached for this enterprise (${enterprise.userLimit} max)")
            }
            
            android.util.Log.d("FirebaseRepository", "Creating user with email: $email")
            
            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user account")
            val userUid = firebaseUser.uid
            
            android.util.Log.d("FirebaseRepository", "User created in Auth with UID: $userUid")
            
            // Update Firebase Auth profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Sign out the newly created user to restore admin session
            auth.signOut()
            android.util.Log.d("FirebaseRepository", "Signed out newly created user")
            
            // Create user with same expiration as enterprise
            val user = User(
                uid = userUid,
                email = email,
                displayName = displayName,
                role = UserRole.USER.name,
                enterpriseId = enterpriseId,
                companyName = enterprise.companyName,
                department = department,
                jobTitle = jobTitle,
                createdBy = currentAdmin?.uid ?: "",
                isActive = true,
                expiresAt = enterprise.expiresAt // Inherit enterprise expiration
            )
            
            // Save user to Firestore
            firestore.collection(FirestoreCollections.USERS)
                .document(userUid)
                .set(user)
                .await()
            
            android.util.Log.d("FirebaseRepository", "User saved to Firestore")
            
            // Save user permissions if provided
            if (permissions != null) {
                val userPermissions = permissions.copy(
                    userId = userUid,
                    enterpriseId = enterpriseId
                )
                firestore.collection(FirestoreCollections.USER_PERMISSIONS)
                    .document(userUid)
                    .set(userPermissions)
                    .await()
                
                android.util.Log.d("FirebaseRepository", "User permissions saved for: $email")
            }
            
            // Increment enterprise user count
            val updatedEnterprise = enterprise.copy(
                currentUserCount = enterprise.currentUserCount + 1
            )
            updateEnterprise(updatedEnterprise)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all users in an enterprise
     */
    suspend fun getEnterpriseUsers(enterpriseId: String): Result<List<User>> {
        return try {
            android.util.Log.d("FirebaseRepository", "========== GET ENTERPRISE USERS ==========")
            android.util.Log.d("FirebaseRepository", "Enterprise ID: $enterpriseId")
            android.util.Log.d("FirebaseRepository", "Current user: ${getCurrentUser()?.uid}")
            // Get current user's role from Firestore
            val currentUserRole = try {
                val currentUserDoc = firestore.collection(FirestoreCollections.USERS)
                    .document(getCurrentUser()?.uid ?: "")
                    .get()
                    .await()
                currentUserDoc.toObject(User::class.java)?.role ?: "UNKNOWN"
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
            android.util.Log.d("FirebaseRepository", "Current user role: $currentUserRole")
            
            val snapshot = firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo("enterpriseId", enterpriseId)
                .get()
                .await()
            
            android.util.Log.d("FirebaseRepository", "Query successful, found ${snapshot.documents.size} documents")
            
            val users = snapshot.documents
                .mapNotNull { it.toObject(User::class.java) }
                .sortedByDescending { it.createdAt }
            
            android.util.Log.d("FirebaseRepository", "Mapped to ${users.size} users")
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "❌ Error getting enterprise users: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all users (Super Admin only)
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.USERS)
                .get()
                .await()
            
            val users = snapshot.documents
                .mapNotNull { it.toObject(User::class.java) }
                .sortedByDescending { it.createdAt }
            
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user details
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            // First update Firebase Auth display name
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(user.displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }
            
            // Then update Firestore document
            firestore.collection(FirestoreCollections.USERS)
                .document(user.uid)
                .set(user)
                .await()
            
            // If user is being deactivated, also disable Firebase Auth
            if (!user.isActive) {
                try {
                    // Note: Firebase Admin SDK would be needed to disable auth users
                    // For now, we'll just update the Firestore record
                    // The app should check isActive before allowing login
                } catch (e: Exception) {
                    // Log but don't fail the operation
                    android.util.Log.w("FirebaseRepository", "Could not disable Firebase Auth user: ${e.message}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete user (Enterprise Admin only)
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // First, get the user to find their enterprise
            val userDoc = firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .get()
                .await()
            
            val user = userDoc.toObject(User::class.java)
            if (user == null) {
                return Result.failure(Exception("User not found"))
            }
            
            // Delete from Firestore
            firestore.collection(FirestoreCollections.USERS)
                .document(userId)
                .delete()
                .await()
            
            // Delete from Firebase Authentication
            try {
                // Note: This requires the user to be deleted from Firebase Auth
                // Since we don't have Admin SDK, we'll disable the account instead
                // The user won't be able to login as their Firestore record is gone
                // and their Auth account is effectively disabled
            } catch (authError: Exception) {
                // Log but don't fail - Firestore deletion is the main operation
                android.util.Log.w("FirebaseRepository", "Could not delete from Auth: ${authError.message}")
            }
            
            // Decrement enterprise user count if user belongs to an enterprise
            if (user.enterpriseId.isNotEmpty()) {
                val enterpriseDoc = firestore.collection(FirestoreCollections.ENTERPRISES)
                    .document(user.enterpriseId)
                    .get()
                    .await()
                
                val enterprise = enterpriseDoc.toObject(Enterprise::class.java)
                if (enterprise != null) {
                    val updatedEnterprise = enterprise.copy(
                        currentUserCount = maxOf(0, enterprise.currentUserCount - 1)
                    )
                    updateEnterprise(updatedEnterprise)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user by UID
     */
    suspend fun getUserById(uid: String): Result<User?> {
        return try {
            val doc = firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .get()
                .await()
            
            Result.success(doc.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ---------------------------
     * Training Resources
     * ---------------------------
     */
    data class TrainingResourceUploadResult(
        val downloadUrl: String,
        val storagePath: String
    )
    
    suspend fun uploadTrainingResourceFile(
        uri: Uri,
        fileName: String?,
        mimeType: String?
    ): Result<TrainingResourceUploadResult> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("FirebaseRepository", "📤 Starting file upload: uri=$uri, fileName=$fileName, mimeType=$mimeType")
            
            val extensionFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val extensionFromName = fileName?.substringAfterLast('.', missingDelimiterValue = "")
            val extension = when {
                !extensionFromMime.isNullOrBlank() -> extensionFromMime
                !extensionFromName.isNullOrBlank() -> extensionFromName
                else -> ""
            }
            val sanitizedExtension = if (extension.isBlank()) "" else ".$extension"
            val uniquePath = "training_resources/${UUID.randomUUID()}$sanitizedExtension"
            
            android.util.Log.d("FirebaseRepository", "📁 Upload path: $uniquePath")
            
            val storageRef = storage.reference.child(uniquePath)
            
            // Create metadata with cache control to help with availability
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(mimeType ?: "application/pdf")
                .setCacheControl("public, max-age=31536000")
                .build()
            
            android.util.Log.d("FirebaseRepository", "⏳ Uploading file to Firebase Storage...")
            
            // Use putFile with the original URI - simpler and let Firebase handle it
            val uploadTask = storageRef.putFile(uri, metadata)
            
            // Use continuation to get result synchronously
            val uploadResult = kotlinx.coroutines.suspendCancellableCoroutine<com.google.firebase.storage.UploadTask.TaskSnapshot> { continuation ->
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    android.util.Log.d("FirebaseRepository", "✅ File uploaded successfully")
                    continuation.resume(taskSnapshot) {}
                }.addOnFailureListener { exception ->
                    android.util.Log.e("FirebaseRepository", "❌ Upload failed: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
            }
            
            android.util.Log.d("FirebaseRepository", "📥 Getting download URL...")
            
            // Get download URL with continuation
            val downloadUrl = kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val url = uri.toString()
                    android.util.Log.d("FirebaseRepository", "✅ Download URL obtained: $url")
                    continuation.resume(url) {}
                }.addOnFailureListener { exception ->
                    android.util.Log.e("FirebaseRepository", "❌ Failed to get download URL: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
            }
            
            if (downloadUrl.isBlank()) {
                throw Exception("Download URL is empty")
            }
            
            Result.success(TrainingResourceUploadResult(downloadUrl, uniquePath))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "❌ Error uploading file: ${e.message}", e)
            Result.failure(Exception("Upload failed: ${e.message}"))
        }
    }
    
    suspend fun deleteTrainingResourceFile(storagePath: String): Result<Unit> {
        return try {
            if (storagePath.isNotBlank()) {
                storage.reference.child(storagePath).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTrainingResources(): Result<List<TrainingResource>> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.TRAINING_RESOURCES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val resources = snapshot.documents
                .mapNotNull { it.toObject(TrainingResource::class.java) }
            
            Result.success(resources)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeTrainingResources(): Flow<List<TrainingResource>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.TRAINING_RESOURCES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepository", "❌ Error observing training resources: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val resources = snapshot?.documents
                    ?.mapNotNull { it.toObject(TrainingResource::class.java) }
                    ?: emptyList()
                
                trySend(resources)
            }
        
        awaitClose { registration.remove() }
    }
    
    suspend fun getTrainingResource(resourceId: String): Result<TrainingResource?> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.TRAINING_RESOURCES)
                .document(resourceId)
                .get()
                .await()
            
            Result.success(snapshot.toObject(TrainingResource::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveTrainingResource(resource: TrainingResource): Result<TrainingResource> {
        return try {
            val collection = firestore.collection(FirestoreCollections.TRAINING_RESOURCES)
            val documentId = resource.id.ifBlank { collection.document().id }
            val resourceToSave = resource.copy(
                id = documentId,
                createdAt = if (resource.createdAt == 0L) System.currentTimeMillis() else resource.createdAt
            )
            
            collection.document(documentId)
                .set(resourceToSave)
                .await()
            
            Result.success(resourceToSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTrainingResource(resourceId: String, storagePath: String? = null): Result<Unit> {
        return try {
            if (!storagePath.isNullOrBlank()) {
                try {
                    storage.reference.child(storagePath).delete().await()
                } catch (storageError: Exception) {
                    android.util.Log.w(
                        "FirebaseRepository",
                        "⚠️ Failed to delete storage file for resource $resourceId: ${storageError.message}"
                    )
                }
            }
            firestore.collection(FirestoreCollections.TRAINING_RESOURCES)
                .document(resourceId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if email is super admin
     * Preset super admin credentials
     * To change these credentials:
     * 1. Update the email and password below
     * 2. Go to Firebase Console > Authentication > Users
     * 3. Find the old super admin user and delete it (or update email/password there)
     * 4. Create a new user with the new email and password
     * 5. Make sure the user's email matches the email returned by isSuperAdminEmail()
     */
    fun isSuperAdminEmail(email: String): Boolean {
        return email == "admin@gxprime.com" // Change this to your new super admin email
    }
    
    /**
     * Get super admin preset password
     * IMPORTANT: This password must match the password set in Firebase Authentication
     */
    fun getSuperAdminPassword(): String {
        return "Pramod@25" // Change this to your new super admin password
    }
    
    /**
     * Get end of day timestamp (11:59:59 PM) for a given date timestamp
     */
    private fun getEndOfDayTimestamp(dateTimestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateTimestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}

