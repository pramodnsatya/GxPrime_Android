package com.pramod.validator.ui.screens

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pramod.validator.data.models.TrainingResource
import com.pramod.validator.data.models.TrainingResourceType
import com.pramod.validator.ui.components.BottomNavigationBar
import com.pramod.validator.viewmodel.TrainingResourceFormState
import com.pramod.validator.viewmodel.TrainingResourcesViewModel
import com.pramod.validator.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminTrainingResourcesScreen(
    currentUserId: String?,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: TrainingResourcesViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPermissions by authViewModel.currentUserPermissions.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var resourceToDelete by remember { mutableStateOf<TrainingResource?>(null) }
    val context = LocalContext.current
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                android.util.Log.w("TrainingResources", "Unable to persist URI permission: ${e.message}")
            }
            val mimeType = context.contentResolver.getType(uri)
            val fileName = resolveDisplayName(context.contentResolver, uri)
            viewModel.setSelectedFile(uri, fileName, mimeType)
        }
    }
    
    // PDF-only file picker for PDF and FDA483 types
    val pdfFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                android.util.Log.w("TrainingResources", "Unable to persist URI permission: ${e.message}")
            }
            val mimeType = context.contentResolver.getType(uri)
            val fileName = resolveDisplayName(context.contentResolver, uri)
            viewModel.setSelectedFile(uri, fileName, mimeType)
        }
    }
    
    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.consumeError()
        }
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.consumeSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "Training Resources",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A) // slate-900
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "resources",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> onNavigateToHistory()
                        "fda483" -> onNavigateToFda483()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> Unit
                    }
                },
                user = currentUser,
                permissions = currentUserPermissions
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF3B82F6)
                )
            }
            
            errorMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2) // red-100
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFDC2626) // red-600
                    )
                }
            }
            
            successMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7) // green-100
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF16A34A) // green-600
                    )
                }
            }
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF3B82F6) // blue-500
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Upload Resource",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color(0xFF3B82F6) else Color(0xFF64748B) // blue-500 : slate-500
                        ) 
                    },
                    icon = { 
                        Icon(
                            Icons.Default.CloudUpload, 
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color(0xFF3B82F6) else Color(0xFF64748B)
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "Manage Resources",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color(0xFF3B82F6) else Color(0xFF64748B) // blue-500 : slate-500
                        ) 
                    },
                    icon = { 
                        Icon(
                            Icons.Default.List, 
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color(0xFF3B82F6) else Color(0xFF64748B)
                        ) 
                    }
                )
            }
            
            when (selectedTab) {
                0 -> UploadResourceForm(
                    formState = formState,
                    onTitleChange = viewModel::updateTitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onTypeChange = viewModel::updateType,
                    onLinkChange = viewModel::updateLinkUrl,
                    onSelectFile = {
                        // Use PDF-only picker for PDF and FDA483, general picker for others
                        if (formState.type == TrainingResourceType.PDF || formState.type == TrainingResourceType.FDA483) {
                            pdfFilePickerLauncher.launch(arrayOf("application/pdf"))
                        } else {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    },
                    onSave = {
                        viewModel.saveResource(currentUserId)
                    },
                    onClear = {
                        viewModel.clearForm()
                    }
                )
                1 -> ManageResourcesList(
                    resources = resources,
                    onEdit = { resource ->
                        viewModel.editResource(resource)
                        selectedTab = 0
                    },
                    onDelete = { resource ->
                        resourceToDelete = resource
                    }
                )
            }
        }
    }
    
    resourceToDelete?.let { resource ->
        AlertDialog(
            onDismissRequest = { resourceToDelete = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { 
                Text(
                    "Delete Resource",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${resource.title}\"?",
                    fontSize = 15.sp,
                    color = Color(0xFF64748B) // slate-500
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteResource(resource)
                        resourceToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626) // red-600
                    )
                ) {
                    Text(
                        "Delete",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { resourceToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Cancel",
                        color = Color(0xFF0F172A) // slate-900
                    )
                }
            }
        )
    }
}

