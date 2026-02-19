package com.pramod.validator.data.models

data class Invitation(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val enterpriseId: String = "",
    val department: String = "",
    val jobTitle: String = "",
    val permissions: UserPermission = UserPermission(),
    val invitedBy: String = "",
    val invitedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
    val isUsed: Boolean = false,
    val usedAt: Long = 0L,
    val token: String = "" // Unique token for the invitation link
)


