package com.vwatek.apply.di

import com.vwatek.apply.data.api.GeminiService
import com.vwatek.apply.data.api.JobTrackerApiClient
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.domain.repository.InterviewRepository
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.LinkedInRepository
import com.vwatek.apply.domain.repository.FileUploadRepository
import com.vwatek.apply.domain.usecase.GetAllResumesUseCase
import com.vwatek.apply.domain.usecase.GetResumeByIdUseCase
import com.vwatek.apply.domain.usecase.SaveResumeUseCase
import com.vwatek.apply.domain.usecase.DeleteResumeUseCase
import com.vwatek.apply.domain.usecase.AnalyzeResumeUseCase
import com.vwatek.apply.domain.usecase.OptimizeResumeUseCase
import com.vwatek.apply.domain.usecase.PerformATSAnalysisUseCase
import com.vwatek.apply.domain.usecase.GenerateImpactBulletsUseCase
import com.vwatek.apply.domain.usecase.AnalyzeGrammarUseCase
import com.vwatek.apply.domain.usecase.RewriteSectionUseCase
import com.vwatek.apply.domain.usecase.GetResumeVersionsUseCase
import com.vwatek.apply.domain.usecase.GetResumeVersionByIdUseCase
import com.vwatek.apply.domain.usecase.CreateResumeVersionUseCase
import com.vwatek.apply.domain.usecase.RestoreResumeVersionUseCase
import com.vwatek.apply.domain.usecase.DeleteResumeVersionUseCase
import com.vwatek.apply.domain.usecase.GetAllCoverLettersUseCase
import com.vwatek.apply.domain.usecase.GenerateCoverLetterUseCase
import com.vwatek.apply.domain.usecase.DeleteCoverLetterUseCase
import com.vwatek.apply.domain.usecase.GetAllInterviewSessionsUseCase
import com.vwatek.apply.domain.usecase.GetInterviewSessionByIdUseCase
import com.vwatek.apply.domain.usecase.StartInterviewSessionUseCase
import com.vwatek.apply.domain.usecase.SubmitInterviewAnswerUseCase
import com.vwatek.apply.domain.usecase.CompleteInterviewSessionUseCase
import com.vwatek.apply.domain.usecase.DeleteInterviewSessionUseCase
import com.vwatek.apply.domain.usecase.GetStarCoachingUseCase
import com.vwatek.apply.domain.usecase.GetAuthStateUseCase
import com.vwatek.apply.domain.usecase.GetCurrentUserUseCase
import com.vwatek.apply.domain.usecase.RegisterWithEmailUseCase
import com.vwatek.apply.domain.usecase.LoginWithEmailUseCase
import com.vwatek.apply.domain.usecase.LoginWithGoogleUseCase
import com.vwatek.apply.domain.usecase.LoginWithLinkedInUseCase
import com.vwatek.apply.domain.usecase.LogoutUseCase
import com.vwatek.apply.domain.usecase.UpdateProfileUseCase
import com.vwatek.apply.domain.usecase.ResetPasswordUseCase
import com.vwatek.apply.domain.usecase.CheckEmailAvailabilityUseCase
import com.vwatek.apply.domain.usecase.GetLinkedInAuthUrlUseCase
import com.vwatek.apply.domain.usecase.ImportLinkedInProfileUseCase
import com.vwatek.apply.domain.usecase.UploadResumeFileUseCase
import com.vwatek.apply.domain.usecase.GetSupportedFileTypesUseCase
import com.vwatek.apply.domain.usecase.GetMaxFileSizeUseCase
import com.vwatek.apply.domain.usecase.tracker.*
import com.vwatek.apply.presentation.resume.ResumeViewModel
import com.vwatek.apply.presentation.coverletter.CoverLetterViewModel
import com.vwatek.apply.presentation.interview.InterviewViewModel
import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.presentation.tracker.TrackerViewModel
import com.vwatek.apply.data.api.NOCApiClient
import com.vwatek.apply.presentation.noc.NOCViewModel
import com.vwatek.apply.data.api.JobBankApiClient
import com.vwatek.apply.presentation.jobbank.JobBankViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformModule(): Module

