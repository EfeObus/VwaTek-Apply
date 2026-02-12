package com.vwatek.apply.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Phase 2: Job Application Tracker Domain Models
 * Shared across all platforms (Android, iOS, Web)
 */

@Serializable
data class JobApplication(
    val id: String,
    val userId: String,
    val resumeId: String? = null,
    val coverLetterId: String? = null,
    val jobTitle: String,
    val companyName: String,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobDescription: String? = null,
    val jobBoardSource: JobBoardSource? = null,
    val externalJobId: String? = null,
    val city: String? = null,
    val province: CanadianProvince? = null,
    val country: String = "Canada",
    val isRemote: Boolean = false,
    val isHybrid: Boolean = false,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryCurrency: String = "CAD",
    val salaryPeriod: SalaryPeriod? = null,
    val status: ApplicationStatus = ApplicationStatus.SAVED,
    val appliedAt: Instant? = null,
    val responseReceivedAt: Instant? = null,
    val nocCode: String? = null,
    val requiresWorkPermit: Boolean? = null,
    val isLmiaRequired: Boolean? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val notes: List<ApplicationNote> = emptyList(),
    val reminders: List<ApplicationReminder> = emptyList(),
    val interviews: List<ApplicationInterview> = emptyList(),
    val statusHistory: List<StatusChange> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val salaryDisplay: String?
        get() {
            if (salaryMin == null) return null
            val min = formatSalary(salaryMin)
            val max = salaryMax?.let { formatSalary(it) }
            val period = salaryPeriod?.displayName ?: ""
            return if (max != null && max != min) {
                "$min - $max $period"
            } else {
                "$min $period"
            }
        }
    
    val locationDisplay: String
        get() = buildString {
            if (isRemote) {
                append("Remote")
                if (city != null || province != null) append(" • ")
            }
            if (city != null) {
                append(city)
                if (province != null) append(", ")
            }
            province?.let { append(it.code) }
        }.ifEmpty { "Location not specified" }
    
    private fun formatSalary(amount: Int): String {
        val symbol = when (salaryCurrency) {
            "CAD", "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "$"
        }
        return "$symbol${amount.toString().reversed().chunked(3).joinToString(",").reversed()}"
    }
}

@Serializable
enum class ApplicationStatus(val displayName: String, val colorHex: Long, val order: Int) {
    SAVED("Saved", 0xFF9E9E9E, 0),              // Grey - Just saved, not applied yet
    APPLIED("Applied", 0xFF2196F3, 1),          // Blue - Application submitted
    VIEWED("Viewed", 0xFF03A9F4, 2),            // Light Blue - Employer viewed
    PHONE_SCREEN("Phone Screen", 0xFF00BCD4, 3), // Cyan - Initial call scheduled
    INTERVIEW("Interview", 0xFF009688, 4),       // Teal - Interview scheduled
    ASSESSMENT("Assessment", 0xFF4CAF50, 5),     // Green - Task/assessment stage
    FINAL_ROUND("Final Round", 0xFF8BC34A, 6),   // Light Green - Final interviews
    OFFER("Offer Received", 0xFFCDDC39, 7),      // Lime - Received offer
    NEGOTIATING("Negotiating", 0xFFFFEB3B, 8),   // Yellow - Negotiating terms
    ACCEPTED("Accepted", 0xFF4CAF50, 9),         // Green - Accepted offer
    REJECTED("Rejected", 0xFFF44336, 10),        // Red - Rejected/declined
    WITHDRAWN("Withdrawn", 0xFF757575, 11),      // Grey - Withdrew application
    NO_RESPONSE("No Response", 0xFFFF9800, 12);  // Orange - No response after time
    
    companion object {
        val activeStatuses = listOf(SAVED, APPLIED, VIEWED, PHONE_SCREEN, INTERVIEW, ASSESSMENT, FINAL_ROUND, OFFER, NEGOTIATING)
        val interviewStatuses = listOf(PHONE_SCREEN, INTERVIEW, FINAL_ROUND)
        val closedStatuses = listOf(ACCEPTED, REJECTED, WITHDRAWN, NO_RESPONSE)
        
        // Default Kanban columns
        val kanbanColumns = listOf(SAVED, APPLIED, INTERVIEW, OFFER)
    }
}

@Serializable
enum class CanadianProvince(val code: String, val fullName: String) {
    AB("AB", "Alberta"),
    BC("BC", "British Columbia"),
    MB("MB", "Manitoba"),
    NB("NB", "New Brunswick"),
    NL("NL", "Newfoundland and Labrador"),
    NS("NS", "Nova Scotia"),
    NT("NT", "Northwest Territories"),
    NU("NU", "Nunavut"),
    ON("ON", "Ontario"),
    PE("PE", "Prince Edward Island"),
    QC("QC", "Quebec"),
    SK("SK", "Saskatchewan"),
    YT("YT", "Yukon");
    
