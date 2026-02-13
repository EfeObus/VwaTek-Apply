package com.vwatek.apply.services.payments

import com.vwatek.apply.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Stripe Payment Service for VwaTek Apply
 * Handles subscription creation, management, and payment processing
 */
class StripeService(private val httpClient: HttpClient) {
    
    private val stripeSecretKey = System.getenv("STRIPE_SECRET_KEY") ?: ""
    private val stripeWebhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET") ?: ""
    private val baseUrl = "https://api.stripe.com/v1"
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Create a Stripe customer for a user
     */
    suspend fun createCustomer(
        email: String,
        name: String,
        userId: String
    ): StripeCustomer {
        val response: HttpResponse = httpClient.post("$baseUrl/customers") {
            headers {
                basicAuth()
            }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "email" to email,
                    "name" to name,
                    "metadata[user_id]" to userId
                ).formUrlEncode()
            )
        }
        
        return response.body()
    }
    
    /**
     * Get a Stripe customer by ID
     */
    suspend fun getCustomer(customerId: String): StripeCustomer {
        val response: HttpResponse = httpClient.get("$baseUrl/customers/$customerId") {
            headers { basicAuth() }
        }
        return response.body()
    }
    
    /**
     * Create a checkout session for subscription
     */
    suspend fun createCheckoutSession(
        customerId: String,
        priceId: String,
        successUrl: String,
        cancelUrl: String,
        trialPeriodDays: Int? = null
    ): StripeCheckoutSession {
        val params = mutableListOf(
            "customer" to customerId,
            "mode" to "subscription",
            "success_url" to successUrl,
            "cancel_url" to cancelUrl,
            "line_items[0][price]" to priceId,
            "line_items[0][quantity]" to "1",
            "allow_promotion_codes" to "true",
            "billing_address_collection" to "required",
            "payment_method_types[0]" to "card"
        )
        
        trialPeriodDays?.let {
            params.add("subscription_data[trial_period_days]" to it.toString())
        }
        
        val response: HttpResponse = httpClient.post("$baseUrl/checkout/sessions") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(params.formUrlEncode())
        }
        
        return response.body()
    }
    
    /**
     * Create a customer portal session for subscription management
     */
    suspend fun createPortalSession(
        customerId: String,
        returnUrl: String
    ): StripePortalSession {
        val response: HttpResponse = httpClient.post("$baseUrl/billing_portal/sessions") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "customer" to customerId,
                    "return_url" to returnUrl
                ).formUrlEncode()
            )
        }
        
        return response.body()
    }
    
    /**
     * Get subscription details
     */
    suspend fun getSubscription(subscriptionId: String): StripeSubscription {
        val response: HttpResponse = httpClient.get("$baseUrl/subscriptions/$subscriptionId") {
            headers { basicAuth() }
        }
        return response.body()
    }
    
    /**
     * Cancel a subscription at period end
     */
    suspend fun cancelSubscriptionAtPeriodEnd(subscriptionId: String): StripeSubscription {
        val response: HttpResponse = httpClient.post("$baseUrl/subscriptions/$subscriptionId") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("cancel_at_period_end=true")
        }
        return response.body()
    }
    
    /**
     * Reactivate a canceled subscription
     */
    suspend fun reactivateSubscription(subscriptionId: String): StripeSubscription {
        val response: HttpResponse = httpClient.post("$baseUrl/subscriptions/$subscriptionId") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("cancel_at_period_end=false")
        }
        return response.body()
    }
    
    /**
     * Update subscription to different price
     */
    suspend fun updateSubscription(
        subscriptionId: String,
        newPriceId: String,
        prorationBehavior: String = "create_prorations"
    ): StripeSubscription {
        // First get the subscription to find the item ID
        val subscription = getSubscription(subscriptionId)
        val itemId = subscription.items.data.firstOrNull()?.id
            ?: throw IllegalStateException("No subscription items found")
        
        val response: HttpResponse = httpClient.post("$baseUrl/subscriptions/$subscriptionId") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "items[0][id]" to itemId,
                    "items[0][price]" to newPriceId,
                    "proration_behavior" to prorationBehavior
                ).formUrlEncode()
            )
        }
        
        return response.body()
    }
    
    /**
     * Cancel subscription immediately
     */
    suspend fun cancelSubscriptionImmediately(subscriptionId: String): StripeSubscription {
        val response: HttpResponse = httpClient.delete("$baseUrl/subscriptions/$subscriptionId") {
            headers { basicAuth() }
        }
        return response.body()
    }
    
    /**
     * Process refund for a payment
     */
    suspend fun createRefund(
        paymentIntentId: String,
        amount: Long? = null,  // In cents, null for full refund
        reason: String? = null
    ): StripeRefund {
        val params = mutableListOf(
            "payment_intent" to paymentIntentId
        )
        amount?.let { params.add("amount" to it.toString()) }
        reason?.let { params.add("reason" to it) }
        
        val response: HttpResponse = httpClient.post("$baseUrl/refunds") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(params.formUrlEncode())
        }
        
        return response.body()
    }
    
    /**
     * List all invoices for a customer
     */
    suspend fun listInvoices(customerId: String, limit: Int = 10): StripeInvoiceList {
        val response: HttpResponse = httpClient.get("$baseUrl/invoices") {
            headers { basicAuth() }
            parameter("customer", customerId)
            parameter("limit", limit)
        }
        return response.body()
    }
    
    /**
     * Verify webhook signature
     */
    fun verifyWebhookSignature(
        payload: String,
        signatureHeader: String
    ): Boolean {
        if (stripeWebhookSecret.isBlank()) {
            throw IllegalStateException("Stripe webhook secret not configured")
        }
        
        val parts = signatureHeader.split(",").associate { part ->
            val (key, value) = part.split("=")
            key to value
        }
        
        val timestamp = parts["t"]?.toLongOrNull() ?: return false
        val signature = parts["v1"] ?: return false
        
        // Check timestamp is within tolerance (5 minutes)
        val now = Clock.System.now().epochSeconds
        if (kotlin.math.abs(now - timestamp) > 300) {
            return false
        }
        
        // Compute expected signature
        val signedPayload = "$timestamp.$payload"
        val expectedSignature = computeHmacSha256(signedPayload, stripeWebhookSecret)
        
        return signature == expectedSignature
    }
    
    private fun computeHmacSha256(data: String, key: String): String {
        val algorithm = "HmacSHA256"
        val mac = javax.crypto.Mac.getInstance(algorithm)
        val secretKey = javax.crypto.spec.SecretKeySpec(key.toByteArray(), algorithm)
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Create a price in Stripe
     */
    suspend fun createPrice(
        productId: String,
        unitAmount: Long,
        currency: String = "cad",
        interval: String = "month",
        intervalCount: Int = 1
    ): StripePrice {
        val response: HttpResponse = httpClient.post("$baseUrl/prices") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "product" to productId,
                    "unit_amount" to unitAmount.toString(),
                    "currency" to currency,
                    "recurring[interval]" to interval,
                    "recurring[interval_count]" to intervalCount.toString()
                ).formUrlEncode()
            )
        }
        
        return response.body()
    }
    
    /**
     * Create a product in Stripe
     */
    suspend fun createProduct(
        name: String,
        description: String? = null
    ): StripeProduct {
        val params = mutableListOf("name" to name)
        description?.let { params.add("description" to it) }
        
        val response: HttpResponse = httpClient.post("$baseUrl/products") {
            headers { basicAuth() }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(params.formUrlEncode())
        }
        
        return response.body()
    }
    
    private fun HeadersBuilder.basicAuth() {
        val credentials = "$stripeSecretKey:"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        append(HttpHeaders.Authorization, "Basic $encoded")
    }
    
    private fun List<Pair<String, String>>.formUrlEncode(): String =
        joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
    
    private fun String.encodeURLParameter(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}

