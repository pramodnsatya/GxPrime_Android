package com.pramod.validator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pramod.validator.data.models.CustomAssessment
import com.pramod.validator.data.FirestoreCollections
import kotlinx.coroutines.tasks.await

class CustomAssessmentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection(FirestoreCollections.CUSTOM_ASSESSMENTS)
    
    suspend fun createCustomAssessment(assessment: CustomAssessment): Result<CustomAssessment> {
        return try {
            val docRef = if (assessment.id.isNotEmpty()) {
                collection.document(assessment.id)
            } else {
                collection.document()
            }
            
            val assessmentWithId = assessment.copy(id = docRef.id)
            // Explicit map without 'id' field (Firestore provides id from document id)
            val data = hashMapOf<String, Any>(
                "userId" to assessmentWithId.userId,
                "name" to assessmentWithId.name,
                "description" to assessmentWithId.description,
                "createdAt" to assessmentWithId.createdAt,
                "updatedAt" to assessmentWithId.updatedAt,
                "isFromChecklist" to assessmentWithId.isFromChecklist,
                "sourceFda483AssessmentId" to assessmentWithId.sourceFda483AssessmentId,
                "questions" to assessmentWithId.questions.map { q ->
                    mapOf(
                        "id" to q.id,
                        "questionText" to q.questionText,
                        "order" to q.order
                    )
                }
            )
            
            docRef.set(data).await()
            Result.success(assessmentWithId)
        } catch (e: Exception) {
            android.util.Log.e("CustomAssessmentRepository", "Error creating custom assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateCustomAssessment(assessment: CustomAssessment): Result<CustomAssessment> {
        return try {
            val updatedAssessment = assessment.copy(updatedAt = System.currentTimeMillis())
            // Write explicit map without 'id' field (Firestore provides id from document id)
            val data = hashMapOf<String, Any>(
                "userId" to updatedAssessment.userId,
                "name" to updatedAssessment.name,
                "description" to updatedAssessment.description,
                "createdAt" to updatedAssessment.createdAt,
                "updatedAt" to updatedAssessment.updatedAt,
                "isFromChecklist" to updatedAssessment.isFromChecklist,
                "sourceFda483AssessmentId" to updatedAssessment.sourceFda483AssessmentId,
                "questions" to updatedAssessment.questions.map { q ->
                    mapOf(
                        "id" to q.id,
                        "questionText" to q.questionText,
                        "order" to q.order
                    )
                }
            )
            
            collection.document(updatedAssessment.id)
                .set(data)
                .await()
            Result.success(updatedAssessment)
        } catch (e: Exception) {
            android.util.Log.e("CustomAssessmentRepository", "Error updating custom assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getCustomAssessmentById(assessmentId: String): Result<CustomAssessment?> {
        return try {
            val doc = collection.document(assessmentId).get().await()
            if (!doc.exists()) return Result.success(null)
            val data = doc.data ?: emptyMap<String, Any>()
            val questionsRaw = (data["questions"] as? List<*>) ?: emptyList<Any>()
            val questions = questionsRaw.mapNotNull { q ->
                (q as? Map<*, *>)?.let { m ->
                    com.pramod.validator.data.models.CustomQuestion(
                        id = (m["id"] as? String).orEmpty(),
                        questionText = (m["questionText"] as? String).orEmpty(),
                        order = ((m["order"] as? Number)?.toInt()) ?: 0
                    )
                }
            }
            val assessment = CustomAssessment(
                id = doc.id,
                userId = (data["userId"] as? String).orEmpty(),
                name = (data["name"] as? String).orEmpty(),
                description = (data["description"] as? String).orEmpty(),
                questions = questions,
                createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                isFromChecklist = (data["isFromChecklist"] as? Boolean) ?: false,
                sourceFda483AssessmentId = (data["sourceFda483AssessmentId"] as? String).orEmpty()
            )
            Result.success(assessment)
        } catch (e: Exception) {
            android.util.Log.e("CustomAssessmentRepository", "Error getting custom assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUserCustomAssessments(userId: String): Result<List<CustomAssessment>> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val assessments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val questionsRaw = (data["questions"] as? List<*>) ?: emptyList<Any>()
                val questions = questionsRaw.mapNotNull { q ->
                    (q as? Map<*, *>)?.let { m ->
                        com.pramod.validator.data.models.CustomQuestion(
                            id = (m["id"] as? String).orEmpty(),
                            questionText = (m["questionText"] as? String).orEmpty(),
                            order = ((m["order"] as? Number)?.toInt()) ?: 0
                        )
                    }
                }
                CustomAssessment(
                    id = doc.id,
                    userId = (data["userId"] as? String).orEmpty(),
                    name = (data["name"] as? String).orEmpty(),
                    description = (data["description"] as? String).orEmpty(),
                    questions = questions,
                    createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                    updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                    isFromChecklist = (data["isFromChecklist"] as? Boolean) ?: false,
                    sourceFda483AssessmentId = (data["sourceFda483AssessmentId"] as? String).orEmpty()
                )
            }.sortedByDescending { it.createdAt }
            
            Result.success(assessments)
        } catch (e: Exception) {
            android.util.Log.e("CustomAssessmentRepository", "Error getting user custom assessments: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteCustomAssessment(assessmentId: String): Result<Unit> {
        return try {
            collection.document(assessmentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CustomAssessmentRepository", "Error deleting custom assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
}

