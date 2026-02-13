package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.*
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.services.payments.StripeService
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class SubscriptionResponse(
    val subscription: SubscriptionDto,
    val limits: FeatureLimits,
    val pricing: SubscriptionPricing?
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val userId: String,
    val tier: String,
    val status: String,
    val billingPeriod: String,
    val paymentProvider: String,
    val currentPeriodStart: String,
    val currentPeriodEnd: String,
    val cancelAtPeriodEnd: Boolean,
    val canceledAt: String?,
    val trialStart: String?,
    val trialEnd: String?
)

@Serializable
data class CreateCheckoutRequest(
    val tier: String,
    val billingPeriod: String,
    val successUrl: String,
    val cancelUrl: String
)

@Serializable
data class CreatePortalRequest(
    val returnUrl: String
)

@Serializable
data class CheckoutResponse(
    val sessionId: String,
    val checkoutUrl: String
)

@Serializable
data class PortalResponse(
    val sessionId: String,
    val portalUrl: String
)

@Serializable
data class UsageLimitsResponse(
    val tier: String,
    val limits: FeatureLimits,
    val usage: UsageStatsDto
)

@Serializable
data class UsageStatsDto(
    val periodStart: String,
    val periodEnd: String,
    val resumeVersionsUsed: Int,
    val resumeVersionsRemaining: Int,
    val aiEnhancementsUsedToday: Int,
    val aiEnhancementsRemaining: Int,
    val coverLettersUsed: Int,
    val coverLettersRemaining: Int,
    val interviewSessionsUsed: Int,
    val interviewSessionsRemaining: Int
)

@Serializable
data class PricingResponse(
    val pricing: List<TierPricingDto>
)

@Serializable
data class TierPricingDto(
    val tier: String,
    val name: String,
    val description: String,
    val monthlyPriceCad: Double,
    val yearlyPriceCad: Double,
    val monthlyPriceUsd: Double,
    val yearlyPriceUsd: Double,
    val features: List<String>,
    val limits: FeatureLimits
)

@Serializable
data class WebhookResponse(
    val received: Boolean
)

private val json = Json { 
    ignoreUnknownKeys = true 
    isLenient = true
}

