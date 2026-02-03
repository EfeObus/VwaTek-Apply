package com.vwatek.apply.di

import com.vwatek.apply.data.repository.LocalStorageResumeRepository
import com.vwatek.apply.data.repository.LocalStorageAnalysisRepository
import com.vwatek.apply.data.repository.LocalStorageCoverLetterRepository
import com.vwatek.apply.data.repository.LocalStorageInterviewRepository
import com.vwatek.apply.data.repository.LocalStorageSettingsRepository
import com.vwatek.apply.data.repository.ApiAuthRepository
import com.vwatek.apply.data.repository.LocalStorageLinkedInRepository
import com.vwatek.apply.data.repository.LocalStorageFileUploadRepository
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.domain.repository.InterviewRepository
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.LinkedInRepository
import com.vwatek.apply.domain.repository.FileUploadRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Use LocalStorage-based repositories for web platform
    // Auth is API-based to connect to backend, others use localStorage for now
    single<ResumeRepository> { LocalStorageResumeRepository() }
    single<AnalysisRepository> { LocalStorageAnalysisRepository() }
    single<CoverLetterRepository> { LocalStorageCoverLetterRepository() }
    single<InterviewRepository> { LocalStorageInterviewRepository() }
    single<SettingsRepository> { LocalStorageSettingsRepository() }
    
    // Authentication - Use API-based repository to connect to backend
    single<AuthRepository> { ApiAuthRepository() }
    single<LinkedInRepository> { LocalStorageLinkedInRepository() }
    single<FileUploadRepository> { LocalStorageFileUploadRepository() }
    
    single {
        HttpClient(Js) {
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
}