    companion object {
        fun fromCode(code: String): CanadianProvince? = entries.find { it.code == code }
        fun fromName(name: String): CanadianProvince? = entries.find { 
            it.fullName.equals(name, ignoreCase = true) 
        }
    }
}

@Serializable
enum class JobBoardSource(val displayName: String, val domain: String?) {
    INDEED("Indeed", "indeed.com"),
    INDEED_CA("Indeed Canada", "indeed.ca"),
    LINKEDIN("LinkedIn", "linkedin.com"),
    JOB_BANK("Job Bank Canada", "jobbank.gc.ca"),
    GLASSDOOR("Glassdoor", "glassdoor.com"),
    GLASSDOOR_CA("Glassdoor Canada", "glassdoor.ca"),
    WORKDAY("Workday", "workday.com"),
    MONSTER("Monster", "monster.ca"),
    WORKOPOLIS("Workopolis", "workopolis.com"),
    COMPANY_SITE("Company Website", null),
    REFERRAL("Referral", null),
    NETWORKING("Networking", null),
    RECRUITER("Recruiter", null),
    MANUAL("Manual Entry", null);
    
    companion object {
        fun fromUrl(url: String): JobBoardSource {
            return entries.find { it.domain != null && url.contains(it.domain) } ?: MANUAL
        }
    }
}

@Serializable
enum class SalaryPeriod(val displayName: String) {
    HOURLY("per hour"),
    DAILY("per day"),
    WEEKLY("per week"),
    BIWEEKLY("bi-weekly"),
    MONTHLY("per month"),
    ANNUAL("per year")
}

@Serializable
data class ApplicationNote(
    val id: String,
    val applicationId: String,
    val content: String,
    val noteType: NoteType = NoteType.GENERAL,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class NoteType(val displayName: String) {
    GENERAL("General"),
    INTERVIEW("Interview"),
    FOLLOW_UP("Follow-up"),
    RESEARCH("Research"),
    FEEDBACK("Feedback"),
    QUESTION("Questions")
}

@Serializable
data class ApplicationReminder(
    val id: String,
    val applicationId: String,
    val reminderType: ReminderType,
    val title: String,
    val message: String? = null,
    val reminderAt: Instant,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant
)

@Serializable
enum class ReminderType(val displayName: String) {
    FOLLOW_UP("Follow Up"),
    INTERVIEW("Interview"),
    DEADLINE("Deadline"),
    ASSESSMENT("Assessment Due"),
    CUSTOM("Custom")
}

@Serializable
data class ApplicationInterview(
    val id: String,
    val applicationId: String,
    val interviewType: InterviewType,
    val scheduledAt: Instant,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val interviewerName: String? = null,
    val interviewerTitle: String? = null,
    val notes: String? = null,
    val feedback: String? = null,
    val status: ScheduledInterviewStatus = ScheduledInterviewStatus.SCHEDULED,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class InterviewType(val displayName: String) {
    PHONE("Phone Screen"),
    VIDEO("Video Call"),
    ONSITE("On-site"),
    TECHNICAL("Technical"),
    BEHAVIORAL("Behavioral"),
    PANEL("Panel"),
    CASE_STUDY("Case Study"),
    FINAL("Final Round")
}

@Serializable
enum class ScheduledInterviewStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    RESCHEDULED("Rescheduled"),
    NO_SHOW("No Show")
}

@Serializable
data class StatusChange(
    val id: String,
    val applicationId: String,
    val fromStatus: ApplicationStatus?,
    val toStatus: ApplicationStatus,
    val notes: String? = null,
    val changedAt: Instant
)

@Serializable
data class TrackerStats(
    val totalApplications: Int = 0,
    val savedCount: Int = 0,
    val appliedCount: Int = 0,
    val interviewCount: Int = 0,
    val offerCount: Int = 0,
    val rejectedCount: Int = 0,
    val interviewRate: Float = 0f,      // interviews / applied
    val offerRate: Float = 0f,          // offers / interviews
    val responseRate: Float = 0f,       // responses / applied
    val averageResponseDays: Float = 0f,
    val averageTimeToInterview: Float = 0f,
    val weeklyApplications: List<WeeklyApplicationData> = emptyList(),
    val applicationsBySource: Map<JobBoardSource, Int> = emptyMap(),
    val applicationsByProvince: Map<CanadianProvince, Int> = emptyMap()
)

@Serializable
data class WeeklyApplicationData(
    val weekStart: Instant,
    val count: Int
)
