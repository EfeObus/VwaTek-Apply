import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin InterviewViewModel observable in SwiftUI
@MainActor
class InterviewViewModelWrapper: ObservableObject {
    private let viewModel: InterviewViewModel
    
    @Published var sessions: [InterviewSession] = []
    @Published var currentSession: InterviewSession? = nil
    @Published var currentQuestionIndex: Int = 0
    @Published var lastFeedback: String? = nil
    @Published var isLoading: Bool = true
    @Published var isStartingSession: Bool = false
    @Published var isSubmittingAnswer: Bool = false
    @Published var error: String? = nil
    
    private var stateWatcher: Closeable?
    
    init() {
        // Get InterviewViewModel from Koin
        self.viewModel = KoinHelperKt.getInterviewViewModel()
        observeState()
    }
    
    deinit {
        stateWatcher?.close()
    }
    
    private func observeState() {
        // Use FlowExtensionsKt.watch to observe StateFlow
        stateWatcher = FlowExtensionsKt.watch(viewModel.state) { [weak self] (state: Any?) in
            guard let self = self, let interviewState = state as? InterviewState else { return }
            Task { @MainActor in
                self.sessions = interviewState.sessions
                self.currentSession = interviewState.currentSession
                self.currentQuestionIndex = Int(interviewState.currentQuestionIndex)
                self.lastFeedback = interviewState.lastFeedback
                self.isLoading = interviewState.isLoading
                self.isStartingSession = interviewState.isStartingSession
                self.isSubmittingAnswer = interviewState.isSubmittingAnswer
                self.error = interviewState.error
            }
        }
    }
    
    func loadSessions() {
        viewModel.onIntent(intent: InterviewIntent.LoadSessions.shared)
    }
    
    func selectSession(id: String?) {
        viewModel.onIntent(intent: InterviewIntent.SelectSession(id: id))
    }
    
    func startSession(resumeContent: String?, jobTitle: String, jobDescription: String) {
        viewModel.onIntent(intent: InterviewIntent.StartSession(
            resumeContent: resumeContent,
            jobTitle: jobTitle,
            jobDescription: jobDescription
        ))
    }
    
    func submitAnswer(question: InterviewQuestion, answer: String, jobTitle: String) {
        viewModel.onIntent(intent: InterviewIntent.SubmitAnswer(
            question: question,
            answer: answer,
            jobTitle: jobTitle
        ))
    }
    
    func completeSession(sessionId: String) {
        viewModel.onIntent(intent: InterviewIntent.CompleteSession(sessionId: sessionId))
    }
    
    func deleteSession(id: String) {
        viewModel.onIntent(intent: InterviewIntent.DeleteSession(id: id))
    }
    
    func setCurrentQuestion(index: Int) {
        viewModel.onIntent(intent: InterviewIntent.SetCurrentQuestion(index: Int32(index)))
    }
    
    func clearError() {
        viewModel.onIntent(intent: InterviewIntent.ClearError.shared)
    }
    
    var currentQuestion: InterviewQuestion? {
        guard let session = currentSession,
              currentQuestionIndex < session.questions.count else {
            return nil
        }
        return session.questions[currentQuestionIndex]
    }
    
    var totalQuestions: Int {
        currentSession?.questions.count ?? 0
    }
}
