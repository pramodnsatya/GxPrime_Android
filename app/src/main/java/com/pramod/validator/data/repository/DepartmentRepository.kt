package com.pramod.validator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pramod.validator.data.models.Department
import com.pramod.validator.data.FirestoreCollections
import kotlinx.coroutines.tasks.await
import android.util.Log

class DepartmentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val departmentsCollection = firestore.collection(FirestoreCollections.DEPARTMENTS)
    private val reportsCollection = firestore.collection(FirestoreCollections.REPORTS)
    private val usersCollection = firestore.collection(FirestoreCollections.USERS)

    /**
     * Create a new department
     */
    suspend fun createDepartment(department: Department): Result<Department> {
        return try {
            val departmentWithId = department.copy(id = departmentsCollection.document().id)
            Log.d("DepartmentRepository", "💾 Creating department: ${departmentWithId.name}")
            
            departmentsCollection.document(departmentWithId.id).set(departmentWithId).await()
            Log.d("DepartmentRepository", "✅ Department created successfully")
            Result.success(departmentWithId)
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error creating department: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all departments for a specific enterprise
     */
    suspend fun getDepartmentsByEnterprise(enterpriseId: String): Result<List<Department>> {
        return try {
            Log.d("DepartmentRepository", "📥 Loading departments for enterprise: $enterpriseId")
            
            val snapshot = departmentsCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .get()
                .await()
            
            val departments = snapshot.documents.mapNotNull { document ->
                try {
                    val department = document.toObject(Department::class.java)
                    if (department != null && department.isActive) department else null
                } catch (e: Exception) {
                    Log.e("DepartmentRepository", "❌ Failed to map department: ${e.message}")
                    null
                }
            }.sortedBy { it.name }
            
            Log.d("DepartmentRepository", "✅ Loaded ${departments.size} departments")
            Result.success(departments)
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error loading departments: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get department by ID
     */
    suspend fun getDepartmentById(departmentId: String): Result<Department?> {
        return try {
            val document = departmentsCollection.document(departmentId).get().await()
            Result.success(document.toObject(Department::class.java))
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error getting department: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing department
     * This will cascade update all users and reports with this department
     */
    suspend fun updateDepartment(department: Department): Result<Unit> {
        return try {
            val updatedDepartment = department.copy(updatedAt = System.currentTimeMillis())
            
            // Get old department name before updating
            val oldDepartment = getDepartmentById(department.id).getOrNull()
            val oldName = oldDepartment?.name ?: ""
            
            // Update the department
            departmentsCollection.document(department.id).set(updatedDepartment).await()
            Log.d("DepartmentRepository", "✅ Department updated: ${updatedDepartment.name}")
            
            // If name changed, cascade update to all users and reports
            if (oldName.isNotEmpty() && oldName != updatedDepartment.name) {
                cascadeUpdateDepartmentName(department.enterpriseId, oldName, updatedDepartment.name)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error updating department: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cascade update department name to all related entities
     */
    private suspend fun cascadeUpdateDepartmentName(enterpriseId: String, oldName: String, newName: String) {
        try {
            Log.d("DepartmentRepository", "🔄 Cascading department name update: '$oldName' → '$newName'")
            
            // Update users
            val usersSnapshot = usersCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .whereEqualTo("department", oldName)
                .get()
                .await()
            
            usersSnapshot.documents.forEach { doc ->
                doc.reference.update("department", newName).await()
            }
            Log.d("DepartmentRepository", "✅ Updated ${usersSnapshot.documents.size} users")
            
            // Update reports
            val reportsSnapshot = reportsCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .whereEqualTo("userDepartment", oldName)
                .get()
                .await()
            
            reportsSnapshot.documents.forEach { doc ->
                doc.reference.update("userDepartment", newName).await()
            }
            Log.d("DepartmentRepository", "✅ Updated ${reportsSnapshot.documents.size} reports")
            
            // Update permissions collection
            val permissionsSnapshot = firestore.collection("user_permissions")
                .whereEqualTo("enterpriseId", enterpriseId)
                .whereEqualTo("department", oldName)
                .get()
                .await()
            
            permissionsSnapshot.documents.forEach { doc ->
                doc.reference.update("department", newName).await()
            }
            Log.d("DepartmentRepository", "✅ Updated ${permissionsSnapshot.documents.size} permissions")
            
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error in cascade update: ${e.message}", e)
        }
    }

    /**
     * Delete a department
     * Returns the count of affected assessments
     */
    suspend fun deleteDepartment(departmentId: String): Result<Int> {
        return try {
            val department = getDepartmentById(departmentId).getOrNull()
            if (department == null) {
                return Result.failure(Exception("Department not found"))
            }
            
            // Count affected assessments
            val affectedCount = countAssessmentsByDepartment(department.enterpriseId, department.name)
            
            // Delete the department
            departmentsCollection.document(departmentId).delete().await()
            Log.d("DepartmentRepository", "✅ Department deleted: ${department.name}")
            
            Result.success(affectedCount)
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error deleting department: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Count assessments for a specific department
     */
    suspend fun countAssessmentsByDepartment(enterpriseId: String, departmentName: String): Int {
        return try {
            val snapshot = reportsCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .whereEqualTo("userDepartment", departmentName)
                .get()
                .await()
            
            snapshot.documents.size
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error counting assessments: ${e.message}", e)
            0
        }
    }

    /**
     * Get department name by ID
     */
    suspend fun getDepartmentName(departmentId: String): Result<String> {
        return try {
            val department = getDepartmentById(departmentId).getOrNull()
            Result.success(department?.name ?: "Unknown Department")
        } catch (e: Exception) {
            Log.e("DepartmentRepository", "❌ Error getting department name: ${e.message}", e)
            Result.failure(e)
        }
    }
}


