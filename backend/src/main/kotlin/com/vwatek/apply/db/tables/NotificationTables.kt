package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.Table

/**
 * Phase 2: Notification Database Tables
 * Stores push notifications, preferences, and device tokens
 */

/**
 * Stores notification records
 */
object NotificationsTable : Table("notifications") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val type = varchar("type", 50) // NotificationType enum
    val title = varchar("title", 255)
    val body = text("body")
    val data = text("data").nullable() // JSON data
    val priority = varchar("priority", 20).default("DEFAULT")
    val relatedEntityId = varchar("related_entity_id", 36).nullable() // e.g., job application ID
    val relatedEntityType = varchar("related_entity_type", 50).nullable() // e.g., "JOB_APPLICATION"
    val scheduledAt = varchar("scheduled_at", 30).nullable()
    val sentAt = varchar("sent_at", 30).nullable()
    val readAt = varchar("read_at", 30).nullable()
    val createdAt = varchar("created_at", 30)
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, userId, readAt)
        index(false, type)
        index(false, scheduledAt)
    }
}

/**
 * Stores user notification preferences
 */
object NotificationPreferencesTable : Table("notification_preferences") {
    val userId = varchar("user_id", 36)
    
    // Channel preferences
    val enablePush = bool("enable_push").default(true)
    val enableEmail = bool("enable_email").default(true)
    val enableInApp = bool("enable_in_app").default(true)
    
    // Notification type preferences
    val applicationReminders = bool("application_reminders").default(true)
    val interviewReminders = bool("interview_reminders").default(true)
    val followUpReminders = bool("follow_up_reminders").default(true)
    val statusChanges = bool("status_changes").default(true)
    val deadlineAlerts = bool("deadline_alerts").default(true)
    val practiceReminders = bool("practice_reminders").default(true)
    val weeklySummary = bool("weekly_summary").default(true)
    val tipsAndAdvice = bool("tips_and_advice").default(false)
    val featureUpdates = bool("feature_updates").default(true)
    
    // Quiet hours
    val quietHoursEnabled = bool("quiet_hours_enabled").default(false)
    val quietHoursStart = varchar("quiet_hours_start", 10).nullable() // "22:00"
    val quietHoursEnd = varchar("quiet_hours_end", 10).nullable()     // "08:00"
    
    // Timing settings
    val interviewReminderHours = integer("interview_reminder_hours").default(24)
    val followUpReminderDays = integer("follow_up_reminder_days").default(7)
    
    val createdAt = varchar("created_at", 30)
    val updatedAt = varchar("updated_at", 30)
    
    override val primaryKey = PrimaryKey(userId)
}

/**
 * Stores device tokens for push notifications
 */
object DeviceTokensTable : Table("device_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val token = text("token") // FCM/APNs token
    val platform = varchar("platform", 20) // ANDROID, IOS, WEB
    val deviceName = varchar("device_name", 100).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = varchar("created_at", 30)
    val lastUsedAt = varchar("last_used_at", 30)
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(true, token) // Unique token
        index(false, userId, isActive)
    }
}

/**
 * Stores scheduled notification jobs
 */
object ScheduledNotificationsTable : Table("scheduled_notifications") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val notificationType = varchar("notification_type", 50)
    val title = varchar("title", 255)
    val body = text("body")
    val data = text("data").nullable()
    val scheduledFor = varchar("scheduled_for", 30)
    val relatedEntityId = varchar("related_entity_id", 36).nullable()
    val relatedEntityType = varchar("related_entity_type", 50).nullable()
    val status = varchar("status", 20).default("PENDING") // PENDING, SENT, CANCELLED
    val sentAt = varchar("sent_at", 30).nullable()
    val createdAt = varchar("created_at", 30)
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, status, scheduledFor)
        index(false, userId)
        index(false, relatedEntityId, relatedEntityType)
    }
}

/**
 * Email notification queue
 */
object EmailQueueTable : Table("email_queue") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val toEmail = varchar("to_email", 255)
    val subject = varchar("subject", 255)
    val body = text("body")
    val templateId = varchar("template_id", 50).nullable()
    val templateData = text("template_data").nullable() // JSON
    val status = varchar("status", 20).default("PENDING") // PENDING, SENT, FAILED
    val attempts = integer("attempts").default(0)
    val lastAttemptAt = varchar("last_attempt_at", 30).nullable()
    val sentAt = varchar("sent_at", 30).nullable()
    val errorMessage = text("error_message").nullable()
    val createdAt = varchar("created_at", 30)
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, status)
        index(false, userId)
    }
}
