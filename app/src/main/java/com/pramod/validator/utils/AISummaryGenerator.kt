package com.pramod.validator.utils

import android.content.Context
import com.pramod.validator.data.models.Report
import com.pramod.validator.data.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background service to generate AI summaries for reports that were saved offline
 */
object AISummaryGenerator {
    
    private val repository = FirebaseRepository()
    
    /**
     * Generate AI summary for a report that was saved with pending status
     * Only generates if report has question texts (meaning questions were loaded from Firebase)
     */
    suspend fun generatePendingSummary(
        report: Report,
        context: Context
    ): Result<Report> {
        return try {
            // Only generate if we have question texts (questions were loaded from Firebase)
            if (report.questionTexts.isEmpty()) {
                android.util.Log.w("AISummaryGenerator", "⚠️ Skipping AI summary generation: No question texts available (questions not loaded from Firebase)")
                return Result.failure(Exception("Cannot generate AI summary: Questions were not loaded from Firebase"))
            }
            
            android.util.Log.d("AISummaryGenerator", "🔄 Generating AI summary for report: ${report.id}")
            
            // Prepare questions and answers for AI
            val questionsAndAnswers = report.responses.map { (questionId, answerString) ->
                val questionText = report.questionTexts[questionId] ?: "Question text not available"
                questionId to (questionText to answerString)
            }.toMap()
            
            // Generate AI summary
            val result = withContext(Dispatchers.IO) {
                if (report.domainId == "custom") {
                    OpenAIService.generateAssessmentSummary(
                        domainName = "Custom Assessment",
                        subDomainName = report.subDomainName,
                        assessmentName = report.assessmentName,
                        questionsAndAnswers = questionsAndAnswers
                    )
                } else {
                    OpenAIService.generateAssessmentSummary(
                        domainName = report.domainName,
                        subDomainName = report.subDomainName,
                        assessmentName = report.assessmentName,
                        questionsAndAnswers = questionsAndAnswers
                    )
                }
            }
            
            val aiSummary = result.getOrElse { error ->
                android.util.Log.e("AISummaryGenerator", "❌ Failed to generate AI summary: ${error.message}", error)
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "background_ai_summary")
                com.pramod.validator.utils.CrashReporting.setCustomKey("report_id", report.id)
                com.pramod.validator.utils.CrashReporting.logException(error, "Background AI summary generation failed")
                return Result.failure(error)
            }
            
            // Update report with AI summary
            val updatedReport = report.copy(
                aiSummary = aiSummary,
                aiSummaryStatus = "completed"
            )
            
            // Save updated report to Firestore
            val saveResult = repository.saveReport(updatedReport)
            saveResult.fold(
                onSuccess = {
                    android.util.Log.d("AISummaryGenerator", "✅ Successfully generated and saved AI summary for report: ${report.id}")
                    Result.success(updatedReport)
                },
                onFailure = { error ->
                    android.util.Log.e("AISummaryGenerator", "❌ Failed to save updated report: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AISummaryGenerator", "❌ Exception generating AI summary: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.logException(e, "Exception in background AI summary generation")
            Result.failure(e)
        }
    }
    
    /**
     * Check for reports with pending AI summaries and generate them when online
     */
    fun processPendingSummaries(
        context: Context,
        scope: CoroutineScope
    ) {
        scope.launch {
            try {
                val networkMonitor = NetworkMonitor(context)
                val isOnline = networkMonitor.isOnline.first()
                
                if (!isOnline) {
                    android.util.Log.d("AISummaryGenerator", "⚠️ Offline: Skipping pending summary generation")
                    return@launch
                }
                
                android.util.Log.d("AISummaryGenerator", "🔄 Checking for reports with pending AI summaries...")
                
                val currentUser = repository.getCurrentUser()
                if (currentUser == null) {
                    android.util.Log.w("AISummaryGenerator", "⚠️ No current user, skipping")
                    return@launch
                }
                
                // Get user reports
                val reportsResult = repository.getUserReports(currentUser.uid)
                reportsResult.fold(
                    onSuccess = { reports ->
                        val pendingReports = reports.filter { it.aiSummaryStatus == "pending" }
                        android.util.Log.d("AISummaryGenerator", "📋 Found ${pendingReports.size} reports with pending AI summaries")
                        
                        // Process each pending report
                        pendingReports.forEach { report ->
                            launch {
                                generatePendingSummary(report, context)
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("AISummaryGenerator", "❌ Failed to load reports: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AISummaryGenerator", "❌ Exception processing pending summaries: ${e.message}", e)
            }
        }
    }
}