fun Route.subscriptionRoutes(httpClient: HttpClient) {
    val stripeService = StripeService(httpClient)
    
    route("/subscriptions") {
        
        // Get current user's subscription
        get {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@get
            }
            
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            
            if (subscription == null) {
                // Return free tier
                val freeLimits = FeatureLimits.forTier(SubscriptionTier.FREE)
                call.respond(
                    SubscriptionResponse(
                        subscription = SubscriptionDto(
                            id = "",
                            userId = userId,
                            tier = "FREE",
                            status = "ACTIVE",
                            billingPeriod = "MONTHLY",
                            paymentProvider = "NONE",
                            currentPeriodStart = Clock.System.now().toString(),
                            currentPeriodEnd = Clock.System.now().toString(),
                            cancelAtPeriodEnd = false,
                            canceledAt = null,
                            trialStart = null,
                            trialEnd = null
                        ),
                        limits = freeLimits,
                        pricing = null
                    )
                )
                return@get
            }
            
            val tier = SubscriptionTier.valueOf(subscription[SubscriptionsTable.tier])
            val limits = FeatureLimits.forTier(tier)
            val pricing = SubscriptionPricing.forTier(tier)
            
            call.respond(
                SubscriptionResponse(
                    subscription = subscription.toDto(),
                    limits = limits,
                    pricing = pricing
                )
            )
        }
        
        // Get pricing tiers
        get("/pricing") {
            val pricing = listOf(
                TierPricingDto(
                    tier = "FREE",
                    name = "Free",
                    description = "Get started with basic features",
                    monthlyPriceCad = 0.0,
                    yearlyPriceCad = 0.0,
                    monthlyPriceUsd = 0.0,
                    yearlyPriceUsd = 0.0,
                    features = listOf(
                        "3 resume versions per month",
                        "2 AI enhancements per day",
                        "5 cover letters per month",
                        "3 interview practice sessions",
                        "Job Bank Canada integration",
                        "NOC code lookup"
                    ),
                    limits = FeatureLimits.forTier(SubscriptionTier.FREE)
                ),
                TierPricingDto(
                    tier = "PRO",
                    name = "Pro",
                    description = "For serious job seekers",
                    monthlyPriceCad = 14.99,
                    yearlyPriceCad = 149.99,
                    monthlyPriceUsd = 11.99,
                    yearlyPriceUsd = 119.99,
                    features = listOf(
                        "15 resume versions per month",
                        "10 AI enhancements per day",
                        "20 cover letters per month",
                        "15 interview practice sessions",
                        "Salary Intelligence insights",
                        "Unlimited application tracking",
                        "Job Bank & NOC integration",
                        "Priority support"
                    ),
                    limits = FeatureLimits.forTier(SubscriptionTier.PRO)
                ),
                TierPricingDto(
                    tier = "PREMIUM",
                    name = "Premium",
                    description = "Everything you need to land your dream job",
                    monthlyPriceCad = 29.99,
                    yearlyPriceCad = 299.99,
                    monthlyPriceUsd = 24.99,
                    yearlyPriceUsd = 249.99,
                    features = listOf(
                        "Unlimited resume versions",
                        "Unlimited AI enhancements",
                        "Unlimited cover letters",
                        "Unlimited interview practice",
                        "Salary Intelligence & insights",
                        "Negotiation Coach AI",
                        "LinkedIn Profile Optimizer",
                        "Unlimited application tracking",
                        "All integrations",
                        "Priority support"
                    ),
                    limits = FeatureLimits.forTier(SubscriptionTier.PREMIUM)
                )
            )
            
            call.respond(PricingResponse(pricing = pricing))
        }
        
        // Create checkout session
        post("/checkout") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            val request = call.receive<CreateCheckoutRequest>()
            
            // Validate tier
            val tier = try {
                SubscriptionTier.valueOf(request.tier)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid subscription tier"))
                return@post
            }
            
            if (tier == SubscriptionTier.FREE) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot checkout for free tier"))
                return@post
            }
            
            // Get or create Stripe customer
            val stripeCustomer = getOrCreateStripeCustomer(userId, stripeService)
            
            // Get price ID
            val pricing = SubscriptionPricing.forTier(tier)
            val priceId = when (request.billingPeriod.uppercase()) {
                "MONTHLY" -> pricing?.stripePriceIdMonthly
                "YEARLY" -> pricing?.stripePriceIdYearly
                else -> null
            }
            
            if (priceId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid billing period"))
                return@post
            }
            
            // Create checkout session
            try {
                val session = stripeService.createCheckoutSession(
                    customerId = stripeCustomer.id,
                    priceId = priceId,
                    successUrl = request.successUrl,
                    cancelUrl = request.cancelUrl,
                    trialPeriodDays = 14  // 14-day free trial
                )
                
                call.respond(CheckoutResponse(
                    sessionId = session.id,
                    checkoutUrl = session.url
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create checkout session: ${e.message}"))
            }
        }
        
        // Create customer portal session
        post("/portal") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            val request = call.receive<CreatePortalRequest>()
            
            // Get Stripe customer
            val stripeCustomer = transaction {
                StripeCustomersTable.select { StripeCustomersTable.userId eq userId }
                    .singleOrNull()
            }
            
            if (stripeCustomer == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No subscription found"))
                return@post
            }
            
            try {
                val session = stripeService.createPortalSession(
                    customerId = stripeCustomer[StripeCustomersTable.stripeCustomerId],
                    returnUrl = request.returnUrl
                )
                
                call.respond(PortalResponse(
                    sessionId = session.id,
                    portalUrl = session.url
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create portal session: ${e.message}"))
            }
        }
        
        // Cancel subscription at period end
        post("/cancel") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            
            if (subscription == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No subscription found"))
                return@post
            }
            
            val externalId = subscription[SubscriptionsTable.externalSubscriptionId]
            if (externalId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot cancel this subscription"))
                return@post
            }
            
            try {
                stripeService.cancelSubscriptionAtPeriodEnd(externalId)
                
                transaction {
                    SubscriptionsTable.update({ SubscriptionsTable.userId eq userId }) {
                        it[cancelAtPeriodEnd] = true
                        it[updatedAt] = Clock.System.now()
                    }
                }
                
                call.respond(mapOf("message" to "Subscription will be canceled at period end"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to cancel subscription: ${e.message}"))
            }
        }
        
        // Reactivate canceled subscription
        post("/reactivate") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            
            if (subscription == null || !subscription[SubscriptionsTable.cancelAtPeriodEnd]) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No canceled subscription found"))
                return@post
            }
            
            val externalId = subscription[SubscriptionsTable.externalSubscriptionId]
            if (externalId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot reactivate this subscription"))
                return@post
            }
            
            try {
                stripeService.reactivateSubscription(externalId)
                
                transaction {
                    SubscriptionsTable.update({ SubscriptionsTable.userId eq userId }) {
                        it[cancelAtPeriodEnd] = false
                        it[canceledAt] = null
                        it[updatedAt] = Clock.System.now()
                    }
                }
                
                call.respond(mapOf("message" to "Subscription reactivated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to reactivate subscription: ${e.message}"))
            }
        }
        
        // Get usage/limits
        get("/usage") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@get
            }
            
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            
            val tier = subscription?.let { SubscriptionTier.valueOf(it[SubscriptionsTable.tier]) }
                ?: SubscriptionTier.FREE
            val limits = FeatureLimits.forTier(tier)
            
            val usage = transaction {
                UsageTrackingTable.select { UsageTrackingTable.userId eq userId }
                    .orderBy(UsageTrackingTable.periodStart, SortOrder.DESC)
                    .firstOrNull()
            }
            
            val now = Clock.System.now()
            val usageDto = if (usage != null) {
                UsageStatsDto(
                    periodStart = usage[UsageTrackingTable.periodStart].toString(),
                    periodEnd = usage[UsageTrackingTable.periodEnd].toString(),
                    resumeVersionsUsed = usage[UsageTrackingTable.resumeVersionsUsed],
                    resumeVersionsRemaining = maxOf(0, limits.resumeVersionsPerMonth - usage[UsageTrackingTable.resumeVersionsUsed]),
                    aiEnhancementsUsedToday = usage[UsageTrackingTable.aiEnhancementsUsedToday],
                    aiEnhancementsRemaining = maxOf(0, limits.aiEnhancementsPerDay - usage[UsageTrackingTable.aiEnhancementsUsedToday]),
                    coverLettersUsed = usage[UsageTrackingTable.coverLettersUsed],
                    coverLettersRemaining = maxOf(0, limits.coverLettersPerMonth - usage[UsageTrackingTable.coverLettersUsed]),
                    interviewSessionsUsed = usage[UsageTrackingTable.interviewSessionsUsed],
                    interviewSessionsRemaining = maxOf(0, limits.interviewSessionsPerMonth - usage[UsageTrackingTable.interviewSessionsUsed])
                )
            } else {
                UsageStatsDto(
                    periodStart = now.toString(),
                    periodEnd = now.toString(),
                    resumeVersionsUsed = 0,
                    resumeVersionsRemaining = limits.resumeVersionsPerMonth,
                    aiEnhancementsUsedToday = 0,
                    aiEnhancementsRemaining = limits.aiEnhancementsPerDay,
                    coverLettersUsed = 0,
                    coverLettersRemaining = limits.coverLettersPerMonth,
                    interviewSessionsUsed = 0,
                    interviewSessionsRemaining = limits.interviewSessionsPerMonth
                )
            }
            
            call.respond(UsageLimitsResponse(
                tier = tier.name,
                limits = limits,
                usage = usageDto
            ))
        }
        
        // Check if feature is available
        get("/feature/{feature}") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@get
            }
            
            val featureName = call.parameters["feature"]
            val feature = try {
                PremiumFeature.valueOf(featureName?.uppercase() ?: "")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid feature name"))
                return@get
            }
            
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            
            val tier = subscription?.let { SubscriptionTier.valueOf(it[SubscriptionsTable.tier]) }
                ?: SubscriptionTier.FREE
            val limits = FeatureLimits.forTier(tier)
            
            val available = when (feature) {
                PremiumFeature.SALARY_INSIGHTS -> limits.salaryInsightsAccess
                PremiumFeature.NEGOTIATION_COACH -> limits.negotiationCoachAccess
                PremiumFeature.LINKEDIN_OPTIMIZER -> limits.linkedInOptimizerAccess
                PremiumFeature.UNLIMITED_APPLICATIONS -> limits.unlimitedApplicationTracking
                PremiumFeature.UNLIMITED_AI_ENHANCEMENTS -> limits.aiEnhancementsPerDay == Int.MAX_VALUE
                PremiumFeature.UNLIMITED_RESUMES -> limits.resumeVersionsPerMonth == Int.MAX_VALUE
                PremiumFeature.UNLIMITED_COVER_LETTERS -> limits.coverLettersPerMonth == Int.MAX_VALUE
                PremiumFeature.UNLIMITED_INTERVIEWS -> limits.interviewSessionsPerMonth == Int.MAX_VALUE
            }
            
            call.respond(mapOf(
                "feature" to feature.name,
                "available" to available,
                "requiredTier" to if (!available) getRequiredTier(feature).name else tier.name
            ))
        }
        
        // Stripe webhook handler
        post("/webhook") {
            val payload = call.receiveText()
            val signature = call.request.headers["Stripe-Signature"]
            
            if (signature == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing signature"))
                return@post
            }
            
            try {
                // Verify signature
                if (!stripeService.verifyWebhookSignature(payload, signature)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid signature"))
                    return@post
                }
                
                val event = json.decodeFromString<com.vwatek.apply.services.payments.StripeWebhookEvent>(payload)
                
                // Check for duplicate event
                val existing = transaction {
                    StripeWebhookEventsTable.select { StripeWebhookEventsTable.stripeEventId eq event.id }
                        .singleOrNull()
                }
                
                if (existing != null) {
                    call.respond(WebhookResponse(received = true))
                    return@post
                }
                
                // Process event
                when (event.type) {
                    "checkout.session.completed" -> handleCheckoutCompleted(event)
                    "customer.subscription.updated" -> handleSubscriptionUpdated(event)
                    "customer.subscription.deleted" -> handleSubscriptionDeleted(event)
                    "invoice.payment_succeeded" -> handlePaymentSucceeded(event)
                    "invoice.payment_failed" -> handlePaymentFailed(event)
                }
                
                // Record event
                transaction {
                    StripeWebhookEventsTable.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[stripeEventId] = event.id
                        it[eventType] = event.type
                        it[processedAt] = Clock.System.now()
                        it[status] = "PROCESSED"
                        it[rawPayload] = payload
                        it[createdAt] = Clock.System.now()
                    }
                }
                
                call.respond(WebhookResponse(received = true))
            } catch (e: Exception) {
                println("Webhook error: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Webhook processing failed"))
            }
        }
    }
}

