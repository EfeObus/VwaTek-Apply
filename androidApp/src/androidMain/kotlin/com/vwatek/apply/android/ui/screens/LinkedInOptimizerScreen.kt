package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.usecase.*
import kotlinx.coroutines.launch

/**
 * Phase 12: LinkedIn Optimizer Screen for Android
 * Uses LinkedInOptimizerManager from shared module to analyze and optimize LinkedIn profiles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInOptimizerScreen() {
    val manager: LinkedInOptimizerManager = org.koin.compose.koinInject()
    val profileState by manager.profileState.collectAsState()
    val optimizationState by manager.optimizationState.collectAsState()
    val historyState by manager.historyState.collectAsState()
    val scope = rememberCoroutineScope()

    var profileUrl by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("") }
    var targetIndustry by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Analyze", "Optimize", "History")
    val snackbarHostState = remember { SnackbarHostState() }

    // Show Snackbar on error states
    val errorMessage = when (profileState) {
        is LinkedInProfileState.Error -> (profileState as LinkedInProfileState.Error).message
        else -> null
    } ?: when (optimizationState) {
        is OptimizationState.Error -> (optimizationState as OptimizationState.Error).message
        else -> null
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) {
        manager.loadHistory()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("LinkedIn Optimizer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A66C2),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AnalyzeTab(
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
                1 -> OptimizeTab(
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
                2 -> HistoryTab(historyState = historyState, onRefresh = {
                    scope.launch { manager.loadHistory() }
                })
            }
        }
    }
}

@Composable
private fun AnalyzeTab(
    profileUrl: String,
    onProfileUrlChange: (String) -> Unit,
    profileState: LinkedInProfileState,
    onAnalyze: () -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Analyze Your Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Enter your LinkedIn profile URL to get AI-powered analysis and recommendations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = profileUrl,
                        onValueChange = onProfileUrlChange,
                        label = { Text("LinkedIn Profile URL") },
                        placeholder = { Text("https://linkedin.com/in/yourprofile") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = profileState !is LinkedInProfileState.Analyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A66C2))
                    ) {
                        if (profileState is LinkedInProfileState.Analyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (profileState is LinkedInProfileState.Analyzing) "Analyzing..." else "Analyze Profile")
                    }
                }
            }
        }

        // Results
        when (profileState) {
            is LinkedInProfileState.Analyzed -> {
                val result = (profileState as LinkedInProfileState.Analyzed).result
                val analysis = result.analysis

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Overall Score", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(20.dp), color = scoreColor(analysis.overallScore)) {
                                    Text(
                                        "${analysis.overallScore}/100",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Section Scores", style = MaterialTheme.typography.titleMedium)
                            ScoreRow("Headline", analysis.sectionScores.headline)
                            ScoreRow("Summary", analysis.sectionScores.summary)
                            ScoreRow("Experience", analysis.sectionScores.experience)
                            ScoreRow("Education", analysis.sectionScores.education)
                            ScoreRow("Skills", analysis.sectionScores.skills)
                            ScoreRow("Completeness", analysis.sectionScores.completeness)
                        }
                    }
                }

                if (analysis.strengths.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Strengths", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50))
                                analysis.strengths.forEach { strength ->
                                    Row {
                                        Text("✅ ", style = MaterialTheme.typography.bodyMedium)
                                        Text(strength, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                if (analysis.improvements.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Improvements", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF44336))
                                analysis.improvements.forEach { improvement ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("⚠️ ${improvement.issue}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(improvement.suggestion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear Analysis")
                    }
                }
            }
            is LinkedInProfileState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            (profileState as LinkedInProfileState.Error).message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            else -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Paste your LinkedIn profile URL above to analyze",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptimizeTab(
    profileState: LinkedInProfileState,
    optimizationState: OptimizationState,
    targetRole: String,
    onTargetRoleChange: (String) -> Unit,
    targetIndustry: String,
    onTargetIndustryChange: (String) -> Unit,
    onOptimize: (String) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (profileState !is LinkedInProfileState.Analyzed) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Text("Analyze your profile first", style = MaterialTheme.typography.titleMedium)
                        Text("Go to the Analyze tab to analyze your LinkedIn profile before optimizing.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            val result = (profileState as LinkedInProfileState.Analyzed).result

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Optimize Content", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = targetRole,
                            onValueChange = onTargetRoleChange,
                            label = { Text("Target Role (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetIndustry,
                            onValueChange = onTargetIndustryChange,
                            label = { Text("Target Industry (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = { onOptimize(result.profileId) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = optimizationState !is OptimizationState.Generating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A66C2))
                        ) {
                            if (optimizationState is OptimizationState.Generating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (optimizationState is OptimizationState.Generating) "Generating..." else "Generate Optimizations")
                        }
                    }
                }
            }

            when (optimizationState) {
                is OptimizationState.Success -> {
                    val content = (optimizationState as OptimizationState.Success).content
                    content.headline?.let { headline ->
                        item {
                            OptimizationCard("Optimized Headline", headline)
                        }
                    }
                    if (content.headlineAlternatives.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Alternative Headlines", style = MaterialTheme.typography.titleMedium)
                                    content.headlineAlternatives.forEachIndexed { index, alt ->
                                        Text("${index + 1}. $alt", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    content.summary?.let { summary ->
                        item {
                            OptimizationCard("Optimized Summary", summary)
                        }
                    }
                    if (content.skillSuggestions.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Suggested Skills", style = MaterialTheme.typography.titleMedium)
                                    content.skillSuggestions.forEach { skill ->
                                        Row {
                                            Text("• ", fontWeight = FontWeight.Bold)
                                            Text(skill, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                            Text("Clear Optimizations")
                        }
                    }
                }
                is OptimizationState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                (optimizationState as OptimizationState.Error).message,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                else -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Analyze your profile first, then optimize",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(historyState: LinkedInHistoryState, onRefresh: () -> Unit) {
    when (historyState) {
        is LinkedInHistoryState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LinkedInHistoryState.Success -> {
            val history = (historyState as LinkedInHistoryState.Success).history
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No analysis history yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onRefresh) { Text("Refresh") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { result ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        result.profile.headline ?: "LinkedIn Profile",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(shape = RoundedCornerShape(12.dp), color = scoreColor(result.analysis.overallScore)) {
                                        Text(
                                            "${result.analysis.overallScore}",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Analyzed: ${result.analyzedAt.take(10)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        is LinkedInHistoryState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        (historyState as LinkedInHistoryState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onRefresh) { Text("Retry") }
                }
            }
        }
    }
}

// Helper composables
@Composable
private fun ScoreRow(label: String, score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.weight(1f).height(8.dp),
            color = scoreColor(score),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("$score", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OptimizationCard(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0A66C2))
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 60 -> Color(0xFFFFC107)
    score >= 40 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}
