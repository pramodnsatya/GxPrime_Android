package com.pramod.validator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.data.models.CustomQuestion
import com.pramod.validator.data.models.Question
import com.pramod.validator.data.repository.CustomAssessmentRepository
import com.pramod.validator.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomQuestionnaireViewModel : ViewModel() {
    private val repository = CustomAssessmentRepository()
    private val firebaseRepository = FirebaseRepository()

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
    
    private var assessmentId: String = ""
    private var assessmentName: String = ""
    private var inProgressAssessmentId: String = "" // ID of the in-progress assessment if resuming

    fun loadQuestions(assessmentId: String) {
        this.assessmentId = assessmentId
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = repository.getCustomAssessmentById(assessmentId)
                result.fold(
                    onSuccess = { assessment ->
                        if (assessment != null) {
                            // Convert CustomQuestion to Question for compatibility
                            val questionsList = assessment.questions.map { customQuestion ->
                                Question(
                                    id = customQuestion.id,
                                    domainId = assessmentId, // Use assessmentId as domainId for compatibility
                                    text = customQuestion.questionText,
                                    order = customQuestion.order
                                )
                            }
                            _questions.value = questionsList
                            android.util.Log.d("CustomQuestionnaireViewModel", "✅ Loaded ${questionsList.size} questions from custom assessment")
                        } else {
                            android.util.Log.e("CustomQuestionnaireViewModel", "❌ Assessment not found")
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("CustomQuestionnaireViewModel", "❌ Error loading assessment: ${error.message}", error)
                        com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "load_custom_assessment")
                        com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", assessmentId)
                        com.pramod.validator.utils.CrashReporting.logException(error, "Failed to load custom assessment")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("CustomQuestionnaireViewModel", "❌ Exception loading questions: ${e.message}", e)
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "load_custom_assessment")
                com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", assessmentId)
                com.pramod.validator.utils.CrashReporting.logException(e, "Exception loading custom assessment questions")
            }
            
            _isLoading.value = false
        }
    }

    fun answerQuestion(questionId: String, answer: AnswerType) {
        _responses.value = _responses.value + (questionId to answer)
        
        // Immediately save to in-progress assessment if it exists
        viewModelScope.launch {
            val inProgressId = inProgressAssessmentId
            if (inProgressId.isNotEmpty() && assessmentId.isNotEmpty() && assessmentName.isNotEmpty()) {
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
                userId = firebaseRepository.getCurrentUser()?.uid ?: "",
                assessmentName = assessmentInfo["assessmentName"] ?: assessmentName,
                facilityId = "",
                facilityName = "",
                domainId = "custom",
                domainName = "Custom Assessment",
                subDomainId = assessmentInfo["assessmentId"] ?: assessmentId,
                subDomainName = assessmentInfo["assessmentName"] ?: assessmentName,
                isCustomAssessment = true,
                currentQuestionIndex = _currentQuestionIndex.value,
                totalQuestions = _questions.value.size,
                responses = responsesMap,
                questionTexts = questionTexts,
                updatedAt = System.currentTimeMillis()
            )
            
            val inProgressRepository = com.pramod.validator.data.repository.InProgressAssessmentRepository()
            inProgressRepository.updateInProgressAssessment(inProgressAssessment)
            
            android.util.Log.d("CustomQuestionnaireViewModel", "💾 Auto-saved response for question: $questionId")
        } catch (e: Exception) {
            android.util.Log.e("CustomQuestionnaireViewModel", "❌ Error auto-saving response: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "auto_save_custom_response")
            com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", assessmentId)
            com.pramod.validator.utils.CrashReporting.logException(e, "Failed to auto-save custom assessment response")
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
        if (inProgressId.isNotEmpty() && assessmentId.isNotEmpty() && assessmentName.isNotEmpty()) {
            viewModelScope.launch {
                val assessmentInfo = getAssessmentInfo()
                val questionTexts = getQuestionTextsMap()
                val responsesMap = _responses.value.mapValues { it.value.name }
                
                val inProgressAssessment = com.pramod.validator.data.models.InProgressAssessment(
                    id = inProgressId,
                    userId = firebaseRepository.getCurrentUser()?.uid ?: "",
                    assessmentName = assessmentInfo["assessmentName"] ?: assessmentName,
                    facilityId = "",
                    facilityName = "",
                    domainId = "custom",
                    domainName = "Custom Assessment",
                    subDomainId = assessmentInfo["assessmentId"] ?: assessmentId,
                    subDomainName = assessmentInfo["assessmentName"] ?: assessmentName,
                    isCustomAssessment = true,
                    currentQuestionIndex = _currentQuestionIndex.value,
                    totalQuestions = _questions.value.size,
                    responses = responsesMap,
                    questionTexts = questionTexts,
                    updatedAt = System.currentTimeMillis()
                )
                
                val inProgressRepository = com.pramod.validator.data.repository.InProgressAssessmentRepository()
                inProgressRepository.updateInProgressAssessment(inProgressAssessment)
                
                android.util.Log.d("CustomQuestionnaireViewModel", "💾 Auto-saved progress: index=${_currentQuestionIndex.value}")
            }
        }
    }

    suspend fun saveAllResponses(): Boolean {
        val userId = firebaseRepository.getCurrentUser()?.uid ?: return false
        _isSaving.value = true
        
        // Note: For custom assessments, we don't save individual responses
        // The responses are saved as part of the Report
        _isSaving.value = false
        return true
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
    
    fun setAssessmentInfo(assessmentId: String, assessmentName: String) {
        this.assessmentId = assessmentId
        this.assessmentName = assessmentName
    }
    
    fun restoreFromProgress(
        responses: Map<String, String>, // questionId to AnswerType string
        currentQuestionIndex: Int,
        questionTexts: Map<String, String>,
        inProgressAssessmentId: String = ""
    ) {
        _isRestoring.value = true
        android.util.Log.d("CustomQuestionnaireViewModel", "🔄 Restoring progress: index=$currentQuestionIndex, responses=${responses.size}, assessmentId=$inProgressAssessmentId, questions=${_questions.value.size}")
        android.util.Log.d("CustomQuestionnaireViewModel", "   - Saved responses keys: ${responses.keys.joinToString()}")
        
        // Convert string responses back to AnswerType
        val restoredResponses = responses.mapNotNull { (questionId, answerString) ->
            try {
                val answerType = AnswerType.valueOf(answerString)
                android.util.Log.d("CustomQuestionnaireViewModel", "   - Restored response: $questionId -> $answerString")
                questionId to answerType
            } catch (e: Exception) {
                android.util.Log.w("CustomQuestionnaireViewModel", "Failed to parse answer: $answerString for question: $questionId", e)
                null
            }
        }.toMap()
        
        // Ensure questions are loaded before setting index
        val validIndex = if (_questions.value.isNotEmpty()) {
            currentQuestionIndex.coerceIn(0, _questions.value.size - 1)
        } else {
            android.util.Log.w("CustomQuestionnaireViewModel", "⚠️ Questions not loaded yet, will set index to 0 temporarily")
            0
        }
        
        // Log current question IDs for comparison
        val currentQuestionIds = _questions.value.map { it.id }
        android.util.Log.d("CustomQuestionnaireViewModel", "   - Current question IDs: ${currentQuestionIds.joinToString()}")
        android.util.Log.d("CustomQuestionnaireViewModel", "   - Restored responses keys: ${restoredResponses.keys.joinToString()}")
        
        _responses.value = restoredResponses
        _currentQuestionIndex.value = validIndex
        this.inProgressAssessmentId = inProgressAssessmentId
        
        android.util.Log.d("CustomQuestionnaireViewModel", "✅ Restored: responses=${restoredResponses.size}, currentIndex=${_currentQuestionIndex.value}, assessmentId=$inProgressAssessmentId")
        android.util.Log.d("CustomQuestionnaireViewModel", "   - _responses.value now contains: ${_responses.value.keys.joinToString()}")
        
        // Clear restoring flag after a brief delay to ensure state propagation
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Increased delay to ensure state propagation
            _isRestoring.value = false
            android.util.Log.d("CustomQuestionnaireViewModel", "✅ Restoration complete, isRestoring=false")
        }
    }
    
    fun getInProgressAssessmentId(): String = inProgressAssessmentId
    
    fun getAssessmentInfo(): Map<String, String> {
        return mapOf(
            "assessmentId" to assessmentId,
            "assessmentName" to assessmentName
        )
    }
}

