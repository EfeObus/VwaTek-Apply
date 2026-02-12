package com.vwatek.apply.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.tracker.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * Phase 2: Job Application Tracker Screen
 * Displays job applications in Kanban, List, or Calendar view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen() {
    val viewModel: TrackerViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedApplicationForDetail by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(TrackerIntent.ClearError)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Job Tracker") },
                actions = {
                    // View mode toggle
                    IconButton(onClick = {
                        val nextMode = when (state.viewMode) {
                            TrackerViewMode.KANBAN -> TrackerViewMode.LIST
                            TrackerViewMode.LIST -> TrackerViewMode.CALENDAR
                            TrackerViewMode.CALENDAR -> TrackerViewMode.KANBAN
                        }
                        viewModel.onIntent(TrackerIntent.SetViewMode(nextMode))
                    }) {
                        Icon(
                            imageVector = when (state.viewMode) {
                                TrackerViewMode.KANBAN -> Icons.Default.ViewKanban
                                TrackerViewMode.LIST -> Icons.Default.List
                                TrackerViewMode.CALENDAR -> Icons.Default.CalendarMonth
                            },
                            contentDescription = "Toggle View"
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
                    
                    // Add button
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Application")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    // Stats bar
                    state.stats?.let { stats ->
                        TrackerStatsBar(stats = stats)
                    }
                    
                    // Main content based on view mode
                    when (state.viewMode) {
                        TrackerViewMode.KANBAN -> KanbanBoard(
                            columns = state.kanbanColumns,
                            onApplicationClick = { app ->
                                selectedApplicationForDetail = app.id
                                viewModel.onIntent(TrackerIntent.SelectApplication(app.id))
                            },
                            onStatusChange = { appId, newStatus ->
                                viewModel.onIntent(TrackerIntent.MoveToStatus(appId, newStatus))
                            }
                        )
                        TrackerViewMode.LIST -> ApplicationListView(
                            applications = state.applications,
                            onApplicationClick = { app ->
                                selectedApplicationForDetail = app.id
                                viewModel.onIntent(TrackerIntent.SelectApplication(app.id))
                            }
                        )
                        TrackerViewMode.CALENDAR -> ApplicationCalendarView(
                            applications = state.applications
                        )
                    }
                }
            }
        }
    }
    
    // Add application dialog
    if (showAddDialog) {
        AddApplicationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { request ->
                viewModel.onIntent(TrackerIntent.CreateApplication(request))
                showAddDialog = false
            }
        )
    }
    
    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            currentStatus = state.filterStatus,
            currentSource = state.filterSource,
            currentProvince = state.filterProvince,
            searchQuery = state.searchQuery,
            onStatusChange = { viewModel.onIntent(TrackerIntent.SetFilterStatus(it)) },
            onSourceChange = { viewModel.onIntent(TrackerIntent.SetFilterSource(it)) },
            onProvinceChange = { viewModel.onIntent(TrackerIntent.SetFilterProvince(it)) },
            onSearchChange = { viewModel.onIntent(TrackerIntent.SetSearchQuery(it)) },
            onClearFilters = { viewModel.onIntent(TrackerIntent.ClearFilters) },
            onDismiss = { showFilterSheet = false }
        )
    }
    
    // Application detail dialog
    state.selectedApplication?.let { detail ->
        ApplicationDetailDialog(
            detail = detail,
            isLoading = state.isLoadingDetail,
            onDismiss = {
                selectedApplicationForDetail = null
                viewModel.onIntent(TrackerIntent.ClearSelectedApplication)
            },
            onStatusChange = { newStatus, notes ->
                viewModel.onIntent(TrackerIntent.UpdateStatus(detail.application.id, newStatus, notes))
            },
            onAddNote = { content, noteType ->
                viewModel.onIntent(TrackerIntent.AddNote(detail.application.id, content, noteType))
            },
            onAddReminder = { reminder ->
                viewModel.onIntent(TrackerIntent.AddReminder(detail.application.id, reminder))
            },
            onDelete = {
                viewModel.onIntent(TrackerIntent.DeleteApplication(detail.application.id))
            }
        )
    }
}

private val TrackerState.hasActiveFilters: Boolean
    get() = filterStatus != null || filterSource != null || filterProvince != null || !searchQuery.isNullOrBlank()

