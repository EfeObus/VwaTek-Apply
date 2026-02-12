package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Phase 2: Job Application Tracker Database Tables
 * Supports the Kanban-style job application tracking system
 */

object JobApplicationsTable : Table("job_applications") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val resumeId = varchar("resume_id", 36).nullable()
    val coverLetterId = varchar("cover_letter_id", 36).nullable()
    
    // Job Details
    val jobTitle = varchar("job_title", 255)
    val companyName = varchar("company_name", 255)
    val companyLogo = text("company_logo").nullable()
    val jobUrl = text("job_url").nullable()
    val jobDescription = text("job_description").nullable()
    val jobBoardSource = varchar("job_board_source", 50).nullable() // INDEED, LINKEDIN, JOB_BANK, MANUAL
    val externalJobId = varchar("external_job_id", 100).nullable()
    
    // Location
    val city = varchar("city", 100).nullable()
    val province = varchar("province", 50).nullable() // Canadian provinces
    val country = varchar("country", 50).default("Canada")
    val isRemote = bool("is_remote").default(false)
    val isHybrid = bool("is_hybrid").default(false)
    
    // Compensation
    val salaryMin = integer("salary_min").nullable()
    val salaryMax = integer("salary_max").nullable()
    val salaryCurrency = varchar("salary_currency", 3).default("CAD")
    val salaryPeriod = varchar("salary_period", 20).nullable() // HOURLY, ANNUAL
    
    // Application Status
    val status = varchar("status", 30).default("SAVED")
    val appliedAt = timestamp("applied_at").nullable()
    val responseReceivedAt = timestamp("response_received_at").nullable()
    
    // Canadian Specific
    val nocCode = varchar("noc_code", 10).nullable()
    val requiresWorkPermit = bool("requires_work_permit").nullable()
    val isLmiaRequired = bool("is_lmia_required").nullable()
    
    // Contact Info
    val contactName = varchar("contact_name", 255).nullable()
    val contactEmail = varchar("contact_email", 255).nullable()
    val contactPhone = varchar("contact_phone", 50).nullable()
    
    // Sync
    val syncStatus = varchar("sync_status", 20).default("SYNCED")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationStatusHistoryTable : Table("job_application_status_history") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val fromStatus = varchar("from_status", 30).nullable()
    val toStatus = varchar("to_status", 30)
    val notes = text("notes").nullable()
    val changedAt = timestamp("changed_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationNotesTable : Table("job_application_notes") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val content = text("content")
    val noteType = varchar("note_type", 30).default("GENERAL") // GENERAL, INTERVIEW, FOLLOW_UP, RESEARCH
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationRemindersTable : Table("job_application_reminders") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val reminderType = varchar("reminder_type", 30) // FOLLOW_UP, INTERVIEW, DEADLINE, CUSTOM
    val title = varchar("title", 255)
    val message = text("message").nullable()
    val reminderAt = timestamp("reminder_at")
    val isCompleted = bool("is_completed").default(false)
    val completedAt = timestamp("completed_at").nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationInterviewsTable : Table("job_application_interviews") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val interviewType = varchar("interview_type", 30) // PHONE, VIDEO, ONSITE, TECHNICAL, BEHAVIORAL
    val scheduledAt = timestamp("scheduled_at")
    val duration = integer("duration_minutes").nullable()
    val location = text("location").nullable() // Address or video link
    val interviewerName = varchar("interviewer_name", 255).nullable()
    val interviewerTitle = varchar("interviewer_title", 255).nullable()
    val notes = text("notes").nullable()
    val feedback = text("feedback").nullable()
    val status = varchar("status", 30).default("SCHEDULED") // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationDocumentsTable : Table("job_application_documents") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val documentType = varchar("document_type", 30) // RESUME, COVER_LETTER, PORTFOLIO, OTHER
    val fileName = varchar("file_name", 255)
    val fileUrl = text("file_url")
    val fileSize = long("file_size").nullable()
    val mimeType = varchar("mime_type", 100).nullable()
    val uploadedAt = timestamp("uploaded_at")
    
    override val primaryKey = PrimaryKey(id)
}
