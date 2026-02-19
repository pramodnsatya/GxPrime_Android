package com.pramod.validator.data.models

import com.google.firebase.firestore.DocumentId

data class CustomAssessment(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val questions: List<CustomQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFromChecklist: Boolean = false, // True if created from FDA 483 checklist
    val sourceFda483AssessmentId: String = "" // ID of the FDA 483 assessment this was created from
)

data class CustomQuestion(
    val id: String = "",
    val questionText: String = "",
    val order: Int = 0
)