private suspend fun getOrCreateStripeCustomer(userId: String, stripeService: StripeService): com.vwatek.apply.services.payments.StripeCustomer {
    // Check if customer exists
    val existing = transaction {
        StripeCustomersTable.select { StripeCustomersTable.userId eq userId }
            .singleOrNull()
    }
    
    if (existing != null) {
        return stripeService.getCustomer(existing[StripeCustomersTable.stripeCustomerId])
    }
    
    // Get user info
    val user = transaction {
        UsersTable.select { UsersTable.id eq userId }
            .singleOrNull()
    } ?: throw IllegalStateException("User not found")
    
    // Create Stripe customer
    val stripeCustomer = stripeService.createCustomer(
        email = user[UsersTable.email],
        name = "${user[UsersTable.firstName]} ${user[UsersTable.lastName]}",
        userId = userId
    )
    
    // Store mapping
    transaction {
        StripeCustomersTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[StripeCustomersTable.userId] = userId
            it[stripeCustomerId] = stripeCustomer.id
            it[email] = user[UsersTable.email]
            it[createdAt] = Clock.System.now()
            it[updatedAt] = Clock.System.now()
        }
    }
    
    return stripeCustomer
}

private fun handleCheckoutCompleted(event: com.vwatek.apply.services.payments.StripeWebhookEvent) {
    val data = event.data.`object`
    val customerId = data["customer"]?.toString()?.replace("\"", "") ?: return
    val subscriptionId = data["subscription"]?.toString()?.replace("\"", "") ?: return
    
    // Find user by customer ID
    val customer = transaction {
        StripeCustomersTable.select { StripeCustomersTable.stripeCustomerId eq customerId }
            .singleOrNull()
    } ?: return
    
    val userId = customer[StripeCustomersTable.userId]
    val now = Clock.System.now()
    
    // Create or update subscription
    transaction {
        val existing = SubscriptionsTable.select { SubscriptionsTable.userId eq userId }.singleOrNull()
        
        if (existing == null) {
            SubscriptionsTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[SubscriptionsTable.userId] = userId
                it[tier] = "PRO"  // Will be updated by subscription.updated event
                it[status] = "ACTIVE"
                it[billingPeriod] = "MONTHLY"
                it[paymentProvider] = "STRIPE"
                it[externalSubscriptionId] = subscriptionId
                it[SubscriptionsTable.customerId] = customerId
                it[currentPeriodStart] = now
                it[currentPeriodEnd] = now
                it[cancelAtPeriodEnd] = false
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            SubscriptionsTable.update({ SubscriptionsTable.userId eq userId }) {
                it[externalSubscriptionId] = subscriptionId
                it[status] = "ACTIVE"
                it[updatedAt] = now
            }
        }
    }
}

