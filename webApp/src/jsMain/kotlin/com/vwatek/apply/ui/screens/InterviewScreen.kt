package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.presentation.interview.InterviewIntent
import com.vwatek.apply.presentation.interview.InterviewViewModel
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

@Composable
fun InterviewScreen() {
    val viewModel = remember { GlobalContext.get().get<InterviewViewModel>() }
    val state by viewModel.state.collectAsState()
    
    var showStartModal by remember { mutableStateOf(false) }
    var showStarModal by remember { mutableStateOf(false) }
    
    Div {
        // Active Interview Session
        state.currentSession?.let { session ->
            ActiveInterviewView(
                session = session,
                currentQuestionIndex = state.currentQuestionIndex,
                isSubmitting = state.isSubmittingAnswer,
                lastFeedback = state.lastFeedback,
                onSubmitAnswer = { question, answer ->
                    viewModel.onIntent(InterviewIntent.SubmitAnswer(question, answer, session.jobTitle))
                },
                onNextQuestion = {
                    if (state.currentQuestionIndex < session.questions.size - 1) {
                        viewModel.onIntent(InterviewIntent.SetCurrentQuestion(state.currentQuestionIndex + 1))
                    }
                },
                onPreviousQuestion = {
                    if (state.currentQuestionIndex > 0) {
                        viewModel.onIntent(InterviewIntent.SetCurrentQuestion(state.currentQuestionIndex - 1))
                    }
                },
                onComplete = {
                    viewModel.onIntent(InterviewIntent.CompleteSession(session.id))
                },
                onExit = {
                    viewModel.onIntent(InterviewIntent.SelectSession(null))
                }
            )
        } ?: run {
            // Header
            Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
                Div {
                    H1 { Text("Interview Prep") }
                    P(attrs = { classes("text-secondary") }) {
                        Text("Practice with AI mock interviews and STAR method coaching.")
                    }
                }
                Div(attrs = { classes("flex", "gap-sm") }) {
                    Button(attrs = {
                        classes("btn", "btn-outline", "btn-lg")
                        onClick { showStarModal = true }
                    }) {
                        Text("STAR Coaching")
                    }
                    Button(attrs = {
                        classes("btn", "btn-primary", "btn-lg")
                        onClick { showStartModal = true }
                    }) {
                        Text("+ Start Interview")
                    }
                }
            }
            
            // STAR Response Display
            state.starResponse?.let { star ->
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("flex", "justify-between", "items-center", "mb-md") }) {
                        H3(attrs = { classes("card-title") }) { Text("STAR Method Breakdown") }
                        Button(attrs = {
                            classes("btn", "btn-secondary", "btn-sm")
                            onClick { viewModel.onIntent(InterviewIntent.ClearStarResponse) }
                        }) {
                            Text("Close")
                        }
                    }
                    
                    Div(attrs = { classes("grid", "grid-2") }) {
                        StarSection("Situation", star.situation, "Describe the context")
                        StarSection("Task", star.task, "What was your responsibility")
                        StarSection("Action", star.action, "What steps did you take")
                        StarSection("Result", star.result, "What was the outcome")
                    }
                    
                    if (star.suggestions.isNotEmpty()) {
                        Div(attrs = { classes("mt-lg") }) {
                            H4(attrs = { classes("mb-sm") }) { Text("Improvement Tips") }
                            Ul(attrs = { style { property("padding-left", "20px") } }) {
                                star.suggestions.forEach { tip ->
                                    Li(attrs = { classes("mb-sm", "text-secondary") }) {
                                        Text(tip)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Interview Sessions List
            if (state.isLoading) {
                Div(attrs = { classes("flex", "justify-center", "mt-lg") }) {
                    Div(attrs = { classes("spinner") })
                }
            } else if (state.sessions.isEmpty()) {
                Div(attrs = { classes("empty-state") }) {
                    Div(attrs = {
                        ref { element ->
                            element.innerHTML = """
                                <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                                    <line x1="12" y1="19" x2="12" y2="23"/>
                                    <line x1="8" y1="23" x2="16" y2="23"/>
                                </svg>
                            """
                            onDispose { }
                        }
                    })
                    H3(attrs = { classes("empty-state-title") }) { Text("No interview sessions yet") }
                    P(attrs = { classes("empty-state-description") }) {
                        Text("Start a mock interview to practice answering questions with AI feedback.")
                    }
                    Button(attrs = {
                        classes("btn", "btn-primary", "btn-lg")
                        onClick { showStartModal = true }
                    }) {
                        Text("Start Interview")
                    }
                }
            } else {
                Div(attrs = { classes("grid", "grid-2") }) {
                    state.sessions.forEach { session ->
                        InterviewSessionCard(
                            session = session,
                            onResume = {
                                viewModel.onIntent(InterviewIntent.SelectSession(session.id))
                            },
                            onDelete = {
                                viewModel.onIntent(InterviewIntent.DeleteSession(session.id))
                            }
                        )
                    }
                }
            }
        }
        
        // Start Interview Modal
        if (showStartModal) {
            StartInterviewModal(
                isStarting = state.isStartingSession,
                onClose = { showStartModal = false },
                onStart = { resume, jobTitle, jobDesc ->
                    viewModel.onIntent(InterviewIntent.StartSession(resume, jobTitle, jobDesc))
                    showStartModal = false
                }
            )
        }
        
        // STAR Coaching Modal
        if (showStarModal) {
            StarCoachingModal(
                isLoading = state.isGettingStarCoaching,
                onClose = { showStarModal = false },
                onSubmit = { experience, context ->
                    viewModel.onIntent(InterviewIntent.GetStarCoaching(experience, context))
                    showStarModal = false
                }
            )
        }
        
        // Error Toast
        state.error?.let { error ->
            Div(attrs = { classes("toast", "toast-error") }) {
                Text(error)
                Button(attrs = {
                    classes("btn", "btn-sm")
                    onClick { viewModel.onIntent(InterviewIntent.ClearError) }
                }) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun StarSection(title: String, content: String, subtitle: String) {
    Div(attrs = { classes("card", "mb-md") }) {
        H4(attrs = { 
            style { 
                property("color", "var(--color-primary)")
                property("margin-bottom", "var(--spacing-xs)")
            }
        }) { Text(title) }
        P(attrs = { classes("text-secondary", "text-sm", "mb-sm") }) {
            Text(subtitle)
        }
        P { Text(content) }
    }
}

@Composable
private fun InterviewSessionCard(
    session: InterviewSession,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    Div(attrs = { classes("card") }) {
        Div(attrs = { classes("card-header") }) {
            H3(attrs = { classes("card-title") }) { Text(session.jobTitle) }
            Span(attrs = { 
                classes("badge")
                when (session.status) {
                    InterviewStatus.IN_PROGRESS -> classes("badge-warning")
                    InterviewStatus.COMPLETED -> classes("badge-success")
                    InterviewStatus.CANCELLED -> classes("badge-error")
                }
            }) {
                Text(session.status.name.replace("_", " "))
            }
        }
        
        P(attrs = { 
            classes("text-secondary", "text-sm", "mb-md")
            style {
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("display", "-webkit-box")
                property("-webkit-line-clamp", "2")
                property("-webkit-box-orient", "vertical")
            }
        }) {
            Text(session.jobDescription.take(100) + "...")
        }
        
        P(attrs = { classes("text-sm", "mb-md") }) {
            Text("${session.questions.size} questions")
        }
        
        Div(attrs = { classes("flex", "gap-sm") }) {
            if (session.status == InterviewStatus.IN_PROGRESS) {
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick { onResume() }
                }) {
                    Text("Continue")
                }
            } else {
                Button(attrs = {
                    classes("btn", "btn-outline")
                    onClick { onResume() }
                }) {
                    Text("Review")
                }
            }
            Button(attrs = {
                classes("btn", "btn-danger")
                onClick { onDelete() }
            }) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun ActiveInterviewView(
    session: InterviewSession,
    currentQuestionIndex: Int,
    isSubmitting: Boolean,
    lastFeedback: String?,
    onSubmitAnswer: (InterviewQuestion, String) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onComplete: () -> Unit,
    onExit: () -> Unit
) {
    var currentAnswer by remember { mutableStateOf("") }
    val currentQuestion = session.questions.getOrNull(currentQuestionIndex)
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Div {
                H2 { Text("Mock Interview") }
                P(attrs = { classes("text-secondary") }) {
                    Text("${session.jobTitle} - Question ${currentQuestionIndex + 1} of ${session.questions.size}")
                }
            }
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick { onExit() }
            }) {
                Text("Exit Interview")
            }
        }
        
        // Progress Bar
        Div(attrs = { classes("progress-bar", "mb-lg") }) {
            Div(attrs = {
                classes("progress-fill")
                style { 
                    property("width", "${((currentQuestionIndex + 1).toFloat() / session.questions.size * 100).toInt()}%")
                }
            })
        }
        
        currentQuestion?.let { question ->
            // Question Card
            Div(attrs = { classes("card", "mb-lg") }) {
                H3(attrs = { classes("mb-md") }) { Text(question.question) }
                
                // Previous Answer and Feedback
                question.userAnswer?.let { answer ->
                    Div(attrs = { 
                        classes("mb-md")
                        style {
                            property("padding", "var(--spacing-md)")
                            property("background-color", "var(--color-surface-variant)")
                            property("border-radius", "var(--radius-md)")
                        }
                    }) {
                        H4(attrs = { classes("text-sm", "mb-sm") }) { Text("Your Answer:") }
                        P { Text(answer) }
                    }
                    
                    question.aiFeedback?.let { feedback ->
                        Div(attrs = { 
                            classes("mb-md")
                            style {
                                property("padding", "var(--spacing-md)")
                                property("background-color", "rgba(25, 118, 210, 0.1)")
                                property("border-radius", "var(--radius-md)")
                                property("border-left", "4px solid var(--color-primary)")
                            }
                        }) {
                            H4(attrs = { classes("text-sm", "mb-sm") }) { Text("AI Feedback:") }
                            P { Text(feedback) }
                        }
                    }
                }
                
                // Answer Input
                if (question.userAnswer == null) {
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Your Answer") }
                        TextArea {
                            classes("form-textarea")
                            placeholder("Type your answer here... Be specific and use concrete examples.")
                            value(currentAnswer)
                            onInput { currentAnswer = it.value }
                            style { property("min-height", "150px") }
                        }
                    }
                    
                    Button(attrs = {
                        classes("btn", "btn-primary", "btn-lg")
                        if (isSubmitting || currentAnswer.isBlank()) {
                            attr("disabled", "true")
                        }
                        onClick {
                            onSubmitAnswer(question, currentAnswer)
                            currentAnswer = ""
                        }
                    }) {
                        if (isSubmitting) {
                            Text("Getting Feedback...")
                        } else {
                            Text("Submit Answer")
                        }
                    }
                }
            }
            
            // Navigation
            Div(attrs = { classes("flex", "justify-between") }) {
                Button(attrs = {
                    classes("btn", "btn-outline")
                    if (currentQuestionIndex == 0) {
                        attr("disabled", "true")
                    }
                    onClick { onPreviousQuestion() }
                }) {
                    Text("Previous")
                }
                
                if (currentQuestionIndex == session.questions.size - 1) {
                    Button(attrs = {
                        classes("btn", "btn-primary")
                        onClick { onComplete() }
                    }) {
                        Text("Complete Interview")
                    }
                } else {
                    Button(attrs = {
                        classes("btn", "btn-primary")
                        onClick { onNextQuestion() }
                    }) {
                        Text("Next Question")
                    }
                }
            }
        }
    }
}

@Composable
private fun StartInterviewModal(
    isStarting: Boolean,
    onClose: () -> Unit,
    onStart: (resume: String?, jobTitle: String, jobDesc: String) -> Unit
) {
    var resumeContent by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("Start Mock Interview") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Title") }
                    Input(InputType.Text) {
                        classes("form-input")
                        placeholder("e.g., Senior Software Engineer")
                        value(jobTitle)
                        onInput { jobTitle = it.value }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Description") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Paste the job description...")
                        value(jobDescription)
                        onInput { jobDescription = it.value }
                        style { property("min-height", "120px") }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Your Resume (Optional)") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Paste your resume to get personalized questions...")
                        value(resumeContent)
                        onInput { resumeContent = it.value }
                        style { property("min-height", "100px") }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("Adding your resume helps generate more relevant questions.")
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) {
                    Text("Cancel")
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (isStarting || jobTitle.isBlank() || jobDescription.isBlank()) {
                        attr("disabled", "true")
                    }
                    onClick { 
                        onStart(
                            resumeContent.ifBlank { null },
                            jobTitle,
                            jobDescription
                        )
                    }
                }) {
                    if (isStarting) {
                        Text("Starting...")
                    } else {
                        Text("Start Interview")
                    }
                }
            }
        }
    }
}

