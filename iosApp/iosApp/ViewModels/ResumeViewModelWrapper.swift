import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin ResumeViewModel observable in SwiftUI
@MainActor
class ResumeViewModelWrapper: ObservableObject {
    private let viewModel: ResumeViewModel
    
    @Published var resumes: [Resume] = []
    @Published var selectedResume: Resume? = nil
    @Published var analysis: ResumeAnalysis? = nil
    @Published var atsAnalysis: ATSAnalysis? = nil
    @Published var sectionRewriteResult: SectionRewriteResult? = nil
    @Published var optimizedContent: String? = nil
    @Published var versionHistory: [ResumeVersion] = []
    @Published var isLoading: Bool = true
    @Published var isAnalyzing: Bool = false
    @Published var isOptimizing: Bool = false
    @Published var isATSAnalyzing: Bool = false
    @Published var isRewritingSection: Bool = false
    @Published var isLoadingVersions: Bool = false
    @Published var isRestoringVersion: Bool = false
    @Published var versionRestoreSuccess: Bool = false
    @Published var error: String? = nil
    
    private var stateWatcher: Closeable?
    
    init() {
        // Get ResumeViewModel from Koin
        self.viewModel = KoinHelperKt.getResumeViewModel()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func observeState() {
        // Use FlowExtensionsKt.watch to observe StateFlow
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let resumeState = state as? ResumeState else { return }
            Task { @MainActor in
                self.resumes = resumeState.resumes
                self.selectedResume = resumeState.selectedResume
                self.analysis = resumeState.analysis
                self.atsAnalysis = resumeState.atsAnalysis
                self.sectionRewriteResult = resumeState.sectionRewriteResult
                self.optimizedContent = resumeState.optimizedContent
                self.versionHistory = resumeState.versionHistory
                self.isLoading = resumeState.isLoading
                self.isAnalyzing = resumeState.isAnalyzing
                self.isOptimizing = resumeState.isOptimizing
                self.isATSAnalyzing = resumeState.isATSAnalyzing
                self.isRewritingSection = resumeState.isRewritingSection
                self.isLoadingVersions = resumeState.isLoadingVersions
                self.isRestoringVersion = resumeState.isRestoringVersion
                self.versionRestoreSuccess = resumeState.versionRestoreSuccess
                self.error = resumeState.error
            }
        }
    }
    
    func loadResumes() {
        viewModel.onIntent(intent: ResumeIntent.LoadResumes.shared)
    }
    
    func setCurrentUserId(_ userId: String?) {
        viewModel.onIntent(intent: ResumeIntent.SetCurrentUserId(userId: userId))
    }
    
    func selectResume(id: String?) {
        viewModel.onIntent(intent: ResumeIntent.SelectResume(id: id))
    }
    
    func createResume(name: String, content: String, industry: String?) {
        viewModel.onIntent(intent: ResumeIntent.CreateResume(name: name, content: content, industry: industry))
    }
    
    func updateResume(_ resume: Resume) {
        viewModel.onIntent(intent: ResumeIntent.UpdateResume(resume: resume))
    }
    
    func deleteResume(id: String) {
        viewModel.onIntent(intent: ResumeIntent.DeleteResume(id: id))
    }
    
    func analyzeResume(_ resume: Resume, jobDescription: String) {
        viewModel.onIntent(intent: ResumeIntent.AnalyzeResume(resume: resume, jobDescription: jobDescription))
    }
    
    func optimizeResume(resumeContent: String, jobDescription: String) {
        viewModel.onIntent(intent: ResumeIntent.OptimizeResume(resumeContent: resumeContent, jobDescription: jobDescription))
    }
    
    func performATSAnalysis(_ resume: Resume, jobDescription: String? = nil) {
        viewModel.onIntent(intent: ResumeIntent.PerformATSAnalysis(resume: resume, jobDescription: jobDescription))
    }
    
    func rewriteSection(sectionType: String, sectionContent: String, targetRole: String?, targetIndustry: String?, style: String) {
        viewModel.onIntent(intent: ResumeIntent.RewriteSection(
            sectionType: sectionType,
            sectionContent: sectionContent,
            targetRole: targetRole,
            targetIndustry: targetIndustry,
            style: style
        ))
    }
    
    func clearError() {
        viewModel.onIntent(intent: ResumeIntent.ClearError.shared)
    }
    
    func clearAnalysis() {
        viewModel.onIntent(intent: ResumeIntent.ClearAnalysis.shared)
    }
    
    func clearOptimizedContent() {
        viewModel.onIntent(intent: ResumeIntent.ClearOptimizedContent.shared)
    }
    
    func clearATSAnalysis() {
        viewModel.onIntent(intent: ResumeIntent.ClearATSAnalysis.shared)
    }
    
    func clearSectionRewrite() {
        viewModel.onIntent(intent: ResumeIntent.ClearSectionRewrite.shared)
    }
    
    // MARK: - Version History
    
    func loadVersionHistory(resumeId: String) {
        viewModel.onIntent(intent: ResumeIntent.LoadVersionHistory(resumeId: resumeId))
    }
    
    func restoreVersion(resumeId: String, versionId: String) {
        viewModel.onIntent(intent: ResumeIntent.RestoreVersion(resumeId: resumeId, versionId: versionId))
    }
    
    func clearVersionHistory() {
        viewModel.onIntent(intent: ResumeIntent.ClearVersionHistory.shared)
        versionRestoreSuccess = false
    }
    
    // MARK: - LinkedIn Import
    
    /// Import resume from LinkedIn using auth code
    /// Note: This uses the AuthViewModel's ImportFromLinkedIn intent
    func importFromLinkedIn(authCode: String) {
        // Get the auth view model and call import
        let authViewModel = KoinHelperKt.getAuthViewModel()
        let intent = AuthIntent.ImportFromLinkedIn(authCode: authCode)
        authViewModel.onIntent(intent: intent)
        
        // Refresh resumes list after a delay to allow import to complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.loadResumes()
        }
    }
}
