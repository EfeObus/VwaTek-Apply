package com.vwatek.apply.domain.model

import kotlinx.serialization.Serializable

/**
 * Phase 2: Push Notification Models
 * Supports job tracker reminders, interview notifications, and application updates
 */

@Serializable
data class PushNotification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val scheduledAt: String? = null,
    val sentAt: String? = null,
    val readAt: String? = null,
    val createdAt: String
)

@Serializable
enum class NotificationType {
    // Job Tracker
    APPLICATION_REMINDER,
    INTERVIEW_REMINDER,
    FOLLOW_UP_REMINDER,
    STATUS_CHANGE,
    DEADLINE_APPROACHING,
    
    // Interview Prep
    PRACTICE_REMINDER,
    SESSION_SCHEDULED,
    
    // Resume/Cover Letter
    DOCUMENT_READY,
    OPTIMIZATION_COMPLETE,
    
    // General
    WEEKLY_SUMMARY,
    TIPS_AND_ADVICE,
    FEATURE_UPDATE;
    
    val displayName: String
        get() = when (this) {
            APPLICATION_REMINDER -> "Application Reminder"
            INTERVIEW_REMINDER -> "Interview Reminder"
            FOLLOW_UP_REMINDER -> "Follow-up Reminder"
            STATUS_CHANGE -> "Status Change"
            DEADLINE_APPROACHING -> "Deadline Approaching"
            PRACTICE_REMINDER -> "Practice Reminder"
            SESSION_SCHEDULED -> "Session Scheduled"
            DOCUMENT_READY -> "Document Ready"
            OPTIMIZATION_COMPLETE -> "Optimization Complete"
            WEEKLY_SUMMARY -> "Weekly Summary"
            TIPS_AND_ADVICE -> "Tips & Advice"
            FEATURE_UPDATE -> "Feature Update"
        }
    
    val icon: String
        get() = when (this) {
            APPLICATION_REMINDER -> "📋"
            INTERVIEW_REMINDER -> "📅"
            FOLLOW_UP_REMINDER -> "📧"
            STATUS_CHANGE -> "🔄"
            DEADLINE_APPROACHING -> "⏰"
            PRACTICE_REMINDER -> "🎯"
            SESSION_SCHEDULED -> "📆"
            DOCUMENT_READY -> "📄"
            OPTIMIZATION_COMPLETE -> "✨"
            WEEKLY_SUMMARY -> "📊"
            TIPS_AND_ADVICE -> "💡"
            FEATURE_UPDATE -> "🆕"
        }
}

@Serializable
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH,
    URGENT
}

@Serializable
data class NotificationPreferences(
    val userId: String,
    val enablePush: Boolean = true,
    val enableEmail: Boolean = true,
    val enableInApp: Boolean = true,
    
    // Notification types
    val applicationReminders: Boolean = true,
    val interviewReminders: Boolean = true,
    val followUpReminders: Boolean = true,
    val statusChanges: Boolean = true,
    val deadlineAlerts: Boolean = true,
    val practiceReminders: Boolean = true,
    val weeklySummary: Boolean = true,
    val tipsAndAdvice: Boolean = false,
    val featureUpdates: Boolean = true,
    
    // Quiet hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String? = null, // "22:00"
    val quietHoursEnd: String? = null,   // "08:00"
    
    // Reminder timing
    val interviewReminderHours: Int = 24, // Hours before interview
    val followUpReminderDays: Int = 7     // Days after application
)

@Serializable
data class DeviceToken(
    val id: String,
    val userId: String,
    val token: String,
    val platform: DevicePlatform,
    val deviceName: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val lastUsedAt: String
)

@Serializable
enum class DevicePlatform {
    ANDROID,
    IOS,
    WEB;
    
    val displayName: String
        get() = when (this) {
            ANDROID -> "Android"
            IOS -> "iOS"
            WEB -> "Web"
        }
}

@Serializable
data class NotificationStats(
    val total: Int,
    val unread: Int,
    val byType: Map<NotificationType, Int>
)
