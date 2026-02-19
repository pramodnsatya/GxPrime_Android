package com.pramod.validator.data.models

data class Facility(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val enterpriseId: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "", // UID of the Enterprise Admin who created this facility
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "" // UID of the Enterprise Admin who last updated this facility
)
