package com.vwatek.apply.integrations.jobbank

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Job Bank Canada API Integration
 * 
 * Provides access to Job Bank Canada (jobbank.gc.ca) job listings.
 * Note: Job Bank doesn't have an official public API, so this uses
 * web scraping patterns or their unofficial endpoints. For production,
 * consider reaching out to ESDC for API access.
 */
class JobBankApiClient(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://www.jobbank.gc.ca"
        private const val API_BASE = "$BASE_URL/api/v1"
        private const val USER_AGENT = "VwaTek-Apply/1.0 (Canadian Job Search App)"
    }
    
    /**
     * Search jobs on Job Bank Canada
     */
    suspend fun searchJobs(
        query: String = "",
        location: String? = null,
        nocCode: String? = null,
        province: String? = null,
        city: String? = null,
        distance: Int? = null,
        salaryMin: Int? = null,
        salaryMax: Int? = null,
        hours: JobHours? = null,
        period: JobPeriod? = null,
        page: Int = 1,
        pageSize: Int = 25
    ): Result<JobBankSearchResult> = runCatching {
        val response = httpClient.get("$API_BASE/jobs/search") {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json")
            header("Accept-Language", "en-CA,en;q=0.9,fr-CA;q=0.8")
            
            if (query.isNotBlank()) parameter("searchstring", query)
            location?.let { parameter("location", it) }
            nocCode?.let { parameter("noc", it) }
            province?.let { parameter("prov", it) }
            city?.let { parameter("city", it) }
            distance?.let { parameter("distance", it) }
            salaryMin?.let { parameter("salarymin", it) }
            salaryMax?.let { parameter("salarymax", it) }
            hours?.let { parameter("hours", it.code) }
            period?.let { parameter("period", it.code) }
            parameter("page", page)
            parameter("per_page", pageSize)
        }
        
        response.body<JobBankSearchResult>()
    }
    
    /**
     * Get job details by ID
     */
    suspend fun getJobDetails(jobId: String): Result<JobBankJobDetail> = runCatching {
        val response = httpClient.get("$API_BASE/jobs/$jobId") {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json")
        }
        
        response.body<JobBankJobDetail>()
    }
    
    /**
     * Search jobs by NOC code
     */
    suspend fun searchByNOC(
        nocCode: String,
        province: String? = null,
        page: Int = 1,
        pageSize: Int = 25
    ): Result<JobBankSearchResult> = searchJobs(
        nocCode = nocCode,
        province = province,
        page = page,
        pageSize = pageSize
    )
    
    /**
     * Search jobs by province
     */
    suspend fun searchByProvince(
        province: String,
        query: String = "",
        page: Int = 1,
        pageSize: Int = 25
    ): Result<JobBankSearchResult> = searchJobs(
        query = query,
        province = province,
        page = page,
        pageSize = pageSize
    )
    
    /**
     * Get trending jobs (based on high demand)
     */
    suspend fun getTrendingJobs(
        province: String? = null,
        limit: Int = 20
    ): Result<JobBankSearchResult> = runCatching {
        val response = httpClient.get("$API_BASE/jobs/trending") {
            header("User-Agent", USER_AGENT)
            province?.let { parameter("prov", it) }
            parameter("limit", limit)
        }
        
        response.body<JobBankSearchResult>()
    }
    
    /**
     * Get job outlook for a NOC code
     */
    suspend fun getJobOutlook(
        nocCode: String,
        province: String? = null
    ): Result<JobOutlookResponse> = runCatching {
        val response = httpClient.get("$API_BASE/outlook/$nocCode") {
            header("User-Agent", USER_AGENT)
            province?.let { parameter("prov", it) }
        }
        
        response.body<JobOutlookResponse>()
    }
}

