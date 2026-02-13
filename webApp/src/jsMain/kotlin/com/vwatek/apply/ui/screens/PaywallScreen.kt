package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.SubscriptionTier
import com.vwatek.apply.domain.model.BillingPeriod
import com.vwatek.apply.domain.model.PremiumFeature
import com.vwatek.apply.domain.model.SubscriptionPricing
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*

/**
 * Paywall Modal for Web
 * Shows when a user tries to access a premium feature
 * Phase 4: Premium & Monetization
 */
@Composable
fun PaywallModal(
    feature: PremiumFeature,
    requiredTier: SubscriptionTier,
    onClose: () -> Unit,
    onUpgrade: (SubscriptionTier, BillingPeriod) -> Unit
) {
    var selectedBillingPeriod by remember { mutableStateOf(BillingPeriod.YEARLY) }
    
    val featureInfo = getFeatureInfo(feature)
    val pricing = SubscriptionPricing.forTier(requiredTier)
    
    Div(attrs = { classes("modal-overlay") }) {
        Div(attrs = { classes("modal", "modal-md", "paywall-modal") }) {
            // Close button
            Button(attrs = {
                classes("modal-close")
                onClick { onClose() }
            }) {
                Text("×")
            }
            
            Div(attrs = { classes("paywall-content", "text-center") }) {
                // Feature icon
                Div(attrs = { classes("feature-icon-large", "mb-lg") }) {
                    Text(featureInfo.icon)
                }
                
                // Feature name
                H2(attrs = { classes("mb-sm") }) { Text(featureInfo.title) }
                
                // Feature description
                P(attrs = { classes("text-secondary", "mb-lg") }) {
                    Text(featureInfo.description)
                }
                
                // Tier badge
                Div(attrs = { classes("tier-badge", "mb-lg") }) {
                    Span(attrs = { classes("icon") }) { Text("🔒") }
                    Span { Text("Requires ${requiredTier.name} plan") }
                }
                
                // Billing period toggle
                Div(attrs = { classes("billing-toggle-compact", "mb-lg") }) {
                    Button(attrs = {
                        classes("toggle-option", if (selectedBillingPeriod == BillingPeriod.MONTHLY) "active" else "")
                        onClick { selectedBillingPeriod = BillingPeriod.MONTHLY }
                    }) {
                        Div(attrs = { classes("toggle-label") }) { Text("Monthly") }
                        Div(attrs = { classes("toggle-price") }) { Text("$${pricing.monthlyPrice}/mo") }
                    }
                    Button(attrs = {
                        classes("toggle-option", if (selectedBillingPeriod == BillingPeriod.YEARLY) "active" else "")
                        onClick { selectedBillingPeriod = BillingPeriod.YEARLY }
                    }) {
                        Div(attrs = { classes("toggle-label") }) { 
                            Text("Yearly") 
                            Span(attrs = { classes("savings-badge") }) { Text("Save 17%") }
                        }
                        Div(attrs = { classes("toggle-price") }) { Text("$${pricing.yearlyPrice / 12}/mo") }
                    }
                }
                
                // Benefits list
                Ul(attrs = { classes("benefits-list", "mb-lg") }) {
                    featureInfo.benefits.forEach { benefit ->
                        Li {
                            Span(attrs = { classes("checkmark") }) { Text("✓") }
                            Text(benefit)
                        }
                    }
                }
                
                // CTA Buttons
                Button(attrs = {
                    classes("btn", "btn-primary", "btn-lg", "btn-full", "mb-md")
                    onClick { onUpgrade(requiredTier, selectedBillingPeriod) }
                }) {
                    Text("Upgrade to ${requiredTier.name}")
                }
                
                Button(attrs = {
                    classes("btn", "btn-text")
                    onClick { onClose() }
                }) {
                    Text("Maybe Later")
                }
                
                // Fine print
                P(attrs = { classes("text-secondary", "text-xs", "mt-md") }) {
                    Text("30-day money-back guarantee • Cancel anytime")
                }
            }
        }
    }
}

/**
 * Inline Paywall Banner component
 */
@Composable
fun PaywallBanner(
    feature: PremiumFeature,
    requiredTier: SubscriptionTier,
    onUpgradeClick: () -> Unit
) {
    val featureInfo = getFeatureInfo(feature)
    
    Div(attrs = { classes("paywall-banner") }) {
        Div(attrs = { classes("banner-icon") }) { Text("🔒") }
        Div(attrs = { classes("banner-content") }) {
            H4 { Text(featureInfo.title) }
            P(attrs = { classes("text-secondary", "text-sm") }) {
                Text("Upgrade to ${requiredTier.name} to unlock")
            }
        }
        Button(attrs = {
            classes("btn", "btn-primary", "btn-sm")
            onClick { onUpgradeClick() }
        }) {
            Text("Upgrade")
        }
    }
}

/**
 * Feature Gated Content wrapper
 */
