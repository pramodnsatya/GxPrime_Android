package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.data.models.Question
import com.pramod.validator.data.models.Response
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.data.QualityUnitQuestions
import com.pramod.validator.utils.NetworkMonitor
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuestionnaireViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _responses = MutableStateFlow<Map<String, AnswerType>>(emptyMap())
    val responses: StateFlow<Map<String, AnswerType>> = _responses.asStateFlow()
    
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()
    
    private val _questionsLoadedFromFirebase = MutableStateFlow(false)
    val questionsLoadedFromFirebase: StateFlow<Boolean> = _questionsLoadedFromFirebase.asStateFlow()

    private var domainId: String = ""
    private var domainName: String = ""
    private var subDomainName: String = ""
    private var facilityId: String = ""
    private var facilityName: String = ""
    private var assessmentName: String = ""
    private var inProgressAssessmentId: String = "" // ID of the in-progress assessment if resuming

    fun loadQuestions(subDomainId: String, context: Context? = null) {
        this.domainId = subDomainId // Store subDomainId for compatibility
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _questionsLoadedFromFirebase.value = false
            
            try {
                // Check network connectivity first
                val isOnline = context?.let {
                    val networkMonitor = NetworkMonitor(it)
                    networkMonitor.isCurrentlyOnline()
                } ?: true // Default to true if context not provided
                
                if (!isOnline) {
                    android.util.Log.w("QuestionnaireViewModel", "⚠️ Offline: Cannot load questions without internet")
                    _loadError.value = "Please connect to the internet to load assessment questions."
                    _questions.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                // Load from Firebase - force server fetch when online
                android.util.Log.d("QuestionnaireViewModel", "🔄 Loading questions for subDomainId: $subDomainId (online: $isOnline)")
                val result = repository.getQuestionsBySubDomain(subDomainId, forceServer = isOnline)
                
                if (result.isSuccess) {
                    val firebaseQuestions = result.getOrNull() ?: emptyList()
                    if (firebaseQuestions.isNotEmpty()) {
                        android.util.Log.d("QuestionnaireViewModel", "✅ Loaded ${firebaseQuestions.size} questions from Firebase")
                        _questions.value = firebaseQuestions
                        _questionsLoadedFromFirebase.value = true
                    } else {
                        // Firebase returned empty - this could mean:
                        // 1. No questions exist in database for this subDomainId
                        // 2. Questions exist but field name mismatch
                        android.util.Log.w("QuestionnaireViewModel", "⚠️ Firebase returned empty questions for subDomainId: $subDomainId")
                        android.util.Log.w("QuestionnaireViewModel", "⚠️ This might indicate: 1) No questions in database, 2) Field name mismatch, 3) Wrong subDomainId")
                        _loadError.value = "No questions found for this assessment. Please contact support or try again later."
                        _questions.value = emptyList()
                    }
                } else {
                    // Firebase error
                    val error = result.exceptionOrNull()
                    android.util.Log.e("QuestionnaireViewModel", "❌ Firebase error loading questions: ${error?.message}", error)
                    
                    // Check if it's a network-related error
                    val isNetworkError = error is com.google.firebase.firestore.FirebaseFirestoreException &&
                        (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ||
                         error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    
                    if (isNetworkError) {
                        _loadError.value = "Failed to load questions. Please check your internet connection and try again."
                    } else {
                        _loadError.value = "An error occurred while loading questions. Please try again or contact support."
                    }
                    _questions.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("QuestionnaireViewModel", "❌ Exception loading questions: ${e.message}", e)
                _loadError.value = "An error occurred while loading questions. Please try again."
                _questions.value = emptyList()
            }
            
            _isLoading.value = false
        }
    }
    
    private fun getSampleQuestions(subDomainId: String): List<Question> {
        // Check if this is a Quality Unit, Packaging & Labeling, Production, Materials, Laboratory, or Facilities & Equipment subdomain and use specific questions
        if (subDomainId.startsWith("qu_") || subDomainId.startsWith("pl_") || subDomainId.startsWith("pr_") || subDomainId.startsWith("mt_") || subDomainId.startsWith("lab_") || subDomainId.startsWith("fe_")) {
            val qualityQuestions = QualityUnitQuestions.getQuestionsForSubDomain(subDomainId)
            if (qualityQuestions.isNotEmpty()) {
                android.util.Log.d("QuestionnaireViewModel", "✅ Using specific questions for $subDomainId")
                return qualityQuestions
            }
        }
        
        // Fallback to generic questions for other domains
        android.util.Log.d("QuestionnaireViewModel", "⚠️ Using generic questions for $subDomainId")
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

    fun answerQuestion(questionId: String, answer: AnswerType) {
        _responses.value = _responses.value + (questionId to answer)
        
        // Immediately save to in-progress assessment if it exists
        viewModelScope.launch {
            val inProgressId = inProgressAssessmentId
            if (inProgressId.isNotEmpty() && domainId.isNotEmpty() && assessmentName.isNotEmpty()) {
                saveProgressToFirestore(questionId, answer)
            }
        }
    }
    
    private suspend fun saveProgressToFirestore(questionId: String, answer: AnswerType) {
        try {
            val inProgressId = inProgressAssessmentId
            if (inProgressId.isEmpty()) return
            
            val assessmentInfo = getAssessmentInfo()
            val questionTexts = getQuestionTextsMap()
            val responsesMap = _responses.value.mapValues { it.value.name }
            
            val inProgressAssessment = com.pramod.validator.data.models.InProgressAssessment(
                id = inProgressId,
                userId = repository.getCurrentUser()?.uid ?: "",
                assessmentName = assessmentInfo["assessmentName"] ?: assessmentName,
                facilityId = assessmentInfo["facilityId"] ?: facilityId,
                facilityName = assessmentInfo["facilityName"] ?: facilityName,
                domainId = assessmentInfo["domainId"] ?: domainId.ifEmpty { "unknown" },
                domainName = assessmentInfo["domainName"] ?: domainName.ifEmpty { "Unknown Domain" },
                subDomainId = domainId, // Using domainId as subDomainId for compatibility
                subDomainName = assessmentInfo["subDomainName"] ?: subDomainName,
                isCustomAssessment = false,
                currentQuestionIndex = _currentQuestionIndex.value,
                totalQuestions = _questions.value.size,
                responses = responsesMap,
                questionTexts = questionTexts,
                updatedAt = System.currentTimeMillis()
            )
            
            val inProgressRepository = com.pramod.validator.data.repository.InProgressAssessmentRepository()
            inProgressRepository.updateInProgressAssessment(inProgressAssessment)
            
            android.util.Log.d("QuestionnaireViewModel", "💾 Auto-saved response for question: $questionId")
        } catch (e: Exception) {
            android.util.Log.e("QuestionnaireViewModel", "❌ Error auto-saving response: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to auto-save response for question: $questionId")
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            _currentQuestionIndex.value++
            // Save progress when navigating to next question
            saveCurrentProgress()
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
            // Save progress when navigating to previous question
            saveCurrentProgress()
        }
    }
    
    private fun saveCurrentProgress() {
        val inProgressId = inProgressAssessmentId
        if (inProgressId.isNotEmpty() && domainId.isNotEmpty() && assessmentName.isNotEmpty()) {
            viewModelScope.launch {
                val assessmentInfo = getAssessmentInfo()
                val questionTexts = getQuestionTextsMap()
                val responsesMap = _responses.value.mapValues { it.value.name }
                
                val inProgressAssessment = com.pramod.validator.data.models.InProgressAssessment(
                    id = inProgressId,
                    userId = repository.getCurrentUser()?.uid ?: "",
                    assessmentName = assessmentInfo["assessmentName"] ?: assessmentName,
                    facilityId = assessmentInfo["facilityId"] ?: facilityId,
                    facilityName = assessmentInfo["facilityName"] ?: facilityName,
                    domainId = assessmentInfo["domainId"] ?: domainId.ifEmpty { "unknown" },
                    domainName = assessmentInfo["domainName"] ?: domainName.ifEmpty { "Unknown Domain" },
                    subDomainId = domainId,
                    subDomainName = assessmentInfo["subDomainName"] ?: subDomainName,
                    isCustomAssessment = false,
                    currentQuestionIndex = _currentQuestionIndex.value,
                    totalQuestions = _questions.value.size,
                    responses = responsesMap,
                    questionTexts = questionTexts,
                    updatedAt = System.currentTimeMillis()
                )
                
                val inProgressRepository = com.pramod.validator.data.repository.InProgressAssessmentRepository()
                inProgressRepository.updateInProgressAssessment(inProgressAssessment)
                
                android.util.Log.d("QuestionnaireViewModel", "💾 Auto-saved progress: index=${_currentQuestionIndex.value}")
            }
        }
    }

    suspend fun saveAllResponses(): Boolean {
        val userId = repository.getCurrentUser()?.uid ?: return false
        _isSaving.value = true
        
        var allSuccess = true
        _responses.value.forEach { (questionId, answer) ->
            val response = Response(
                id = "${userId}_${questionId}_${System.currentTimeMillis()}",
                userId = userId,
                questionId = questionId,
                domainId = domainId,
                answer = answer,
                timestamp = System.currentTimeMillis()
            )
            
            val result = repository.saveResponse(response)
            if (result.isFailure) {
                allSuccess = false
            }
        }
        
        _isSaving.value = false
        return allSuccess
    }

    fun getCurrentQuestion(): Question? {
        return _questions.value.getOrNull(_currentQuestionIndex.value)
    }

    fun isLastQuestion(): Boolean {
        return _currentQuestionIndex.value == _questions.value.size - 1
    }

    fun isFirstQuestion(): Boolean {
        return _currentQuestionIndex.value == 0
    }

    fun hasAnsweredCurrentQuestion(): Boolean {
        val currentQuestion = getCurrentQuestion()
        return currentQuestion != null && _responses.value.containsKey(currentQuestion.id)
    }
    
    fun getQuestionTextsMap(): Map<String, String> {
        return _questions.value.associate { it.id to it.text }
    }
    
    fun hasProgress(): Boolean {
        return _responses.value.isNotEmpty()
    }
    
    fun setAssessmentInfo(
        domainId: String,
        domainName: String,
        subDomainName: String,
        facilityId: String,
        facilityName: String,
        assessmentName: String
    ) {
        this.domainId = domainId
        this.domainName = domainName
        this.subDomainName = subDomainName
        this.facilityId = facilityId
        this.facilityName = facilityName
        this.assessmentName = assessmentName
    }
    
    fun restoreFromProgress(
        responses: Map<String, String>, // questionId to AnswerType string
        currentQuestionIndex: Int,
        questionTexts: Map<String, String>,
        inProgressAssessmentId: String = ""
    ) {
        _isRestoring.value = true
        android.util.Log.d("QuestionnaireViewModel", "🔄 Restoring progress: index=$currentQuestionIndex, responses=${responses.size}, assessmentId=$inProgressAssessmentId, questions=${_questions.value.size}")
        android.util.Log.d("QuestionnaireViewModel", "   - Saved responses keys: ${responses.keys.joinToString()}")
        
        // Convert string responses back to AnswerType
        val restoredResponses = responses.mapNotNull { (questionId, answerString) ->
            try {
                val answerType = AnswerType.valueOf(answerString)
                android.util.Log.d("QuestionnaireViewModel", "   - Restored response: $questionId -> $answerString")
                questionId to answerType
            } catch (e: Exception) {
                android.util.Log.w("QuestionnaireViewModel", "Failed to parse answer: $answerString for question: $questionId", e)
                null
            }
        }.toMap()
        
        // Ensure questions are loaded before setting index
        val validIndex = if (_questions.value.isNotEmpty()) {
            currentQuestionIndex.coerceIn(0, _questions.value.size - 1)
        } else {
            android.util.Log.w("QuestionnaireViewModel", "⚠️ Questions not loaded yet, will set index to 0 temporarily")
            0
        }
        
        // Log current question IDs for comparison
        val currentQuestionIds = _questions.value.map { it.id }
        android.util.Log.d("QuestionnaireViewModel", "   - Current question IDs: ${currentQuestionIds.joinToString()}")
        android.util.Log.d("QuestionnaireViewModel", "   - Restored responses keys: ${restoredResponses.keys.joinToString()}")
        
        _responses.value = restoredResponses
        _currentQuestionIndex.value = validIndex
        this.inProgressAssessmentId = inProgressAssessmentId
        
        android.util.Log.d("QuestionnaireViewModel", "✅ Restored: responses=${restoredResponses.size}, currentIndex=${_currentQuestionIndex.value}, assessmentId=$inProgressAssessmentId")
        android.util.Log.d("QuestionnaireViewModel", "   - _responses.value now contains: ${_responses.value.keys.joinToString()}")
        
        // Clear restoring flag after a brief delay to ensure state propagation
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Increased delay to ensure state propagation
            _isRestoring.value = false
            android.util.Log.d("QuestionnaireViewModel", "✅ Restoration complete, isRestoring=false")
        }
    }
    
    fun getInProgressAssessmentId(): String = inProgressAssessmentId
    
    fun getAssessmentInfo(): Map<String, String> {
        return mapOf(
            "domainId" to domainId,
            "domainName" to domainName,
            "subDomainName" to subDomainName,
            "facilityId" to facilityId,
            "facilityName" to facilityName,
            "assessmentName" to assessmentName
        )
    }
}

