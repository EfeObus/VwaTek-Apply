package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.tracker.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

/**
 * Phase 2: Job Application Tracker Screen for Web
 * Displays job applications in Kanban, List, or Calendar view
 */
@Composable
fun TrackerScreen() {
    val viewModel = remember { GlobalContext.get().get<TrackerViewModel>() }
    val state by viewModel.state.collectAsState()
    
    var showAddModal by remember { mutableStateOf(false) }
    var showFilterModal by remember { mutableStateOf(false) }
    var selectedApplicationId by remember { mutableStateOf<String?>(null) }
    
    Div {
        // Header
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Div {
                H1 { Text("Job Tracker") }
                P(attrs = { classes("text-secondary") }) {
                    Text("Track your job applications with our Kanban board.")
                }
            }
            Div(attrs = { classes("flex", "gap-sm") }) {
                // View mode toggle
                Div(attrs = { classes("btn-group") }) {
                    Button(attrs = {
                        classes("btn", if (state.viewMode == TrackerViewMode.KANBAN) "btn-primary" else "btn-outline")
                        onClick { viewModel.onIntent(TrackerIntent.SetViewMode(TrackerViewMode.KANBAN)) }
                    }) {
                        Text("Kanban")
                    }
                    Button(attrs = {
                        classes("btn", if (state.viewMode == TrackerViewMode.LIST) "btn-primary" else "btn-outline")
                        onClick { viewModel.onIntent(TrackerIntent.SetViewMode(TrackerViewMode.LIST)) }
                    }) {
                        Text("List")
                    }
                    Button(attrs = {
                        classes("btn", if (state.viewMode == TrackerViewMode.CALENDAR) "btn-primary" else "btn-outline")
                        onClick { viewModel.onIntent(TrackerIntent.SetViewMode(TrackerViewMode.CALENDAR)) }
                    }) {
                        Text("Calendar")
                    }
                }
                
                Button(attrs = {
                    classes("btn", "btn-outline")
                    onClick { showFilterModal = true }
                }) {
                    Text("Filter")
                    if (state.hasActiveFilters) {
                        Span(attrs = { classes("badge", "badge-primary", "ml-xs") }) {
                            Text("!")
                        }
                    }
                }
                
                Button(attrs = {
                    classes("btn", "btn-primary")
                    onClick { showAddModal = true }
                }) {
                    Text("+ Add Job")
                }
            }
        }
        
        // Stats Bar
        state.stats?.let { stats ->
            TrackerStatsBar(stats)
        }
        
        // Main Content
        if (state.isLoading) {
            Div(attrs = { classes("flex", "justify-center", "mt-lg") }) {
                Div(attrs = { classes("spinner") })
            }
        } else {
            when (state.viewMode) {
                TrackerViewMode.KANBAN -> KanbanBoard(
                    columns = state.kanbanColumns,
                    onApplicationClick = { app ->
                        selectedApplicationId = app.id
                        viewModel.onIntent(TrackerIntent.SelectApplication(app.id))
                    },
                    onStatusChange = { appId, newStatus ->
                        viewModel.onIntent(TrackerIntent.MoveToStatus(appId, newStatus))
                    }
                )
                TrackerViewMode.LIST -> ApplicationListView(
                    applications = state.applications,
                    onApplicationClick = { app ->
                        selectedApplicationId = app.id
                        viewModel.onIntent(TrackerIntent.SelectApplication(app.id))
                    }
                )
                TrackerViewMode.CALENDAR -> CalendarView()
            }
        }
    }
    
    // Add Job Modal
    if (showAddModal) {
        AddApplicationModal(
            onClose = { showAddModal = false },
            onAdd = { request ->
                viewModel.onIntent(TrackerIntent.CreateApplication(request))
                showAddModal = false
            }
        )
    }
    
    // Filter Modal
    if (showFilterModal) {
        FilterModal(
            currentStatus = state.filterStatus,
            currentSource = state.filterSource,
            currentProvince = state.filterProvince,
            searchQuery = state.searchQuery,
            onStatusChange = { viewModel.onIntent(TrackerIntent.SetFilterStatus(it)) },
            onSourceChange = { viewModel.onIntent(TrackerIntent.SetFilterSource(it)) },
            onProvinceChange = { viewModel.onIntent(TrackerIntent.SetFilterProvince(it)) },
            onSearchChange = { viewModel.onIntent(TrackerIntent.SetSearchQuery(it)) },
            onClearFilters = { viewModel.onIntent(TrackerIntent.ClearFilters) },
            onClose = { showFilterModal = false }
        )
    }
    
    // Application Detail Modal
    state.selectedApplication?.let { detail ->
        ApplicationDetailModal(
            detail = detail,
            isLoading = state.isLoadingDetail,
            onClose = {
                selectedApplicationId = null
                viewModel.onIntent(TrackerIntent.ClearSelectedApplication)
            },
            onStatusChange = { newStatus, notes ->
                viewModel.onIntent(TrackerIntent.UpdateStatus(detail.application.id, newStatus, notes))
            },
            onAddNote = { content, noteType ->
                viewModel.onIntent(TrackerIntent.AddNote(detail.application.id, content, noteType))
            },
            onDelete = {
                viewModel.onIntent(TrackerIntent.DeleteApplication(detail.application.id))
            }
        )
    }
    
    // Error Toast
    state.error?.let { error ->
        Div(attrs = { 
            classes("toast", "toast-error")
            onClick { viewModel.onIntent(TrackerIntent.ClearError) }
        }) {
            Text(error)
        }
    }
}

