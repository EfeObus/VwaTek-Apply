package com.vwatek.apply.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.presentation.coverletter.CoverLetterIntent
import com.vwatek.apply.presentation.coverletter.CoverLetterViewModel
import com.vwatek.apply.presentation.resume.ResumeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterScreen() {
    val coverLetterViewModel: CoverLetterViewModel = koinInject()
    val resumeViewModel: ResumeViewModel = koinInject()
    
    val coverLetterState by coverLetterViewModel.state.collectAsState()
    val resumeState by resumeViewModel.state.collectAsState()
    
    var showGenerateSheet by remember { mutableStateOf(false) }
    var selectedCoverLetter by remember { mutableStateOf<CoverLetter?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cover Letters") },
                actions = {
                    IconButton(onClick = { showGenerateSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Generate new")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showGenerateSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Generate") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (coverLetterState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (coverLetterState.coverLetters.isEmpty()) {
                EmptyCoverLetterState(
                    onGenerateNew = { showGenerateSheet = true }
                )
            } else {
                CoverLetterList(
                    coverLetters = coverLetterState.coverLetters,
                    onCoverLetterClick = { selectedCoverLetter = it },
                    onDeleteClick = { coverLetter ->
                        coverLetterViewModel.onIntent(CoverLetterIntent.DeleteCoverLetter(coverLetter.id))
                    }
                )
            }
            
            coverLetterState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { coverLetterViewModel.onIntent(CoverLetterIntent.ClearError) }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
    
    // Watch for successful generation and close sheet
    LaunchedEffect(coverLetterState.generatedCoverLetter) {
        if (coverLetterState.generatedCoverLetter != null) {
            showGenerateSheet = false
            // Clear the generated state after showing
            coverLetterViewModel.onIntent(CoverLetterIntent.ClearGenerated)
        }
    }
    
    // Watch for error during generation
    LaunchedEffect(coverLetterState.error) {
        if (coverLetterState.error != null && coverLetterState.isGenerating.not()) {
            // Keep sheet open on error so user can see the error and retry
        }
    }
    
    if (showGenerateSheet) {
        GenerateCoverLetterSheet(
            resumes = resumeState.resumes,
            isGenerating = coverLetterState.isGenerating,
            error = coverLetterState.error,
            onDismiss = { 
                showGenerateSheet = false
                coverLetterViewModel.onIntent(CoverLetterIntent.ClearError)
            },
            onGenerate = { resumeContent, jobTitle, companyName, jobDescription, tone ->
                coverLetterViewModel.onIntent(
                    CoverLetterIntent.GenerateCoverLetter(
                        resumeContent = resumeContent,
                        jobTitle = jobTitle,
                        companyName = companyName,
                        jobDescription = jobDescription,
                        tone = tone
                    )
                )
                // Don't dismiss here - wait for generation to complete
            }
        )
    }
    
    selectedCoverLetter?.let { coverLetter ->
        CoverLetterDetailSheet(
            coverLetter = coverLetter,
            onDismiss = { selectedCoverLetter = null }
        )
    }
}

@Composable
private fun EmptyCoverLetterState(onGenerateNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Cover Letters Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Generate your first AI-powered cover letter",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onGenerateNew,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Cover Letter")
        }
    }
}

@Composable
private fun CoverLetterList(
    coverLetters: List<CoverLetter>,
    onCoverLetterClick: (CoverLetter) -> Unit,
    onDeleteClick: (CoverLetter) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Your Cover Letters",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(coverLetters.size) { index ->
            val coverLetter = coverLetters[index]
            CoverLetterCard(
                coverLetter = coverLetter,
                onClick = { onCoverLetterClick(coverLetter) },
                onDeleteClick = { onDeleteClick(coverLetter) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverLetterCard(
    coverLetter: CoverLetter,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${coverLetter.jobTitle} at ${coverLetter.companyName}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                coverLetter.tone.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = coverLetter.createdAt.toString().take(10),
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
private fun GenerateCoverLetterSheet(
    resumes: List<Resume>,
    isGenerating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onGenerate: (resumeContent: String, jobTitle: String, companyName: String, jobDescription: String, tone: CoverLetterTone) -> Unit
) {
    var selectedResume by remember { mutableStateOf<Resume?>(null) }
    var jobTitle by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf(CoverLetterTone.PROFESSIONAL) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Generate Cover Letter",
                style = MaterialTheme.typography.headlineSmall
            )
            
            // Resume Dropdown
            if (resumes.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                
                Text(
                    text = "Select Resume (Optional)",
                    style = MaterialTheme.typography.labelLarge
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedResume?.name ?: "No resume selected",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No resume") },
                            onClick = {
                                selectedResume = null
                                expanded = false
                            }
                        )
                        resumes.forEach { resume ->
                            DropdownMenuItem(
                                text = { Text(resume.name) },
                                onClick = {
                                    selectedResume = resume
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text("Job Title *") },
                placeholder = { Text("e.g., Software Engineer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name *") },
                placeholder = { Text("e.g., Google") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = jobDescription,
                onValueChange = { jobDescription = it },
                label = { Text("Job Description") },
                placeholder = { Text("Paste the job description for better results...") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Tone Selection
            Text(
                text = "Tone",
                style = MaterialTheme.typography.labelLarge
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoverLetterTone.entries.forEach { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { selectedTone = tone },
                        label = { Text(tone.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show error if any
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Button(
                onClick = {
                    onGenerate(
                        selectedResume?.content ?: "",
                        jobTitle,
                        companyName,
                        jobDescription,
                        selectedTone
                    )
                    // Don't dismiss - wait for generation to complete
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = jobTitle.isNotBlank() && companyName.isNotBlank() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isGenerating) "Generating..." else "Generate Cover Letter")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverLetterDetailSheet(
    coverLetter: CoverLetter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "${coverLetter.jobTitle} at ${coverLetter.companyName}",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(coverLetter.tone.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                    Text(
                        text = "Created ${coverLetter.createdAt.toString().take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = coverLetter.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Cover Letter", coverLetter.content)
                            clipboardManager.setPrimaryClip(clip)
                            scope.launch {
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}
