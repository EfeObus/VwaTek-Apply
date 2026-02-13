package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.PremiumFeature
import com.vwatek.apply.domain.model.SubscriptionTier
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Salary Insights Screen for Web - Premium Feature
 * Phase 4: Premium & Monetization
 */
@Composable
fun SalaryInsightsScreen() {
    var jobTitle by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var yearsExperience by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(false) } // Would be checked from subscription manager
    var showPaywall by remember { mutableStateOf(false) }
    
    var salaryInsights by remember { mutableStateOf<SalaryInsightsData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    // Check feature access
    if (!hasAccess) {
        FeatureGatedContent(
            feature = PremiumFeature.SALARY_INSIGHTS,
            hasAccess = hasAccess,
            requiredTier = SubscriptionTier.PRO,
            onUpgradeClick = { showPaywall = true }
        ) { }
        
        if (showPaywall) {
            PaywallModal(
                feature = PremiumFeature.SALARY_INSIGHTS,
                requiredTier = SubscriptionTier.PRO,
                onClose = { showPaywall = false },
                onUpgrade = { tier, period ->
                    // Handle upgrade
                    showPaywall = false
                }
            )
        }
        return
    }
    
    Div(attrs = { classes("salary-insights-screen") }) {
        // Header
        H1(attrs = { classes("mb-lg") }) { Text("Salary Insights") }
        
        // Search Form Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Search Salary Data") }
            
            Div(attrs = { classes("form-grid") }) {
                // Job Title
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Job Title") }
                    Div(attrs = { classes("input-with-icon") }) {
                        Span(attrs = { classes("input-icon") }) { Text("💼") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("e.g., Software Engineer")
                            value(jobTitle)
                            onInput { jobTitle = it.value }
                        }
                    }
                }
                
                // Location
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Location") }
                    Div(attrs = { classes("input-with-icon") }) {
                        Span(attrs = { classes("input-icon") }) { Text("📍") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("e.g., Toronto, ON")
                            value(location)
                            onInput { location = it.value }
                        }
                    }
                }
                
                // Years of Experience
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Years of Experience") }
                    Div(attrs = { classes("input-with-icon") }) {
                        Span(attrs = { classes("input-icon") }) { Text("⏱️") }
                        Input(InputType.Number) {
                            classes("form-input")
                            placeholder("e.g., 5")
                            value(yearsExperience)
                            onInput { yearsExperience = it.value }
                        }
                    }
                }
                
                // Skills
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Key Skills") }
                    Div(attrs = { classes("input-with-icon") }) {
                        Span(attrs = { classes("input-icon") }) { Text("🔧") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("e.g., Kotlin, Android, AWS (comma separated)")
                            value(skills)
                            onInput { skills = it.value }
                        }
                    }
                }
            }
            
            Button(attrs = {
                classes("btn", "btn-primary", "btn-full", "mt-md")
                if (jobTitle.isBlank() || location.isBlank()) {
                    classes("btn-disabled")
                }
                onClick {
                    if (jobTitle.isNotBlank() && location.isNotBlank()) {
                        scope.launch {
                            searchSalary(jobTitle, location, yearsExperience.toIntOrNull() ?: 0, skills) { result ->
                                salaryInsights = result
                            }
                        }
                    }
                }
            }) {
                Span { Text("🔍") }
                Text(" Get Salary Insights")
            }
        }
        
        // Results section
        if (isLoading) {
            Div(attrs = { classes("loading-container") }) {
                Div(attrs = { classes("spinner") })
                Text("Loading salary data...")
            }
        } else if (errorMessage != null) {
            Div(attrs = { classes("alert", "alert-error") }) {
                Text(errorMessage!!)
            }
        } else if (salaryInsights != null) {
            SalaryResults(salaryInsights!!)
        } else {
            // Empty state
            Div(attrs = { classes("empty-state", "text-center") }) {
                Div(attrs = { classes("empty-icon") }) { Text("💰") }
                H3 { Text("Search for Salary Data") }
                P(attrs = { classes("text-secondary") }) {
                    Text("Enter a job title and location to see salary insights for your target role")
                }
            }
        }
    }
    
    // Paywall modal
    if (showPaywall) {
        PaywallModal(
            feature = PremiumFeature.SALARY_INSIGHTS,
            requiredTier = SubscriptionTier.PRO,
            onClose = { showPaywall = false },
            onUpgrade = { tier, period ->
                showPaywall = false
            }
        )
    }
}

