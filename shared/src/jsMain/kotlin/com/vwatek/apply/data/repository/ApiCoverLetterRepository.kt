package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.repository.CoverLetterRepository
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.w3c.fetch.RequestInit
import kotlinx.coroutines.await
import kotlin.js.json as jsJson

/**
 * API-based Cover Letter Repository that communicates with the backend Cloud SQL database
 */
class ApiCoverLetterRepository : CoverLetterRepository {
    
    private val _coverLetters = MutableStateFlow<List<CoverLetter>>(emptyList())
    
    override fun getAllCoverLetters(): Flow<List<CoverLetter>> {
        refreshCoverLetters()
        return _coverLetters.asStateFlow()
    }
    
    private fun refreshCoverLetters() {
        launchAsync {
            try {
                val userId = getCurrentUserId()
                val response = window.fetch(
                    "${getApiBaseUrl()}/api/v1/cover-letters",
                    RequestInit(
                        method = "GET",
                        headers = jsJson(
                            "Content-Type" to "application/json",
                            "X-User-Id" to (userId ?: "")
                        )
                    )
                ).await()
                
                if (response.ok) {
                    val responseText = response.text().await()
                    val apiCoverLetters = apiJson.decodeFromString<List<CoverLetterApiResponse>>(responseText)
                    _coverLetters.value = apiCoverLetters.map { it.toCoverLetter() }
                    console.log("Loaded ${apiCoverLetters.size} cover letters from API")
                } else {
                    console.error("Failed to load cover letters: ${response.status}")
                }
            } catch (e: Exception) {
                console.error("Error loading cover letters: ${e.message}")
            }
        }
    }
    
    override suspend fun getCoverLetterById(id: String): CoverLetter? {
        return try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/cover-letters/$id",
                RequestInit(
                    method = "GET",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val apiCoverLetter = apiJson.decodeFromString<CoverLetterApiResponse>(responseText)
                apiCoverLetter.toCoverLetter()
            } else {
                null
            }
        } catch (e: Exception) {
            console.error("Error getting cover letter: ${e.message}")
            null
        }
    }
    
    override suspend fun insertCoverLetter(coverLetter: CoverLetter) {
        try {
            val userId = getCurrentUserId()
            val requestBody = apiJson.encodeToString(CoverLetterApiRequest(
                resumeId = coverLetter.resumeId,
                jobTitle = coverLetter.jobTitle,
                companyName = coverLetter.companyName,
                content = coverLetter.content,
                tone = coverLetter.tone.name
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/cover-letters",
                RequestInit(
                    method = "POST",
                    headers = jsJson(
                        "Content-Type" to "application/json",
                        "X-User-Id" to (userId ?: "")
                    ),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val newCoverLetter = apiJson.decodeFromString<CoverLetterApiResponse>(responseText).toCoverLetter()
                _coverLetters.value = _coverLetters.value + newCoverLetter
                console.log("Cover letter created: ${newCoverLetter.id}")
            } else {
                console.error("Failed to create cover letter: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error inserting cover letter: ${e.message}")
        }
    }
    
    override suspend fun updateCoverLetter(id: String, content: String) {
        try {
            val existing = _coverLetters.value.find { it.id == id } ?: return
            val requestBody = apiJson.encodeToString(CoverLetterApiRequest(
                resumeId = existing.resumeId,
                jobTitle = existing.jobTitle,
                companyName = existing.companyName,
                content = content,
                tone = existing.tone.name
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/cover-letters/$id",
                RequestInit(
                    method = "PUT",
                    headers = jsJson("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                _coverLetters.value = _coverLetters.value.map { 
                    if (it.id == id) it.copy(content = content) else it 
                }
                console.log("Cover letter updated: $id")
            } else {
                console.error("Failed to update cover letter: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error updating cover letter: ${e.message}")
        }
    }
    
    override suspend fun deleteCoverLetter(id: String) {
        try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/cover-letters/$id",
                RequestInit(
                    method = "DELETE",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok || response.status.toInt() == 204) {
                _coverLetters.value = _coverLetters.value.filter { it.id != id }
                console.log("Cover letter deleted: $id")
            } else {
                console.error("Failed to delete cover letter: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error deleting cover letter: ${e.message}")
        }
    }
}

// API DTOs
@Serializable
private data class CoverLetterApiRequest(
    val resumeId: String? = null,
    val jobTitle: String,
    val companyName: String,
    val content: String,
    val tone: String = "PROFESSIONAL"
)

@Serializable
private data class CoverLetterApiResponse(
    val id: String,
    val resumeId: String? = null,
    val jobTitle: String,
    val companyName: String,
    val content: String,
    val tone: String,
    val createdAt: String
) {
    fun toCoverLetter(): CoverLetter = CoverLetter(
        id = id,
        resumeId = resumeId,
        jobTitle = jobTitle,
        companyName = companyName,
        content = content,
        tone = try { CoverLetterTone.valueOf(tone) } catch (e: Exception) { CoverLetterTone.PROFESSIONAL },
        createdAt = try { Instant.parse(createdAt) } catch (e: Exception) { Clock.System.now() }
    )
}
