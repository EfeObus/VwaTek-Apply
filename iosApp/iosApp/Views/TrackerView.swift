import SwiftUI
import shared

/**
 * Phase 2: Job Application Tracker View for iOS
 * Displays job applications in Kanban, List, or Calendar view
 */
struct TrackerView: View {
    @StateObject private var viewModel = TrackerViewModelWrapper()
    @State private var showAddSheet = false
    @State private var showFilterSheet = false
    @State private var selectedApplication: JobApplication?
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Stats bar
                if let stats = viewModel.stats {
                    TrackerStatsBar(stats: stats)
                }
                
                // Main content based on view mode
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    switch viewModel.viewMode {
                    case .kanban:
                        KanbanBoardView(
                            columns: viewModel.kanbanColumns,
                            onApplicationTap: { app in
                                selectedApplication = app
                                viewModel.selectApplication(id: app.id)
                            },
                            onStatusChange: { appId, newStatus in
                                viewModel.moveToStatus(applicationId: appId, targetStatus: newStatus)
                            }
                        )
                    case .list:
                        ApplicationListView(
                            applications: viewModel.applications,
                            onApplicationTap: { app in
                                selectedApplication = app
                                viewModel.selectApplication(id: app.id)
                            }
                        )
                    case .calendar:
                        CalendarPlaceholderView()
                    }
                }
            }
            .navigationTitle("Job Tracker")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        // View mode toggle
                        Button(action: {
                            viewModel.cycleViewMode()
                        }) {
                            Image(systemName: viewModel.viewModeIcon)
                        }
                        
                        // Filter button
                        Button(action: {
                            showFilterSheet = true
                        }) {
                            Image(systemName: viewModel.hasActiveFilters ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                        }
                        
                        // Add button
                        Button(action: {
                            showAddSheet = true
                        }) {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            AddApplicationSheet(
                onAdd: { request in
                    viewModel.createApplication(request: request)
                    showAddSheet = false
                }
            )
        }
        .sheet(isPresented: $showFilterSheet) {
            FilterSheet(
                viewModel: viewModel,
                isPresented: $showFilterSheet
            )
        }
        .sheet(item: $selectedApplication) { app in
            if let detail = viewModel.selectedApplicationDetail {
                ApplicationDetailSheet(
                    detail: detail,
                    isLoading: viewModel.isLoadingDetail,
                    onStatusChange: { newStatus, notes in
                        viewModel.updateStatus(id: app.id, newStatus: newStatus, notes: notes)
                    },
                    onAddNote: { content, noteType in
                        viewModel.addNote(applicationId: app.id, content: content, noteType: noteType)
                    },
                    onDelete: {
                        viewModel.deleteApplication(id: app.id)
                        selectedApplication = nil
                    },
                    onDismiss: {
                        selectedApplication = nil
                        viewModel.clearSelectedApplication()
                    }
                )
            }
        }
        .alert("Error", isPresented: $showError) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.error ?? "An unknown error occurred")
        }
        .onChange(of: viewModel.error) { error in
            showError = error != nil
        }
    }
}

// MARK: - Stats Bar

struct TrackerStatsBar: View {
    let stats: TrackerStats
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                StatItem(label: "Total", value: "\(stats.totalApplications)")
                StatItem(label: "Applied", value: "\(stats.appliedCount)")
                StatItem(label: "Interviews", value: "\(stats.interviewCount)")
                StatItem(label: "Offers", value: "\(stats.offerCount)")
                StatItem(label: "Interview Rate", value: "\(Int(stats.interviewRate * 100))%")
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
        }
        .background(Color(.systemGray6))
    }
}

struct StatItem: View {
    let label: String
    let value: String
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Kanban Board

struct KanbanBoardView: View {
    let columns: [ApplicationStatus: [JobApplication]]
    let onApplicationTap: (JobApplication) -> Void
    let onStatusChange: (String, ApplicationStatus) -> Void
    
    private let activeStatuses: [ApplicationStatus] = [
        .saved, .applied, .screening, .phoneInterview,
        .technicalInterview, .onsiteInterview, .finalInterview,
        .offerReceived, .negotiating
    ]
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .top, spacing: 12) {
                ForEach(activeStatuses, id: \.self) { status in
                    KanbanColumnView(
                        status: status,
                        applications: columns[status] ?? [],
                        onApplicationTap: onApplicationTap
                    )
                }
            }
            .padding()
        }
    }
}

