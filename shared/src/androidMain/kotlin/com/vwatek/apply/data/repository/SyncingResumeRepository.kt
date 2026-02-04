package com.vwatek.apply.data.repository

import com.vwatek.apply.db.VwaTekDatabase
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

/**
 * Resume Repository that syncs between local SQLite database and remote API.
 * - Local database provides offline-first capability
 * - Syncs with backend API when user is logged in
 */
class SyncingResumeRepository(
    private val database: VwaTekDatabase,
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) : ResumeRepository {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queries = database.vwaTekDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    
    // Base URL for the API - should match the web app URL
    private val apiBaseUrl = "https://vwatek-backend-21443684777.us-central1.run.app"
    
    init {
        // Initial load from local database
        android.util.Log.d("SyncingRepo", "SyncingResumeRepository initialized")
        loadLocalResumes()
        // Trigger initial sync
        syncWithServer()
    }
    
    private fun loadLocalResumes() {
        android.util.Log.d("SyncingRepo", "Loading local resumes...")
        scope.launch {
            try {
                queries.selectAllResumes()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .collect { list ->
                        android.util.Log.d("SyncingRepo", "Loaded ${list.size} resumes from local DB")
                        _resumes.value = list.map { it.toDomain() }
                    }
            } catch (e: Exception) {
                android.util.Log.e("SyncingRepo", "Error loading local resumes", e)
            }
        }
    }
    
    override fun getAllResumes(): Flow<List<Resume>> {
        // Trigger background sync
        android.util.Log.d("SyncingRepo", "getAllResumes() called - triggering sync")
        syncWithServer()
        
        return queries.selectAllResumes()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun getResumeById(id: String): Resume? {
        return queries.selectResumeById(id).executeAsOneOrNull()?.toDomain()
    }
    
    override suspend fun getResumesByUserId(userId: String): List<Resume> {
        return queries.selectResumesByUserId(userId).executeAsList().map { it.toDomain() }
    }
    
    override suspend fun insertResume(resume: Resume) {
        // Insert locally first
        queries.insertResume(
            id = resume.id,
            userId = resume.userId,
            name = resume.name,
            content = resume.content,
            industry = resume.industry,
            createdAt = resume.createdAt.toEpochMilliseconds(),
            updatedAt = resume.updatedAt.toEpochMilliseconds()
        )
        
        // Sync to server if user is logged in
        syncResumeToServer(resume)
    }
    
    override suspend fun updateResume(resume: Resume) {
        queries.updateResume(
            name = resume.name,
            content = resume.content,
            industry = resume.industry,
            updatedAt = resume.updatedAt.toEpochMilliseconds(),
            id = resume.id
        )
        
        // Sync update to server
        updateResumeOnServer(resume)
    }
    
    override suspend fun deleteResume(id: String) {
        queries.deleteResume(id)
        
        // Sync deletion to server
        deleteResumeOnServer(id)
    }
    
    // Version control - stub implementations
    override fun getVersionsByResumeId(resumeId: String): Flow<List<ResumeVersion>> = flowOf(emptyList())
    override suspend fun getVersionById(id: String): ResumeVersion? = null
    override suspend fun insertVersion(version: ResumeVersion) { }
    override suspend fun deleteVersion(id: String) { }
    override suspend fun deleteVersionsByResumeId(resumeId: String) { }
    
    // --- Sync Methods ---
    
    private fun syncWithServer() {
        scope.launch {
            try {
                val user = authRepository.getCurrentUser()
                // Don't filter by userId when fetching - get all resumes like web app does
                // The web app sends empty string which makes backend return all resumes
                
                android.util.Log.d("SyncingRepo", "Syncing with server (fetching all resumes)")
                
                val response = httpClient.get("$apiBaseUrl/api/v1/resumes") {
                    contentType(ContentType.Application.Json)
                    // Don't send X-User-Id to get all resumes
                }
                
                android.util.Log.d("SyncingRepo", "API response status: ${response.status}")
                
                if (response.status.isSuccess()) {
                    val responseText = response.bodyAsText()
                    android.util.Log.d("SyncingRepo", "API response: $responseText")
                    
                    val serverResumes: List<ResumeApiResponse> = json.decodeFromString(responseText)
                    android.util.Log.d("SyncingRepo", "Loaded ${serverResumes.size} resumes from server")
                    
                    // Merge server resumes with local
                    for (apiResume in serverResumes) {
                        val localResume = queries.selectResumeById(apiResume.id).executeAsOneOrNull()
                        val serverTimestamp = parseInstant(apiResume.updatedAt).toEpochMilliseconds()
                        
                        if (localResume == null) {
                            // Server has resume we don't have - insert it
                            queries.insertResume(
                                id = apiResume.id,
                                userId = user?.id,
                                name = apiResume.name,
                                content = apiResume.content,
                                industry = apiResume.industry,
                                createdAt = parseInstant(apiResume.createdAt).toEpochMilliseconds(),
                                updatedAt = serverTimestamp
                            )
                            android.util.Log.d("SyncingRepo", "Inserted resume from server: ${apiResume.name}")
                        } else if (serverTimestamp > localResume.updatedAt) {
                            // Server version is newer - update local
                            queries.updateResume(
                                name = apiResume.name,
                                content = apiResume.content,
                                industry = apiResume.industry,
                                updatedAt = serverTimestamp,
                                id = apiResume.id
                            )
                            android.util.Log.d("SyncingRepo", "Updated resume from server: ${apiResume.name}")
                        }
                    }
                    
                    // Push local-only resumes to server (only if logged in)
                    if (user != null) {
                        val localResumes = queries.selectResumesByUserId(user.id).executeAsList()
                        val serverIds = serverResumes.map { it.id }.toSet()
                        
                        for (local in localResumes) {
                            if (local.id !in serverIds) {
                                // Local resume not on server - push it
                                pushResumeToServer(local.toDomain(), user.id)
                            }
                        }
                    }
                } else {
                    android.util.Log.e("SyncingRepo", "API request failed: ${response.status}")
                }
            } catch (e: Exception) {
                // Sync failed - continue with local data
                android.util.Log.e("SyncingRepo", "Sync failed: ${e.message}", e)
            }
        }
    }
    
    private suspend fun syncResumeToServer(resume: Resume) {
        try {
            val user = authRepository.getCurrentUser() ?: return
            pushResumeToServer(resume, user.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun pushResumeToServer(resume: Resume, userId: String) {
        try {
            val requestBody = json.encodeToString(ResumeApiRequest(
                name = resume.name,
                content = resume.content,
                industry = resume.industry,
                sourceType = resume.sourceType.name
            ))
            
            httpClient.post("$apiBaseUrl/api/v1/resumes") {
                contentType(ContentType.Application.Json)
                header("X-User-Id", userId)
                setBody(requestBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun updateResumeOnServer(resume: Resume) {
        try {
            val user = authRepository.getCurrentUser() ?: return
            
            val requestBody = json.encodeToString(ResumeApiRequest(
                name = resume.name,
                content = resume.content,
                industry = resume.industry,
                sourceType = resume.sourceType.name
            ))
            
            httpClient.put("$apiBaseUrl/api/v1/resumes/${resume.id}") {
                contentType(ContentType.Application.Json)
                header("X-User-Id", user.id)
                setBody(requestBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun deleteResumeOnServer(id: String) {
        try {
            val user = authRepository.getCurrentUser() ?: return
            
            httpClient.delete("$apiBaseUrl/api/v1/resumes/$id") {
                header("X-User-Id", user.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun parseInstant(isoString: String): Instant {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            Clock.System.now()
        }
    }
}

// Extension function to convert database entity to domain model
private fun com.vwatek.apply.db.Resume.toDomain(): Resume {
    return Resume(
        id = this.id,
        userId = this.userId,
        name = this.name,
        content = this.content,
        industry = this.industry,
        sourceType = ResumeSourceType.MANUAL,
        createdAt = Instant.fromEpochMilliseconds(this.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(this.updatedAt)
    )
}

@Serializable
private data class ResumeApiRequest(
    val name: String,
    val content: String,
    val industry: String?,
    val sourceType: String = "MANUAL"
)

@Serializable
private data class ResumeApiResponse(
    val id: String,
    val name: String,
    val content: String,
    val industry: String?,
    val sourceType: String,
    val createdAt: String,
    val updatedAt: String
)
