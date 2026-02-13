package com.vwatek.apply.presentation.jobbank

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.vwatek.apply.data.api.JobBankApiClient
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.i18n.LocaleManager
import com.vwatek.apply.i18n.Locale

/**
 * ViewModel for Job Bank Canada job search
 * 
 * Manages job search, filtering, and job details viewing
 */
class JobBankViewModel(
    private val jobBankApiClient: JobBankApiClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(JobBankState())
    val state: StateFlow<JobBankState> = _state.asStateFlow()
    
    init {
        loadProvinces()
        loadTrendingJobs()
    }
    
    /**
     * Search for jobs
     */
    fun searchJobs(
        query: String? = null,
        location: String? = null,
        provinceCode: String? = null,
        nocCode: String? = null
    ) {
        scope.launch {
            _state.update { 
                it.copy(
                    isSearching = true, 
                    searchError = null,
                    lastSearchQuery = query,
                    lastLocationFilter = location,
                    lastProvinceFilter = provinceCode,
                    lastNocFilter = nocCode
                )
            }
            
            jobBankApiClient.searchJobs(
                query = query,
                location = location,
                provinceCode = provinceCode,
                nocCode = nocCode,
                page = 0,
                perPage = 20
            ).onSuccess { response ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchResults = response.jobs.map { job -> job.toDomainModel() },
                        totalResults = response.total,
                        currentPage = response.page,
                        hasMoreResults = response.hasMore
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
     * Load more search results (pagination)
     */
    fun loadMoreResults() {
        val currentState = _state.value
        if (currentState.isSearching || currentState.isLoadingMore || !currentState.hasMoreResults) return
        
        scope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            
            jobBankApiClient.searchJobs(
                query = currentState.lastSearchQuery,
                location = currentState.lastLocationFilter,
                provinceCode = currentState.lastProvinceFilter,
                nocCode = currentState.lastNocFilter,
                page = currentState.currentPage + 1
            ).onSuccess { response ->
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        searchResults = it.searchResults + response.jobs.map { job -> job.toDomainModel() },
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
     * Search jobs by NOC code
     */
    fun searchByNOC(nocCode: String) {
        scope.launch {
            _state.update { it.copy(isSearching = true, searchError = null, lastNocFilter = nocCode) }
            
            jobBankApiClient.searchByNOC(nocCode).onSuccess { response ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchResults = response.jobs.map { job -> job.toDomainModel() },
                        totalResults = response.total,
                        currentPage = 0,
                        hasMoreResults = response.hasMore
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isSearching = false, searchError = error.message)
                }
            }
        }
    }
    
    /**
     * Search jobs by province
     */
    fun searchByProvince(provinceCode: String) {
        scope.launch {
            _state.update { it.copy(isSearching = true, searchError = null, lastProvinceFilter = provinceCode) }
            
            jobBankApiClient.searchByProvince(provinceCode).onSuccess { response ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchResults = response.jobs.map { job -> job.toDomainModel() },
                        totalResults = response.total,
                        currentPage = 0,
                        hasMoreResults = response.hasMore
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isSearching = false, searchError = error.message)
                }
            }
        }
    }
    
    /**
     * Load job details
     */
    fun loadJobDetails(jobId: String) {
        scope.launch {
            _state.update { it.copy(isLoadingDetails = true, detailsError = null) }
            
            jobBankApiClient.getJobDetails(jobId).onSuccess { response ->
                _state.update {
                    it.copy(
                        isLoadingDetails = false,
                        selectedJob = response.toDomainModel()
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoadingDetails = false, detailsError = error.message)
                }
            }
        }
    }
    
    /**
     * Load job outlook for a NOC code
     */
    fun loadJobOutlook(nocCode: String, provinceCode: String? = null) {
        scope.launch {
            _state.update { it.copy(isLoadingOutlook = true) }
            
            jobBankApiClient.getJobOutlook(nocCode, provinceCode).onSuccess { response ->
                _state.update {
                    it.copy(
                        isLoadingOutlook = false,
                        jobOutlook = response.toDomainModel()
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoadingOutlook = false)
                }
            }
        }
    }
    
    /**
     * Load trending jobs
     */
    fun loadTrendingJobs(provinceCode: String? = null) {
        scope.launch {
            _state.update { it.copy(isLoadingTrending = true) }
            
            jobBankApiClient.getTrendingJobs(provinceCode, 10).onSuccess { response ->
                _state.update {
                    it.copy(
                        isLoadingTrending = false,
                        trendingJobs = response.jobs.map { job -> job.toDomainModel() }
                    )
                }
            }.onFailure {
                _state.update { it.copy(isLoadingTrending = false) }
            }
        }
    }
    
    /**
     * Load available provinces
     */
    private fun loadProvinces() {
        scope.launch {
            jobBankApiClient.getProvinces().onSuccess { provinces ->
                _state.update {
                    it.copy(provinces = provinces.map { p -> p.toDomainModel() })
                }
            }.onFailure {
                // Use default list on failure
                _state.update { it.copy(provinces = CanadianProvince.ALL_PROVINCES) }
            }
        }
    }
    
    /**
     * Clear selected job
     */
    fun clearSelectedJob() {
        _state.update { it.copy(selectedJob = null) }
    }
    
    /**
     * Clear search results
     */
    fun clearSearch() {
        _state.update {
            it.copy(
                searchResults = emptyList(),
                totalResults = 0,
                currentPage = 0,
                hasMoreResults = false,
                lastSearchQuery = null,
                lastLocationFilter = null,
                lastProvinceFilter = null,
                lastNocFilter = null,
                searchError = null
            )
        }
    }
    
    /**
     * Set language
     */
    fun setLanguage(locale: Locale) {
        LocaleManager.setLocale(locale)
        _state.update { it.copy(currentLocale = locale) }
    }
    
    /**
     * Toggle between English and French
     */
    fun toggleLanguage() {
        val newLocale = if (_state.value.currentLocale == Locale.ENGLISH) Locale.FRENCH else Locale.ENGLISH
        setLanguage(newLocale)
    }
}

/**
 * UI State for Job Bank screen
 */
data class JobBankState(
    // Search state
    val isSearching: Boolean = false,
    val searchResults: List<JobBankJob> = emptyList(),
    val totalResults: Int = 0,
    val currentPage: Int = 0,
    val hasMoreResults: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchError: String? = null,
    
    // Filter state
    val lastSearchQuery: String? = null,
    val lastLocationFilter: String? = null,
    val lastProvinceFilter: String? = null,
    val lastNocFilter: String? = null,
    
    // Job details state
    val selectedJob: JobBankJob? = null,
    val isLoadingDetails: Boolean = false,
    val detailsError: String? = null,
    
    // Job outlook
    val jobOutlook: JobOutlook? = null,
    val isLoadingOutlook: Boolean = false,
    
    // Trending jobs
    val trendingJobs: List<JobBankJob> = emptyList(),
    val isLoadingTrending: Boolean = false,
    
    // Reference data
    val provinces: List<CanadianProvince> = CanadianProvince.ALL_PROVINCES,
    
    // Settings
    val currentLocale: Locale = Locale.ENGLISH
) {
    val hasActiveFilters: Boolean
        get() = lastLocationFilter != null || lastProvinceFilter != null || lastNocFilter != null
    
    val hasSearchResults: Boolean
        get() = searchResults.isNotEmpty()
    
    val showTrending: Boolean
        get() = !isSearching && !hasSearchResults && trendingJobs.isNotEmpty()
}