// Stripe API Response Models

@Serializable
data class StripeCustomer(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val default_source: String? = null,
    val invoice_settings: StripeInvoiceSettings? = null,
    val created: Long
)

@Serializable
data class StripeInvoiceSettings(
    val default_payment_method: String? = null
)

@Serializable
data class StripeCheckoutSession(
    val id: String,
    val url: String,
    val customer: String? = null,
    val subscription: String? = null,
    val status: String,
    val payment_status: String
)

@Serializable
data class StripePortalSession(
    val id: String,
    val url: String,
    val customer: String,
    val return_url: String
)

@Serializable
data class StripeSubscription(
    val id: String,
    val customer: String,
    val status: String,
    val current_period_start: Long,
    val current_period_end: Long,
    val cancel_at_period_end: Boolean,
    val canceled_at: Long? = null,
    val trial_start: Long? = null,
    val trial_end: Long? = null,
    val items: StripeSubscriptionItems,
    val latest_invoice: String? = null,
    val default_payment_method: String? = null
)

@Serializable
data class StripeSubscriptionItems(
    val data: List<StripeSubscriptionItem>
)

@Serializable
data class StripeSubscriptionItem(
    val id: String,
    val price: StripePrice
)

@Serializable
data class StripePrice(
    val id: String,
    val product: String,
    val unit_amount: Long,
    val currency: String,
    val recurring: StripePriceRecurring? = null,
    val active: Boolean = true
)

@Serializable
data class StripePriceRecurring(
    val interval: String,
    val interval_count: Int
)

@Serializable
data class StripeProduct(
    val id: String,
    val name: String,
    val description: String? = null,
    val active: Boolean = true
)

@Serializable
data class StripeRefund(
    val id: String,
    val amount: Long,
    val currency: String,
    val payment_intent: String,
    val status: String,
    val reason: String? = null
)

@Serializable
data class StripeInvoiceList(
    val data: List<StripeInvoice>,
    val has_more: Boolean
)

@Serializable
data class StripeInvoice(
    val id: String,
    val customer: String,
    val subscription: String? = null,
    val status: String,
    val amount_due: Long,
    val amount_paid: Long,
    val currency: String,
    val invoice_pdf: String? = null,
    val hosted_invoice_url: String? = null,
    val created: Long
)

@Serializable
data class StripeWebhookEvent(
    val id: String,
    val type: String,
    val data: StripeEventData,
    val created: Long
)

@Serializable
data class StripeEventData(
    val `object`: kotlinx.serialization.json.JsonObject
)
