package com.vwatek.apply.domain.model

import kotlinx.serialization.Serializable

/**
 * NOC (National Occupational Classification) Domain Models
 * 
 * Based on NOC 2021 Version 1.0 with TEER classification system
 */

@Serializable
data class NOCCode(
    val code: String,
    val titleEn: String,
    val titleFr: String,
    val teerLevel: Int,
    val category: String,
    val majorGroup: String,
    val subMajorGroup: String,
    val minorGroup: String,
    val unitGroup: String,
    val descriptionEn: String,
    val descriptionFr: String,
    val leadStatementEn: String? = null,
    val leadStatementFr: String? = null
) {
    val title: String get() = titleEn // Default to English, use LocaleManager for proper localization
    val description: String get() = descriptionEn
}

@Serializable
data class NOCMainDuty(
    val id: String,
    val nocCode: String,
    val dutyEn: String,
    val dutyFr: String,
    val orderIndex: Int
) {
    val duty: String get() = dutyEn
}

@Serializable
data class NOCEmploymentRequirement(
    val id: String,
    val nocCode: String,
    val requirementEn: String,
    val requirementFr: String,
    val orderIndex: Int
) {
    val requirement: String get() = requirementEn
}

@Serializable
data class NOCAdditionalInfo(
    val nocCode: String,
    val exampleTitlesEn: List<String>,
    val exampleTitlesFr: List<String>,
    val classifiedElsewhereEn: List<String>? = null,
    val classifiedElsewhereFr: List<String>? = null,
    val exclusionsEn: String? = null,
    val exclusionsFr: String? = null
) {
    val exampleTitles: List<String> get() = exampleTitlesEn
}

@Serializable
data class NOCSkill(
    val id: String,
    val nocCode: String,
    val skillNameEn: String,
    val skillNameFr: String,
    val skillLevel: Int,
    val skillType: SkillType
) {
    val skillName: String get() = skillNameEn
}

@Serializable
enum class SkillType {
    TECHNICAL,
    SOFT,
    LANGUAGE,
    COGNITIVE,
    PHYSICAL,
    OTHER
}

@Serializable
data class NOCProvincialDemand(
    val id: String,
    val nocCode: String,
    val provinceCode: String,
    val demandLevel: DemandLevel,
    val medianSalary: Double? = null,
    val salaryLow: Double? = null,
    val salaryHigh: Double? = null,
    val jobOpenings: Int? = null,
    val outlookYear: Int,
    val dataSource: String
)

