package com.pramod.validator.data.models

/**
 * Represents a risk area identified in the FDA 483
 */
data class RiskArea(
    val area: String = "", // Area name (e.g., "Documentation", "Quality Control")
    val description: String = "", // Detailed description of the risk
    val specificDetails: String = "" // More specific details about the risk
)

/**
 * Represents a checklist item to avoid getting flagged again
 */
data class ChecklistItem(
    val item: String = "", // Checklist item description
    val priority: String = "Medium" // High, Medium, Low
)

/**
 * Represents an FDA 483 assessment
 */
data class Fda483Assessment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val fileName: String = "", // Original file name
    val fileSize: Long = 0L, // File size in bytes
    val uploadedAt: Long = System.currentTimeMillis(),
    val processedAt: Long = 0L, // When AI processing completed
    val summary: String = "", // Overall summary of risk areas
    val riskAreas: List<RiskArea> = emptyList(), // List of identified risk areas
    val checklist: List<ChecklistItem> = emptyList(), // Checklist to avoid getting flagged
    val aiAnalysis: String = "", // Full AI analysis JSON
    val status: String = "processing" // processing, completed, failed
)


