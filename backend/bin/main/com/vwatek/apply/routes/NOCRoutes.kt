package com.vwatek.apply.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.vwatek.apply.db.tables.*
import java.util.UUID

/**
 * NOC (National Occupational Classification) API Routes
 * 
 * Provides endpoints for searching, browsing, and analyzing NOC codes
 * for Canadian job market integration.
 */
fun Route.nocRoutes() {
    route("/noc") {
        
        // Search NOC codes
        get("/search") {
            val query = call.request.queryParameters["q"] ?: ""
            val teerLevels = call.request.queryParameters["teer"]?.split(",")?.mapNotNull { it.toIntOrNull() }
            val category = call.request.queryParameters["category"]
            val province = call.request.queryParameters["province"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            
            val result = transaction {
                var baseQuery = NOCCodesTable.selectAll()
                    .where { NOCCodesTable.isActive eq true }
                
                if (query.isNotBlank()) {
                    baseQuery = baseQuery.andWhere {
                        (NOCCodesTable.code like "%$query%") or
                        (NOCCodesTable.titleEn.lowerCase() like "%${query.lowercase()}%") or
                        (NOCCodesTable.titleFr.lowerCase() like "%${query.lowercase()}%") or
                        (NOCCodesTable.descriptionEn.lowerCase() like "%${query.lowercase()}%")
                    }
                }
                
                if (!teerLevels.isNullOrEmpty()) {
                    baseQuery = baseQuery.andWhere { NOCCodesTable.teerLevel inList teerLevels }
                }
                
                if (!category.isNullOrBlank()) {
                    baseQuery = baseQuery.andWhere { NOCCodesTable.category eq category }
                }
                
                val totalCount = baseQuery.count().toInt()
                val codes = baseQuery
                    .limit(pageSize)
                    .offset(((page - 1) * pageSize).toLong())
                    .orderBy(NOCCodesTable.code)
                    .map { row ->
                        NOCCodeResponse(
                            code = row[NOCCodesTable.code],
                            titleEn = row[NOCCodesTable.titleEn],
                            titleFr = row[NOCCodesTable.titleFr],
                            teerLevel = row[NOCCodesTable.teerLevel],
                            category = row[NOCCodesTable.category],
                            majorGroup = row[NOCCodesTable.majorGroup],
                            descriptionEn = row[NOCCodesTable.descriptionEn].take(200) + "..."
                        )
                    }
                
                NOCSearchResponse(
                    codes = codes,
                    totalCount = totalCount,
                    page = page,
                    pageSize = pageSize,
                    hasMore = (page * pageSize) < totalCount
                )
            }
            
            call.respond(result)
        }
        
        // Get NOC code details
        get("/{code}") {
            val code = call.parameters["code"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing NOC code")
            )
            
            val details = transaction {
                val nocRow = NOCCodesTable.selectAll()
                    .where { NOCCodesTable.code eq code }
                    .singleOrNull()
                    ?: return@transaction null
                
                val duties = NOCMainDutiesTable.selectAll()
                    .where { NOCMainDutiesTable.nocCode eq code }
                    .orderBy(NOCMainDutiesTable.orderIndex)
                    .map { row ->
                        NOCDutyResponse(
                            id = row[NOCMainDutiesTable.id],
                            dutyEn = row[NOCMainDutiesTable.dutyEn],
                            dutyFr = row[NOCMainDutiesTable.dutyFr]
                        )
                    }
                
                val requirements = NOCEmploymentRequirementsTable.selectAll()
                    .where { NOCEmploymentRequirementsTable.nocCode eq code }
                    .orderBy(NOCEmploymentRequirementsTable.orderIndex)
                    .map { row ->
                        NOCRequirementResponse(
                            id = row[NOCEmploymentRequirementsTable.id],
                            requirementEn = row[NOCEmploymentRequirementsTable.requirementEn],
                            requirementFr = row[NOCEmploymentRequirementsTable.requirementFr]
                        )
                    }
                
                val additionalInfo = NOCAdditionalInfoTable.selectAll()
                    .where { NOCAdditionalInfoTable.nocCode eq code }
                    .singleOrNull()?.let { row ->
                        NOCAdditionalInfoResponse(
                            exampleTitlesEn = row[NOCAdditionalInfoTable.exampleTitlesEn],
                            exampleTitlesFr = row[NOCAdditionalInfoTable.exampleTitlesFr],
                            classifiedElsewhereEn = row[NOCAdditionalInfoTable.classifiedElsewhereEn],
                            exclusionsEn = row[NOCAdditionalInfoTable.exclusionsEn]
                        )
                    }
                
                val skills = NOCSkillsTable.selectAll()
                    .where { NOCSkillsTable.nocCode eq code }
                    .orderBy(NOCSkillsTable.skillLevel to SortOrder.DESC)
                    .map { row ->
                        NOCSkillResponse(
                            id = row[NOCSkillsTable.id],
                            skillNameEn = row[NOCSkillsTable.skillNameEn],
                            skillNameFr = row[NOCSkillsTable.skillNameFr],
                            skillLevel = row[NOCSkillsTable.skillLevel],
                            skillType = row[NOCSkillsTable.skillType]
                        )
                    }
                
                val provincialDemand = NOCProvincialDemandTable.selectAll()
                    .where { NOCProvincialDemandTable.nocCode eq code }
                    .orderBy(NOCProvincialDemandTable.provinceCode)
                    .map { row ->
                        NOCProvincialDemandResponse(
                            provinceCode = row[NOCProvincialDemandTable.provinceCode],
                            demandLevel = row[NOCProvincialDemandTable.demandLevel],
                            medianSalary = row[NOCProvincialDemandTable.medianSalary]?.toDouble(),
                            salaryLow = row[NOCProvincialDemandTable.salaryLow]?.toDouble(),
                            salaryHigh = row[NOCProvincialDemandTable.salaryHigh]?.toDouble(),
                            jobOpenings = row[NOCProvincialDemandTable.jobOpenings],
                            outlookYear = row[NOCProvincialDemandTable.outlookYear]
                        )
                    }
                
                val immigrationPathways = NOCImmigrationPathwaysTable.selectAll()
                    .where { NOCImmigrationPathwaysTable.nocCode eq code }
                    .map { row ->
                        NOCImmigrationPathwayResponse(
                            pathwayName = row[NOCImmigrationPathwaysTable.pathwayName],
                            pathwayType = row[NOCImmigrationPathwaysTable.pathwayType],
                            provinceCode = row[NOCImmigrationPathwaysTable.provinceCode],
                            isEligible = row[NOCImmigrationPathwaysTable.isEligible],
                            eligibilityNotes = row[NOCImmigrationPathwaysTable.eligibilityNotes]
                        )
                    }
                
                NOCDetailsResponse(
                    code = nocRow[NOCCodesTable.code],
                    titleEn = nocRow[NOCCodesTable.titleEn],
                    titleFr = nocRow[NOCCodesTable.titleFr],
                    teerLevel = nocRow[NOCCodesTable.teerLevel],
                    category = nocRow[NOCCodesTable.category],
                    majorGroup = nocRow[NOCCodesTable.majorGroup],
                    subMajorGroup = nocRow[NOCCodesTable.subMajorGroup],
                    minorGroup = nocRow[NOCCodesTable.minorGroup],
                    unitGroup = nocRow[NOCCodesTable.unitGroup],
                    descriptionEn = nocRow[NOCCodesTable.descriptionEn],
                    descriptionFr = nocRow[NOCCodesTable.descriptionFr],
                    leadStatementEn = nocRow[NOCCodesTable.leadStatementEn],
                    leadStatementFr = nocRow[NOCCodesTable.leadStatementFr],
                    mainDuties = duties,
                    employmentRequirements = requirements,
                    additionalInfo = additionalInfo,
                    skills = skills,
                    provincialDemand = provincialDemand,
                    immigrationPathways = immigrationPathways
                )
            }
            
            if (details == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("NOC code not found: $code"))
            } else {
                call.respond(details)
            }
        }
        
        // Get TEER level overview
        get("/teer") {
            val teerLevels = listOf(
                TEERLevelResponse(0, "Management", "Gestion", "University degree or significant management experience"),
                TEERLevelResponse(1, "Professional", "Professionnel", "University degree (bachelor's, master's, or doctorate)"),
                TEERLevelResponse(2, "Technical", "Technique", "College diploma, apprenticeship (2+ years), or specialized training"),
                TEERLevelResponse(3, "Skilled Trades", "Métiers spécialisés", "College diploma, apprenticeship (<2 years), or specific training"),
                TEERLevelResponse(4, "Intermediate", "Intermédiaire", "High school diploma and on-the-job training"),
                TEERLevelResponse(5, "Labour", "Manoeuvre", "Short work demonstration, no formal education")
            )
            call.respond(TEEROverviewResponse(teerLevels))
        }
        
        // Get categories (broad occupational categories)
        get("/categories") {
            val categories = transaction {
                NOCCodesTable.select(NOCCodesTable.category, NOCCodesTable.majorGroup, NOCCodesTable.titleEn)
                    .where { NOCCodesTable.isActive eq true }
                    .groupBy(NOCCodesTable.category, NOCCodesTable.majorGroup, NOCCodesTable.titleEn)
                    .orderBy(NOCCodesTable.category)
                    .map { row ->
                        NOCCategoryResponse(
                            category = row[NOCCodesTable.category],
                            majorGroup = row[NOCCodesTable.majorGroup],
                            name = row[NOCCodesTable.titleEn]
                        )
                    }
                    .distinctBy { it.category }
            }
            call.respond(NOCCategoriesResponse(categories))
        }
        
        // Get provincial demand for a NOC code
        get("/{code}/demand") {
            val code = call.parameters["code"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing NOC code")
            )
            
            val demand = transaction {
                NOCProvincialDemandTable.selectAll()
                    .where { NOCProvincialDemandTable.nocCode eq code }
                    .orderBy(NOCProvincialDemandTable.provinceCode)
                    .map { row ->
                        NOCProvincialDemandResponse(
                            provinceCode = row[NOCProvincialDemandTable.provinceCode],
                            demandLevel = row[NOCProvincialDemandTable.demandLevel],
                            medianSalary = row[NOCProvincialDemandTable.medianSalary]?.toDouble(),
                            salaryLow = row[NOCProvincialDemandTable.salaryLow]?.toDouble(),
                            salaryHigh = row[NOCProvincialDemandTable.salaryHigh]?.toDouble(),
                            jobOpenings = row[NOCProvincialDemandTable.jobOpenings],
                            outlookYear = row[NOCProvincialDemandTable.outlookYear]
                        )
                    }
            }
            
            call.respond(NOCDemandListResponse(code, demand))
        }
        
        // Get immigration pathways for a NOC code
        get("/{code}/immigration") {
            val code = call.parameters["code"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing NOC code")
            )
            
            val pathways = transaction {
                NOCImmigrationPathwaysTable.selectAll()
                    .where { NOCImmigrationPathwaysTable.nocCode eq code }
                    .map { row ->
                        NOCImmigrationPathwayResponse(
                            pathwayName = row[NOCImmigrationPathwaysTable.pathwayName],
                            pathwayType = row[NOCImmigrationPathwaysTable.pathwayType],
                            provinceCode = row[NOCImmigrationPathwaysTable.provinceCode],
                            isEligible = row[NOCImmigrationPathwaysTable.isEligible],
                            eligibilityNotes = row[NOCImmigrationPathwaysTable.eligibilityNotes]
                        )
                    }
            }
            
            call.respond(NOCImmigrationListResponse(code, pathways))
        }
        
        // Save user's NOC match
        post("/users/{userId}/matches") {
            val userId = call.parameters["userId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing user ID")
            )
            
            val request = call.receive<CreateNOCMatchRequest>()
            
            val matchId = transaction {
                val id = UUID.randomUUID().toString()
                UserNOCMatchesTable.insert {
                    it[UserNOCMatchesTable.id] = id
                    it[UserNOCMatchesTable.userId] = userId
                    it[resumeId] = request.resumeId
                    it[nocCode] = request.nocCode
                    it[matchScore] = request.matchScore
                    it[teerLevelFit] = request.teerLevelFit
                    it[matchedDuties] = request.matchedDuties.joinToString(",")
                    it[missingSkills] = request.missingSkills.joinToString(",")
                    it[recommendations] = request.recommendations.joinToString("|")
                    it[createdAt] = java.time.Instant.now().toString()
                }
                id
            }
            
            call.respond(HttpStatusCode.Created, CreateNOCMatchResponse(matchId))
        }
        
        // Get user's NOC matches
        get("/users/{userId}/matches") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing user ID")
            )
            
            val matches = transaction {
                UserNOCMatchesTable.selectAll()
                    .where { UserNOCMatchesTable.userId eq userId }
                    .orderBy(UserNOCMatchesTable.createdAt to SortOrder.DESC)
                    .map { row ->
                        UserNOCMatchResponse(
                            id = row[UserNOCMatchesTable.id],
                            resumeId = row[UserNOCMatchesTable.resumeId],
                            nocCode = row[UserNOCMatchesTable.nocCode],
                            matchScore = row[UserNOCMatchesTable.matchScore],
                            teerLevelFit = row[UserNOCMatchesTable.teerLevelFit],
                            matchedDuties = row[UserNOCMatchesTable.matchedDuties].split(",").filter { it.isNotBlank() },
                            missingSkills = row[UserNOCMatchesTable.missingSkills].split(",").filter { it.isNotBlank() },
                            recommendations = row[UserNOCMatchesTable.recommendations].split("|").filter { it.isNotBlank() },
                            createdAt = row[UserNOCMatchesTable.createdAt]
                        )
                    }
            }
            
            call.respond(UserNOCMatchListResponse(matches))
        }
    }
}

