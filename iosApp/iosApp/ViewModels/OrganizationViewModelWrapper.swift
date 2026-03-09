import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin OrganizationViewModel observable in SwiftUI
@MainActor
class OrganizationViewModelWrapper: ObservableObject {
    private let viewModel: OrganizationViewModel
    
    @Published var organizations: [OrganizationDto] = []
    @Published var selectedOrganization: OrganizationDto? = nil
    @Published var members: [MemberDto] = []
    @Published var invitations: [InvitationDto] = []
    @Published var templates: [TemplateDto] = []
    @Published var isLoading: Bool = false
    @Published var isCreating: Bool = false
    @Published var isLoadingMembers: Bool = false
    @Published var isInviting: Bool = false
    @Published var isAcceptingInvitation: Bool = false
    @Published var isLoadingTemplates: Bool = false
    @Published var error: String? = nil
    
    private var stateWatcher: Closeable?
    
    init() {
        self.viewModel = KoinHelperKt.getOrganizationViewModel()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func observeState() {
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let orgState = state as? OrganizationState else { return }
            Task { @MainActor in
                self.organizations = orgState.organizations
                self.selectedOrganization = orgState.selectedOrganization
                self.members = orgState.members
                self.invitations = orgState.invitations
                self.templates = orgState.templates
                self.isLoading = orgState.isLoading
                self.isCreating = orgState.isCreating
                self.isLoadingMembers = orgState.isLoadingMembers
                self.isInviting = orgState.isInviting
                self.isAcceptingInvitation = orgState.isAcceptingInvitation
                self.isLoadingTemplates = orgState.isLoadingTemplates
                self.error = orgState.error
            }
        }
    }
    
    // MARK: - Actions
    
    func loadOrganizations() {
        viewModel.onIntent(intent: OrganizationIntent.LoadOrganizations.shared)
    }
    
    func selectOrganization(orgId: String) {
        viewModel.onIntent(intent: OrganizationIntent.SelectOrganization(orgId: orgId))
    }
    
    func clearSelectedOrganization() {
        viewModel.onIntent(intent: OrganizationIntent.ClearSelectedOrganization.shared)
    }
    
    func createOrganization(name: String, description: String?, industry: String?) {
        viewModel.onIntent(intent: OrganizationIntent.CreateOrganization(name: name, description: description, industry: industry))
    }
    
    func loadMembers(orgId: String) {
        viewModel.onIntent(intent: OrganizationIntent.LoadMembers(orgId: orgId))
    }
    
    func inviteMember(orgId: String, email: String, role: String) {
        viewModel.onIntent(intent: OrganizationIntent.InviteMember(orgId: orgId, email: email, role: role))
    }
    
    func removeMember(orgId: String, memberId: String) {
        viewModel.onIntent(intent: OrganizationIntent.RemoveMember(orgId: orgId, memberId: memberId))
    }
    
    func acceptInvitation(token: String) {
        viewModel.onIntent(intent: OrganizationIntent.AcceptInvitation(token: token))
    }
    
    func loadTemplates(orgId: String) {
        viewModel.onIntent(intent: OrganizationIntent.LoadTemplates(orgId: orgId))
    }
    
    func clearError() {
        viewModel.onIntent(intent: OrganizationIntent.ClearError.shared)
    }
}
