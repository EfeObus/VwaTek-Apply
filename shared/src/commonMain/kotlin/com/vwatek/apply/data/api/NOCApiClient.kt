package com.vwatek.apply.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import com.vwatek.apply.domain.model.*

/**
 * NOC API Client for interacting with the NOC backend endpoints
 */
class NOCApiClient(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig = ApiConfig
) {
    private val baseUrl get() = apiConfig.baseUrl
    
    /**
     * Search NOC codes
     */
    suspend fun searchNOCCodes(
        query: String = "",
        teerLevels: List<Int>? = null,
        category: String? = null,
        province: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<NOCSearchResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/search") {
            parameter("q", query)
            teerLevels?.let { parameter("teer", it.joinToString(",")) }
            category?.let { parameter("category", it) }
            province?.let { parameter("province", it) }
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()
    }
    
    /**
     * Get NOC code details
     */
    suspend fun getNOCDetails(code: String): Result<NOCDetailsResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/$code").body()
    }
    
    /**
     * Get TEER levels overview
     */
    suspend fun getTEERLevels(): Result<TEEROverviewResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/teer").body()
    }
    
    /**
     * Get NOC categories
     */
    suspend fun getCategories(): Result<NOCCategoriesResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/categories").body()
    }
    
    /**
     * Get provincial demand for a NOC code
     */
    suspend fun getProvincialDemand(code: String): Result<NOCDemandListResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/$code/demand").body()
    }
    
    /**
     * Get immigration pathways for a NOC code
     */
    suspend fun getImmigrationPathways(code: String): Result<NOCImmigrationListResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/$code/immigration").body()
    }
    
    /**
     * Save user's NOC match
     */
    suspend fun saveNOCMatch(
        userId: String,
        request: CreateNOCMatchRequest
    ): Result<CreateNOCMatchResponse> = runCatching {
        httpClient.post("$baseUrl/api/v1/noc/users/$userId/matches") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    /**
     * Get user's NOC matches
     */
    suspend fun getUserNOCMatches(userId: String): Result<UserNOCMatchListResponse> = runCatching {
        httpClient.get("$baseUrl/api/v1/noc/users/$userId/matches").body()
    }
}

// Response DTOs (matching backend responses)
@Serializable
data class NOCCodeDto(
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
    val codes: List<NOCCodeDto>,
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
    val mainDuties: List<NOCDutyDto>,
    val employmentRequirements: List<NOCRequirementDto>,
    val additionalInfo: NOCAdditionalInfoDto?,
    val skills: List<NOCSkillDto>,
    val provincialDemand: List<NOCProvincialDemandDto>,
    val immigrationPathways: List<NOCImmigrationPathwayDto>
)

@Serializable
data class NOCDutyDto(
    val id: String,
    val dutyEn: String,
    val dutyFr: String
)

@Serializable
data class NOCRequirementDto(
    val id: String,
    val requirementEn: String,
    val requirementFr: String
)

@Serializable
data class NOCAdditionalInfoDto(
    val exampleTitlesEn: String,
    val exampleTitlesFr: String,
    val classifiedElsewhereEn: String?,
    val exclusionsEn: String?
)

@Serializable
data class NOCSkillDto(
    val id: String,
    val skillNameEn: String,
    val skillNameFr: String,
    val skillLevel: Int,
    val skillType: String
)

@Serializable
data class NOCProvincialDemandDto(
    val provinceCode: String,
    val demandLevel: String,
    val medianSalary: Double?,
    val salaryLow: Double?,
    val salaryHigh: Double?,
    val jobOpenings: Int?,
    val outlookYear: Int
)

@Serializable
data class NOCImmigrationPathwayDto(
    val pathwayName: String,
    val pathwayType: String,
    val provinceCode: String?,
    val isEligible: Boolean,
    val eligibilityNotes: String?
)

