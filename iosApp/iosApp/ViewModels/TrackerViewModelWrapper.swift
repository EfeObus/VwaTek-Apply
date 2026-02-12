import Foundation
import shared
import Combine
import SwiftUI

/// Wrapper class to make Kotlin TrackerViewModel observable in SwiftUI
@MainActor
class TrackerViewModelWrapper: ObservableObject {
    private let viewModel: TrackerViewModel
    
    @Published var applications: [JobApplication] = []
    @Published var kanbanColumns: [ApplicationStatus: [JobApplication]] = [:]
    @Published var selectedApplicationDetail: JobApplicationDetail? = nil
    @Published var stats: TrackerStats? = nil
    @Published var viewMode: ViewMode = .kanban
    @Published var filterStatus: ApplicationStatus? = nil
    @Published var filterSource: JobBoardSource? = nil
    @Published var filterProvince: CanadianProvince? = nil
    @Published var searchQuery: String? = nil
    @Published var isLoading: Bool = true
    @Published var isLoadingDetail: Bool = false
    @Published var isSubmitting: Bool = false
    @Published var error: String? = nil
    
    private var stateWatcher: Closeable?
    
    enum ViewMode {
        case kanban
        case list
        case calendar
    }
    
    var viewModeIcon: String {
        switch viewMode {
        case .kanban: return "rectangle.split.3x1"
        case .list: return "list.bullet"
        case .calendar: return "calendar"
        }
    }
    
    var hasActiveFilters: Bool {
        filterStatus != nil || filterSource != nil || filterProvince != nil || (searchQuery != nil && !searchQuery!.isEmpty)
    }
    
    init() {
        // Get TrackerViewModel from Koin
        self.viewModel = KoinHelperKt.getTrackerViewModel()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func observeState() {
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let trackerState = state as? TrackerState else { return }
            Task { @MainActor in
                self.applications = trackerState.applications
                self.kanbanColumns = self.convertKanbanColumns(trackerState.kanbanColumns)
                self.selectedApplicationDetail = trackerState.selectedApplication
                self.stats = trackerState.stats
                self.viewMode = self.convertViewMode(trackerState.viewMode)
                self.filterStatus = trackerState.filterStatus
                self.filterSource = trackerState.filterSource
                self.filterProvince = trackerState.filterProvince
                self.searchQuery = trackerState.searchQuery
                self.isLoading = trackerState.isLoading
                self.isLoadingDetail = trackerState.isLoadingDetail
                self.isSubmitting = trackerState.isSubmitting
                self.error = trackerState.error
            }
        }
    }
    
    private func convertKanbanColumns(_ kotlinMap: [ApplicationStatus: [JobApplication]]) -> [ApplicationStatus: [JobApplication]] {
        var result: [ApplicationStatus: [JobApplication]] = [:]
        for (key, value) in kotlinMap {
            result[key] = value
        }
        return result
    }
    
    private func convertViewMode(_ mode: TrackerViewMode) -> ViewMode {
        switch mode {
        case .kanban: return .kanban
        case .list: return .list
        case .calendar: return .calendar
        default: return .kanban
        }
    }
    
    func cycleViewMode() {
        let nextMode: TrackerViewMode
        switch viewMode {
        case .kanban: nextMode = .list
        case .list: nextMode = .calendar
        case .calendar: nextMode = .kanban
        }
        viewModel.onIntent(intent: TrackerIntent.SetViewMode(mode: nextMode))
    }
    
    func loadApplications() {
        viewModel.onIntent(intent: TrackerIntent.LoadApplications.shared)
    }
    
    func refreshApplications() {
        viewModel.onIntent(intent: TrackerIntent.RefreshApplications.shared)
    }
    
    func selectApplication(id: String) {
        viewModel.onIntent(intent: TrackerIntent.SelectApplication(id: id))
    }
    
    func clearSelectedApplication() {
        viewModel.onIntent(intent: TrackerIntent.ClearSelectedApplication.shared)
    }
    
    func createApplication(request: CreateJobApplicationRequest) {
        viewModel.onIntent(intent: TrackerIntent.CreateApplication(request: request))
    }
    
    func updateApplication(id: String, request: UpdateJobApplicationRequest) {
        viewModel.onIntent(intent: TrackerIntent.UpdateApplication(id: id, request: request))
    }
    
