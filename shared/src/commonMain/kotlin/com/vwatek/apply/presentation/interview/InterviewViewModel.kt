package com.vwatek.apply.presentation.interview

import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.usecase.GetAllInterviewSessionsUseCase
import com.vwatek.apply.domain.usecase.GetInterviewSessionByIdUseCase
import com.vwatek.apply.domain.usecase.StartInterviewSessionUseCase
import com.vwatek.apply.domain.usecase.SubmitInterviewAnswerUseCase
import com.vwatek.apply.domain.usecase.CompleteInterviewSessionUseCase
import com.vwatek.apply.domain.usecase.DeleteInterviewSessionUseCase
import com.vwatek.apply.domain.usecase.GetStarCoachingUseCase
import com.vwatek.apply.domain.usecase.StarResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InterviewViewModel(
    private val getAllSessionsUseCase: GetAllInterviewSessionsUseCase,
    private val getSessionByIdUseCase: GetInterviewSessionByIdUseCase,
    private val startSessionUseCase: StartInterviewSessionUseCase,
    private val submitAnswerUseCase: SubmitInterviewAnswerUseCase,
    private val completeSessionUseCase: CompleteInterviewSessionUseCase,
    private val deleteSessionUseCase: DeleteInterviewSessionUseCase,
    private val getStarCoachingUseCase: GetStarCoachingUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _state = MutableStateFlow(InterviewState())
    val state: StateFlow<InterviewState> = _state.asStateFlow()
    
    init {
        loadSessions()
    }
    
    fun onIntent(intent: InterviewIntent) {
        when (intent) {
            is InterviewIntent.LoadSessions -> loadSessions()
            is InterviewIntent.SelectSession -> selectSession(intent.id)
            is InterviewIntent.StartSession -> startSession(intent.resumeContent, intent.jobTitle, intent.jobDescription)
            is InterviewIntent.SubmitAnswer -> submitAnswer(intent.question, intent.answer, intent.jobTitle)
            is InterviewIntent.CompleteSession -> completeSession(intent.sessionId)
            is InterviewIntent.DeleteSession -> deleteSession(intent.id)
            is InterviewIntent.GetStarCoaching -> getStarCoaching(intent.experience, intent.jobContext)
            is InterviewIntent.SetCurrentQuestion -> setCurrentQuestion(intent.index)
            is InterviewIntent.ClearError -> clearError()
            is InterviewIntent.ClearStarResponse -> clearStarResponse()
        }
    }
    
    private fun loadSessions() {
        scope.launch {
            getAllSessionsUseCase().collect { sessions ->
                _state.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }
    }
    
    private fun selectSession(id: String?) {
        scope.launch {
            if (id == null) {
                _state.update { it.copy(currentSession = null, currentQuestionIndex = 0) }
            } else {
                val session = getSessionByIdUseCase(id)
                _state.update { it.copy(currentSession = session, currentQuestionIndex = 0) }
            }
        }
    }
    
    private fun startSession(resumeContent: String?, jobTitle: String, jobDescription: String) {
        scope.launch {
            _state.update { it.copy(isStartingSession = true) }
            startSessionUseCase(resumeContent, jobTitle, jobDescription)
                .onSuccess { session ->
                    _state.update { 
                        it.copy(
                            isStartingSession = false,
                            currentSession = session,
                            currentQuestionIndex = 0
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isStartingSession = false, error = e.message) }
                }
        }
    }
    
    private fun submitAnswer(question: InterviewQuestion, answer: String, jobTitle: String) {
        scope.launch {
            _state.update { it.copy(isSubmittingAnswer = true) }
            submitAnswerUseCase(question.id, answer, question.question, jobTitle)
                .onSuccess { feedback ->
                    val updatedQuestion = question.copy(userAnswer = answer, aiFeedback = feedback)
                    val currentSession = _state.value.currentSession
                    if (currentSession != null) {
                        val updatedQuestions = currentSession.questions.map {
                            if (it.id == question.id) updatedQuestion else it
                        }
                        _state.update { 
                            it.copy(
                                isSubmittingAnswer = false,
                                currentSession = currentSession.copy(questions = updatedQuestions),
                                lastFeedback = feedback
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmittingAnswer = false, error = e.message) }
                }
        }
    }
    
    private fun completeSession(sessionId: String) {
        scope.launch {
            try {
                completeSessionUseCase(sessionId)
                val updatedSession = getSessionByIdUseCase(sessionId)
                _state.update { it.copy(currentSession = updatedSession) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun deleteSession(id: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                deleteSessionUseCase(id)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        currentSession = if (it.currentSession?.id == id) null else it.currentSession
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun getStarCoaching(experience: String, jobContext: String) {
        scope.launch {
            _state.update { it.copy(isGettingStarCoaching = true, starResponse = null) }
            getStarCoachingUseCase(experience, jobContext)
                .onSuccess { response ->
                    _state.update { it.copy(isGettingStarCoaching = false, starResponse = response) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isGettingStarCoaching = false, error = e.message) }
                }
        }
    }
    
    private fun setCurrentQuestion(index: Int) {
        _state.update { it.copy(currentQuestionIndex = index) }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun clearStarResponse() {
        _state.update { it.copy(starResponse = null) }
    }
}

data class InterviewState(
    val sessions: List<InterviewSession> = emptyList(),
    val currentSession: InterviewSession? = null,
    val currentQuestionIndex: Int = 0,
    val lastFeedback: String? = null,
    val starResponse: StarResponse? = null,
    val isLoading: Boolean = true,
    val isStartingSession: Boolean = false,
    val isSubmittingAnswer: Boolean = false,
    val isGettingStarCoaching: Boolean = false,
    val error: String? = null
)

sealed class InterviewIntent {
    data object LoadSessions : InterviewIntent()
    data class SelectSession(val id: String?) : InterviewIntent()
    data class StartSession(
        val resumeContent: String?,
        val jobTitle: String,
        val jobDescription: String
    ) : InterviewIntent()
    data class SubmitAnswer(
        val question: InterviewQuestion,
        val answer: String,
        val jobTitle: String
    ) : InterviewIntent()
    data class CompleteSession(val sessionId: String) : InterviewIntent()
    data class DeleteSession(val id: String) : InterviewIntent()
    data class GetStarCoaching(val experience: String, val jobContext: String) : InterviewIntent()
    data class SetCurrentQuestion(val index: Int) : InterviewIntent()
    data object ClearError : InterviewIntent()
    data object ClearStarResponse : InterviewIntent()
}
