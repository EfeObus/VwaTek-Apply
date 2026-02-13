package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Salary Intelligence Tables for VwaTek Apply
 * Stores salary data, comparisons, and negotiation sessions
 */

// Salary Data Table (from Statistics Canada, Job Bank, etc.)
object SalaryDataTable : Table("salary_data") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10)
    val jobTitle = varchar("job_title", 255)
    val province = varchar("province", 50)
    val city = varchar("city", 100).nullable()
    val medianSalary = decimal("median_salary", 12, 2)
    val lowSalary = decimal("low_salary", 12, 2)       // 10th percentile
    val highSalary = decimal("high_salary", 12, 2)     // 90th percentile
    val averageSalary = decimal("average_salary", 12, 2)
    val sampleSize = integer("sample_size")
    val dataSource = varchar("data_source", 30)        // STATISTICS_CANADA, JOB_BANK_CANADA, etc.
    val currency = varchar("currency", 3).default("CAD")
    val yearOfData = integer("year_of_data")
    val quarterOfData = integer("quarter_of_data").nullable()
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_salary_noc", false, nocCode)
        index("idx_salary_province", false, province)
        index("idx_salary_job_title", false, jobTitle)
        index("idx_salary_location", false, province, city)
    }
}

// Salary Comparison History Table
object SalaryComparisonHistoryTable : Table("salary_comparison_history") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val jobTitle = varchar("job_title", 255)
    val nocCode = varchar("noc_code", 10).nullable()
    val province = varchar("province", 50)
    val city = varchar("city", 100).nullable()
    val currentSalary = decimal("current_salary", 12, 2).nullable()
    val yearsExperience = integer("years_experience").nullable()
    val insightsJson = text("insights_json")           // Full SalaryInsights as JSON
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_salary_history_user", false, userId)
    }
}

// Job Offers Table (for offer evaluation)
object JobOffersTable : Table("job_offers") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val jobTitle = varchar("job_title", 255)
    val company = varchar("company", 255)
    val nocCode = varchar("noc_code", 10).nullable()
    val baseSalary = decimal("base_salary", 12, 2)
    val signingBonus = decimal("signing_bonus", 12, 2).nullable()
    val annualBonus = decimal("annual_bonus", 5, 2).nullable()     // Target bonus percentage
    val stockOptionsJson = text("stock_options_json").nullable()   // StockOptions as JSON
    val benefitsJson = text("benefits_json").nullable()            // Benefits as JSON
    val province = varchar("province", 50)
    val city = varchar("city", 100).nullable()
    val isRemote = bool("is_remote").default(false)
    val yearsExperienceRequired = integer("years_experience_required").nullable()
    val evaluationJson = text("evaluation_json").nullable()        // OfferEvaluation as JSON
    val status = varchar("status", 20).default("PENDING")          // PENDING, ACCEPTED, DECLINED, NEGOTIATING
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_job_offers_user", false, userId)
        index("idx_job_offers_status", false, status)
    }
}

// Negotiation Sessions Table
object NegotiationSessionsTable : Table("negotiation_sessions") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val offerId = varchar("offer_id", 36).references(JobOffersTable.id, onDelete = ReferenceOption.CASCADE)
    val status = varchar("status", 20).default("ACTIVE")   // ACTIVE, COMPLETED, ARCHIVED
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_negotiation_user", false, userId)
        index("idx_negotiation_offer", false, offerId)
    }
}

// Negotiation Messages Table
object NegotiationMessagesTable : Table("negotiation_messages") {
    val id = varchar("id", 36)
    val sessionId = varchar("session_id", 36).references(NegotiationSessionsTable.id, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 20)                     // USER, COACH, SYSTEM
    val content = text("content")
    val suggestedResponse = text("suggested_response").nullable()
    val timestamp = timestamp("timestamp")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_negotiation_messages_session", false, sessionId)
    }
}

// Salary Data Import Log Table (for tracking data updates)
object SalaryDataImportLogTable : Table("salary_data_import_log") {
    val id = varchar("id", 36)
    val dataSource = varchar("data_source", 30)
    val recordsImported = integer("records_imported")
    val recordsUpdated = integer("records_updated")
    val recordsFailed = integer("records_failed")
    val importedAt = timestamp("imported_at")
    val importedBy = varchar("imported_by", 100).nullable()  // System or admin user
    val notes = text("notes").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

// User Saved Salary Searches Table (for quick access)
object SavedSalarySearchesTable : Table("saved_salary_searches") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val jobTitle = varchar("job_title", 255)
    val nocCode = varchar("noc_code", 10).nullable()
    val province = varchar("province", 50)
    val city = varchar("city", 100).nullable()
    val notifications = bool("notifications").default(false)  // Notify on salary updates
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_saved_searches_user", false, userId)
    }
}
