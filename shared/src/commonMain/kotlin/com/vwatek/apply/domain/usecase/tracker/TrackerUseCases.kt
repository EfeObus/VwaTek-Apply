package com.vwatek.apply.domain.usecase.tracker

import com.vwatek.apply.data.api.*
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.presentation.tracker.*

/**
 * Phase 2: Job Tracker Use Case Implementations
 * Bridges TrackerViewModel to JobTrackerApiClient
 */

// ===== Get Applications Use Case =====

class GetJobApplicationsUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : GetJobApplicationsUseCase {
    
    override suspend fun invoke(
        status: String?,
        source: String?,
        province: String?,
        search: String?
    ): Result<List<JobApplication>> {
        return apiClient.getApplications(
            status = status,
            source = source,
            province = province,
            search = search
        ).map { response ->
            response.applications.map { it.toDomainModel() }
        }
    }
}

// ===== Get Application By ID Use Case =====

class GetJobApplicationByIdUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : GetJobApplicationByIdUseCase {
    
    override suspend fun invoke(id: String): Result<JobApplicationDetail> {
        return apiClient.getApplication(id).map { it.toDomainModel() }
    }
}

// ===== Create Application Use Case =====

class CreateJobApplicationUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : CreateJobApplicationUseCase {
    
    override suspend fun invoke(request: CreateJobApplicationRequest): Result<String> {
        return apiClient.createApplication(request.toDto()).map { it.id }
    }
}

// ===== Update Application Use Case =====

class UpdateJobApplicationUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : UpdateJobApplicationUseCase {
    
    override suspend fun invoke(id: String, request: UpdateJobApplicationRequest): Result<Unit> {
        return apiClient.updateApplication(id, request.toDto())
    }
}

// ===== Update Status Use Case =====

class UpdateApplicationStatusUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : UpdateApplicationStatusUseCase {
    
    override suspend fun invoke(id: String, status: String, notes: String?): Result<Unit> {
        return apiClient.updateStatus(id, status, notes)
    }
}

// ===== Delete Application Use Case =====

class DeleteJobApplicationUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : DeleteJobApplicationUseCase {
    
    override suspend fun invoke(id: String): Result<Unit> {
        return apiClient.deleteApplication(id)
    }
}

// ===== Add Note Use Case =====

class AddApplicationNoteUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : AddApplicationNoteUseCase {
    
    override suspend fun invoke(
        applicationId: String, 
        content: String, 
        noteType: String
    ): Result<String> {
        return apiClient.addNote(applicationId, content, noteType)
    }
}

// ===== Add Reminder Use Case =====

class AddApplicationReminderUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : AddApplicationReminderUseCase {
    
    override suspend fun invoke(
        applicationId: String, 
        reminder: CreateReminderRequest
    ): Result<String> {
        return apiClient.addReminder(
            applicationId = applicationId,
            reminderType = reminder.reminderType.name, // Convert enum to String
            title = reminder.title,
            message = reminder.message,
            reminderAt = reminder.reminderAt
        )
    }
}

// ===== Add Interview Use Case =====

class AddApplicationInterviewUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : AddApplicationInterviewUseCase {
    
    override suspend fun invoke(
        applicationId: String, 
        interview: CreateInterviewRequest
    ): Result<String> {
        return apiClient.addInterview(
            applicationId = applicationId,
            interviewType = interview.interviewType.name, // Convert enum to String
            scheduledAt = interview.scheduledAt,
            durationMinutes = interview.durationMinutes,
            location = interview.location,
            interviewerName = interview.interviewerName,
            interviewerTitle = interview.interviewerTitle,
            notes = interview.notes
        )
    }
}

// ===== Get Stats Use Case =====

class GetTrackerStatsUseCaseImpl(
    private val apiClient: JobTrackerApiClient
) : GetTrackerStatsUseCase {
    
    override suspend fun invoke(): Result<TrackerStats> {
        return apiClient.getStats().map { it.toDomainModel() }
    }
}

// ===== DTO to Domain Mappers =====

private fun JobApplicationDto.toDomainModel(): JobApplication {
    val now = kotlinx.datetime.Clock.System.now()
    return JobApplication(
        id = id,
        userId = "", // Not returned from API, will be set by backend based on auth
        jobTitle = jobTitle,
        companyName = companyName,
        companyLogo = companyLogo,
        jobUrl = jobUrl,
        jobBoardSource = jobBoardSource?.let { 
            try { JobBoardSource.valueOf(it) } catch (e: Exception) { null }
        },
        city = city,
        province = province?.let {
            try { CanadianProvince.fromCode(it) } catch (e: Exception) { null }
        },
        isRemote = isRemote,
        isHybrid = isHybrid,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        salaryCurrency = salaryCurrency,
        salaryPeriod = salaryPeriod?.let {
            try { SalaryPeriod.valueOf(it) } catch (e: Exception) { null }
        },
        status = try { ApplicationStatus.valueOf(status) } catch (e: Exception) { ApplicationStatus.SAVED },
        appliedAt = appliedAt?.let { 
            try { kotlinx.datetime.Instant.parse(it) } catch (e: Exception) { null }
        },
        nocCode = nocCode,
        createdAt = try { kotlinx.datetime.Instant.parse(createdAt) } catch (e: Exception) { now },
        updatedAt = try { kotlinx.datetime.Instant.parse(updatedAt) } catch (e: Exception) { now }
    )
}

