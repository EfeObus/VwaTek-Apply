package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.vwatek.apply.presentation.resume.ResumeViewModel
import com.vwatek.apply.presentation.coverletter.CoverLetterViewModel
import com.vwatek.apply.presentation.interview.InterviewViewModel
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

@Composable
fun DashboardScreen(
    onNavigateToResumes: () -> Unit,
    onNavigateToCoverLetters: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToOptimizer: () -> Unit = {}
) {
    val resumeViewModel = remember { GlobalContext.get().get<ResumeViewModel>() }
    val coverLetterViewModel = remember { GlobalContext.get().get<CoverLetterViewModel>() }
    val interviewViewModel = remember { GlobalContext.get().get<InterviewViewModel>() }
    
    val resumeState by resumeViewModel.state.collectAsState()
    val coverLetterState by coverLetterViewModel.state.collectAsState()
    val interviewState by interviewViewModel.state.collectAsState()
    
    // Aggregate errors from all ViewModels
    val activeError = resumeState.error ?: coverLetterState.error ?: interviewState.error
    
    Div {
        // Error banner
        if (activeError != null) {
            Div(attrs = {
                classes("toast", "toast-error")
                style {
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "space-between")
                    property("padding", "12px 16px")
                    property("margin-bottom", "16px")
                    property("background", "#fef2f2")
                    property("border", "1px solid #fca5a5")
                    property("border-radius", "8px")
                    property("color", "#dc2626")
                }
            }) {
                Span { Text(activeError) }
                Button(attrs = {
                    style {
                        property("background", "none")
                        property("border", "none")
                        property("cursor", "pointer")
                        property("color", "#dc2626")
                        property("font-size", "1.2rem")
                    }
                    onClick {
                        resumeViewModel.onIntent(com.vwatek.apply.presentation.resume.ResumeIntent.ClearError)
                        coverLetterViewModel.onIntent(com.vwatek.apply.presentation.coverletter.CoverLetterIntent.ClearError)
                        interviewViewModel.onIntent(com.vwatek.apply.presentation.interview.InterviewIntent.ClearError)
                    }
                }) { Text("×") }
            }
        }
        
        // Loading state
        val isLoading = resumeState.isLoading || coverLetterState.isLoading || interviewState.isLoading
        if (isLoading) {
            Div(attrs = {
                classes("flex", "justify-center", "p-xl")
            }) {
                Span(attrs = { classes("spinner") })
            }
        } else {
        // Header
        Div(attrs = { classes("dashboard-welcome", "mb-xl") }) {
            H1 { Text("Welcome to VwaTek Apply") }
            P(attrs = { classes("text-secondary") }) {
                Text("Your AI-powered career suite. Transform your job hunt into a data-driven strategy.")
            }
        }
        
        // Quick Stats
        Div(attrs = { classes("stats-grid", "mb-xl") }) {
            // Resume count
            Div(attrs = {
                classes("stat-card")
                onClick { onNavigateToResumes() }
                style { property("cursor", "pointer") }
            }) {
                Div(attrs = { classes("stat-value") }) {
                    Text("${resumeState.resumes.size}")
                }
                Div(attrs = { classes("stat-label") }) { Text("Resumes") }
            }
            // Cover Letter count
            Div(attrs = {
                classes("stat-card")
                onClick { onNavigateToCoverLetters() }
                style { property("cursor", "pointer") }
            }) {
                Div(attrs = { classes("stat-value") }) {
                    Text("${coverLetterState.coverLetters.size}")
                }
                Div(attrs = { classes("stat-label") }) { Text("Cover Letters") }
            }
            // Interview count
            Div(attrs = {
                classes("stat-card")
                onClick { onNavigateToInterview() }
                style { property("cursor", "pointer") }
            }) {
                Div(attrs = { classes("stat-value") }) {
                    Text("${interviewState.sessions.size}")
                }
                Div(attrs = { classes("stat-label") }) { Text("Interviews") }
            }
        }
        
        // Getting Started Section
        Div(attrs = { classes("getting-started", "mb-xl") }) {
            H2 { Text("Getting Started") }
            
            Div(attrs = { classes("getting-started-list") }) {
                GettingStartedCard(
                    stepNumber = 1,
                    title = "Create or Upload Your Resume",
                    description = "Start by creating a professional resume or uploading an existing one.",
                    isCompleted = resumeState.resumes.isNotEmpty(),
                    onClick = onNavigateToResumes
                )
                
                GettingStartedCard(
                    stepNumber = 2,
                    title = "Optimize for ATS",
                    description = "Use the Optimizer to check ATS compatibility and rewrite sections.",
                    isCompleted = false,
                    onClick = onNavigateToOptimizer
                )
                
                GettingStartedCard(
                    stepNumber = 3,
                    title = "Generate Cover Letters",
                    description = "Use AI to generate tailored cover letters for specific job postings.",
                    isCompleted = coverLetterState.coverLetters.isNotEmpty(),
                    onClick = onNavigateToCoverLetters
                )
                
                GettingStartedCard(
                    stepNumber = 4,
                    title = "Practice Interviews",
                    description = "Prepare for interviews with AI-powered mock interview sessions.",
                    isCompleted = interviewState.sessions.isNotEmpty(),
                    onClick = onNavigateToInterview
                )
            }
        }
        
        // Quick Actions Grid
        Div(attrs = { classes("feature-cards-grid") }) {
            // Resume Card
            Div(attrs = {
                classes("feature-card")
                onClick { onNavigateToResumes() }
            }) {
                Div(attrs = { classes("feature-card-icon") }) { Text("📄") }
                H3 { Text("Resume Review") }
                P {
                    Text("Analyze your resume against job descriptions and get ATS optimization tips.")
                }
            }
            
            // Cover Letter Card
            Div(attrs = {
                classes("feature-card")
                onClick { onNavigateToCoverLetters() }
            }) {
                Div(attrs = { classes("feature-card-icon") }) { Text("✉️") }
                H3 { Text("Cover Letters") }
                P {
                    Text("Generate tailored cover letters that highlight your skills and experience.")
                }
            }
            
            // Interview Prep Card
            Div(attrs = {
                classes("feature-card")
                onClick { onNavigateToInterview() }
            }) {
                Div(attrs = { classes("feature-card-icon") }) { Text("🎯") }
                H3 { Text("Interview Prep") }
                P {
                    Text("Practice with AI mock interviews and get STAR method coaching.")
                }
            }
            
            // Resume Optimizer Card
            Div(attrs = {
                classes("feature-card")
                onClick { onNavigateToOptimizer() }
            }) {
                Div(attrs = { classes("feature-card-icon") }) { Text("⚡") }
                H3 { Text("Resume Optimizer") }
                P {
                    Text("Check ATS compatibility and rewrite resume sections with AI.")
                }
            }
        }
        
        // Pro Tip Card
        Div(attrs = {
            classes("card", "mb-lg")
            style {
                property("background", "linear-gradient(135deg, var(--color-primary-light, #e3f2fd), var(--color-surface))")
                property("border-left", "4px solid var(--color-primary)")
            }
        }) {
            H3(attrs = {
                classes("card-title", "mb-sm")
                style { property("color", "var(--color-primary)") }
            }) { Text("\uD83D\uDCA1 Pro Tip") }
            P(attrs = { classes("text-secondary") }) {
                Text("Tailor your resume for each job application. Use keywords from the job description to improve your chances of passing ATS systems.")
            }
        }
        
        // Features Section
        Div(attrs = { classes("card") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Key Features") }
            
            Div(attrs = { classes("grid", "grid-2") }) {
                FeatureItem(
                    title = "ATS Optimization",
                    description = "Get your resume past automated screening systems with keyword optimization."
                )
                FeatureItem(
                    title = "Match Scoring",
                    description = "See how well your resume matches specific job descriptions."
                )
                FeatureItem(
                    title = "Impact Enhancement",
                    description = "Convert passive entries into metric-driven achievement statements."
                )
                FeatureItem(
                    title = "STAR Coaching",
                    description = "Structure your experiences using the proven STAR framework."
                )
            }
        }
        } // else (loading)
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Div(attrs = { classes("flex", "gap-md", "mb-md") }) {
        Div(attrs = {
            style {
                property("width", "8px")
                property("height", "8px")
                property("background-color", "var(--color-primary)")
                property("border-radius", "50%")
                property("margin-top", "8px")
                property("flex-shrink", "0")
            }
        })
        Div {
            H4(attrs = { 
                style { 
                    property("font-size", "var(--font-size-md)")
                    property("margin-bottom", "var(--spacing-xs)")
                }
            }) { Text(title) }
            P(attrs = { classes("text-secondary", "text-sm") }) {
                Text(description)
            }
        }
    }
}

@Composable
private fun GettingStartedCard(
    stepNumber: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Div(attrs = {
        classes("step-card")
        onClick { onClick() }
    }) {
        // Step number circle
        Div(attrs = {
            classes("step-number")
            if (isCompleted) {
                style {
                    property("background", "var(--color-success)")
                }
            }
        }) {
            if (isCompleted) {
                Text("✓")
            } else {
                Text("$stepNumber")
            }
        }
        
        // Content
        Div(attrs = { classes("step-content") }) {
            H3 { Text(title) }
            P {
                Text(description)
            }
        }
        
        // Chevron
        Span(attrs = {
            classes("text-secondary")
            style { property("font-size", "1.5rem") }
        }) {
            Text("›")
        }
    }
}
