package com.vwatek.apply.ui.screens

import androidx.compose.runtime.*
import com.vwatek.apply.domain.usecase.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.launch

/**
 * Phase 12: LinkedIn Optimizer Screen for Web
 */
@Composable
fun LinkedInOptimizerScreen() {
    val manager = remember { GlobalContext.get().get<LinkedInOptimizerManager>() }
    val profileState by manager.profileState.collectAsState()
    val optimizationState by manager.optimizationState.collectAsState()
    val historyState by manager.historyState.collectAsState()
    val scope = rememberCoroutineScope()

    var profileUrl by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("") }
    var targetIndustry by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("analyze") }

    LaunchedEffect(Unit) {
        manager.loadHistory()
    }

    Div {
        // Header
        Div(attrs = { classes("mb-xl") }) {
            H1(attrs = { classes("mb-xs") }) {
                Text("LinkedIn Optimizer")
            }
            P(attrs = { classes("text-secondary", "m-0") }) {
                Text("Analyze and optimize your LinkedIn profile with AI-powered recommendations")
            }
        }

        // Tab Navigation
        Div(attrs = { classes("tabs-nav", "mb-lg") }) {
            Button(attrs = {
                classes(buildList {
                    add("tab-btn")
                    if (activeTab == "analyze") add("active")
                })
                onClick { activeTab = "analyze" }
            }) { Text("Analyze") }
            Button(attrs = {
                classes(buildList {
                    add("tab-btn")
                    if (activeTab == "optimize") add("active")
                })
                onClick { activeTab = "optimize" }
            }) { Text("Optimize") }
            Button(attrs = {
                classes(buildList {
                    add("tab-btn")
                    if (activeTab == "history") add("active")
                })
                onClick { activeTab = "history" }
            }) { Text("History") }
        }

        when (activeTab) {
            "analyze" -> AnalyzeTabContent(
                profileUrl = profileUrl,
                onProfileUrlChange = { profileUrl = it },
                profileState = profileState,
                onAnalyze = {
                    scope.launch {
                        manager.analyzeProfile(profileUrl = profileUrl.ifBlank { null })
                    }
                },
                onClear = { manager.clearProfile() }
            )
            "optimize" -> OptimizeTabContent(
                profileState = profileState,
                optimizationState = optimizationState,
                targetRole = targetRole,
                onTargetRoleChange = { targetRole = it },
                targetIndustry = targetIndustry,
                onTargetIndustryChange = { targetIndustry = it },
                onOptimize = { profileId ->
                    scope.launch {
                        manager.getOptimizedContent(
                            profileId = profileId,
                            targetRole = targetRole.ifBlank { null },
                            targetIndustry = targetIndustry.ifBlank { null }
                        )
                    }
                },
                onClear = { manager.clearOptimization() }
            )
            "history" -> HistoryTabContent(
                historyState = historyState,
                onRefresh = { scope.launch { manager.loadHistory() } }
            )
        }
    }
}

