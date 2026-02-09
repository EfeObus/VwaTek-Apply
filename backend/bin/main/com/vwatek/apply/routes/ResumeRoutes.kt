package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.ResumesTable
import com.vwatek.apply.db.tables.ResumeAnalysesTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class ResumeRequest(
    val name: String,
    val content: String,
    val industry: String? = null,
    val sourceType: String = "MANUAL"
)

@Serializable
data class ResumeResponse(
    val id: String,
    val name: String,
    val content: String,
    val industry: String?,
    val sourceType: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class AnalysisRequest(
    val resumeId: String,
    val jobDescription: String,
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>
)

@Serializable
data class AnalysisResponse(
    val id: String,
    val resumeId: String,
    val jobDescription: String,
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>,
    val createdAt: String
)

fun Route.resumeRoutes() {
    route("/resumes") {
        // Get all resumes
        get {
            val userId = call.request.headers["X-User-Id"]
            
            val resumes = transaction {
                val query = if (userId != null) {
                    ResumesTable.select { ResumesTable.userId eq userId }
                } else {
                    ResumesTable.selectAll()
                }
                query.orderBy(ResumesTable.updatedAt, SortOrder.DESC)
                    .map { row ->
                        ResumeResponse(
                            id = row[ResumesTable.id],
                            name = row[ResumesTable.name],
                            content = row[ResumesTable.content],
                            industry = row[ResumesTable.industry],
                            sourceType = row[ResumesTable.sourceType],
                            createdAt = row[ResumesTable.createdAt].toString(),
                            updatedAt = row[ResumesTable.updatedAt].toString()
                        )
                    }
            }
            
            call.respond(resumes)
        }
        
        // Get single resume
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing resume ID")
            
            val resume = transaction {
                ResumesTable.select { ResumesTable.id eq id }.firstOrNull()
            }
            
            if (resume == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Resume not found"))
                return@get
            }
            
            call.respond(ResumeResponse(
                id = resume[ResumesTable.id],
                name = resume[ResumesTable.name],
                content = resume[ResumesTable.content],
                industry = resume[ResumesTable.industry],
                sourceType = resume[ResumesTable.sourceType],
                createdAt = resume[ResumesTable.createdAt].toString(),
                updatedAt = resume[ResumesTable.updatedAt].toString()
            ))
        }
        
        // Create resume
        post {
            val request = call.receive<ResumeRequest>()
            val userId = call.request.headers["X-User-Id"]
            
            val resumeId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            transaction {
                ResumesTable.insert {
                    it[id] = resumeId
                    it[ResumesTable.userId] = userId
                    it[name] = request.name
                    it[content] = request.content
                    it[industry] = request.industry
                    it[sourceType] = request.sourceType
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            
            call.respond(HttpStatusCode.Created, ResumeResponse(
                id = resumeId,
                name = request.name,
                content = request.content,
                industry = request.industry,
                sourceType = request.sourceType,
                createdAt = now.toString(),
                updatedAt = now.toString()
            ))
        }
        
        // Update resume
        put("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing resume ID")
            val request = call.receive<ResumeRequest>()
            val now = Clock.System.now()
            
            val updated = transaction {
                ResumesTable.update({ ResumesTable.id eq id }) {
                    it[name] = request.name
                    it[content] = request.content
                    it[industry] = request.industry
                    it[sourceType] = request.sourceType
                    it[updatedAt] = now
                }
            }
            
            if (updated == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Resume not found"))
                return@put
            }
            
            call.respond(ResumeResponse(
                id = id,
                name = request.name,
                content = request.content,
                industry = request.industry,
                sourceType = request.sourceType,
                createdAt = now.toString(), // We don't have original createdAt here
                updatedAt = now.toString()
            ))
        }
        
        // Delete resume
        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing resume ID")
            
            val deleted = transaction {
                ResumesTable.deleteWhere { ResumesTable.id eq id }
            }
            
            if (deleted == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Resume not found"))
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
        
        // Get analyses for a resume
        get("/{id}/analyses") {
            val resumeId = call.parameters["id"] ?: throw IllegalArgumentException("Missing resume ID")
            
            val analyses = transaction {
                ResumeAnalysesTable.select { ResumeAnalysesTable.resumeId eq resumeId }
                    .orderBy(ResumeAnalysesTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        AnalysisResponse(
                            id = row[ResumeAnalysesTable.id],
                            resumeId = row[ResumeAnalysesTable.resumeId],
                            jobDescription = row[ResumeAnalysesTable.jobDescription],
                            matchScore = row[ResumeAnalysesTable.matchScore],
                            missingKeywords = row[ResumeAnalysesTable.missingKeywords].split(",").filter { it.isNotBlank() },
                            recommendations = row[ResumeAnalysesTable.recommendations].split("|||").filter { it.isNotBlank() },
                            createdAt = row[ResumeAnalysesTable.createdAt].toString()
                        )
                    }
            }
            
            call.respond(analyses)
        }
    }
    
    // Analysis routes
    route("/analyses") {
        post {
            val request = call.receive<AnalysisRequest>()
            val analysisId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            transaction {
                ResumeAnalysesTable.insert {
                    it[id] = analysisId
                    it[resumeId] = request.resumeId
                    it[jobDescription] = request.jobDescription
                    it[matchScore] = request.matchScore
                    it[missingKeywords] = request.missingKeywords.joinToString(",")
                    it[recommendations] = request.recommendations.joinToString("|||")
                    it[createdAt] = now
                }
            }
            
            call.respond(HttpStatusCode.Created, AnalysisResponse(
                id = analysisId,
                resumeId = request.resumeId,
                jobDescription = request.jobDescription,
                matchScore = request.matchScore,
                missingKeywords = request.missingKeywords,
                recommendations = request.recommendations,
                createdAt = now.toString()
            ))
        }
    }
}