@Serializable
data class TEERLevelDto(
    val level: Int,
    val titleEn: String,
    val titleFr: String,
    val educationRequirement: String
)

@Serializable
data class TEEROverviewResponse(
    val levels: List<TEERLevelDto>
)

@Serializable
data class NOCCategoryDto(
    val category: String,
    val majorGroup: String,
    val name: String
)

@Serializable
data class NOCCategoriesResponse(
    val categories: List<NOCCategoryDto>
)

@Serializable
data class NOCDemandListResponse(
    val nocCode: String,
    val demand: List<NOCProvincialDemandDto>
)

@Serializable
data class NOCImmigrationListResponse(
    val nocCode: String,
    val pathways: List<NOCImmigrationPathwayDto>
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
data class UserNOCMatchDto(
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
    val matches: List<UserNOCMatchDto>
)

// Extensions to convert DTOs to domain models
fun NOCCodeDto.toDomainModel() = NOCCode(
    code = code,
    titleEn = titleEn,
    titleFr = titleFr,
    teerLevel = teerLevel,
    category = category,
    majorGroup = majorGroup,
    subMajorGroup = "",
    minorGroup = "",
    unitGroup = "",
    descriptionEn = descriptionEn,
    descriptionFr = ""
)

fun NOCDetailsResponse.toDomainModel() = NOCDetails(
    noc = NOCCode(
        code = code,
        titleEn = titleEn,
        titleFr = titleFr,
        teerLevel = teerLevel,
        category = category,
        majorGroup = majorGroup,
        subMajorGroup = subMajorGroup,
        minorGroup = minorGroup,
        unitGroup = unitGroup,
        descriptionEn = descriptionEn,
        descriptionFr = descriptionFr,
        leadStatementEn = leadStatementEn,
        leadStatementFr = leadStatementFr
    ),
    mainDuties = mainDuties.mapIndexed { index, it ->
        NOCMainDuty(id = it.id, nocCode = code, dutyEn = it.dutyEn, dutyFr = it.dutyFr, orderIndex = index)
    },
    employmentRequirements = employmentRequirements.mapIndexed { index, it ->
        NOCEmploymentRequirement(id = it.id, nocCode = code, requirementEn = it.requirementEn, requirementFr = it.requirementFr, orderIndex = index)
    },
    additionalInfo = additionalInfo?.let {
        NOCAdditionalInfo(
            nocCode = code,
            exampleTitlesEn = it.exampleTitlesEn.split(",").map { s -> s.trim() },
            exampleTitlesFr = it.exampleTitlesFr.split(",").map { s -> s.trim() },
            classifiedElsewhereEn = it.classifiedElsewhereEn?.split(",")?.map { s -> s.trim() },
            exclusionsEn = it.exclusionsEn
        )
    },
    skills = skills.map {
        NOCSkill(
            id = it.id,
            nocCode = code,
            skillNameEn = it.skillNameEn,
            skillNameFr = it.skillNameFr,
            skillLevel = it.skillLevel,
            skillType = SkillType.valueOf(it.skillType.uppercase())
        )
    },
    provincialDemand = provincialDemand.map {
        NOCProvincialDemand(
            id = "",
            nocCode = code,
            provinceCode = it.provinceCode,
            demandLevel = DemandLevel.valueOf(it.demandLevel.uppercase()),
            medianSalary = it.medianSalary,
            salaryLow = it.salaryLow,
            salaryHigh = it.salaryHigh,
            jobOpenings = it.jobOpenings,
            outlookYear = it.outlookYear,
            dataSource = "Job Bank Canada"
        )
    },
    immigrationPathways = immigrationPathways.map {
        NOCImmigrationPathway(
            id = "",
            nocCode = code,
            pathwayName = it.pathwayName,
            pathwayType = PathwayType.valueOf(it.pathwayType.uppercase()),
            provinceCode = it.provinceCode,
            isEligible = it.isEligible,
            eligibilityNotes = it.eligibilityNotes
        )
    }
)
