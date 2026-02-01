package com.vwatek.apply.presentation.resume

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.ATSAnalysis
import com.vwatek.apply.domain.model.ImpactBullet
import com.vwatek.apply.domain.model.GrammarIssue
import com.vwatek.apply.domain.usecase.GetAllResumesUseCase
import com.vwatek.apply.domain.usecase.GetResumeByIdUseCase
import com.vwatek.apply.domain.usecase.SaveResumeUseCase
import com.vwatek.apply.domain.usecase.DeleteResumeUseCase
import com.vwatek.apply.domain.usecase.AnalyzeResumeUseCase
import com.vwatek.apply.domain.usecase.OptimizeResumeUseCase
import com.vwatek.apply.domain.usecase.PerformATSAnalysisUseCase
import com.vwatek.apply.domain.usecase.GenerateImpactBulletsUseCase
import com.vwatek.apply.domain.usecase.AnalyzeGrammarUseCase
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
    private val optimizeResumeUseCase: OptimizeResumeUseCase,
    private val performATSAnalysisUseCase: PerformATSAnalysisUseCase,
    private val generateImpactBulletsUseCase: GenerateImpactBulletsUseCase,
    private val analyzeGrammarUseCase: AnalyzeGrammarUseCase
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
            is ResumeIntent.PerformATSAnalysis -> performATSAnalysis(intent.resume, intent.jobDescription)
            is ResumeIntent.GenerateImpactBullets -> generateImpactBullets(intent.experiences, intent.jobContext)
            is ResumeIntent.AnalyzeGrammar -> analyzeGrammar(intent.text)
            is ResumeIntent.ClearError -> clearError()
            is ResumeIntent.ClearAnalysis -> clearAnalysis()
            is ResumeIntent.ClearOptimizedContent -> clearOptimizedContent()
            is ResumeIntent.ClearATSAnalysis -> clearATSAnalysis()
            is ResumeIntent.ClearImpactBullets -> clearImpactBullets()
            is ResumeIntent.ClearGrammarIssues -> clearGrammarIssues()
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
    
    private fun performATSAnalysis(resume: Resume, jobDescription: String?) {
        scope.launch {
            _state.update { it.copy(isATSAnalyzing = true, atsAnalysis = null) }
            performATSAnalysisUseCase(resume.content, resume.id, jobDescription)
                .onSuccess { analysis ->
                    _state.update { it.copy(isATSAnalyzing = false, atsAnalysis = analysis) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isATSAnalyzing = false, error = e.message) }
                }
        }
    }
    
    private fun generateImpactBullets(experiences: List<String>, jobContext: String) {
        scope.launch {
            _state.update { it.copy(isGeneratingBullets = true, impactBullets = emptyList()) }
            generateImpactBulletsUseCase(experiences, jobContext)
                .onSuccess { bullets ->
                    _state.update { it.copy(isGeneratingBullets = false, impactBullets = bullets) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isGeneratingBullets = false, error = e.message) }
                }
        }
    }
    
    private fun analyzeGrammar(text: String) {
        scope.launch {
            _state.update { it.copy(isAnalyzingGrammar = true, grammarIssues = emptyList()) }
            analyzeGrammarUseCase(text)
                .onSuccess { issues ->
                    _state.update { it.copy(isAnalyzingGrammar = false, grammarIssues = issues) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isAnalyzingGrammar = false, error = e.message) }
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
    
    private fun clearATSAnalysis() {
        _state.update { it.copy(atsAnalysis = null) }
    }
    
    private fun clearImpactBullets() {
        _state.update { it.copy(impactBullets = emptyList()) }
    }
    
    private fun clearGrammarIssues() {
        _state.update { it.copy(grammarIssues = emptyList()) }
    }
}

data class ResumeState(
    val resumes: List<Resume> = emptyList(),
    val selectedResume: Resume? = null,
    val analysis: ResumeAnalysis? = null,
    val atsAnalysis: ATSAnalysis? = null,
    val impactBullets: List<ImpactBullet> = emptyList(),
    val grammarIssues: List<GrammarIssue> = emptyList(),
    val optimizedContent: String? = null,
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false,
    val isOptimizing: Boolean = false,
    val isATSAnalyzing: Boolean = false,
    val isGeneratingBullets: Boolean = false,
    val isAnalyzingGrammar: Boolean = false,
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
    data class PerformATSAnalysis(val resume: Resume, val jobDescription: String? = null) : ResumeIntent()
    data class GenerateImpactBullets(val experiences: List<String>, val jobContext: String) : ResumeIntent()
    data class AnalyzeGrammar(val text: String) : ResumeIntent()
    data object ClearError : ResumeIntent()
    data object ClearAnalysis : ResumeIntent()
    data object ClearOptimizedContent : ResumeIntent()
    data object ClearATSAnalysis : ResumeIntent()
    data object ClearImpactBullets : ResumeIntent()
    data object ClearGrammarIssues : ResumeIntent()
}
