package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.LinkedInProfile
import com.vwatek.apply.domain.model.OptimizedLinkedInContent
import com.vwatek.apply.domain.usecase.LinkedInAnalysisResult
import com.vwatek.apply.domain.usecase.LinkedInApiClient
import com.vwatek.apply.domain.usecase.LinkedInSection
import com.vwatek.apply.domain.usecase.HeadlineSuggestion
import com.vwatek.apply.domain.usecase.SummarySuggestion
import com.vwatek.apply.domain.usecase.SummaryTone
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 12: LinkedIn API Client Implementation
 * Communicates with backend LinkedIn optimizer endpoints
 */
class LinkedInApiClientImpl(
    private val httpClient: HttpClient
) : LinkedInApiClient {
    private val baseUrl = "${ApiConfig.apiV1Url}/linkedin"

    override suspend fun analyzeProfile(
        profileUrl: String?,
        manualProfile: LinkedInProfile?
    ): Result<LinkedInAnalysisResult> {
        return try {
            val response = httpClient.post("$baseUrl/analyze") {
                contentType(ContentType.Application.Json)
                setBody(AnalyzeProfileRequest(profileUrl, manualProfile))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOptimizedContent(
        profileId: String,
        targetRole: String?,
        targetIndustry: String?,
        focusAreas: List<LinkedInSection>
    ): Result<OptimizedLinkedInContent> {
        return try {
            val response = httpClient.post("$baseUrl/optimize") {
                contentType(ContentType.Application.Json)
                setBody(OptimizeContentRequest(profileId, targetRole, targetIndustry, focusAreas))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProfile(profile: LinkedInProfile): Result<LinkedInProfile> {
        return try {
            val response = httpClient.post("$baseUrl/profile") {
                contentType(ContentType.Application.Json)
                setBody(profile)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAnalysisHistory(): Result<List<LinkedInAnalysisResult>> {
        return try {
            val response = httpClient.get("$baseUrl/history")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateHeadlines(
        currentHeadline: String,
        targetRole: String,
        skills: List<String>,
        yearsExperience: Int
    ): Result<List<HeadlineSuggestion>> {
        return try {
            val response = httpClient.post("$baseUrl/headlines") {
                contentType(ContentType.Application.Json)
                setBody(GenerateHeadlinesRequest(currentHeadline, targetRole, skills, yearsExperience))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateSummary(
        currentSummary: String?,
        experience: List<String>,
        skills: List<String>,
        targetRole: String,
        tone: SummaryTone
    ): Result<SummarySuggestion> {
        return try {
            val response = httpClient.post("$baseUrl/summary") {
                contentType(ContentType.Application.Json)
                setBody(GenerateSummaryRequest(currentSummary, experience, skills, targetRole, tone))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Request DTOs
@Serializable
private data class AnalyzeProfileRequest(
    val profileUrl: String? = null,
    val manualProfile: LinkedInProfile? = null
)

@Serializable
private data class OptimizeContentRequest(
    val profileId: String,
    val targetRole: String? = null,
    val targetIndustry: String? = null,
    val focusAreas: List<LinkedInSection> = emptyList()
)

@Serializable
private data class GenerateHeadlinesRequest(
    val currentHeadline: String,
    val targetRole: String,
    val skills: List<String>,
    val yearsExperience: Int
)

@Serializable
private data class GenerateSummaryRequest(
    val currentSummary: String? = null,
    val experience: List<String>,
    val skills: List<String>,
    val targetRole: String,
    val tone: SummaryTone
)
