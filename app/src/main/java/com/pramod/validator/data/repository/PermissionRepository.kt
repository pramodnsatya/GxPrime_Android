package com.pramod.validator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.data.models.Department
import com.pramod.validator.data.models.DepartmentType
import com.pramod.validator.data.FirestoreCollections
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class PermissionRepository {
    private val db = FirebaseFirestore.getInstance()
    
    // User Permissions Collection
    private val userPermissionsCollection = db.collection(FirestoreCollections.USER_PERMISSIONS)
    private val departmentsCollection = db.collection("departments") // Note: departments collection not in constants yet
    
    /**
     * Create user permissions
     */
    suspend fun createUserPermission(permission: UserPermission): Result<UserPermission> {
        return try {
            val docRef = userPermissionsCollection.document()
            val permissionWithId = permission.copy(id = docRef.id)
            docRef.set(permissionWithId).await()
            Result.success(permissionWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user permissions
     */
    suspend fun updateUserPermission(permission: UserPermission): Result<UserPermission> {
        return try {
            val updatedPermission = permission.copy(updatedAt = System.currentTimeMillis())
            userPermissionsCollection.document(permission.id)
                .set(updatedPermission)
                .await()
            Result.success(updatedPermission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user permissions by user ID
     */
    suspend fun getUserPermissions(userId: String): Result<UserPermission?> {
        return try {
            val snapshot = userPermissionsCollection
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            
            val permission = if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents.first().toObject(UserPermission::class.java)
            }
            
            Result.success(permission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all permissions for an enterprise
     */
    suspend fun getEnterprisePermissions(enterpriseId: String): Result<List<UserPermission>> {
        return try {
            val snapshot = userPermissionsCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val permissions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserPermission::class.java)
            }
            
            Result.success(permissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get permissions by department
     */
    suspend fun getDepartmentPermissions(enterpriseId: String, department: String): Result<List<UserPermission>> {
        return try {
            val snapshot = userPermissionsCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .whereEqualTo("department", department)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val permissions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserPermission::class.java)
            }
            
            Result.success(permissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete user permissions
     */
    suspend fun deleteUserPermission(permissionId: String): Result<Unit> {
        return try {
            userPermissionsCollection.document(permissionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create department
     */
    suspend fun createDepartment(department: Department): Result<Department> {
        return try {
            val docRef = departmentsCollection.document()
            val departmentWithId = department.copy(id = docRef.id)
            docRef.set(departmentWithId).await()
            Result.success(departmentWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get departments for an enterprise
     */
    suspend fun getEnterpriseDepartments(enterpriseId: String): Result<List<Department>> {
        return try {
            val snapshot = departmentsCollection
                .whereEqualTo("enterpriseId", enterpriseId)
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()
            
            val departments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Department::class.java)
            }
            
            Result.success(departments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get default domains for a department type
     */
    fun getDefaultDomainsForDepartment(departmentType: DepartmentType): List<String> {
        return departmentType.defaultDomains
    }
    
    /**
     * Check if user has permission to access domain
     */
    suspend fun hasDomainAccess(userId: String, domainId: String): Result<Boolean> {
        return try {
            val permissionResult = getUserPermissions(userId)
            if (permissionResult.isFailure) {
                return Result.failure(permissionResult.exceptionOrNull()!!)
            }
            
            val permission = permissionResult.getOrNull()
            // In simplified permission system, all users can access all domains
            val hasAccess = permission != null
            
            Result.success(hasAccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user can create assessments
     */
    suspend fun canCreateAssessments(userId: String): Result<Boolean> {
        return try {
            val permissionResult = getUserPermissions(userId)
            if (permissionResult.isFailure) {
                return Result.failure(permissionResult.exceptionOrNull()!!)
            }
            
            val permission = permissionResult.getOrNull()
            val canCreate = permission?.canCreateAssessments ?: false
            
            Result.success(canCreate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user can view department assessments
     */
    suspend fun canViewDepartmentAssessments(userId: String): Result<Boolean> {
        return try {
            val permissionResult = getUserPermissions(userId)
            if (permissionResult.isFailure) {
                return Result.failure(permissionResult.exceptionOrNull()!!)
            }
            
            val permission = permissionResult.getOrNull()
            val canView = permission?.canViewDepartmentAssessments ?: false
            
            Result.success(canView)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get filtered domains based on user permissions
     * In simplified permission system, all users can access all domains
     */
    suspend fun getFilteredDomainsForUser(userId: String): Result<List<String>> {
        return try {
            val permissionResult = getUserPermissions(userId)
            if (permissionResult.isFailure) {
                return Result.failure(permissionResult.exceptionOrNull()!!)
            }
            
            // Return empty list to indicate all domains are accessible
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
