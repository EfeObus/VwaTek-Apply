package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vwatek.apply.data.api.SalaryInsightsResponse
import com.vwatek.apply.data.api.OfferEvaluationResponse
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.domain.usecase.SalaryInsightsState
import com.vwatek.apply.domain.usecase.SalaryIntelligenceManager
import com.vwatek.apply.domain.usecase.SubscriptionManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Salary Insights Screen - Premium Feature
 * Phase 4: Premium & Monetization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryInsightsScreen(
    onNavigateBack: () -> Unit = {},
    onShowPaywall: (PremiumFeature) -> Unit = {}
) {
    val salaryManager: SalaryIntelligenceManager = koinInject()
    val subscriptionManager: SubscriptionManager = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val insightsState by salaryManager.insightsState.collectAsState()
    
    // Search form state
    var jobTitle by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var yearsExperience by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    
    // Check feature access
    val hasAccess = subscriptionManager.canUseFeature(PremiumFeature.SALARY_INSIGHTS)
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Salary Insights") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!hasAccess) {
            // Show upgrade prompt
            FeatureGatedContent(
                feature = PremiumFeature.SALARY_INSIGHTS,
                subscriptionManager = subscriptionManager,
                onUpgradeClick = { onShowPaywall(PremiumFeature.SALARY_INSIGHTS) },
                modifier = Modifier.padding(padding)
            ) { }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Search Form
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Search Salary Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title") },
                            placeholder = { Text("e.g., Software Engineer") },
                            leadingIcon = { Icon(Icons.Default.Work, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            placeholder = { Text("e.g., Toronto, ON") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = yearsExperience,
                            onValueChange = { yearsExperience = it },
                            label = { Text("Years of Experience") },
                            placeholder = { Text("e.g., 5") },
                            leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = skills,
                            onValueChange = { skills = it },
                            label = { Text("Key Skills (comma separated)") },
                            placeholder = { Text("e.g., Kotlin, Android, AWS") },
                            leadingIcon = { Icon(Icons.Default.Code, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    salaryManager.searchSalary(
                                        jobTitle = jobTitle,
                                        location = location,
                                        yearsExperience = yearsExperience.toIntOrNull() ?: 0,
                                        skills = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    )
                                }
                            },
                            enabled = jobTitle.isNotBlank() && location.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Salary Insights")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Results
                when (val state = insightsState) {
                    is SalaryInsightsState.Idle -> {
                        EmptyStateMessage(
                            icon = Icons.Default.AttachMoney,
                            title = "Search for Salary Data",
                            message = "Enter a job title and location to see salary insights for your target role"
                        )
                    }
                    is SalaryInsightsState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is SalaryInsightsState.Success -> {
                        SalaryInsightsResults(insights = state.insights)
                    }
                    is SalaryInsightsState.OfferEvaluated -> {
                        OfferEvaluationResults(evaluation = state.evaluation)
                    }
                    is SalaryInsightsState.Error -> {
                        ErrorMessage(message = state.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryInsightsResults(insights: SalaryInsightsResponse) {
    Column {
        // Salary Range Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = insights.jobTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = insights.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Salary range visualization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SalaryDataPoint(label = "Min", value = insights.minSalary)
                    SalaryDataPoint(label = "Median", value = insights.medianSalary, highlight = true)
                    SalaryDataPoint(label = "Max", value = insights.maxSalary)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Salary bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    val medianPosition = (insights.medianSalary - insights.minSalary).toFloat() / 
                                         (insights.maxSalary - insights.minSalary).toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(medianPosition)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Market Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                StatRow(label = "Data Points", value = "${insights.dataPoints} salaries")
                StatRow(label = "Experience", value = "${insights.yearsExperience} years")
                StatRow(label = "Confidence", value = "${insights.confidence}%")
                
                if (insights.percentile != null) {
                    StatRow(label = "Your Position", value = "${insights.percentile}th percentile")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Insights
        if (insights.insights.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Key Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    insights.insights.forEach { insight ->
                        InsightItem(text = insight)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recommendations
        if (insights.recommendations.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    insights.recommendations.forEach { recommendation ->
                        RecommendationItem(text = recommendation)
                    }
                }
            }
        }
    }
}

@Composable
private fun SalaryDataPoint(
    label: String,
    value: Long,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = formatSalary(value),
            style = if (highlight) MaterialTheme.typography.headlineMedium 
                    else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InsightItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RecommendationItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.TipsAndUpdates,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OfferEvaluationResults(evaluation: OfferEvaluationResponse) {
    Column {
        // Overall score
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    evaluation.overallScore >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    evaluation.overallScore >= 60 -> Color(0xFFFFC107).copy(alpha = 0.1f)
                    else -> Color(0xFFF44336).copy(alpha = 0.1f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Offer Score",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${evaluation.overallScore}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        evaluation.overallScore >= 80 -> Color(0xFF4CAF50)
                        evaluation.overallScore >= 60 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = evaluation.recommendation,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Comparison to market
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Market Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                StatRow(label = "Your Offer", value = formatSalary(evaluation.offeredSalary))
                StatRow(label = "Market Median", value = formatSalary(evaluation.marketMedian))
                StatRow(label = "Difference", value = "${if (evaluation.percentageDiff >= 0) "+" else ""}${evaluation.percentageDiff}%")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Negotiation opportunities
        if (evaluation.negotiationOpportunities.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Negotiation Opportunities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    evaluation.negotiationOpportunities.forEach { opportunity ->
                        NegotiationOpportunityItem(opportunity = opportunity)
                    }
                }
            }
        }
    }
}

@Composable
private fun NegotiationOpportunityItem(opportunity: NegotiationOpportunityDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = opportunity.area.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when (opportunity.priority) {
                        "HIGH" -> MaterialTheme.colorScheme.error
                        "MEDIUM" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    }.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = opportunity.priority,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = opportunity.suggestion,
                style = MaterialTheme.typography.bodySmall
            )
            if (opportunity.potentialValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Potential value: ${formatSalary(opportunity.potentialValue)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// Helper data class for negotiation opportunity
data class NegotiationOpportunityDto(
    val area: String,
    val suggestion: String,
    val priority: String,
    val potentialValue: Long? = null
)

private fun formatSalary(amount: Long): String {
    return "$${"%,.0f".format(amount.toDouble())}"
}
