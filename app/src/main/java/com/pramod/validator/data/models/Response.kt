package com.pramod.validator.data.models

data class Response(
    val id: String = "",
    val userId: String = "",
    val questionId: String = "",
    val domainId: String = "",
    val answer: AnswerType = AnswerType.NOT_APPLICABLE,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AnswerType {
    COMPLIANT,
    NON_COMPLIANT,
    NOT_APPLICABLE
}