struct KanbanColumnView: View {
    let status: ApplicationStatus
    let applications: [JobApplication]
    let onApplicationTap: (JobApplication) -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text(status.displayName)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Spacer()
                Text("\(applications.count)")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(status.color.opacity(0.3))
                    .clipShape(Capsule())
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(status.color.opacity(0.2))
            
            // Applications
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(applications, id: \.id) { application in
                        ApplicationCardView(application: application)
                            .onTapGesture {
                                onApplicationTap(application)
                            }
                    }
                    
                    if applications.isEmpty {
                        Text("No applications")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.vertical, 40)
                    }
                }
                .padding(8)
            }
        }
        .frame(width: 260)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

struct ApplicationCardView: View {
    let application: JobApplication
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Company
            HStack(spacing: 8) {
                if let logo = application.companyLogo {
                    AsyncImage(url: URL(string: logo)) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        CompanyInitialView(name: application.companyName)
                    }
                    .frame(width: 24, height: 24)
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                } else {
                    CompanyInitialView(name: application.companyName)
                        .frame(width: 24, height: 24)
                }
                
                Text(application.companyName)
                    .font(.caption)
                    .lineLimit(1)
            }
            
            // Job title
            Text(application.jobTitle)
                .font(.subheadline)
                .fontWeight(.medium)
                .lineLimit(2)
            
            // Location
            if !application.locationDisplay.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "mappin")
                        .font(.caption2)
                    Text(application.locationDisplay)
                        .font(.caption)
                        .lineLimit(1)
                }
                .foregroundColor(.secondary)
            }
            
            // Salary
            if !application.salaryDisplay.isEmpty {
                Text(application.salaryDisplay)
                    .font(.caption)
                    .foregroundColor(.blue)
            }
            
            // Tags
            HStack(spacing: 4) {
                if application.isRemote {
                    TagView(text: "Remote")
                }
                if application.requiresWorkPermit {
                    TagView(text: "Work Permit")
                }
            }
        }
        .padding(12)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(8)
    }
}

struct CompanyInitialView: View {
    let name: String
    
    var body: some View {
        Text(String(name.prefix(1)).uppercased())
            .font(.caption2)
            .fontWeight(.semibold)
            .frame(width: 24, height: 24)
            .background(Color.blue.opacity(0.2))
            .cornerRadius(4)
    }
}

struct TagView: View {
    let text: String
    
    var body: some View {
        Text(text)
            .font(.system(size: 10))
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color(.systemGray5))
            .cornerRadius(4)
    }
}

// MARK: - List View

struct ApplicationListView: View {
    let applications: [JobApplication]
    let onApplicationTap: (JobApplication) -> Void
    
    var body: some View {
        if applications.isEmpty {
            VStack(spacing: 16) {
                Image(systemName: "briefcase")
                    .font(.system(size: 48))
                    .foregroundColor(.secondary)
                Text("No applications yet")
                    .font(.headline)
                Text("Tap + to add your first job application")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List(applications, id: \.id) { application in
                ApplicationListRow(application: application)
                    .onTapGesture {
                        onApplicationTap(application)
                    }
            }
            .listStyle(.plain)
        }
    }
}

struct ApplicationListRow: View {
    let application: JobApplication
    
