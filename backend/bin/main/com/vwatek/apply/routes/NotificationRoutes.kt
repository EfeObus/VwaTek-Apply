package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * Phase 2: Notification API Routes
 * Handles push notifications, preferences, and device tokens
 */
fun Route.notificationRoutes() {
    route("/api/v1/notifications") {
        authenticate("jwt") {
            // Get user's notifications
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val unreadOnly = call.request.queryParameters["unreadOnly"]?.toBoolean() ?: false
                val type = call.request.queryParameters["type"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                
                val (notifications, total) = transaction {
                    var query = NotificationsTable.select { NotificationsTable.userId eq userId }
                    
                    if (unreadOnly) {
                        query = query.andWhere { NotificationsTable.readAt.isNull() }
                    }
                    
                    if (type != null) {
                        query = query.andWhere { NotificationsTable.type eq type }
                    }
                    
                    val totalCount = query.count().toInt()
                    
                    val results = query
                        .orderBy(NotificationsTable.createdAt, SortOrder.DESC)
                        .limit(limit, offset.toLong())
                        .map { it.toNotificationDto() }
                    
                    Pair(results, totalCount)
                }
                
                call.respond(NotificationsListDto(
                    notifications = notifications,
                    total = total,
                    unread = notifications.count { it.readAt == null },
                    limit = limit,
                    offset = offset
                ))
            }
            
            // Mark notification as read
            patch("/{id}/read") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                
                val notificationId = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing notification ID")
                
                val updated = transaction {
                    NotificationsTable.update({
                        NotificationsTable.id eq notificationId and (NotificationsTable.userId eq userId)
                    }) {
                        it[readAt] = Instant.now().toString()
                    }
                }
                
                if (updated > 0) {
                    call.respond(mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Notification not found")
                }
            }
            
            // Mark all as read
            post("/read-all") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val updated = transaction {
                    NotificationsTable.update({
                        NotificationsTable.userId eq userId and NotificationsTable.readAt.isNull()
                    }) {
                        it[readAt] = Instant.now().toString()
                    }
                }
                
                call.respond(mapOf("success" to true, "count" to updated))
            }
            
            // Delete notification
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                
                val notificationId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing notification ID")
                
                val deleted = transaction {
                    NotificationsTable.deleteWhere {
                        NotificationsTable.id eq notificationId and (NotificationsTable.userId eq userId)
                    }
                }
                
                if (deleted > 0) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Notification not found")
                }
            }
            
            // Get notification stats
            get("/stats") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val stats = transaction {
                    val notifications = NotificationsTable.select { NotificationsTable.userId eq userId }
                    val total = notifications.count().toInt()
                    val unread = notifications.andWhere { NotificationsTable.readAt.isNull() }.count().toInt()
                    
                    val byType = NotificationsTable
                        .slice(NotificationsTable.type, NotificationsTable.id.count())
                        .select { NotificationsTable.userId eq userId }
                        .groupBy(NotificationsTable.type)
                        .associate { it[NotificationsTable.type] to it[NotificationsTable.id.count()].toInt() }
                    
                    NotificationStatsDto(total, unread, byType)
                }
                
                call.respond(stats)
            }
        }
    }
    
    // Notification preferences
    route("/api/v1/notifications/preferences") {
        authenticate("jwt") {
            // Get preferences
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val preferences = transaction {
                    NotificationPreferencesTable.select { NotificationPreferencesTable.userId eq userId }
                        .firstOrNull()
                        ?.toPreferencesDto()
                        ?: createDefaultPreferences(userId)
                }
                
                call.respond(preferences)
            }
            
            // Update preferences
            put {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                
                val request = call.receive<UpdatePreferencesDto>()
                
                transaction {
                    val exists = NotificationPreferencesTable.select { NotificationPreferencesTable.userId eq userId }
                        .count() > 0
                    
                    if (exists) {
                        NotificationPreferencesTable.update({ NotificationPreferencesTable.userId eq userId }) {
                            request.enablePush?.let { v -> it[enablePush] = v }
                            request.enableEmail?.let { v -> it[enableEmail] = v }
                            request.enableInApp?.let { v -> it[enableInApp] = v }
                            request.applicationReminders?.let { v -> it[applicationReminders] = v }
                            request.interviewReminders?.let { v -> it[interviewReminders] = v }
                            request.followUpReminders?.let { v -> it[followUpReminders] = v }
                            request.statusChanges?.let { v -> it[statusChanges] = v }
                            request.deadlineAlerts?.let { v -> it[deadlineAlerts] = v }
                            request.practiceReminders?.let { v -> it[practiceReminders] = v }
                            request.weeklySummary?.let { v -> it[weeklySummary] = v }
                            request.tipsAndAdvice?.let { v -> it[tipsAndAdvice] = v }
                            request.featureUpdates?.let { v -> it[featureUpdates] = v }
                            request.quietHoursEnabled?.let { v -> it[quietHoursEnabled] = v }
                            request.quietHoursStart?.let { v -> it[quietHoursStart] = v }
                            request.quietHoursEnd?.let { v -> it[quietHoursEnd] = v }
                            request.interviewReminderHours?.let { v -> it[interviewReminderHours] = v }
                            request.followUpReminderDays?.let { v -> it[followUpReminderDays] = v }
                            it[updatedAt] = Instant.now().toString()
                        }
                    } else {
                        NotificationPreferencesTable.insert {
                            it[NotificationPreferencesTable.userId] = userId
                            it[enablePush] = request.enablePush ?: true
                            it[enableEmail] = request.enableEmail ?: true
                            it[enableInApp] = request.enableInApp ?: true
                            it[applicationReminders] = request.applicationReminders ?: true
                            it[interviewReminders] = request.interviewReminders ?: true
                            it[followUpReminders] = request.followUpReminders ?: true
                            it[statusChanges] = request.statusChanges ?: true
                            it[deadlineAlerts] = request.deadlineAlerts ?: true
                            it[practiceReminders] = request.practiceReminders ?: true
                            it[weeklySummary] = request.weeklySummary ?: true
                            it[tipsAndAdvice] = request.tipsAndAdvice ?: false
                            it[featureUpdates] = request.featureUpdates ?: true
                            it[quietHoursEnabled] = request.quietHoursEnabled ?: false
                            it[quietHoursStart] = request.quietHoursStart
                            it[quietHoursEnd] = request.quietHoursEnd
                            it[interviewReminderHours] = request.interviewReminderHours ?: 24
                            it[followUpReminderDays] = request.followUpReminderDays ?: 7
                            it[createdAt] = Instant.now().toString()
                            it[updatedAt] = Instant.now().toString()
                        }
                    }
                }
                
                call.respond(mapOf("success" to true))
            }
        }
    }
    
    // Device tokens
    route("/api/v1/notifications/devices") {
        authenticate("jwt") {
            // Register device token
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val request = call.receive<RegisterDeviceDto>()
                
                val deviceId = transaction {
                    // Check if token already exists
                    val existing = DeviceTokensTable.select { DeviceTokensTable.token eq request.token }
                        .firstOrNull()
                    
                    if (existing != null) {
                        // Update existing
                        DeviceTokensTable.update({ DeviceTokensTable.token eq request.token }) {
                            it[DeviceTokensTable.userId] = userId
                            it[isActive] = true
                            it[lastUsedAt] = Instant.now().toString()
                            request.deviceName?.let { name -> it[deviceName] = name }
                        }
                        existing[DeviceTokensTable.id]
                    } else {
                        // Create new
                        val id = UUID.randomUUID().toString()
                        DeviceTokensTable.insert {
                            it[DeviceTokensTable.id] = id
                            it[DeviceTokensTable.userId] = userId
                            it[token] = request.token
                            it[platform] = request.platform
                            it[deviceName] = request.deviceName
                            it[isActive] = true
                            it[createdAt] = Instant.now().toString()
                            it[lastUsedAt] = Instant.now().toString()
                        }
                        id
                    }
                }
                
                call.respond(HttpStatusCode.Created, mapOf("id" to deviceId))
            }
            
            // Get user's devices
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val devices = transaction {
                    DeviceTokensTable.select { 
                        DeviceTokensTable.userId eq userId and (DeviceTokensTable.isActive eq true)
                    }
                        .map { it.toDeviceDto() }
                }
                
                call.respond(devices)
            }
            
            // Deactivate device
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                
                val deviceId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing device ID")
                
                val updated = transaction {
                    DeviceTokensTable.update({
                        DeviceTokensTable.id eq deviceId and (DeviceTokensTable.userId eq userId)
                    }) {
                        it[isActive] = false
                    }
                }
                
                if (updated > 0) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Device not found")
                }
            }
        }
    }
}

