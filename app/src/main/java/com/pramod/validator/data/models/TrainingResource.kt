package com.pramod.validator.data.models

/**
 * Represents a reusable training resource (article, FDA 483, PDF, video, etc.)
 * that can be uploaded by a Super Admin and consumed by end users.
 */
data class TrainingResource(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: TrainingResourceType = TrainingResourceType.ARTICLE,
    val resourceUrl: String = "",
    val videoUrl: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val storagePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val tags: List<String> = emptyList()
)

enum class TrainingResourceType {
    ARTICLE,
    FDA483,
    VIDEO,
    PDF,
    OTHER
}

