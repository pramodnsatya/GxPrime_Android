package com.pramod.validator.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.data.models.Domain
import com.pramod.validator.data.models.InProgressAssessment
import com.pramod.validator.viewmodel.QuestionnaireViewModel
import com.pramod.validator.viewmodel.InProgressAssessmentViewModel
import com.pramod.validator.ui.components.OfflineIndicator
import com.pramod.validator.utils.NetworkMonitor
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    subDomainId: String,
    subDomainName: String,
    facilityId: String,
    facilityName: String,
    domainId: String = "",
    domainName: String = "",
    assessmentName: String = "",
    onComplete: (Map<String, AnswerType>, Map<String, String>) -> Unit,
    onBack: () -> Unit,
    questionnaireViewModel: QuestionnaireViewModel = viewModel(),
    inProgressViewModel: InProgressAssessmentViewModel = viewModel()
) {
    val questions by questionnaireViewModel.questions.collectAsState()
    val currentQuestionIndex by questionnaireViewModel.currentQuestionIndex.collectAsState()
    val responses by questionnaireViewModel.responses.collectAsState()
    val isLoading by questionnaireViewModel.isLoading.collectAsState()
    val isSaving by questionnaireViewModel.isSaving.collectAsState()
    val isRestoring by questionnaireViewModel.isRestoring.collectAsState()
    val loadError by questionnaireViewModel.loadError.collectAsState()
    val questionsLoadedFromFirebase by questionnaireViewModel.questionsLoadedFromFirebase.collectAsState()
    
    // Network connectivity monitoring
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var isSavingProgress by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    val assessmentToRestore by inProgressViewModel.assessmentToRestore.collectAsState()
    
    LaunchedEffect(subDomainId) {
        try {
            if (questions.isEmpty() && subDomainId.isNotBlank()) {
                questionnaireViewModel.loadQuestions(subDomainId, context)
                // Set assessment info for saving progress
                questionnaireViewModel.setAssessmentInfo(
                    domainId = domainId.ifEmpty { "unknown" },
                    domainName = domainName.ifEmpty { "Unknown Domain" },
                    subDomainName = subDomainName,
                    facilityId = facilityId,
                    facilityName = facilityName,
                    assessmentName = assessmentName.ifEmpty { subDomainName }
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("QuestionnaireScreen", "Error loading questions: ${e.message}", e)
        }
    }
    
    // Restore progress when questions are loaded and assessment to restore is available
    LaunchedEffect(questions, assessmentToRestore, subDomainId, facilityId) {
        val assessment = assessmentToRestore
        android.util.Log.d("QuestionnaireScreen", "🔍 Checking restoration: questions=${questions.size}, assessment=${assessment?.id}, subDomainId=$subDomainId, facilityId=$facilityId, responses=${responses.size}, isRestoring=$isRestoring")
        
        if (assessment != null) {
            android.util.Log.d("QuestionnaireScreen", "   - Assessment details: subDomainId=${assessment.subDomainId}, isCustom=${assessment.isCustomAssessment}, facilityId=${assessment.facilityId}, responses=${assessment.responses.size}")
        }
        
        if (questions.isNotEmpty() && assessment != null && 
            assessment.subDomainId == subDomainId &&
            !assessment.isCustomAssessment &&
            assessment.facilityId == facilityId &&
            responses.isEmpty() &&
            !isRestoring) { // Only restore if we don't already have responses and not already restoring
            android.util.Log.d("QuestionnaireScreen", "✅ CONDITIONS MET - Restoring assessment: ${assessment.id}, index=${assessment.currentQuestionIndex}, questions=${questions.size}, saved responses=${assessment.responses.size}")
            android.util.Log.d("QuestionnaireScreen", "   - Response keys: ${assessment.responses.keys.joinToString()}")
            questionnaireViewModel.restoreFromProgress(
                responses = assessment.responses,
                currentQuestionIndex = assessment.currentQuestionIndex,
                questionTexts = assessment.questionTexts,
                inProgressAssessmentId = assessment.id
            )
            // Clear the assessment to restore after restoration completes
            kotlinx.coroutines.delay(200)
            inProgressViewModel.clearAssessmentToRestore()
            // Don't delete the assessment here - let it be deleted when assessment is completed
            // inProgressViewModel.deleteInProgressAssessment(assessment.id)
        } else if (assessment != null) {
            android.util.Log.w("QuestionnaireScreen", "⚠️ Restoration conditions not met: questions=${questions.size}, assessment=${assessment.id}, responses=${responses.size}, isRestoring=$isRestoring")
        }
    }
    
    // Update index if questions were loaded after restoration
    LaunchedEffect(questions, isRestoring, assessmentToRestore) {
        val assessment = assessmentToRestore
        if (questions.isNotEmpty() && !isRestoring && assessment != null) {
            val savedIndex = assessment.currentQuestionIndex
            val currentIndex = questionnaireViewModel.currentQuestionIndex.value
            if (currentIndex == 0 && savedIndex > 0 && savedIndex < questions.size) {
                android.util.Log.d("QuestionnaireScreen", "🔄 Updating index after questions loaded: $savedIndex")
                // Use the ViewModel's method to set the index if available, or restore again
                questionnaireViewModel.restoreFromProgress(
                    responses = assessment.responses,
                    currentQuestionIndex = savedIndex,
                    questionTexts = assessment.questionTexts,
                    inProgressAssessmentId = assessment.id
                )
            }
        }
    }
    
    val handleBack = {
        if (questionnaireViewModel.hasProgress()) {
            showSaveDialog = true
        } else {
            onBack()
        }
    }
    
    // Handle system back button
    BackHandler(enabled = true) {
        handleBack()
    }
    
    val successMessage by inProgressViewModel.successMessage.collectAsState()
    
    val saveProgress: () -> Unit = {
        isSavingProgress = true
        coroutineScope.launch {
            val assessmentInfo = questionnaireViewModel.getAssessmentInfo()
            val questionTexts = questionnaireViewModel.getQuestionTextsMap()
            val responsesMap = responses.mapValues { it.value.name } // Convert AnswerType to String
            val existingAssessmentId = questionnaireViewModel.getInProgressAssessmentId()
            
            android.util.Log.d("QuestionnaireScreen", "💾 Saving progress: index=$currentQuestionIndex, responses=${responsesMap.size}, existingId=$existingAssessmentId")
            
            val inProgressAssessment = InProgressAssessment(
                id = existingAssessmentId, // Use existing ID if resuming, empty string if new
                userId = "", // Will be set by ViewModel
                assessmentName = assessmentInfo["assessmentName"] ?: subDomainName,
                facilityId = assessmentInfo["facilityId"] ?: facilityId,
                facilityName = assessmentInfo["facilityName"] ?: facilityName,
                domainId = assessmentInfo["domainId"] ?: domainId.ifEmpty { "unknown" },
                domainName = assessmentInfo["domainName"] ?: domainName.ifEmpty { "Unknown Domain" },
                subDomainId = subDomainId,
                subDomainName = assessmentInfo["subDomainName"] ?: subDomainName,
                isCustomAssessment = false,
                currentQuestionIndex = currentQuestionIndex,
                totalQuestions = questions.size,
                responses = responsesMap,
                questionTexts = questionTexts
            )
            
            // Clear any previous success message
            inProgressViewModel.clearMessages()
            inProgressViewModel.saveInProgressAssessment(inProgressAssessment)
            // Wait for save to complete (check success message or wait a bit)
            var attempts = 0
            var saved = false
            while (!saved && attempts < 50) {
                delay(100)
                val savedId = inProgressViewModel.lastSavedAssessmentId.value
                if (savedId != null && savedId.isNotEmpty()) {
                    // Update the ViewModel's inProgressAssessmentId so auto-save works
                    questionnaireViewModel.restoreFromProgress(
                        responses = responsesMap,
                        currentQuestionIndex = currentQuestionIndex,
                        questionTexts = questionTexts,
                        inProgressAssessmentId = savedId
                    )
                    saved = true
                    android.util.Log.d("QuestionnaireScreen", "✅ Save completed, ID stored: $savedId")
                }
                attempts++
            }
            if (!saved) {
                android.util.Log.w("QuestionnaireScreen", "⚠️ Save timeout, but proceeding anyway")
            }
            // Give more time for Firestore to sync and list to refresh
            delay(500)
            isSavingProgress = false
            showSaveDialog = false
            onBack()
        }
    }

    Scaffold(
        topBar = {
            Column {
                OfflineIndicator(isOnline = isOnline)
                Surface(
                    shadowElevation = 4.dp,
                    color = Color(0xFF1E3A8A) // blue-900 (same as GxPrime header)
                ) {
                    TopAppBar(
                        title = { 
                            Text(
                                assessmentName.ifEmpty { subDomainName },
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = handleBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1E3A8A), // blue-900 (same as GxPrime header)
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
        ) {
            if (isLoading || isRestoring) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading questions...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (loadError != null) {
                // Show error message when questions fail to load
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to Load Questions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadError ?: "An error occurred while loading questions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            questionnaireViewModel.loadQuestions(subDomainId, context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go Back")
                    }
                }
            } else if (questions.isNotEmpty()) {
                val currentQuestion = questionnaireViewModel.getCurrentQuestion()
                val hasAnswer = currentQuestion?.let { responses.containsKey(it.id) } ?: false
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFF1E3A8A), // blue-900 (same as GxPrime header)
                        trackColor = Color(0xFFE2E8F0) // slate-200
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A), // slate-900
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Question card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            currentQuestion?.let { question ->
                                // Question text
                                Text(
                                    text = question.text,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF0F172A), // slate-900
                                    lineHeight = 26.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Answer options
                                val selectedAnswer = responses[question.id]
                                
                                // Log for debugging
                                LaunchedEffect(question.id, selectedAnswer) {
                                    android.util.Log.d("QuestionnaireScreen", "📋 Question ${question.id}: selectedAnswer=$selectedAnswer, all responses: ${responses.keys.joinToString()}")
                                }

                                ModernAnswerButton(
                                    text = "Compliant",
                                    isSelected = selectedAnswer == AnswerType.COMPLIANT,
                                    onClick = {
                                        questionnaireViewModel.answerQuestion(
                                            question.id,
                                            AnswerType.COMPLIANT
                                        )
                                    },
                                    answerType = AnswerType.COMPLIANT
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                ModernAnswerButton(
                                    text = "Non-Compliant",
                                    isSelected = selectedAnswer == AnswerType.NON_COMPLIANT,
                                    onClick = {
                                        questionnaireViewModel.answerQuestion(
                                            question.id,
                                            AnswerType.NON_COMPLIANT
                                        )
                                    },
                                    answerType = AnswerType.NON_COMPLIANT
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                ModernAnswerButton(
                                    text = "Not Applicable",
                                    isSelected = selectedAnswer == AnswerType.NOT_APPLICABLE,
                                    onClick = {
                                        questionnaireViewModel.answerQuestion(
                                            question.id,
                                            AnswerType.NOT_APPLICABLE
                                        )
                                    },
                                    answerType = AnswerType.NOT_APPLICABLE
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Show message if no answer selected
                    if (!hasAnswer) {
                        Text(
                            text = "Please select an answer to continue",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B), // slate-500
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!questionnaireViewModel.isFirstQuestion()) {
                            OutlinedButton(
                                onClick = { questionnaireViewModel.previousQuestion() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF0F172A) // slate-900
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) // slate-200
                            ) {
                                Text("Previous", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                if (questionnaireViewModel.isLastQuestion()) {
                                    // Navigate immediately - don't wait for save operations
                                    val questionTexts = questionnaireViewModel.getQuestionTextsMap()
                                    // Navigate first, then do cleanup in background
                                    onComplete(responses, questionTexts)
                                    // Do cleanup operations in background without blocking
                                    coroutineScope.launch {
                                        try {
                                            questionnaireViewModel.saveAllResponses()
                                            // Delete in-progress assessment if it exists
                                            val inProgressId = questionnaireViewModel.getInProgressAssessmentId()
                                            if (inProgressId.isNotEmpty()) {
                                                android.util.Log.d("QuestionnaireScreen", "🗑️ Deleting completed in-progress assessment: $inProgressId")
                                                inProgressViewModel.deleteInProgressAssessment(inProgressId)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("QuestionnaireScreen", "Error in background cleanup: ${e.message}", e)
                                        }
                                    }
                                } else {
                                    questionnaireViewModel.nextQuestion()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = hasAnswer && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasAnswer) Color(0xFF1E3A8A) else Color(0xFFE2E8F0), // blue-900 or slate-200
                                contentColor = if (hasAnswer) Color.White else Color(0xFF94A3B8), // white or slate-400
                                disabledContainerColor = Color(0xFFE2E8F0), // slate-200
                                disabledContentColor = Color(0xFF94A3B8) // slate-400
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (questionnaireViewModel.isLastQuestion()) "Finish" else "Next",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Save Progress Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                containerColor = Color.White,
                title = { 
                    Text(
                        "Save Progress?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    ) 
                },
                text = { 
                    Text(
                        "You have unsaved progress. Do you want to save it and continue later?",
                        fontSize = 15.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                },
                confirmButton = {
                    Button(
                        onClick = saveProgress,
                        enabled = !isSavingProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A) // blue-900
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Save",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            onBack()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF0F172A) // slate-900
                        )
                    ) {
                        Text(
                            "Don't Save",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ModernAnswerButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    answerType: AnswerType
) {
    val icon = when (answerType) {
        AnswerType.COMPLIANT -> Icons.Default.CheckCircle
        AnswerType.NON_COMPLIANT -> Icons.Default.Cancel
        AnswerType.NOT_APPLICABLE -> Icons.Default.RemoveCircle
    }
    
    val iconColor = when (answerType) {
        AnswerType.COMPLIANT -> Color(0xFF10B981) // emerald-500
        AnswerType.NON_COMPLIANT -> Color(0xFFEF4444) // red-500
        AnswerType.NOT_APPLICABLE -> Color(0xFF64748B) // slate-500
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1E3A8A) else Color.White, // blue-900 or white
            contentColor = if (isSelected) Color.White else Color(0xFF0F172A) // white or slate-900
        ),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null // slate-200
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Start,
                color = if (isSelected) Color.White else Color(0xFF0F172A) // white or slate-900
            )
        }
    }
}