@Composable
fun FeatureGatedContent(
    feature: PremiumFeature,
    hasAccess: Boolean,
    requiredTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    content: @Composable () -> Unit
) {
    if (hasAccess) {
        content()
    } else {
        Div(attrs = { classes("feature-gated") }) {
            val featureInfo = getFeatureInfo(feature)
            
            Div(attrs = { classes("gated-content", "text-center") }) {
                Div(attrs = { classes("gated-icon") }) { Text(featureInfo.icon) }
                H3(attrs = { classes("mb-sm") }) { Text(featureInfo.title) }
                P(attrs = { classes("text-secondary", "mb-lg") }) {
                    Text(featureInfo.description)
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick { onUpgradeClick() }
                }) {
                    Span { Text("🔒") }
                    Text(" Upgrade to ${requiredTier.name}")
                }
            }
        }
    }
}

// Feature information helper
private data class FeatureInfo(
    val title: String,
    val description: String,
    val icon: String,
    val benefits: List<String>
)

private fun getFeatureInfo(feature: PremiumFeature): FeatureInfo {
    return when (feature) {
        PremiumFeature.SALARY_INSIGHTS -> FeatureInfo(
            title = "Salary Insights",
            description = "Get real-time salary data and market comparisons for your target roles",
            icon = "💰",
            benefits = listOf(
                "Real-time salary benchmarks",
                "Cost of living adjustments",
                "Industry-specific data",
                "Compare multiple locations"
            )
        )
        PremiumFeature.NEGOTIATION_COACH -> FeatureInfo(
            title = "AI Negotiation Coach",
            description = "Get personalized negotiation strategies powered by AI",
            icon = "🧠",
            benefits = listOf(
                "Personalized negotiation scripts",
                "Counter-offer strategies",
                "Real-time coaching chat",
                "Role-play practice scenarios"
            )
        )
        PremiumFeature.LINKEDIN_OPTIMIZER -> FeatureInfo(
            title = "LinkedIn Optimizer",
            description = "Optimize your LinkedIn profile to attract more recruiters",
            icon = "👤",
            benefits = listOf(
                "Profile analysis & scoring",
                "AI-optimized headlines",
                "Keyword optimization",
                "Section-by-section improvements"
            )
        )
        PremiumFeature.UNLIMITED_APPLICATIONS -> FeatureInfo(
            title = "Unlimited Applications",
            description = "Track unlimited job applications without restrictions",
            icon = "💼",
            benefits = listOf(
                "Unlimited application tracking",
                "Advanced analytics",
                "Custom application stages",
                "Bulk operations"
            )
        )
        PremiumFeature.UNLIMITED_AI_ENHANCEMENTS -> FeatureInfo(
            title = "Unlimited AI Enhancements",
            description = "Use AI to enhance your documents without daily limits",
            icon = "✨",
            benefits = listOf(
                "Unlimited AI suggestions",
                "Priority AI processing",
                "Advanced AI models",
                "Custom enhancement styles"
            )
        )
        PremiumFeature.UNLIMITED_RESUMES -> FeatureInfo(
            title = "Unlimited Resumes",
            description = "Create unlimited resume versions for different roles",
            icon = "📄",
            benefits = listOf(
                "Unlimited resume versions",
                "Role-specific optimization",
                "A/B testing versions",
                "Version history"
            )
        )
        PremiumFeature.UNLIMITED_COVER_LETTERS -> FeatureInfo(
            title = "Unlimited Cover Letters",
            description = "Generate unlimited cover letters tailored to each job",
            icon = "✉️",
            benefits = listOf(
                "Unlimited cover letters",
                "Company-specific tailoring",
                "Multiple tones & styles",
                "Quick regeneration"
            )
        )
        PremiumFeature.UNLIMITED_INTERVIEWS -> FeatureInfo(
            title = "Unlimited Interview Practice",
            description = "Practice interviews with AI without session limits",
            icon = "🎤",
            benefits = listOf(
                "Unlimited practice sessions",
                "Industry-specific questions",
                "Detailed feedback",
                "Progress tracking"
            )
        )
    }
}

/**
 * Standalone Paywall Screen for navigation
 * Shows full subscription pricing page
 */