@Composable
private fun SalaryResults(insights: SalaryInsightsData) {
    // Salary Range Card
    Div(attrs = { classes("card", "card-highlight", "mb-lg") }) {
        Div(attrs = { classes("salary-header", "mb-md") }) {
            H3 { Text(insights.jobTitle) }
            P(attrs = { classes("text-secondary") }) { Text(insights.location) }
        }
        
        // Salary range display
        Div(attrs = { classes("salary-range", "mb-lg") }) {
            Div(attrs = { classes("salary-point") }) {
                Span(attrs = { classes("label") }) { Text("Min") }
                Span(attrs = { classes("value") }) { Text(formatSalary(insights.minSalary)) }
            }
            Div(attrs = { classes("salary-point", "salary-point-highlight") }) {
                Span(attrs = { classes("label") }) { Text("Median") }
                Span(attrs = { classes("value", "value-large") }) { Text(formatSalary(insights.medianSalary)) }
            }
            Div(attrs = { classes("salary-point") }) {
                Span(attrs = { classes("label") }) { Text("Max") }
                Span(attrs = { classes("value") }) { Text(formatSalary(insights.maxSalary)) }
            }
        }
        
        // Salary bar visualization
        Div(attrs = { classes("salary-bar-container") }) {
            Div(attrs = { 
                classes("salary-bar") 
                style {
                    property("width", "${((insights.medianSalary - insights.minSalary).toDouble() / (insights.maxSalary - insights.minSalary).toDouble() * 100).toInt()}%")
                }
            })
        }
    }
    
    // Statistics Card
    Div(attrs = { classes("card", "mb-lg") }) {
        H4(attrs = { classes("card-title", "mb-md") }) { Text("Market Data") }
        
        Div(attrs = { classes("stats-grid") }) {
            StatItem("Data Points", "${insights.dataPoints} salaries")
            StatItem("Experience", "${insights.yearsExperience} years")
            StatItem("Confidence", "${insights.confidence}%")
            if (insights.percentile != null) {
                StatItem("Your Position", "${insights.percentile}th percentile")
            }
        }
    }
    
    // Insights Card
    if (insights.insights.isNotEmpty()) {
        Div(attrs = { classes("card", "mb-lg") }) {
            H4(attrs = { classes("card-title", "mb-md") }) { Text("Key Insights") }
            
            Ul(attrs = { classes("insights-list") }) {
                insights.insights.forEach { insight ->
                    Li {
                        Span(attrs = { classes("insight-icon") }) { Text("💡") }
                        Text(insight)
                    }
                }
            }
        }
    }
    
    // Recommendations Card
    if (insights.recommendations.isNotEmpty()) {
        Div(attrs = { classes("card", "mb-lg") }) {
            H4(attrs = { classes("card-title", "mb-md") }) { Text("Recommendations") }
            
            Ul(attrs = { classes("recommendations-list") }) {
                insights.recommendations.forEach { recommendation ->
                    Li {
                        Span(attrs = { classes("recommendation-icon") }) { Text("⭐") }
                        Text(recommendation)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Div(attrs = { classes("stat-item") }) {
        Span(attrs = { classes("stat-label") }) { Text(label) }
        Span(attrs = { classes("stat-value") }) { Text(value) }
    }
}

// Data class for salary insights
private data class SalaryInsightsData(
    val jobTitle: String,
    val location: String,
    val minSalary: Long,
    val medianSalary: Long,
    val maxSalary: Long,
    val dataPoints: Int,
    val yearsExperience: Int,
    val confidence: Int,
    val percentile: Int? = null,
    val insights: List<String>,
    val recommendations: List<String>
)

// Simulated search function
private suspend fun searchSalary(
    jobTitle: String,
    location: String,
    yearsExperience: Int,
    skills: String,
    onResult: (SalaryInsightsData) -> Unit
) {
    // Simulated API call - would be replaced with actual API call
    kotlinx.coroutines.delay(1500)
    
    onResult(
        SalaryInsightsData(
            jobTitle = jobTitle,
            location = location,
            minSalary = 70000,
            medianSalary = 95000,
            maxSalary = 150000,
            dataPoints = 1234,
            yearsExperience = yearsExperience,
            confidence = 87,
            percentile = 65,
            insights = listOf(
                "Salaries in $location are 12% above the national average",
                "Skills like ${if (skills.isEmpty()) "cloud computing" else skills} can increase salary by 15%",
                "Remote positions typically offer 8% higher compensation"
            ),
            recommendations = listOf(
                "Consider highlighting your experience with distributed systems",
                "Certifications could increase your market value by 10-15%",
                "Negotiating for equity can significantly increase total compensation"
            )
        )
    )
}

private fun formatSalary(amount: Long): String {
    return "$${"%,d".format(amount)}"
}
