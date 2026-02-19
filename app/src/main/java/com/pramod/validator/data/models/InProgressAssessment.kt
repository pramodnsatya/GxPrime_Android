package com.pramod.validator.data.models

import com.google.firebase.firestore.DocumentId

data class InProgressAssessment(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val assessmentName: String = "",
    val facilityId: String = "",
    val facilityName: String = "",
    val domainId: String = "",
    val domainName: String = "",
    val subDomainId: String = "",
    val subDomainName: String = "",
    val isCustomAssessment: Boolean = false, // true if custom assessment, false if regular domain assessment
    val currentQuestionIndex: Int = 0, // The question index where user left off
    val totalQuestions: Int = 0, // Total number of questions in the assessment
    val responses: Map<String, String> = emptyMap(), // questionId to AnswerType string mapping
    val questionTexts: Map<String, String> = emptyMap(), // questionId to question text mapping
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