@Serializable
enum class DemandLevel {
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

@Serializable
data class NOCImmigrationPathway(
    val id: String,
    val nocCode: String,
    val pathwayName: String,
    val pathwayType: PathwayType,
    val provinceCode: String? = null,
    val isEligible: Boolean = true,
    val eligibilityNotes: String? = null
)

@Serializable
enum class PathwayType {
    FEDERAL,
    PROVINCIAL,
    TERRITORIAL
}

/**
 * TEER Level Descriptions
 * 
 * TEER = Training, Education, Experience and Responsibilities
 */
@Serializable
enum class TEERLevel(
    val level: Int,
    val titleEn: String,
    val titleFr: String,
    val educationRequirementEn: String,
    val educationRequirementFr: String
) {
    TEER_0(
        0,
        "Management Occupations",
        "Postes de gestion",
        "University degree or significant management experience",
        "Diplôme universitaire ou expérience de gestion significative"
    ),
    TEER_1(
        1,
        "Professional Occupations",
        "Professions",
        "University degree (bachelor's, master's, or doctorate)",
        "Diplôme universitaire (baccalauréat, maîtrise ou doctorat)"
    ),
    TEER_2(
        2,
        "Technical Occupations",
        "Postes techniques",
        "College diploma, apprenticeship (2+ years), or supervisory/specialized training",
        "Diplôme collégial, apprentissage (2+ ans) ou formation spécialisée/de supervision"
    ),
    TEER_3(
        3,
        "Skilled Trades",
        "Métiers spécialisés",
        "College diploma, apprenticeship (less than 2 years), or specific on-the-job training",
        "Diplôme collégial, apprentissage (moins de 2 ans) ou formation en cours d'emploi spécifique"
    ),
    TEER_4(
        4,
        "Intermediate Occupations",
        "Professions intermédiaires",
        "High school diploma and several weeks of on-the-job training",
        "Diplôme d'études secondaires et plusieurs semaines de formation en cours d'emploi"
    ),
    TEER_5(
        5,
        "Labour Occupations",
        "Postes de manœuvre",
        "Short work demonstration and no formal education requirement",
        "Courte démonstration de travail et aucune exigence d'éducation formelle"
    );
    
    companion object {
        fun fromLevel(level: Int): TEERLevel? = entries.find { it.level == level }
    }
}

/**
 * Complete NOC Details with all related data
 */
@Serializable
data class NOCDetails(
    val noc: NOCCode,
    val mainDuties: List<NOCMainDuty>,
    val employmentRequirements: List<NOCEmploymentRequirement>,
    val additionalInfo: NOCAdditionalInfo?,
    val skills: List<NOCSkill>,
    val provincialDemand: List<NOCProvincialDemand>,
    val immigrationPathways: List<NOCImmigrationPathway>
) {
    val teerLevel: TEERLevel? get() = TEERLevel.fromLevel(noc.teerLevel)
}

/**
 * NOC Match Result from AI analysis
 */
@Serializable
data class NOCMatchResult(
    val topMatch: NOCMatch?,
    val alternatives: List<NOCMatch>,
    val extractedJobInfo: ExtractedJobInfo
)

@Serializable
data class NOCMatch(
    val nocCode: String,
    val nocTitle: String,
    val teerLevel: Int,
    val confidenceScore: Float // 0.0 - 1.0
)

@Serializable
data class ExtractedJobInfo(
    val jobTitle: String,
    val skills: List<String>,
    val duties: List<String>,
    val educationRequirements: List<String>,
    val experienceYears: Int?
)

/**
 * NOC Fit Analysis for resume
 */
@Serializable
data class NOCFitAnalysis(
    val overallFitScore: Int, // 0-100
    val teerLevelFit: TeerFit,
    val dutiesMatch: DutiesMatchResult,
    val requirementsMatch: RequirementsMatchResult,
    val immigrationReadiness: ImmigrationReadiness,
    val resumeImprovements: List<ImprovementSuggestion>
)

@Serializable
enum class TeerFit {
    EXCEEDS,
    MEETS,
    BELOW
}

@Serializable
data class DutiesMatchResult(
    val matched: List<String>,
    val partialMatch: List<String>,
    val missing: List<String>
)

@Serializable
data class RequirementsMatchResult(
    val met: List<String>,
    val partiallyMet: List<String>,
    val notMet: List<String>
)

@Serializable
data class ImmigrationReadiness(
    val expressEntryPoints: Int, // Estimated CRS points for work experience
    val provincialNomineeEligibility: List<String>, // Province codes
    val recommendations: List<String>
)

@Serializable
data class ImprovementSuggestion(
    val section: String,
    val suggestion: String,
    val example: String?
)

/**
 * User's saved NOC match
 */
@Serializable
data class UserNOCMatch(
    val id: String,
    val userId: String,
    val resumeId: String?,
    val nocCode: String,
    val matchScore: Int,
    val teerLevelFit: TeerFit,
    val matchedDuties: List<String>,
    val missingSkills: List<String>,
    val recommendations: List<String>,
    val createdAt: String
)

/**
 * Search filters for NOC codes
 */
@Serializable
data class NOCSearchFilters(
    val query: String? = null,
    val teerLevels: List<Int>? = null,
    val categories: List<String>? = null,
    val provinces: List<String>? = null,
    val demandLevels: List<DemandLevel>? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val hasImmigrationPathway: Boolean? = null
)

/**
 * NOC Search Result
 */
@Serializable
data class NOCSearchResult(
    val codes: List<NOCCode>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)
