package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vwatek.apply.domain.model.SubscriptionTier
import com.vwatek.apply.domain.model.BillingPeriod
import com.vwatek.apply.domain.model.FeatureLimits
import com.vwatek.apply.domain.model.SubscriptionPricing
import com.vwatek.apply.domain.usecase.SubscriptionState
import com.vwatek.apply.domain.usecase.SubscriptionManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Subscription Screen - Shows pricing tiers and allows upgrading
 * Phase 4: Premium & Monetization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit = {},
    onStartCheckout: (tier: SubscriptionTier, billingPeriod: BillingPeriod) -> Unit = { _, _ -> },
    onManageBilling: () -> Unit = {}
) {
    val subscriptionManager: SubscriptionManager = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val subscriptionState by subscriptionManager.subscriptionState.collectAsState()
    
    var selectedBillingPeriod by remember { mutableStateOf(BillingPeriod.YEARLY) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        subscriptionManager.refreshSubscription()
        isLoading = false
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Subscription") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading || subscriptionState is SubscriptionState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Demo mode banner
                val currentState = subscriptionState
                if (currentState is SubscriptionState.Success && currentState.isDemoMode) {
                    DemoModeBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Current subscription banner
                if (currentState is SubscriptionState.Success) {
                    if (currentState.tier != SubscriptionTier.FREE && !currentState.isDemoMode) {
                        CurrentSubscriptionBanner(
                            tier = currentState.tier,
                            onManageBilling = onManageBilling
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Header
                Text(
                    text = "Choose Your Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Unlock powerful features to accelerate your job search",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Billing period toggle
                BillingPeriodToggle(
                    selectedPeriod = selectedBillingPeriod,
                    onPeriodSelected = { selectedBillingPeriod = it }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Pricing cards
                val currentTier = (subscriptionState as? SubscriptionState.Success)?.tier ?: SubscriptionTier.FREE
                
                // Free Tier
                PricingCard(
                    tier = SubscriptionTier.FREE,
                    billingPeriod = selectedBillingPeriod,
                    isCurrentPlan = currentTier == SubscriptionTier.FREE,
                    onSelect = { /* Already on free */ }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pro Tier
                PricingCard(
                    tier = SubscriptionTier.PRO,
                    billingPeriod = selectedBillingPeriod,
                    isCurrentPlan = currentTier == SubscriptionTier.PRO,
                    isPopular = true,
                    onSelect = {
                        scope.launch {
                            onStartCheckout(SubscriptionTier.PRO, selectedBillingPeriod)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Premium Tier
                PricingCard(
                    tier = SubscriptionTier.PREMIUM,
                    billingPeriod = selectedBillingPeriod,
                    isCurrentPlan = currentTier == SubscriptionTier.PREMIUM,
                    onSelect = {
                        scope.launch {
                            onStartCheckout(SubscriptionTier.PREMIUM, selectedBillingPeriod)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Feature comparison
                FeatureComparisonSection()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // FAQ / Fine print
                Text(
                    text = "30-day money-back guarantee. Cancel anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CurrentSubscriptionBanner(
    tier: SubscriptionTier,
    onManageBilling: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Plan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = tier.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(onClick = onManageBilling) {
                Text("Manage Billing")
            }
        }
    }
}

@Composable
private fun DemoModeBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Demo Mode Active",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "Enjoy all Premium features free during our beta!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF388E3C)
                )
            }
        }
    }
}

@Composable
private fun BillingPeriodToggle(
    selectedPeriod: BillingPeriod,
    onPeriodSelected: (BillingPeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BillingPeriodOption(
                label = "Monthly",
                isSelected = selectedPeriod == BillingPeriod.MONTHLY,
                onClick = { onPeriodSelected(BillingPeriod.MONTHLY) },
                modifier = Modifier.weight(1f)
            )
            BillingPeriodOption(
                label = "Yearly",
                subtitle = "Save 17%",
                isSelected = selectedPeriod == BillingPeriod.YEARLY,
                onClick = { onPeriodSelected(BillingPeriod.YEARLY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BillingPeriodOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PricingCard(
    tier: SubscriptionTier,
    billingPeriod: BillingPeriod,
    isCurrentPlan: Boolean,
    isPopular: Boolean = false,
    onSelect: () -> Unit
) {
    val pricing = SubscriptionPricing.forTier(tier)
    val limits = FeatureLimits.forTier(tier)
    
    val price = when (billingPeriod) {
        BillingPeriod.MONTHLY -> pricing.monthlyPrice
        BillingPeriod.YEARLY -> pricing.yearlyPrice / 12 // Show monthly equivalent
    }
    
    val borderColor = when {
        isPopular -> MaterialTheme.colorScheme.primary
        isCurrentPlan -> MaterialTheme.colorScheme.outline
        else -> Color.Transparent
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPopular || isCurrentPlan) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Popular badge
            if (isPopular) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "MOST POPULAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Tier name
            Text(
                text = tier.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                if (tier == SubscriptionTier.FREE) {
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f".format(price),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/mo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }
            
            if (billingPeriod == BillingPeriod.YEARLY && tier != SubscriptionTier.FREE) {
                Text(
                    text = "Billed $%.2f yearly".format(pricing.yearlyPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features list
            PricingFeatureItem(
                icon = Icons.Default.Description,
                text = "${if (limits.resumeVersionsPerMonth == Int.MAX_VALUE) "Unlimited" else limits.resumeVersionsPerMonth} resume versions/month"
            )
            PricingFeatureItem(
                icon = Icons.Default.Mail,
                text = "${if (limits.coverLettersPerMonth == Int.MAX_VALUE) "Unlimited" else limits.coverLettersPerMonth} cover letters/month"
            )
            PricingFeatureItem(
                icon = Icons.Default.AutoAwesome,
                text = "${if (limits.aiEnhancementsPerDay == Int.MAX_VALUE) "Unlimited" else limits.aiEnhancementsPerDay} AI enhancements/day"
            )
            PricingFeatureItem(
                icon = Icons.Default.RecordVoiceOver,
                text = "${if (limits.interviewSessionsPerMonth == Int.MAX_VALUE) "Unlimited" else limits.interviewSessionsPerMonth} interview sessions/month"
            )
            
            if (limits.salaryInsightsAccess) {
                PricingFeatureItem(
                    icon = Icons.Default.AttachMoney,
                    text = "Salary insights & benchmarks"
                )
            }
            if (limits.negotiationCoachAccess) {
                PricingFeatureItem(
                    icon = Icons.Default.Psychology,
                    text = "AI negotiation coach"
                )
            }
            if (limits.linkedInOptimizerAccess) {
                PricingFeatureItem(
                    icon = Icons.Default.Person,
                    text = "LinkedIn profile optimizer"
                )
            }
            if (limits.prioritySupport) {
                PricingFeatureItem(
                    icon = Icons.Default.Support,
                    text = "Priority support"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // CTA Button
            Button(
                onClick = onSelect,
                enabled = !isCurrentPlan && tier != SubscriptionTier.FREE,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isPopular) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            ) {
                Text(
                    text = when {
                        isCurrentPlan -> "Current Plan"
                        tier == SubscriptionTier.FREE -> "Free Forever"
                        else -> "Upgrade to ${tier.name}"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PricingFeatureItem(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FeatureComparisonSection() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Compare Plans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Feature comparison table header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Feature",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Pro",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ComparisonRow("Resume builder", "3/mo", "10/mo", "∞")
            ComparisonRow("Cover letters", "3/mo", "10/mo", "∞")
            ComparisonRow("AI enhancements", "5/day", "20/day", "∞")
            ComparisonRow("Interview practice", "3/mo", "10/mo", "∞")
            ComparisonRow("Application tracker", "10", "∞", "∞")
            ComparisonRow("Salary insights", "✗", "✓", "✓")
            ComparisonRow("Negotiation coach", "✗", "✗", "✓")
            ComparisonRow("LinkedIn optimizer", "✗", "✗", "✓")
            ComparisonRow("Priority support", "✗", "✗", "✓")
        }
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    free: String,
    pro: String,
    premium: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = free,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = if (free == "✗") MaterialTheme.colorScheme.onSurfaceVariant 
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = pro,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = if (pro == "✓") MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = premium,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = if (premium == "✓" || premium == "∞") MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (premium == "∞") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
