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
    @Published var optimizedContent: String? = nil
    @Published var isLoading: Bool = true
    @Published var isAnalyzing: Bool = false
    @Published var isOptimizing: Bool = false
    @Published var isATSAnalyzing: Bool = false
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
                self.optimizedContent = resumeState.optimizedContent
                self.isLoading = resumeState.isLoading
                self.isAnalyzing = resumeState.isAnalyzing
                self.isOptimizing = resumeState.isOptimizing
                self.isATSAnalyzing = resumeState.isATSAnalyzing
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
    
    func clearError() {
        viewModel.onIntent(intent: ResumeIntent.ClearError.shared)
    }
    
    func clearAnalysis() {
        viewModel.onIntent(intent: ResumeIntent.ClearAnalysis.shared)
    }
    
    func clearOptimizedContent() {
        viewModel.onIntent(intent: ResumeIntent.ClearOptimizedContent.shared)
    }
}
