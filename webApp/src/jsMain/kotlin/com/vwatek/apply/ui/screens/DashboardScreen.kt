package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun DashboardScreen(
    onNavigateToResumes: () -> Unit,
    onNavigateToCoverLetters: () -> Unit,
    onNavigateToInterview: () -> Unit
) {
    Div {
        // Header
        Div(attrs = { classes("mb-lg") }) {
            H1 { Text("Welcome to VwaTek Apply") }
            P(attrs = { classes("text-secondary") }) {
                Text("Your AI-powered career suite. Transform your job hunt into a data-driven strategy.")
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
