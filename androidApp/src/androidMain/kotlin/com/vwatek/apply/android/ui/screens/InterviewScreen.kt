package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.presentation.interview.InterviewIntent
import com.vwatek.apply.presentation.interview.InterviewViewModel
import com.vwatek.apply.presentation.resume.ResumeViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewScreen() {
    val viewModel: InterviewViewModel = koinInject()
    val resumeViewModel: ResumeViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val resumeState by resumeViewModel.state.collectAsState()
    
    var showSetupDialog by remember { mutableStateOf(false) }
    
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
            onSessionClick = { session ->
                viewModel.onIntent(InterviewIntent.SelectSession(session.id))
            }
        )
    }
    
    if (showSetupDialog) {
        InterviewSetupDialog(
            onDismiss = { showSetupDialog = false },
            onStartInterview = { jobTitle, jobDescription ->
                val resumeContent = resumeState.resumes.firstOrNull()?.content
                viewModel.onIntent(
                    InterviewIntent.StartSession(
                        resumeContent = resumeContent,
                        jobTitle = jobTitle,
                        jobDescription = jobDescription
                    )
                )
                showSetupDialog = false
            }
        )
    }
}

@Composable
private fun InterviewSetupContent(
    sessions: List<InterviewSession>,
    isLoading: Boolean,
    onStartInterview: () -> Unit,
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
    onDismiss: () -> Unit,
    onStartInterview: (jobTitle: String, jobDescription: String) -> Unit
) {
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Mock Interview") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                onClick = { onStartInterview(jobTitle, jobDescription) },
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
