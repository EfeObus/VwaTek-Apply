package com.vwatek.apply.presentation.tracker

import com.vwatek.apply.domain.model.*
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

/**
 * Phase 2: Job Application Tracker ViewModel
 * Manages state for Kanban board, list view, and calendar view of job applications
 */
class TrackerViewModel(
    private val getApplicationsUseCase: GetJobApplicationsUseCase,
    private val getApplicationByIdUseCase: GetJobApplicationByIdUseCase,
    private val createApplicationUseCase: CreateJobApplicationUseCase,
    private val updateApplicationUseCase: UpdateJobApplicationUseCase,
    private val updateStatusUseCase: UpdateApplicationStatusUseCase,
    private val deleteApplicationUseCase: DeleteJobApplicationUseCase,
    private val addNoteUseCase: AddApplicationNoteUseCase,
    private val addReminderUseCase: AddApplicationReminderUseCase,
    private val addInterviewUseCase: AddApplicationInterviewUseCase,
    private val getStatsUseCase: GetTrackerStatsUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _state = MutableStateFlow(TrackerState())
    val state: StateFlow<TrackerState> = _state.asStateFlow()
    
    private var currentUserId: String? = null
    
    init {
        loadApplications()
        loadStats()
    }
    
    fun onIntent(intent: TrackerIntent) {
        when (intent) {
            is TrackerIntent.LoadApplications -> loadApplications()
            is TrackerIntent.RefreshApplications -> refreshApplications()
            is TrackerIntent.SetCurrentUserId -> setCurrentUserId(intent.userId)
            is TrackerIntent.SelectApplication -> selectApplication(intent.id)
            is TrackerIntent.ClearSelectedApplication -> clearSelectedApplication()
            is TrackerIntent.CreateApplication -> createApplication(intent.request)
            is TrackerIntent.UpdateApplication -> updateApplication(intent.id, intent.request)
            is TrackerIntent.UpdateStatus -> updateStatus(intent.id, intent.newStatus, intent.notes)
            is TrackerIntent.DeleteApplication -> deleteApplication(intent.id)
            is TrackerIntent.AddNote -> addNote(intent.applicationId, intent.content, intent.noteType)
            is TrackerIntent.AddReminder -> addReminder(intent.applicationId, intent.reminder)
            is TrackerIntent.AddInterview -> addInterview(intent.applicationId, intent.interview)
            is TrackerIntent.SetViewMode -> setViewMode(intent.mode)
            is TrackerIntent.SetFilterStatus -> setFilterStatus(intent.status)
            is TrackerIntent.SetFilterSource -> setFilterSource(intent.source)
            is TrackerIntent.SetFilterProvince -> setFilterProvince(intent.province)
            is TrackerIntent.SetSearchQuery -> setSearchQuery(intent.query)
            is TrackerIntent.ClearFilters -> clearFilters()
            is TrackerIntent.LoadStats -> loadStats()
            is TrackerIntent.ClearError -> clearError()
            is TrackerIntent.MoveToStatus -> moveToStatus(intent.applicationId, intent.targetStatus)
        }
    }
    
    private fun setCurrentUserId(userId: String?) {
        currentUserId = userId
        loadApplications()
        loadStats()
    }
    
    private fun loadApplications() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val currentState = _state.value
                val result = getApplicationsUseCase(
                    status = currentState.filterStatus?.name,
                    source = currentState.filterSource?.name,
                    province = currentState.filterProvince?.code,
                    search = currentState.searchQuery
                )
                result.fold(
                    onSuccess = { applications ->
                        val grouped = groupByStatus(applications)
                        _state.update { it.copy(
                            applications = applications,
                            kanbanColumns = grouped,
                            isLoading = false
                        )}
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message, isLoading = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private fun refreshApplications() {
        loadApplications()
    }
    
    private fun selectApplication(id: String) {
        scope.launch {
            _state.update { it.copy(isLoadingDetail = true) }
            try {
                val result = getApplicationByIdUseCase(id)
                result.fold(
                    onSuccess = { detail ->
                        _state.update { it.copy(
                            selectedApplication = detail,
                            isLoadingDetail = false
                        )}
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message, isLoadingDetail = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoadingDetail = false) }
            }
        }
    }
    
    private fun clearSelectedApplication() {
        _state.update { it.copy(selectedApplication = null) }
    }
    
    private fun createApplication(request: CreateJobApplicationRequest) {
        scope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val result = createApplicationUseCase(request)
                result.fold(
                    onSuccess = { id ->
                        _state.update { it.copy(isSubmitting = false) }
                        loadApplications()
                        loadStats()
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message, isSubmitting = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isSubmitting = false) }
            }
        }
    }
    
    private fun updateApplication(id: String, request: UpdateJobApplicationRequest) {
        scope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val result = updateApplicationUseCase(id, request)
                result.fold(
                    onSuccess = {
                        _state.update { it.copy(isSubmitting = false) }
                        loadApplications()
                        // Refresh selected application if it's the one being updated
                        if (_state.value.selectedApplication?.application?.id == id) {
                            selectApplication(id)
                        }
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message, isSubmitting = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isSubmitting = false) }
            }
        }
    }
    
    private fun updateStatus(id: String, newStatus: ApplicationStatus, notes: String?) {
        scope.launch {
            try {
                val result = updateStatusUseCase(id, newStatus.name, notes)
                result.fold(
                    onSuccess = {
                        loadApplications()
                        loadStats()
                        // Refresh selected application if it's the one being updated
                        if (_state.value.selectedApplication?.application?.id == id) {
                            selectApplication(id)
                        }
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    /**
     * Handle drag-and-drop status change in Kanban board
     */
    private fun moveToStatus(applicationId: String, targetStatus: ApplicationStatus) {
        // Optimistically update the UI
        val currentApplications = _state.value.applications
        val updatedApplications = currentApplications.map { app ->
            if (app.id == applicationId) app.copy(status = targetStatus) else app
        }
        val grouped = groupByStatus(updatedApplications)
        _state.update { it.copy(
            applications = updatedApplications,
            kanbanColumns = grouped
        )}
        
        // Then update on server
        updateStatus(applicationId, targetStatus, null)
    }
    
    private fun deleteApplication(id: String) {
        scope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val result = deleteApplicationUseCase(id)
                result.fold(
                    onSuccess = {
                        _state.update { it.copy(
                            isSubmitting = false,
                            selectedApplication = null
                        )}
                        loadApplications()
                        loadStats()
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message, isSubmitting = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isSubmitting = false) }
            }
        }
    }
    
    private fun addNote(applicationId: String, content: String, noteType: NoteType) {
        scope.launch {
            try {
                val result = addNoteUseCase(applicationId, content, noteType.name)
                result.fold(
                    onSuccess = {
                        // Refresh the selected application to show new note
                        selectApplication(applicationId)
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun addReminder(applicationId: String, reminder: CreateReminderRequest) {
        scope.launch {
            try {
                val result = addReminderUseCase(applicationId, reminder)
                result.fold(
                    onSuccess = {
                        selectApplication(applicationId)
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun addInterview(applicationId: String, interview: CreateInterviewRequest) {
        scope.launch {
            try {
                val result = addInterviewUseCase(applicationId, interview)
                result.fold(
                    onSuccess = {
                        selectApplication(applicationId)
                        loadApplications() // Interview might change status
                    },
                    onFailure = { error ->
                        _state.update { it.copy(error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun setViewMode(mode: TrackerViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }
    
    private fun setFilterStatus(status: ApplicationStatus?) {
        _state.update { it.copy(filterStatus = status) }
        loadApplications()
    }
    
    private fun setFilterSource(source: JobBoardSource?) {
        _state.update { it.copy(filterSource = source) }
        loadApplications()
    }
    
    private fun setFilterProvince(province: CanadianProvince?) {
        _state.update { it.copy(filterProvince = province) }
        loadApplications()
    }
    
    private fun setSearchQuery(query: String?) {
        _state.update { it.copy(searchQuery = query) }
        // Debounce search - in real implementation, use a debounce mechanism
        loadApplications()
    }
    
    private fun clearFilters() {
        _state.update { it.copy(
            filterStatus = null,
            filterSource = null,
            filterProvince = null,
            searchQuery = null
        )}
        loadApplications()
    }
    
    private fun loadStats() {
        scope.launch {
            try {
                val result = getStatsUseCase()
                result.fold(
                    onSuccess = { stats ->
                        _state.update { it.copy(stats = stats) }
                    },
                    onFailure = { error ->
                        // Stats loading failure is not critical
                        println("Failed to load stats: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                println("Failed to load stats: ${e.message}")
            }
        }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Group applications by status for Kanban board display
     */
    private fun groupByStatus(applications: List<JobApplication>): Map<ApplicationStatus, List<JobApplication>> {
        return ApplicationStatus.entries.associateWith { status ->
            applications.filter { it.status == status }
        }
    }
}

// ===== State =====

data class TrackerState(
    val applications: List<JobApplication> = emptyList(),
    val kanbanColumns: Map<ApplicationStatus, List<JobApplication>> = emptyMap(),
    val selectedApplication: JobApplicationDetail? = null,
    val stats: TrackerStats? = null,
    val viewMode: TrackerViewMode = TrackerViewMode.KANBAN,
    val filterStatus: ApplicationStatus? = null,
    val filterSource: JobBoardSource? = null,
    val filterProvince: CanadianProvince? = null,
    val searchQuery: String? = null,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

enum class TrackerViewMode {
    KANBAN,
    LIST,
    CALENDAR
}

// ===== Intents =====

sealed interface TrackerIntent {
    data object LoadApplications : TrackerIntent
    data object RefreshApplications : TrackerIntent
    data class SetCurrentUserId(val userId: String?) : TrackerIntent
    data class SelectApplication(val id: String) : TrackerIntent
    data object ClearSelectedApplication : TrackerIntent
    data class CreateApplication(val request: CreateJobApplicationRequest) : TrackerIntent
    data class UpdateApplication(val id: String, val request: UpdateJobApplicationRequest) : TrackerIntent
    data class UpdateStatus(val id: String, val newStatus: ApplicationStatus, val notes: String? = null) : TrackerIntent
    data class DeleteApplication(val id: String) : TrackerIntent
    data class AddNote(val applicationId: String, val content: String, val noteType: NoteType = NoteType.GENERAL) : TrackerIntent
    data class AddReminder(val applicationId: String, val reminder: CreateReminderRequest) : TrackerIntent
    data class AddInterview(val applicationId: String, val interview: CreateInterviewRequest) : TrackerIntent
    data class SetViewMode(val mode: TrackerViewMode) : TrackerIntent
    data class SetFilterStatus(val status: ApplicationStatus?) : TrackerIntent
    data class SetFilterSource(val source: JobBoardSource?) : TrackerIntent
    data class SetFilterProvince(val province: CanadianProvince?) : TrackerIntent
    data class SetSearchQuery(val query: String?) : TrackerIntent
    data object ClearFilters : TrackerIntent
    data object LoadStats : TrackerIntent
    data object ClearError : TrackerIntent
    data class MoveToStatus(val applicationId: String, val targetStatus: ApplicationStatus) : TrackerIntent
}

// ===== Request Models =====

data class CreateJobApplicationRequest(
    val jobTitle: String,
    val companyName: String,
    val resumeId: String? = null,
    val coverLetterId: String? = null,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobDescription: String? = null,
    val jobBoardSource: JobBoardSource? = null,
    val externalJobId: String? = null,
    val city: String? = null,
    val province: CanadianProvince? = null,
    val country: String? = "Canada",
    val isRemote: Boolean = false,
    val isHybrid: Boolean = false,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryCurrency: String = "CAD",
    val salaryPeriod: SalaryPeriod? = null,
    val status: ApplicationStatus = ApplicationStatus.SAVED,
    val appliedAt: String? = null,
    val nocCode: String? = null,
    val requiresWorkPermit: Boolean = false,
    val isLmiaRequired: Boolean = false,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null
)

data class UpdateJobApplicationRequest(
    val jobTitle: String? = null,
    val companyName: String? = null,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobDescription: String? = null,
    val city: String? = null,
    val province: CanadianProvince? = null,
    val isRemote: Boolean? = null,
    val isHybrid: Boolean? = null,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryPeriod: SalaryPeriod? = null,
    val nocCode: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null
)

data class CreateReminderRequest(
    val reminderType: ReminderType,
    val title: String,
    val message: String? = null,
    val reminderAt: String // ISO 8601 datetime
)

data class CreateInterviewRequest(
    val interviewType: InterviewType,
    val scheduledAt: String, // ISO 8601 datetime
    val durationMinutes: Int? = null,
    val location: String? = null,
    val interviewerName: String? = null,
    val interviewerTitle: String? = null,
    val notes: String? = null
)

// ===== Detail Model =====

data class JobApplicationDetail(
    val application: JobApplication,
    val notes: List<ApplicationNote>,
    val reminders: List<ApplicationReminder>,
    val interviews: List<ApplicationInterview>,
    val statusHistory: List<StatusChange>
)

data class StatusChange(
    val id: String,
    val fromStatus: ApplicationStatus?,
    val toStatus: ApplicationStatus,
    val notes: String?,
    val changedAt: String
)

// ===== Use Case Interfaces =====

interface GetJobApplicationsUseCase {
    suspend operator fun invoke(
        status: String? = null,
        source: String? = null,
        province: String? = null,
        search: String? = null
    ): Result<List<JobApplication>>
}

interface GetJobApplicationByIdUseCase {
    suspend operator fun invoke(id: String): Result<JobApplicationDetail>
}

interface CreateJobApplicationUseCase {
    suspend operator fun invoke(request: CreateJobApplicationRequest): Result<String>
}

interface UpdateJobApplicationUseCase {
    suspend operator fun invoke(id: String, request: UpdateJobApplicationRequest): Result<Unit>
}

interface UpdateApplicationStatusUseCase {
    suspend operator fun invoke(id: String, status: String, notes: String?): Result<Unit>
}

interface DeleteJobApplicationUseCase {
    suspend operator fun invoke(id: String): Result<Unit>
}

interface AddApplicationNoteUseCase {
    suspend operator fun invoke(applicationId: String, content: String, noteType: String): Result<String>
}

interface AddApplicationReminderUseCase {
    suspend operator fun invoke(applicationId: String, reminder: CreateReminderRequest): Result<String>
}

interface AddApplicationInterviewUseCase {
    suspend operator fun invoke(applicationId: String, interview: CreateInterviewRequest): Result<String>
}

interface GetTrackerStatsUseCase {
    suspend operator fun invoke(): Result<TrackerStats>
}
