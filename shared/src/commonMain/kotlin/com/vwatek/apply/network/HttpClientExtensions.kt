package com.vwatek.apply.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Configures the HttpClient with:
 * - Retry logic with exponential backoff for transient failures
 * - HTTP 401 auto-logout callback
 * - Timeout configuration
 */
fun HttpClientConfig<*>.installRetryAndAuthHandling(
    onUnauthorized: (() -> Unit)? = null,
    maxRetries: Int = 3,
    initialDelayMs: Long = 500
) {
    // Retry on transient failures with exponential backoff
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = maxRetries)
        retryOnException(maxRetries = maxRetries, retryOnTimeout = true)
        exponentialDelay(
            base = initialDelayMs.toDouble() / 1000.0,  // base in seconds
            maxDelayMs = 10_000
        )
        modifyRequest { request ->
            request.headers.append("X-Retry-Count", retryCount.toString())
        }
    }

    // Response validation — auto-logout on 401
    HttpResponseValidator {
        validateResponse { response ->
            when (response.status) {
                HttpStatusCode.Unauthorized -> {
                    onUnauthorized?.invoke()
                    throw ClientRequestException(response, "Unauthorized — session expired")
                }
                HttpStatusCode.Forbidden -> {
                    throw ClientRequestException(response, "Forbidden — insufficient permissions")
                }
                HttpStatusCode.TooManyRequests -> {
                    throw ClientRequestException(response, "Rate limited — try again later")
                }
                else -> { /* OK */ }
            }
        }
    }
}
