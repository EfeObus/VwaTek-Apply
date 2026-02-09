package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.CoverLettersTable
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
data class CoverLetterRequest(
    val resumeId: String? = null,
    val jobTitle: String,
    val companyName: String,
    val content: String,
    val tone: String = "PROFESSIONAL"
)

@Serializable
data class CoverLetterResponse(
    val id: String,
    val resumeId: String?,
    val jobTitle: String,
    val companyName: String,
    val content: String,
    val tone: String,
    val createdAt: String
)

fun Route.coverLetterRoutes() {
    route("/cover-letters") {
        // Get all cover letters
        get {
            val userId = call.request.headers["X-User-Id"]
            
            val coverLetters = transaction {
                val query = if (userId != null) {
                    CoverLettersTable.select { CoverLettersTable.userId eq userId }
                } else {
                    CoverLettersTable.selectAll()
                }
                query.orderBy(CoverLettersTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        CoverLetterResponse(
                            id = row[CoverLettersTable.id],
                            resumeId = row[CoverLettersTable.resumeId],
                            jobTitle = row[CoverLettersTable.jobTitle],
                            companyName = row[CoverLettersTable.companyName],
                            content = row[CoverLettersTable.content],
                            tone = row[CoverLettersTable.tone],
                            createdAt = row[CoverLettersTable.createdAt].toString()
                        )
                    }
            }
            
            call.respond(coverLetters)
        }
        
        // Get single cover letter
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing cover letter ID")
            
            val coverLetter = transaction {
                CoverLettersTable.select { CoverLettersTable.id eq id }.firstOrNull()
            }
            
            if (coverLetter == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Cover letter not found"))
                return@get
            }
            
            call.respond(CoverLetterResponse(
                id = coverLetter[CoverLettersTable.id],
                resumeId = coverLetter[CoverLettersTable.resumeId],
                jobTitle = coverLetter[CoverLettersTable.jobTitle],
                companyName = coverLetter[CoverLettersTable.companyName],
                content = coverLetter[CoverLettersTable.content],
                tone = coverLetter[CoverLettersTable.tone],
                createdAt = coverLetter[CoverLettersTable.createdAt].toString()
            ))
        }
        
        // Create cover letter
        post {
            val request = call.receive<CoverLetterRequest>()
            val userId = call.request.headers["X-User-Id"]
            
            val coverLetterId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            transaction {
                CoverLettersTable.insert {
                    it[id] = coverLetterId
                    it[CoverLettersTable.userId] = userId
                    it[resumeId] = request.resumeId
                    it[jobTitle] = request.jobTitle
                    it[companyName] = request.companyName
                    it[content] = request.content
                    it[tone] = request.tone
                    it[createdAt] = now
                }
            }
            
            call.respond(HttpStatusCode.Created, CoverLetterResponse(
                id = coverLetterId,
                resumeId = request.resumeId,
                jobTitle = request.jobTitle,
                companyName = request.companyName,
                content = request.content,
                tone = request.tone,
                createdAt = now.toString()
            ))
        }
        
        // Delete cover letter
        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing cover letter ID")
            
            val deleted = transaction {
                CoverLettersTable.deleteWhere { CoverLettersTable.id eq id }
            }
            
            if (deleted == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Cover letter not found"))
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