private fun handleSubscriptionUpdated(event: com.vwatek.apply.services.payments.StripeWebhookEvent) {
    val data = event.data.`object`
    val subscriptionId = data["id"]?.toString()?.replace("\"", "") ?: return
    val status = data["status"]?.toString()?.replace("\"", "") ?: return
    val cancelAtPeriodEnd = data["cancel_at_period_end"]?.toString()?.toBoolean() ?: false
    val currentPeriodStart = data["current_period_start"]?.toString()?.toLongOrNull()
    val currentPeriodEnd = data["current_period_end"]?.toString()?.toLongOrNull()
    
    val now = Clock.System.now()
    
    transaction {
        SubscriptionsTable.update({ SubscriptionsTable.externalSubscriptionId eq subscriptionId }) {
            it[SubscriptionsTable.status] = status.uppercase()
            it[SubscriptionsTable.cancelAtPeriodEnd] = cancelAtPeriodEnd
            currentPeriodStart?.let { start ->
                it[SubscriptionsTable.currentPeriodStart] = Instant.fromEpochSeconds(start)
            }
            currentPeriodEnd?.let { end ->
                it[SubscriptionsTable.currentPeriodEnd] = Instant.fromEpochSeconds(end)
            }
            it[updatedAt] = now
        }
    }
}