    var body: some View {
        HStack(spacing: 12) {
            // Company logo
            if let logo = application.companyLogo {
                AsyncImage(url: URL(string: logo)) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    CompanyInitialView(name: application.companyName)
                }
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                Text(String(application.companyName.prefix(2)).uppercased())
                    .font(.headline)
                    .frame(width: 48, height: 48)
                    .background(Color.blue.opacity(0.2))
                    .cornerRadius(8)
            }
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(application.jobTitle)
                    .font(.headline)
                    .lineLimit(1)
                Text(application.companyName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                HStack(spacing: 8) {
                    if !application.locationDisplay.isEmpty {
                        HStack(spacing: 2) {
                            Image(systemName: "mappin")
                                .font(.caption2)
                            Text(application.locationDisplay)
                        }
                        .font(.caption)
                        .foregroundColor(.secondary)
                    }
                    
                    if !application.salaryDisplay.isEmpty {
                        Text("•")
                            .foregroundColor(.secondary)
                        Text(application.salaryDisplay)
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                }
            }
            
            Spacer()
            
            // Status
            VStack(alignment: .trailing, spacing: 4) {
                Text(application.status.displayName)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(application.status.color.opacity(0.2))
                    .foregroundColor(application.status.color)
                    .cornerRadius(4)
                
                if let appliedAt = application.appliedAt {
                    Text(formatDate(appliedAt))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Calendar Placeholder

struct CalendarPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "calendar")
                .font(.system(size: 64))
                .foregroundColor(.blue)
            Text("Calendar View")
                .font(.title2)
                .fontWeight(.semibold)
            Text("Coming soon - View interviews and deadlines")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Add Application Sheet

struct AddApplicationSheet: View {
    @Environment(\.dismiss) var dismiss
    let onAdd: (CreateJobApplicationRequest) -> Void
    
    @State private var jobTitle = ""
    @State private var companyName = ""
    @State private var jobUrl = ""
    @State private var city = ""
    @State private var selectedProvince: CanadianProvince?
    @State private var isRemote = false
    @State private var salaryMin = ""
    @State private var salaryMax = ""
    @State private var selectedSource: JobBoardSource?
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Required") {
                    TextField("Job Title", text: $jobTitle)
                    TextField("Company Name", text: $companyName)
                }
                
                Section("Job Details") {
                    TextField("Job URL", text: $jobUrl)
                        .textContentType(.URL)
                        .autocapitalization(.none)
                    
                    Picker("Source", selection: $selectedSource) {
                        Text("Select Source").tag(nil as JobBoardSource?)
                        ForEach(JobBoardSource.allCases, id: \.self) { source in
                            Text(source.displayName).tag(source as JobBoardSource?)
                        }
                    }
                }
                
                Section("Location") {
                    TextField("City", text: $city)
                    
                    Picker("Province", selection: $selectedProvince) {
                        Text("Select Province").tag(nil as CanadianProvince?)
                        ForEach(CanadianProvince.allCases, id: \.self) { province in
                            Text("\(province.code) - \(province.displayName)").tag(province as CanadianProvince?)
                        }
                    }
                    
                    Toggle("Remote Position", isOn: $isRemote)
                }
                
                Section("Salary (CAD)") {
                    HStack {
                        TextField("Min", text: $salaryMin)
                            .keyboardType(.numberPad)
                        Text("-")
                        TextField("Max", text: $salaryMax)
                            .keyboardType(.numberPad)
                    }
                }
            }
            .navigationTitle("Add Application")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        let request = CreateJobApplicationRequest(
                            jobTitle: jobTitle,
                            companyName: companyName,
                            jobUrl: jobUrl.isEmpty ? nil : jobUrl,
                            jobBoardSource: selectedSource,
                            city: city.isEmpty ? nil : city,
                            province: selectedProvince,
                            isRemote: isRemote,
                            salaryMin: Int(salaryMin),
                            salaryMax: Int(salaryMax)
                        )
                        onAdd(request)
                    }
                    .disabled(jobTitle.isEmpty || companyName.isEmpty)
                }
            }
        }
    }
}

// MARK: - Filter Sheet

struct FilterSheet: View {
    @ObservedObject var viewModel: TrackerViewModelWrapper
    @Binding var isPresented: Bool
    @State private var searchText = ""
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Search") {
                    TextField("Search jobs...", text: $searchText)
                        .onChange(of: searchText) { newValue in
                            viewModel.setSearchQuery(query: newValue.isEmpty ? nil : newValue)
                        }
                }
                
                Section("Status") {
                    Picker("Status", selection: Binding(
                        get: { viewModel.filterStatus },
                        set: { viewModel.setFilterStatus(status: $0) }
                    )) {
                        Text("All").tag(nil as ApplicationStatus?)
                        ForEach(ApplicationStatus.allCases, id: \.self) { status in
                            Text(status.displayName).tag(status as ApplicationStatus?)
                        }
                    }
                }
                
                Section("Province") {
                    Picker("Province", selection: Binding(
                        get: { viewModel.filterProvince },
                        set: { viewModel.setFilterProvince(province: $0) }
                    )) {
                        Text("All").tag(nil as CanadianProvince?)
                        ForEach(CanadianProvince.allCases, id: \.self) { province in
                            Text(province.code).tag(province as CanadianProvince?)
                        }
                    }
                }
                
                Section {
                    Button("Clear All Filters") {
                        searchText = ""
                        viewModel.clearFilters()
                    }
                }
            }
            .navigationTitle("Filter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        isPresented = false
                    }
                }
            }
        }
    }
}

