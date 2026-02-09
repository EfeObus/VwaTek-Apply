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
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * iOS Resume Repository that syncs between local SQLite database and remote API.
 * - Local database provides offline-first capability
 * - Syncs with backend API when user is logged in
 */
class IosSyncingResumeRepository(
    private val database: VwaTekDatabase,
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) : ResumeRepository {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queries = database.vwaTekDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    
    // Base URL for the API
    private val apiBaseUrl = "https://vwatek-backend-21443684777.us-central1.run.app"
    
    init {
        println("IosSyncingResumeRepository initialized")
        loadLocalResumes()
        syncWithServer()
    }
    
    private fun loadLocalResumes() {
        println("Loading local resumes...")
        scope.launch {
            try {
                queries.selectAllResumes()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .collect { list ->
                        println("Loaded ${list.size} resumes from local DB")
                        _resumes.value = list.map { it.toResumeDomain() }
                    }
            } catch (e: Exception) {
                println("Error loading local resumes: ${e.message}")
            }
        }
    }
    
    override fun getAllResumes(): Flow<List<Resume>> {
        println("getAllResumes() called - triggering sync")
        syncWithServer()
        
        return queries.selectAllResumes()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toResumeDomain() } }
    }
    
    override suspend fun getResumeById(id: String): Resume? {
        return queries.selectResumeById(id).executeAsOneOrNull()?.toResumeDomain()
    }
    
    override suspend fun getResumesByUserId(userId: String): List<Resume> {
        return queries.selectResumesByUserId(userId).executeAsList().map { it.toResumeDomain() }
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
                
                if (user == null) {
                    println("IosSyncingResumeRepository: No user logged in - skipping sync")
                    return@launch
                }
                
                println("IosSyncingResumeRepository: Syncing with server for user: ${user.id}")
                
                val response = httpClient.get("$apiBaseUrl/api/v1/resumes") {
                    contentType(ContentType.Application.Json)
                    header("X-User-Id", user.id)
                }
                
                println("IosSyncingResumeRepository: API response status: ${response.status}")
                
                if (response.status.isSuccess()) {
                    val responseText = response.bodyAsText()
                    println("IosSyncingResumeRepository: API response length: ${responseText.length}")
                    
                    val serverResumes: List<ResumeApiResponse> = json.decodeFromString(responseText)
                    println("IosSyncingResumeRepository: Parsed ${serverResumes.size} resumes from server")
                    
                    // Clear existing resumes for this user and insert fresh from server
                    // This ensures we're in sync with the server
                    serverResumes.forEach { apiResume ->
                        println("IosSyncingResumeRepository: Processing resume '${apiResume.name}' (id: ${apiResume.id})")
                        val localResume = queries.selectResumeById(apiResume.id).executeAsOneOrNull()
                        val serverTimestamp = parseInstant(apiResume.updatedAt).toEpochMilliseconds()
                        
                        if (localResume == null) {
                            // Server has resume we don't have - insert it
                            println("IosSyncingResumeRepository: Inserting new resume: ${apiResume.name}")
                            queries.insertResume(
                                id = apiResume.id,
                                userId = user.id,
                                name = apiResume.name,
                                content = apiResume.content,
                                industry = apiResume.industry,
                                createdAt = parseInstant(apiResume.createdAt).toEpochMilliseconds(),
                                updatedAt = serverTimestamp
                            )
                            println("IosSyncingResumeRepository: Successfully inserted: ${apiResume.name}")
                        } else {
                            // Update existing resume
                            println("IosSyncingResumeRepository: Updating existing resume: ${apiResume.name}")
                            queries.updateResume(
                                name = apiResume.name,
                                content = apiResume.content,
                                industry = apiResume.industry,
                                updatedAt = serverTimestamp,
                                id = apiResume.id
                            )
                            // Also update userId in case it was null
                            queries.updateResumeUserId(userId = user.id, id = apiResume.id)
                        }
                    }
                    
                    // Log final count
                    val finalCount = queries.selectAllResumes().executeAsList().size
                    println("IosSyncingResumeRepository: Sync complete. Total resumes in DB: $finalCount")
                    
                    // Push local-only resumes to server (only if logged in)
                    val localResumes = queries.selectResumesByUserId(user.id).executeAsList()
                    val serverIds = serverResumes.map { it.id }.toSet()
                    
                    for (local in localResumes) {
                        if (local.id !in serverIds) {
                            // Local resume not on server - push it
                            pushResumeToServer(local.toResumeDomain(), user.id)
                        }
                    }
                } else {
                    println("IosSyncingResumeRepository: API request failed: ${response.status}")
                    val errorBody = response.bodyAsText()
                    println("IosSyncingResumeRepository: Error body: $errorBody")
                }
            } catch (e: Exception) {
                // Sync failed - continue with local data
                println("IosSyncingResumeRepository: Sync failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun syncResumeToServer(resume: Resume) {
        try {
            val user = authRepository.getCurrentUser() ?: return
            pushResumeToServer(resume, user.id)
        } catch (e: Exception) {
            println("Error syncing resume: ${e.message}")
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
            println("Pushed resume to server: ${resume.name}")
        } catch (e: Exception) {
            println("Error pushing resume: ${e.message}")
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
            println("Updated resume on server: ${resume.name}")
        } catch (e: Exception) {
            println("Error updating resume on server: ${e.message}")
        }
    }
    
    private suspend fun deleteResumeOnServer(id: String) {
        try {
            val user = authRepository.getCurrentUser() ?: return
            
            httpClient.delete("$apiBaseUrl/api/v1/resumes/$id") {
                header("X-User-Id", user.id)
            }
            println("Deleted resume from server: $id")
        } catch (e: Exception) {
            println("Error deleting resume on server: ${e.message}")
        }
    }
}

// API DTOs
@Serializable
internal data class ResumeApiResponse(
    val id: String,
    val userId: String?,
    val name: String,
    val content: String,
    val industry: String? = null,
    val sourceType: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
internal data class ResumeApiRequest(
    val name: String,
    val content: String,
    val industry: String?,
    val sourceType: String
)

// Extension to convert database entity to domain model
private fun com.vwatek.apply.db.Resume.toResumeDomain(): Resume {
    return Resume(
        id = this.id,
        userId = this.userId,
        name = this.name,
        content = this.content,
        industry = this.industry,
        sourceType = ResumeSourceType.MANUAL,
        fileName = null,
        fileType = null,
        originalFileData = null,
        createdAt = Instant.fromEpochMilliseconds(this.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(this.updatedAt),
        currentVersionId = null
    )
}

// Helper to parse ISO instant strings
internal fun parseInstant(isoString: String): Instant {
    return try {
        Instant.parse(isoString)
    } catch (e: Exception) {
        Clock.System.now()
    }
}
