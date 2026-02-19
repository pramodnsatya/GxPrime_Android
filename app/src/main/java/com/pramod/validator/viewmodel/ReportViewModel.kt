package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.data.models.Question
import com.pramod.validator.data.models.Report
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.data.QualityUnitQuestions
import com.pramod.validator.utils.OpenAIService
import com.pramod.validator.utils.NetworkMonitor
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class ReportViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    
    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _aiSummaryPending = MutableStateFlow(false)
    val aiSummaryPending: StateFlow<Boolean> = _aiSummaryPending.asStateFlow()
    
    // Helper function to get sample questions (same as QuestionnaireViewModel)
    private fun getSampleQuestions(subDomainId: String): List<Question> {
        // Check if this is a Quality Unit, Packaging & Labeling, Production, Materials, Laboratory, or Facilities & Equipment subdomain and use specific questions
        if (subDomainId.startsWith("qu_") || subDomainId.startsWith("pl_") || subDomainId.startsWith("pr_") || subDomainId.startsWith("mt_") || subDomainId.startsWith("lab_") || subDomainId.startsWith("fe_")) {
            val qualityQuestions = QualityUnitQuestions.getQuestionsForSubDomain(subDomainId)
            if (qualityQuestions.isNotEmpty()) {
                android.util.Log.d("ReportViewModel", "✅ Using specific questions for $subDomainId")
                return qualityQuestions
            }
        }
        
        // Fallback to generic questions for other domains
        android.util.Log.d("ReportViewModel", "⚠️ Using generic questions for $subDomainId")
        return listOf(
            Question("${subDomainId}_1", subDomainId, "Are all procedures documented and current?", 1),
            Question("${subDomainId}_2", subDomainId, "Is equipment properly qualified and maintained?", 2),
            Question("${subDomainId}_3", subDomainId, "Are records complete and reviewed on time?", 3),
            Question("${subDomainId}_4", subDomainId, "Is training provided and documented for staff?", 4),
            Question("${subDomainId}_5", subDomainId, "Are deviations investigated and documented?", 5),
            Question("${subDomainId}_6", subDomainId, "Is there proper environmental monitoring?", 6),
            Question("${subDomainId}_7", subDomainId, "Are validation protocols followed and documented?", 7),
            Question("${subDomainId}_8", subDomainId, "Is data integrity maintained and verified?", 8),
            Question("${subDomainId}_9", subDomainId, "Are change controls properly implemented?", 9),
            Question("${subDomainId}_10", subDomainId, "Are audits conducted and findings addressed?", 10)
        )
    }

    fun generateReport(
        domainId: String,
        domainName: String,
        subDomainId: String,
        subDomainName: String,
        assessmentName: String,
        facilityId: String,
        facilityName: String,
        responses: Map<String, AnswerType>,
        questionTexts: Map<String, String> = emptyMap(), // Use question texts already loaded during assessment
        context: Context? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val currentFirebaseUser = repository.getCurrentUser()
                if (currentFirebaseUser == null) {
                    android.util.Log.e("ReportViewModel", "❌ No current user found")
                    _isLoading.value = false
                    return@launch
                }
                
                val userId = currentFirebaseUser.uid
                
                // Get full user object from Firestore to access all properties
                val currentUserResult = repository.getUserById(userId)
                val currentUser = currentUserResult.getOrNull()
                
                // Get user's display name (tries Firebase Auth, then Firestore)
                val userName = currentUser?.displayName ?: repository.getUserDisplayName(userId)
                
                // Get enterprise context from Firestore user data
                val (enterpriseId, enterpriseName) = withContext(Dispatchers.IO) {
                    try {
                        val userResult = repository.getUserById(userId)
                        val user = userResult.getOrNull()
                        Pair(user?.enterpriseId ?: "", user?.companyName ?: "")
                    } catch (e: Exception) {
                        Pair("", "")
                    }
                }
                
                val compliantCount = responses.values.count { it == AnswerType.COMPLIANT }
                val nonCompliantCount = responses.values.count { it == AnswerType.NON_COMPLIANT }
                val notApplicableCount = responses.values.count { it == AnswerType.NOT_APPLICABLE }
                
                // Convert responses to string map for Firestore
                val responsesMap = responses.mapValues { it.value.name }
                
                // Check network connectivity first
                val isOnline = context?.let { 
                    val networkMonitor = NetworkMonitor(it)
                    networkMonitor.isCurrentlyOnline()
                } ?: true // Default to true if context not provided (for backward compatibility)
                
                // Use question texts already loaded during assessment, or try to load from Firebase if not provided
                val completeQuestionTexts = if (questionTexts.isNotEmpty()) {
                    // Use the question texts that were already loaded during the assessment
                    android.util.Log.d("ReportViewModel", "✅ Using ${questionTexts.size} question texts from assessment")
                    questionTexts
                } else if (!isOnline) {
                    // Offline and no question texts provided: Can't load questions, return empty map
                    // This will trigger the "pending" status for AI summary
                    android.util.Log.w("ReportViewModel", "⚠️ Offline: No question texts provided. Will mark AI summary as pending.")
                    emptyMap<String, String>()
                } else {
                    // Online but no question texts provided: Try to load questions from Firebase
                    // Use withTimeout to prevent hanging
                    try {
                        withContext(Dispatchers.IO) {
                            withTimeout(10000) { // 10 second timeout
                                val questionsResult = repository.getQuestionsBySubDomain(subDomainId)
                                if (questionsResult.isSuccess) {
                                    val fbQuestions = questionsResult.getOrNull() ?: emptyList()
                                    if (fbQuestions.isNotEmpty()) {
                                        android.util.Log.d("ReportViewModel", "✅ Loaded ${fbQuestions.size} questions from Firebase for report")
                                        fbQuestions.associate { it.id to it.text }
                                    } else {
                                        android.util.Log.w("ReportViewModel", "⚠️ Firebase returned empty questions")
                                        emptyMap<String, String>()
                                    }
                                } else {
                                    val error = questionsResult.exceptionOrNull()
                                    android.util.Log.e("ReportViewModel", "❌ Firebase error: ${error?.message}")
                                    com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "load_questions_for_report")
                                    com.pramod.validator.utils.CrashReporting.setCustomKey("subdomain_id", subDomainId)
                                    error?.let { com.pramod.validator.utils.CrashReporting.logException(it, "Failed to load questions for report") }
                                    emptyMap<String, String>()
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        android.util.Log.w("ReportViewModel", "⚠️ Timeout loading questions, proceeding without them")
                        emptyMap<String, String>()
                    } catch (e: Exception) {
                        android.util.Log.e("ReportViewModel", "❌ Exception: ${e.message}", e)
                        com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "load_questions_for_report")
                        com.pramod.validator.utils.CrashReporting.setCustomKey("subdomain_id", subDomainId)
                        com.pramod.validator.utils.CrashReporting.logException(e, "Exception loading questions for report")
                        emptyMap<String, String>()
                    }
                }
                
                // Check if a report already exists for this assessment to prevent duplicates
                val existingReportResult = repository.findExistingReport(
                    userId = userId,
                    assessmentName = assessmentName,
                    subDomainId = subDomainId,
                    facilityId = facilityId,
                    domainId = domainId
                )
                
                val existingReport = existingReportResult.getOrNull()
                
                // If report exists and already has AI summary completed, use it
                // If report exists with pending status, we'll update it with the AI summary
                if (existingReport != null) {
                    android.util.Log.d("ReportViewModel", "📋 Found existing report: ${existingReport.id}, aiSummaryStatus: ${existingReport.aiSummaryStatus}")
                    
                    // If AI summary is already completed, use the existing report
                    if (existingReport.aiSummaryStatus == "completed" && existingReport.aiSummary.isNotEmpty()) {
                        android.util.Log.d("ReportViewModel", "✅ Existing report already has AI summary, using it")
                        _report.value = existingReport
                        _isLoading.value = false
                        return@launch
                    }
                    
                    // If report exists with pending status and we're online with question texts, generate AI summary
                    if (existingReport.aiSummaryStatus == "pending" && isOnline && completeQuestionTexts.isNotEmpty()) {
                        android.util.Log.d("ReportViewModel", "🔄 Existing report has pending AI summary, generating now...")
                        // Continue to generate AI summary and update the existing report
                    } else if (existingReport.aiSummaryStatus == "pending") {
                        // Report exists with pending status, but we can't generate AI summary yet
                        android.util.Log.d("ReportViewModel", "⏳ Existing report has pending AI summary, will be generated later")
                        _report.value = existingReport
                        _isLoading.value = false
                        return@launch
                    }
                }
                
                // Determine AI summary status based on online status and question texts availability
                // If offline, mark as pending immediately (don't try to generate)
                val (aiSummary, aiSummaryStatus) = if (!isOnline) {
                    // Offline: Save report without AI summary, mark as pending for background generation
                    android.util.Log.w("ReportViewModel", "⚠️ Offline: Saving report without AI summary. Will generate later when online.")
                    _aiSummaryPending.value = true
                    Pair("", "pending")
                } else if (completeQuestionTexts.isNotEmpty()) {
                    // Online and have question texts: Generate AI summary
                    val summary = withContext(Dispatchers.IO) {
                        try {
                            android.util.Log.d("ReportViewModel", "🤖 Starting AI summary generation...")
                            
                            // Prepare ALL questions and answers for AI with FULL TEXT
                            val questionsAndAnswers = responses.map { (questionId, answer) ->
                                val questionText = completeQuestionTexts[questionId] ?: "Question text not available"
                                questionId to (questionText to answer.name)
                            }.toMap()
                            
                            android.util.Log.d("ReportViewModel", "📊 Prepared ${questionsAndAnswers.size} questions for AI analysis")
                            
                            val result = OpenAIService.generateAssessmentSummary(
                                domainName = domainName,
                                subDomainName = subDomainName,
                                assessmentName = assessmentName,
                                questionsAndAnswers = questionsAndAnswers
                            )
                            
                            val summaryText = result.getOrElse { error ->
                                android.util.Log.e("ReportViewModel", "❌ AI summary generation failed: ${error.message}", error)
                                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_ai_summary")
                                com.pramod.validator.utils.CrashReporting.setCustomKey("domain", domainName)
                                com.pramod.validator.utils.CrashReporting.setCustomKey("subdomain", subDomainName)
                                com.pramod.validator.utils.CrashReporting.logException(error, "AI summary generation failed")
                                "AI Summary generation failed: ${error.message}. Manual review recommended for compliance gaps."
                            }
                            
                            android.util.Log.d("ReportViewModel", "✅ AI summary generated: ${summaryText.take(100)}...")
                            summaryText
                        } catch (e: Exception) {
                            android.util.Log.e("ReportViewModel", "❌ Exception during AI summary generation: ${e.message}", e)
                            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_ai_summary_exception")
                            com.pramod.validator.utils.CrashReporting.setCustomKey("domain", domainName)
                            com.pramod.validator.utils.CrashReporting.logException(e, "Exception during AI summary generation")
                            "AI Summary unavailable. Manual review recommended."
                        }
                    }
                    Pair(summary, "completed")
                } else {
                    // Online but no question texts available: Don't generate AI summary
                    android.util.Log.w("ReportViewModel", "⚠️ No question texts available: Questions were not loaded from Firebase. Skipping AI summary.")
                    Pair("", "failed")
                }
                
                // Use existing report ID if found, otherwise create new one
                val reportId = existingReport?.id ?: "${userId}_${domainId}_${subDomainId}_${System.currentTimeMillis()}"
                
                val report = Report(
                    id = reportId,
                    userId = userId,
                    userEmail = currentUser?.email ?: currentFirebaseUser.email ?: "",
                    userName = userName,
                    userDepartment = currentUser?.department ?: "",
                    userJobTitle = currentUser?.jobTitle ?: "",
                    enterpriseId = enterpriseId,
                    enterpriseName = enterpriseName,
                    assessmentName = assessmentName,
                    facilityId = facilityId,
                    facilityName = facilityName,
                    domainId = domainId,
                    domainName = domainName,
                    subDomainId = subDomainId,
                    subDomainName = subDomainName,
                    totalQuestions = responses.size,
                    compliantCount = compliantCount,
                    nonCompliantCount = nonCompliantCount,
                    notApplicableCount = notApplicableCount,
                    completedAt = existingReport?.completedAt ?: System.currentTimeMillis(), // Preserve original completion time if updating
                    responses = responsesMap,
                    questionTexts = completeQuestionTexts,
                    aiSummary = aiSummary,
                    aiSummaryStatus = aiSummaryStatus
                )
                
                // Set report immediately so UI can show it (especially important when offline)
                _report.value = report
                
                // Save to Firestore (will use offline persistence if offline)
                repository.saveReport(report)
            } catch (e: Exception) {
                android.util.Log.e("ReportViewModel", "❌ Error generating report: ${e.message}", e)
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_report")
                com.pramod.validator.utils.CrashReporting.setCustomKey("domain", domainName)
                com.pramod.validator.utils.CrashReporting.logException(e, "Error generating report")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateCustomAssessmentReport(
        assessmentId: String,
        assessmentName: String,
        responses: Map<String, AnswerType>,
        questionTexts: Map<String, String>,
        context: Context? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val currentFirebaseUser = repository.getCurrentUser()
                if (currentFirebaseUser == null) {
                    android.util.Log.e("ReportViewModel", "❌ No current user found")
                    _isLoading.value = false
                    return@launch
                }
                
                val userId = currentFirebaseUser.uid
                
                // Get full user object from Firestore to access all properties
                val currentUserResult = repository.getUserById(userId)
                val currentUser = currentUserResult.getOrNull()
                
                // Get user's display name (tries Firebase Auth, then Firestore)
                val userName = currentUser?.displayName ?: repository.getUserDisplayName(userId)
                
                // Get enterprise context from Firestore user data
                val (enterpriseId, enterpriseName) = withContext(Dispatchers.IO) {
                    try {
                        val userResult = repository.getUserById(userId)
                        val user = userResult.getOrNull()
                        Pair(user?.enterpriseId ?: "", user?.companyName ?: "")
                    } catch (e: Exception) {
                        Pair("", "")
                    }
                }
                
                val compliantCount = responses.values.count { it == AnswerType.COMPLIANT }
                val nonCompliantCount = responses.values.count { it == AnswerType.NON_COMPLIANT }
                val notApplicableCount = responses.values.count { it == AnswerType.NOT_APPLICABLE }
                
                // Convert responses to string map for Firestore
                val responsesMap = responses.mapValues { it.value.name }
                
                // Check network connectivity before generating AI summary
                val isOnline = context?.let { 
                    val networkMonitor = NetworkMonitor(it)
                    networkMonitor.isCurrentlyOnline()
                } ?: true // Default to true if context not provided
                
                // Check if a report already exists for this assessment to prevent duplicates
                val existingReportResult = repository.findExistingReport(
                    userId = userId,
                    assessmentName = assessmentName,
                    subDomainId = assessmentId,
                    facilityId = "",
                    domainId = "custom"
                )
                
                val existingReport = existingReportResult.getOrNull()
                
                // If report exists and already has AI summary completed, use it
                if (existingReport != null) {
                    android.util.Log.d("ReportViewModel", "📋 Found existing custom report: ${existingReport.id}, aiSummaryStatus: ${existingReport.aiSummaryStatus}")
                    
                    if (existingReport.aiSummaryStatus == "completed" && existingReport.aiSummary.isNotEmpty()) {
                        android.util.Log.d("ReportViewModel", "✅ Existing custom report already has AI summary, using it")
                        _report.value = existingReport
                        _isLoading.value = false
                        return@launch
                    }
                    
                    if (existingReport.aiSummaryStatus == "pending" && !isOnline) {
                        android.util.Log.d("ReportViewModel", "⏳ Existing custom report has pending AI summary, will be generated later")
                        _report.value = existingReport
                        _isLoading.value = false
                        return@launch
                    }
                }
                
                val (aiSummary, aiSummaryStatus) = if (isOnline) {
                    // Generate AI summary
                    val summary = withContext(Dispatchers.IO) {
                        try {
                            android.util.Log.d("ReportViewModel", "🤖 Starting AI summary generation for custom assessment...")
                            
                            // Prepare ALL questions and answers for AI
                            val questionsAndAnswers = responses.map { (questionId, answer) ->
                                val questionText = questionTexts[questionId] ?: "Question text not available"
                                questionId to (questionText to answer.name)
                            }.toMap()
                            
                            android.util.Log.d("ReportViewModel", "📊 Prepared ${questionsAndAnswers.size} questions for AI analysis")
                            
                            val result = OpenAIService.generateAssessmentSummary(
                                domainName = "Custom Assessment",
                                subDomainName = assessmentName,
                                assessmentName = assessmentName,
                                questionsAndAnswers = questionsAndAnswers
                            )
                            
                            val summaryText = result.getOrElse { error ->
                                android.util.Log.e("ReportViewModel", "❌ AI summary generation failed: ${error.message}", error)
                                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_ai_summary_custom")
                                com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_name", assessmentName)
                                com.pramod.validator.utils.CrashReporting.logException(error, "AI summary generation failed for custom assessment")
                                "AI Summary generation failed: ${error.message}. Manual review recommended for compliance gaps."
                            }
                            
                            android.util.Log.d("ReportViewModel", "✅ AI summary generated: ${summaryText.take(100)}...")
                            summaryText
                        } catch (e: Exception) {
                            android.util.Log.e("ReportViewModel", "❌ Exception during AI summary generation: ${e.message}", e)
                            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_ai_summary_custom_exception")
                            com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_name", assessmentName)
                            com.pramod.validator.utils.CrashReporting.logException(e, "Exception during custom assessment AI summary generation")
                            "AI Summary unavailable. Manual review recommended."
                        }
                    }
                    Pair(summary, "completed")
                } else {
                    // Offline: Save report without AI summary, mark as pending
                    android.util.Log.w("ReportViewModel", "⚠️ Offline: Saving custom assessment report without AI summary. Will generate later.")
                    _aiSummaryPending.value = true
                    Pair("", "pending")
                }
                
                // Use existing report ID if found, otherwise create new one
                val reportId = existingReport?.id ?: "${userId}_custom_${assessmentId}_${System.currentTimeMillis()}"
                
                val report = Report(
                    id = reportId,
                    userId = userId,
                    userEmail = currentUser?.email ?: currentFirebaseUser.email ?: "",
                    userName = userName,
                    userDepartment = currentUser?.department ?: "",
                    userJobTitle = currentUser?.jobTitle ?: "",
                    enterpriseId = enterpriseId,
                    enterpriseName = enterpriseName,
                    assessmentName = assessmentName,
                    facilityId = "",
                    facilityName = "",
                    domainId = "custom",
                    domainName = "Custom Assessment",
                    subDomainId = assessmentId,
                    subDomainName = assessmentName,
                    totalQuestions = responses.size,
                    compliantCount = compliantCount,
                    nonCompliantCount = nonCompliantCount,
                    notApplicableCount = notApplicableCount,
                    completedAt = existingReport?.completedAt ?: System.currentTimeMillis(), // Preserve original completion time if updating
                    responses = responsesMap,
                    questionTexts = questionTexts,
                    aiSummary = aiSummary,
                    aiSummaryStatus = aiSummaryStatus
                )
                
                // Set report immediately so UI can show it (especially important when offline)
                _report.value = report
                
                // Save to Firestore (will use offline persistence if offline)
                repository.saveReport(report)
            } catch (e: Exception) {
                android.util.Log.e("ReportViewModel", "❌ Error generating custom assessment report: ${e.message}", e)
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_custom_report")
                com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_name", assessmentName)
                com.pramod.validator.utils.CrashReporting.logException(e, "Error generating custom assessment report")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCompliancePercentage(): Float {
        val report = _report.value ?: return 0f
        val total = report.compliantCount + report.nonCompliantCount
        return if (total > 0) {
            (report.compliantCount.toFloat() / total) * 100
        } else {
            0f
        }
    }
}

