package com.pramod.validator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pramod.validator.data.models.Fda483Assessment
import com.pramod.validator.data.FirestoreCollections
import kotlinx.coroutines.tasks.await

class Fda483Repository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection(FirestoreCollections.FDA483_ASSESSMENTS)
    
    /**
     * Create a new FDA 483 assessment
     */
    suspend fun createAssessment(assessment: Fda483Assessment): Result<Fda483Assessment> {
        return try {
            val docRef = if (assessment.id.isNotEmpty()) {
                collection.document(assessment.id)
            } else {
                collection.document()
            }
            
            val assessmentWithId = assessment.copy(id = docRef.id)
            docRef.set(assessmentWithId).await()
            Result.success(assessmentWithId)
        } catch (e: Exception) {
            android.util.Log.e("Fda483Repository", "Error creating assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing FDA 483 assessment
     */
    suspend fun updateAssessment(assessment: Fda483Assessment): Result<Fda483Assessment> {
        return try {
            collection.document(assessment.id)
                .set(assessment)
                .await()
            Result.success(assessment)
        } catch (e: Exception) {
            android.util.Log.e("Fda483Repository", "Error updating assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get assessment by ID
     */
    suspend fun getAssessmentById(assessmentId: String): Result<Fda483Assessment?> {
        return try {
            val doc = collection.document(assessmentId).get().await()
            val assessment = doc.toObject(Fda483Assessment::class.java)
            Result.success(assessment)
        } catch (e: Exception) {
            android.util.Log.e("Fda483Repository", "Error getting assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all assessments for a specific user
     */
    suspend fun getUserAssessments(userId: String): Result<List<Fda483Assessment>> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val assessments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Fda483Assessment::class.java)
            }.sortedByDescending { it.uploadedAt } // Sort in memory
            
            Result.success(assessments)
        } catch (e: Exception) {
            android.util.Log.e("Fda483Repository", "Error getting user assessments: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete an assessment
     */
    suspend fun deleteAssessment(assessmentId: String): Result<Unit> {
        return try {
            collection.document(assessmentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("Fda483Repository", "Error deleting assessment: ${e.message}", e)
            Result.failure(e)
        }
    }
}


