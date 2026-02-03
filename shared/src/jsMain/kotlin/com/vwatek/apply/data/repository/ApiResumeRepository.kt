package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.domain.repository.ResumeRepository
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
 * API-based Resume Repository that communicates with the backend Cloud SQL database
 */
class ApiResumeRepository : ResumeRepository {
    
    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    private val _versions = MutableStateFlow<List<ResumeVersion>>(emptyList())
    
    override fun getAllResumes(): Flow<List<Resume>> {
        refreshResumes()
        return _resumes.asStateFlow()
    }
    
    private fun refreshResumes() {
        launchAsync {
            try {
                val userId = getCurrentUserId()
                val response = window.fetch(
                    "${getApiBaseUrl()}/api/v1/resumes",
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
                    val apiResumes = apiJson.decodeFromString<List<ResumeApiResponse>>(responseText)
                    _resumes.value = apiResumes.map { it.toResume(userId) }
                    console.log("Loaded ${apiResumes.size} resumes from API")
                } else {
                    console.error("Failed to load resumes: ${response.status}")
                }
            } catch (e: Exception) {
                console.error("Error loading resumes: ${e.message}")
            }
        }
    }
    
    override suspend fun getResumeById(id: String): Resume? {
        return try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/resumes/$id",
                RequestInit(
                    method = "GET",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val apiResume = apiJson.decodeFromString<ResumeApiResponse>(responseText)
                apiResume.toResume(getCurrentUserId())
            } else {
                null
            }
        } catch (e: Exception) {
            console.error("Error getting resume: ${e.message}")
            null
        }
    }
    
    override suspend fun getResumesByUserId(userId: String): List<Resume> {
        return _resumes.value.filter { it.userId == userId }
    }
    
    override suspend fun insertResume(resume: Resume) {
        try {
            val userId = getCurrentUserId()
            val requestBody = apiJson.encodeToString(ResumeApiRequest(
                name = resume.name,
                content = resume.content,
                industry = resume.industry,
                sourceType = resume.sourceType.name
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/resumes",
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
                val newResume = apiJson.decodeFromString<ResumeApiResponse>(responseText).toResume(userId)
                _resumes.value = _resumes.value + newResume
                console.log("Resume created: ${newResume.id}")
            } else {
                console.error("Failed to create resume: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error inserting resume: ${e.message}")
        }
    }
    
    override suspend fun updateResume(resume: Resume) {
        try {
            val requestBody = apiJson.encodeToString(ResumeApiRequest(
                name = resume.name,
                content = resume.content,
                industry = resume.industry,
                sourceType = resume.sourceType.name
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/resumes/${resume.id}",
                RequestInit(
                    method = "PUT",
                    headers = jsJson("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                _resumes.value = _resumes.value.map { 
                    if (it.id == resume.id) resume else it 
                }
                console.log("Resume updated: ${resume.id}")
            } else {
                console.error("Failed to update resume: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error updating resume: ${e.message}")
        }
    }
    
    override suspend fun deleteResume(id: String) {
        try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/resumes/$id",
                RequestInit(
                    method = "DELETE",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok || response.status.toInt() == 204) {
                _resumes.value = _resumes.value.filter { it.id != id }
                console.log("Resume deleted: $id")
            } else {
                console.error("Failed to delete resume: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error deleting resume: ${e.message}")
        }
    }
    
    // Version methods - stored locally for now
    override fun getVersionsByResumeId(resumeId: String): Flow<List<ResumeVersion>> {
        return MutableStateFlow(_versions.value.filter { it.resumeId == resumeId }).asStateFlow()
    }
    
    override suspend fun getVersionById(id: String): ResumeVersion? {
        return _versions.value.find { it.id == id }
    }
    
    override suspend fun insertVersion(version: ResumeVersion) {
        _versions.value = _versions.value + version
    }
    
    override suspend fun deleteVersion(id: String) {
        _versions.value = _versions.value.filter { it.id != id }
    }
    
    override suspend fun deleteVersionsByResumeId(resumeId: String) {
        _versions.value = _versions.value.filter { it.resumeId != resumeId }
    }
}

// API DTOs
@Serializable
private data class ResumeApiRequest(
    val name: String,
    val content: String,
    val industry: String? = null,
    val sourceType: String = "MANUAL"
)

@Serializable
private data class ResumeApiResponse(
    val id: String,
    val name: String,
    val content: String,
    val industry: String? = null,
    val sourceType: String = "MANUAL",
    val createdAt: String,
    val updatedAt: String
) {
    fun toResume(userId: String?): Resume = Resume(
        id = id,
        userId = userId,
        name = name,
        content = content,
        industry = industry,
        sourceType = try { ResumeSourceType.valueOf(sourceType) } catch (e: Exception) { ResumeSourceType.MANUAL },
        createdAt = try { Instant.parse(createdAt) } catch (e: Exception) { Clock.System.now() },
        updatedAt = try { Instant.parse(updatedAt) } catch (e: Exception) { Clock.System.now() }
    )
}
