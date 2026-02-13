package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 4: Salary Intelligence API Client
 * Handles salary insights, offer evaluation, and negotiation coaching
 */
class SalaryApiClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "${ApiConfig.apiV1Url}/salary"
    // Negotiation routes are nested under /salary in the backend
    private val negotiationBaseUrl = "${ApiConfig.apiV1Url}/salary"
    
    // ===== Salary Insights =====
    
    /**
     * Get salary insights for a job title and location
     */
    suspend fun getSalaryInsights(request: SalaryInsightsRequest): Result<SalaryInsightsResponse> {
        return try {
            val response = httpClient.post("$baseUrl/insights") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's salary comparison history
     */
    suspend fun getSalaryHistory(): Result<SalaryHistoryResponse> {
        return try {
            val response = httpClient.get("$baseUrl/history")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Offer Evaluation =====
    
    /**
     * Evaluate a job offer
     */
    suspend fun evaluateOffer(request: EvaluateOfferRequest): Result<OfferEvaluationResponse> {
        return try {
            val response = httpClient.post("$baseUrl/evaluate-offer") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's saved job offers
     */
    suspend fun getOffers(): Result<OffersListResponse> {
        return try {
            val response = httpClient.get("$baseUrl/offers")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update offer status (PENDING, ACCEPTED, DECLINED, NEGOTIATING)
     */
    suspend fun updateOfferStatus(offerId: String, status: String): Result<MessageResponse> {
        return try {
            val response = httpClient.put("$baseUrl/offers/$offerId/status") {
                contentType(ContentType.Application.Json)
                setBody(UpdateOfferStatusRequest(status))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Negotiation Coach =====
    
    /**
     * Create a negotiation coaching session for an offer
     */
    suspend fun createNegotiationSession(offerId: String): Result<NegotiationSessionResponse> {
        return try {
            val response = httpClient.post("$negotiationBaseUrl/sessions") {
                contentType(ContentType.Application.Json)
                setBody(CreateNegotiationSessionRequest(offerId))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a negotiation session
     */
    suspend fun getNegotiationSession(sessionId: String): Result<NegotiationSessionResponse> {
        return try {
            val response = httpClient.get("$negotiationBaseUrl/sessions/$sessionId")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a message to the negotiation coach
     */
    suspend fun sendNegotiationMessage(
        sessionId: String,
        content: String
    ): Result<NegotiationSessionResponse> {
        return try {
            val response = httpClient.post("$negotiationBaseUrl/sessions/$sessionId/messages") {
                contentType(ContentType.Application.Json)
                setBody(SendMessageRequest(content))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ===== Response DTOs =====

@Serializable
data class SalaryInsightsResponse(
    val insights: SalaryInsights,
    val chartData: SalaryChartData?
)

@Serializable
data class SalaryChartData(
    val ranges: List<SalaryRangeData>,
    val trend: List<SalaryTrendData>
)

@Serializable
data class SalaryRangeData(
    val label: String,
    val value: Double,
    val percentile: Int
)

@Serializable
data class SalaryTrendData(
    val year: Int,
    val median: Double
)

@Serializable
data class SalaryHistoryResponse(
    val history: List<SalaryHistoryItem>
)

@Serializable
data class SalaryHistoryItem(
    val id: String,
    val jobTitle: String,
    val province: String,
    val city: String?,
    val createdAt: String
)

@Serializable
data class OfferEvaluationResponse(
    val evaluation: OfferEvaluation
)

@Serializable
data class OffersListResponse(
    val offers: List<OfferListItem>
)

@Serializable
data class OfferListItem(
    val id: String,
    val jobTitle: String,
    val company: String,
    val baseSalary: Double,
    val status: String,
    val createdAt: String
)

@Serializable
data class UpdateOfferStatusRequest(
    val status: String
)

@Serializable
data class CreateNegotiationSessionRequest(
    val offerId: String
)

@Serializable
data class SendMessageRequest(
    val content: String
)

@Serializable
data class NegotiationSessionResponse(
    val session: NegotiationSessionDto
)

@Serializable
data class NegotiationSessionDto(
    val id: String,
    val userId: String,
    val offerId: String,
    val status: String,
    val messages: List<NegotiationMessageDto>,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class NegotiationMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val suggestedResponse: String?,
    val timestamp: String
)
