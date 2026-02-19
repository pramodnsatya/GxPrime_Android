package com.pramod.validator.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.data.models.CustomQuestion
import com.pramod.validator.data.models.InProgressAssessment
import com.pramod.validator.viewmodel.CustomQuestionnaireViewModel
import com.pramod.validator.viewmodel.InProgressAssessmentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomQuestionnaireScreen(
    assessmentId: String,
    assessmentName: String,
    onComplete: (Map<String, AnswerType>, Map<String, String>) -> Unit,
    onBack: () -> Unit,
    viewModel: CustomQuestionnaireViewModel = viewModel(),
    inProgressViewModel: InProgressAssessmentViewModel = viewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val responses by viewModel.responses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var isSavingProgress by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    val assessmentToRestore by inProgressViewModel.assessmentToRestore.collectAsState()
    
    LaunchedEffect(assessmentId) {
        try {
            if (questions.isEmpty() && assessmentId.isNotBlank()) {
                viewModel.loadQuestions(assessmentId)
                viewModel.setAssessmentInfo(assessmentId, assessmentName)
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomQuestionnaireScreen", "Error loading questions: ${e.message}", e)
        }
    }
    
    // Restore progress when questions are loaded and assessment to restore is available
    LaunchedEffect(questions, assessmentToRestore) {
        val assessment = assessmentToRestore
        if (questions.isNotEmpty() && assessment != null && 
            assessment.subDomainId == assessmentId &&
            assessment.isCustomAssessment &&
            responses.isEmpty() &&
            !isRestoring) { // Only restore if we don't already have responses and not already restoring
            android.util.Log.d("CustomQuestionnaireScreen", "🔄 Restoring assessment: ${assessment.id}, index=${assessment.currentQuestionIndex}, questions=${questions.size}")
            viewModel.restoreFromProgress(
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
        }
    }
    
    // Update index if questions were loaded after restoration
    LaunchedEffect(questions, isRestoring, assessmentToRestore) {
        val assessment = assessmentToRestore
        if (questions.isNotEmpty() && !isRestoring && assessment != null) {
            val savedIndex = assessment.currentQuestionIndex
            val currentIndex = viewModel.currentQuestionIndex.value
            if (currentIndex == 0 && savedIndex > 0 && savedIndex < questions.size) {
                android.util.Log.d("CustomQuestionnaireScreen", "🔄 Updating index after questions loaded: $savedIndex")
                // Use the ViewModel's method to set the index if available, or restore again
                viewModel.restoreFromProgress(
                    responses = assessment.responses,
                    currentQuestionIndex = savedIndex,
                    questionTexts = assessment.questionTexts,
                    inProgressAssessmentId = assessment.id
                )
            }
        }
    }
    
    val handleBack = {
        if (viewModel.hasProgress()) {
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
            val assessmentInfo = viewModel.getAssessmentInfo()
            val questionTexts = viewModel.getQuestionTextsMap()
            val responsesMap = responses.mapValues { it.value.name } // Convert AnswerType to String
            val existingAssessmentId = viewModel.getInProgressAssessmentId()
            
            android.util.Log.d("CustomQuestionnaireScreen", "💾 Saving progress: index=$currentQuestionIndex, responses=${responsesMap.size}, existingId=$existingAssessmentId")
            
            val inProgressAssessment = InProgressAssessment(
                id = existingAssessmentId, // Use existing ID if resuming, empty string if new
                userId = "", // Will be set by ViewModel
                assessmentName = assessmentInfo["assessmentName"] ?: assessmentName,
                facilityId = "",
                facilityName = "",
                domainId = "custom",
                domainName = "Custom Assessment",
                subDomainId = assessmentInfo["assessmentId"] ?: assessmentId,
                subDomainName = assessmentInfo["assessmentName"] ?: assessmentName,
                isCustomAssessment = true,
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
                    viewModel.restoreFromProgress(
                        responses = responsesMap,
                        currentQuestionIndex = currentQuestionIndex,
                        questionTexts = questionTexts,
                        inProgressAssessmentId = savedId
                    )
                    saved = true
                    android.util.Log.d("CustomQuestionnaireScreen", "✅ Save completed, ID stored: $savedId")
                }
                attempts++
            }
            if (!saved) {
                android.util.Log.w("CustomQuestionnaireScreen", "⚠️ Save timeout, but proceeding anyway")
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
            Surface(
                shadowElevation = 4.dp,
                color = Color(0xFF1E3A8A) // blue-900 (same as GxPrime header)
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            assessmentName,
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF3B82F6)
                )
            } else if (questions.isNotEmpty()) {
                val currentQuestion = viewModel.getCurrentQuestion()
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
                                    android.util.Log.d("CustomQuestionnaireScreen", "📋 Question ${question.id}: selectedAnswer=$selectedAnswer, all responses: ${responses.keys.joinToString()}")
                                }

                                ModernAnswerButton(
                                    text = "Compliant",
                                    isSelected = selectedAnswer == AnswerType.COMPLIANT,
                                    onClick = {
                                        viewModel.answerQuestion(
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
                                        viewModel.answerQuestion(
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
                                        viewModel.answerQuestion(
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
                        if (!viewModel.isFirstQuestion()) {
                            OutlinedButton(
                                onClick = { viewModel.previousQuestion() },
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
                                if (viewModel.isLastQuestion()) {
                                    // Navigate immediately - don't wait for save operations
                                    val questionTexts = viewModel.getQuestionTextsMap()
                                    // Navigate first, then do cleanup in background
                                    onComplete(responses, questionTexts)
                                    // Do cleanup operations in background without blocking
                                    coroutineScope.launch {
                                        try {
                                            viewModel.saveAllResponses()
                                            // Delete in-progress assessment if it exists
                                            val inProgressId = viewModel.getInProgressAssessmentId()
                                            if (inProgressId.isNotEmpty()) {
                                                android.util.Log.d("CustomQuestionnaireScreen", "🗑️ Deleting completed in-progress assessment: $inProgressId")
                                                inProgressViewModel.deleteInProgressAssessment(inProgressId)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CustomQuestionnaireScreen", "Error in background cleanup: ${e.message}", e)
                                        }
                                    }
                                } else {
                                    viewModel.nextQuestion()
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
                                    text = if (viewModel.isLastQuestion()) "Finish" else "Next",
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
                title = { Text("Save Progress?") },
                text = { 
                    Text("You have unsaved progress. Do you want to save it and continue later?")
                },
                confirmButton = {
                    Button(
                        onClick = saveProgress,
                        enabled = !isSavingProgress
                    ) {
                        if (isSavingProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            onBack()
                        }
                    ) {
                        Text("Don't Save")
                    }
                }
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