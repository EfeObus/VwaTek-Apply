package com.vwatek.apply.domain.usecase

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.data.api.GeminiService
import kotlinx.coroutines.flow.Flow

class GetAllResumesUseCase(
    private val repository: ResumeRepository
) {
    operator fun invoke(): Flow<List<Resume>> = repository.getAllResumes()
}

class GetResumeByIdUseCase(
    private val repository: ResumeRepository
) {
    suspend operator fun invoke(id: String): Resume? = repository.getResumeById(id)
}

class SaveResumeUseCase(
    private val repository: ResumeRepository
) {
    suspend operator fun invoke(resume: Resume) {
        val existing = repository.getResumeById(resume.id)
        if (existing != null) {
            repository.updateResume(resume)
        } else {
            repository.insertResume(resume)
        }
    }
}

class DeleteResumeUseCase(
    private val repository: ResumeRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteResume(id)
}

class AnalyzeResumeUseCase(
    private val geminiService: GeminiService,
    private val analysisRepository: AnalysisRepository
) {
    suspend operator fun invoke(
        resume: Resume,
        jobDescription: String
    ): Result<ResumeAnalysis> {
        return try {
            val analysis = geminiService.analyzeResume(resume.content, jobDescription)
            analysisRepository.insertAnalysis(analysis)
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class OptimizeResumeUseCase(
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        resumeContent: String,
        jobDescription: String
    ): Result<String> {
        return try {
            val optimized = geminiService.optimizeResume(resumeContent, jobDescription)
            Result.success(optimized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GenerateCoverLetterUseCase(
    private val geminiService: GeminiService,
    private val coverLetterRepository: CoverLetterRepository
) {
    suspend operator fun invoke(
        resumeContent: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: CoverLetterTone
    ): Result<CoverLetter> {
        return try {
            val coverLetter = geminiService.generateCoverLetter(
                resumeContent = resumeContent,
                jobTitle = jobTitle,
                companyName = companyName,
                jobDescription = jobDescription,
                tone = tone
            )
            coverLetterRepository.insertCoverLetter(coverLetter)
            Result.success(coverLetter)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAllCoverLettersUseCase(
    private val repository: CoverLetterRepository
) {
    operator fun invoke(): Flow<List<CoverLetter>> = repository.getAllCoverLetters()
}

class DeleteCoverLetterUseCase(
    private val repository: CoverLetterRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteCoverLetter(id)
}
