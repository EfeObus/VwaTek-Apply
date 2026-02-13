package com.vwatek.apply.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import com.vwatek.apply.domain.model.*

/**
 * Job Bank Canada API Client
 * Interacts with backend Job Bank integration endpoints
 */
class JobBankApiClient(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig = ApiConfig
) {
    private val baseUrl get() = "${apiConfig.apiV1Url}/jobbank"
    
    /**
     * Search for jobs on Job Bank Canada
     */
    suspend fun searchJobs(
        query: String? = null,
        location: String? = null,
        provinceCode: String? = null,
        nocCode: String? = null,
        page: Int = 0,
        perPage: Int = 20
    ): Result<JobBankSearchResponse> = runCatching {
        httpClient.get("$baseUrl/search") {
            query?.let { parameter("q", it) }
            location?.let { parameter("location", it) }
            provinceCode?.let { parameter("province", it) }
            nocCode?.let { parameter("nocCode", it) }
            parameter("page", page)
            parameter("perPage", perPage)
        }.body()
    }
    
    /**
     * Get detailed job information
     */
    suspend fun getJobDetails(jobId: String): Result<JobBankJobResponse> = runCatching {
        httpClient.get("$baseUrl/jobs/$jobId").body()
    }
    
    /**
     * Search jobs by NOC code
     */
    suspend fun searchByNOC(nocCode: String, page: Int = 0, perPage: Int = 20): Result<JobBankSearchResponse> = runCatching {
        httpClient.get("$baseUrl/noc/$nocCode") {
            parameter("page", page)
            parameter("perPage", perPage)
        }.body()
    }
    
    /**
     * Search jobs by province
     */
    suspend fun searchByProvince(provinceCode: String, page: Int = 0, perPage: Int = 20): Result<JobBankSearchResponse> = runCatching {
        httpClient.get("$baseUrl/province/$provinceCode") {
            parameter("page", page)
            parameter("perPage", perPage)
        }.body()
    }
    
    /**
     * Get trending jobs
     */
    suspend fun getTrendingJobs(provinceCode: String? = null, limit: Int = 10): Result<TrendingJobsResponse> = runCatching {
        httpClient.get("$baseUrl/trending") {
            provinceCode?.let { parameter("province", it) }
            parameter("limit", limit)
        }.body()
    }
    
    /**
     * Get job outlook for a NOC code
     */
    suspend fun getJobOutlook(nocCode: String, provinceCode: String? = null): Result<JobOutlookResponse> = runCatching {
        httpClient.get("$baseUrl/outlook/$nocCode") {
            provinceCode?.let { parameter("province", it) }
        }.body()
    }
    
    /**
     * Get list of Canadian provinces
     */
    suspend fun getProvinces(): Result<List<ProvinceDto>> = runCatching {
        httpClient.get("$baseUrl/provinces").body()
    }
}

// Response DTOs
@Serializable
data class JobBankSearchResponse(
    val jobs: List<JobBankJobResponse>,
    val page: Int,
    val perPage: Int,
    val total: Int,
    val hasMore: Boolean
)

@Serializable
data class JobBankJobResponse(
    val id: String,
    val title: String,
    val employer: String,
    val location: JobBankLocationResponse,
    val salary: JobBankSalaryResponse? = null,
    val nocCode: String? = null,
    val postingDate: String,
    val expiryDate: String? = null,
    val description: String,
    val requirements: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val hours: String? = null,
    val jobType: String? = null,
    val vacancies: Int = 1,
    val url: String
) {
    fun toDomainModel() = JobBankJob(
        id = id,
        title = title,
        employer = employer,
        location = JobBankLocation(
            city = location.city,
            province = location.province,
            postalCode = location.postalCode,
            isRemote = location.isRemote
        ),
        salary = salary?.let { 
            JobBankSalary(
                min = it.min,
                max = it.max,
                period = it.period,
                currency = it.currency
            )
        },
        nocCode = nocCode,
        postingDate = postingDate,
        expiryDate = expiryDate,
        description = description,
        requirements = requirements,
        benefits = benefits,
        hours = hours,
        jobType = jobType,
        vacancies = vacancies,
        url = url
    )
}

@Serializable
data class JobBankLocationResponse(
    val city: String,
    val province: String,
    val postalCode: String? = null,
    val isRemote: Boolean = false
)

@Serializable
data class JobBankSalaryResponse(
    val min: Double? = null,
    val max: Double? = null,
    val period: String,
    val currency: String = "CAD"
)

@Serializable
data class TrendingJobsResponse(
    val jobs: List<JobBankJobResponse>,
    val province: String? = null,
    val count: Int
)

@Serializable
data class JobOutlookResponse(
    val nocCode: String,
    val province: String? = null,
    val rating: String,
    val description: String,
    val employmentGrowth: Double? = null,
    val retirementReplacements: Int? = null,
    val projectedOpenings: Int? = null,
    val medianWage: Double? = null
) {
    fun toDomainModel() = JobOutlook(
        nocCode = nocCode,
        provinceCode = province,
        rating = OutlookRating.entries.find { it.name == rating } ?: OutlookRating.UNDETERMINED,
        description = description,
        employmentGrowth = employmentGrowth,
        retirementReplacements = retirementReplacements,
        projectedOpenings = projectedOpenings,
        medianWage = medianWage
    )
}

@Serializable
data class ProvinceDto(
    val code: String,
    val name: String,
    val nameFr: String
) {
    fun toDomainModel() = CanadianProvince(code, name, nameFr)
}
