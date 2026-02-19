package com.pramod.validator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pramod.validator.data.models.InProgressAssessment
import com.pramod.validator.data.FirestoreCollections
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class InProgressAssessmentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection(FirestoreCollections.IN_PROGRESS_ASSESSMENTS)
    
    suspend fun saveInProgressAssessment(assessment: InProgressAssessment): Result<InProgressAssessment> {
        return try {
            val docRef = if (assessment.id.isNotEmpty()) {
                collection.document(assessment.id)
            } else {
                collection.document()
            }
            
            val assessmentWithId = assessment.copy(id = docRef.id, updatedAt = System.currentTimeMillis())
            // Explicit map without 'id' field (Firestore provides id from document id)
            val data = hashMapOf<String, Any>(
                "userId" to assessmentWithId.userId,
                "assessmentName" to assessmentWithId.assessmentName,
                "facilityId" to assessmentWithId.facilityId,
                "facilityName" to assessmentWithId.facilityName,
                "domainId" to assessmentWithId.domainId,
                "domainName" to assessmentWithId.domainName,
                "subDomainId" to assessmentWithId.subDomainId,
                "subDomainName" to assessmentWithId.subDomainName,
                "isCustomAssessment" to assessmentWithId.isCustomAssessment,
                "currentQuestionIndex" to assessmentWithId.currentQuestionIndex,
                "totalQuestions" to assessmentWithId.totalQuestions,
                "responses" to assessmentWithId.responses,
                "questionTexts" to assessmentWithId.questionTexts,
                "createdAt" to assessmentWithId.createdAt,
                "updatedAt" to assessmentWithId.updatedAt
            )
            
            docRef.set(data).await()
            android.util.Log.d("InProgressAssessmentRepository", "✅ Saved in-progress assessment to Firestore: ${docRef.id}")
            android.util.Log.d("InProgressAssessmentRepository", "   - userId: ${assessmentWithId.userId}")
            android.util.Log.d("InProgressAssessmentRepository", "   - assessmentName: ${assessmentWithId.assessmentName}")
            android.util.Log.d("InProgressAssessmentRepository", "   - responses count: ${assessmentWithId.responses.size}")
            Result.success(assessmentWithId)
        } catch (e: Exception) {
            android.util.Log.e("InProgressAssessmentRepository", "Error saving in-progress assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getInProgressAssessmentById(assessmentId: String): Result<InProgressAssessment?> {
        return try {
            val doc = collection.document(assessmentId).get().await()
            if (!doc.exists()) return Result.success(null)
            val data = doc.data ?: emptyMap<String, Any>()
            
            val responsesRaw = (data["responses"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val responses = responsesRaw.mapNotNull { (k, v) ->
                (k as? String)?.let { key ->
                    (v as? String)?.let { value ->
                        key to value
                    }
                }
            }.toMap()
            
            val questionTextsRaw = (data["questionTexts"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val questionTexts = questionTextsRaw.mapNotNull { (k, v) ->
                (k as? String)?.let { key ->
                    (v as? String)?.let { value ->
                        key to value
                    }
                }
            }.toMap()
            
            val assessment = InProgressAssessment(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                assessmentName = (data["assessmentName"] as? String).orEmpty(),
                facilityId = (data["facilityId"] as? String).orEmpty(),
                facilityName = (data["facilityName"] as? String).orEmpty(),
                domainId = (data["domainId"] as? String).orEmpty(),
                domainName = (data["domainName"] as? String).orEmpty(),
                subDomainId = (data["subDomainId"] as? String).orEmpty(),
                subDomainName = (data["subDomainName"] as? String).orEmpty(),
                isCustomAssessment = (data["isCustomAssessment"] as? Boolean) ?: false,
                currentQuestionIndex = ((data["currentQuestionIndex"] as? Number)?.toInt()) ?: 0,
                totalQuestions = ((data["totalQuestions"] as? Number)?.toInt()) ?: questionTexts.size.takeIf { it > 0 } ?: 0,
                responses = responses,
                questionTexts = questionTexts,
                createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
            )
            Result.success(assessment)
        } catch (e: Exception) {
            android.util.Log.e("InProgressAssessmentRepository", "Error getting in-progress assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserInProgressAssessments(userId: String): Result<List<InProgressAssessment>> {
        return try {
            // Query without orderBy to avoid requiring an index
            // We'll sort in memory instead
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            android.util.Log.d("InProgressAssessmentRepository", "📥 Fetching in-progress assessments for userId: $userId")
            android.util.Log.d("InProgressAssessmentRepository", "   Found ${snapshot.documents.size} documents")
            
            val assessments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                val responsesRaw = (data["responses"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                val responses = responsesRaw.mapNotNull { (k, v) ->
                    (k as? String)?.let { key ->
                        (v as? String)?.let { value ->
                            key to value
                        }
                    }
                }.toMap()
                
                val questionTextsRaw = (data["questionTexts"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                val questionTexts = questionTextsRaw.mapNotNull { (k, v) ->
                    (k as? String)?.let { key ->
                        (v as? String)?.let { value ->
                            key to value
                        }
                    }
                }.toMap()
                
                InProgressAssessment(
                    id = doc.id,
                    userId = (data["userId"] as? String).orEmpty(),
                    assessmentName = (data["assessmentName"] as? String).orEmpty(),
                    facilityId = (data["facilityId"] as? String).orEmpty(),
                    facilityName = (data["facilityName"] as? String).orEmpty(),
                    domainId = (data["domainId"] as? String).orEmpty(),
                    domainName = (data["domainName"] as? String).orEmpty(),
                    subDomainId = (data["subDomainId"] as? String).orEmpty(),
                    subDomainName = (data["subDomainName"] as? String).orEmpty(),
                    isCustomAssessment = (data["isCustomAssessment"] as? Boolean) ?: false,
                    currentQuestionIndex = ((data["currentQuestionIndex"] as? Number)?.toInt()) ?: 0,
                    totalQuestions = ((data["totalQuestions"] as? Number)?.toInt()) ?: questionTexts.size.takeIf { it > 0 } ?: 0,
                    responses = responses,
                    questionTexts = questionTexts,
                    createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                    updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
                )
            }
            
            // Sort by updatedAt descending in memory
            val sortedAssessments = assessments.sortedByDescending { it.updatedAt }
            
            android.util.Log.d("InProgressAssessmentRepository", "✅ Successfully parsed ${sortedAssessments.size} assessments")
            Result.success(sortedAssessments)
        } catch (e: Exception) {
            android.util.Log.e("InProgressAssessmentRepository", "Error getting user in-progress assessments: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteInProgressAssessment(assessmentId: String): Result<Unit> {
        return try {
            collection.document(assessmentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("InProgressAssessmentRepository", "Error deleting in-progress assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateInProgressAssessment(assessment: InProgressAssessment): Result<InProgressAssessment> {
        return try {
            val updatedAssessment = assessment.copy(updatedAt = System.currentTimeMillis())
            val data = hashMapOf<String, Any>(
                "userId" to updatedAssessment.userId,
                "assessmentName" to updatedAssessment.assessmentName,
                "facilityId" to updatedAssessment.facilityId,
                "facilityName" to updatedAssessment.facilityName,
                "domainId" to updatedAssessment.domainId,
                "domainName" to updatedAssessment.domainName,
                "subDomainId" to updatedAssessment.subDomainId,
                "subDomainName" to updatedAssessment.subDomainName,
                "isCustomAssessment" to updatedAssessment.isCustomAssessment,
                "currentQuestionIndex" to updatedAssessment.currentQuestionIndex,
                "totalQuestions" to updatedAssessment.totalQuestions,
                "responses" to updatedAssessment.responses,
                "questionTexts" to updatedAssessment.questionTexts,
                "createdAt" to updatedAssessment.createdAt,
                "updatedAt" to updatedAssessment.updatedAt
            )
            
            collection.document(updatedAssessment.id)
                .set(data)
                .await()
            Result.success(updatedAssessment)
        } catch (e: Exception) {
            android.util.Log.e("InProgressAssessmentRepository", "Error updating in-progress assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun findExistingAssessment(
        userId: String,
        assessmentName: String,
        facilityId: String,
        subDomainId: String,
        isCustomAssessment: Boolean
    ): Result<InProgressAssessment?> {
        return try {
            var query = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("assessmentName", assessmentName)
                .whereEqualTo("subDomainId", subDomainId)
                .whereEqualTo("isCustomAssessment", isCustomAssessment)
            
            // For non-custom assessments, also match facilityId
            if (!isCustomAssessment) {
                query = query.whereEqualTo("facilityId", facilityId) as com.google.firebase.firestore.Query
            }
            
            val snapshot = query.get().await()
            
            if (snapshot.documents.isEmpty()) {
                return Result.success(null)
            }
            
            // Get the first matching document
            val doc = snapshot.documents.first()
            val data = doc.data ?: return Result.success(null)
            
            val responsesRaw = (data["responses"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val responses = responsesRaw.mapNotNull { (k, v) ->
                (k as? String)?.let { key ->
                    (v as? String)?.let { value ->
                        key to value
                    }
                }
            }.toMap()
            
            val questionTextsRaw = (data["questionTexts"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val questionTexts = questionTextsRaw.mapNotNull { (k, v) ->
                (k as? String)?.let { key ->
                    (v as? String)?.let { value ->
                        key to value
                    }
                }
            }.toMap()
            
            val assessment = InProgressAssessment(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                assessmentName = (data["assessmentName"] as? String).orEmpty(),
                facilityId = (data["facilityId"] as? String).orEmpty(),
                facilityName = (data["facilityName"] as? String).orEmpty(),
                domainId = (data["domainId"] as? String).orEmpty(),
                domainName = (data["domainName"] as? String).orEmpty(),
                subDomainId = (data["subDomainId"] as? String).orEmpty(),
                subDomainName = (data["subDomainName"] as? String).orEmpty(),
                isCustomAssessment = (data["isCustomAssessment"] as? Boolean) ?: false,
                currentQuestionIndex = ((data["currentQuestionIndex"] as? Number)?.toInt()) ?: 0,
                totalQuestions = ((data["totalQuestions"] as? Number)?.toInt()) ?: questionTexts.size.takeIf { it > 0 } ?: 0,
                responses = responses,
                questionTexts = questionTexts,
                createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
            )
            
            Result.success(assessment)
        } catch (e: Exception) {
            android.util.Log.e("InProgressAssessmentRepository", "Error finding existing assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Listen to real-time updates for a specific in-progress assessment
     * Returns a Flow that emits updated assessments
     */
    fun listenToInProgressAssessment(assessmentId: String): Flow<Result<InProgressAssessment?>> = callbackFlow {
        val listener = collection.document(assessmentId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("InProgressAssessmentRepository", "Error listening to assessment: ${error.message}", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            
            if (snapshot == null || !snapshot.exists()) {
                trySend(Result.success(null))
                return@addSnapshotListener
            }
            
            try {
                val data = snapshot.data ?: emptyMap<String, Any>()
                
                val responsesRaw = (data["responses"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                val responses = responsesRaw.mapNotNull { (k, v) ->
                    (k as? String)?.let { key ->
                        (v as? String)?.let { value ->
                            key to value
                        }
                    }
                }.toMap()
                
                val questionTextsRaw = (data["questionTexts"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                val questionTexts = questionTextsRaw.mapNotNull { (k, v) ->
                    (k as? String)?.let { key ->
                        (v as? String)?.let { value ->
                            key to value
                        }
                    }
                }.toMap()
                
                val assessment = InProgressAssessment(
                    id = snapshot.id,
                    userId = (data["userId"] as? String).orEmpty(),
                    assessmentName = (data["assessmentName"] as? String).orEmpty(),
                    facilityId = (data["facilityId"] as? String).orEmpty(),
                    facilityName = (data["facilityName"] as? String).orEmpty(),
                    domainId = (data["domainId"] as? String).orEmpty(),
                    domainName = (data["domainName"] as? String).orEmpty(),
                    subDomainId = (data["subDomainId"] as? String).orEmpty(),
                    subDomainName = (data["subDomainName"] as? String).orEmpty(),
                    isCustomAssessment = (data["isCustomAssessment"] as? Boolean) ?: false,
                    currentQuestionIndex = ((data["currentQuestionIndex"] as? Number)?.toInt()) ?: 0,
                    totalQuestions = ((data["totalQuestions"] as? Number)?.toInt()) ?: questionTexts.size.takeIf { it > 0 } ?: 0,
                    responses = responses,
                    questionTexts = questionTexts,
                    createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                    updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
                )
                
                android.util.Log.d("InProgressAssessmentRepository", "📡 Real-time update received for assessment: ${assessment.id}")
                trySend(Result.success(assessment))
            } catch (e: Exception) {
                android.util.Log.e("InProgressAssessmentRepository", "Error parsing assessment from snapshot: ${e.message}", e)
                trySend(Result.failure(e))
            }
        }
        
        awaitClose { listener.remove() }
    }
}