val sharedModule = module {
    // API Service (repositories are provided by platform module)
    single { 
        GeminiService(
            httpClient = get(),
            settingsRepository = get()
        )
    }
    
    // Resume Use Cases
    factory { GetAllResumesUseCase(get()) }
    factory { GetResumeByIdUseCase(get()) }
    factory { SaveResumeUseCase(get()) }
    factory { DeleteResumeUseCase(get()) }
    factory { AnalyzeResumeUseCase(get(), get()) }
    factory { OptimizeResumeUseCase(get()) }
    factory { PerformATSAnalysisUseCase(get()) }
    factory { GenerateImpactBulletsUseCase(get()) }
    factory { AnalyzeGrammarUseCase(get()) }
    factory { RewriteSectionUseCase(get()) }
    
    // Resume Version Control Use Cases
    factory { GetResumeVersionsUseCase(get()) }
    factory { GetResumeVersionByIdUseCase(get()) }
    factory { CreateResumeVersionUseCase(get()) }
    factory { RestoreResumeVersionUseCase(get(), get()) }
    factory { DeleteResumeVersionUseCase(get()) }
    
    // Cover Letter Use Cases
    factory { GetAllCoverLettersUseCase(get()) }
    factory { GenerateCoverLetterUseCase(get(), get()) }
    factory { DeleteCoverLetterUseCase(get()) }
    
    // Interview Use Cases
    factory { GetAllInterviewSessionsUseCase(get()) }
    factory { GetInterviewSessionByIdUseCase(get()) }
    factory { StartInterviewSessionUseCase(get(), get()) }
    factory { SubmitInterviewAnswerUseCase(get(), get()) }
    factory { CompleteInterviewSessionUseCase(get()) }
    factory { DeleteInterviewSessionUseCase(get()) }
    factory { GetStarCoachingUseCase(get()) }
    
    // Auth Use Cases
    factory { GetAuthStateUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { RegisterWithEmailUseCase(get()) }
    factory { LoginWithEmailUseCase(get()) }
    factory { LoginWithGoogleUseCase(get()) }
    factory { LoginWithLinkedInUseCase(get(), get()) }
    factory { LogoutUseCase(get()) }
    factory { UpdateProfileUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { CheckEmailAvailabilityUseCase(get()) }
    
    // LinkedIn Import Use Cases
    factory { GetLinkedInAuthUrlUseCase(get()) }
    factory { ImportLinkedInProfileUseCase(get(), get()) }
    
    // File Upload Use Cases
    factory { UploadResumeFileUseCase(get(), get()) }
    factory { GetSupportedFileTypesUseCase(get()) }
    factory { GetMaxFileSizeUseCase(get()) }
    
    // Phase 2: Job Tracker API Client
    single { JobTrackerApiClient(get()) }
    
    // Phase 3: NOC API Client
    single { NOCApiClient(get()) }
    
    // Phase 3: Job Bank API Client
    single { JobBankApiClient(get()) }
    
    // Phase 2: Tracker Use Cases
    factory<com.vwatek.apply.presentation.tracker.GetJobApplicationsUseCase> { GetJobApplicationsUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.GetJobApplicationByIdUseCase> { GetJobApplicationByIdUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.CreateJobApplicationUseCase> { CreateJobApplicationUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.UpdateJobApplicationUseCase> { UpdateJobApplicationUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.UpdateApplicationStatusUseCase> { UpdateApplicationStatusUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.DeleteJobApplicationUseCase> { DeleteJobApplicationUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.AddApplicationNoteUseCase> { AddApplicationNoteUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.AddApplicationReminderUseCase> { AddApplicationReminderUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.AddApplicationInterviewUseCase> { AddApplicationInterviewUseCaseImpl(get()) }
    factory<com.vwatek.apply.presentation.tracker.GetTrackerStatsUseCase> { GetTrackerStatsUseCaseImpl(get()) }
    
    // ViewModels
    factory { ResumeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { CoverLetterViewModel(get(), get(), get()) }
    factory { InterviewViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { 
        AuthViewModel(
            get(), get(), get(), get(), get(), get(), 
            get(), get(), get(), get(), get(), get(), get()
        )
    }
    factory {
        TrackerViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
    
    // Phase 3: NOC ViewModel
    factory { NOCViewModel(get()) }
    
    // Phase 3: Job Bank ViewModel
    factory { JobBankViewModel(get()) }
}
