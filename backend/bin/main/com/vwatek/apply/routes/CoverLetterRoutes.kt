package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.CoverLettersTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.vwatek.apply.auth.requireUserId
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
    authenticate("jwt") {
    route("/cover-letters") {
        // Get all cover letters
        get {
            val userId = call.requireUserId() ?: return@get
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 100)
            val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
            
            val coverLetters = transaction {
                CoverLettersTable.select { CoverLettersTable.userId eq userId }
                    .orderBy(CoverLettersTable.createdAt, SortOrder.DESC)
                    .limit(limit, offset.toLong())
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
            val userId = call.requireUserId() ?: return@post
            
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
    } // authenticate("jwt")
}
