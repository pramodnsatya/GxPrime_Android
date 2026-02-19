package com.pramod.validator.utils

import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.data.models.UserRole

/**
 * Centralized permission checking utility - Simplified Version
 * This class provides methods to check the 5 core user permissions
 */
object PermissionChecker {
    
    /**
     * Check if user can create assessments
     * All users (standalone and enterprise) can always create assessments
     */
    fun canCreateAssessments(user: User?, permissions: UserPermission?): Boolean {
        // Super Admin and Enterprise Admin can always create assessments
        if (user?.role == UserRole.SUPER_ADMIN.name || user?.role == UserRole.ENTERPRISE_ADMIN.name) {
            return true
        }
        
        // Standalone users (no enterprise) can always create assessments
        if (user?.enterpriseId.isNullOrEmpty()) {
            return true
        }
        
        // Enterprise users - always enabled (core permission)
        return permissions?.canCreateAssessments ?: true
    }
    
    /**
     * Check if user can view their own assessments
     * All users can always view their own assessments
     */
    fun canViewOwnAssessments(user: User?, permissions: UserPermission?): Boolean {
        // Everyone can view their own assessments by default
        if (user?.role == UserRole.SUPER_ADMIN.name || user?.role == UserRole.ENTERPRISE_ADMIN.name) {
            return true
        }
        
        // Standalone users can always view their own
        if (user?.enterpriseId.isNullOrEmpty()) {
            return true
        }
        
        // Enterprise users - always enabled (core permission)
        return permissions?.canViewOwnAssessments ?: true
    }
    
    /**
     * Check if user can view department assessments
     */
    fun canViewDepartmentAssessments(user: User?, permissions: UserPermission?): Boolean {
        if (user?.role == UserRole.SUPER_ADMIN.name || user?.role == UserRole.ENTERPRISE_ADMIN.name) {
            return true
        }
        
        // Standalone users cannot view department assessments (no department)
        if (user?.enterpriseId.isNullOrEmpty()) {
            return false
        }
        
        return permissions?.canViewDepartmentAssessments == true
    }
    
    /**
     * Check if user can view all assessments in the enterprise
     */
    fun canViewAllAssessments(user: User?, permissions: UserPermission?): Boolean {
        if (user?.role == UserRole.SUPER_ADMIN.name || user?.role == UserRole.ENTERPRISE_ADMIN.name) {
            return true
        }
        
        // Standalone users cannot view all assessments (no enterprise)
        if (user?.enterpriseId.isNullOrEmpty()) {
            return false
        }
        
        return permissions?.canViewAllAssessments == true
    }
    
    /**
     * Check if user can access FDA 483 Analysis feature
     */
    fun canAccessFda483Analysis(user: User?, permissions: UserPermission?): Boolean {
        // Super Admin and Enterprise Admin can always access
        if (user?.role == UserRole.SUPER_ADMIN.name || user?.role == UserRole.ENTERPRISE_ADMIN.name) {
            return true
        }
        
        // Standalone users can always access (no restrictions)
        if (user?.enterpriseId.isNullOrEmpty()) {
            return true
        }
        
        // Enterprise users need explicit permission
        return permissions?.canAccessFda483Analysis == true
    }
    
    
    /**
     * Get user's department
     */
    fun getUserDepartment(user: User?, permissions: UserPermission?): String {
        return permissions?.department ?: user?.department ?: ""
    }
    
    /**
     * Check if user belongs to a specific department
     */
    fun isInDepartment(user: User?, permissions: UserPermission?, department: String): Boolean {
        val userDept = getUserDepartment(user, permissions)
        return userDept.equals(department, ignoreCase = true)
    }
    
    /**
     * Determine which assessments a user can view
     * Returns a filter strategy: ALL, DEPARTMENT, OWN
     */
    fun getAssessmentViewScope(user: User?, permissions: UserPermission?): AssessmentViewScope {
        if (user?.role == UserRole.SUPER_ADMIN.name || user?.role == UserRole.ENTERPRISE_ADMIN.name) {
            return AssessmentViewScope.ALL
        }
        
        // Standalone users (no enterprise) can only view their own assessments
        if (user?.enterpriseId.isNullOrEmpty()) {
            return AssessmentViewScope.OWN
        }
        
        // Enterprise users follow their assigned permissions
        // Note: canViewOwnAssessments is always true, so minimum is OWN
        return when {
            permissions?.canViewAllAssessments == true -> AssessmentViewScope.ALL
            permissions?.canViewDepartmentAssessments == true -> AssessmentViewScope.DEPARTMENT
            else -> AssessmentViewScope.OWN // Always at least OWN (core permission)
        }
    }
}

/**
 * Defines the scope of assessments a user can view
 */
enum class AssessmentViewScope {
    ALL,            // Can view all assessments in enterprise
    DEPARTMENT,     // Can view assessments in their department (includes own)
    OWN             // Can only view their own assessments (always enabled)
}