// ===== Helper Functions =====

private fun createDefaultPreferences(userId: String): NotificationPreferencesDto {
    val now = Instant.now().toString()
    transaction {
        NotificationPreferencesTable.insert {
            it[NotificationPreferencesTable.userId] = userId
            it[enablePush] = true
            it[enableEmail] = true
            it[enableInApp] = true
            it[applicationReminders] = true
            it[interviewReminders] = true
            it[followUpReminders] = true
            it[statusChanges] = true
            it[deadlineAlerts] = true
            it[practiceReminders] = true
            it[weeklySummary] = true
            it[tipsAndAdvice] = false
            it[featureUpdates] = true
            it[quietHoursEnabled] = false
            it[interviewReminderHours] = 24
            it[followUpReminderDays] = 7
            it[createdAt] = now
            it[updatedAt] = now
        }
    }
    return NotificationPreferencesDto(
        enablePush = true,
        enableEmail = true,
        enableInApp = true,
        applicationReminders = true,
        interviewReminders = true,
        followUpReminders = true,
        statusChanges = true,
        deadlineAlerts = true,
        practiceReminders = true,
        weeklySummary = true,
        tipsAndAdvice = false,
        featureUpdates = true,
        quietHoursEnabled = false,
        quietHoursStart = null,
        quietHoursEnd = null,
        interviewReminderHours = 24,
        followUpReminderDays = 7
    )
}

