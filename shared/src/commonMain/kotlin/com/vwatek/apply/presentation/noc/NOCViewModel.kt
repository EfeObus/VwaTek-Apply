package com.vwatek.apply.presentation.noc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.vwatek.apply.data.api.*
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.i18n.LocaleManager
import com.vwatek.apply.i18n.Locale

/**
 * ViewModel for NOC (National Occupational Classification) features
 * 
 * Manages NOC search, details viewing, and resume fit analysis
 */
class NOCViewModel(
    private val nocApiClient: NOCApiClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(NOCState())
    val state: StateFlow<NOCState> = _state.asStateFlow()
    
    // Initialize by loading TEER levels and categories
    init {
        loadTEERLevels()
        loadCategories()
    }
    
    /**
     * Search NOC codes
     */
    fun searchNOCCodes(
        query: String = "",
        teerLevels: List<Int>? = null,
        category: String? = null
    ) {
        scope.launch {
            _state.update { it.copy(isSearching = true, searchError = null) }
            
            nocApiClient.searchNOCCodes(
                query = query,
                teerLevels = teerLevels,
                category = category,
                page = 1,
                pageSize = 20
            ).onSuccess { response ->
                _state.update { 
                    it.copy(
                        isSearching = false,
                        searchResults = response.codes.map { dto -> dto.toDomainModel() },
                        totalResults = response.totalCount,
                        currentPage = response.page,
                        hasMoreResults = response.hasMore,
                        lastSearchQuery = query,
                        lastTeerFilter = teerLevels,
                        lastCategoryFilter = category
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isSearching = false,
                        searchError = error.message ?: "Search failed"
                    )
                }
            }
        }
    }
    
    /**
     * Load more search results
     */
    fun loadMoreResults() {
        val currentState = _state.value
        if (currentState.isSearching || !currentState.hasMoreResults) return
        
        scope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            
            nocApiClient.searchNOCCodes(
                query = currentState.lastSearchQuery,
                teerLevels = currentState.lastTeerFilter,
                category = currentState.lastCategoryFilter,
                page = currentState.currentPage + 1
            ).onSuccess { response ->
                _state.update { 
                    it.copy(
                        isLoadingMore = false,
                        searchResults = it.searchResults + response.codes.map { dto -> dto.toDomainModel() },
                        currentPage = response.page,
                        hasMoreResults = response.hasMore
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoadingMore = false,
                        searchError = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Load NOC details
     */
    fun loadNOCDetails(code: String) {
        scope.launch {
            _state.update { it.copy(isLoadingDetails = true, detailsError = null) }
            
            nocApiClient.getNOCDetails(code).onSuccess { response ->
                _state.update { 
                    it.copy(
                        isLoadingDetails = false,
                        selectedNOCDetails = response.toDomainModel()
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoadingDetails = false,
                        detailsError = error.message ?: "Failed to load NOC details"
                    )
                }
            }
        }
    }
    
    /**
     * Load TEER levels
     */
    private fun loadTEERLevels() {
        scope.launch {
            nocApiClient.getTEERLevels().onSuccess { response ->
                _state.update { 
                    it.copy(
                        teerLevels = response.levels.map { level ->
                            TEERLevelInfo(
                                level = level.level,
                                titleEn = level.titleEn,
                                titleFr = level.titleFr,
                                educationRequirement = level.educationRequirement
                            )
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Load categories
     */
    private fun loadCategories() {
        scope.launch {
            nocApiClient.getCategories().onSuccess { response ->
                _state.update { 
                    it.copy(
                        categories = response.categories.map { cat ->
                            NOCCategoryInfo(
                                category = cat.category,
                                majorGroup = cat.majorGroup,
                                name = cat.name
                            )
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Load provincial demand for selected NOC
     */
    fun loadProvincialDemand(code: String) {
        scope.launch {
            nocApiClient.getProvincialDemand(code).onSuccess { response ->
                _state.update { 
                    it.copy(
                        provincialDemand = response.demand.map { d ->
                            ProvincialDemandInfo(
                                provinceCode = d.provinceCode,
                                demandLevel = d.demandLevel,
                                medianSalary = d.medianSalary,
                                salaryLow = d.salaryLow,
                                salaryHigh = d.salaryHigh,
                                jobOpenings = d.jobOpenings,
                                outlookYear = d.outlookYear
                            )
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Load immigration pathways for selected NOC
     */
    fun loadImmigrationPathways(code: String) {
        scope.launch {
            nocApiClient.getImmigrationPathways(code).onSuccess { response ->
                _state.update { 
                    it.copy(
                        immigrationPathways = response.pathways.map { p ->
                            ImmigrationPathwayInfo(
                                pathwayName = p.pathwayName,
                                pathwayType = p.pathwayType,
                                provinceCode = p.provinceCode,
                                isEligible = p.isEligible,
                                eligibilityNotes = p.eligibilityNotes
                            )
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Save user's NOC match
     */
    fun saveNOCMatch(
        userId: String,
        resumeId: String?,
        nocCode: String,
        analysis: NOCFitAnalysis
    ) {
        scope.launch {
            _state.update { it.copy(isSavingMatch = true) }
            
            val request = CreateNOCMatchRequest(
                resumeId = resumeId,
                nocCode = nocCode,
                matchScore = analysis.overallFitScore,
                teerLevelFit = analysis.teerLevelFit.name,
                matchedDuties = analysis.dutiesMatch.matched,
                missingSkills = analysis.requirementsMatch.notMet,
                recommendations = analysis.resumeImprovements.map { it.suggestion }
            )
            
            nocApiClient.saveNOCMatch(userId, request).onSuccess { response ->
                _state.update { 
                    it.copy(
                        isSavingMatch = false,
                        savedMatchId = response.id
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isSavingMatch = false,
                        saveMatchError = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Load user's saved NOC matches
     */
    fun loadUserMatches(userId: String) {
        scope.launch {
            _state.update { it.copy(isLoadingUserMatches = true) }
            
            nocApiClient.getUserNOCMatches(userId).onSuccess { response ->
                _state.update { 
                    it.copy(
                        isLoadingUserMatches = false,
                        userMatches = response.matches.map { m ->
                            UserNOCMatchInfo(
                                id = m.id,
                                resumeId = m.resumeId,
                                nocCode = m.nocCode,
                                matchScore = m.matchScore,
                                teerLevelFit = m.teerLevelFit,
                                matchedDuties = m.matchedDuties,
                                missingSkills = m.missingSkills,
                                recommendations = m.recommendations,
                                createdAt = m.createdAt
                            )
                        }
                    )
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoadingUserMatches = false,
                        userMatchesError = error.message
                    )
                }
            }
        }
    }
    
    /**
     * Set TEER filter
     */
    fun setTEERFilter(levels: List<Int>?) {
        _state.update { it.copy(selectedTeerLevels = levels) }
    }
    
    /**
     * Set category filter
     */
    fun setCategoryFilter(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _state.update { 
            it.copy(
                searchResults = emptyList(),
                lastSearchQuery = "",
                totalResults = 0,
                currentPage = 1,
                hasMoreResults = false
            )
        }
    }
    
    /**
     * Clear selected details
     */
    fun clearSelectedDetails() {
        _state.update { 
            it.copy(
                selectedNOCDetails = null,
                provincialDemand = emptyList(),
                immigrationPathways = emptyList()
            )
        }
    }
    
    /**
     * Toggle language
     */
    fun toggleLanguage() {
        LocaleManager.toggleLocale()
        _state.update { 
            it.copy(currentLocale = LocaleManager.currentLocale.value)
        }
    }
    
    /**
     * Set language
     */
    fun setLanguage(locale: Locale) {
        LocaleManager.setLocale(locale)
        _state.update { 
            it.copy(currentLocale = locale)
        }
    }
}

/**
 * NOC UI State
 */
data class NOCState(
    // Search state
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchResults: List<NOCCode> = emptyList(),
    val totalResults: Int = 0,
    val currentPage: Int = 1,
    val hasMoreResults: Boolean = false,
    val searchError: String? = null,
    val lastSearchQuery: String = "",
    val lastTeerFilter: List<Int>? = null,
    val lastCategoryFilter: String? = null,
    
    // Details state
    val isLoadingDetails: Boolean = false,
    val selectedNOCDetails: NOCDetails? = null,
    val detailsError: String? = null,
    
    // Filter data
    val teerLevels: List<TEERLevelInfo> = emptyList(),
    val categories: List<NOCCategoryInfo> = emptyList(),
    val selectedTeerLevels: List<Int>? = null,
    val selectedCategory: String? = null,
    
    // Provincial demand
    val provincialDemand: List<ProvincialDemandInfo> = emptyList(),
    
    // Immigration pathways
    val immigrationPathways: List<ImmigrationPathwayInfo> = emptyList(),
    
    // User matches
    val isLoadingUserMatches: Boolean = false,
    val userMatches: List<UserNOCMatchInfo> = emptyList(),
    val userMatchesError: String? = null,
    
    // Save match
    val isSavingMatch: Boolean = false,
    val savedMatchId: String? = null,
    val saveMatchError: String? = null,
    
    // Locale
    val currentLocale: Locale = Locale.ENGLISH
)

// UI model classes
data class TEERLevelInfo(
    val level: Int,
    val titleEn: String,
    val titleFr: String,
    val educationRequirement: String
) {
    fun getTitle(locale: Locale): String = when (locale) {
        Locale.FRENCH -> titleFr
        Locale.ENGLISH -> titleEn
    }
}

data class NOCCategoryInfo(
    val category: String,
    val majorGroup: String,
    val name: String
)

data class ProvincialDemandInfo(
    val provinceCode: String,
    val demandLevel: String,
    val medianSalary: Double?,
    val salaryLow: Double?,
    val salaryHigh: Double?,
    val jobOpenings: Int?,
    val outlookYear: Int
) {
    fun formatSalary(): String {
        return medianSalary?.let { "$${"%.0f".format(it)}/year" } ?: "N/A"
    }
    
    fun formatSalaryRange(): String {
        return if (salaryLow != null && salaryHigh != null) {
            "$${"%.0f".format(salaryLow)} - $${"%.0f".format(salaryHigh)}/year"
        } else {
            formatSalary()
        }
    }
}

data class ImmigrationPathwayInfo(
    val pathwayName: String,
    val pathwayType: String,
    val provinceCode: String?,
    val isEligible: Boolean,
    val eligibilityNotes: String?
)

data class UserNOCMatchInfo(
    val id: String,
    val resumeId: String?,
    val nocCode: String,
    val matchScore: Int,
    val teerLevelFit: String,
    val matchedDuties: List<String>,
    val missingSkills: List<String>,
    val recommendations: List<String>,
    val createdAt: String
) {
    fun getScoreColor(): String = when {
        matchScore >= 80 -> "#22C55E" // Green
        matchScore >= 60 -> "#EAB308" // Yellow
        else -> "#EF4444" // Red
    }
}
