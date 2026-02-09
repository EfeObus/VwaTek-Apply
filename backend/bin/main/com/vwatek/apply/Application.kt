package com.vwatek.apply

import com.vwatek.apply.config.DatabaseConfig
import com.vwatek.apply.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8090
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseConfig.init()
    
    // Configure plugins
    configureSerialization()
    configureCORS()
    configureRouting()
    configureStatusPages()
    configureCallLogging()
}
