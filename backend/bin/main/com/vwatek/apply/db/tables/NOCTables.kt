package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.Table

/**
 * NOC 2021 Classification Database Tables
 * 
 * Based on the National Occupational Classification (NOC) 2021 Version 1.0
 * which uses the TEER (Training, Education, Experience and Responsibilities) system
 * 
 * TEER Levels:
 * - 0: Management occupations
 * - 1: Occupations usually requiring a university degree
 * - 2: Occupations usually requiring college diploma, apprenticeship, or supervisor/specialized training
 * - 3: Occupations usually requiring college diploma, apprenticeship, or specific training
 * - 4: Occupations usually requiring high school diploma and on-the-job training
 * - 5: Occupations usually requiring short-term work demonstration and no formal education
 */

// Main NOC Codes Table
object NOCCodesTable : Table("noc_codes") {
    val code = varchar("code", 10) // e.g., "21231" for Software Developers
    val titleEn = varchar("title_en", 255)
    val titleFr = varchar("title_fr", 255)
    val teerLevel = integer("teer_level") // 0-5
    val category = varchar("category", 10) // Broad occupational category (0-9)
    val majorGroup = varchar("major_group", 10) // 2-digit
    val subMajorGroup = varchar("sub_major_group", 10) // 3-digit
    val minorGroup = varchar("minor_group", 10) // 4-digit
    val unitGroup = varchar("unit_group", 10) // 5-digit (full NOC code)
    val descriptionEn = text("description_en")
    val descriptionFr = text("description_fr")
    val leadStatementEn = text("lead_statement_en").nullable()
    val leadStatementFr = text("lead_statement_fr").nullable()
    val isActive = bool("is_active").default(true)
    
    override val primaryKey = PrimaryKey(code)
    
    init {
        index(false, teerLevel)
        index(false, category)
        index(false, majorGroup)
    }
}

// Main duties associated with each NOC code
object NOCMainDutiesTable : Table("noc_main_duties") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val dutyEn = text("duty_en")
    val dutyFr = text("duty_fr")
    val orderIndex = integer("order_index")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, nocCode)
    }
}

// Employment requirements for each NOC code
object NOCEmploymentRequirementsTable : Table("noc_employment_requirements") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val requirementEn = text("requirement_en")
    val requirementFr = text("requirement_fr")
    val orderIndex = integer("order_index")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, nocCode)
    }
}

// Additional info: example titles, exclusions, etc.
object NOCAdditionalInfoTable : Table("noc_additional_info") {
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val exampleTitlesEn = text("example_titles_en") // JSON array
    val exampleTitlesFr = text("example_titles_fr")
    val classifiedElsewhereEn = text("classified_elsewhere_en").nullable() // JSON array
    val classifiedElsewhereFr = text("classified_elsewhere_fr").nullable()
    val exclusionsEn = text("exclusions_en").nullable()
    val exclusionsFr = text("exclusions_fr").nullable()
    
    override val primaryKey = PrimaryKey(nocCode)
}

// Skills and competencies (mapped from O*NET or Canadian Skills Framework)
object NOCSkillsTable : Table("noc_skills") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val skillNameEn = varchar("skill_name_en", 255)
    val skillNameFr = varchar("skill_name_fr", 255)
    val skillLevel = integer("skill_level") // 1-5 importance
    val skillType = varchar("skill_type", 50) // TECHNICAL, SOFT, LANGUAGE, etc.
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, nocCode)
        index(false, skillType)
    }
}

// Provincial demand data for NOC codes
object NOCProvincialDemandTable : Table("noc_provincial_demand") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val provinceCode = varchar("province_code", 2) // ON, BC, AB, QC, etc.
    val demandLevel = varchar("demand_level", 20) // HIGH, MEDIUM, LOW
    val medianSalary = decimal("median_salary", 10, 2).nullable()
    val salaryLow = decimal("salary_low", 10, 2).nullable()
    val salaryHigh = decimal("salary_high", 10, 2).nullable()
    val jobOpenings = integer("job_openings").nullable()
    val outlookYear = integer("outlook_year") // e.g., 2024
    val dataSource = varchar("data_source", 100) // "Job Bank Canada", "StatsCan"
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex(nocCode, provinceCode, outlookYear)
        index(false, provinceCode)
        index(false, demandLevel)
    }
}

// Immigration pathway eligibility for NOC codes
object NOCImmigrationPathwaysTable : Table("noc_immigration_pathways") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val pathwayName = varchar("pathway_name", 100) // "Express Entry", "PNP Ontario", etc.
    val pathwayType = varchar("pathway_type", 50) // FEDERAL, PROVINCIAL, TERRITORIAL
    val provinceCode = varchar("province_code", 2).nullable() // For PNPs
    val isEligible = bool("is_eligible").default(true)
    val eligibilityNotes = text("eligibility_notes").nullable()
    val lastUpdated = varchar("last_updated", 10) // Date string
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, nocCode)
        index(false, pathwayType)
    }
}

// User's NOC match history
object UserNOCMatchesTable : Table("user_noc_matches") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val resumeId = varchar("resume_id", 36).references(ResumesTable.id).nullable()
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val matchScore = integer("match_score") // 0-100
    val teerLevelFit = varchar("teer_level_fit", 20) // EXCEEDS, MEETS, BELOW
    val matchedDuties = text("matched_duties") // JSON array
    val missingSkills = text("missing_skills") // JSON array
    val recommendations = text("recommendations") // JSON array
    val createdAt = varchar("created_at", 30)
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, resumeId)
    }
}
