package com.vwatek.apply.di

import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.presentation.resume.ResumeViewModel
import com.vwatek.apply.presentation.coverletter.CoverLetterViewModel
import com.vwatek.apply.presentation.interview.InterviewViewModel
import com.vwatek.apply.presentation.tracker.TrackerViewModel
import com.vwatek.apply.presentation.noc.NOCViewModel
import com.vwatek.apply.presentation.jobbank.JobBankViewModel
import com.vwatek.apply.domain.usecase.SubscriptionManager
import com.vwatek.apply.domain.usecase.SalaryIntelligenceManager
import com.vwatek.apply.domain.usecase.CreateCheckoutSessionUseCase
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Helper class to access Koin from iOS
 */
object KoinHelper : KoinComponent {
    private val authViewModel: AuthViewModel by inject()
    private val resumeViewModel: ResumeViewModel by inject()
    private val coverLetterViewModel: CoverLetterViewModel by inject()
    private val interviewViewModel: InterviewViewModel by inject()
    private val trackerViewModel: TrackerViewModel by inject()
    private val nocViewModel: NOCViewModel by inject()
    private val jobBankViewModel: JobBankViewModel by inject()
    private val subscriptionManager: SubscriptionManager by inject()
    private val salaryIntelligenceManager: SalaryIntelligenceManager by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val linkedInOptimizerManager: com.vwatek.apply.domain.usecase.LinkedInOptimizerManager by inject()
    private val organizationViewModel: com.vwatek.apply.presentation.organization.OrganizationViewModel by inject()
    private val createCheckoutSessionUseCase: CreateCheckoutSessionUseCase by inject()
    
    fun getAuthViewModel(): AuthViewModel = authViewModel
    fun getResumeViewModel(): ResumeViewModel = resumeViewModel
    fun getCoverLetterViewModel(): CoverLetterViewModel = coverLetterViewModel
    fun getInterviewViewModel(): InterviewViewModel = interviewViewModel
    fun getTrackerViewModel(): TrackerViewModel = trackerViewModel
    fun getNOCViewModel(): NOCViewModel = nocViewModel
    fun getJobBankViewModel(): JobBankViewModel = jobBankViewModel
    fun getSubscriptionManager(): SubscriptionManager = subscriptionManager
    fun getSalaryIntelligenceManager(): SalaryIntelligenceManager = salaryIntelligenceManager
    fun getSettingsRepository(): SettingsRepository = settingsRepository
    fun getLinkedInOptimizerManager(): com.vwatek.apply.domain.usecase.LinkedInOptimizerManager = linkedInOptimizerManager
    fun getOrganizationViewModel(): com.vwatek.apply.presentation.organization.OrganizationViewModel = organizationViewModel
    fun getCreateCheckoutSessionUseCase(): CreateCheckoutSessionUseCase = createCheckoutSessionUseCase
}

/**
 * Swift-friendly settings helper with blocking methods
 * This allows Swift to call settings operations without dealing with suspend functions
 */
object SettingsHelper : KoinComponent {
    private val settingsRepository: SettingsRepository by inject()
    
    fun getSetting(key: String): String? = runBlocking {
        settingsRepository.getSetting(key)
    }
    
    fun setSetting(key: String, value: String) = runBlocking {
        settingsRepository.setSetting(key, value)
    }
    
    fun deleteSetting(key: String) = runBlocking {
        settingsRepository.deleteSetting(key)
    }
}

/**
 * Initialize Koin for iOS
 */
fun initKoin() {
    startKoin {
        modules(
            sharedModule,
            platformModule()
        )
    }
}

/**
 * Direct accessor functions for Swift
 */
fun getAuthViewModel(): AuthViewModel = KoinHelper.getAuthViewModel()
fun getResumeViewModel(): ResumeViewModel = KoinHelper.getResumeViewModel()
fun getCoverLetterViewModel(): CoverLetterViewModel = KoinHelper.getCoverLetterViewModel()
fun getInterviewViewModel(): InterviewViewModel = KoinHelper.getInterviewViewModel()
fun getTrackerViewModel(): TrackerViewModel = KoinHelper.getTrackerViewModel()
fun getNOCViewModel(): NOCViewModel = KoinHelper.getNOCViewModel()
fun getJobBankViewModel(): JobBankViewModel = KoinHelper.getJobBankViewModel()
fun getSubscriptionManager(): SubscriptionManager = KoinHelper.getSubscriptionManager()
fun getSalaryIntelligenceManager(): SalaryIntelligenceManager = KoinHelper.getSalaryIntelligenceManager()
fun getSettingsRepository(): SettingsRepository = KoinHelper.getSettingsRepository()
fun getLinkedInOptimizerManager(): com.vwatek.apply.domain.usecase.LinkedInOptimizerManager = KoinHelper.getLinkedInOptimizerManager()
fun getOrganizationViewModel(): com.vwatek.apply.presentation.organization.OrganizationViewModel = KoinHelper.getOrganizationViewModel()
fun getCreateCheckoutSessionUseCase(): CreateCheckoutSessionUseCase = KoinHelper.getCreateCheckoutSessionUseCase()

// Settings helper functions for Swift
fun getSetting(key: String): String? = SettingsHelper.getSetting(key)
fun setSetting(key: String, value: String) = SettingsHelper.setSetting(key, value)
fun deleteSetting(key: String) = SettingsHelper.deleteSetting(key)