@Composable
private fun InfoBanner(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = contentColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadResourceForm(
    formState: TrainingResourceFormState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (TrainingResourceType) -> Unit,
    onLinkChange: (String) -> Unit,
    onSelectFile: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val typeOptions = TrainingResourceType.values()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = formState.title,
            onValueChange = onTitleChange,
            label = { Text("Resource Title *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
            )
        )
        
        OutlinedTextField(
            value = formState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
            )
        )
        
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            OutlinedTextField(
                value = when (formState.type) {
                    TrainingResourceType.FDA483 -> "FDA483"
                    TrainingResourceType.PDF -> "PDF"
                    else -> formState.type.name.replace("_", " ").lowercase()
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) } }
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Resource Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                    unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                )
            )
            
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                typeOptions.forEach { type ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                when (type) {
                                    TrainingResourceType.FDA483 -> "FDA483"
                                    TrainingResourceType.PDF -> "PDF"
                                    else -> type.name.replace("_", " ").lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) } }
                                },
                                color = Color(0xFF0F172A) // slate-900
                            ) 
                        },
                        onClick = {
                            onTypeChange(type)
                            isDropdownExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color(0xFF0F172A), // slate-900
                            leadingIconColor = Color(0xFF0F172A),
                            trailingIconColor = Color(0xFF0F172A),
                            disabledTextColor = Color(0xFF94A3B8), // slate-400
                            disabledLeadingIconColor = Color(0xFF94A3B8),
                            disabledTrailingIconColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
        
        when (formState.type) {
            TrainingResourceType.VIDEO -> {
                OutlinedTextField(
                    value = formState.linkUrl,
                    onValueChange = onLinkChange,
                    label = { Text("Video URL (YouTube) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
                Text(
                    text = "Users will be able to watch this video inside the app.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B) // slate-500
                )
            }
            TrainingResourceType.ARTICLE -> {
                OutlinedTextField(
                    value = formState.linkUrl,
                    onValueChange = onLinkChange,
                    label = { Text("Article URL *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://www.example.com/article") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E3A8A), // blue-900
                        unfocusedBorderColor = Color(0xFFE2E8F0) // slate-200
                    )
                )
            }
            TrainingResourceType.PDF, TrainingResourceType.FDA483 -> {
                FilePickerRow(
                    fileName = formState.selectedFileName ?: formState.existingResourceUrl,
                    onSelectFile = onSelectFile
                )
                Text(
                    text = "Only PDF files are accepted for this resource type.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B) // slate-500
                )
            }
            else -> {
                FilePickerRow(
                    fileName = formState.selectedFileName ?: formState.existingResourceUrl,
                    onSelectFile = onSelectFile
                )
                Text(
                    text = "Supported formats: PDF, PPT, DOC, images.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A) // blue-900
                )
            ) {
                Icon(
                    imageVector = if (formState.isEditing) Icons.Default.Save else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (formState.isEditing) "Update Resource" else "Upload Resource",
                    color = Color.White
                )
            }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.widthIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0F172A) // slate-900
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) // slate-200
            ) {
                Text(
                    "Clear",
                    color = Color(0xFF0F172A) // slate-900
                )
            }
        }
    }
}

@Composable
private fun FilePickerRow(
    fileName: String?,
    onSelectFile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName ?: "No file selected",
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (fileName == null) "Select a file to attach" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(onClick = onSelectFile) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Select")
        }
    }
}

@Composable
private fun ManageResourcesList(
    resources: List<TrainingResource>,
    onEdit: (TrainingResource) -> Unit,
    onDelete: (TrainingResource) -> Unit
) {
    if (resources.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text("No resources yet", fontWeight = FontWeight.Bold)
                Text(
                    "Upload your first training resource to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(resources) { resource ->
            ResourceCard(
                resource = resource,
                onEdit = { onEdit(resource) },
                onDelete = { onDelete(resource) }
            )
        }
    }
}

