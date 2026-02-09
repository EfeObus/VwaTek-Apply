package com.vwatek.apply.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.usecase.StarResponse
import com.vwatek.apply.presentation.interview.InterviewIntent
import com.vwatek.apply.presentation.interview.InterviewViewModel
import com.vwatek.apply.presentation.resume.ResumeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewScreen() {
    val viewModel: InterviewViewModel = koinInject()
    val resumeViewModel: ResumeViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val resumeState by resumeViewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showSetupDialog by remember { mutableStateOf(false) }
    var showStarCoachingDialog by remember { mutableStateOf(false) }
    
    // Show snackbar for STAR coaching copy confirmation
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (state.currentSession != null) {
                ActiveInterviewContent(
                    state = state,
                    viewModel = viewModel
                )
            } else {
                InterviewSetupContent(
                    sessions = state.sessions,
                    isLoading = state.isLoading,
                    onStartInterview = { showSetupDialog = true },
                    onStarCoaching = { showStarCoachingDialog = true },
                    onSessionClick = { session ->
                        viewModel.onIntent(InterviewIntent.SelectSession(session.id))
                    }
                )
            }
        }
    }
    
    if (showSetupDialog) {
        InterviewSetupDialog(
            resumes = resumeState.resumes,
            onDismiss = { showSetupDialog = false },
            onStartInterview = { selectedResume, jobTitle, jobDescription ->
                viewModel.onIntent(
                    InterviewIntent.StartSession(
                        resumeContent = selectedResume?.content,
                        jobTitle = jobTitle,
                        jobDescription = jobDescription
                    )
                )
                showSetupDialog = false
            }
        )
    }
    
    if (showStarCoachingDialog) {
        StarCoachingDialog(
            state = state,
            viewModel = viewModel,
            context = context,
            snackbarHostState = snackbarHostState,
            onDismiss = { 
                showStarCoachingDialog = false
                viewModel.onIntent(InterviewIntent.ClearStarResponse)
            }
        )
    }
}

