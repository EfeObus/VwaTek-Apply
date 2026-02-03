package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.repository.AnalysisRepository
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.w3c.fetch.RequestInit
import kotlinx.coroutines.await
import kotlin.js.json as jsJson

/**
 * API-based Analysis Repository that communicates with the backend Cloud SQL database
 */
class ApiAnalysisRepository : AnalysisRepository {
    
    private val _analyses = MutableStateFlow<List<ResumeAnalysis>>(emptyList())
    
    override fun getAnalysesByResumeId(resumeId: String): Flow<List<ResumeAnalysis>> {
        refreshAnalyses(resumeId)
        return _analyses.asStateFlow().map { analyses ->
            analyses.filter { it.resumeId == resumeId }
        }
    }
    
    private fun refreshAnalyses(resumeId: String) {
        launchAsync {
            try {
                val response = window.fetch(
                    "${getApiBaseUrl()}/api/v1/resumes/$resumeId/analyses",
                    RequestInit(
                        method = "GET",
                        headers = jsJson("Content-Type" to "application/json")
                    )
                ).await()
                
                if (response.ok) {
                    val responseText = response.text().await()
                    val apiAnalyses = apiJson.decodeFromString<List<AnalysisApiResponse>>(responseText)
                    // Update cache with analyses for this resume
                    val existingOther = _analyses.value.filter { it.resumeId != resumeId }
                    _analyses.value = existingOther + apiAnalyses.map { it.toResumeAnalysis() }
                    console.log("Loaded ${apiAnalyses.size} analyses for resume $resumeId")
                } else {
                    console.error("Failed to load analyses: ${response.status}")
                }
            } catch (e: Exception) {
                console.error("Error loading analyses: ${e.message}")
            }
        }
    }
    
    override suspend fun insertAnalysis(analysis: ResumeAnalysis) {
        try {
            val requestBody = apiJson.encodeToString(AnalysisApiRequest(
                resumeId = analysis.resumeId,
                jobDescription = analysis.jobDescription,
                matchScore = analysis.matchScore,
                missingKeywords = analysis.missingKeywords,
                recommendations = analysis.recommendations
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/analyses",
                RequestInit(
                    method = "POST",
                    headers = jsJson("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val newAnalysis = apiJson.decodeFromString<AnalysisApiResponse>(responseText).toResumeAnalysis()
                _analyses.value = _analyses.value + newAnalysis
                console.log("Analysis created: ${newAnalysis.id}")
            } else {
                console.error("Failed to create analysis: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error inserting analysis: ${e.message}")
        }
    }
    
    override suspend fun deleteAnalysis(id: String) {
        try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/analyses/$id",
                RequestInit(
                    method = "DELETE",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok || response.status.toInt() == 204) {
                _analyses.value = _analyses.value.filter { it.id != id }
                console.log("Analysis deleted: $id")
            } else {
                console.error("Failed to delete analysis: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error deleting analysis: ${e.message}")
        }
    }
}

// API DTOs
@Serializable
private data class AnalysisApiRequest(
    val resumeId: String,
    val jobDescription: String,
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>
)

@Serializable
private data class AnalysisApiResponse(
    val id: String,
    val resumeId: String,
    val jobDescription: String,
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>,
    val createdAt: String
) {
    fun toResumeAnalysis(): ResumeAnalysis = ResumeAnalysis(
        id = id,
        resumeId = resumeId,
        jobDescription = jobDescription,
        matchScore = matchScore,
        missingKeywords = missingKeywords,
        recommendations = recommendations,
        createdAt = try { Instant.parse(createdAt) } catch (e: Exception) { Clock.System.now() }
    )
}
