package com.vwatek.apply.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.jobbank.*
import com.vwatek.apply.i18n.*
import org.koin.compose.koinInject

/**
 * Phase 3: Job Bank Canada Search Screen
 * Allows users to search and explore Job Bank Canada job listings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobBankScreen(
    onNavigateBack: () -> Unit = {},
    onSaveJob: (JobBankJob) -> Unit = {}
) {
    val viewModel: JobBankViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val strings = LocaleManager.strings
    
    var searchQuery by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showJobDetails by remember { mutableStateOf(false) }
    var selectedProvinceCode by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.searchError) {
        state.searchError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.jobBankTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Language toggle
                    IconButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(
                            text = if (state.currentLocale == Locale.ENGLISH) "FR" else "EN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Filter button
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (state.hasActiveFilters) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                location = locationQuery,
                onLocationChange = { locationQuery = it },
                onSearch = { 
                    viewModel.searchJobs(
                        query = searchQuery.ifBlank { null },
                        location = locationQuery.ifBlank { null },
                        provinceCode = selectedProvinceCode
                    )
                },
                isSearching = state.isSearching
            )
            
            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Loading indicator
                if (state.isSearching && state.searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                // Results count
                if (state.hasSearchResults) {
                    item {
                        Text(
                            text = "${state.totalResults} jobs found",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Search results
                items(state.searchResults) { job ->
                    JobCard(
                        job = job,
                        onClick = {
                            viewModel.loadJobDetails(job.id)
                            showJobDetails = true
                        },
                        onSave = { onSaveJob(job) }
                    )
                }
                
                // Load more
                if (state.hasMoreResults && !state.isSearching) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                TextButton(onClick = { viewModel.loadMoreResults() }) {
                                    Text("Load more")
                                }
                            }
                        }
                    }
                }
                
                // Trending Jobs (when no search)
                if (state.showTrending) {
                    item {
                        Text(
                            text = "Trending Jobs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    if (state.isLoadingTrending) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    
                    items(state.trendingJobs) { job ->
                        JobCard(
                            job = job,
                            onClick = {
                                viewModel.loadJobDetails(job.id)
                                showJobDetails = true
                            },
                            onSave = { onSaveJob(job) }
                        )
                    }
                }
                
                // Empty state
                if (!state.isSearching && !state.hasSearchResults && !state.showTrending) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Work,
                            title = "Search Job Bank Canada",
                            message = "Enter a job title, keyword, or location to search thousands of job listings from the Government of Canada Job Bank."
                        )
                    }
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            FilterSheet(
                provinces = state.provinces,
                selectedProvinceCode = selectedProvinceCode,
                onProvinceSelect = { selectedProvinceCode = it },
                onApply = {
                    viewModel.searchJobs(
                        query = searchQuery.ifBlank { null },
                        location = locationQuery.ifBlank { null },
                        provinceCode = selectedProvinceCode
                    )
                    showFilterSheet = false
                },
                onClear = {
                    selectedProvinceCode = null
                    viewModel.clearSearch()
                    showFilterSheet = false
                }
            )
        }
    }
    
    // Job Details Bottom Sheet
    if (showJobDetails) {
        ModalBottomSheet(
            onDismissRequest = { 
                showJobDetails = false
                viewModel.clearSelectedJob()
            }
        ) {
            JobDetailsSheet(
                job = state.selectedJob,
                isLoading = state.isLoadingDetails,
                onSave = { state.selectedJob?.let { onSaveJob(it) } },
                onApply = { /* Open URL */ }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Job title or keyword") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("City or postal code") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true
            )
            
            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Search Jobs")
            }
        }
    }
}

@Composable
private fun JobCard(
    job: JobBankJob,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = job.employer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onSave) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = "Save job",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = job.location.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Salary
            job.salary?.let { salary ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = salary.displayRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // NOC Code
            job.nocCode?.let { noc ->
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("NOC: $noc") },
                    modifier = Modifier.height(24.dp)
                )
            }
            
            // Posted date
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Posted: ${job.postingDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun FilterSheet(
    provinces: List<CanadianProvince>,
    selectedProvinceCode: String?,
    onProvinceSelect: (String?) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Filter Jobs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Province/Territory",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(Modifier.height(8.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            provinces.forEach { province ->
                FilterChip(
                    selected = selectedProvinceCode == province.code,
                    onClick = { 
                        onProvinceSelect(
                            if (selectedProvinceCode == province.code) null else province.code
                        )
                    },
                    label = { Text(province.code) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply")
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun JobDetailsSheet(
    job: JobBankJob?,
    isLoading: Boolean,
    onSave: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (job != null) {
            // Header
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = job.employer,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Location & Salary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(job.location.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                
                job.salary?.let { salary ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(salary.displayRange, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            // NOC Code chip
            job.nocCode?.let { noc ->
                Spacer(Modifier.height(12.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("NOC Code: $noc") },
                    leadingIcon = { Icon(Icons.Default.WorkOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            // Description
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Requirements
            if (job.requirements.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Requirements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                job.requirements.forEach { req ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(req, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            // Benefits
            if (job.benefits.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Benefits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                job.benefits.forEach { benefit ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(benefit, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
                
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply")
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