// ===== Stats Bar =====

@Composable
private fun TrackerStatsBar(stats: TrackerStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem("Total", stats.totalApplications.toString())
            StatItem("Applied", stats.appliedCount.toString())
            StatItem("Interviews", stats.interviewCount.toString())
            StatItem("Offers", stats.offerCount.toString())
            StatItem("Interview Rate", "${(stats.interviewRate * 100).toInt()}%")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ===== Kanban Board =====

@Composable
private fun KanbanBoard(
    columns: Map<ApplicationStatus, List<JobApplication>>,
    onApplicationClick: (JobApplication) -> Unit,
    onStatusChange: (String, ApplicationStatus) -> Unit
) {
    // Show only active statuses for Kanban (exclude terminal states)
    val activeStatuses = listOf(
        ApplicationStatus.SAVED,
        ApplicationStatus.APPLIED,
        ApplicationStatus.SCREENING,
        ApplicationStatus.PHONE_INTERVIEW,
        ApplicationStatus.TECHNICAL_INTERVIEW,
        ApplicationStatus.ONSITE_INTERVIEW,
        ApplicationStatus.FINAL_INTERVIEW,
        ApplicationStatus.OFFER_RECEIVED,
        ApplicationStatus.NEGOTIATING
    )
    
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(activeStatuses) { status ->
            KanbanColumn(
                status = status,
                applications = columns[status] ?: emptyList(),
                onApplicationClick = onApplicationClick,
                onDropReceived = { appId -> onStatusChange(appId, status) }
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    status: ApplicationStatus,
    applications: List<JobApplication>,
    onApplicationClick: (JobApplication) -> Unit,
    onDropReceived: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Column header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(android.graphics.Color.parseColor(status.color)).copy(alpha = 0.2f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = Color(android.graphics.Color.parseColor(status.color))
                ) {
                    Text(applications.size.toString())
                }
            }
            
            // Applications list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(applications, key = { it.id }) { application ->
                    ApplicationCard(
                        application = application,
                        onClick = { onApplicationClick(application) }
                    )
                }
                
                if (applications.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No applications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationCard(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Company and logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (application.companyLogo != null) {
                    AsyncImage(
                        model = application.companyLogo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = application.companyName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                Text(
                    text = application.companyName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Job title
            Text(
                text = application.jobTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Location
            if (application.locationDisplay.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = application.locationDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Salary
            if (application.salaryDisplay.isNotEmpty()) {
                Text(
                    text = application.salaryDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Tags row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (application.isRemote) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Remote", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                if (application.requiresWorkPermit) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Work Permit", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

// ===== List View =====

@Composable
private fun ApplicationListView(
    applications: List<JobApplication>,
    onApplicationClick: (JobApplication) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(applications, key = { it.id }) { application ->
            ApplicationListItem(
                application = application,
                onClick = { onApplicationClick(application) }
            )
        }
        
        if (applications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No applications yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add your first job application",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationListItem(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Company logo/initial
            if (application.companyLogo != null) {
                AsyncImage(
                    model = application.companyLogo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = application.companyName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = application.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (application.locationDisplay.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = application.locationDisplay,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (application.salaryDisplay.isNotEmpty()) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = application.salaryDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Status badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = Color(android.graphics.Color.parseColor(application.status.color)).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = application.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(android.graphics.Color.parseColor(application.status.color))
                    )
                }
                
                application.appliedAt?.let { appliedAt ->
                    Text(
                        text = formatDate(appliedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ===== Calendar View (Placeholder) =====

@Composable
private fun ApplicationCalendarView(applications: List<JobApplication>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Calendar View",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Coming soon - View interviews and deadlines",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===== Add Application Dialog =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddApplicationDialog(
    onDismiss: () -> Unit,
    onAdd: (CreateJobApplicationRequest) -> Unit
) {
    var jobTitle by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var jobUrl by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedProvince by remember { mutableStateOf<CanadianProvince?>(null) }
    var isRemote by remember { mutableStateOf(false) }
    var salaryMin by remember { mutableStateOf("") }
    var salaryMax by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf<JobBoardSource?>(null) }
    
    var showProvinceDropdown by remember { mutableStateOf(false) }
    var showSourceDropdown by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Job Application") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = jobUrl,
                    onValueChange = { jobUrl = it },
                    label = { Text("Job URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Source dropdown
                ExposedDropdownMenuBox(
                    expanded = showSourceDropdown,
                    onExpandedChange = { showSourceDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedSource?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Job Board Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSourceDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showSourceDropdown,
                        onDismissRequest = { showSourceDropdown = false }
                    ) {
                        JobBoardSource.entries.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.displayName) },
                                onClick = {
                                    selectedSource = source
                                    showSourceDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Province dropdown
                    ExposedDropdownMenuBox(
                        expanded = showProvinceDropdown,
                        onExpandedChange = { showProvinceDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedProvince?.code ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Province") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProvinceDropdown) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showProvinceDropdown,
                            onDismissRequest = { showProvinceDropdown = false }
                        ) {
                            CanadianProvince.entries.forEach { province ->
                                DropdownMenuItem(
                                    text = { Text("${province.code} - ${province.displayName}") },
                                    onClick = {
                                        selectedProvince = province
                                        showProvinceDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRemote,
                        onCheckedChange = { isRemote = it }
                    )
                    Text("Remote position")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = salaryMin,
                        onValueChange = { salaryMin = it.filter { c -> c.isDigit() } },
                        label = { Text("Salary Min (CAD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = salaryMax,
                        onValueChange = { salaryMax = it.filter { c -> c.isDigit() } },
                        label = { Text("Salary Max (CAD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (jobTitle.isNotBlank() && companyName.isNotBlank()) {
                        onAdd(CreateJobApplicationRequest(
                            jobTitle = jobTitle,
                            companyName = companyName,
                            jobUrl = jobUrl.takeIf { it.isNotBlank() },
                            jobBoardSource = selectedSource,
                            city = city.takeIf { it.isNotBlank() },
                            province = selectedProvince,
                            isRemote = isRemote,
                            salaryMin = salaryMin.toIntOrNull(),
                            salaryMax = salaryMax.toIntOrNull()
                        ))
                    }
                },
                enabled = jobTitle.isNotBlank() && companyName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ===== Filter Bottom Sheet =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentStatus: ApplicationStatus?,
    currentSource: JobBoardSource?,
    currentProvince: CanadianProvince?,
    searchQuery: String?,
    onStatusChange: (ApplicationStatus?) -> Unit,
    onSourceChange: (JobBoardSource?) -> Unit,
    onProvinceChange: (CanadianProvince?) -> Unit,
    onSearchChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var localSearchQuery by remember { mutableStateOf(searchQuery ?: "") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filter Applications",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Search
            OutlinedTextField(
                value = localSearchQuery,
                onValueChange = { 
                    localSearchQuery = it
                    onSearchChange(it.takeIf { q -> q.isNotBlank() })
                },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Status filter
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentStatus == null,
                        onClick = { onStatusChange(null) },
                        label = { Text("All") }
                    )
                }
                items(ApplicationStatus.entries) { status ->
                    FilterChip(
                        selected = currentStatus == status,
                        onClick = { onStatusChange(status) },
                        label = { Text(status.displayName) }
                    )
                }
            }
            
            // Province filter
            Text(
                text = "Province",
                style = MaterialTheme.typography.titleSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentProvince == null,
                        onClick = { onProvinceChange(null) },
                        label = { Text("All") }
                    )
                }
                items(CanadianProvince.entries) { province ->
                    FilterChip(
                        selected = currentProvince == province,
                        onClick = { onProvinceChange(province) },
                        label = { Text(province.code) }
                    )
                }
            }
            
            // Clear filters button
            Button(
                onClick = {
                    localSearchQuery = ""
                    onClearFilters()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Filters")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ===== Application Detail Dialog =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationDetailDialog(
    detail: JobApplicationDetail,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onStatusChange: (ApplicationStatus, String?) -> Unit,
    onAddNote: (String, NoteType) -> Unit,
    onAddReminder: (CreateReminderRequest) -> Unit,
    onDelete: () -> Unit
) {
    val application = detail.application
    var showStatusChangeDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Notes", "Reminders", "History")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = application.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    // Status chip with change button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(android.graphics.Color.parseColor(application.status.color)).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = application.status.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color(android.graphics.Color.parseColor(application.status.color))
                            )
                        }
                        
                        TextButton(onClick = { showStatusChangeDialog = true }) {
                            Text("Change Status")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    
                    // Tab content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)
                    ) {
                        when (selectedTab) {
                            0 -> DetailTabContent(application)
                            1 -> NotesTabContent(
                                notes = detail.notes,
                                onAddNote = { showAddNoteDialog = true }
                            )
                            2 -> RemindersTabContent(detail.reminders)
                            3 -> HistoryTabContent(detail.statusHistory)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {}
    )
    
    // Status change dialog
    if (showStatusChangeDialog) {
        StatusChangeDialog(
            currentStatus = application.status,
            onDismiss = { showStatusChangeDialog = false },
            onConfirm = { newStatus, notes ->
                onStatusChange(newStatus, notes)
                showStatusChangeDialog = false
            }
        )
    }
    
    // Add note dialog
    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onAdd = { content, noteType ->
                onAddNote(content, noteType)
                showAddNoteDialog = false
            }
        )
    }
    
    // Delete confirmation
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Application?") },
            text = { Text("Are you sure you want to delete this job application? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailTabContent(application: JobApplication) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (application.locationDisplay.isNotBlank()) {
            DetailRow(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = application.locationDisplay
            )
        }
        
        if (application.salaryDisplay.isNotEmpty()) {
            DetailRow(
                icon = Icons.Default.AttachMoney,
                label = "Salary",
                value = application.salaryDisplay
            )
        }
        
        application.jobBoardSource?.let { source ->
            DetailRow(
                icon = Icons.Default.Source,
                label = "Source",
                value = source.displayName
            )
        }
        
        application.nocCode?.let { noc ->
            DetailRow(
                icon = Icons.Default.Code,
                label = "NOC Code",
                value = noc
            )
        }
        
        application.appliedAt?.let { date ->
            DetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Applied",
                value = formatDate(date)
            )
        }
        
        if (application.requiresWorkPermit) {
            DetailRow(
                icon = Icons.Default.Work,
                label = "Work Permit",
                value = "Required"
            )
        }
        
        if (application.isLmiaRequired) {
            DetailRow(
                icon = Icons.Default.Assignment,
                label = "LMIA",
                value = "Required"
            )
        }
        
        // Contact info
        application.contactName?.let { name ->
            DetailRow(
                icon = Icons.Default.Person,
                label = "Contact",
                value = name
            )
        }
        
        application.contactEmail?.let { email ->
            DetailRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = email
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NotesTabContent(
    notes: List<ApplicationNote>,
    onAddNote: () -> Unit
) {
    Column {
        Button(
            onClick = onAddNote,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Note")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = note.noteType.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatDate(note.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersTabContent(reminders: List<ApplicationReminder>) {
    if (reminders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No reminders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reminders) { reminder ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (reminder.isCompleted) 
                                Icons.Default.CheckCircle else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (reminder.isCompleted) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatDateTime(reminder.reminderAt),
                                style = MaterialTheme.typography.bodySmall,
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
private fun HistoryTabContent(history: List<StatusChange>) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No status history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { change ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (change.fromStatus != null) 
                                    "${change.fromStatus.displayName} → ${change.toStatus.displayName}"
                                else "Created as ${change.toStatus.displayName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatDateTime(change.changedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            change.notes?.let { notes ->
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== Status Change Dialog =====

@Composable
private fun StatusChangeDialog(
    currentStatus: ApplicationStatus,
    onDismiss: () -> Unit,
    onConfirm: (ApplicationStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Status") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(ApplicationStatus.entries) { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStatus = status }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status }
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(status.color)),
                                        CircleShape
                                    )
                            )
                            Text(status.displayName)
                        }
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedStatus, notes.takeIf { it.isNotBlank() }) }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ===== Add Note Dialog =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, NoteType) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NoteType.GENERAL) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = showTypeDropdown,
                    onExpandedChange = { showTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Note Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        NoteType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(content, selectedType) },
                enabled = content.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ===== Helper Functions =====

private fun formatDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.year}"
    } catch (e: Exception) {
        isoString
    }
}

private fun formatDateTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.year} at ${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        isoString
    }
}