@Composable
private fun AnalyzeTabContent(
    profileUrl: String,
    onProfileUrlChange: (String) -> Unit,
    profileState: LinkedInProfileState,
    onAnalyze: () -> Unit,
    onClear: () -> Unit
) {
    // Input Card
    Div(attrs = { classes("card", "mb-lg") }) {
        Div(attrs = { classes("card-body") }) {
            H3(attrs = { classes("mb-sm") }) { Text("Analyze Your Profile") }
            P(attrs = { classes("text-secondary", "mb-md") }) {
                Text("Enter your LinkedIn profile URL to get AI-powered analysis and recommendations.")
            }
            Div(attrs = { classes("form-group", "mb-md") }) {
                Label(attrs = { classes("form-label") }) { Text("LinkedIn Profile URL") }
                TextInput(profileUrl) {
                    classes("form-control")
                    placeholder("https://linkedin.com/in/yourprofile")
                    onInput { onProfileUrlChange(it.value) }
                }
            }
            Button(attrs = {
                classes("btn", "btn-primary")
                if (profileState is LinkedInProfileState.Analyzing) attr("disabled", "")
                onClick { onAnalyze() }
                style {
                    property("background-color", "#0A66C2")
                    property("width", "100%")
                }
            }) {
                Text(if (profileState is LinkedInProfileState.Analyzing) "Analyzing..." else "Analyze Profile")
            }
        }
    }

    // Results
    when (profileState) {
        is LinkedInProfileState.Analyzed -> {
            val result = (profileState as LinkedInProfileState.Analyzed).result
            val analysis = result.analysis

            // Overall Score
            Div(attrs = { classes("card", "mb-lg") }) {
                Div(attrs = { classes("card-body") }) {
                    Div(attrs = {
                        style {
                            property("display", "flex")
                            property("justify-content", "space-between")
                            property("align-items", "center")
                        }
                    }) {
                        H3 { Text("Overall Score") }
                        Span(attrs = {
                            style {
                                property("background-color", scoreColorHex(analysis.overallScore))
                                property("color", "white")
                                property("padding", "6px 16px")
                                property("border-radius", "20px")
                                property("font-weight", "bold")
                                property("font-size", "1.2rem")
                            }
                        }) {
                            Text("${analysis.overallScore}/100")
                        }
                    }
                }
            }

            // Section Scores
            Div(attrs = { classes("card", "mb-lg") }) {
                Div(attrs = { classes("card-body") }) {
                    H3(attrs = { classes("mb-md") }) { Text("Section Scores") }
                    ScoreBar("Headline", analysis.sectionScores.headline)
                    ScoreBar("Summary", analysis.sectionScores.summary)
                    ScoreBar("Experience", analysis.sectionScores.experience)
                    ScoreBar("Education", analysis.sectionScores.education)
                    ScoreBar("Skills", analysis.sectionScores.skills)
                    ScoreBar("Completeness", analysis.sectionScores.completeness)
                }
            }

            // Strengths
            if (analysis.strengths.isNotEmpty()) {
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("card-body") }) {
                        H3(attrs = {
                            style { property("color", "#4CAF50") }
                            classes("mb-sm")
                        }) { Text("Strengths") }
                        Ul {
                            analysis.strengths.forEach { strength ->
                                Li(attrs = { classes("mb-xs") }) { Text(" $strength") }
                            }
                        }
                    }
                }
            }

            // Improvements
            if (analysis.improvements.isNotEmpty()) {
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("card-body") }) {
                        H3(attrs = {
                            style { property("color", "#F44336") }
                            classes("mb-sm")
                        }) { Text("Improvements") }
                        analysis.improvements.forEach { improvement ->
                            Div(attrs = { classes("mb-sm") }) {
                                P(attrs = { style { property("font-weight", "600") } }) {
                                    Text(" ${improvement.issue}")
                                }
                                P(attrs = { classes("text-secondary") }) {
                                    Text(improvement.suggestion)
                                }
                            }
                        }
                    }
                }
            }

            // Clear button
            Div(attrs = { style { property("text-align", "center") } }) {
                Button(attrs = {
                    classes("btn", "btn-outline")
                    onClick { onClear() }
                }) { Text("Clear Analysis") }
            }
        }
        is LinkedInProfileState.Error -> {
            Div(attrs = { classes("card", "mb-lg") }) {
                Div(attrs = {
                    classes("card-body")
                    style { property("background-color", "#ffebee") }
                }) {
                    P(attrs = { style { property("color", "#c62828") } }) {
                        Text((profileState as LinkedInProfileState.Error).message)
                    }
                }
            }
        }
        else -> {
            Div(attrs = {
                style {
                    property("text-align", "center")
                    property("padding", "48px")
                }
            }) {
                P(attrs = { classes("text-secondary") }) {
                    Text("Paste your LinkedIn profile URL above to analyze your profile.")
                }
            }
        }
    }
}

