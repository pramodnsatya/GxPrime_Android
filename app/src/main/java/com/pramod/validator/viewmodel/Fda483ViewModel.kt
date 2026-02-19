package com.pramod.validator.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pramod.validator.data.models.Fda483Assessment
import com.pramod.validator.data.models.RiskArea
import com.pramod.validator.data.models.ChecklistItem
import com.pramod.validator.data.repository.Fda483Repository
import com.pramod.validator.data.repository.FirebaseRepository
import com.pramod.validator.utils.OpenAIService
import com.pramod.validator.utils.PdfTextExtractor
import com.pramod.validator.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class Fda483ViewModel : ViewModel() {
    private val repository = Fda483Repository()
    private val firebaseRepository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _assessments = MutableStateFlow<List<Fda483Assessment>>(emptyList())
    val assessments: StateFlow<List<Fda483Assessment>> = _assessments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _currentAssessment = MutableStateFlow<Fda483Assessment?>(null)
    val currentAssessment: StateFlow<Fda483Assessment?> = _currentAssessment.asStateFlow()
    
    private val _completedAssessmentId = MutableStateFlow<String?>(null)
    val completedAssessmentId: StateFlow<String?> = _completedAssessmentId.asStateFlow()
    
    private val _shouldShowHistoryTab = MutableStateFlow(false)
    val shouldShowHistoryTab: StateFlow<Boolean> = _shouldShowHistoryTab.asStateFlow()
    
    init {
        loadUserAssessments()
    }
    
    fun loadUserAssessments() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = repository.getUserAssessments(currentUser.uid)
                result.fold(
                    onSuccess = { assessments ->
                        _assessments.value = assessments
                    },
                    onFailure = { error ->
                        android.util.Log.e("Fda483ViewModel", "Error loading assessments: ${error.message}", error)
                        _errorMessage.value = "Failed to load assessments: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("Fda483ViewModel", "Error loading assessments: ${e.message}", e)
                _errorMessage.value = "Failed to load assessments: ${e.message}"
            }
        }
    }
    
    fun uploadAndProcessPdf(context: Context, uri: Uri, fileName: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                android.util.Log.d("Fda483ViewModel", "Starting PDF upload process...")
                android.util.Log.d("Fda483ViewModel", "URI: $uri, FileName: $fileName")
                // Get file size
                val fileSize = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.available().toLong()
                    } ?: 0L
                }
                
                // Extract text from PDF
                android.util.Log.d("Fda483ViewModel", "Starting PDF text extraction...")
                val pdfTextResult = withContext(Dispatchers.IO) {
                    PdfTextExtractor.extractTextFromUri(context, uri)
                }
                
                val pdfText = pdfTextResult.getOrElse { error ->
                    android.util.Log.e("Fda483ViewModel", "PDF extraction failed: ${error.message}", error)
                    _isLoading.value = false
                    _errorMessage.value = "Failed to extract text from PDF: ${error.message}"
                    return@launch
                }
                
                android.util.Log.d("Fda483ViewModel", "PDF text extracted successfully. Length: ${pdfText.length}")
                
                if (pdfText.isBlank()) {
                    _isLoading.value = false
                    _errorMessage.value = "PDF appears to be empty or could not be read"
                    return@launch
                }
                
                // Get user info
                android.util.Log.d("Fda483ViewModel", "Fetching user info...")
                val userDoc = firebaseRepository.getUserById(currentUser.uid)
                val user = userDoc.getOrNull()
                android.util.Log.d("Fda483ViewModel", "User info fetched. Display name: ${user?.displayName ?: currentUser.displayName}")
                
                // Create initial assessment
                val assessment = Fda483Assessment(
                    userId = currentUser.uid,
                    userName = user?.displayName ?: currentUser.displayName ?: "Unknown",
                    userEmail = currentUser.email ?: "",
                    fileName = fileName,
                    fileSize = fileSize,
                    uploadedAt = System.currentTimeMillis(),
                    status = "processing"
                )
                
                // Save assessment to database
                android.util.Log.d("Fda483ViewModel", "Saving assessment to database...")
                val createResult = repository.createAssessment(assessment)
                val savedAssessment = createResult.getOrElse { error ->
                    android.util.Log.e("Fda483ViewModel", "Failed to save assessment: ${error.message}", error)
                    com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "save_fda483_assessment")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("file_name", fileName)
                    com.pramod.validator.utils.CrashReporting.logException(error, "Failed to save FDA 483 assessment")
                    _isLoading.value = false
                    _errorMessage.value = "Couldn't save the assessment. Please try again."
                    return@launch
                }
                android.util.Log.d("Fda483ViewModel", "Assessment saved with ID: ${savedAssessment.id}")
                
                // Check network connectivity before processing with AI
                val networkMonitor = NetworkMonitor(context)
                val isOnline = networkMonitor.isCurrentlyOnline()
                
                if (!isOnline) {
                    android.util.Log.w("Fda483ViewModel", "⚠️ Offline: Cannot process FDA 483 without internet")
                    _isLoading.value = false
                    _errorMessage.value = "Please connect to the internet to generate the AI summary. Once connected, you can process the FDA 483 document."
                    return@launch
                }
                
                // Process with AI
                android.util.Log.d("Fda483ViewModel", "Starting AI analysis...")
                val aiResult = withContext(Dispatchers.IO) {
                    OpenAIService.analyzeFda483(pdfText)
                }
                
                val aiAnalysis = aiResult.getOrElse { error ->
                    android.util.Log.e("Fda483ViewModel", "AI analysis failed: ${error.message}", error)
                    com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "fda483_ai_analysis")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", savedAssessment.id)
                    com.pramod.validator.utils.CrashReporting.setCustomKey("file_name", fileName)
                    com.pramod.validator.utils.CrashReporting.logException(error, "AI analysis failed for FDA 483")
                    // Update assessment with error status
                    val failedAssessment = savedAssessment.copy(
                        status = "failed"
                    )
                    repository.updateAssessment(failedAssessment)
                    _isLoading.value = false
                    _errorMessage.value = "AI analysis failed. Please check your connection and try again."
                    return@launch
                }
                
                android.util.Log.d("Fda483ViewModel", "AI analysis completed. Response length: ${aiAnalysis.length}")
                
                // Parse AI response
                android.util.Log.d("Fda483ViewModel", "Parsing AI response...")
                val parsedResult = parseAiAnalysis(aiAnalysis)
                android.util.Log.d("Fda483ViewModel", "AI response parsed. Risk areas: ${parsedResult.riskAreas.size}, Checklist items: ${parsedResult.checklist.size}")
                
                // Update assessment with results
                val completedAssessment = savedAssessment.copy(
                    summary = parsedResult.summary,
                    riskAreas = parsedResult.riskAreas,
                    checklist = parsedResult.checklist,
                    aiAnalysis = aiAnalysis,
                    processedAt = System.currentTimeMillis(),
                    status = "completed"
                )
                
                val updateResult = repository.updateAssessment(completedAssessment)
                updateResult.fold(
                    onSuccess = {
                        _isLoading.value = false
                        _successMessage.value = "FDA 483 analysis completed successfully"
                        loadUserAssessments() // Reload to show new assessment
                        // Set the completed assessment ID to trigger navigation
                        _completedAssessmentId.value = completedAssessment.id
                    },
                    onFailure = { error ->
                        com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "update_fda483_assessment")
                        com.pramod.validator.utils.CrashReporting.setCustomKey("assessment_id", completedAssessment.id)
                        com.pramod.validator.utils.CrashReporting.logException(error, "Failed to update FDA 483 assessment")
                        _isLoading.value = false
                        _errorMessage.value = "Couldn't save the analysis results. Please try again."
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("Fda483ViewModel", "Error processing PDF: ${e.message}", e)
                android.util.Log.e("Fda483ViewModel", "Stack trace: ${e.stackTraceToString()}")
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "process_fda483_pdf")
                com.pramod.validator.utils.CrashReporting.setCustomKey("file_name", fileName)
                com.pramod.validator.utils.CrashReporting.logException(e, "Exception processing FDA 483 PDF")
                _isLoading.value = false
                _errorMessage.value = "Error processing PDF. Please try again or contact support if the problem persists."
            } catch (e: Throwable) {
                android.util.Log.e("Fda483ViewModel", "Fatal error processing PDF: ${e.message}", e)
                android.util.Log.e("Fda483ViewModel", "Stack trace: ${e.stackTraceToString()}")
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "process_fda483_pdf_fatal")
                com.pramod.validator.utils.CrashReporting.setCustomKey("file_name", fileName)
                com.pramod.validator.utils.CrashReporting.logException(e, "Fatal error processing FDA 483 PDF")
                _isLoading.value = false
                _errorMessage.value = "A fatal error occurred. Please try again or contact support."
            }
        }
    }
    
    private fun parseAiAnalysis(aiAnalysis: String): ParsedAnalysis {
        return try {
            android.util.Log.d("Fda483ViewModel", "Parsing AI analysis JSON...")
            val json = JSONObject(aiAnalysis)
            val summary = json.optString("summary", "").takeIf { it.isNotBlank() } ?: "No summary provided"
            
            val riskAreasList = mutableListOf<RiskArea>()
            val riskAreasArray = json.optJSONArray("riskAreas")
            if (riskAreasArray != null) {
                for (i in 0 until riskAreasArray.length()) {
                    try {
                        val riskObj = riskAreasArray.getJSONObject(i)
                        riskAreasList.add(
                            RiskArea(
                                area = riskObj.optString("area", "").takeIf { it.isNotBlank() } ?: "Unknown Area",
                                description = riskObj.optString("description", ""),
                                specificDetails = riskObj.optString("specificDetails", "")
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("Fda483ViewModel", "Error parsing risk area at index $i: ${e.message}")
                        // Continue with next item
                    }
                }
            } else {
                android.util.Log.w("Fda483ViewModel", "No riskAreas array found in AI response")
            }
            
            val checklistList = mutableListOf<ChecklistItem>()
            val checklistArray = json.optJSONArray("checklist")
            if (checklistArray != null) {
                for (i in 0 until checklistArray.length()) {
                    try {
                        val itemObj = checklistArray.getJSONObject(i)
                        checklistList.add(
                            ChecklistItem(
                                item = itemObj.optString("item", "").takeIf { it.isNotBlank() } ?: "Unknown Item",
                                priority = itemObj.optString("priority", "Medium").takeIf { it.isNotBlank() } ?: "Medium"
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("Fda483ViewModel", "Error parsing checklist item at index $i: ${e.message}")
                        // Continue with next item
                    }
                }
            } else {
                android.util.Log.w("Fda483ViewModel", "No checklist array found in AI response")
            }
            
            android.util.Log.d("Fda483ViewModel", "Parsed ${riskAreasList.size} risk areas and ${checklistList.size} checklist items")
            ParsedAnalysis(summary, riskAreasList, checklistList)
        } catch (e: Exception) {
            android.util.Log.e("Fda483ViewModel", "Error parsing AI analysis: ${e.message}", e)
            android.util.Log.e("Fda483ViewModel", "AI response was: ${aiAnalysis.take(500)}")
            ParsedAnalysis(
                summary = "Error parsing AI analysis: ${e.message}. Please try again.",
                riskAreas = emptyList(),
                checklist = emptyList()
            )
        }
    }
    
    fun getAssessmentById(assessmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getAssessmentById(assessmentId)
                result.fold(
                    onSuccess = { assessment ->
                        _currentAssessment.value = assessment
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load assessment: ${error.message}"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading assessment: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    fun clearCompletedAssessmentId() {
        _completedAssessmentId.value = null
    }
    
    fun setShouldShowHistoryTab(shouldShow: Boolean) {
        _shouldShowHistoryTab.value = shouldShow
    }
    
    fun clearShouldShowHistoryTab() {
        _shouldShowHistoryTab.value = false
    }
    
    fun deleteAssessment(assessmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteAssessment(assessmentId)
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        _successMessage.value = "Assessment deleted successfully"
                        loadUserAssessments()
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        _errorMessage.value = "Failed to delete assessment: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error deleting assessment: ${e.message}"
            }
        }
    }
    
    private data class ParsedAnalysis(
        val summary: String,
        val riskAreas: List<RiskArea>,
        val checklist: List<ChecklistItem>
    )
}


