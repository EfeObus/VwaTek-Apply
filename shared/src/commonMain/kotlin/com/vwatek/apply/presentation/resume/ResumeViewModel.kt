package com.vwatek.apply.presentation.resume

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.usecase.GetAllResumesUseCase
import com.vwatek.apply.domain.usecase.GetResumeByIdUseCase
import com.vwatek.apply.domain.usecase.SaveResumeUseCase
import com.vwatek.apply.domain.usecase.DeleteResumeUseCase
import com.vwatek.apply.domain.usecase.AnalyzeResumeUseCase
import com.vwatek.apply.domain.usecase.OptimizeResumeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ResumeViewModel(
    private val getAllResumesUseCase: GetAllResumesUseCase,
    private val getResumeByIdUseCase: GetResumeByIdUseCase,
    private val saveResumeUseCase: SaveResumeUseCase,
    private val deleteResumeUseCase: DeleteResumeUseCase,
    private val analyzeResumeUseCase: AnalyzeResumeUseCase,
    private val optimizeResumeUseCase: OptimizeResumeUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _state = MutableStateFlow(ResumeState())
    val state: StateFlow<ResumeState> = _state.asStateFlow()
    
    init {
        loadResumes()
    }
    
    fun onIntent(intent: ResumeIntent) {
        when (intent) {
            is ResumeIntent.LoadResumes -> loadResumes()
            is ResumeIntent.SelectResume -> selectResume(intent.id)
            is ResumeIntent.CreateResume -> createResume(intent.name, intent.content, intent.industry)
            is ResumeIntent.UpdateResume -> updateResume(intent.resume)
            is ResumeIntent.DeleteResume -> deleteResume(intent.id)
            is ResumeIntent.AnalyzeResume -> analyzeResume(intent.resume, intent.jobDescription)
            is ResumeIntent.OptimizeResume -> optimizeResume(intent.resumeContent, intent.jobDescription)
            is ResumeIntent.ClearError -> clearError()
            is ResumeIntent.ClearAnalysis -> clearAnalysis()
            is ResumeIntent.ClearOptimizedContent -> clearOptimizedContent()
        }
    }
    
    private fun loadResumes() {
        scope.launch {
            getAllResumesUseCase().collect { resumes ->
                _state.update { it.copy(resumes = resumes, isLoading = false) }
            }
        }
    }
    
    private fun selectResume(id: String?) {
        scope.launch {
            if (id == null) {
                _state.update { it.copy(selectedResume = null) }
            } else {
                val resume = getResumeByIdUseCase(id)
                _state.update { it.copy(selectedResume = resume) }
            }
        }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    private fun createResume(name: String, content: String, industry: String?) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val now = Clock.System.now()
                val resume = Resume(
                    id = Uuid.random().toString(),
                    name = name,
                    content = content,
                    industry = industry,
                    createdAt = now,
                    updatedAt = now
                )
                saveResumeUseCase(resume)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun updateResume(resume: Resume) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val updatedResume = resume.copy(updatedAt = Clock.System.now())
                saveResumeUseCase(updatedResume)
                _state.update { it.copy(isLoading = false, selectedResume = updatedResume) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun deleteResume(id: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                deleteResumeUseCase(id)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        selectedResume = if (it.selectedResume?.id == id) null else it.selectedResume
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    private fun analyzeResume(resume: Resume, jobDescription: String) {
        scope.launch {
            _state.update { it.copy(isAnalyzing = true, analysis = null) }
            analyzeResumeUseCase(resume, jobDescription)
                .onSuccess { analysis ->
                    _state.update { it.copy(isAnalyzing = false, analysis = analysis) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isAnalyzing = false, error = e.message) }
                }
        }
    }
    
    private fun optimizeResume(resumeContent: String, jobDescription: String) {
        scope.launch {
            _state.update { it.copy(isOptimizing = true, optimizedContent = null) }
            optimizeResumeUseCase(resumeContent, jobDescription)
                .onSuccess { optimized ->
                    _state.update { it.copy(isOptimizing = false, optimizedContent = optimized) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isOptimizing = false, error = e.message) }
                }
        }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun clearAnalysis() {
        _state.update { it.copy(analysis = null) }
    }
    
    private fun clearOptimizedContent() {
        _state.update { it.copy(optimizedContent = null) }
    }
}

data class ResumeState(
    val resumes: List<Resume> = emptyList(),
    val selectedResume: Resume? = null,
    val analysis: ResumeAnalysis? = null,
    val optimizedContent: String? = null,
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false,
    val isOptimizing: Boolean = false,
    val error: String? = null
)

sealed class ResumeIntent {
    data object LoadResumes : ResumeIntent()
    data class SelectResume(val id: String?) : ResumeIntent()
    data class CreateResume(val name: String, val content: String, val industry: String?) : ResumeIntent()
    data class UpdateResume(val resume: Resume) : ResumeIntent()
    data class DeleteResume(val id: String) : ResumeIntent()
    data class AnalyzeResume(val resume: Resume, val jobDescription: String) : ResumeIntent()
    data class OptimizeResume(val resumeContent: String, val jobDescription: String) : ResumeIntent()
    data object ClearError : ResumeIntent()
    data object ClearAnalysis : ResumeIntent()
    data object ClearOptimizedContent : ResumeIntent()
}
