package com.vwatek.apply.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ATSAnalysis
import com.vwatek.apply.domain.model.ATSIssue
import com.vwatek.apply.domain.model.ATSRecommendation
import com.vwatek.apply.domain.model.IssueSeverity
import com.vwatek.apply.domain.model.GrammarIssue
import com.vwatek.apply.domain.model.GrammarIssueType
import com.vwatek.apply.domain.model.ImpactBullet
import com.vwatek.apply.domain.usecase.SectionRewriteResult
import com.vwatek.apply.presentation.resume.ResumeIntent
import com.vwatek.apply.presentation.resume.ResumeViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OptimizerScreen() {
    val viewModel: ResumeViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Tab state: 0 = ATS Score, 1 = Section Rewriter, 2 = Grammar, 3 = Impact Bullets
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("ATS Score", "Section Rewriter", "Grammar", "Impact Bullets")
    
    var selectedResume by remember { mutableStateOf<Resume?>(null) }
    var jobDescription by remember { mutableStateOf("") }
    var targetKeywords by remember { mutableStateOf("") }
    
    // Section Rewriter state
    var sectionType by remember { mutableStateOf("Summary") }
    var sectionContent by remember { mutableStateOf("") }
    var writingStyle by remember { mutableStateOf("professional") }
    var targetRole by remember { mutableStateOf("") }
    var targetIndustry by remember { mutableStateOf("") }
    
    // Grammar state
    var grammarText by remember { mutableStateOf("") }
    
    // Impact Bullets state
    var experienceText by remember { mutableStateOf("") }
    var jobContext by remember { mutableStateOf("") }
    
    // Show error snackbar when error occurs
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.onIntent(ResumeIntent.ClearError)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resume Optimizer") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Star, contentDescription = null)
                                1 -> Icon(Icons.Default.Edit, contentDescription = null)
                                2 -> Icon(Icons.Default.CheckCircle, contentDescription = null)
                                3 -> Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            }
                        }
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> ATSScoreContent(
                    viewModel = viewModel,
                    state = state,
                    selectedResume = selectedResume,
                    onResumeSelected = { selectedResume = it },
                    jobDescription = jobDescription,
                    onJobDescriptionChange = { jobDescription = it },
                    targetKeywords = targetKeywords,
                    onTargetKeywordsChange = { targetKeywords = it },
                    context = context
                )
                1 -> SectionRewriterContent(
                    viewModel = viewModel,
                    state = state,
                    sectionType = sectionType,
                    onSectionTypeChange = { sectionType = it },
                    sectionContent = sectionContent,
                    onSectionContentChange = { sectionContent = it },
                    writingStyle = writingStyle,
                    onWritingStyleChange = { writingStyle = it },
                    targetRole = targetRole,
                    onTargetRoleChange = { targetRole = it },
                    targetIndustry = targetIndustry,
                    onTargetIndustryChange = { targetIndustry = it },
                    context = context
                )
                2 -> GrammarCheckContent(
                    viewModel = viewModel,
                    state = state,
                    grammarText = grammarText,
                    onGrammarTextChange = { grammarText = it },
                    context = context
                )
                3 -> ImpactBulletsContent(
                    viewModel = viewModel,
                    state = state,
                    experienceText = experienceText,
                    onExperienceTextChange = { experienceText = it },
                    jobContext = jobContext,
                    onJobContextChange = { jobContext = it },
                    context = context
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ATSScoreContent(
    viewModel: ResumeViewModel,
    state: com.vwatek.apply.presentation.resume.ResumeState,
    selectedResume: Resume?,
    onResumeSelected: (Resume?) -> Unit,
    jobDescription: String,
    onJobDescriptionChange: (String) -> Unit,
    targetKeywords: String,
    onTargetKeywordsChange: (String) -> Unit,
    context: Context
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step 1: Select Resume
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("1", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Select Resume", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (state.resumes.isEmpty()) {
                        Text(
                            text = "No resumes available. Create one first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedResume?.name ?: "Select a resume",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                state.resumes.forEach { resume ->
                                    DropdownMenuItem(
                                        text = { Text(resume.name) },
                                        onClick = {
                                            onResumeSelected(resume)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Step 2: Enter Job Description
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("2", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Enter Job Description", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = onJobDescriptionChange,
                        label = { Text("Job Description") },
                        placeholder = { Text("Paste the job description here...") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Step 3: Target Keywords (NEW)
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("3", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Target Keywords (Optional)", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = targetKeywords,
                        onValueChange = onTargetKeywordsChange,
                        label = { Text("Target Keywords") },
                        placeholder = { Text("leadership, project management, agile... (comma separated)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Add specific keywords you want to target for better analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Analyze Button
        item {
            Button(
                onClick = {
                    if (selectedResume != null) {
                        viewModel.onIntent(ResumeIntent.PerformATSAnalysis(selectedResume!!, jobDescription.ifBlank { null }))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedResume != null && !state.isATSAnalyzing
            ) {
                if (state.isATSAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.isATSAnalyzing) "Analyzing..." else "Analyze ATS Score")
            }
        }
        
        // ATS Analysis Result (Enhanced)
        state.atsAnalysis?.let { analysis ->
            // Score Overview Card
            item {
                ATSScoreOverviewCard(analysis = analysis)
            }
            
            // Score Breakdown Card
            item {
                ATSScoreBreakdownCard(analysis = analysis)
            }
            
            // Formatting Issues Card
            if (analysis.formattingIssues.isNotEmpty()) {
                item {
                    ATSIssuesCard(
                        title = "Formatting Issues",
                        issues = analysis.formattingIssues,
                        icon = Icons.Default.Warning
                    )
                }
            }
            
            // Structure Issues Card
            if (analysis.structureIssues.isNotEmpty()) {
                item {
                    ATSIssuesCard(
                        title = "Structure Issues",
                        issues = analysis.structureIssues,
                        icon = Icons.Default.Info
                    )
                }
            }
            
            // Keyword Density Card
            if (analysis.keywordDensity.isNotEmpty()) {
                item {
                    KeywordDensityCard(
                        keywordDensity = analysis.keywordDensity,
                        targetKeywords = targetKeywords
                    )
                }
            }
            
            // Recommendations Card
            if (analysis.recommendations.isNotEmpty()) {
                item {
                    ATSRecommendationsCard(recommendations = analysis.recommendations)
                }
            }
        }
        
        // Legacy Analysis Result (keep for backward compatibility)
        state.analysis?.let { analysis ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Analysis Results",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Match Score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Match Score")
                            Text(
                                text = "${analysis.matchScore}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = when {
                                    analysis.matchScore >= 80 -> MaterialTheme.colorScheme.primary
                                    analysis.matchScore >= 60 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Missing Keywords
                        if (analysis.missingKeywords.isNotEmpty()) {
                            Text(
                                text = "Missing Keywords",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                analysis.missingKeywords.forEach { keyword ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(keyword) }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Recommendations
                        if (analysis.recommendations.isNotEmpty()) {
                            Text(
                                text = "Recommendations",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            analysis.recommendations.forEach { rec ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Tips Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Optimization Tips",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Use keywords from the job description\n" +
                                "• Quantify your achievements with numbers\n" +
                                "• Tailor your summary for each application\n" +
                                "• Keep formatting ATS-friendly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ATSScoreOverviewCard(analysis: ATSAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                analysis.overallScore >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                analysis.overallScore >= 60 -> Color(0xFFFFC107).copy(alpha = 0.1f)
                else -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ATS Score",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${analysis.overallScore}",
                style = MaterialTheme.typography.displayLarge,
                color = when {
                    analysis.overallScore >= 80 -> Color(0xFF4CAF50)
                    analysis.overallScore >= 60 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
            
            Text(
                text = when {
                    analysis.overallScore >= 80 -> "Excellent! Your resume is well-optimized."
                    analysis.overallScore >= 60 -> "Good, but there's room for improvement."
                    else -> "Needs work. Review the suggestions below."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ATSScoreBreakdownCard(analysis: ATSAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Score Breakdown",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ScoreRow(label = "Formatting", score = analysis.formattingScore)
            ScoreRow(label = "Keywords", score = analysis.keywordScore)
            ScoreRow(label = "Structure", score = analysis.structureScore)
            ScoreRow(label = "Readability", score = analysis.readabilityScore)
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .width(120.dp)
                    .height(8.dp),
                color = when {
                    score >= 80 -> Color(0xFF4CAF50)
                    score >= 60 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$score%",
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    score >= 80 -> Color(0xFF4CAF50)
                    score >= 60 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        }
    }
}

@Composable
private fun ATSIssuesCard(
    title: String,
    issues: List<ATSIssue>,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            issues.forEach { issue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = when (issue.severity) {
                            IssueSeverity.HIGH -> Icons.Default.Warning
                            IssueSeverity.MEDIUM -> Icons.Default.Info
                            IssueSeverity.LOW -> Icons.Default.Check
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (issue.severity) {
                            IssueSeverity.HIGH -> Color(0xFFF44336)
                            IssueSeverity.MEDIUM -> Color(0xFFFFC107)
                            IssueSeverity.LOW -> Color(0xFF4CAF50)
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = issue.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = issue.suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordDensityCard(
    keywordDensity: Map<String, Int>,
    targetKeywords: String
) {
    val targetList = targetKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keyword Density",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keywordDensity.entries.sortedByDescending { it.value }.forEach { (keyword, count) ->
                    val isTargeted = targetList.contains(keyword.lowercase())
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text("$keyword ($count)") 
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isTargeted) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        leadingIcon = if (isTargeted) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
            
            // Show missing target keywords
            if (targetList.isNotEmpty()) {
                val missingKeywords = targetList.filter { target ->
                    keywordDensity.keys.none { it.lowercase() == target }
                }
                
                if (missingKeywords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Missing Target Keywords:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        missingKeywords.forEach { keyword ->
                            AssistChip(
                                onClick = { },
                                label = { Text(keyword) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFF44336)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ATSRecommendationsCard(recommendations: List<ATSRecommendation>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.sortedBy { it.priority }.forEach { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                text = rec.title,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (rec.priority) {
                                    1 -> Color(0xFFF44336)
                                    2 -> Color(0xFFFFC107)
                                    else -> Color(0xFF4CAF50)
                                }
                            ) {
                                Text(
                                    text = "P${rec.priority}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rec.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Impact: ${rec.impact}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SectionRewriterContent(
    viewModel: ResumeViewModel,
    state: com.vwatek.apply.presentation.resume.ResumeState,
    sectionType: String,
    onSectionTypeChange: (String) -> Unit,
    sectionContent: String,
    onSectionContentChange: (String) -> Unit,
    writingStyle: String,
    onWritingStyleChange: (String) -> Unit,
    targetRole: String,
    onTargetRoleChange: (String) -> Unit,
    targetIndustry: String,
    onTargetIndustryChange: (String) -> Unit,
    context: Context
) {
    val sectionTypes = listOf("Summary", "Experience", "Skills", "Education")
    val writingStyles = listOf(
        "professional" to "Professional",
        "confident" to "Confident",
        "results-driven" to "Results-Driven",
        "innovative" to "Innovative"
    )
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Type Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("1", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Select Section Type", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = sectionType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            sectionTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        onSectionTypeChange(type)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Section Content Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("2", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Enter Section Content", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = sectionContent,
                        onValueChange = onSectionContentChange,
                        label = { Text("Section Content") },
                        placeholder = { 
                            Text(
                                when (sectionType) {
                                    "Summary" -> "Enter your professional summary..."
                                    "Experience" -> "Enter your work experience bullet points..."
                                    "Skills" -> "Enter your skills section..."
                                    "Education" -> "Enter your education details..."
                                    else -> "Enter section content..."
                                }
                            ) 
                        },
                        minLines = 6,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Writing Style Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("3", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Writing Style", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        writingStyles.forEach { (value, label) ->
                            FilterChip(
                                onClick = { onWritingStyleChange(value) },
                                label = { Text(label) },
                                selected = writingStyle == value,
                                leadingIcon = if (writingStyle == value) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }
        
        // Target Role & Industry
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("4", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Target Context (Optional)", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = targetRole,
                        onValueChange = onTargetRoleChange,
                        label = { Text("Target Role") },
                        placeholder = { Text("e.g., Senior Software Engineer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = targetIndustry,
                        onValueChange = onTargetIndustryChange,
                        label = { Text("Target Industry") },
                        placeholder = { Text("e.g., Technology, Healthcare, Finance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Rewrite Button
        item {
            Button(
                onClick = {
                    if (sectionContent.isNotBlank()) {
                        viewModel.onIntent(
                            ResumeIntent.RewriteSection(
                                sectionType = sectionType.lowercase(),
                                sectionContent = sectionContent,
                                targetRole = targetRole.ifBlank { null },
                                targetIndustry = targetIndustry.ifBlank { null },
                                style = writingStyle
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = sectionContent.isNotBlank() && !state.isRewritingSection
            ) {
                if (state.isRewritingSection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.isRewritingSection) "Rewriting..." else "Rewrite Section")
            }
        }
        
        // Rewrite Result
        state.sectionRewriteResult?.let { result ->
            item {
                SectionRewriteResultCard(
                    result = result,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Rewritten Section", result.rewrittenContent)
                        clipboard.setPrimaryClip(clip)
                    },
                    onClear = {
                        viewModel.onIntent(ResumeIntent.ClearSectionRewrite)
                    }
                )
            }
        }
        
        // Tips Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✍️ Writing Tips",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Use action verbs to describe accomplishments\n" +
                                "• Quantify results whenever possible\n" +
                                "• Match the tone to your target industry\n" +
                                "• Keep content concise and impactful",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionRewriteResultCard(
    result: SectionRewriteResult,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rewritten Content",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Rewritten content
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = result.rewrittenContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            // Changes made
            if (result.changes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Changes Made:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.changes.forEach { change ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Keywords added
            if (result.keywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Keywords Added:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.keywords.forEach { keyword ->
                        AssistChip(
                            onClick = { },
                            label = { Text(keyword) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
            
            // Tips
            if (result.tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tips:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.tips.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFC107)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ========== Grammar Check Content ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrammarCheckContent(
    viewModel: ResumeViewModel,
    state: com.vwatek.apply.presentation.resume.ResumeState,
    grammarText: String,
    onGrammarTextChange: (String) -> Unit,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF5C6BC0).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF5C6BC0))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grammar & Tone Check", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C6BC0))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paste text from your resume to check for grammar, spelling, tone, and clarity issues.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            OutlinedTextField(
                value = grammarText,
                onValueChange = onGrammarTextChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                label = { Text("Text to Check") },
                placeholder = { Text("Paste resume text here...") }
            )
        }
        item {
            Button(
                onClick = { viewModel.onIntent(ResumeIntent.AnalyzeGrammar(grammarText)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = grammarText.isNotBlank() && !state.isAnalyzingGrammar
            ) {
                if (state.isAnalyzingGrammar) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Grammar")
                }
            }
        }
        if (state.grammarIssues.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Issues Found (${state.grammarIssues.size})", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.onIntent(ResumeIntent.ClearGrammarIssues) }) { Text("Clear") }
                }
            }
            items(state.grammarIssues) { issue -> GrammarIssueCard(issue = issue) }
        }
    }
}

@Composable
private fun GrammarIssueCard(issue: GrammarIssue) {
    val typeColor = when (issue.type) {
        GrammarIssueType.GRAMMAR -> Color(0xFFF44336)
        GrammarIssueType.SPELLING -> Color(0xFFFF9800)
        GrammarIssueType.TONE -> Color(0xFF9C27B0)
        GrammarIssueType.CLARITY -> Color(0xFF2196F3)
        GrammarIssueType.REDUNDANCY -> Color.Gray
    }
    val typeLabel = when (issue.type) {
        GrammarIssueType.GRAMMAR -> "Grammar"
        GrammarIssueType.SPELLING -> "Spelling"
        GrammarIssueType.TONE -> "Tone"
        GrammarIssueType.CLARITY -> "Clarity"
        GrammarIssueType.REDUNDANCY -> "Redundancy"
    }
    @OptIn(ExperimentalMaterial3Api::class)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssistChip(onClick = { }, label = { Text(typeLabel) }, colors = AssistChipDefaults.assistChipColors(containerColor = typeColor.copy(alpha = 0.15f), labelColor = typeColor))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF44336))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = issue.original, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = issue.corrected, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = issue.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ========== Impact Bullets Content ==========

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ImpactBulletsContent(
    viewModel: ResumeViewModel,
    state: com.vwatek.apply.presentation.resume.ResumeState,
    experienceText: String,
    onExperienceTextChange: (String) -> Unit,
    jobContext: String,
    onJobContextChange: (String) -> Unit,
    context: Context
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Impact Bullets Generator", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF9800))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Transform plain experience descriptions into powerful XYZ-format impact bullets.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            OutlinedTextField(value = experienceText, onValueChange = onExperienceTextChange, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), label = { Text("Experience Descriptions") }, placeholder = { Text("Enter one experience per line...") })
        }
        item {
            OutlinedTextField(value = jobContext, onValueChange = onJobContextChange, modifier = Modifier.fillMaxWidth(), label = { Text("Job Context (Optional)") }, placeholder = { Text("e.g., Senior Software Engineer at a fintech startup") })
        }
        item {
            val experiences = experienceText.lines().filter { it.isNotBlank() }
            Button(
                onClick = { viewModel.onIntent(ResumeIntent.GenerateImpactBullets(experiences, jobContext)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = experiences.isNotEmpty() && !state.isGeneratingBullets,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                if (state.isGeneratingBullets) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Impact Bullets")
                }
            }
        }
        if (state.impactBullets.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Generated Bullets (${state.impactBullets.size})", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.onIntent(ResumeIntent.ClearImpactBullets) }) { Text("Clear") }
                }
            }
            items(state.impactBullets) { bullet -> ImpactBulletCard(bullet = bullet, clipboardManager = clipboardManager) }
        }
    }
}

@Composable
private fun ImpactBulletCard(bullet: ImpactBullet, clipboardManager: ClipboardManager) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("Before:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = bullet.original, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("After:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    Text(text = bullet.improved, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                }
                IconButton(onClick = {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Impact Bullet", bullet.improved))
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                }
            }
            bullet.xyzFormat?.let { xyz ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("XYZ Breakdown:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.height(4.dp))
                        if (xyz.accomplished.isNotBlank()) { Text("X (Accomplished): ${xyz.accomplished}", style = MaterialTheme.typography.bodySmall) }
                        if (xyz.measuredBy.isNotBlank()) { Text("Y (Measured by): ${xyz.measuredBy}", style = MaterialTheme.typography.bodySmall) }
                        if (xyz.byDoing.isNotBlank()) { Text("Z (By doing): ${xyz.byDoing}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
