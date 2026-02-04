package com.vwatek.apply.di

import com.vwatek.apply.data.local.DatabaseDriverFactory
import com.vwatek.apply.data.local.createDatabaseDriverFactory
import com.vwatek.apply.data.repository.ResumeRepositoryImpl
import com.vwatek.apply.data.repository.AnalysisRepositoryImpl
import com.vwatek.apply.data.repository.CoverLetterRepositoryImpl
import com.vwatek.apply.data.repository.InterviewRepositoryImpl
import com.vwatek.apply.data.repository.SettingsRepositoryImpl
import com.vwatek.apply.data.repository.IosAuthRepository
import com.vwatek.apply.data.repository.IosLinkedInRepository
import com.vwatek.apply.data.repository.IosFileUploadRepository
import com.vwatek.apply.db.VwaTekDatabase
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.domain.repository.InterviewRepository
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.LinkedInRepository
import com.vwatek.apply.domain.repository.FileUploadRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Database
    single<DatabaseDriverFactory> { createDatabaseDriverFactory() }
    
    single {
        val driverFactory: DatabaseDriverFactory = get()
        runBlocking {
            VwaTekDatabase(driverFactory.createDriver())
        }
    }
    
    // HTTP Client
    single {
        HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                })
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
            }
            
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
    
    // Repositories using local SQLDelight database
    single<ResumeRepository> { ResumeRepositoryImpl(get()) }
    single<AnalysisRepository> { AnalysisRepositoryImpl(get()) }
    single<CoverLetterRepository> { CoverLetterRepositoryImpl(get()) }
    single<InterviewRepository> { InterviewRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    
    // Auth, LinkedIn, and FileUpload repositories
    single<AuthRepository> { IosAuthRepository(get()) }
    single<LinkedInRepository> { IosLinkedInRepository() }
    single<FileUploadRepository> { IosFileUploadRepository() }
}
