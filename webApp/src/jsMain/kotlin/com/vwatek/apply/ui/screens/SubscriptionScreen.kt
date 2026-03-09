package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.vwatek.apply.data.api.SubscriptionApiClient
import com.vwatek.apply.domain.model.SubscriptionTier
import com.vwatek.apply.domain.model.BillingPeriod
import com.vwatek.apply.domain.model.FeatureLimits
import com.vwatek.apply.domain.model.SubscriptionPricing
import com.vwatek.apply.domain.model.PremiumFeature
import com.vwatek.apply.domain.usecase.SubscriptionManager
import com.vwatek.apply.domain.usecase.SubscriptionState
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Subscription Screen for Web
 * Shows pricing tiers and allows users to upgrade via Stripe Checkout
 * Wired to shared SubscriptionManager and SubscriptionApiClient
 */
@Composable
fun SubscriptionScreen() {
    val subscriptionManager = remember { GlobalContext.get().get<SubscriptionManager>() }
    val subscriptionApiClient = remember { GlobalContext.get().get<SubscriptionApiClient>() }
    val subscriptionState by subscriptionManager.subscriptionState.collectAsState()
    
    var selectedBillingPeriod by remember { mutableStateOf(BillingPeriod.YEARLY) }
    var showCheckoutModal by remember { mutableStateOf(false) }
    var selectedTier by remember { mutableStateOf<SubscriptionTier?>(null) }
    var isCheckingOut by remember { mutableStateOf(false) }
    var checkoutError by remember { mutableStateOf<String?>(null) }
    
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    // Derive state from SubscriptionManager
    val currentTier = when (val state = subscriptionState) {
        is SubscriptionState.Success -> state.tier
        else -> SubscriptionTier.FREE
    }
    val isDemoMode = when (val state = subscriptionState) {
        is SubscriptionState.Success -> state.isDemoMode
        else -> SubscriptionManager.isDemoMode
    }
    val isLoading = subscriptionState is SubscriptionState.Loading
    
    // Refresh subscription on mount
    LaunchedEffect(Unit) {
        subscriptionManager.refreshSubscription()
    }
    
    Div(attrs = { classes("subscription-screen") }) {
        // Demo mode banner
        if (isDemoMode) {
            Div(attrs = { 
                classes("demo-mode-banner", "mb-lg")
                style { 
                    property("background", "linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%)")
                    property("border", "1px solid #4CAF50")
                    property("border-radius", "12px")
                    property("padding", "16px")
                }
            }) {
                Div(attrs = { classes("flex", "align-center", "gap-md") }) {
                    Span(attrs = { style { property("font-size", "24px") } }) { Text("⭐") }
                    Div {
                        Div(attrs = { 
                            style { 
                                property("font-weight", "bold")
                                property("color", "#2E7D32")
                            }
                        }) { Text("Demo Mode Active") }
                        Div(attrs = { 
                            style { 
                                property("font-size", "14px")
                                property("color", "#388E3C")
                            }
                        }) { Text(SubscriptionManager.DEMO_MODE_MESSAGE) }
                    }
                }
            }
        }
        
        // Header
        Div(attrs = { classes("subscription-header", "text-center", "mb-xl") }) {
            H1(attrs = { classes("mb-md") }) { Text("Choose Your Plan") }
            P(attrs = { classes("text-secondary", "text-lg") }) {
                Text("Unlock powerful features to accelerate your job search")
            }
        }
        
        // Current plan banner (if not free and not in demo mode)
        if (currentTier != SubscriptionTier.FREE && !isDemoMode) {
            Div(attrs = { classes("card", "card-highlight", "mb-lg") }) {
                Div(attrs = { classes("flex", "justify-between", "align-center") }) {
                    Div {
                        Span(attrs = { classes("text-sm", "text-secondary") }) { Text("Current Plan") }
                        H3 { Text(currentTier.name) }
                    }
                    Button(attrs = {
                        classes("btn", "btn-outline")
                        onClick {
                            scope.launch {
                                subscriptionApiClient.createPortalSession(
                                    returnUrl = kotlinx.browser.window.location.href
                                ).onSuccess { response ->
                                    kotlinx.browser.window.location.href = response.portalUrl
                                }.onFailure { error ->
                                    checkoutError = "Failed to open billing portal: ${error.message}"
                                }
                            }
                        }
                    }) {
                        Text("Manage Billing")
                    }
                }
            }
        }
        
        // Billing period toggle
        Div(attrs = { classes("billing-toggle", "flex", "justify-center", "mb-xl") }) {
            Div(attrs = { classes("toggle-container") }) {
                Button(attrs = {
                    classes("toggle-btn", if (selectedBillingPeriod == BillingPeriod.MONTHLY) "active" else "")
                    onClick { selectedBillingPeriod = BillingPeriod.MONTHLY }
                }) {
                    Text("Monthly")
                }
                Button(attrs = {
                    classes("toggle-btn", if (selectedBillingPeriod == BillingPeriod.YEARLY) "active" else "")
                    onClick { selectedBillingPeriod = BillingPeriod.YEARLY }
                }) {
                    Text("Yearly")
                    Span(attrs = { classes("badge", "badge-success", "ml-sm") }) {
                        Text("Save 17%")
                    }
                }
            }
        }
        
        // Pricing cards grid
        Div(attrs = { classes("pricing-grid") }) {
            // Free Tier
            PricingCard(
                tier = SubscriptionTier.FREE,
                billingPeriod = selectedBillingPeriod,
                isCurrentTier = currentTier == SubscriptionTier.FREE,
                isPopular = false,
                onSelect = { }
            )
            
            // Pro Tier
            PricingCard(
                tier = SubscriptionTier.PRO,
                billingPeriod = selectedBillingPeriod,
                isCurrentTier = currentTier == SubscriptionTier.PRO,
                isPopular = true,
                onSelect = {
                    selectedTier = SubscriptionTier.PRO
                    showCheckoutModal = true
                }
            )
            
            // Premium Tier
            PricingCard(
                tier = SubscriptionTier.PREMIUM,
                billingPeriod = selectedBillingPeriod,
                isCurrentTier = currentTier == SubscriptionTier.PREMIUM,
                isPopular = false,
                onSelect = {
                    selectedTier = SubscriptionTier.PREMIUM
                    showCheckoutModal = true
                }
            )
        }
        
        // Feature comparison table
        Div(attrs = { classes("card", "mt-xl") }) {
            H3(attrs = { classes("card-title", "mb-lg") }) { Text("Compare Plans") }
            
            Table(attrs = { classes("comparison-table") }) {
                Thead {
                    Tr {
                        Th { Text("Feature") }
                        Th(attrs = { classes("text-center") }) { Text("Free") }
                        Th(attrs = { classes("text-center") }) { Text("Pro") }
                        Th(attrs = { classes("text-center") }) { Text("Premium") }
                    }
                }
                Tbody {
                    ComparisonRow("Resume builder", "3/mo", "10/mo", "Unlimited")
                    ComparisonRow("Cover letters", "3/mo", "10/mo", "Unlimited")
                    ComparisonRow("AI enhancements", "5/day", "20/day", "Unlimited")
                    ComparisonRow("Interview practice", "3/mo", "10/mo", "Unlimited")
                    ComparisonRow("Application tracker", "10", "Unlimited", "Unlimited")
                    ComparisonRow("Salary insights", "✗", "✓", "✓", hasCheckmark = true)
                    ComparisonRow("Negotiation coach", "✗", "✗", "✓", hasCheckmark = true)
                    ComparisonRow("LinkedIn optimizer", "✗", "✗", "✓", hasCheckmark = true)
                    ComparisonRow("Priority support", "✗", "✗", "✓", hasCheckmark = true)
                }
            }
        }
        
        // Fine print
        P(attrs = { classes("text-center", "text-secondary", "text-sm", "mt-lg") }) {
            Text("30-day money-back guarantee. Cancel anytime.")
        }
    }
    
    // Checkout Modal
    if (showCheckoutModal && selectedTier != null) {
        CheckoutModal(
            tier = selectedTier!!,
            billingPeriod = selectedBillingPeriod,
            isLoading = isCheckingOut,
            onClose = { showCheckoutModal = false },
            onCheckout = { tier, period ->
                isCheckingOut = true
                checkoutError = null
                scope.launch {
                    val baseUrl = kotlinx.browser.window.location.origin
                    subscriptionApiClient.createCheckoutSession(
                        tier = tier,
                        billingPeriod = period,
                        successUrl = "$baseUrl/?checkout=success",
                        cancelUrl = "$baseUrl/?checkout=cancel"
                    ).onSuccess { response ->
                        // Redirect to Stripe Checkout
                        kotlinx.browser.window.location.href = response.checkoutUrl
                    }.onFailure { error ->
                        isCheckingOut = false
                        checkoutError = "Failed to start checkout: ${error.message}"
                    }
                }
            }
        )
    }
    
    // Error toast
    checkoutError?.let { error ->
        Div(attrs = { 
            classes("alert", "alert-error", "mt-md")
            style {
                property("position", "fixed")
                property("bottom", "24px")
                property("right", "24px")
                property("z-index", "1000")
                property("max-width", "400px")
            }
        }) {
            Text(error)
            Button(attrs = {
                classes("btn-icon", "ml-md")
                onClick { checkoutError = null }
            }) { Text("×") }
        }
    }
}

