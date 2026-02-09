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
    
    Div {
        // Header
        Div(attrs = { classes("mb-lg") }) {
            H1 { Text("Welcome to VwaTek Apply") }
            P(attrs = { classes("text-secondary") }) {
                Text("Your AI-powered career suite. Transform your job hunt into a data-driven strategy.")
            }
        }
        
        // Getting Started Section
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Getting Started") }
            
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
        Div(attrs = { classes("grid", "grid-3", "mb-lg") }) {
            // Resume Card
            Div(attrs = {
                classes("card")
                onClick { onNavigateToResumes() }
                style { property("cursor", "pointer") }
            }) {
                Div(attrs = { classes("card-header") }) {
                    H3(attrs = { classes("card-title") }) { Text("Resume Review") }
                }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("Analyze your resume against job descriptions and get ATS optimization tips.")
                }
                Div(attrs = { classes("mt-md") }) {
                    Button(attrs = { classes("btn", "btn-primary") }) {
                        Text("Get Started")
                    }
                }
            }
            
            // Cover Letter Card
            Div(attrs = {
                classes("card")
                onClick { onNavigateToCoverLetters() }
                style { property("cursor", "pointer") }
            }) {
                Div(attrs = { classes("card-header") }) {
                    H3(attrs = { classes("card-title") }) { Text("Cover Letters") }
                }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("Generate tailored cover letters that highlight your skills and experience.")
                }
                Div(attrs = { classes("mt-md") }) {
                    Button(attrs = { classes("btn", "btn-primary") }) {
                        Text("Create New")
                    }
                }
            }
            
            // Interview Prep Card
            Div(attrs = {
                classes("card")
                onClick { onNavigateToInterview() }
                style { property("cursor", "pointer") }
            }) {
                Div(attrs = { classes("card-header") }) {
                    H3(attrs = { classes("card-title") }) { Text("Interview Prep") }
                }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("Practice with AI mock interviews and get STAR method coaching.")
                }
                Div(attrs = { classes("mt-md") }) {
                    Button(attrs = { classes("btn", "btn-primary") }) {
                        Text("Start Practice")
                    }
                }
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
        classes("getting-started-card")
        onClick { onClick() }
        style {
            property("display", "flex")
            property("align-items", "center")
            property("gap", "var(--spacing-md)")
            property("padding", "var(--spacing-md)")
            property("background", "var(--color-surface-variant)")
            property("border-radius", "var(--radius-md)")
            property("cursor", "pointer")
            property("transition", "var(--transition-normal)")
            property("margin-bottom", "var(--spacing-sm)")
        }
    }) {
        // Step number circle
        Div(attrs = {
            style {
                property("width", "36px")
                property("height", "36px")
                property("border-radius", "50%")
                property("background", if (isCompleted) "var(--color-success)" else "var(--color-primary)")
                property("color", "white")
                property("display", "flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("font-weight", "600")
                property("flex-shrink", "0")
            }
        }) {
            if (isCompleted) {
                Text("✓")
            } else {
                Text("$stepNumber")
            }
        }
        
        // Content
        Div(attrs = { style { property("flex", "1") } }) {
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
        
        // Chevron
        Span(attrs = {
            classes("text-secondary")
            style { property("font-size", "1.2rem") }
        }) {
            Text("›")
        }
    }
}
