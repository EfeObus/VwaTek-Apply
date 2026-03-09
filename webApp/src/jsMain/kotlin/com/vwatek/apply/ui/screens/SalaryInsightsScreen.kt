package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.domain.usecase.SalaryIntelligenceManager
import com.vwatek.apply.domain.usecase.SalaryInsightsState
import com.vwatek.apply.domain.usecase.SubscriptionManager
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Salary Insights Screen for Web - Premium Feature
 * Uses SalaryIntelligenceManager from shared module for real API data
 */
@Composable
fun SalaryInsightsScreen() {
    val salaryManager = remember { GlobalContext.get().get<SalaryIntelligenceManager>() }
    val subscriptionManager = remember { GlobalContext.get().get<SubscriptionManager>() }
    
    var jobTitle by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var yearsExperience by remember { mutableStateOf("") }
    
    var showPaywall by remember { mutableStateOf(false) }
    var showOfferTab by remember { mutableStateOf(false) }
    
    // Offer form fields
    var offerJobTitle by remember { mutableStateOf("") }
    var offerCompany by remember { mutableStateOf("") }
    var offerBaseSalary by remember { mutableStateOf("") }
    var offerProvince by remember { mutableStateOf("") }
    
    val hasAccess = remember { subscriptionManager.canUseFeature(PremiumFeature.SALARY_INSIGHTS) }
    val insightsState by salaryManager.insightsState.collectAsState()
    
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
                    showPaywall = false
                }
            )
        }
        return
    }
    
    Div(attrs = { classes("salary-insights-screen") }) {
        // Header
        H1(attrs = { classes("mb-lg") }) { Text("Salary Insights") }
        
        // Tab selector
        Div(attrs = { classes("tab-bar", "mb-lg") }) {
            Button(attrs = {
                classes("tab-btn", if (!showOfferTab) "active" else "")
                onClick { showOfferTab = false }
            }) { Text("Salary Search") }
            Button(attrs = {
                classes("tab-btn", if (showOfferTab) "active" else "")
                onClick { showOfferTab = true }
            }) { Text("Offer Evaluation") }
        }
        
        if (showOfferTab) {
            // Offer Evaluation Form
            Div(attrs = { classes("card", "mb-lg") }) {
                H3(attrs = { classes("card-title", "mb-md") }) { Text("Evaluate a Job Offer") }
                
                Div(attrs = { classes("form-grid") }) {
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Job Title") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("💼") }
                            Input(InputType.Text) {
                                classes("form-input")
                                placeholder("e.g., Software Engineer")
                                value(offerJobTitle)
                                onInput { offerJobTitle = it.value }
                            }
                        }
                    }
                    
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Company") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("🏢") }
                            Input(InputType.Text) {
                                classes("form-input")
                                placeholder("e.g., Shopify")
                                value(offerCompany)
                                onInput { offerCompany = it.value }
                            }
                        }
                    }
                    
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Base Salary (CAD)") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("💰") }
                            Input(InputType.Number) {
                                classes("form-input")
                                placeholder("e.g., 120000")
                                attr("value", offerBaseSalary)
                                onInput { offerBaseSalary = it.value?.toString() ?: "" }
                            }
                        }
                    }
                    
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Province") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("🗺️") }
                            Input(InputType.Text) {
                                classes("form-input")
                                placeholder("e.g., Ontario")
                                value(offerProvince)
                                onInput { offerProvince = it.value }
                            }
                        }
                    }
                }
                
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-full", "mt-md")
                    if (offerJobTitle.isBlank() || offerCompany.isBlank() || offerBaseSalary.isBlank()) {
                        classes("btn-disabled")
                    }
                    onClick {
                        val salary = offerBaseSalary.toDoubleOrNull() ?: return@onClick
                        val offer = JobOffer(
                            jobTitle = offerJobTitle,
                            company = offerCompany,
                            nocCode = null,
                            baseSalary = salary,
                            signingBonus = null,
                            annualBonus = null,
                            stockOptions = null,
                            benefits = null,
                            province = offerProvince,
                            city = null,
                            isRemote = false,
                            yearsExperienceRequired = null
                        )
                        scope.launch { salaryManager.evaluateOffer(offer) }
                    }
                }) {
                    Span { Text("📊") }
                    Text(" Evaluate Offer")
                }
            }
        } else {
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
                    
                    // Province
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Province") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("🗺️") }
                            Input(InputType.Text) {
                                classes("form-input")
                                placeholder("e.g., Ontario")
                                value(province)
                                onInput { province = it.value }
                            }
                        }
                    }
                    
                    // City
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("City (optional)") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("🏙️") }
                            Input(InputType.Text) {
                                classes("form-input")
                                placeholder("e.g., Toronto")
                                value(city)
                                onInput { city = it.value }
                            }
                        }
                    }
                    
                    // Years of Experience
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Years of Experience (optional)") }
                        Div(attrs = { classes("input-with-icon") }) {
                            Span(attrs = { classes("input-icon") }) { Text("⏱️") }
                            Input(InputType.Number) {
                                classes("form-input")
                                placeholder("e.g., 5")
                                attr("value", yearsExperience)
                                onInput { yearsExperience = it.value?.toString() ?: "" }
                            }
                        }
                    }
                }
                
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-full", "mt-md")
                    if (jobTitle.isBlank() || province.isBlank()) {
                        classes("btn-disabled")
                    }
                    onClick {
                        if (jobTitle.isNotBlank() && province.isNotBlank()) {
                            scope.launch {
                                salaryManager.searchSalary(
                                    jobTitle = jobTitle,
                                    province = province,
                                    city = city.ifBlank { null },
                                    yearsExperience = yearsExperience.toIntOrNull()
                                )
                            }
                        }
                    }
                }) {
                    Span { Text("🔍") }
                    Text(" Get Salary Insights")
                }
            }
        }
        
        // Results section based on state
        when (val state = insightsState) {
            is SalaryInsightsState.Loading -> {
                Div(attrs = { classes("loading-container") }) {
                    Div(attrs = { classes("spinner") })
                    Text("Loading salary data...")
                }
            }
            is SalaryInsightsState.Error -> {
                Div(attrs = { classes("alert", "alert-error") }) {
                    Text(state.message)
                }
            }
            is SalaryInsightsState.Success -> {
                SalaryResults(state.insights.insights, state.insights.chartData)
            }
            is SalaryInsightsState.OfferEvaluated -> {
                OfferEvaluationResults(state.evaluation.evaluation)
            }
            is SalaryInsightsState.Idle -> {
                // Empty state
                Div(attrs = { classes("empty-state", "text-center") }) {
                    Div(attrs = { classes("empty-icon") }) { Text(if (showOfferTab) "📊" else "💰") }
                    H3 { Text(if (showOfferTab) "Evaluate a Job Offer" else "Search for Salary Data") }
                    P(attrs = { classes("text-secondary") }) {
                        Text(
                            if (showOfferTab) "Enter your offer details to get a comprehensive market analysis"
                            else "Enter a job title and province to see salary insights based on Canadian market data"
                        )
                    }
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
private fun SalaryResults(
    insights: SalaryInsights,
    chartData: com.vwatek.apply.data.api.SalaryChartData?
) {
    // Salary Range Card
    Div(attrs = { classes("card", "card-highlight", "mb-lg") }) {
        Div(attrs = { classes("salary-header", "mb-md") }) {
            H3 { Text(insights.jobTitle) }
            P(attrs = { classes("text-secondary") }) { Text(insights.location) }
        }
        
        // Salary range display
        Div(attrs = { classes("salary-range", "mb-lg") }) {
            Div(attrs = { classes("salary-point") }) {
                Span(attrs = { classes("label") }) { Text("Low") }
                Span(attrs = { classes("value") }) { Text(formatSalary(insights.salaryRange.low)) }
            }
            Div(attrs = { classes("salary-point", "salary-point-highlight") }) {
                Span(attrs = { classes("label") }) { Text("Median") }
                Span(attrs = { classes("value", "value-large") }) { Text(formatSalary(insights.medianSalary)) }
            }
            Div(attrs = { classes("salary-point") }) {
                Span(attrs = { classes("label") }) { Text("High") }
                Span(attrs = { classes("value") }) { Text(formatSalary(insights.salaryRange.high)) }
            }
        }
        
        // Salary bar visualization
        val range = insights.salaryRange.high - insights.salaryRange.low
        val medianPct = if (range > 0) ((insights.medianSalary - insights.salaryRange.low) / range * 100).toInt() else 50
        Div(attrs = { classes("salary-bar-container") }) {
            Div(attrs = { 
                classes("salary-bar") 
                style { property("width", "${medianPct}%") }
            })
        }
        
        // Market trend
        Div(attrs = { 
            classes("flex", "align-center", "gap-sm", "mt-md")
        }) {
            val trendEmoji = when (insights.marketTrend) {
                MarketTrend.INCREASING -> "📈"
                MarketTrend.STABLE -> "➡️"
                MarketTrend.DECREASING -> "📉"
                else -> "➡️"
            }
            Span { Text(trendEmoji) }
            Span(attrs = { classes("text-secondary", "text-sm") }) {
                Text("Market trend: ${insights.marketTrend.name.lowercase().replaceFirstChar { it.uppercase() }}")
            }
        }
    }
    
    // Market Comparison Card
    Div(attrs = { classes("card", "mb-lg") }) {
        H4(attrs = { classes("card-title", "mb-md") }) { Text("Market Comparison") }
        
        Div(attrs = { classes("stats-grid") }) {
            insights.percentile?.let { percentile ->
                StatItem("Your Position", "${percentile}th percentile")
            }
            StatItem("vs Provincial Avg", formatPercentage(insights.comparisonToProvincialAverage))
            StatItem("vs National Avg", formatPercentage(insights.comparisonToNationalAverage))
        }
    }
    
    // Related Job Salaries
    if (insights.relatedJobSalaries.isNotEmpty()) {
        Div(attrs = { classes("card", "mb-lg") }) {
            H4(attrs = { classes("card-title", "mb-md") }) { Text("Related Job Salaries") }
            
            insights.relatedJobSalaries.forEach { related ->
                Div(attrs = { classes("flex", "justify-between", "align-center", "mb-sm") }) {
                    Div {
                        Span(attrs = { classes("font-medium") }) { Text(related.jobTitle) }
                        Br()
                        Span(attrs = { classes("text-secondary", "text-sm") }) { Text("NOC: ${related.nocCode}") }
                    }
                    Div(attrs = { style { property("text-align", "right") } }) {
                        Span(attrs = { classes("font-bold") }) { Text(formatSalary(related.medianSalary)) }
                        Br()
                        Span(attrs = {
                            classes("text-sm")
                            if (related.salaryDifference >= 0) classes("text-success") else classes("text-error")
                        }) { Text(formatPercentage(related.salaryDifference)) }
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
private fun OfferEvaluationResults(evaluation: OfferEvaluation) {
    // Overall Rating
    Div(attrs = { classes("card", "card-highlight", "mb-lg", "text-center") }) {
        H4(attrs = { classes("card-title", "mb-md") }) { Text("Offer Rating") }
        
        Div(attrs = {
            classes("rating-badge", "mb-md")
            style {
                property("font-size", "2rem")
                property("font-weight", "bold")
                property("color", when (evaluation.overallRating) {
                    OfferRating.EXCELLENT -> "#4CAF50"
                    OfferRating.GOOD -> "#2196F3"
                    OfferRating.FAIR -> "#FF9800"
                    OfferRating.BELOW_MARKET -> "#F44336"
                    OfferRating.POOR -> "#F44336"
                    else -> "#666"
                })
            }
        }) { Text(evaluation.overallRating.name.replace("_", " ")) }
        
        P(attrs = { classes("text-secondary") }) { Text(evaluation.recommendation) }
    }
    
    // Compensation Analysis
    Div(attrs = { classes("card", "mb-lg") }) {
        H4(attrs = { classes("card-title", "mb-md") }) { Text("Compensation Analysis") }
        
        Div(attrs = { classes("stats-grid") }) {
            StatItem("Base Salary", formatSalary(evaluation.offer.baseSalary))
            StatItem("Market Median", formatSalary(evaluation.marketAnalysis.marketMedian))
            StatItem("vs Market", formatPercentage(evaluation.marketAnalysis.comparisonToMarket))
            StatItem("Total (1st Year)", formatSalary(evaluation.totalCompensation.totalFirstYear))
            StatItem("Total (Annual)", formatSalary(evaluation.totalCompensation.totalAnnualized))
        }
    }
    
    // Strengths
    if (evaluation.strengths.isNotEmpty()) {
        Div(attrs = { classes("card", "mb-lg") }) {
            H4(attrs = { classes("card-title", "mb-md") }) { Text("Strengths") }
            Ul(attrs = { classes("insights-list") }) {
                evaluation.strengths.forEach { strength ->
                    Li {
                        Span(attrs = { classes("insight-icon") }) { Text("✅") }
                        Text(strength)
                    }
                }
            }
        }
    }
    
    // Concerns
    if (evaluation.concerns.isNotEmpty()) {
        Div(attrs = { classes("card", "mb-lg") }) {
            H4(attrs = { classes("card-title", "mb-md") }) { Text("Concerns") }
            Ul(attrs = { classes("insights-list") }) {
                evaluation.concerns.forEach { concern ->
                    Li {
                        Span(attrs = { classes("insight-icon") }) { Text("⚠️") }
                        Text(concern)
                    }
                }
            }
        }
    }
    
    // Negotiation Opportunities
    if (evaluation.negotiationOpportunities.isNotEmpty()) {
        Div(attrs = { classes("card", "mb-lg") }) {
            H4(attrs = { classes("card-title", "mb-md") }) { Text("Negotiation Opportunities") }
            
            evaluation.negotiationOpportunities.forEach { opportunity ->
                Div(attrs = {
                    classes("card", "mb-sm")
                    style { property("background", "#f8f9fa") }
                }) {
                    Div(attrs = { classes("flex", "justify-between", "align-center", "mb-sm") }) {
                        Span(attrs = { classes("font-bold") }) { Text(opportunity.area.name.replace("_", " ")) }
                        Span(attrs = {
                            classes("badge")
                            style {
                                property("background", when (opportunity.priority) {
                                    NegotiationPriority.HIGH -> "#F44336"
                                    NegotiationPriority.MEDIUM -> "#FF9800"
                                    NegotiationPriority.LOW -> "#2196F3"
                                    else -> "#666"
                                })
                                property("color", "white")
                                property("padding", "2px 8px")
                                property("border-radius", "8px")
                                property("font-size", "12px")
                            }
                        }) { Text(opportunity.priority.name) }
                    }
                    
                    Div(attrs = { classes("text-sm", "mb-sm") }) {
                        Span(attrs = { classes("text-secondary") }) { Text("Current: ") }
                        Text(opportunity.currentValue)
                        Span(attrs = { classes("text-secondary") }) { Text(" → Target: ") }
                        Span(attrs = { classes("text-success", "font-bold") }) { Text(opportunity.suggestedTarget) }
                    }
                    
                    P(attrs = { classes("text-secondary", "text-sm") }) {
                        Text(opportunity.marketJustification)
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

private fun formatSalary(amount: Double): String {
    return "$${amount.asDynamic().toLocaleString("en-CA", js("({style: 'decimal', minimumFractionDigits: 0, maximumFractionDigits: 0})")) as String} CAD"
}

private fun formatPercentage(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign${value.asDynamic().toFixed(1)}%"
}
