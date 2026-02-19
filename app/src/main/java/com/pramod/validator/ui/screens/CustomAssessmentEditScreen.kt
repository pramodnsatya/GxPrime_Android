package com.pramod.validator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.CustomQuestion
import com.pramod.validator.viewmodel.CustomAssessmentViewModel
import com.pramod.validator.ui.components.AbstractBackground
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAssessmentEditScreen(
    assessmentId: String,
    onNavigateBack: () -> Unit,
    viewModel: CustomAssessmentViewModel = viewModel()
) {
    val currentAssessment by viewModel.currentAssessment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var assessmentName by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf<List<CustomQuestion>>(emptyList()) }
    var currentQuestionText by remember { mutableStateOf("") }
    var editingQuestionId by remember { mutableStateOf<String?>(null) }
    var editingQuestionText by remember { mutableStateOf("") }
    
    // Load assessment when screen opens
    LaunchedEffect(assessmentId) {
        viewModel.getCustomAssessmentById(assessmentId)
    }
    
    // Update local state when assessment is loaded
    LaunchedEffect(currentAssessment) {
        currentAssessment?.let { assessment ->
            assessmentName = assessment.name
            questions = assessment.questions.sortedBy { it.order }
        }
    }
    
    // Navigate back on success
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(1000) // Show success message briefly
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Assessment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error/Success messages
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            successMessage?.let { success ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (isLoading && currentAssessment == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = assessmentName,
                    onValueChange = { assessmentName = it },
                    label = { Text("Assessment Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider()

                // Add new question
                OutlinedTextField(
                    value = currentQuestionText,
                    onValueChange = { currentQuestionText = it },
                    label = { Text("Add New Question") },
                    modifier = Modifier.fillMaxWidth(),
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
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Question")
                        }
                    }
                )

                if (questions.isNotEmpty()) {
                    Text(
                        text = "Questions (${questions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    questions.forEachIndexed { index, question ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                if (editingQuestionId == question.id) {
                                    // Edit mode
                                    OutlinedTextField(
                                        value = editingQuestionText,
                                        onValueChange = { editingQuestionText = it },
                                        label = { Text("Question Text") },
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        if (editingQuestionText.isNotBlank()) {
                                                            questions = questions.map { q ->
                                                                if (q.id == question.id) {
                                                                    q.copy(questionText = editingQuestionText.trim())
                                                                } else {
                                                                    q
                                                                }
                                                            }
                                                            editingQuestionId = null
                                                            editingQuestionText = ""
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                                }
                                                IconButton(
                                                    onClick = {
                                                        editingQuestionId = null
                                                        editingQuestionText = ""
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Display mode
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = question.questionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingQuestionId = question.id
                                                    editingQuestionText = question.questionText
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            TextButton(
                                                onClick = {
                                                    questions = questions.filter { it.id != question.id }
                                                        .mapIndexed { idx, q -> q.copy(order = idx + 1) }
                                                }
                                            ) {
                                                Text("Remove")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateCustomAssessment(assessmentId, assessmentName, questions)
                    },
                    enabled = assessmentName.isNotBlank() && questions.isNotEmpty() && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update Assessment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