// MARK: - Application Detail Sheet

struct ApplicationDetailSheet: View {
    let detail: JobApplicationDetail
    let isLoading: Bool
    let onStatusChange: (ApplicationStatus, String?) -> Void
    let onAddNote: (String, NoteType) -> Void
    let onDelete: () -> Void
    let onDismiss: () -> Void
    
    @State private var selectedTab = 0
    @State private var showStatusChangeSheet = false
    @State private var showAddNoteSheet = false
    @State private var showDeleteConfirmation = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    Text(detail.application.jobTitle)
                        .font(.title2)
                        .fontWeight(.bold)
                    Text(detail.application.companyName)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    HStack {
                        Text(detail.application.status.displayName)
                            .font(.caption)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(detail.application.status.color.opacity(0.2))
                            .foregroundColor(detail.application.status.color)
                            .cornerRadius(6)
                        
                        Spacer()
                        
                        Button("Change Status") {
                            showStatusChangeSheet = true
                        }
                        .font(.subheadline)
                    }
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                
                // Tabs
                Picker("Tab", selection: $selectedTab) {
                    Text("Details").tag(0)
                    Text("Notes").tag(1)
                    Text("Reminders").tag(2)
                    Text("History").tag(3)
                }
                .pickerStyle(.segmented)
                .padding()
                
                // Tab content
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    TabView(selection: $selectedTab) {
                        DetailsTab(application: detail.application)
                            .tag(0)
                        NotesTab(notes: detail.notes, onAddNote: { showAddNoteSheet = true })
                            .tag(1)
                        RemindersTab(reminders: detail.reminders)
                            .tag(2)
                        HistoryTab(history: detail.statusHistory)
                            .tag(3)
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        onDismiss()
                    }
                }
                ToolbarItem(placement: .destructiveAction) {
                    Button(role: .destructive) {
                        showDeleteConfirmation = true
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }
        }
        .sheet(isPresented: $showStatusChangeSheet) {
            StatusChangeSheet(
                currentStatus: detail.application.status,
                onConfirm: { newStatus, notes in
                    onStatusChange(newStatus, notes)
                    showStatusChangeSheet = false
                }
            )
        }
        .sheet(isPresented: $showAddNoteSheet) {
            AddNoteSheet(onAdd: { content, noteType in
                onAddNote(content, noteType)
                showAddNoteSheet = false
            })
        }
        .alert("Delete Application?", isPresented: $showDeleteConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                onDelete()
            }
        } message: {
            Text("Are you sure you want to delete this job application? This action cannot be undone.")
        }
    }
}

struct DetailsTab: View {
    let application: JobApplication
    
    var body: some View {
        List {
            if !application.locationDisplay.isEmpty {
                DetailRow(icon: "mappin", label: "Location", value: application.locationDisplay)
            }
            if !application.salaryDisplay.isEmpty {
                DetailRow(icon: "dollarsign.circle", label: "Salary", value: application.salaryDisplay)
            }
            if let source = application.jobBoardSource {
                DetailRow(icon: "link", label: "Source", value: source.displayName)
            }
            if let noc = application.nocCode {
                DetailRow(icon: "number", label: "NOC Code", value: noc)
            }
            if let appliedAt = application.appliedAt {
                DetailRow(icon: "calendar", label: "Applied", value: formatDate(appliedAt))
            }
            if application.requiresWorkPermit {
                DetailRow(icon: "doc.badge.clock", label: "Work Permit", value: "Required")
            }
            if application.isLmiaRequired {
                DetailRow(icon: "doc.text", label: "LMIA", value: "Required")
            }
            if let contact = application.contactName {
                DetailRow(icon: "person", label: "Contact", value: contact)
            }
            if let email = application.contactEmail {
                DetailRow(icon: "envelope", label: "Email", value: email)
            }
        }
        .listStyle(.insetGrouped)
    }
}

struct DetailRow: View {
    let icon: String
    let label: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.secondary)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(value)
                    .font(.body)
            }
        }
    }
}

struct NotesTab: View {
    let notes: [ApplicationNote]
    let onAddNote: () -> Void
    