@Composable
private fun OptimizeTabContent(
    profileState: LinkedInProfileState,
    optimizationState: OptimizationState,
    targetRole: String,
    onTargetRoleChange: (String) -> Unit,
    targetIndustry: String,
    onTargetIndustryChange: (String) -> Unit,
    onOptimize: (String) -> Unit,
    onClear: () -> Unit
) {
    if (profileState !is LinkedInProfileState.Analyzed) {
        Div(attrs = { classes("card", "mb-lg") }) {
            Div(attrs = {
                classes("card-body")
                style { property("text-align", "center") }
            }) {
                H3(attrs = { classes("mb-sm") }) { Text("Analyze your profile first") }
                P(attrs = { classes("text-secondary") }) {
                    Text("Go to the Analyze tab to analyze your LinkedIn profile before optimizing.")
                }
            }
        }
        return
    }

    val result = (profileState as LinkedInProfileState.Analyzed).result

    // Optimization form
    Div(attrs = { classes("card", "mb-lg") }) {
        Div(attrs = { classes("card-body") }) {
            H3(attrs = { classes("mb-md") }) { Text("Optimize Content") }
            Div(attrs = { classes("form-group", "mb-md") }) {
                Label(attrs = { classes("form-label") }) { Text("Target Role (optional)") }
                TextInput(targetRole) {
                    classes("form-control")
                    placeholder("e.g., Software Engineer")
                    onInput { onTargetRoleChange(it.value) }
                }
            }
            Div(attrs = { classes("form-group", "mb-md") }) {
                Label(attrs = { classes("form-label") }) { Text("Target Industry (optional)") }
                TextInput(targetIndustry) {
                    classes("form-control")
                    placeholder("e.g., Technology")
                    onInput { onTargetIndustryChange(it.value) }
                }
            }
            Button(attrs = {
                classes("btn", "btn-primary")
                if (optimizationState is OptimizationState.Generating) attr("disabled", "")
                onClick { onOptimize(result.profileId) }
                style {
                    property("background-color", "#0A66C2")
                    property("width", "100%")
                }
            }) {
                Text(if (optimizationState is OptimizationState.Generating) "Generating..." else "Generate Optimizations")
            }
        }
    }

    // Results
    when (optimizationState) {
        is OptimizationState.Success -> {
            val content = (optimizationState as OptimizationState.Success).content
            content.headline?.let { headline ->
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("card-body") }) {
                        H3(attrs = {
                            style { property("color", "#0A66C2") }
                            classes("mb-sm")
                        }) { Text("Optimized Headline") }
                        P { Text(headline) }
                    }
                }
            }
            if (content.headlineAlternatives.isNotEmpty()) {
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("card-body") }) {
                        H3(attrs = { classes("mb-sm") }) { Text("Alternative Headlines") }
                        Ol {
                            content.headlineAlternatives.forEach { alt ->
                                Li(attrs = { classes("mb-xs") }) { Text(alt) }
                            }
                        }
                    }
                }
            }
            content.summary?.let { summary ->
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("card-body") }) {
                        H3(attrs = {
                            style { property("color", "#0A66C2") }
                            classes("mb-sm")
                        }) { Text("Optimized Summary") }
                        P { Text(summary) }
                    }
                }
            }
            if (content.skillSuggestions.isNotEmpty()) {
                Div(attrs = { classes("card", "mb-lg") }) {
                    Div(attrs = { classes("card-body") }) {
                        H3(attrs = { classes("mb-sm") }) { Text("Suggested Skills") }
                        Ul {
                            content.skillSuggestions.forEach { skill ->
                                Li(attrs = { classes("mb-xs") }) { Text("• $skill") }
                            }
                        }
                    }
                }
            }
            Div(attrs = { style { property("text-align", "center") } }) {
                Button(attrs = {
                    classes("btn", "btn-outline")
                    onClick { onClear() }
                }) { Text("Clear Optimizations") }
            }
        }
        is OptimizationState.Error -> {
            Div(attrs = { classes("card", "mb-lg") }) {
                Div(attrs = {
                    classes("card-body")
                    style { property("background-color", "#ffebee") }
                }) {
                    P(attrs = { style { property("color", "#c62828") } }) {
                        Text((optimizationState as OptimizationState.Error).message)
                    }
                }
            }
        }
        else -> {
            Div(attrs = {
                style {
                    property("text-align", "center")
                    property("padding", "48px")
                }
            }) {
                P(attrs = { classes("text-secondary") }) {
                    Text("Analyze your profile first, then optimize it for your target role.")
                }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(historyState: LinkedInHistoryState, onRefresh: () -> Unit) {
    when (historyState) {
        is LinkedInHistoryState.Loading -> {
            Div(attrs = {
                style {
                    property("text-align", "center")
                    property("padding", "48px")
                }
            }) {
                P(attrs = { classes("text-secondary") }) { Text("Loading history...") }
            }
        }
        is LinkedInHistoryState.Success -> {
            val history = (historyState as LinkedInHistoryState.Success).history
            if (history.isEmpty()) {
                Div(attrs = {
                    style {
                        property("text-align", "center")
                        property("padding", "48px")
                    }
                }) {
                    H3(attrs = { classes("text-secondary", "mb-sm") }) { Text("No analysis history yet") }
                    P(attrs = { classes("text-secondary", "mb-md") }) { Text("Analyze your LinkedIn profile to see your history here.") }
                    Button(attrs = {
                        classes("btn", "btn-outline")
                        onClick { onRefresh() }
                    }) { Text("Refresh") }
                }
            } else {
                Div(attrs = { classes("mb-md") }) {
                    Button(attrs = {
                        classes("btn", "btn-outline", "btn-sm")
                        onClick { onRefresh() }
                    }) { Text("Refresh") }
                }
                history.forEach { result ->
                    Div(attrs = { classes("card", "mb-md") }) {
                        Div(attrs = { classes("card-body") }) {
                            Div(attrs = {
                                style {
                                    property("display", "flex")
                                    property("justify-content", "space-between")
                                    property("align-items", "center")
                                }
                            }) {
                                Div {
                                    H4 { Text(result.profile.headline ?: "LinkedIn Profile") }
                                    P(attrs = { classes("text-secondary", "text-sm") }) {
                                        Text("Analyzed: ${result.analyzedAt.take(10)}")
                                    }
                                }
                                Span(attrs = {
                                    style {
                                        property("background-color", scoreColorHex(result.analysis.overallScore))
                                        property("color", "white")
                                        property("padding", "4px 12px")
                                        property("border-radius", "12px")
                                        property("font-weight", "bold")
                                    }
                                }) {
                                    Text("${result.analysis.overallScore}")
                                }
                            }
                        }
                    }
                }
            }
        }
        is LinkedInHistoryState.Error -> {
            Div(attrs = {
                style {
                    property("text-align", "center")
                    property("padding", "48px")
                }
            }) {
                P(attrs = { style { property("color", "#c62828") } }) {
                    Text((historyState as LinkedInHistoryState.Error).message)
                }
                Button(attrs = {
                    classes("btn", "btn-outline")
                    onClick { onRefresh() }
                }) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    Div(attrs = {
        classes("mb-sm")
        style {
            property("display", "flex")
            property("align-items", "center")
            property("gap", "12px")
        }
    }) {
        Span(attrs = { style { property("min-width", "100px") } }) { Text(label) }
        Div(attrs = {
            style {
                property("flex", "1")
                property("height", "8px")
                property("background-color", "#e0e0e0")
                property("border-radius", "4px")
                property("overflow", "hidden")
            }
        }) {
            Div(attrs = {
                style {
                    property("width", "${score}%")
                    property("height", "100%")
                    property("background-color", scoreColorHex(score))
                    property("border-radius", "4px")
                    property("transition", "width 0.3s ease")
                }
            })
        }
        Span(attrs = {
            style {
                property("min-width", "30px")
                property("text-align", "right")
                property("font-weight", "bold")
            }
        }) { Text("$score") }
    }
}

private fun scoreColorHex(score: Int): String = when {
    score >= 80 -> "#4CAF50"
    score >= 60 -> "#FFC107"
    score >= 40 -> "#FF9800"
    else -> "#F44336"
}
