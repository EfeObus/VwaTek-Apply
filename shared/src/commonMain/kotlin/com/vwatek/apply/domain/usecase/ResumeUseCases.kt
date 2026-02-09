package com.vwatek.apply.domain.usecase

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.ATSAnalysis
import com.vwatek.apply.domain.model.ATSIssue
import com.vwatek.apply.domain.model.ATSRecommendation
import com.vwatek.apply.domain.model.ImpactBullet
import com.vwatek.apply.domain.model.XYZFormat
import com.vwatek.apply.domain.model.GrammarIssue
import com.vwatek.apply.domain.model.IssueSeverity
import com.vwatek.apply.domain.model.GrammarIssueType
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.data.api.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

// Version Control Use Cases
class GetResumeVersionsUseCase(
    private val repository: ResumeRepository
) {
    operator fun invoke(resumeId: String): Flow<List<ResumeVersion>> = 
        repository.getVersionsByResumeId(resumeId)
}

class GetResumeVersionByIdUseCase(
    private val repository: ResumeRepository
) {
    suspend operator fun invoke(id: String): ResumeVersion? = repository.getVersionById(id)
}

@OptIn(ExperimentalUuidApi::class)
class CreateResumeVersionUseCase(
    private val repository: ResumeRepository
) {
    suspend operator fun invoke(
        resumeId: String,
        content: String,
        changeDescription: String
    ): ResumeVersion {
        // Get current version count to determine version number
        val existingVersions = repository.getVersionsByResumeId(resumeId).first()
        val nextVersionNumber = (existingVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1
        
        val version = ResumeVersion(
            id = Uuid.random().toString(),
            resumeId = resumeId,
            versionNumber = nextVersionNumber,
            content = content,
            changeDescription = changeDescription,
            createdAt = Clock.System.now()
        )
        
        repository.insertVersion(version)
        
        // Update resume with current version ID
        val resume = repository.getResumeById(resumeId)
        if (resume != null) {
            repository.updateResume(resume.copy(currentVersionId = version.id))
        }
        
        return version
    }
}

class RestoreResumeVersionUseCase(
    private val repository: ResumeRepository,
    private val createVersionUseCase: CreateResumeVersionUseCase
) {
    suspend operator fun invoke(
        resumeId: String,
        versionId: String
    ): Result<Resume> {
        return try {
            val version = repository.getVersionById(versionId) 
                ?: return Result.failure(Exception("Version not found"))
            
            val resume = repository.getResumeById(resumeId)
                ?: return Result.failure(Exception("Resume not found"))
            
            // Create a new version with the restored content
            createVersionUseCase(
                resumeId = resumeId,
                content = version.content,
                changeDescription = "Restored from version ${version.versionNumber}"
            )
            
            // Update resume with restored content
            val updatedResume = resume.copy(
                content = version.content,
                updatedAt = Clock.System.now()
            )
            repository.updateResume(updatedResume)
            
            Result.success(updatedResume)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteResumeVersionUseCase(
    private val repository: ResumeRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteVersion(id)
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

class PerformATSAnalysisUseCase(
    private val geminiService: GeminiService
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        resumeContent: String,
        resumeId: String,
        jobDescription: String? = null
    ): Result<ATSAnalysis> {
        return try {
            val result = geminiService.performATSAnalysis(resumeContent, jobDescription)
            
            val analysis = ATSAnalysis(
                id = Uuid.random().toString(),
                resumeId = resumeId,
                overallScore = result.overallScore,
                formattingScore = result.formattingScore,
                keywordScore = result.keywordScore,
                structureScore = result.structureScore,
                readabilityScore = result.readabilityScore,
                formattingIssues = result.formattingIssues.map { 
                    ATSIssue(
                        severity = try { IssueSeverity.valueOf(it.severity) } catch (e: Exception) { IssueSeverity.MEDIUM },
                        category = it.category,
                        description = it.description,
                        suggestion = it.suggestion
                    )
                },
                structureIssues = result.structureIssues.map {
                    ATSIssue(
                        severity = try { IssueSeverity.valueOf(it.severity) } catch (e: Exception) { IssueSeverity.MEDIUM },
                        category = it.category,
                        description = it.description,
                        suggestion = it.suggestion
                    )
                },
                keywordDensity = result.keywordDensity,
                recommendations = result.recommendations.map {
                    ATSRecommendation(
                        priority = it.priority,
                        category = it.category,
                        title = it.title,
                        description = it.description,
                        impact = it.impact
                    )
                },
                impactBullets = result.impactBullets.map {
                    ImpactBullet(
                        original = it.original,
                        improved = it.improved,
                        xyzFormat = it.xyzFormat?.let { xyz ->
                            XYZFormat(
                                accomplished = xyz.accomplished,
                                measuredBy = xyz.measuredBy,
                                byDoing = xyz.byDoing
                            )
                        }
                    )
                },
                grammarIssues = result.grammarIssues.map {
                    GrammarIssue(
                        original = it.original,
                        corrected = it.corrected,
                        explanation = it.explanation,
                        type = try { GrammarIssueType.valueOf(it.type) } catch (e: Exception) { GrammarIssueType.GRAMMAR }
                    )
                },
                createdAt = Clock.System.now()
            )
            
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GenerateImpactBulletsUseCase(
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        experiences: List<String>,
        jobContext: String
    ): Result<List<ImpactBullet>> {
        return try {
            val results = geminiService.generateImpactBullets(experiences, jobContext)
            val bullets = results.map {
                ImpactBullet(
                    original = it.original,
                    improved = it.improved,
                    xyzFormat = if (it.accomplished.isNotBlank()) {
                        XYZFormat(
                            accomplished = it.accomplished,
                            measuredBy = it.measuredBy,
                            byDoing = it.byDoing
                        )
                    } else null
                )
            }
            Result.success(bullets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AnalyzeGrammarUseCase(
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(text: String): Result<List<GrammarIssue>> {
        return try {
            val results = geminiService.analyzeGrammarAndTone(text)
            val issues = results.map {
                GrammarIssue(
                    original = it.original,
                    corrected = it.corrected,
                    explanation = it.explanation,
                    type = try { GrammarIssueType.valueOf(it.type) } catch (e: Exception) { GrammarIssueType.GRAMMAR }
                )
            }
            Result.success(issues)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RewriteSectionUseCase(
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        sectionType: String,
        sectionContent: String,
        targetRole: String? = null,
        targetIndustry: String? = null,
        style: String = "professional"
    ): Result<SectionRewriteResult> {
        return try {
            val result = geminiService.rewriteResumeSection(
                sectionType = sectionType,
                sectionContent = sectionContent,
                targetRole = targetRole,
                targetIndustry = targetIndustry,
                style = style
            )
            Result.success(
                SectionRewriteResult(
                    rewrittenContent = result.rewrittenContent,
                    changes = result.changes,
                    keywords = result.keywords,
                    tips = result.tips
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@kotlinx.serialization.Serializable
data class SectionRewriteResult(
    val rewrittenContent: String,
    val changes: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)