@Composable
private fun InterviewSetupContent(
    sessions: List<InterviewSession>,
    isLoading: Boolean,
    onStartInterview: () -> Unit,
    onStarCoaching: () -> Unit,
    onSessionClick: (InterviewSession) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "AI Mock Interview",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Practice with AI-powered interviews and get instant feedback",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onStartInterview,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start New Interview")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onStarCoaching,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STAR Method Coaching")
                    }
                }
            }
        }
        
        // STAR Method Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STAR Method",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Structure your answers using:\n• Situation - Set the context\n• Task - Describe your responsibility\n• Action - Explain what you did\n• Result - Share the outcome",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        if (sessions.isNotEmpty()) {
            item {
                Text(
                    text = "Previous Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            items(sessions) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: InterviewSession,
    onClick: () -> Unit
) {
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
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.jobTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${session.questions.size} questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (session.status == com.vwatek.apply.domain.model.InterviewStatus.COMPLETED) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActiveInterviewContent(
    state: com.vwatek.apply.presentation.interview.InterviewState,
    viewModel: InterviewViewModel
) {
    val currentSession = state.currentSession ?: return
    val questions = currentSession.questions
    val currentIndex = state.currentQuestionIndex
    val currentQuestion = questions.getOrNull(currentIndex)
    
    var answer by remember(currentIndex) { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / questions.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${currentIndex + 1} of ${questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextButton(
                onClick = {
                    viewModel.onIntent(InterviewIntent.CompleteSession(currentSession.id))
                    viewModel.onIntent(InterviewIntent.SelectSession(null))
                }
            ) {
                Text("End Interview")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (currentQuestion != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Your Answer") },
                placeholder = { Text("Type your answer here...") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (state.lastFeedback != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Feedback",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.lastFeedback!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = {
                            viewModel.onIntent(InterviewIntent.SetCurrentQuestion(currentIndex - 1))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Previous")
                    }
                }
                
                Button(
                    onClick = {
                        if (answer.isNotBlank()) {
                            viewModel.onIntent(
                                InterviewIntent.SubmitAnswer(
                                    question = currentQuestion,
                                    answer = answer,
                                    jobTitle = currentSession.jobTitle
                                )
                            )
                            if (currentIndex < questions.size - 1) {
                                viewModel.onIntent(InterviewIntent.SetCurrentQuestion(currentIndex + 1))
                                answer = ""
                            }
                        }
                    },
                    enabled = answer.isNotBlank() && !state.isSubmittingAnswer,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSubmittingAnswer) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (currentIndex < questions.size - 1) "Submit & Next" else "Submit")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (state.isStartingSession) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating questions...")
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Interview Complete!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(InterviewIntent.SelectSession(null))
                            }
                        ) {
                            Text("Return to Home")
                        }
                    }
                }
            }
        }
        
        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.onIntent(InterviewIntent.ClearError) }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterviewSetupDialog(
    resumes: List<Resume>,
    onDismiss: () -> Unit,
    onStartInterview: (selectedResume: Resume?, jobTitle: String, jobDescription: String) -> Unit
) {
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedResume by remember { mutableStateOf<Resume?>(null) }
    var resumeDropdownExpanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Mock Interview") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resume Selection Dropdown
                if (resumes.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = resumeDropdownExpanded,
                        onExpandedChange = { resumeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedResume?.name ?: "Select a resume (optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Resume") },
                            trailingIcon = { 
                                Row {
                                    if (selectedResume != null) {
                                        IconButton(
                                            onClick = { selectedResume = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = resumeDropdownExpanded) 
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = resumeDropdownExpanded,
                            onDismissRequest = { resumeDropdownExpanded = false }
                        ) {
                            resumes.forEach { resume ->
                                DropdownMenuItem(
                                    text = { Text(resume.name) },
                                    onClick = {
                                        selectedResume = resume
                                        resumeDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "Including your resume helps generate more relevant questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title") },
                    placeholder = { Text("e.g., Software Engineer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    label = { Text("Job Description") },
                    placeholder = { Text("Paste the job description or key requirements...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartInterview(selectedResume, jobTitle, jobDescription) },
                enabled = jobTitle.isNotBlank() && jobDescription.isNotBlank()
            ) {
                Text("Start Interview")
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
private fun StarCoachingDialog(
    state: com.vwatek.apply.presentation.interview.InterviewState,
    viewModel: InterviewViewModel,
    context: Context,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    var experience by remember { mutableStateOf("") }
    var jobContext by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("STAR Method Coaching") 
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input Section (only show if no result yet)
                if (state.starResponse == null) {
                    Text(
                        text = "Describe your experience and get AI-powered STAR format coaching",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = experience,
                        onValueChange = { experience = it },
                        label = { Text("Your Experience") },
                        placeholder = { Text("Describe an accomplishment or challenge you faced...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = jobContext,
                        onValueChange = { jobContext = it },
                        label = { Text("Job Context") },
                        placeholder = { Text("e.g., Software Engineer at a tech startup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            if (experience.isNotBlank() && jobContext.isNotBlank()) {
                                viewModel.onIntent(InterviewIntent.GetStarCoaching(experience, jobContext))
                            }
                        },
                        enabled = experience.isNotBlank() && jobContext.isNotBlank() && !state.isGettingStarCoaching,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isGettingStarCoaching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate STAR Response")
                        }
                    }
                }
                
                // Result Section
                state.starResponse?.let { starResponse ->
                    StarResponseContent(
                        starResponse = starResponse,
                        onCopy = {
                            val fullContent = buildStarContent(starResponse)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("STAR Response", fullContent)
                            clipboard.setPrimaryClip(clip)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("STAR response copied to clipboard")
                            }
                        },
                        onReset = {
                            viewModel.onIntent(InterviewIntent.ClearStarResponse)
                            experience = ""
                            jobContext = ""
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun StarResponseContent(
    starResponse: StarResponse,
    onCopy: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your STAR Response",
                style = MaterialTheme.typography.titleMedium
            )
            Row {
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        // Situation Card
        StarCard(
            title = "Situation",
            content = starResponse.situation,
            color = Color(0xFF4CAF50)
        )
        
        // Task Card
        StarCard(
            title = "Task",
            content = starResponse.task,
            color = Color(0xFF2196F3)
        )
        
        // Action Card
        StarCard(
            title = "Action",
            content = starResponse.action,
            color = Color(0xFFFF9800)
        )
        
        // Result Card
        StarCard(
            title = "Result",
            content = starResponse.result,
            color = Color(0xFF9C27B0)
        )
        
        // Suggestions
        if (starResponse.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Improvement Suggestions",
                style = MaterialTheme.typography.titleSmall
            )
            
            starResponse.suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StarCard(
    title: String,
    content: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = color,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = title.first().toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun buildStarContent(starResponse: StarResponse): String {
    return buildString {
        appendLine("SITUATION:")
        appendLine(starResponse.situation)
        appendLine()
        appendLine("TASK:")
        appendLine(starResponse.task)
        appendLine()
        appendLine("ACTION:")
        appendLine(starResponse.action)
        appendLine()
        appendLine("RESULT:")
        appendLine(starResponse.result)
        if (starResponse.suggestions.isNotEmpty()) {
            appendLine()
            appendLine("SUGGESTIONS:")
            starResponse.suggestions.forEach { suggestion ->
                appendLine("• $suggestion")
            }
        }
    }
}
