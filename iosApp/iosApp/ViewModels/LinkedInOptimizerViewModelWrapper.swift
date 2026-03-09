import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin LinkedInOptimizerManager observable in SwiftUI
/// Bridges the shared module's StateFlow-based API to @Published properties for SwiftUI
@MainActor
class LinkedInOptimizerViewModelWrapper: ObservableObject {
    private let manager: LinkedInOptimizerManager
    
    // MARK: - Published State
    
    // Profile analysis state
    @Published var isAnalyzing: Bool = false
    @Published var analysisResult: LinkedInAnalysisResult? = nil
    @Published var analysisError: String? = nil
    
    // Optimization state
    @Published var isGenerating: Bool = false
    @Published var optimizedContent: OptimizedLinkedInContent? = nil
    @Published var optimizationError: String? = nil
    
    // History state
    @Published var isLoadingHistory: Bool = false
    @Published var history: [LinkedInAnalysisResult] = []
    @Published var historyError: String? = nil
    
    private var profileWatcher: Closeable?
    private var optimizationWatcher: Closeable?
    private var historyWatcher: Closeable?
    
    init() {
        self.manager = KoinHelperKt.getLinkedInOptimizerManager()
        observeState()
    }
    
    deinit {
        profileWatcher?.close()
        optimizationWatcher?.close()
        historyWatcher?.close()
    }
    
    // MARK: - State Observation
    
    private func observeState() {
        // Watch profile state
        profileWatcher = FlowExtensionsKt.watch(manager.profileState) { [weak self] (state: Any?) in
            guard let self = self else { return }
            Task { @MainActor in
                if state is LinkedInProfileState.Idle {
                    self.isAnalyzing = false
                    self.analysisResult = nil
                    self.analysisError = nil
                } else if state is LinkedInProfileState.Analyzing {
                    self.isAnalyzing = true
                    self.analysisError = nil
                } else if let analyzed = state as? LinkedInProfileState.Analyzed {
                    self.isAnalyzing = false
                    self.analysisResult = analyzed.result
                    self.analysisError = nil
                } else if let error = state as? LinkedInProfileState.Error {
                    self.isAnalyzing = false
                    self.analysisError = error.message
                }
            }
        }
        
        // Watch optimization state
        optimizationWatcher = FlowExtensionsKt.watch(manager.optimizationState) { [weak self] (state: Any?) in
            guard let self = self else { return }
            Task { @MainActor in
                if state is OptimizationState.Idle {
                    self.isGenerating = false
                    self.optimizedContent = nil
                    self.optimizationError = nil
                } else if state is OptimizationState.Generating {
                    self.isGenerating = true
                    self.optimizationError = nil
                } else if let success = state as? OptimizationState.Success {
                    self.isGenerating = false
                    self.optimizedContent = success.content
                    self.optimizationError = nil
                } else if let error = state as? OptimizationState.Error {
                    self.isGenerating = false
                    self.optimizationError = error.message
                }
            }
        }
        
        // Watch history state
        historyWatcher = FlowExtensionsKt.watch(manager.historyState) { [weak self] (state: Any?) in
            guard let self = self else { return }
            Task { @MainActor in
                if state is LinkedInHistoryState.Loading {
                    self.isLoadingHistory = true
                    self.historyError = nil
                } else if let success = state as? LinkedInHistoryState.Success {
                    self.isLoadingHistory = false
                    self.history = success.history
                    self.historyError = nil
                } else if let error = state as? LinkedInHistoryState.Error {
                    self.isLoadingHistory = false
                    self.historyError = error.message
                }
            }
        }
    }
    
    // MARK: - Actions
    
    func analyzeProfile(profileUrl: String?) {
        Task {
            try await manager.analyzeProfile(profileUrl: profileUrl, manualProfile: nil)
        }
    }
    
    func getOptimizedContent(profileId: String, targetRole: String?, targetIndustry: String?) {
        Task {
            try await manager.getOptimizedContent(profileId: profileId, targetRole: targetRole, targetIndustry: targetIndustry)
        }
    }
    
    func loadHistory() {
        Task {
            try await manager.loadHistory()
        }
    }
    
    func clearProfile() {
        manager.clearProfile()
    }
    
    func clearOptimization() {
        manager.clearOptimization()
    }
}
