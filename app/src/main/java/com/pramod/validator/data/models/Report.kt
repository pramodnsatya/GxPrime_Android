package com.pramod.validator.data.models

data class Report(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val userDepartment: String = "", // User's department
    val userJobTitle: String = "", // User's job title/position
    val enterpriseId: String = "", // Enterprise ID if user belongs to one
    val enterpriseName: String = "", // Enterprise name if user belongs to one
    val assessmentName: String = "", // Name given to this assessment
    val facilityId: String = "", // Facility ID where assessment was conducted
    val facilityName: String = "", // Facility name where assessment was conducted
    val domainId: String = "",
    val domainName: String = "",
    val subDomainId: String = "",
    val subDomainName: String = "",
    val totalQuestions: Int = 0,
    val compliantCount: Int = 0,
    val nonCompliantCount: Int = 0,
    val notApplicableCount: Int = 0,
    val completedAt: Long = System.currentTimeMillis(),
    val responses: Map<String, String> = emptyMap(), // questionId to answer mapping
    val questionTexts: Map<String, String> = emptyMap(), // questionId to question text
    val aiSummary: String = "", // AI-generated summary
    val aiSummaryStatus: String = "completed" // "pending", "completed", "failed" - tracks if AI summary needs to be generated
)