    var body: some View {
        VStack {
            Button(action: onAddNote) {
                Label("Add Note", systemImage: "plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .padding()
            
            if notes.isEmpty {
                Text("No notes yet")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(notes, id: \.id) { note in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(note.noteType.displayName)
                                .font(.caption)
                                .foregroundColor(.blue)
                            Spacer()
                            Text(formatDate(note.createdAt))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        Text(note.content)
                            .font(.body)
                    }
                    .padding(.vertical, 4)
                }
                .listStyle(.plain)
            }
        }
    }
}

struct RemindersTab: View {
    let reminders: [ApplicationReminder]
    
    var body: some View {
        if reminders.isEmpty {
            Text("No reminders")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List(reminders, id: \.id) { reminder in
                HStack(spacing: 12) {
                    Image(systemName: reminder.isCompleted ? "checkmark.circle.fill" : "bell")
                        .foregroundColor(reminder.isCompleted ? .green : .secondary)
                    VStack(alignment: .leading) {
                        Text(reminder.title)
                            .font(.body)
                        Text(formatDateTime(reminder.reminderAt))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .listStyle(.plain)
        }
    }
}

struct HistoryTab: View {
    let history: [StatusChange]
    
    var body: some View {
        if history.isEmpty {
            Text("No status history")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List(history, id: \.id) { change in
                HStack(spacing: 12) {
                    Image(systemName: "arrow.right.circle")
                        .foregroundColor(.secondary)
                    VStack(alignment: .leading) {
                        if let from = change.fromStatus {
                            Text("\(from.displayName) → \(change.toStatus.displayName)")
                        } else {
                            Text("Created as \(change.toStatus.displayName)")
                        }
                        Text(formatDateTime(change.changedAt))
                            .font(.caption)
                            .foregroundColor(.secondary)
                        if let notes = change.notes {
                            Text(notes)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }
}

struct StatusChangeSheet: View {
    @Environment(\.dismiss) var dismiss
    let currentStatus: ApplicationStatus
    let onConfirm: (ApplicationStatus, String?) -> Void
    
    @State private var selectedStatus: ApplicationStatus
    @State private var notes = ""
    
    init(currentStatus: ApplicationStatus, onConfirm: @escaping (ApplicationStatus, String?) -> Void) {
        self.currentStatus = currentStatus
        self.onConfirm = onConfirm
        self._selectedStatus = State(initialValue: currentStatus)
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Select Status") {
                    ForEach(ApplicationStatus.allCases, id: \.self) { status in
                        Button(action: { selectedStatus = status }) {
                            HStack {
                                Circle()
                                    .fill(status.color)
                                    .frame(width: 12, height: 12)
                                Text(status.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedStatus == status {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
                
                Section("Notes (optional)") {
                    TextEditor(text: $notes)
                        .frame(minHeight: 100)
                }
            }
            .navigationTitle("Change Status")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Update") {
                        onConfirm(selectedStatus, notes.isEmpty ? nil : notes)
                    }
                }
            }
        }
    }
}

struct AddNoteSheet: View {
    @Environment(\.dismiss) var dismiss
    let onAdd: (String, NoteType) -> Void
    
    @State private var content = ""
    @State private var selectedType: NoteType = .general
    
    var body: some View {
        NavigationStack {
            Form {
                Section("Note Type") {
                    Picker("Type", selection: $selectedType) {
                        ForEach(NoteType.allCases, id: \.self) { type in
                            Text(type.displayName).tag(type)
                        }
                    }
                }
                
                Section("Content") {
                    TextEditor(text: $content)
                        .frame(minHeight: 150)
                }
            }
            .navigationTitle("Add Note")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(content, selectedType)
                    }
                    .disabled(content.isEmpty)
                }
            }
        }
    }
}

// MARK: - Helper Functions

private func formatDate(_ isoString: String) -> String {
    let formatter = ISO8601DateFormatter()
    if let date = formatter.date(from: isoString) {
        let displayFormatter = DateFormatter()
        displayFormatter.dateStyle = .medium
        return displayFormatter.string(from: date)
    }
    return isoString
}

private func formatDateTime(_ isoString: String) -> String {
    let formatter = ISO8601DateFormatter()
    if let date = formatter.date(from: isoString) {
        let displayFormatter = DateFormatter()
        displayFormatter.dateStyle = .medium
        displayFormatter.timeStyle = .short
        return displayFormatter.string(from: date)
    }
    return isoString
}

// MARK: - Preview

#Preview {
    TrackerView()
}