// Job Bank API Response Models
@Serializable
data class JobBankSearchResult(
    val jobs: List<JobBankJob>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

@Serializable
data class JobBankJob(
    val jobId: String,
    val title: String,
    val titleFr: String? = null,
    val employer: String,
    val location: JobBankLocation,
    val salary: JobBankSalary?,
    val nocCode: String?,
    val postingDate: String,
    val expiryDate: String?,
    val jobUrl: String,
    val isLmiaApproved: Boolean = false,
    val workHours: String? = null,
    val employmentType: String? = null // Full-time, Part-time, Contract
)

@Serializable
data class JobBankJobDetail(
    val jobId: String,
    val title: String,
    val titleFr: String? = null,
    val employer: String,
    val location: JobBankLocation,
    val salary: JobBankSalary?,
    val nocCode: String?,
    val nocTitle: String? = null,
    val postingDate: String,
    val expiryDate: String?,
    val jobUrl: String,
    val description: String,
    val descriptionFr: String? = null,
    val requirements: List<String>,
    val qualifications: List<String>,
    val benefits: List<String>?,
    val workHours: String?,
    val employmentType: String?,
    val isLmiaApproved: Boolean = false,
    val vacancies: Int = 1,
    val applicationDeadline: String?,
    val howToApply: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val website: String?
)

@Serializable
data class JobBankLocation(
    val city: String,
    val province: String,
    val provinceCode: String,
    val postalCode: String?,
    val isRemote: Boolean = false,
    val isHybrid: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class JobBankSalary(
    val min: Double?,
    val max: Double?,
    val type: SalaryType,
    val currency: String = "CAD"
) {
    fun displayString(): String {
        val formatter = { value: Double -> "$${String.format("%.2f", value)}" }
        return when {
            min != null && max != null -> "${formatter(min)} - ${formatter(max)} ${type.display}"
            min != null -> "From ${formatter(min)} ${type.display}"
            max != null -> "Up to ${formatter(max)} ${type.display}"
            else -> "Salary not specified"
        }
    }
}

@Serializable
enum class SalaryType(val display: String) {
    HOURLY("per hour"),
    DAILY("per day"),
    WEEKLY("per week"),
    BIWEEKLY("bi-weekly"),
    MONTHLY("per month"),
    ANNUAL("per year")
}

@Serializable
enum class JobHours(val code: String, val displayEn: String, val displayFr: String) {
    FULL_TIME("ft", "Full-time", "Temps plein"),
    PART_TIME("pt", "Part-time", "Temps partiel"),
    FLEXIBLE("flex", "Flexible", "Flexible"),
    ON_CALL("oncall", "On Call", "Sur appel")
}

@Serializable
enum class JobPeriod(val code: String, val displayEn: String, val displayFr: String) {
    PERMANENT("perm", "Permanent", "Permanent"),
    TEMPORARY("temp", "Temporary", "Temporaire"),
    SEASONAL("seasonal", "Seasonal", "Saisonnier"),
    CONTRACT("contract", "Contract", "Contrat"),
    CASUAL("casual", "Casual", "Occasionnel")
}

@Serializable
data class JobOutlookResponse(
    val nocCode: String,
    val nocTitle: String,
    val nationalOutlook: OutlookRating,
    val provincialOutlook: List<ProvincialOutlook>,
    val medianWage: Double?,
    val employmentGrowth: String?,
    val retirements: String?,
    val jobOpenings: Int?,
    val lastUpdated: String
)

@Serializable
data class ProvincialOutlook(
    val provinceCode: String,
    val provinceName: String,
    val outlook: OutlookRating,
    val medianWage: Double?,
    val jobOpenings: Int?
)

@Serializable
enum class OutlookRating(val stars: Int, val displayEn: String, val displayFr: String) {
    VERY_GOOD(3, "Very Good", "Très bon"),
    GOOD(2, "Good", "Bon"),
    FAIR(1, "Fair", "Acceptable"),
    LIMITED(0, "Limited", "Limité"),
    UNDETERMINED(-1, "Undetermined", "Indéterminé")
}

// Canadian Province codes for filtering
enum class CanadianProvince(val code: String, val nameEn: String, val nameFr: String) {
    ALBERTA("AB", "Alberta", "Alberta"),
    BRITISH_COLUMBIA("BC", "British Columbia", "Colombie-Britannique"),
    MANITOBA("MB", "Manitoba", "Manitoba"),
    NEW_BRUNSWICK("NB", "New Brunswick", "Nouveau-Brunswick"),
    NEWFOUNDLAND("NL", "Newfoundland and Labrador", "Terre-Neuve-et-Labrador"),
    NORTHWEST_TERRITORIES("NT", "Northwest Territories", "Territoires du Nord-Ouest"),
    NOVA_SCOTIA("NS", "Nova Scotia", "Nouvelle-Écosse"),
    NUNAVUT("NU", "Nunavut", "Nunavut"),
    ONTARIO("ON", "Ontario", "Ontario"),
    PRINCE_EDWARD_ISLAND("PE", "Prince Edward Island", "Île-du-Prince-Édouard"),
    QUEBEC("QC", "Quebec", "Québec"),
    SASKATCHEWAN("SK", "Saskatchewan", "Saskatchewan"),
    YUKON("YT", "Yukon", "Yukon");
    
    companion object {
        fun fromCode(code: String): CanadianProvince? = entries.find { it.code == code }
    }
}
