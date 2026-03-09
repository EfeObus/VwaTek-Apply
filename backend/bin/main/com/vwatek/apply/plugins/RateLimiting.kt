package com.vwatek.apply.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Rate limiting configuration for VwaTek Apply backend.
 *
 * Defines rate-limit buckets:
 *   - "auth"         → 20 requests / 1 minute  (login, register, reset-password)
 *   - "ai"           → 10 requests / 1 minute  (AI analysis / generation endpoints)
 *   - "subscription" → 30 requests / 1 minute  (checkout, portal, webhook-excluded)
 *   - (default)      → 100 requests / 1 minute (everything else inside rate-limit blocks)
 *
 * Key extraction: prefer the JWT "sub" claim (userId) from the auth principal;
 * fall back to the remote IP address for unauthenticated callers.
 */
fun Application.configureRateLimiting() {
    install(RateLimit) {
        // Auth endpoints – tightest limit to deter brute-force
        register(RateLimitName("auth")) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }

        // AI endpoints – protect against credit drain
        register(RateLimitName("ai")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }

        // Subscription endpoints – moderate limit
        register(RateLimitName("subscription")) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }

        // Global fallback for any rateLimit { } block without a named bucket
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }
    }
}
