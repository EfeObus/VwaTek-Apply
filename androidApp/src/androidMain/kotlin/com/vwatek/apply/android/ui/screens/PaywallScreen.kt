package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.PremiumFeature
import com.vwatek.apply.domain.model.SubscriptionTier
import com.vwatek.apply.domain.model.BillingPeriod
import com.vwatek.apply.domain.model.SubscriptionPricing
import com.vwatek.apply.domain.usecase.SubscriptionManager

/**
 * Paywall Screen - Shows when a user tries to access a premium feature
 * Phase 4: Premium & Monetization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    feature: PremiumFeature,
    requiredTier: SubscriptionTier,
    onDismiss: () -> Unit,
    onUpgrade: (tier: SubscriptionTier, billingPeriod: BillingPeriod) -> Unit
) {
    var selectedBillingPeriod by remember { mutableStateOf(BillingPeriod.YEARLY) }
    
    val featureInfo = getFeatureInfo(feature)
    val pricing = SubscriptionPricing.forTier(requiredTier)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = featureInfo.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Feature name
            Text(
                text = featureInfo.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Feature description
            Text(
                text = featureInfo.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Unlock badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Requires ${requiredTier.name} plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Billing period toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PaywallBillingOption(
                    label = "Monthly",
                    price = "$${pricing.monthlyPrice}/mo",
                    isSelected = selectedBillingPeriod == BillingPeriod.MONTHLY,
                    onClick = { selectedBillingPeriod = BillingPeriod.MONTHLY },
                    modifier = Modifier.weight(1f)
                )
                PaywallBillingOption(
                    label = "Yearly",
                    price = "$${pricing.yearlyPrice / 12}/mo",
                    savings = "Save 17%",
                    isSelected = selectedBillingPeriod == BillingPeriod.YEARLY,
                    onClick = { selectedBillingPeriod = BillingPeriod.YEARLY },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Benefits list
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                featureInfo.benefits.forEach { benefit ->
                    PaywallBenefitItem(text = benefit)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // CTA Button
            Button(
                onClick = { onUpgrade(requiredTier, selectedBillingPeriod) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Upgrade to ${requiredTier.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Secondary button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Maybe Later")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Fine print
            Text(
                text = "30-day money-back guarantee • Cancel anytime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaywallBillingOption(
    label: String,
    price: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    savings: String? = null
) {
    Surface(
        modifier = modifier
            .padding(4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurface
            )
            if (savings != null) {
                Text(
                    text = savings,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PaywallBenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
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

/**
 * Inline paywall banner for displaying within screens
 */
@Composable
fun PaywallBanner(
    feature: PremiumFeature,
    requiredTier: SubscriptionTier,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featureInfo = getFeatureInfo(feature)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = featureInfo.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Upgrade to ${requiredTier.name} to unlock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = onUpgradeClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Upgrade",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Feature gated content wrapper
 */
@Composable
fun FeatureGatedContent(
    feature: PremiumFeature,
    subscriptionManager: SubscriptionManager,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hasAccess = subscriptionManager.canUseFeature(feature)
    val requiredTier = subscriptionManager.getRequiredTierForFeature(feature)
    
    if (hasAccess) {
        content()
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val featureInfo = getFeatureInfo(feature)
            
            Icon(
                imageVector = featureInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = featureInfo.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = featureInfo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onUpgradeClick) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upgrade to ${requiredTier.name}")
            }
        }
    }
}

// Feature information helper
private data class FeatureInfo(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val benefits: List<String>
)

private fun getFeatureInfo(feature: PremiumFeature): FeatureInfo {
    return when (feature) {
        PremiumFeature.SALARY_INSIGHTS -> FeatureInfo(
            title = "Salary Insights",
            description = "Get real-time salary data and market comparisons for your target roles",
            icon = Icons.Default.AttachMoney,
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
            icon = Icons.Default.Psychology,
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
            icon = Icons.Default.Person,
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
            icon = Icons.Default.Work,
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
            icon = Icons.Default.AutoAwesome,
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
            icon = Icons.Default.Description,
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
            icon = Icons.Default.Mail,
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
            icon = Icons.Default.RecordVoiceOver,
            benefits = listOf(
                "Unlimited practice sessions",
                "Industry-specific questions",
                "Detailed feedback",
                "Progress tracking"
            )
        )
    }
}
