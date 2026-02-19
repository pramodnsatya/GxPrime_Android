package com.pramod.validator.data.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents user permissions within an enterprise
 * Simplified permission system with 5 core permissions
 */
data class UserPermission(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val enterpriseId: String = "",
    val department: String = "", // QA, Production, Materials, Laboratory, Facilities, etc.
    
    // Core Permissions - Always true, cannot be disabled
    val canCreateAssessments: Boolean = true, // Main feature - always enabled
    val canViewOwnAssessments: Boolean = true, // View own assessment history - always enabled
    
    // Optional Permissions
    val canViewDepartmentAssessments: Boolean = false, // View assessments from same department (includes own)
    val canViewAllAssessments: Boolean = false, // View all enterprise assessments (includes department + own)
    val canAccessFda483Analysis: Boolean = false, // Show FDA 483 Analysis in bottom bar
    
    val createdBy: String = "", // Enterprise Admin who created this permission
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a department within an enterprise
 */
data class Department(
    @DocumentId
    val id: String = "",
    val enterpriseId: String = "",
    val name: String = "", // QA, Production, Materials, Laboratory, Facilities, etc.
    val description: String = "",
    val allowedDomains: List<String> = emptyList(), // Default domains for this department
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents domain access permissions
 */
data class DomainAccess(
    val domainId: String = "",
    val domainName: String = "",
    val subdomains: List<String> = emptyList(),
    val isAllowed: Boolean = false
)

/**
 * Permission levels for different actions
 */
enum class PermissionLevel {
    NONE,           // No access
    READ_ONLY,      // Can only view
    CREATE,         // Can create assessments
    MANAGE,         // Can manage department assessments
    ADMIN           // Full administrative access
}

/**
 * Department types with their associated domains
 */
enum class DepartmentType(val displayName: String, val defaultDomains: List<String>) {
    QA("Quality Assurance", listOf("qu_", "pl_")),
    PRODUCTION("Production", listOf("pr_")),
    MATERIALS("Materials Management", listOf("mt_")),
    LABORATORY("Laboratory", listOf("lab_")),
    FACILITIES("Facilities & Equipment", listOf("fe_")),
    REGULATORY("Regulatory Affairs", listOf("qu_", "pl_", "pr_", "mt_", "lab_", "fe_")),
    MANAGEMENT("Management", listOf("qu_", "pl_", "pr_", "mt_", "lab_", "fe_"))
}
