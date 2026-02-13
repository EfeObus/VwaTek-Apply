package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Subscription Tables for VwaTek Apply
 * Manages user subscriptions, payments, and usage tracking
 */

// Subscriptions Table
object SubscriptionsTable : Table("subscriptions") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val tier = varchar("tier", 20)                     // FREE, PRO, PREMIUM
    val status = varchar("status", 30)                 // ACTIVE, TRIALING, PAST_DUE, etc.
    val billingPeriod = varchar("billing_period", 20)  // MONTHLY, YEARLY
    val paymentProvider = varchar("payment_provider", 20) // STRIPE, APPLE_IAP, GOOGLE_PLAY
    val externalSubscriptionId = varchar("external_subscription_id", 255).nullable()
    val customerId = varchar("customer_id", 255).nullable() // Stripe customer ID
    val currentPeriodStart = timestamp("current_period_start")
    val currentPeriodEnd = timestamp("current_period_end")
    val cancelAtPeriodEnd = bool("cancel_at_period_end").default(false)
    val canceledAt = timestamp("canceled_at").nullable()
    val trialStart = timestamp("trial_start").nullable()
    val trialEnd = timestamp("trial_end").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_subscriptions_user", userId)
        index("idx_subscriptions_status", false, status)
        index("idx_subscriptions_external_id", false, externalSubscriptionId)
    }
}

// Payment History Table
object PaymentsTable : Table("payments") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val subscriptionId = varchar("subscription_id", 36).references(SubscriptionsTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val externalPaymentId = varchar("external_payment_id", 255)  // Stripe payment intent ID
    val provider = varchar("provider", 20)             // STRIPE, APPLE_IAP, GOOGLE_PLAY
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("CAD")
    val status = varchar("status", 30)                 // SUCCEEDED, PENDING, FAILED, REFUNDED
    val description = varchar("description", 255).nullable()
    val invoiceId = varchar("invoice_id", 255).nullable()
    val receiptUrl = text("receipt_url").nullable()
    val refundedAmount = decimal("refunded_amount", 10, 2).nullable()
    val refundedAt = timestamp("refunded_at").nullable()
    val refundReason = varchar("refund_reason", 255).nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_payments_user", false, userId)
        index("idx_payments_external_id", false, externalPaymentId)
        index("idx_payments_status", false, status)
    }
}

// Stripe Customers Table (for mapping users to Stripe)
object StripeCustomersTable : Table("stripe_customers") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val stripeCustomerId = varchar("stripe_customer_id", 255).uniqueIndex()
    val email = varchar("email", 255)
    val defaultPaymentMethodId = varchar("default_payment_method_id", 255).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_stripe_customers_user", userId)
    }
}

// Usage Tracking Table
object UsageTrackingTable : Table("usage_tracking") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val periodStart = timestamp("period_start")
    val periodEnd = timestamp("period_end")
    val resumeVersionsUsed = integer("resume_versions_used").default(0)
    val aiEnhancementsUsedToday = integer("ai_enhancements_used_today").default(0)
    val aiEnhancementsResetAt = timestamp("ai_enhancements_reset_at")
    val coverLettersUsed = integer("cover_letters_used").default(0)
    val interviewSessionsUsed = integer("interview_sessions_used").default(0)
    val salaryInsightsUsed = integer("salary_insights_used").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_usage_user_period", false, userId, periodStart)
    }
}

// Subscription Events Table (for audit trail)
object SubscriptionEventsTable : Table("subscription_events") {
    val id = varchar("id", 36)
    val subscriptionId = varchar("subscription_id", 36).references(SubscriptionsTable.id, onDelete = ReferenceOption.CASCADE)
    val eventType = varchar("event_type", 50)          // CREATED, UPGRADED, DOWNGRADED, CANCELED, etc.
    val previousTier = varchar("previous_tier", 20).nullable()
    val newTier = varchar("new_tier", 20).nullable()
    val eventData = text("event_data").nullable()      // JSON for additional data
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_subscription_events", false, subscriptionId)
    }
}

// Stripe Webhook Events Table (for idempotency)
object StripeWebhookEventsTable : Table("stripe_webhook_events") {
    val id = varchar("id", 36)
    val stripeEventId = varchar("stripe_event_id", 255).uniqueIndex()
    val eventType = varchar("event_type", 100)
    val processedAt = timestamp("processed_at")
    val status = varchar("status", 20)                 // PROCESSED, FAILED
    val errorMessage = text("error_message").nullable()
    val rawPayload = text("raw_payload")
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Price Configuration Table (for dynamic pricing)
object PriceConfigurationTable : Table("price_configuration") {
    val id = varchar("id", 36)
    val tier = varchar("tier", 20)                     // PRO, PREMIUM
    val billingPeriod = varchar("billing_period", 20)  // MONTHLY, YEARLY
    val currency = varchar("currency", 3)
    val amount = decimal("amount", 10, 2)
    val stripePriceId = varchar("stripe_price_id", 255)
    val appleProductId = varchar("apple_product_id", 255).nullable()
    val googleProductId = varchar("google_product_id", 255).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_price_config_unique", tier, billingPeriod, currency)
    }
}

// Promotions/Coupons Table
object PromotionsTable : Table("promotions") {
    val id = varchar("id", 36)
    val code = varchar("code", 50).uniqueIndex()
    val description = varchar("description", 255)
    val discountType = varchar("discount_type", 20)    // PERCENTAGE, FIXED_AMOUNT
    val discountValue = decimal("discount_value", 10, 2)
    val currency = varchar("currency", 3).nullable()   // Only for FIXED_AMOUNT
    val applicableTiers = varchar("applicable_tiers", 100) // Comma-separated: PRO,PREMIUM
    val maxRedemptions = integer("max_redemptions").nullable()
    val currentRedemptions = integer("current_redemptions").default(0)
    val validFrom = timestamp("valid_from")
    val validUntil = timestamp("valid_until").nullable()
    val isActive = bool("is_active").default(true)
    val stripeCouponId = varchar("stripe_coupon_id", 255).nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Promotion Redemptions Table
object PromotionRedemptionsTable : Table("promotion_redemptions") {
    val id = varchar("id", 36)
    val promotionId = varchar("promotion_id", 36).references(PromotionsTable.id)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val subscriptionId = varchar("subscription_id", 36).references(SubscriptionsTable.id).nullable()
    val redeemedAt = timestamp("redeemed_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_promotion_user_unique", promotionId, userId)
    }
}
