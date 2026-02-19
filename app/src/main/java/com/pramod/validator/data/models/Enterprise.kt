package com.pramod.validator.data.models

/**
 * Enterprise/Company account
 * Created by Super Admin
 * Can have up to 50 users
 */
data class Enterprise(
    val id: String = "",
    val companyName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val adminEmail: String = "", // Enterprise admin's email
    val adminName: String = "", // Enterprise admin's name
    val adminUid: String = "", // Enterprise admin's user ID
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "", // Super admin UID who created this
    val userLimit: Int = 50, // Max users allowed
    val currentUserCount: Int = 0, // Current number of users
    val isActive: Boolean = true,
    val address: String = "",
    val industry: String = "",
    val expiresAt: Long = 0L // Subscription expiration timestamp
)

