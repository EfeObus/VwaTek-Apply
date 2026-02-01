package com.vwatek.apply.ui.screens

import androidx.compose.runtime.*
import com.vwatek.apply.domain.model.ATSAnalysis
import com.vwatek.apply.domain.model.ATSIssue
import com.vwatek.apply.domain.model.ATSRecommendation
import com.vwatek.apply.domain.model.IssueSeverity
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.presentation.resume.ResumeIntent
import com.vwatek.apply.presentation.resume.ResumeViewModel
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

@Composable
fun ResumeOptimizerScreen() {
    val viewModel = remember { GlobalContext.get().get<ResumeViewModel>() }
    val state by viewModel.state.collectAsState()
    
    var selectedResumeId by remember { mutableStateOf<String?>(null) }
    var targetJobDescription by remember { mutableStateOf("") }
    var targetKeywords by remember { mutableStateOf("") }
    var showAnalysisResults by remember { mutableStateOf(false) }
    
    val selectedResume = state.resumes.find { resume -> resume.id == selectedResumeId }
    
    Div {
        // Header
        Div(attrs = { classes("mb-xl") }) {
            H1(attrs = { classes("mb-xs") }) {
                Text("Resume Optimizer")
            }
            P(attrs = { classes("text-secondary", "m-0") }) {
                Text("Optimize your resume for ATS systems and specific job descriptions")
            }
        }
        
        // Main Content Grid
        Div(attrs = { classes("grid", "grid-cols-2", "gap-lg") }) {
            // Left Panel - Input Section
            Div(attrs = { classes("card") }) {
                H3(attrs = { classes("card-title", "mb-md") }) {
                    Text("Optimization Settings")
                }
                
                // Resume Selection
                Div(attrs = { classes("form-group", "mb-lg") }) {
                    Label(attrs = { classes("form-label") }) {
                        Text("Select Resume")
                    }
                    
                    Select(attrs = {
                        classes("form-input")
                        onChange { event ->
                            val value = event.target.value
                            selectedResumeId = if (value.isEmpty()) null else value
                            showAnalysisResults = false
                        }
                    }) {
                        Option(value = "") {
                            Text("-- Select a resume --")
                        }
                        state.resumes.forEach { resume: Resume ->
                            Option(
                                value = resume.id,
                                attrs = {
                                    if (resume.id == selectedResumeId) {
                                        selected()
                                    }
                                }
                            ) {
                                Text(resume.name)
                            }
                        }
                    }
                }
                
                // Target Job Description
                Div(attrs = { classes("form-group", "mb-lg") }) {
                    Label(attrs = { classes("form-label") }) {
                        Text("Target Job Description (Optional)")
                    }
                    TextArea(attrs = {
                        classes("form-input")
                        attr("placeholder", "Paste the job description here to get targeted optimization suggestions...")
                        attr("rows", "6")
                        onInput { event ->
                            targetJobDescription = event.target.value
                        }
                    })
                    P(attrs = { classes("form-helper") }) {
                        Text("Providing a job description helps tailor the analysis to match specific requirements")
                    }
                }
                
                // Target Keywords
                Div(attrs = { classes("form-group", "mb-lg") }) {
                    Label(attrs = { classes("form-label") }) {
                        Text("Target Keywords (comma-separated)")
                    }
                    Input(type = InputType.Text) {
                        classes("form-input")
                        attr("placeholder", "e.g., Python, Machine Learning, Data Analysis, AWS")
                        value(targetKeywords)
                        onInput { event ->
                            targetKeywords = event.target.value
                        }
                    }
                    P(attrs = { classes("form-helper") }) {
                        Text("Enter keywords from the job posting to check keyword density")
                    }
                }
                
                // Analyze Button
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-block")
                    onClick {
                        val resume = selectedResume
                        if (resume != null) {
                            val jobDesc = if (targetJobDescription.isNotBlank()) targetJobDescription else null
                            viewModel.onIntent(ResumeIntent.PerformATSAnalysis(resume, jobDesc))
                            showAnalysisResults = true
                        }
                    }
                    if (selectedResumeId == null || state.isATSAnalyzing) {
                        attr("disabled", "true")
                    }
                }) {
                    if (state.isATSAnalyzing) {
                        Text("⏳ Analyzing...")
                    } else {
                        Text("🔍 Analyze & Optimize")
                    }
                }
            }
            
            // Right Panel - Results Section
            Div(attrs = { classes("card") }) {
                if (!showAnalysisResults) {
                    // Empty State
                    Div(attrs = { classes("empty-state") }) {
                        Div(attrs = { classes("empty-state-icon") }) {
                            Text("📊")
                        }
                        H3(attrs = { classes("mb-sm") }) {
                            Text("No Analysis Yet")
                        }
                        P(attrs = { classes("text-secondary") }) {
                            Text("Select a resume and click 'Analyze & Optimize' to get personalized optimization suggestions")
                        }
                    }
                } else if (state.isATSAnalyzing) {
                    // Loading State
                    Div(attrs = { classes("loading-state") }) {
                        Div(attrs = { classes("spinner", "mb-md") })
                        P(attrs = { classes("text-secondary") }) {
                            Text("Analyzing your resume...")
                        }
                    }
                } else {
                    // Results
                    val currentAnalysis = state.atsAnalysis
                    if (currentAnalysis != null) {
                        val resumeContent = selectedResume?.content ?: ""
                        OptimizationResults(currentAnalysis, targetKeywords, resumeContent)
                    } else {
                        Div(attrs = { classes("alert", "alert-warning") }) {
                            Text("⚠️ Analysis failed. Please check your API key settings and try again.")
                        }
                    }
                }
            }
        }
        
        // Quick Tips Section
        Div(attrs = { classes("card", "mt-xl") }) {
            H3(attrs = { classes("card-title", "mb-md") }) {
                Text("💡 Quick Optimization Tips")
            }
            
            Div(attrs = { classes("grid", "grid-cols-3", "gap-md") }) {
                QuickTipCard(
                    icon = "📝",
                    title = "Use Keywords",
                    description = "Include keywords from the job description naturally throughout your resume"
                )
                QuickTipCard(
                    icon = "📊",
                    title = "Quantify Results",
                    description = "Use the X-Y-Z format: Accomplished [X] as measured by [Y] by doing [Z]"
                )
                QuickTipCard(
                    icon = "🎯",
                    title = "Tailor Content",
                    description = "Customize your resume for each job application to maximize relevance"
                )
            }
        }
    }
}

