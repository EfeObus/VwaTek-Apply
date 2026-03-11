plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.owasp.dependencycheck") version "9.2.0"
}

group = "com.vwatek.apply"
version = "1.0.0"

application {
    mainClass.set("com.vwatek.apply.ApplicationKt")
}

// Copy web frontend assets into backend resources so they're embedded in the JAR
// Only when webApp project is available (not in backend-only Docker builds)
val webAppProject = rootProject.findProject(":webApp")

if (webAppProject != null) {
    val copyWebAssets by tasks.registering(Copy::class) {
        dependsOn(":webApp:jsBrowserDistribution")
        from(rootProject.file("webApp/build/dist/js/productionExecutable"))
        into(layout.buildDirectory.dir("resources/main/web"))
    }

    tasks.named("processResources") {
        dependsOn(copyWebAssets)
    }
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
    implementation("io.ktor:ktor-server-call-id:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-default-headers:${libs.versions.ktor.get()}")
    implementation(libs.ktor.server.metrics.micrometer)
    implementation("io.ktor:ktor-server-rate-limit:${libs.versions.ktor.get()}")
    implementation(libs.logback.classic)
    
    // Metrics & Monitoring
    implementation(libs.micrometer.registry.prometheus)
    
    // Ktor Client (for LinkedIn OAuth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    
    // Database
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    
    // Database Migrations (Flyway)
    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.10.0")
    
    // Password Hashing (bcrypt)
    implementation("at.favre.lib:bcrypt:0.10.2")
    
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