// Response DTOs
@Serializable
data class NOCCodeResponse(
    val code: String,
    val titleEn: String,
    val titleFr: String,
    val teerLevel: Int,
    val category: String,
    val majorGroup: String,
    val descriptionEn: String
)

@Serializable
data class NOCSearchResponse(
    val codes: List<NOCCodeResponse>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

@Serializable
data class NOCDetailsResponse(
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
    val leadStatementEn: String?,
    val leadStatementFr: String?,
    val mainDuties: List<NOCDutyResponse>,
    val employmentRequirements: List<NOCRequirementResponse>,
    val additionalInfo: NOCAdditionalInfoResponse?,
    val skills: List<NOCSkillResponse>,
    val provincialDemand: List<NOCProvincialDemandResponse>,
    val immigrationPathways: List<NOCImmigrationPathwayResponse>
)

@Serializable
data class NOCDutyResponse(
    val id: String,
    val dutyEn: String,
    val dutyFr: String
)

@Serializable
data class NOCRequirementResponse(
    val id: String,
    val requirementEn: String,
    val requirementFr: String
)

@Serializable
data class NOCAdditionalInfoResponse(
    val exampleTitlesEn: String,
    val exampleTitlesFr: String,
    val classifiedElsewhereEn: String?,
    val exclusionsEn: String?
)

@Serializable
data class NOCSkillResponse(
    val id: String,
    val skillNameEn: String,
    val skillNameFr: String,
    val skillLevel: Int,
    val skillType: String
)

@Serializable
data class NOCProvincialDemandResponse(
    val provinceCode: String,
    val demandLevel: String,
    val medianSalary: Double?,
    val salaryLow: Double?,
    val salaryHigh: Double?,
    val jobOpenings: Int?,
    val outlookYear: Int
)

@Serializable
data class NOCImmigrationPathwayResponse(
    val pathwayName: String,
    val pathwayType: String,
    val provinceCode: String?,
    val isEligible: Boolean,
    val eligibilityNotes: String?
)

@Serializable
data class TEERLevelResponse(
    val level: Int,
    val titleEn: String,
    val titleFr: String,
    val educationRequirement: String
)

@Serializable
data class TEEROverviewResponse(
    val levels: List<TEERLevelResponse>
)

@Serializable
data class NOCCategoryResponse(
    val category: String,
    val majorGroup: String,
    val name: String
)

@Serializable
data class NOCCategoriesResponse(
    val categories: List<NOCCategoryResponse>
)

@Serializable
data class NOCDemandListResponse(
    val nocCode: String,
    val demand: List<NOCProvincialDemandResponse>
)

@Serializable
data class NOCImmigrationListResponse(
    val nocCode: String,
    val pathways: List<NOCImmigrationPathwayResponse>
)

@Serializable
data class CreateNOCMatchRequest(
    val resumeId: String?,
    val nocCode: String,
    val matchScore: Int,
    val teerLevelFit: String,
    val matchedDuties: List<String>,
    val missingSkills: List<String>,
    val recommendations: List<String>
)

@Serializable
data class CreateNOCMatchResponse(
    val id: String
)

@Serializable
data class UserNOCMatchResponse(
    val id: String,
    val resumeId: String?,
    val nocCode: String,
    val matchScore: Int,
    val teerLevelFit: String,
    val matchedDuties: List<String>,
    val missingSkills: List<String>,
    val recommendations: List<String>,
    val createdAt: String
)

@Serializable
data class UserNOCMatchListResponse(
    val matches: List<UserNOCMatchResponse>
)

@Serializable
data class ErrorResponse(val message: String)