@Composable
private fun ResourceCard(
    resource: TrainingResource,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val uploadDate = remember(resource.createdAt) {
        if (resource.createdAt > 0) {
            dateFormat.format(resource.createdAt)
        } else {
            "Unknown date"
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A) // slate-900
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uploadDate,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
                ResourceTypeBadge(type = resource.type)
            }
            
            if (resource.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = resource.description,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B), // slate-500
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceTypeBadge(type: TrainingResourceType) {
    val (label, color) = when (type) {
        TrainingResourceType.VIDEO -> "Video" to MaterialTheme.colorScheme.primary
        TrainingResourceType.ARTICLE -> "Article" to MaterialTheme.colorScheme.secondary
        TrainingResourceType.FDA483 -> "FDA 483" to MaterialTheme.colorScheme.tertiary
        TrainingResourceType.PDF -> "PDF" to MaterialTheme.colorScheme.primary
        TrainingResourceType.OTHER -> "Resource" to MaterialTheme.colorScheme.secondary
    }
    AssistChip(
        onClick = { },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = when (type) {
                    TrainingResourceType.VIDEO -> Icons.Default.PlayCircle
                    TrainingResourceType.ARTICLE -> Icons.Default.Article
                    TrainingResourceType.FDA483 -> Icons.Default.Assignment
                    TrainingResourceType.PDF -> Icons.Default.PictureAsPdf
                    TrainingResourceType.OTHER -> Icons.Default.LibraryBooks
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.15f),
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTrainingResourcesScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToFda483: () -> Unit,
    onResourceSelected: (TrainingResource) -> Unit,
    viewModel: TrainingResourcesViewModel = viewModel()
) {
    val resources by viewModel.resources.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        if (resources.isEmpty()) {
            viewModel.refreshResources()
        }
    }
    
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "Resources",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A) // slate-900
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "resources",
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigateToHome()
                        "history" -> onNavigateToHistory()
                        "fda483" -> onNavigateToFda483()
                        "profile" -> onNavigateToPersonalDetails()
                        "resources" -> Unit
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { padding ->
        if (isLoading && resources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (resources.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryBooks,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8), // slate-400
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No resources yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A) // slate-900
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Please check back later for new training content.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B), // slate-500
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(resources) { resource ->
                    ModernUserResourceCard(
                        resource = resource,
                        onClick = { onResourceSelected(resource) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernUserResourceCard(
    resource: TrainingResource,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val uploadDate = remember(resource.createdAt) {
        if (resource.createdAt > 0) {
            dateFormat.format(Date(resource.createdAt))
        } else {
            "Unknown date"
        }
    }
    
    // Determine gradient colors based on resource type
    val (gradientStart, gradientEnd) = when (resource.type) {
        TrainingResourceType.VIDEO -> Pair(Color(0xFF3B82F6), Color(0xFF2563EB)) // blue
        TrainingResourceType.ARTICLE -> Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)) // violet
        TrainingResourceType.FDA483 -> Pair(Color(0xFFEF4444), Color(0xFFDC2626)) // red
        TrainingResourceType.PDF -> Pair(Color(0xFF10B981), Color(0xFF059669)) // emerald
        TrainingResourceType.OTHER -> Pair(Color(0xFF64748B), Color(0xFF475569)) // slate
    }
    
    // Determine button text and icon based on resource type
    val (buttonText, buttonIcon) = when (resource.type) {
        TrainingResourceType.VIDEO -> "Video" to Icons.Default.PlayCircle
        TrainingResourceType.PDF -> "Open" to Icons.Default.PictureAsPdf
        TrainingResourceType.ARTICLE -> "Read" to Icons.Default.Article
        TrainingResourceType.FDA483 -> "View" to Icons.Default.Assignment
        TrainingResourceType.OTHER -> "Open" to Icons.Default.LibraryBooks
    }
    
    // Check if resource has valid URL
    val hasValidUrl = when (resource.type) {
        TrainingResourceType.VIDEO -> resource.videoUrl.isNotBlank()
        else -> resource.resourceUrl.isNotBlank()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored gradient top bar - 6px height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(gradientStart, gradientEnd)
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title and type badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resource.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF64748B) // slate-500
                            )
                            Text(
                                text = "Uploaded on $uploadDate",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B) // slate-500
                            )
                        }
                    }
                }
                
                // Description - always show
                Text(
                    text = if (resource.description.isNotBlank()) {
                        resource.description
                    } else {
                        "No description provided"
                    },
                    fontSize = 14.sp,
                    color = if (resource.description.isNotBlank()) {
                        Color(0xFF0F172A) // slate-900 - darker for actual description
                    } else {
                        Color(0xFF94A3B8) // slate-400 - lighter for placeholder
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Action button
                if (hasValidUrl) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gradientStart
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buttonText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    // Show disabled button if no valid URL
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFFE2E8F0), // slate-200
                            disabledContentColor = Color(0xFF94A3B8) // slate-400
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Unavailable",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String {
    var result = ""
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                result = it.getString(index)
            }
        }
    }
    if (result.isBlank()) {
        result = uri.lastPathSegment ?: "selected_file"
    }
    return result
}