// ===== Row Mappers =====

private fun ResultRow.toNotificationDto() = NotificationDto(
    id = this[NotificationsTable.id],
    type = this[NotificationsTable.type],
    title = this[NotificationsTable.title],
    body = this[NotificationsTable.body],
    priority = this[NotificationsTable.priority],
    scheduledAt = this[NotificationsTable.scheduledAt],
    sentAt = this[NotificationsTable.sentAt],
    readAt = this[NotificationsTable.readAt],
    createdAt = this[NotificationsTable.createdAt]
)

private fun ResultRow.toPreferencesDto() = NotificationPreferencesDto(
    enablePush = this[NotificationPreferencesTable.enablePush],
    enableEmail = this[NotificationPreferencesTable.enableEmail],
    enableInApp = this[NotificationPreferencesTable.enableInApp],
    applicationReminders = this[NotificationPreferencesTable.applicationReminders],
    interviewReminders = this[NotificationPreferencesTable.interviewReminders],
    followUpReminders = this[NotificationPreferencesTable.followUpReminders],
    statusChanges = this[NotificationPreferencesTable.statusChanges],
    deadlineAlerts = this[NotificationPreferencesTable.deadlineAlerts],
    practiceReminders = this[NotificationPreferencesTable.practiceReminders],
    weeklySummary = this[NotificationPreferencesTable.weeklySummary],
    tipsAndAdvice = this[NotificationPreferencesTable.tipsAndAdvice],
    featureUpdates = this[NotificationPreferencesTable.featureUpdates],
    quietHoursEnabled = this[NotificationPreferencesTable.quietHoursEnabled],
    quietHoursStart = this[NotificationPreferencesTable.quietHoursStart],
    quietHoursEnd = this[NotificationPreferencesTable.quietHoursEnd],
    interviewReminderHours = this[NotificationPreferencesTable.interviewReminderHours],
    followUpReminderDays = this[NotificationPreferencesTable.followUpReminderDays]
)

private fun ResultRow.toDeviceDto() = DeviceDto(
    id = this[DeviceTokensTable.id],
    platform = this[DeviceTokensTable.platform],
    deviceName = this[DeviceTokensTable.deviceName],
    lastUsedAt = this[DeviceTokensTable.lastUsedAt]
)

// ===== DTOs =====

@Serializable
data class NotificationsListDto(
    val notifications: List<NotificationDto>,
    val total: Int,
    val unread: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val priority: String = "DEFAULT",
    val scheduledAt: String? = null,
    val sentAt: String? = null,
    val readAt: String? = null,
    val createdAt: String
)

@Serializable
data class NotificationStatsDto(
    val total: Int,
    val unread: Int,
    val byType: Map<String, Int>
)

@Serializable
data class NotificationPreferencesDto(
    val enablePush: Boolean,
    val enableEmail: Boolean,
    val enableInApp: Boolean,
    val applicationReminders: Boolean,
    val interviewReminders: Boolean,
    val followUpReminders: Boolean,
    val statusChanges: Boolean,
    val deadlineAlerts: Boolean,
    val practiceReminders: Boolean,
    val weeklySummary: Boolean,
    val tipsAndAdvice: Boolean,
    val featureUpdates: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: String?,
    val quietHoursEnd: String?,
    val interviewReminderHours: Int,
    val followUpReminderDays: Int
)

@Serializable
data class UpdatePreferencesDto(
    val enablePush: Boolean? = null,
    val enableEmail: Boolean? = null,
    val enableInApp: Boolean? = null,
    val applicationReminders: Boolean? = null,
    val interviewReminders: Boolean? = null,
    val followUpReminders: Boolean? = null,
    val statusChanges: Boolean? = null,
    val deadlineAlerts: Boolean? = null,
    val practiceReminders: Boolean? = null,
    val weeklySummary: Boolean? = null,
    val tipsAndAdvice: Boolean? = null,
    val featureUpdates: Boolean? = null,
    val quietHoursEnabled: Boolean? = null,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val interviewReminderHours: Int? = null,
    val followUpReminderDays: Int? = null
)

@Serializable
data class RegisterDeviceDto(
    val token: String,
    val platform: String,
    val deviceName: String? = null
)

@Serializable
data class DeviceDto(
    val id: String,
    val platform: String,
    val deviceName: String?,
    val lastUsedAt: String
)
