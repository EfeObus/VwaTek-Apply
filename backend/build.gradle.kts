plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vwatek.apply"
version = "1.0.0"

application {
    mainClass.set("com.vwatek.apply.ApplicationKt")
}

// Shadow JAR configuration for fat JAR
tasks.shadowJar {
    archiveBaseName.set("backend")
    archiveClassifier.set("all")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to "com.vwatek.apply.ApplicationKt"
        )
    }
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.logback.classic)
    
    // Metrics & Monitoring
    implementation(libs.micrometer.registry.prometheus)
    
    // Ktor Client (for LinkedIn OAuth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    
    // Database
    implementation(libs.hikari)
    implementation(libs.mysql.connector)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    
    // Google Cloud SQL Socket Factory for Cloud Run (updated for MySQL 8.4 compatibility)
    implementation("com.google.cloud.sql:mysql-socket-factory-connector-j-8:1.21.0")
    
    // Email (JavaMail/Jakarta Mail)
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    
    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    
    // Testing
    testImplementation(libs.kotlin.test)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
