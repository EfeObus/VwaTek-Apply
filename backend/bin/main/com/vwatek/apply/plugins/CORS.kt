package com.vwatek.apply.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader("X-User-Id")
        
        // Allow requests from web frontends
        allowHost("storage.googleapis.com", schemes = listOf("https"))
        allowHost("vwatek-apply-frontend.storage.googleapis.com", schemes = listOf("https"))
        allowHost("localhost:8080", schemes = listOf("http"))
        allowHost("127.0.0.1:8080", schemes = listOf("http"))
        
        // For development - allow any host (comment out in strict production)
        anyHost()
        
        allowCredentials = true
        allowNonSimpleContentTypes = true
        
        // Expose headers for the frontend
        exposeHeader(HttpHeaders.Authorization)
        exposeHeader(HttpHeaders.ContentType)
    }
}