private fun JobApplicationDetailDto.toDomainModel(): JobApplicationDetail {
    val app = application.toDomainModel()
    return JobApplicationDetail(
        application = app,
        notes = notes.map { it.toDomainModel(app.id) },
        reminders = reminders.map { it.toDomainModel(app.id) },
        interviews = interviews.map { it.toDomainModel(app.id) },
        statusHistory = statusHistory.map { it.toDomainModel() }
    )
}

private fun NoteDto.toDomainModel(applicationId: String): ApplicationNote {
    val now = kotlinx.datetime.Clock.System.now()
    return ApplicationNote(
        id = id,
        applicationId = applicationId,
        content = content,
        noteType = try { NoteType.valueOf(noteType) } catch (e: Exception) { NoteType.GENERAL },
        createdAt = try { kotlinx.datetime.Instant.parse(createdAt) } catch (e: Exception) { now },
        updatedAt = try { kotlinx.datetime.Instant.parse(updatedAt) } catch (e: Exception) { now }
    )
}

private fun ReminderDto.toDomainModel(applicationId: String): ApplicationReminder {
    val now = kotlinx.datetime.Clock.System.now()
    return ApplicationReminder(
        id = id,
        applicationId = applicationId,
        reminderType = try { ReminderType.valueOf(reminderType) } catch (e: Exception) { ReminderType.FOLLOW_UP },
        title = title,
        message = message,
        reminderAt = try { kotlinx.datetime.Instant.parse(reminderAt) } catch (e: Exception) { now },
        isCompleted = isCompleted,
        completedAt = completedAt?.let {
            try { kotlinx.datetime.Instant.parse(it) } catch (e: Exception) { null }
        },
        createdAt = now // Not returned from API
    )
}

private fun InterviewDto.toDomainModel(applicationId: String): ApplicationInterview {
    val now = kotlinx.datetime.Clock.System.now()
    return ApplicationInterview(
        id = id,
        applicationId = applicationId,
        interviewType = try { InterviewType.valueOf(interviewType) } catch (e: Exception) { InterviewType.PHONE },
        scheduledAt = try { kotlinx.datetime.Instant.parse(scheduledAt) } catch (e: Exception) { now },
        durationMinutes = durationMinutes,
        location = location,
        interviewerName = interviewerName,
        interviewerTitle = interviewerTitle,
        status = try { ScheduledInterviewStatus.valueOf(status) } catch (e: Exception) { ScheduledInterviewStatus.SCHEDULED },
        createdAt = now, // Not returned from API
        updatedAt = now  // Not returned from API
    )
}

// Note: Using TrackerViewModel's StatusChange (String changedAt, no applicationId)
// not the domain model's StatusChange (Instant changedAt, has applicationId)
private fun StatusChangeDto.toDomainModel(): com.vwatek.apply.presentation.tracker.StatusChange {
    return com.vwatek.apply.presentation.tracker.StatusChange(
        id = id,
        fromStatus = fromStatus?.let { 
            try { ApplicationStatus.valueOf(it) } catch (e: Exception) { null }
        },
        toStatus = try { ApplicationStatus.valueOf(toStatus) } catch (e: Exception) { ApplicationStatus.SAVED },
        notes = notes,
        changedAt = changedAt
    )
}

private fun TrackerStatsDto.toDomainModel() = TrackerStats(
    totalApplications = totalApplications,
    savedCount = savedCount,
    appliedCount = appliedCount,
    interviewCount = interviewCount,
    offerCount = offerCount,
    rejectedCount = rejectedCount,
    interviewRate = interviewRate / 100f, // Convert from percentage to decimal
    offerRate = offerRate / 100f
)

// ===== Domain to DTO Mappers =====

private fun CreateJobApplicationRequest.toDto() = CreateJobApplicationDto(
    jobTitle = jobTitle,
    companyName = companyName,
    resumeId = resumeId,
    coverLetterId = coverLetterId,
    companyLogo = companyLogo,
    jobUrl = jobUrl,
    jobDescription = jobDescription,
    jobBoardSource = jobBoardSource?.name,
    externalJobId = externalJobId,
    city = city,
    province = province?.code,
    country = country,
    isRemote = isRemote,
    isHybrid = isHybrid,
    salaryMin = salaryMin,
    salaryMax = salaryMax,
    salaryCurrency = salaryCurrency,
    salaryPeriod = salaryPeriod?.name,
    status = status?.name,
    appliedAt = appliedAt,
    nocCode = nocCode,
    requiresWorkPermit = requiresWorkPermit,
    isLmiaRequired = isLmiaRequired,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone
)

private fun UpdateJobApplicationRequest.toDto() = UpdateJobApplicationDto(
    jobTitle = jobTitle,
    companyName = companyName,
    companyLogo = companyLogo,
    jobUrl = jobUrl,
    jobDescription = jobDescription,
    city = city,
    province = province?.code,
    isRemote = isRemote,
    isHybrid = isHybrid,
    salaryMin = salaryMin,
    salaryMax = salaryMax,
    salaryPeriod = salaryPeriod?.name,
    nocCode = nocCode,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone
)
