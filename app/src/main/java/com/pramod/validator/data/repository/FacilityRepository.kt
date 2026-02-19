package com.pramod.validator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pramod.validator.data.models.Facility
import com.pramod.validator.data.FirestoreCollections
import kotlinx.coroutines.tasks.await
import android.util.Log

class FacilityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val facilitiesCollection = firestore.collection(FirestoreCollections.FACILITIES)

    /**
     * Create a new facility
     */
    suspend fun createFacility(facility: Facility): Result<Facility> {
        return try {
            val facilityWithId = facility.copy(id = facilitiesCollection.document().id)
            Log.d("FacilityRepository", "💾 Creating facility: ${facilityWithId.name}")
            
            facilitiesCollection.document(facilityWithId.id).set(facilityWithId).await()
            Log.d("FacilityRepository", "✅ Facility created successfully")
            Result.success(facilityWithId)
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error creating facility: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all facilities for a specific enterprise
     */
    suspend fun getFacilitiesByEnterprise(enterpriseId: String): Result<List<Facility>> {
        return try {
            Log.d("FacilityRepository", "📥 Loading facilities for enterprise: $enterpriseId")
            
            val snapshot = facilitiesCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .get()
                .await()
            
            val facilities = snapshot.documents.mapNotNull { document ->
                try {
                    val facility = document.toObject(Facility::class.java)
                    if (facility != null) {
                        // Check if facility is active
                        if (facility.isActive) facility else null
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("FacilityRepository", "❌ Failed to map facility: ${e.message}")
                    null
                }
            }.sortedBy { it.name }
            
            Log.d("FacilityRepository", "✅ Loaded ${facilities.size} facilities")
            Result.success(facilities)
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error loading facilities: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get facility by ID
     */
    suspend fun getFacilityById(facilityId: String): Result<Facility?> {
        return try {
            val document = facilitiesCollection.document(facilityId).get().await()
            Result.success(document.toObject(Facility::class.java))
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error getting facility: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing facility
     * This will cascade update all reports with this facility
     */
    suspend fun updateFacility(facility: Facility): Result<Unit> {
        return try {
            val updatedFacility = facility.copy(updatedAt = System.currentTimeMillis())
            
            // Get old facility name before updating
            val oldFacility = getFacilityById(facility.id).getOrNull()
            val oldName = oldFacility?.name ?: ""
            
            // Update the facility
            facilitiesCollection.document(facility.id).set(updatedFacility).await()
            Log.d("FacilityRepository", "✅ Facility updated: ${updatedFacility.name}")
            
            // If name changed, cascade update to all reports
            if (oldName.isNotEmpty() && oldName != updatedFacility.name) {
                cascadeUpdateFacilityName(facility.id, updatedFacility.name)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error updating facility: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cascade update facility name to all related reports
     */
    private suspend fun cascadeUpdateFacilityName(facilityId: String, newName: String) {
        try {
            Log.d("FacilityRepository", "🔄 Cascading facility name update for ID: $facilityId → '$newName'")
            
            // Update reports
            val reportsCollection = firestore.collection(FirestoreCollections.REPORTS)
            val reportsSnapshot = reportsCollection
                .whereEqualTo("facilityId", facilityId)
                .get()
                .await()
            
            reportsSnapshot.documents.forEach { doc ->
                doc.reference.update("facilityName", newName).await()
            }
            Log.d("FacilityRepository", "✅ Updated ${reportsSnapshot.documents.size} reports")
            
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error in cascade update: ${e.message}", e)
        }
    }

    /**
     * Delete a facility (hard delete - permanently remove from database)
     * Returns the count of affected assessments
     */
    suspend fun deleteFacility(facilityId: String): Result<Int> {
        return try {
            // Get facility info before deletion
            val facility = getFacilityById(facilityId).getOrNull()
            if (facility == null) {
                return Result.failure(Exception("Facility not found"))
            }
            
            // Count affected assessments
            val affectedCount = countAssessmentsByFacility(facilityId)
            
            // Permanently delete from Firestore
            facilitiesCollection.document(facilityId).delete().await()
            Log.d("FacilityRepository", "✅ Facility permanently deleted: ${facility.name} (ID: $facilityId)")
            
            Result.success(affectedCount)
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error deleting facility: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Count assessments for a specific facility
     */
    suspend fun countAssessmentsByFacility(facilityId: String): Int {
        return try {
            val reportsCollection = firestore.collection(FirestoreCollections.REPORTS)
            val snapshot = reportsCollection
                .whereEqualTo("facilityId", facilityId)
                .get()
                .await()
            
            snapshot.documents.size
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error counting assessments: ${e.message}", e)
            0
        }
    }

    /**
     * Get facility name by ID (for display purposes)
     */
    suspend fun getFacilityName(facilityId: String): Result<String> {
        return try {
            val facility = getFacilityById(facilityId).getOrNull()
            Result.success(facility?.name ?: "Unknown Facility")
        } catch (e: Exception) {
            Log.e("FacilityRepository", "❌ Error getting facility name: ${e.message}", e)
            Result.failure(e)
        }
    }
}