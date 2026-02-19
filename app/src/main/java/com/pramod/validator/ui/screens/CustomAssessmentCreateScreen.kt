package com.pramod.validator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.CustomQuestion
import com.pramod.validator.viewmodel.CustomAssessmentViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAssessmentCreateScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomAssessmentViewModel = viewModel()
) {
    var assessmentName by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf<List<CustomQuestion>>(emptyList()) }
    var currentQuestionText by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "Create Custom Assessment",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A) // slate-900
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A), // slate-900
                        navigationIconContentColor = Color(0xFF0F172A) // slate-900
                    )
                )
            }
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // slate-50
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Assessment Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Assessment Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    
                    OutlinedTextField(
                        value = assessmentName,
                        onValueChange = { assessmentName = it },
                        label = { Text("Assessment Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        )
                    )
                }
            }

            // Questions Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Questions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    
                    OutlinedTextField(
                        value = currentQuestionText,
                        onValueChange = { currentQuestionText = it },
                        label = { Text("Question") },
                        placeholder = { Text("Enter a question...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                            unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (currentQuestionText.isNotBlank()) {
                                        questions = questions + CustomQuestion(
                                            id = UUID.randomUUID().toString(),
                                            questionText = currentQuestionText.trim(),
                                            order = questions.size + 1
                                        )
                                        currentQuestionText = ""
                                    }
                                },
                                enabled = currentQuestionText.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add Question",
                                    tint = if (currentQuestionText.isNotBlank()) 
                                        Color(0xFF1E3A8A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    )
                }
            }

            // Questions List Card
            if (questions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Questions (${questions.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )

                        questions.forEachIndexed { index, question ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF8FAFC) // slate-50
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = Color(0xFF1E3A8A), // blue-900
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Text(
                                        text = question.questionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        color = Color(0xFF0F172A) // slate-900
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            questions = questions.filter { it.id != question.id }
                                                .mapIndexed { idx, q -> q.copy(order = idx + 1) }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFDC2626), // red-600
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Create Button
            Button(
                onClick = {
                    viewModel.createCustomAssessment(assessmentName, questions)
                    onNavigateBack()
                },
                enabled = assessmentName.isNotBlank() && questions.isNotEmpty() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A), // blue-900
                    disabledContainerColor = Color(0xFFE2E8F0) // slate-200
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Create Assessment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}