@Composable
private fun StarCoachingModal(
    isLoading: Boolean,
    onClose: () -> Unit,
    onSubmit: (experience: String, context: String) -> Unit
) {
    var experience by remember { mutableStateOf("") }
    var jobContext by remember { mutableStateOf("") }
    
    Div(attrs = { classes("modal-backdrop") }) {
        Div(attrs = { classes("modal") }) {
            Div(attrs = { classes("modal-header") }) {
                H3(attrs = { classes("modal-title") }) { Text("STAR Method Coaching") }
                Button(attrs = {
                    classes("modal-close")
                    onClick { onClose() }
                }) {
                    Text("X")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                P(attrs = { classes("mb-md", "text-secondary") }) {
                    Text("Describe an experience and get help structuring it using the STAR method (Situation, Task, Action, Result).")
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Your Experience") }
                    TextArea {
                        classes("form-textarea")
                        placeholder("Describe a work experience, achievement, or challenge you faced...")
                        value(experience)
                        onInput { experience = it.value }
                        style { property("min-height", "120px") }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Context") }
                    Input(InputType.Text) {
                        classes("form-input")
                        placeholder("e.g., Software Engineer at a startup")
                        value(jobContext)
                        onInput { jobContext = it.value }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("The role you're targeting helps tailor the coaching.")
                    }
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-secondary")
                    onClick { onClose() }
                }) {
                    Text("Cancel")
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (isLoading || experience.isBlank() || jobContext.isBlank()) {
                        attr("disabled", "true")
                    }
                    onClick { onSubmit(experience, jobContext) }
                }) {
                    if (isLoading) {
                        Text("Analyzing...")
                    } else {
                        Text("Get Coaching")
                    }
                }
            }
        }
    }
}
