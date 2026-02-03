package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// Users Table
object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255).nullable()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val phone = varchar("phone", 20).nullable()
    val street = varchar("street", 255).nullable()
    val city = varchar("city", 100).nullable()
    val state = varchar("state", 100).nullable()
    val zipCode = varchar("zip_code", 20).nullable()
    val country = varchar("country", 100).nullable()
    val profileImageUrl = text("profile_image_url").nullable()
    val authProvider = varchar("auth_provider", 20) // EMAIL, GOOGLE, LINKEDIN
    val linkedInProfileUrl = text("linkedin_profile_url").nullable()
    val emailVerified = bool("email_verified").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Resumes Table
object ResumesTable : Table("resumes") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).nullable()
    val name = varchar("name", 255)
    val content = text("content")
    val industry = varchar("industry", 100).nullable()
    val sourceType = varchar("source_type", 20).default("MANUAL") // MANUAL, LINKEDIN, UPLOAD
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Resume Versions Table
object ResumeVersionsTable : Table("resume_versions") {
    val id = varchar("id", 36)
    val resumeId = varchar("resume_id", 36).references(ResumesTable.id)
    val versionNumber = integer("version_number")
    val content = text("content")
    val changeDescription = text("change_description").nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Resume Analyses Table
object ResumeAnalysesTable : Table("resume_analyses") {
    val id = varchar("id", 36)
    val resumeId = varchar("resume_id", 36).references(ResumesTable.id)
    val jobDescription = text("job_description")
    val matchScore = integer("match_score")
    val missingKeywords = text("missing_keywords") // JSON array
    val recommendations = text("recommendations") // JSON array
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Cover Letters Table
object CoverLettersTable : Table("cover_letters") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).nullable()
    val resumeId = varchar("resume_id", 36).references(ResumesTable.id).nullable()
    val jobTitle = varchar("job_title", 255)
    val companyName = varchar("company_name", 255)
    val content = text("content")
    val tone = varchar("tone", 50) // PROFESSIONAL, FRIENDLY, ENTHUSIASTIC, FORMAL
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Interview Sessions Table
object InterviewSessionsTable : Table("interview_sessions") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).nullable()
    val resumeId = varchar("resume_id", 36).references(ResumesTable.id).nullable()
    val jobTitle = varchar("job_title", 255)
    val jobDescription = text("job_description")
    val status = varchar("status", 20).default("IN_PROGRESS") // IN_PROGRESS, COMPLETED
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

// Interview Questions Table
object InterviewQuestionsTable : Table("interview_questions") {
    val id = varchar("id", 36)
    val sessionId = varchar("session_id", 36).references(InterviewSessionsTable.id)
    val question = text("question")
    val userAnswer = text("user_answer").nullable()
    val aiFeedback = text("ai_feedback").nullable()
    val questionOrder = integer("question_order")
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Settings Table
object SettingsTable : Table("settings") {
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val key = varchar("key", 100)
    val value = text("value")
    
    override val primaryKey = PrimaryKey(userId, key)
}