private val TrackerState.hasActiveFilters: Boolean
    get() = filterStatus != null || filterSource != null || filterProvince != null || !searchQuery.isNullOrBlank()

// ===== Stats Bar =====

@Composable
private fun TrackerStatsBar(stats: TrackerStats) {
    Div(attrs = { 
        classes("card", "mb-lg")
        style {
            backgroundColor(Color("#f8f9fa"))
            padding(16.px)
        }
    }) {
        Div(attrs = { classes("flex", "justify-around", "flex-wrap", "gap-md") }) {
            StatItem("Total", stats.totalApplications.toString())
            StatItem("Applied", stats.appliedCount.toString())
            StatItem("Interviews", stats.interviewCount.toString())
            StatItem("Offers", stats.offerCount.toString())
            StatItem("Interview Rate", "${(stats.interviewRate * 100).toInt()}%")
            StatItem("Offer Rate", "${(stats.offerRate * 100).toInt()}%")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Div(attrs = { classes("text-center") }) {
        Div(attrs = { 
            classes("text-xl", "font-bold")
            style { color(Color("#333")) }
        }) {
            Text(value)
        }
        Div(attrs = { classes("text-sm", "text-secondary") }) {
            Text(label)
        }
    }
}

// ===== Kanban Board =====

@Composable
private fun KanbanBoard(
    columns: Map<ApplicationStatus, List<JobApplication>>,
    onApplicationClick: (JobApplication) -> Unit,
    onStatusChange: (String, ApplicationStatus) -> Unit
) {
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
    
    Div(attrs = {
        classes("kanban-board")
        style {
            display(DisplayStyle.Flex)
            gap(16.px)
            overflowX("auto")
            paddingBottom(16.px)
            minHeight(500.px)
        }
    }) {
        activeStatuses.forEach { status ->
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
    Div(attrs = {
        classes("kanban-column")
        style {
            minWidth(280.px)
            maxWidth(280.px)
            backgroundColor(Color.white)
            borderRadius(12.px)
            property("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
        attr("data-status", status.name)
        onDragOver { it.preventDefault() }
        onDrop { e ->
            e.preventDefault()
            val appId = e.dataTransfer?.getData("application/x-job-id")
            if (appId != null) {
                onDropReceived(appId)
            }
        }
    }) {
        // Column Header
        Div(attrs = {
            style {
                padding(12.px, 16.px)
                backgroundColor(Color(status.color).let { Color("${it}33") })
                property("border-radius", "12px 12px 0 0")
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Center)
            }
        }) {
            Span(attrs = { 
                classes("font-semibold")
                style { fontSize(14.px) }
            }) {
                Text(status.displayName)
            }
            Span(attrs = {
                style {
                    backgroundColor(Color(status.color))
                    color(Color.white)
                    padding(2.px, 8.px)
                    borderRadius(999.px)
                    fontSize(12.px)
                }
            }) {
                Text(applications.size.toString())
            }
        }
        
        // Applications
        Div(attrs = {
            style {
                padding(8.px)
                flexGrow(1)
                overflowY("auto")
            }
        }) {
            if (applications.isEmpty()) {
                Div(attrs = {
                    classes("text-secondary", "text-center")
                    style {
                        padding(40.px, 16.px)
                        fontSize(14.px)
                    }
                }) {
                    Text("No applications")
                }
            } else {
                applications.forEach { application ->
                    ApplicationCard(
                        application = application,
                        onClick = { onApplicationClick(application) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: JobApplication,
    onClick: () -> Unit
) {
    Div(attrs = {
        classes("application-card")
        style {
            backgroundColor(Color("#f8f9fa"))
            borderRadius(8.px)
            padding(12.px)
            marginBottom(8.px)
            cursor("pointer")
            property("transition", "transform 0.2s, box-shadow 0.2s")
        }
        onClick { onClick() }
        draggable(Draggable.True)
        onDragStart { e ->
            e.dataTransfer?.setData("application/x-job-id", application.id)
        }
    }) {
        // Company
        Div(attrs = {
            classes("flex", "items-center", "gap-sm", "mb-xs")
        }) {
            if (application.companyLogo != null) {
                Img(src = application.companyLogo!!, attrs = {
                    style {
                        width(24.px)
                        height(24.px)
                        borderRadius(4.px)
                        property("object-fit", "cover")
                    }
                })
            } else {
                Div(attrs = {
                    style {
                        width(24.px)
                        height(24.px)
                        backgroundColor(Color("#e3e8ee"))
                        borderRadius(4.px)
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.Center)
                        fontSize(10.px)
                    }
                }) {
                    Text(application.companyName.firstOrNull()?.uppercase() ?: "?")
                }
            }
            Span(attrs = { 
                classes("text-sm")
                style { 
                    property("white-space", "nowrap")
                    property("overflow", "hidden")
                    property("text-overflow", "ellipsis")
                }
            }) {
                Text(application.companyName)
            }
        }
        
        // Job Title
        Div(attrs = {
            classes("font-medium", "mb-xs")
            style {
                fontSize(14.px)
                property("display", "-webkit-box")
                property("-webkit-line-clamp", "2")
                property("-webkit-box-orient", "vertical")
                property("overflow", "hidden")
            }
        }) {
            Text(application.jobTitle)
        }
        
        // Location
        if (application.locationDisplay.isNotBlank()) {
            Div(attrs = {
                classes("flex", "items-center", "gap-xs", "mb-xs")
                style {
                    fontSize(12.px)
                    color(Color("#666"))
                }
            }) {
                Text("📍 ${application.locationDisplay}")
            }
        }
        
        // Salary
        if (application.salaryDisplay.isNotEmpty()) {
            Div(attrs = {
                style {
                    fontSize(12.px)
                    color(Color("#0066cc"))
                    marginBottom(8.px)
                }
            }) {
                Text(application.salaryDisplay)
            }
        }
        
        // Tags
        Div(attrs = { classes("flex", "gap-xs", "flex-wrap") }) {
            if (application.isRemote) {
                Tag("Remote")
            }
            if (application.requiresWorkPermit) {
                Tag("Work Permit")
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Span(attrs = {
        style {
            backgroundColor(Color("#e3e8ee"))
            padding(2.px, 6.px)
            borderRadius(4.px)
            fontSize(10.px)
            color(Color("#333"))
        }
    }) {
        Text(text)
    }
}

// ===== List View =====

@Composable
private fun ApplicationListView(
    applications: List<JobApplication>,
    onApplicationClick: (JobApplication) -> Unit
) {
    if (applications.isEmpty()) {
        EmptyState(
            icon = "📋",
            title = "No applications yet",
            description = "Click '+ Add Job' to track your first job application."
        )
    } else {
        Div(attrs = { classes("space-y-md") }) {
            applications.forEach { application ->
                ApplicationListItem(application) { onApplicationClick(application) }
            }
        }
    }
}

@Composable
private fun ApplicationListItem(
    application: JobApplication,
    onClick: () -> Unit
) {
    Div(attrs = {
        classes("card", "card-hover")
        style { cursor("pointer") }
        onClick { onClick() }
    }) {
        Div(attrs = { classes("flex", "justify-between", "items-start") }) {
            Div(attrs = { classes("flex", "gap-md") }) {
                // Logo
                if (application.companyLogo != null) {
                    Img(src = application.companyLogo!!, attrs = {
                        style {
                            width(48.px)
                            height(48.px)
                            borderRadius(8.px)
                            property("object-fit", "cover")
                        }
                    })
                } else {
                    Div(attrs = {
                        style {
                            width(48.px)
                            height(48.px)
                            backgroundColor(Color("#e3e8ee"))
                            borderRadius(8.px)
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            fontSize(18.px)
                        }
                    }) {
                        Text(application.companyName.take(2).uppercase())
                    }
                }
                
                // Content
                Div {
                    H3(attrs = { 
                        classes("mb-xs")
                        style { fontSize(16.px) }
                    }) {
                        Text(application.jobTitle)
                    }
                    P(attrs = { classes("text-secondary", "mb-xs") }) {
                        Text(application.companyName)
                    }
                    Div(attrs = { classes("flex", "gap-md", "text-sm", "text-secondary") }) {
                        if (application.locationDisplay.isNotBlank()) {
                            Span { Text("📍 ${application.locationDisplay}") }
                        }
                        if (application.salaryDisplay.isNotEmpty()) {
                            Span(attrs = { style { color(Color("#0066cc")) } }) {
                                Text(application.salaryDisplay)
                            }
                        }
                    }
                }
            }
            
            // Status badge
            Div(attrs = { classes("text-right") }) {
                Span(attrs = {
                    style {
                        backgroundColor(Color(application.status.color).let { Color("${it}33") })
                        color(Color(application.status.color))
                        padding(4.px, 12.px)
                        borderRadius(4.px)
                        fontSize(12.px)
                    }
                }) {
                    Text(application.status.displayName)
                }
                application.appliedAt?.let { date ->
                    P(attrs = {
                        classes("text-sm", "text-secondary", "mt-xs")
                    }) {
                        Text(formatDate(date))
                    }
                }
            }
        }
    }
}

// ===== Calendar View =====

@Composable
private fun CalendarView() {
    EmptyState(
        icon = "📅",
        title = "Calendar View",
        description = "Coming soon - View interviews and application deadlines on a calendar."
    )
}

// ===== Add Application Modal =====

@Composable
private fun AddApplicationModal(
    onClose: () -> Unit,
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
    
    Modal(title = "Add Job Application", onClose = onClose) {
        Form(attrs = {
            onSubmit { e ->
                e.preventDefault()
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
            }
        }) {
            Div(attrs = { classes("form-group") }) {
                Label { Text("Job Title *") }
                Input(InputType.Text) {
                    classes("form-control")
                    value(jobTitle)
                    onInput { jobTitle = it.value }
                    attr("required", "")
                }
            }
            
            Div(attrs = { classes("form-group") }) {
                Label { Text("Company Name *") }
                Input(InputType.Text) {
                    classes("form-control")
                    value(companyName)
                    onInput { companyName = it.value }
                    attr("required", "")
                }
            }
            
            Div(attrs = { classes("form-group") }) {
                Label { Text("Job URL") }
                Input(InputType.Url) {
                    classes("form-control")
                    value(jobUrl)
                    onInput { jobUrl = it.value }
                }
            }
            
            Div(attrs = { classes("form-group") }) {
                Label { Text("Job Board Source") }
                Select(attrs = {
                    classes("form-control")
                    onChange { e ->
                        val value = (e.target as? org.w3c.dom.HTMLSelectElement)?.value
                        selectedSource = value?.let { v -> 
                            JobBoardSource.entries.find { it.name == v }
                        }
                    }
                }) {
                    Option("") { Text("Select Source") }
                    JobBoardSource.entries.forEach { source ->
                        Option(source.name) { Text(source.displayName) }
                    }
                }
            }
            
            Div(attrs = { classes("grid", "grid-2", "gap-md") }) {
                Div(attrs = { classes("form-group") }) {
                    Label { Text("City") }
                    Input(InputType.Text) {
                        classes("form-control")
                        value(city)
                        onInput { city = it.value }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label { Text("Province") }
                    Select(attrs = {
                        classes("form-control")
                        onChange { e ->
                            val value = (e.target as? org.w3c.dom.HTMLSelectElement)?.value
                            selectedProvince = value?.let { v ->
                                CanadianProvince.entries.find { it.code == v }
                            }
                        }
                    }) {
                        Option("") { Text("Select Province") }
                        CanadianProvince.entries.forEach { province ->
                            Option(province.code) { Text("${province.code} - ${province.displayName}") }
                        }
                    }
                }
            }
            
            Div(attrs = { classes("form-group") }) {
                Label(attrs = { classes("flex", "items-center", "gap-sm") }) {
                    Input(InputType.Checkbox) {
                        checked(isRemote)
                        onChange { isRemote = it.value }
                    }
                    Text("Remote Position")
                }
            }
            
            Div(attrs = { classes("grid", "grid-2", "gap-md") }) {
                Div(attrs = { classes("form-group") }) {
                    Label { Text("Salary Min (CAD)") }
                    Input(InputType.Number) {
                        classes("form-control")
                        value(salaryMin)
                        onInput { salaryMin = it.value }
                    }
                }
                
                Div(attrs = { classes("form-group") }) {
                    Label { Text("Salary Max (CAD)") }
                    Input(InputType.Number) {
                        classes("form-control")
                        value(salaryMax)
                        onInput { salaryMax = it.value }
                    }
                }
            }
            
            Div(attrs = { classes("flex", "justify-end", "gap-sm", "mt-lg") }) {
                Button(attrs = {
                    classes("btn", "btn-outline")
                    type(ButtonType.Button)
                    onClick { onClose() }
                }) {
                    Text("Cancel")
                }
                Button(attrs = {
                    classes("btn", "btn-primary")
                    type(ButtonType.Submit)
                    if (jobTitle.isBlank() || companyName.isBlank()) {
                        disabled()
                    }
                }) {
                    Text("Add Application")
                }
            }
        }
    }
}

// ===== Filter Modal =====

@Composable
private fun FilterModal(
    currentStatus: ApplicationStatus?,
    currentSource: JobBoardSource?,
    currentProvince: CanadianProvince?,
    searchQuery: String?,
    onStatusChange: (ApplicationStatus?) -> Unit,
    onSourceChange: (JobBoardSource?) -> Unit,
    onProvinceChange: (CanadianProvince?) -> Unit,
    onSearchChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onClose: () -> Unit
) {
    var localSearch by remember { mutableStateOf(searchQuery ?: "") }
    
    Modal(title = "Filter Applications", onClose = onClose) {
        Div(attrs = { classes("form-group") }) {
            Label { Text("Search") }
            Input(InputType.Search) {
                classes("form-control")
                value(localSearch)
                onInput { 
                    localSearch = it.value
                    onSearchChange(it.value.takeIf { v -> v.isNotBlank() })
                }
                attr("placeholder", "Search jobs...")
            }
        }
        
        Div(attrs = { classes("form-group") }) {
            Label { Text("Status") }
            Select(attrs = {
                classes("form-control")
                onChange { e ->
                    val value = (e.target as? org.w3c.dom.HTMLSelectElement)?.value
                    onStatusChange(value?.takeIf { it.isNotEmpty() }?.let { v ->
                        ApplicationStatus.entries.find { it.name == v }
                    })
                }
            }) {
                Option("") { Text("All Statuses") }
                ApplicationStatus.entries.forEach { status ->
                    Option(status.name, attrs = {
                        if (currentStatus == status) selected()
                    }) {
                        Text(status.displayName)
                    }
                }
            }
        }
        
        Div(attrs = { classes("form-group") }) {
            Label { Text("Province") }
            Select(attrs = {
                classes("form-control")
                onChange { e ->
                    val value = (e.target as? org.w3c.dom.HTMLSelectElement)?.value
                    onProvinceChange(value?.takeIf { it.isNotEmpty() }?.let { v ->
                        CanadianProvince.entries.find { it.code == v }
                    })
                }
            }) {
                Option("") { Text("All Provinces") }
                CanadianProvince.entries.forEach { province ->
                    Option(province.code, attrs = {
                        if (currentProvince == province) selected()
                    }) {
                        Text("${province.code} - ${province.displayName}")
                    }
                }
            }
        }
        
        Div(attrs = { classes("flex", "justify-between", "mt-lg") }) {
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick {
                    localSearch = ""
                    onClearFilters()
                }
            }) {
                Text("Clear Filters")
            }
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick { onClose() }
            }) {
                Text("Done")
            }
        }
    }
}

// ===== Application Detail Modal =====

@Composable
private fun ApplicationDetailModal(
    detail: JobApplicationDetail,
    isLoading: Boolean,
    onClose: () -> Unit,
    onStatusChange: (ApplicationStatus, String?) -> Unit,
    onAddNote: (String, NoteType) -> Unit,
    onDelete: () -> Unit
) {
    val application = detail.application
    var activeTab by remember { mutableStateOf(0) }
    var showStatusModal by remember { mutableStateOf(false) }
    var showAddNoteModal by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Modal(title = application.jobTitle, onClose = onClose, large = true) {
        // Header
        Div(attrs = { classes("mb-lg") }) {
            P(attrs = { classes("text-lg", "text-secondary") }) {
                Text(application.companyName)
            }
            Div(attrs = { classes("flex", "justify-between", "items-center", "mt-sm") }) {
                Span(attrs = {
                    style {
                        backgroundColor(Color(application.status.color).let { Color("${it}33") })
                        color(Color(application.status.color))
                        padding(6.px, 16.px)
                        borderRadius(6.px)
                        fontSize(14.px)
                    }
                }) {
                    Text(application.status.displayName)
                }
                Button(attrs = {
                    classes("btn", "btn-outline", "btn-sm")
                    onClick { showStatusModal = true }
                }) {
                    Text("Change Status")
                }
            }
        }
        
        // Tabs
        Div(attrs = { classes("tabs", "mb-md") }) {
            listOf("Details", "Notes", "Reminders", "History").forEachIndexed { index, tab ->
                Button(attrs = {
                    classes("tab", if (activeTab == index) "tab-active" else "")
                    onClick { activeTab = index }
                }) {
                    Text(tab)
                }
            }
        }
        
        // Tab Content
        Div(attrs = { 
            classes("tab-content")
            style { minHeight(300.px) }
        }) {
            if (isLoading) {
                Div(attrs = { classes("flex", "justify-center", "items-center") }) {
                    Div(attrs = { classes("spinner") })
                }
            } else {
                when (activeTab) {
                    0 -> DetailsTab(application)
                    1 -> NotesTab(detail.notes) { showAddNoteModal = true }
                    2 -> RemindersTab(detail.reminders)
                    3 -> HistoryTab(detail.statusHistory)
                }
            }
        }
        
        // Footer
        Div(attrs = { classes("flex", "justify-between", "mt-lg", "pt-md", "border-top") }) {
            Button(attrs = {
                classes("btn", "btn-danger")
                onClick { showDeleteConfirm = true }
            }) {
                Text("Delete")
            }
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick { onClose() }
            }) {
                Text("Close")
            }
        }
    }
    
    // Status Change Modal
    if (showStatusModal) {
        StatusChangeModal(
            currentStatus = application.status,
            onClose = { showStatusModal = false },
            onConfirm = { newStatus, notes ->
                onStatusChange(newStatus, notes)
                showStatusModal = false
            }
        )
    }
    
    // Add Note Modal
    if (showAddNoteModal) {
        AddNoteModal(
            onClose = { showAddNoteModal = false },
            onAdd = { content, noteType ->
                onAddNote(content, noteType)
                showAddNoteModal = false
            }
        )
    }
    
    // Delete Confirmation
    if (showDeleteConfirm) {
        ConfirmModal(
            title = "Delete Application?",
            message = "Are you sure you want to delete this job application? This action cannot be undone.",
            confirmText = "Delete",
            isDanger = true,
            onClose = { showDeleteConfirm = false },
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            }
        )
    }
}

@Composable
private fun DetailsTab(application: JobApplication) {
    Div(attrs = { classes("space-y-md") }) {
        if (application.locationDisplay.isNotBlank()) {
            DetailRow("📍 Location", application.locationDisplay)
        }
        if (application.salaryDisplay.isNotEmpty()) {
            DetailRow("💰 Salary", application.salaryDisplay)
        }
        application.jobBoardSource?.let {
            DetailRow("🔗 Source", it.displayName)
        }
        application.nocCode?.let {
            DetailRow("📋 NOC Code", it)
        }
        application.appliedAt?.let {
            DetailRow("📅 Applied", formatDate(it))
        }
        if (application.requiresWorkPermit) {
            DetailRow("📄 Work Permit", "Required")
        }
        if (application.isLmiaRequired) {
            DetailRow("📄 LMIA", "Required")
        }
        application.contactName?.let {
            DetailRow("👤 Contact", it)
        }
        application.contactEmail?.let {
            DetailRow("✉️ Email", it)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Div(attrs = { classes("flex", "gap-md") }) {
        Span(attrs = { 
            classes("text-secondary")
            style { minWidth(120.px) }
        }) {
            Text(label)
        }
        Span { Text(value) }
    }
}

@Composable
private fun NotesTab(notes: List<ApplicationNote>, onAddNote: () -> Unit) {
    Button(attrs = {
        classes("btn", "btn-primary", "w-full", "mb-md")
        onClick { onAddNote() }
    }) {
        Text("+ Add Note")
    }
    
    if (notes.isEmpty()) {
        Div(attrs = { classes("text-center", "text-secondary", "py-lg") }) {
            Text("No notes yet")
        }
    } else {
        Div(attrs = { classes("space-y-sm") }) {
            notes.forEach { note ->
                Div(attrs = { classes("card", "p-md") }) {
                    Div(attrs = { classes("flex", "justify-between", "mb-xs") }) {
                        Span(attrs = { classes("text-primary", "text-sm") }) {
                            Text(note.noteType.displayName)
                        }
                        Span(attrs = { classes("text-secondary", "text-sm") }) {
                            Text(formatDate(note.createdAt))
                        }
                    }
                    P { Text(note.content) }
                }
            }
        }
    }
}

@Composable
private fun RemindersTab(reminders: List<ApplicationReminder>) {
    if (reminders.isEmpty()) {
        Div(attrs = { classes("text-center", "text-secondary", "py-lg") }) {
            Text("No reminders")
        }
    } else {
        Div(attrs = { classes("space-y-sm") }) {
            reminders.forEach { reminder ->
                Div(attrs = { classes("card", "p-md", "flex", "gap-md", "items-center") }) {
                    Span(attrs = { 
                        style { fontSize(20.px) }
                    }) {
                        Text(if (reminder.isCompleted) "✅" else "🔔")
                    }
                    Div {
                        P(attrs = { classes("font-medium") }) { Text(reminder.title) }
                        P(attrs = { classes("text-sm", "text-secondary") }) {
                            Text(formatDateTime(reminder.reminderAt))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<StatusChange>) {
    if (history.isEmpty()) {
        Div(attrs = { classes("text-center", "text-secondary", "py-lg") }) {
            Text("No status history")
        }
    } else {
        Div(attrs = { classes("space-y-sm") }) {
            history.forEach { change ->
                Div(attrs = { classes("card", "p-md", "flex", "gap-md", "items-center") }) {
                    Span(attrs = { style { fontSize(20.px) } }) { Text("➡️") }
                    Div {
                        P(attrs = { classes("font-medium") }) {
                            if (change.fromStatus != null) {
                                Text("${change.fromStatus!!.displayName} → ${change.toStatus.displayName}")
                            } else {
                                Text("Created as ${change.toStatus.displayName}")
                            }
                        }
                        P(attrs = { classes("text-sm", "text-secondary") }) {
                            Text(formatDateTime(change.changedAt))
                        }
                        change.notes?.let { notes ->
                            P(attrs = { classes("text-sm", "text-secondary", "mt-xs") }) {
                                Text(notes)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== Helper Modals =====

@Composable
private fun StatusChangeModal(
    currentStatus: ApplicationStatus,
    onClose: () -> Unit,
    onConfirm: (ApplicationStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var notes by remember { mutableStateOf("") }
    
    Modal(title = "Change Status", onClose = onClose) {
        Div(attrs = { classes("form-group") }) {
            Label { Text("Select Status") }
            Div(attrs = { classes("space-y-xs") }) {
                ApplicationStatus.entries.forEach { status ->
                    Label(attrs = {
                        classes("flex", "items-center", "gap-sm", "p-sm", "cursor-pointer")
                        style {
                            if (selectedStatus == status) {
                                backgroundColor(Color("#f0f0f0"))
                            }
                            borderRadius(4.px)
                        }
                    }) {
                        Input(InputType.Radio) {
                            name("status")
                            checked(selectedStatus == status)
                            onChange { selectedStatus = status }
                        }
                        Span(attrs = {
                            style {
                                width(12.px)
                                height(12.px)
                                backgroundColor(Color(status.color))
                                borderRadius(999.px)
                                display(DisplayStyle.InlineBlock)
                            }
                        })
                        Text(status.displayName)
                    }
                }
            }
        }
        
        Div(attrs = { classes("form-group") }) {
            Label { Text("Notes (optional)") }
            TextArea(attrs = {
                classes("form-control")
                value(notes)
                onInput { notes = it.value }
                attr("rows", "3")
            })
        }
        
        Div(attrs = { classes("flex", "justify-end", "gap-sm", "mt-lg") }) {
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick { onClose() }
            }) {
                Text("Cancel")
            }
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick { onConfirm(selectedStatus, notes.takeIf { it.isNotBlank() }) }
            }) {
                Text("Update")
            }
        }
    }
}

@Composable
private fun AddNoteModal(
    onClose: () -> Unit,
    onAdd: (String, NoteType) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NoteType.GENERAL) }
    
    Modal(title = "Add Note", onClose = onClose) {
        Div(attrs = { classes("form-group") }) {
            Label { Text("Note Type") }
            Select(attrs = {
                classes("form-control")
                onChange { e ->
                    val value = (e.target as? org.w3c.dom.HTMLSelectElement)?.value
                    selectedType = value?.let { NoteType.entries.find { t -> t.name == it } } ?: NoteType.GENERAL
                }
            }) {
                NoteType.entries.forEach { type ->
                    Option(type.name) { Text(type.displayName) }
                }
            }
        }
        
        Div(attrs = { classes("form-group") }) {
            Label { Text("Content") }
            TextArea(attrs = {
                classes("form-control")
                value(content)
                onInput { content = it.value }
                attr("rows", "5")
            })
        }
        
        Div(attrs = { classes("flex", "justify-end", "gap-sm", "mt-lg") }) {
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick { onClose() }
            }) {
                Text("Cancel")
            }
            Button(attrs = {
                classes("btn", "btn-primary")
                if (content.isBlank()) disabled()
                onClick { onAdd(content, selectedType) }
            }) {
                Text("Add Note")
            }
        }
    }
}

// ===== Reusable Components =====

@Composable
private fun Modal(
    title: String,
    onClose: () -> Unit,
    large: Boolean = false,
    content: @Composable () -> Unit
) {
    Div(attrs = {
        classes("modal-overlay")
        onClick { onClose() }
    }) {
        Div(attrs = {
            classes("modal", if (large) "modal-lg" else "")
            onClick { it.stopPropagation() }
        }) {
            Div(attrs = { classes("modal-header") }) {
                H2 { Text(title) }
                Button(attrs = {
                    classes("btn-close")
                    onClick { onClose() }
                }) {
                    Text("×")
                }
            }
            Div(attrs = { classes("modal-body") }) {
                content()
            }
        }
    }
}

@Composable
private fun ConfirmModal(
    title: String,
    message: String,
    confirmText: String,
    isDanger: Boolean = false,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Modal(title = title, onClose = onClose) {
        P { Text(message) }
        Div(attrs = { classes("flex", "justify-end", "gap-sm", "mt-lg") }) {
            Button(attrs = {
                classes("btn", "btn-outline")
                onClick { onClose() }
            }) {
                Text("Cancel")
            }
            Button(attrs = {
                classes("btn", if (isDanger) "btn-danger" else "btn-primary")
                onClick { onConfirm() }
            }) {
                Text(confirmText)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, description: String) {
    Div(attrs = { classes("empty-state") }) {
        Div(attrs = { style { fontSize(64.px) } }) { Text(icon) }
        H3(attrs = { classes("empty-state-title") }) { Text(title) }
        P(attrs = { classes("empty-state-description") }) { Text(description) }
    }
}

// ===== Helper Functions =====

private fun formatDate(isoString: String): String {
    return try {
        val date = js("new Date(isoString)")
        js("date.toLocaleDateString('en-CA', { year: 'numeric', month: 'short', day: 'numeric' })") as String
    } catch (e: Exception) {
        isoString
    }
}

private fun formatDateTime(isoString: String): String {
    return try {
        val date = js("new Date(isoString)")
        js("date.toLocaleString('en-CA', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })") as String
    } catch (e: Exception) {
        isoString
    }
}