@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit = {},
    onSubscriptionComplete: () -> Unit = {}
) {
    var selectedTier by remember { mutableStateOf(SubscriptionTier.PRO) }
    var selectedBillingPeriod by remember { mutableStateOf(BillingPeriod.YEARLY) }
    var isProcessing by remember { mutableStateOf(false) }
    
    Div(attrs = { classes("paywall-screen") }) {
        // Header
        Div(attrs = { classes("section-header", "mb-xl", "text-center") }) {
            H1(attrs = { classes("mb-sm") }) { Text("Upgrade Your Career") }
            P(attrs = { classes("text-secondary", "text-lg") }) {
                Text("Choose the plan that's right for you")
            }
        }
        
        // Billing toggle
        Div(attrs = { 
            classes("billing-toggle", "flex", "justify-center", "mb-xl", "gap-md")
            style {
                property("background", "var(--bg-secondary)")
                property("padding", "8px")
                property("border-radius", "12px")
                property("display", "inline-flex")
                property("margin", "0 auto 2rem auto")
            }
        }) {
            Button(attrs = {
                classes("btn", if (selectedBillingPeriod == BillingPeriod.MONTHLY) "btn-primary" else "btn-ghost")
                onClick { selectedBillingPeriod = BillingPeriod.MONTHLY }
            }) {
                Text("Monthly")
            }
            Button(attrs = {
                classes("btn", if (selectedBillingPeriod == BillingPeriod.YEARLY) "btn-primary" else "btn-ghost")
                onClick { selectedBillingPeriod = BillingPeriod.YEARLY }
            }) {
                Text("Yearly")
                Span(attrs = { 
                    style { 
                        property("background", "var(--success-color)")
                        property("color", "white")
                        property("padding", "2px 8px")
                        property("border-radius", "4px")
                        property("font-size", "0.75rem")
                        property("margin-left", "8px")
                    }
                }) {
                    Text("Save 17%")
                }
            }
        }
        
        // Pricing cards
        Div(attrs = { 
            classes("pricing-cards", "grid", "gap-lg")
            style {
                property("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
                property("max-width", "1000px")
                property("margin", "0 auto")
            }
        }) {
            listOf(SubscriptionTier.PRO, SubscriptionTier.PREMIUM).forEach { tier ->
                val pricing = SubscriptionPricing.forTier(tier)
                val isSelected = selectedTier == tier
                
                Div(attrs = { 
                    classes("pricing-card", "card")
                    if (isSelected) classes("selected")
                    onClick { selectedTier = tier }
                    style {
                        property("border", if (isSelected) "2px solid var(--primary-color)" else "1px solid var(--border-color)")
                        property("cursor", "pointer")
                        property("transition", "all 0.2s ease")
                    }
                }) {
                    if (tier == SubscriptionTier.PREMIUM) {
                        Div(attrs = { 
                            classes("popular-badge")
                            style {
                                property("background", "var(--primary-color)")
                                property("color", "white")
                                property("padding", "4px 12px")
                                property("border-radius", "0 0 8px 8px")
                                property("font-size", "0.75rem")
                                property("font-weight", "600")
                                property("text-align", "center")
                                property("margin", "-1px -1px 16px -1px")
                            }
                        }) {
                            Text("MOST POPULAR")
                        }
                    }
                    
                    H3(attrs = { classes("mb-sm") }) { Text(tier.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    
                    Div(attrs = { classes("price", "mb-md") }) {
                        val price = if (selectedBillingPeriod == BillingPeriod.YEARLY)
                            pricing.yearlyCad / 12 else pricing.monthlyCad
                        Span(attrs = { 
                            style { 
                                property("font-size", "2.5rem")
                                property("font-weight", "700")
                            }
                        }) {
                            Text("$${"%.2f".format(price)}")
                        }
                        Span(attrs = { classes("text-secondary") }) {
                            Text("/month")
                        }
                    }
                    
                    if (selectedBillingPeriod == BillingPeriod.YEARLY) {
                        P(attrs = { 
                            classes("text-secondary", "text-sm", "mb-md")
                        }) {
                            Text("Billed annually at $${"%.2f".format(pricing.yearlyCad)}")
                        }
                    }
                    
                    Ul(attrs = { 
                        classes("feature-list")
                        style { 
                            property("list-style", "none")
                            property("padding", "0")
                        }
                    }) {
                        pricing.features.forEach { feature ->
                            Li(attrs = { 
                                classes("flex", "items-center", "gap-sm", "mb-sm")
                            }) {
                                Span(attrs = { 
                                    style { property("color", "var(--success-color)") }
                                }) { Text("✓") }
                                Text(feature)
                            }
                        }
                    }
                }
            }
        }
        
        // CTA Button
        Div(attrs = { 
            classes("mt-xl", "text-center")
        }) {
            Button(attrs = {
                classes("btn", "btn-primary", "btn-lg")
                if (isProcessing) attr("disabled", "true")
                onClick { 
                    isProcessing = true
                    // Would initiate Stripe checkout here
                    // stripeService.createCheckoutSession(...)
                    onSubscriptionComplete()
                }
            }) {
                if (isProcessing) {
                    Text("Processing...")
                } else {
                    Text("Subscribe to ${selectedTier.name}")
                }
            }
            
            P(attrs = { 
                classes("text-secondary", "text-sm", "mt-md")
            }) {
                Text("30-day money-back guarantee • Cancel anytime")
            }
        }
        
        // Back button
        Div(attrs = { classes("mt-lg", "text-center") }) {
            Button(attrs = {
                classes("btn", "btn-ghost")
                onClick { onNavigateBack() }
            }) {
                Text("← Back")
            }
        }
    }
}