private fun handleSubscriptionDeleted(event: com.vwatek.apply.services.payments.StripeWebhookEvent) {
    val data = event.data.`object`
    val subscriptionId = data["id"]?.toString()?.replace("\"", "") ?: return
    
    val now = Clock.System.now()
    
    transaction {
        SubscriptionsTable.update({ SubscriptionsTable.externalSubscriptionId eq subscriptionId }) {
            it[status] = "CANCELED"
            it[canceledAt] = now
            it[updatedAt] = now
        }
    }
}

private fun handlePaymentSucceeded(event: com.vwatek.apply.services.payments.StripeWebhookEvent) {
    val data = event.data.`object`
    val customerId = data["customer"]?.toString()?.replace("\"", "") ?: return
    val subscriptionId = data["subscription"]?.toString()?.replace("\"", "")
    val amountPaid = data["amount_paid"]?.toString()?.toLongOrNull() ?: 0
    val invoiceId = data["id"]?.toString()?.replace("\"", "")
    
    // Find user
    val customer = transaction {
        StripeCustomersTable.select { StripeCustomersTable.stripeCustomerId eq customerId }
            .singleOrNull()
    } ?: return
    
    val userId = customer[StripeCustomersTable.userId]
    val now = Clock.System.now()
    
    // Record payment
    transaction {
        PaymentsTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[PaymentsTable.userId] = userId
            it[PaymentsTable.subscriptionId] = subscriptionId?.let { subId ->
                SubscriptionsTable.select { SubscriptionsTable.externalSubscriptionId eq subId }
                    .singleOrNull()?.get(SubscriptionsTable.id)
            }
            it[externalPaymentId] = invoiceId ?: UUID.randomUUID().toString()
            it[provider] = "STRIPE"
            it[amount] = java.math.BigDecimal.valueOf(amountPaid / 100.0)
            it[currency] = "CAD"
            it[status] = "SUCCEEDED"
            it[PaymentsTable.invoiceId] = invoiceId
            it[createdAt] = now
        }
    }
}