@Composable
private fun PricingCard(
    tier: SubscriptionTier,
    billingPeriod: BillingPeriod,
    isCurrentTier: Boolean,
    isPopular: Boolean,
    onSelect: () -> Unit
) {
    val pricing = SubscriptionPricing.forTier(tier)
    val limits = FeatureLimits.forTier(tier)
    
    val displayPrice = when (billingPeriod) {
        BillingPeriod.MONTHLY -> pricing?.monthlyPriceCad ?: 0.0
        BillingPeriod.YEARLY -> (pricing?.yearlyPriceCad ?: 0.0) / 12
    }
    
    Div(attrs = {
        classes(
            "pricing-card",
            if (isPopular) "pricing-card-popular" else "",
            if (isCurrentTier) "pricing-card-current" else ""
        )
    }) {
        // Popular badge
        if (isPopular) {
            Div(attrs = { classes("popular-badge") }) {
                Text("MOST POPULAR")
            }
        }
        
        // Tier name
        H3(attrs = { classes("tier-name") }) { Text(tier.name) }
        
        // Price
        Div(attrs = { classes("price-container", "mb-md") }) {
            if (tier == SubscriptionTier.FREE) {
                Span(attrs = { classes("price") }) { Text("Free") }
            } else {
                Span(attrs = { classes("currency") }) { Text("CA$") }
                Span(attrs = { classes("price") }) { Text(displayPrice.asDynamic().toFixed(2) as String) }
                Span(attrs = { classes("period") }) { Text("/mo") }
            }
        }
        
        if (billingPeriod == BillingPeriod.YEARLY && tier != SubscriptionTier.FREE) {
            P(attrs = { classes("billing-note", "text-secondary", "text-sm") }) {
                Text("Billed CA$${pricing?.yearlyPriceCad ?: 0.0} yearly")
            }
        }
        
        // Features list
        Ul(attrs = { classes("features-list") }) {
            FeatureListItem(
                icon = "📄",
                text = "${if (limits.resumeVersionsPerMonth == Int.MAX_VALUE) "Unlimited" else limits.resumeVersionsPerMonth} resume versions/month"
            )
            FeatureListItem(
                icon = "✉️",
                text = "${if (limits.coverLettersPerMonth == Int.MAX_VALUE) "Unlimited" else limits.coverLettersPerMonth} cover letters/month"
            )
            FeatureListItem(
                icon = "✨",
                text = "${if (limits.aiEnhancementsPerDay == Int.MAX_VALUE) "Unlimited" else limits.aiEnhancementsPerDay} AI enhancements/day"
            )
            FeatureListItem(
                icon = "🎤",
                text = "${if (limits.interviewSessionsPerMonth == Int.MAX_VALUE) "Unlimited" else limits.interviewSessionsPerMonth} interview sessions/month"
            )
            
            if (limits.salaryInsightsAccess) {
                FeatureListItem(icon = "💰", text = "Salary insights & benchmarks")
            }
            if (limits.negotiationCoachAccess) {
                FeatureListItem(icon = "🧠", text = "AI negotiation coach")
            }
            if (limits.linkedInOptimizerAccess) {
                FeatureListItem(icon = "👤", text = "LinkedIn profile optimizer")
            }
            if (tier == SubscriptionTier.PREMIUM) {
                FeatureListItem(icon = "⭐", text = "Priority support")
            }
        }
        
        // CTA Button
        Button(attrs = {
            classes(
                "btn",
                "btn-full",
                if (isPopular) "btn-primary" else "btn-secondary",
                if (isCurrentTier || tier == SubscriptionTier.FREE) "btn-disabled" else ""
            )
            if (!isCurrentTier && tier != SubscriptionTier.FREE) {
                onClick { onSelect() }
            }
        }) {
            Text(when {
                isCurrentTier -> "Current Plan"
                tier == SubscriptionTier.FREE -> "Free Forever"
                else -> "Upgrade to ${tier.name}"
            })
        }
    }
}

