import Foundation
import shared
import Combine
import SwiftUI

/// Wrapper class to make Kotlin TrackerViewModel observable in SwiftUI
@MainActor
class TrackerViewModelWrapper: ObservableObject {
    private let viewModel: TrackerViewModel
    
    @Published var applications: [JobApplication_] = []
    @Published var kanbanColumns: [ApplicationStatus: [JobApplication_]] = [:]
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
    
    private func convertKanbanColumns(_ kotlinMap: [ApplicationStatus: [JobApplication_]]) -> [ApplicationStatus: [JobApplication_]] {
        var result: [ApplicationStatus: [JobApplication_]] = [:]
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
        viewModel.onIntent(intent: TrackerIntentSetViewMode(mode: nextMode))
    }
    
    func loadApplications() {
        viewModel.onIntent(intent: TrackerIntentLoadApplications())
    }
    
    func refreshApplications() {
        viewModel.onIntent(intent: TrackerIntentRefreshApplications())
    }
    
    func selectApplication(id: String) {
        viewModel.onIntent(intent: TrackerIntentSelectApplication(id: id))
    }
    
    func clearSelectedApplication() {
        viewModel.onIntent(intent: TrackerIntentClearSelectedApplication())
    }
    
    func createApplication(request: CreateJobApplicationRequest) {
        viewModel.onIntent(intent: TrackerIntentCreateApplication(request: request))
    }
    
    func updateApplication(id: String, request: UpdateJobApplicationRequest) {
        viewModel.onIntent(intent: TrackerIntentUpdateApplication(id: id, request: request))
    }
    
    func updateStatus(id: String, newStatus: ApplicationStatus, notes: String?) {
        viewModel.onIntent(intent: TrackerIntentUpdateStatus(id: id, newStatus: newStatus, notes: notes))
    }
    
    func deleteApplication(id: String) {
        viewModel.onIntent(intent: TrackerIntentDeleteApplication(id: id))
    }
    
    func addNote(applicationId: String, content: String, noteType: NoteType) {
        viewModel.onIntent(intent: TrackerIntentAddNote(applicationId: applicationId, content: content, noteType: noteType))
    }
    
    func addReminder(applicationId: String, reminder: CreateReminderRequest) {
        viewModel.onIntent(intent: TrackerIntentAddReminder(applicationId: applicationId, reminder: reminder))
    }
    
    func addInterview(applicationId: String, interview: CreateInterviewRequest) {
        viewModel.onIntent(intent: TrackerIntentAddInterview(applicationId: applicationId, interview: interview))
    }
    
    func setFilterStatus(status: ApplicationStatus?) {
        viewModel.onIntent(intent: TrackerIntentSetFilterStatus(status: status))
    }
    
    func setFilterSource(source: JobBoardSource?) {
        viewModel.onIntent(intent: TrackerIntentSetFilterSource(source: source))
    }
    
    func setFilterProvince(province: CanadianProvince?) {
        viewModel.onIntent(intent: TrackerIntentSetFilterProvince(province: province))
    }
    
    func setSearchQuery(query: String?) {
        viewModel.onIntent(intent: TrackerIntentSetSearchQuery(query: query))
    }
    
    func clearFilters() {
        viewModel.onIntent(intent: TrackerIntentClearFilters())
    }
    
    func moveToStatus(applicationId: String, targetStatus: ApplicationStatus) {
        viewModel.onIntent(intent: TrackerIntentMoveToStatus(applicationId: applicationId, targetStatus: targetStatus))
    }
    
    func clearError() {
        viewModel.onIntent(intent: TrackerIntentClearError())
    }
}

// MARK: - Swift Extensions for shared types

extension ApplicationStatus {
    static var allCases: [ApplicationStatus] = [
        .saved, .applied, .viewed, .phoneScreen, .interview,
        .assessment, .finalRound, .offer, .negotiating,
        .accepted, .rejected, .withdrawn, .noResponse
    ]
    
    var color: Color {
        switch self {
        case .saved: return Color.gray
        case .applied: return Color.blue
        case .viewed: return Color.cyan
        case .phoneScreen: return Color.purple
        case .interview: return Color.orange
        case .assessment: return Color.pink
        case .finalRound: return Color.indigo
        case .offer: return Color.green
        case .negotiating: return Color.yellow
        case .accepted: return Color(red: 0, green: 0.5, blue: 0)
        case .rejected: return Color.red
        case .withdrawn: return Color(red: 0.6, green: 0.6, blue: 0.6)
        case .noResponse: return Color.brown
        default: return Color.gray
        }
    }
}

extension JobBoardSource {
    static var allCases: [JobBoardSource] = [
        .indeed, .indeedCa, .linkedin, .jobBank, .glassdoor,
        .glassdoorCa, .workday, .monster, .workopolis, .companySite,
        .referral, .networking, .recruiter, .manual
    ]
}

extension CanadianProvince {
    static var allCases: [CanadianProvince] = [
        .on, .qc, .bc, .ab, .mb, .sk, .ns, .nb, .nl, .pe, .nt, .nu, .yt
    ]
}

extension NoteType {
    static var allCases: [NoteType] = [
        .general, .interview, .followUp, .research, .feedback, .question
    ]
}

extension JobApplication_: Identifiable {}
extension ApplicationNote: Identifiable {}
extension ApplicationReminder: Identifiable {}
extension StatusChange_: Identifiable {}
