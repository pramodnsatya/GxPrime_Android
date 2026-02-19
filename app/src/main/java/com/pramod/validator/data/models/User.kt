package com.pramod.validator.data.models

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = UserRole.USER.name, // SUPER_ADMIN, ENTERPRISE_ADMIN, or USER
    val enterpriseId: String = "", // ID of enterprise if user belongs to one
    val companyName: String = "", // For enterprise admin and enterprise users
    val department: String = "", // For enterprise users
    val jobTitle: String = "", // For enterprise users
    val permissions: UserPermission? = null, // User-specific permissions (only for enterprise users)
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "", // UID of who created this user (super admin or enterprise admin)
    val isActive: Boolean = true,
    val expiresAt: Long = 0L // Subscription expiration timestamp (0 = never expires, for super admin)
)

