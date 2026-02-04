package com.vwatek.apply.di

import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.presentation.resume.ResumeViewModel
import com.vwatek.apply.presentation.coverletter.CoverLetterViewModel
import com.vwatek.apply.presentation.interview.InterviewViewModel
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
    
    fun getAuthViewModel(): AuthViewModel = authViewModel
    fun getResumeViewModel(): ResumeViewModel = resumeViewModel
    fun getCoverLetterViewModel(): CoverLetterViewModel = coverLetterViewModel
    fun getInterviewViewModel(): InterviewViewModel = interviewViewModel
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
