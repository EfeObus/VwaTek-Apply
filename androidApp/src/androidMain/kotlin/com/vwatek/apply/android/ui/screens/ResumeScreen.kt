package com.vwatek.apply.android.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vwatek.apply.android.auth.LinkedInAuthHelper
import com.vwatek.apply.android.util.PdfExportUtil
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.presentation.auth.AuthIntent
import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.presentation.resume.ResumeIntent
import com.vwatek.apply.presentation.resume.ResumeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeScreen() {
    val viewModel: ResumeViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    val authViewModel: AuthViewModel = koinInject()
    val authState by authViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var uploadedFileName by remember { mutableStateOf("") }
    var uploadedContent by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showLinkedInImportDialog by remember { mutableStateOf(false) }
    var linkedInImportLoading by remember { mutableStateOf(false) }
    
    // PDF Export state
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var resumeToExport by remember { mutableStateOf<Resume?>(null) }
    val pdfExportUtil = remember { PdfExportUtil(context) }
    
    // Version History state
    var showVersionHistoryDialog by remember { mutableStateOf(false) }
    var resumeForVersionHistory by remember { mutableStateOf<Resume?>(null) }
    
    val linkedInHelper = remember { LinkedInAuthHelper(context) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "Uploaded Resume"
                
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                } ?: ""
                
                uploadedFileName = fileName.removeSuffix(".txt").removeSuffix(".pdf").removeSuffix(".docx")
                uploadedContent = content
                showUploadDialog = true
            } catch (e: Exception) {
                // Handle error - could show a snackbar
                e.printStackTrace()
            }
        }
    }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showOptionsDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Resume") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.resumes.isEmpty()) {
                EmptyResumeState(
                    onCreateNew = { showCreateDialog = true }
                )
            } else {
                ResumeList(
                    resumes = state.resumes,
                    selectedResumeId = state.selectedResume?.id,
                    onResumeClick = { resume ->
                        viewModel.onIntent(ResumeIntent.SelectResume(resume.id))
                    },
                    onDeleteClick = { resume ->
                        viewModel.onIntent(ResumeIntent.DeleteResume(resume.id))
                    },
                    onExportClick = { resume ->
                        resumeToExport = resume
                        showPdfExportDialog = true
                    },
                    onVersionHistoryClick = { resume ->
                        resumeForVersionHistory = resume
                        showVersionHistoryDialog = true
                        viewModel.onIntent(ResumeIntent.LoadVersionHistory(resume.id))
                    }
                )
            }
            
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onIntent(ResumeIntent.ClearError) }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateResumeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, content, industry ->
                viewModel.onIntent(ResumeIntent.CreateResume(name, content, industry))
                showCreateDialog = false
            }
        )
    }
    
    // Options Dialog - Choose between upload or create
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Add Resume") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How would you like to add your resume?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Upload option
                    OutlinedCard(
                        onClick = {
                            showOptionsDialog = false
                            filePickerLauncher.launch(arrayOf(
                                "text/plain",
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            ))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Upload Resume",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Upload from your device (PDF, DOCX, TXT)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Create option
                    OutlinedCard(
                        onClick = {
                            showOptionsDialog = false
                            showCreateDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Create New Resume",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Build a new resume from scratch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // LinkedIn Import option
                    OutlinedCard(
                        onClick = {
                            showOptionsDialog = false
                            showLinkedInImportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF0A66C2))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "in",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF0A66C2)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Import from LinkedIn",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Import your profile automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // LinkedIn Import Dialog
    if (showLinkedInImportDialog) {
        LinkedInImportDialog(
            isLoading = linkedInImportLoading || authState.isLoading,
            onDismiss = { showLinkedInImportDialog = false },
            onImport = {
                linkedInImportLoading = true
                linkedInHelper.startLinkedInAuthSimple()
                // Note: The actual import happens when the user returns from the OAuth flow
                // This will need deep link handling to complete the flow
                showLinkedInImportDialog = false
                linkedInImportLoading = false
            }
        )
    }
    
    // Check if LinkedIn import completed (via authState)
    LaunchedEffect(authState.uploadedResume) {
        authState.uploadedResume?.let { resume ->
            Log.d("ResumeScreen", "LinkedIn import completed: ${resume.name}")
            // Refresh resumes list
            viewModel.onIntent(ResumeIntent.LoadResumes)
        }
    }
    
    // Upload confirmation dialog
    if (showUploadDialog) {
        UploadResumeDialog(
            fileName = uploadedFileName,
            content = uploadedContent,
            onDismiss = { 
                showUploadDialog = false
                uploadedFileName = ""
                uploadedContent = ""
            },
            onCreate = { name, content, industry ->
                viewModel.onIntent(ResumeIntent.CreateResume(name, content, industry))
                showUploadDialog = false
                uploadedFileName = ""
                uploadedContent = ""
            }
        )
    }
    
    // PDF Export Template Selection Dialog
    if (showPdfExportDialog && resumeToExport != null) {
        PdfExportDialog(
            resume = resumeToExport!!,
            onDismiss = {
                showPdfExportDialog = false
                resumeToExport = null
            },
            onExport = { template ->
                val uri = pdfExportUtil.exportToPdf(resumeToExport!!, template)
                showPdfExportDialog = false
                resumeToExport = null
                
                if (uri != null) {
                    pdfExportUtil.sharePdf(uri)
                } else {
                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    // Version History Dialog
    if (showVersionHistoryDialog && resumeForVersionHistory != null) {
        VersionHistoryDialog(
            resume = resumeForVersionHistory!!,
            versions = state.versionHistory,
            isLoading = state.isLoadingVersions,
            isRestoring = state.isRestoringVersion,
            onDismiss = {
                showVersionHistoryDialog = false
                resumeForVersionHistory = null
                viewModel.onIntent(ResumeIntent.ClearVersionHistory)
            },
            onRestoreVersion = { versionId ->
                viewModel.onIntent(ResumeIntent.RestoreVersion(resumeForVersionHistory!!.id, versionId))
            }
        )
    }
    
    // Handle version restore success
    LaunchedEffect(state.versionRestoreSuccess) {
        if (state.versionRestoreSuccess) {
            Toast.makeText(context, "Version restored successfully", Toast.LENGTH_SHORT).show()
            showVersionHistoryDialog = false
            resumeForVersionHistory = null
            viewModel.onIntent(ResumeIntent.ClearVersionHistory)
        }
    }
}

@Composable
private fun EmptyResumeState(onCreateNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Resumes Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Create your first professional resume to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateNew,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Resume")
        }
    }
}

@Composable
private fun ResumeList(
    resumes: List<Resume>,
    selectedResumeId: String?,
    onResumeClick: (Resume) -> Unit,
    onDeleteClick: (Resume) -> Unit,
    onExportClick: (Resume) -> Unit,
    onVersionHistoryClick: (Resume) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Your Resumes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(resumes) { resume ->
            ResumeCard(
                resume = resume,
                isSelected = resume.id == selectedResumeId,
                onClick = { onResumeClick(resume) },
                onDeleteClick = { onDeleteClick(resume) },
                onExportClick = { onExportClick(resume) },
                onVersionHistoryClick = { onVersionHistoryClick(resume) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeCard(
    resume: Resume,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit,
    onVersionHistoryClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resume.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (resume.industry != null) {
                        AssistChip(
                            onClick = { },
                            label = { Text(resume.industry!!, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = "Updated ${resume.updatedAt.toString().take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export PDF") },
                        onClick = {
                            showMenu = false
                            onExportClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Version History") },
                        onClick = {
                            showMenu = false
                            onVersionHistoryClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateResumeDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, content: String, industry: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Resume") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Resume Name") },
                    placeholder = { Text("e.g., Software Engineer Resume") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry (Optional)") },
                    placeholder = { Text("e.g., Technology") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Resume Content") },
                    placeholder = { Text("Paste your resume content here...") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, content, industry.ifBlank { null }) },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadResumeDialog(
    fileName: String,
    content: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, content: String, industry: String?) -> Unit
) {
    var name by remember { mutableStateOf(fileName) }
    var editedContent by remember { mutableStateOf(content) }
    var industry by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Upload") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Review and confirm your uploaded resume:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Resume Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry (Optional)") },
                    placeholder = { Text("e.g., Technology") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    label = { Text("Resume Content") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (content.length > 100) {
                    Text(
                        text = "${content.length} characters extracted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, editedContent, industry.ifBlank { null }) },
                enabled = name.isNotBlank() && editedContent.isNotBlank()
            ) {
                Text("Save Resume")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LinkedInImportDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "in",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF0A66C2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import from LinkedIn")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Import your LinkedIn profile to automatically create a resume with your:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("•", color = Color(0xFF0A66C2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Work experience", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("•", color = Color(0xFF0A66C2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Education history", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("•", color = Color(0xFF0A66C2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Skills and certifications", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("•", color = Color(0xFF0A66C2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Professional summary", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "You'll be redirected to LinkedIn to authorize access to your profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A66C2)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Connect LinkedIn")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfExportDialog(
    resume: Resume,
    onDismiss: () -> Unit,
    onExport: (PdfExportUtil.Template) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(PdfExportUtil.Template.PROFESSIONAL) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export to PDF") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose a template for \"${resume.name}\":",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Template options
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfExportUtil.Template.values().forEach { template ->
                        TemplateOption(
                            template = template,
                            isSelected = selectedTemplate == template,
                            onClick = { selectedTemplate = template }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(selectedTemplate) }) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TemplateOption(
    template: PdfExportUtil.Template,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (title, description, color) = when (template) {
        PdfExportUtil.Template.PROFESSIONAL -> Triple(
            "Professional",
            "Clean design with blue accents, ideal for corporate roles",
            Color(0xFF1a365d)
        )
        PdfExportUtil.Template.MODERN -> Triple(
            "Modern",
            "Fresh look with teal highlights, great for tech & creative",
            Color(0xFF0ea5e9)
        )
        PdfExportUtil.Template.CLASSIC -> Triple(
            "Classic",
            "Traditional serif fonts, perfect for academia & law",
            Color(0xFF374151)
        )
        PdfExportUtil.Template.MINIMAL -> Triple(
            "Minimal",
            "Simple and elegant with lots of whitespace",
            Color(0xFF6b7280)
        )
    }
    
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) 
                color.copy(alpha = 0.08f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color,
                modifier = Modifier.size(32.dp)
            ) {}
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Selected",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionHistoryDialog(
    resume: Resume,
    versions: List<ResumeVersion>,
    isLoading: Boolean,
    isRestoring: Boolean,
    onDismiss: () -> Unit,
    onRestoreVersion: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Version History")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = resume.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    versions.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No version history yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Versions are created when you edit your resume",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(versions) { version ->
                                VersionHistoryItem(
                                    version = version,
                                    isRestoring = isRestoring,
                                    onRestore = { onRestoreVersion(version.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRestoring
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun VersionHistoryItem(
    version: ResumeVersion,
    isRestoring: Boolean,
    onRestore: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Version ${version.versionNumber}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = version.changeDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = version.createdAtFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedButton(
                onClick = onRestore,
                enabled = !isRestoring,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Restore", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