@Composable
private fun OptimizationResults(
    analysis: ATSAnalysis,
    targetKeywords: String,
    resumeContent: String
) {
    Div(attrs = { classes("results-container") }) {
        // Overall Score
        Div(attrs = { classes("score-card", "mb-lg") }) {
            Div(attrs = { classes("text-secondary", "mb-xs") }) {
                Text("ATS Compatibility Score")
            }
            Div(attrs = { 
                classes("score-value", getScoreClass(analysis.overallScore))
            }) {
                Text("${analysis.overallScore}")
            }
            Div(attrs = { classes("progress-bar", "mt-sm") }) {
                Div(attrs = { 
                    classes("progress-fill", getScoreClass(analysis.overallScore))
                    style {
                        property("width", "${analysis.overallScore}%")
                    }
                })
            }
            Div(attrs = { classes("text-secondary", "text-sm", "mt-sm") }) {
                Text(getScoreLabel(analysis.overallScore))
            }
        }
        
        // Score Breakdown
        Div(attrs = { classes("mb-lg") }) {
            H4(attrs = { classes("mb-md") }) {
                Text("📈 Score Breakdown")
            }
            
            ScoreBar("Formatting", analysis.formattingScore)
            ScoreBar("Keywords", analysis.keywordScore)
            ScoreBar("Structure", analysis.structureScore)
            ScoreBar("Readability", analysis.readabilityScore)
        }
        
        // Keyword Analysis
        if (targetKeywords.isNotBlank()) {
            Div(attrs = { classes("mb-lg") }) {
                H4(attrs = { classes("mb-md") }) {
                    Text("🔑 Keyword Match Analysis")
                }
                
                val keywords = targetKeywords.split(",").map { kw -> kw.trim().lowercase() }.filter { kw -> kw.isNotEmpty() }
                val resumeLower = resumeContent.lowercase()
                
                Div(attrs = { classes("flex", "flex-wrap", "gap-sm") }) {
                    keywords.forEach { keyword: String ->
                        val found = resumeLower.contains(keyword)
                        Span(attrs = {
                            classes("badge", if (found) "badge-success" else "badge-error")
                        }) {
                            Text("${if (found) "✓" else "✗"} $keyword")
                        }
                    }
                }
                
                val matchCount = keywords.count { kw -> resumeLower.contains(kw) }
                val matchPercent = if (keywords.isNotEmpty()) (matchCount * 100 / keywords.size) else 0
                
                Div(attrs = { classes("text-secondary", "text-sm", "mt-sm") }) {
                    Text("$matchCount of ${keywords.size} keywords found ($matchPercent% match)")
                }
            }
        }
        
        // Issues Found
        val allIssues = analysis.formattingIssues + analysis.structureIssues
        if (allIssues.isNotEmpty()) {
            Div(attrs = { classes("mb-lg") }) {
                H4(attrs = { classes("mb-md") }) {
                    Text("⚠️ Issues Found (${allIssues.size})")
                }
                
                allIssues.take(5).forEach { issue: ATSIssue ->
                    IssueCard(issue)
                }
                
                if (allIssues.size > 5) {
                    P(attrs = { classes("text-secondary", "text-sm") }) {
                        Text("+ ${allIssues.size - 5} more issues")
                    }
                }
            }
        }
        
        // Recommendations
        if (analysis.recommendations.isNotEmpty()) {
            Div {
                H4(attrs = { classes("mb-md") }) {
                    Text("✨ Recommendations")
                }
                
                analysis.recommendations.take(5).forEach { rec: ATSRecommendation ->
                    RecommendationCard(rec)
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    Div(attrs = { classes("score-bar-container", "mb-sm") }) {
        Div(attrs = { classes("flex", "justify-between", "mb-xs") }) {
            Span(attrs = { classes("text-secondary", "text-sm") }) {
                Text(label)
            }
            Span(attrs = { classes("text-sm", "font-medium", getScoreClass(score)) }) {
                Text("$score%")
            }
        }
        Div(attrs = { classes("progress-bar", "progress-bar-sm") }) {
            Div(attrs = { 
                classes("progress-fill", getScoreClass(score))
                style {
                    property("width", "${score}%")
                }
            })
        }
    }
}

@Composable
private fun IssueCard(issue: ATSIssue) {
    Div(attrs = { 
        classes("issue-card", getSeverityClass(issue.severity), "mb-sm")
    }) {
        Div(attrs = { classes("font-medium", "mb-xs") }) {
            Text(issue.description)
        }
        Div(attrs = { classes("text-secondary", "text-sm") }) {
            Text("Category: ${issue.category} • Severity: ${issue.severity.name}")
        }
        if (issue.suggestion.isNotBlank()) {
            Div(attrs = { classes("text-success", "text-sm", "mt-xs") }) {
                Text("💡 ${issue.suggestion}")
            }
        }
    }
}

@Composable
private fun RecommendationCard(rec: ATSRecommendation) {
    Div(attrs = { classes("recommendation-card", "mb-sm") }) {
        Div(attrs = { classes("flex", "items-center", "gap-sm", "mb-xs") }) {
            Span(attrs = { classes("badge", getPriorityBadgeClass(rec.priority)) }) {
                Text(getPriorityLabel(rec.priority))
            }
            Span(attrs = { classes("font-medium") }) {
                Text(rec.title)
            }
        }
        P(attrs = { classes("text-secondary", "text-sm", "m-0") }) {
            Text(rec.description)
        }
    }
}

@Composable
private fun QuickTipCard(icon: String, title: String, description: String) {
    Div(attrs = { classes("tip-card") }) {
        Div(attrs = { classes("tip-icon") }) {
            Text(icon)
        }
        H4(attrs = { classes("mb-xs") }) {
            Text(title)
        }
        P(attrs = { classes("text-secondary", "text-sm", "m-0") }) {
            Text(description)
        }
    }
}

private fun getScoreClass(score: Int): String = when {
    score >= 80 -> "score-good"
    score >= 60 -> "score-fair"
    else -> "score-poor"
}

private fun getScoreLabel(score: Int): String = when {
    score >= 90 -> "Excellent! Your resume is highly ATS-optimized"
    score >= 80 -> "Great! Your resume is well-optimized"
    score >= 70 -> "Good, but there's room for improvement"
    score >= 60 -> "Fair - consider addressing the issues below"
    else -> "Needs work - follow the recommendations to improve"
}

private fun getSeverityClass(severity: IssueSeverity): String = when (severity) {
    IssueSeverity.HIGH -> "issue-high"
    IssueSeverity.MEDIUM -> "issue-medium"
    IssueSeverity.LOW -> "issue-low"
}

private fun getPriorityBadgeClass(priority: Int): String = when {
    priority >= 8 -> "badge-error"
    priority >= 5 -> "badge-warning"
    else -> "badge-success"
}

private fun getPriorityLabel(priority: Int): String = when {
    priority >= 8 -> "HIGH"
    priority >= 5 -> "MEDIUM"
    else -> "LOW"
}