@Composable
private fun FeatureListItem(icon: String, text: String) {
    Li(attrs = { classes("feature-item") }) {
        Span(attrs = { classes("feature-icon") }) { Text(icon) }
        Span(attrs = { classes("feature-text") }) { Text(text) }
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    free: String,
    pro: String,
    premium: String,
    hasCheckmark: Boolean = false
) {
    Tr {
        Td { Text(feature) }
        Td(attrs = { classes("text-center", if (free == "✗") "text-muted" else "") }) { 
            if (hasCheckmark && free == "✓") {
                Span(attrs = { classes("checkmark") }) { Text("✓") }
            } else {
                Text(free)
            }
        }
        Td(attrs = { classes("text-center", if (pro == "✓") "text-success" else "") }) {
            if (hasCheckmark && pro == "✓") {
                Span(attrs = { classes("checkmark", "text-success") }) { Text("✓") }
            } else {
                Text(pro)
            }
        }
        Td(attrs = { classes("text-center", if (premium == "✓" || premium == "Unlimited") "text-success" else "", "font-bold") }) {
            if (hasCheckmark && premium == "✓") {
                Span(attrs = { classes("checkmark", "text-success") }) { Text("✓") }
            } else {
                Text(premium)
            }
        }
    }
}

@Composable
private fun CheckoutModal(
    tier: SubscriptionTier,
    billingPeriod: BillingPeriod,
    isLoading: Boolean = false,
    onClose: () -> Unit,
    onCheckout: (SubscriptionTier, BillingPeriod) -> Unit
) {
    val pricing = SubscriptionPricing.forTier(tier)
    val price = when (billingPeriod) {
        BillingPeriod.MONTHLY -> pricing?.monthlyPriceCad ?: 0.0
        BillingPeriod.YEARLY -> pricing?.yearlyPriceCad ?: 0.0
    }
    
    Div(attrs = { classes("modal-overlay") }) {
        Div(attrs = { classes("modal", "modal-md") }) {
            Div(attrs = { classes("modal-header") }) {
                H3 { Text("Upgrade to ${tier.name}") }
                Button(attrs = {
                    classes("btn-icon")
                    onClick { onClose() }
                }) {
                    Text("×")
                }
            }
            
            Div(attrs = { classes("modal-body") }) {
                Div(attrs = { classes("checkout-summary", "mb-lg") }) {
                    Div(attrs = { classes("flex", "justify-between", "mb-md") }) {
                        Span { Text("Plan") }
                        Span(attrs = { classes("font-bold") }) { Text(tier.name) }
                    }
                    Div(attrs = { classes("flex", "justify-between", "mb-md") }) {
                        Span { Text("Billing") }
                        Span { Text(if (billingPeriod == BillingPeriod.MONTHLY) "Monthly" else "Yearly") }
                    }
                    Hr()
                    Div(attrs = { classes("flex", "justify-between", "font-bold") }) {
                        Span { Text("Total") }
                        Span { 
                            Text("CA$$${price}")
                            Span(attrs = { classes("text-secondary", "text-sm") }) {
                                Text(if (billingPeriod == BillingPeriod.MONTHLY) "/month" else "/year")
                            }
                        }
                    }
                }
                
                P(attrs = { classes("text-secondary", "text-sm", "mb-lg") }) {
                    Text("You will be redirected to our secure payment provider to complete your purchase.")
                }
            }
            
            Div(attrs = { classes("modal-footer") }) {
                Button(attrs = {
                    classes("btn", "btn-outline")
                    if (!isLoading) onClick { onClose() }
                }) {
                    Text("Cancel")
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    if (isLoading) classes("btn-disabled")
                    if (!isLoading) onClick { onCheckout(tier, billingPeriod) }
                }) {
                    if (isLoading) {
                        Text("Processing...")
                    } else {
                        Text("Proceed to Checkout")
                    }
                }
            }
        }
    }
}
