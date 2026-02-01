package com.vwatek.apply.presentation.coverletter

import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.usecase.GetAllCoverLettersUseCase
import com.vwatek.apply.domain.usecase.GenerateCoverLetterUseCase
import com.vwatek.apply.domain.usecase.DeleteCoverLetterUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoverLetterViewModel(
    private val getAllCoverLettersUseCase: GetAllCoverLettersUseCase,
    private val generateCoverLetterUseCase: GenerateCoverLetterUseCase,
    private val deleteCoverLetterUseCase: DeleteCoverLetterUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _state = MutableStateFlow(CoverLetterState())
    val state: StateFlow<CoverLetterState> = _state.asStateFlow()
    
    init {
        loadCoverLetters()
    }
    
    fun onIntent(intent: CoverLetterIntent) {
        when (intent) {
            is CoverLetterIntent.LoadCoverLetters -> loadCoverLetters()
            is CoverLetterIntent.SelectCoverLetter -> selectCoverLetter(intent.coverLetter)
            is CoverLetterIntent.GenerateCoverLetter -> generateCoverLetter(
                intent.resumeContent,
                intent.jobTitle,
                intent.companyName,
                intent.jobDescription,
                intent.tone
            )
            is CoverLetterIntent.DeleteCoverLetter -> deleteCoverLetter(intent.id)
            is CoverLetterIntent.ClearError -> clearError()
            is CoverLetterIntent.ClearGenerated -> clearGenerated()
        }
    }
    
    private fun loadCoverLetters() {
        scope.launch {
            getAllCoverLettersUseCase().collect { coverLetters ->
                _state.update { it.copy(coverLetters = coverLetters, isLoading = false) }
            }
        }
    }
    
    private fun selectCoverLetter(coverLetter: CoverLetter?) {
        _state.update { it.copy(selectedCoverLetter = coverLetter) }
    }
    
    private fun generateCoverLetter(
        resumeContent: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: CoverLetterTone
    ) {
        scope.launch {
            _state.update { it.copy(isGenerating = true, generatedCoverLetter = null) }
            generateCoverLetterUseCase(resumeContent, jobTitle, companyName, jobDescription, tone)
                .onSuccess { coverLetter ->
                    _state.update { it.copy(isGenerating = false, generatedCoverLetter = coverLetter) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isGenerating = false, error = e.message) }
                }
        }
    }
    
    private fun deleteCoverLetter(id: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                deleteCoverLetterUseCase(id)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        selectedCoverLetter = if (it.selectedCoverLetter?.id == id) null else it.selectedCoverLetter
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun clearGenerated() {
        _state.update { it.copy(generatedCoverLetter = null) }
    }
}

data class CoverLetterState(
    val coverLetters: List<CoverLetter> = emptyList(),
    val selectedCoverLetter: CoverLetter? = null,
    val generatedCoverLetter: CoverLetter? = null,
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val error: String? = null
)

sealed class CoverLetterIntent {
    data object LoadCoverLetters : CoverLetterIntent()
    data class SelectCoverLetter(val coverLetter: CoverLetter?) : CoverLetterIntent()
    data class GenerateCoverLetter(
        val resumeContent: String,
        val jobTitle: String,
        val companyName: String,
        val jobDescription: String,
        val tone: CoverLetterTone
    ) : CoverLetterIntent()
    data class DeleteCoverLetter(val id: String) : CoverLetterIntent()
    data object ClearError : CoverLetterIntent()
    data object ClearGenerated : CoverLetterIntent()
}