    func updateStatus(id: String, newStatus: ApplicationStatus, notes: String?) {
        viewModel.onIntent(intent: TrackerIntent.UpdateStatus(id: id, newStatus: newStatus, notes: notes))
    }
    
    func deleteApplication(id: String) {
        viewModel.onIntent(intent: TrackerIntent.DeleteApplication(id: id))
    }
    
    func addNote(applicationId: String, content: String, noteType: NoteType) {
        viewModel.onIntent(intent: TrackerIntent.AddNote(applicationId: applicationId, content: content, noteType: noteType))
    }
    
    func addReminder(applicationId: String, reminder: CreateReminderRequest) {
        viewModel.onIntent(intent: TrackerIntent.AddReminder(applicationId: applicationId, reminder: reminder))
    }
    
    func addInterview(applicationId: String, interview: CreateInterviewRequest) {
        viewModel.onIntent(intent: TrackerIntent.AddInterview(applicationId: applicationId, interview: interview))
    }
    
    func setFilterStatus(status: ApplicationStatus?) {
        viewModel.onIntent(intent: TrackerIntent.SetFilterStatus(status: status))
    }
    
    func setFilterSource(source: JobBoardSource?) {
        viewModel.onIntent(intent: TrackerIntent.SetFilterSource(source: source))
    }
    
    func setFilterProvince(province: CanadianProvince?) {
        viewModel.onIntent(intent: TrackerIntent.SetFilterProvince(province: province))
    }
    
    func setSearchQuery(query: String?) {
        viewModel.onIntent(intent: TrackerIntent.SetSearchQuery(query: query))
    }
    
    func clearFilters() {
        viewModel.onIntent(intent: TrackerIntent.ClearFilters.shared)
    }
    
    func moveToStatus(applicationId: String, targetStatus: ApplicationStatus) {
        viewModel.onIntent(intent: TrackerIntent.MoveToStatus(applicationId: applicationId, targetStatus: targetStatus))
    }
    
    func clearError() {
        viewModel.onIntent(intent: TrackerIntent.ClearError.shared)
    }
}

// MARK: - Swift Extensions for shared types

extension ApplicationStatus: CaseIterable {
    public static var allCases: [ApplicationStatus] = [
        .saved, .applied, .screening, .phoneInterview, .technicalInterview,
        .onsiteInterview, .finalInterview, .offerReceived, .negotiating,
        .accepted, .rejected, .withdrawn, .noResponse
    ]
    
    var color: Color {
        switch self {
        case .saved: return Color.gray
        case .applied: return Color.blue
        case .screening: return Color.cyan
        case .phoneInterview: return Color.purple
        case .technicalInterview: return Color.orange
        case .onsiteInterview: return Color.pink
        case .finalInterview: return Color.indigo
        case .offerReceived: return Color.green
        case .negotiating: return Color.yellow
        case .accepted: return Color(red: 0, green: 0.5, blue: 0)
        case .rejected: return Color.red
        case .withdrawn: return Color(red: 0.6, green: 0.6, blue: 0.6)
        case .noResponse: return Color.brown
        default: return Color.gray
        }
    }
}

extension JobBoardSource: CaseIterable {
    public static var allCases: [JobBoardSource] = [
        .indeed, .linkedin, .glassdoor, .zipRecruiter, .monster,
        .jobBank, .workopolis, .simplyHired, .careerBuilder, .dice,
        .angelList, .companyWebsite, .referral, .other
    ]
}

extension CanadianProvince: CaseIterable {
    public static var allCases: [CanadianProvince] = [
        .ontario, .quebec, .britishColumbia, .alberta, .manitoba,
        .saskatchewan, .novascotia, .newBrunswick, .newfoundlandAndLabrador,
        .princeEdwardIsland, .northwestTerritories, .nunavut, .yukon
    ]
}

extension NoteType: CaseIterable {
    public static var allCases: [NoteType] = [
        .general, .interviewPrep, .companyResearch, .followUp, .feedback
    ]
}

extension JobApplication: Identifiable {}
extension ApplicationNote: Identifiable {}
extension ApplicationReminder: Identifiable {}
extension StatusChange: Identifiable {}
