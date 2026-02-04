import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin CoverLetterViewModel observable in SwiftUI
@MainActor
class CoverLetterViewModelWrapper: ObservableObject {
    private let viewModel: CoverLetterViewModel
    
    @Published var coverLetters: [CoverLetter] = []
    @Published var selectedCoverLetter: CoverLetter? = nil
    @Published var generatedCoverLetter: CoverLetter? = nil
    @Published var isLoading: Bool = true
    @Published var isGenerating: Bool = false
    @Published var error: String? = nil
    
    private var stateWatcher: Closeable?
    
    init() {
        // Get CoverLetterViewModel from Koin
        self.viewModel = KoinHelperKt.getCoverLetterViewModel()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func observeState() {
        // Use FlowExtensionsKt.watch to observe StateFlow
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let coverLetterState = state as? CoverLetterState else { return }
            Task { @MainActor in
                self.coverLetters = coverLetterState.coverLetters
                self.selectedCoverLetter = coverLetterState.selectedCoverLetter
                self.generatedCoverLetter = coverLetterState.generatedCoverLetter
                self.isLoading = coverLetterState.isLoading
                self.isGenerating = coverLetterState.isGenerating
                self.error = coverLetterState.error
            }
        }
    }
    
    func loadCoverLetters() {
        viewModel.onIntent(intent: CoverLetterIntent.LoadCoverLetters.shared)
    }
    
    func selectCoverLetter(_ coverLetter: CoverLetter?) {
        viewModel.onIntent(intent: CoverLetterIntent.SelectCoverLetter(coverLetter: coverLetter))
    }
    
    func generateCoverLetter(
        resumeContent: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: CoverLetterTone = .professional
    ) {
        viewModel.onIntent(intent: CoverLetterIntent.GenerateCoverLetter(
            resumeContent: resumeContent,
            jobTitle: jobTitle,
            companyName: companyName,
            jobDescription: jobDescription,
            tone: tone
        ))
    }
    
    func deleteCoverLetter(id: String) {
        viewModel.onIntent(intent: CoverLetterIntent.DeleteCoverLetter(id: id))
    }
    
    func clearError() {
        viewModel.onIntent(intent: CoverLetterIntent.ClearError.shared)
    }
    
    func clearGenerated() {
        viewModel.onIntent(intent: CoverLetterIntent.ClearGenerated.shared)
    }
}