private fun handlePaymentFailed(event: com.vwatek.apply.services.payments.StripeWebhookEvent) {
    val data = event.data.`object`
    val subscriptionId = data["subscription"]?.toString()?.replace("\"", "") ?: return
    
    val now = Clock.System.now()
    
    transaction {
        SubscriptionsTable.update({ SubscriptionsTable.externalSubscriptionId eq subscriptionId }) {
            it[status] = "PAST_DUE"
            it[updatedAt] = now
        }
    }
}

private fun getRequiredTier(feature: PremiumFeature): SubscriptionTier {
    return when (feature) {
        PremiumFeature.SALARY_INSIGHTS -> SubscriptionTier.PRO
        PremiumFeature.NEGOTIATION_COACH -> SubscriptionTier.PREMIUM
        PremiumFeature.LINKEDIN_OPTIMIZER -> SubscriptionTier.PREMIUM
        PremiumFeature.UNLIMITED_APPLICATIONS -> SubscriptionTier.PRO
        PremiumFeature.UNLIMITED_AI_ENHANCEMENTS -> SubscriptionTier.PREMIUM
        PremiumFeature.UNLIMITED_RESUMES -> SubscriptionTier.PREMIUM
        PremiumFeature.UNLIMITED_COVER_LETTERS -> SubscriptionTier.PREMIUM
        PremiumFeature.UNLIMITED_INTERVIEWS -> SubscriptionTier.PREMIUM
    }
}

private fun ResultRow.toDto(): SubscriptionDto {
    return SubscriptionDto(
        id = this[SubscriptionsTable.id],
        userId = this[SubscriptionsTable.userId],
        tier = this[SubscriptionsTable.tier],
        status = this[SubscriptionsTable.status],
        billingPeriod = this[SubscriptionsTable.billingPeriod],
        paymentProvider = this[SubscriptionsTable.paymentProvider],
        currentPeriodStart = this[SubscriptionsTable.currentPeriodStart].toString(),
        currentPeriodEnd = this[SubscriptionsTable.currentPeriodEnd].toString(),
        cancelAtPeriodEnd = this[SubscriptionsTable.cancelAtPeriodEnd],
        canceledAt = this[SubscriptionsTable.canceledAt]?.toString(),
        trialStart = this[SubscriptionsTable.trialStart]?.toString(),
        trialEnd = this[SubscriptionsTable.trialEnd]?.toString()
    )
}
