package com.vwatek.apply.routes

import com.vwatek.apply.integrations.jobbank.JobBankApiClient
import com.vwatek.apply.integrations.jobbank.CanadianProvince
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Phase 3: Job Bank Canada Integration Routes
 * Provides access to Job Bank job listings and labour market information
 */
fun Route.jobBankRoutes() {
    val jobBankClient = JobBankApiClient()
    
    route("/jobbank") {
        /**
         * Search jobs
         * GET /api/v1/jobbank/search?q=developer&location=Toronto&province=ON&nocCode=21232&page=0&perPage=20
         */
        get("/search") {
            try {
                val query = call.parameters["q"]
                val location = call.parameters["location"]
                val provinceCode = call.parameters["province"]
                val nocCode = call.parameters["nocCode"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 0
                val perPage = call.parameters["perPage"]?.toIntOrNull() ?: 20
                
                val province = provinceCode?.let { 
                    CanadianProvince.entries.find { p -> p.name == it }
                }
                
                val jobs = jobBankClient.searchJobs(
                    query = query,
                    location = location,
                    province = province,
                    nocCode = nocCode,
                    page = page,
                    perPage = perPage
                )
                
                call.respond(JobBankSearchResponse(
                    jobs = jobs.map { it.toResponse() },
                    page = page,
                    perPage = perPage,
                    total = jobs.size,
                    hasMore = jobs.size == perPage
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        /**
         * Get job details
         * GET /api/v1/jobbank/jobs/{jobId}
         */
        get("/jobs/{jobId}") {
            try {
                val jobId = call.parameters["jobId"] 
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Job ID required"))
                
                val job = jobBankClient.getJobDetails(jobId)
                if (job != null) {
                    call.respond(job.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Job not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        /**
         * Search jobs by NOC code
         * GET /api/v1/jobbank/noc/{nocCode}?page=0&perPage=20
         */
        get("/noc/{nocCode}") {
            try {
                val nocCode = call.parameters["nocCode"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "NOC code required"))
                val page = call.parameters["page"]?.toIntOrNull() ?: 0
                val perPage = call.parameters["perPage"]?.toIntOrNull() ?: 20
                
                val jobs = jobBankClient.searchByNOC(nocCode, page, perPage)
                
                call.respond(JobBankSearchResponse(
                    jobs = jobs.map { it.toResponse() },
                    page = page,
                    perPage = perPage,
                    total = jobs.size,
                    hasMore = jobs.size == perPage
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        /**
         * Search jobs by province
         * GET /api/v1/jobbank/province/{provinceCode}?page=0&perPage=20
         */
        get("/province/{provinceCode}") {
            try {
                val provinceCode = call.parameters["provinceCode"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Province code required"))
                
                val province = CanadianProvince.entries.find { it.name == provinceCode }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid province code"))
                
                val page = call.parameters["page"]?.toIntOrNull() ?: 0
                val perPage = call.parameters["perPage"]?.toIntOrNull() ?: 20
                
                val jobs = jobBankClient.searchByProvince(province, page, perPage)
                
                call.respond(JobBankSearchResponse(
                    jobs = jobs.map { it.toResponse() },
                    page = page,
                    perPage = perPage,
                    total = jobs.size,
                    hasMore = jobs.size == perPage
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        /**
         * Get trending jobs
         * GET /api/v1/jobbank/trending?province=ON&limit=10
         */
        get("/trending") {
            try {
                val provinceCode = call.parameters["province"]
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
                
                val province = provinceCode?.let { 
                    CanadianProvince.entries.find { p -> p.name == it }
                }
                
                val jobs = jobBankClient.getTrendingJobs(province, limit)
                
                call.respond(mapOf(
                    "jobs" to jobs.map { it.toResponse() },
                    "province" to provinceCode,
                    "count" to jobs.size
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        /**
         * Get job outlook for an occupation
         * GET /api/v1/jobbank/outlook/{nocCode}?province=ON
         */
        get("/outlook/{nocCode}") {
            try {
                val nocCode = call.parameters["nocCode"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "NOC code required"))
                val provinceCode = call.parameters["province"]
                
                val province = provinceCode?.let { 
                    CanadianProvince.entries.find { p -> p.name == it }
                }
                
                val outlook = jobBankClient.getJobOutlook(nocCode, province)
                call.respond(outlook)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
        
        /**
         * Get list of Canadian provinces
         * GET /api/v1/jobbank/provinces
         */
        get("/provinces") {
            call.respond(CanadianProvince.entries.map { province ->
                ProvinceResponse(
                    code = province.name,
                    name = province.displayName,
                    nameFr = province.displayNameFr
                )
            })
        }
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
    val salary: JobBankSalaryResponse?,
    val nocCode: String?,
    val postingDate: String,
    val expiryDate: String?,
    val description: String,
    val requirements: List<String>,
    val benefits: List<String>,
    val hours: String?,
    val jobType: String?,
    val vacancies: Int,
    val url: String
)

@Serializable
data class JobBankLocationResponse(
    val city: String,
    val province: String,
    val postalCode: String?,
    val isRemote: Boolean
)

@Serializable
data class JobBankSalaryResponse(
    val min: Double?,
    val max: Double?,
    val period: String,
    val currency: String = "CAD"
)

@Serializable
data class ProvinceResponse(
    val code: String,
    val name: String,
    val nameFr: String
)

// Extension function to convert domain model to response
private fun com.vwatek.apply.integrations.jobbank.JobBankJob.toResponse() = JobBankJobResponse(
    id = id,
    title = title,
    employer = employer,
    location = JobBankLocationResponse(
        city = location.city,
        province = location.province.displayName,
        postalCode = location.postalCode,
        isRemote = location.isRemote
    ),
    salary = salary?.let { 
        JobBankSalaryResponse(
            min = it.min,
            max = it.max,
            period = it.period.name
        )
    },
    nocCode = nocCode,
    postingDate = postingDate,
    expiryDate = expiryDate,
    description = description,
    requirements = requirements,
    benefits = benefits,
    hours = hours?.name,
    jobType = jobType,
    vacancies = vacancies,
    url = url
)
