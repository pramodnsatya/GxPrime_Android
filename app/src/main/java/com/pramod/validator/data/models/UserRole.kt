package com.pramod.validator.data.models

/**
 * User roles in the system
 * SUPER_ADMIN - Company owner, full control, preset credentials
 * ENTERPRISE_ADMIN - Company account, can create up to 50 users
 * USER - Regular user with access to app features
 */
enum class UserRole {
    SUPER_ADMIN,
    ENTERPRISE_ADMIN,
    USER
}

