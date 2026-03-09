package com.vwatek.apply.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

/**
 * Security headers: Content-Security-Policy, HSTS, X-Content-Type-Options, etc.
 * Hardens the backend against XSS, clickjacking, and downgrade attacks.
 */
fun Application.configureSecurityHeaders() {
    install(DefaultHeaders) {
        // HSTS — force HTTPS for 1 year, include subdomains
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")

        // Prevent MIME-type sniffing
        header("X-Content-Type-Options", "nosniff")

        // Prevent clickjacking
        header("X-Frame-Options", "DENY")

        // XSS protection (legacy browsers)
        header("X-XSS-Protection", "1; mode=block")

        // Referrer policy — send origin only for cross-origin requests
        header("Referrer-Policy", "strict-origin-when-cross-origin")

        // Content-Security-Policy — restrict resource loading
        header(
            "Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self' https://generativelanguage.googleapis.com https://api.openai.com; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'"
        )

        // Permissions policy — disable unnecessary browser APIs
        header("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()")
    }
}
