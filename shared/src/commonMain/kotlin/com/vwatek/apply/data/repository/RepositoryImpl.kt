package com.vwatek.apply.data.repository

import com.vwatek.apply.db.VwaTekDatabase
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.domain.repository.InterviewRepository
import com.vwatek.apply.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers

class ResumeRepositoryImpl(
    private val database: VwaTekDatabase
) : ResumeRepository {
    
    private val queries = database.vwaTekDatabaseQueries
    
    override fun getAllResumes(): Flow<List<Resume>> {
        return queries.selectAllResumes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun getResumeById(id: String): Resume? {
        return queries.selectResumeById(id).executeAsOneOrNull()?.toDomain()
    }
    
    override suspend fun insertResume(resume: Resume) {
        queries.insertResume(
            id = resume.id,
            userId = resume.userId,
            name = resume.name,
            content = resume.content,
            industry = resume.industry,
            createdAt = resume.createdAt.toEpochMilliseconds(),
            updatedAt = resume.updatedAt.toEpochMilliseconds()
        )
    }
    
    override suspend fun updateResume(resume: Resume) {
        queries.updateResume(
            name = resume.name,
            content = resume.content,
            industry = resume.industry,
            updatedAt = resume.updatedAt.toEpochMilliseconds(),
            id = resume.id
        )
    }
    
    override suspend fun getResumesByUserId(userId: String): List<Resume> {
        return queries.selectResumesByUserId(userId).executeAsList().map { it.toDomain() }
    }
    
    suspend fun updateResumeUserId(resumeId: String, userId: String) {
        queries.updateResumeUserId(userId = userId, id = resumeId)
    }
    
    override suspend fun deleteResume(id: String) {
        queries.deleteResume(id)
    }
    
    // Version control - stub implementations for SQLDelight (to be implemented when adding DB schema)
    override fun getVersionsByResumeId(resumeId: String): Flow<List<ResumeVersion>> = flowOf(emptyList())
    override suspend fun getVersionById(id: String): ResumeVersion? = null
    override suspend fun insertVersion(version: ResumeVersion) { /* TODO: Add SQLDelight schema */ }
    override suspend fun deleteVersion(id: String) { /* TODO: Add SQLDelight schema */ }
    override suspend fun deleteVersionsByResumeId(resumeId: String) { /* TODO: Add SQLDelight schema */ }
}

class AnalysisRepositoryImpl(
    private val database: VwaTekDatabase
) : AnalysisRepository {
    
    private val queries = database.vwaTekDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }
    
    override fun getAnalysesByResumeId(resumeId: String): Flow<List<ResumeAnalysis>> {
        return queries.selectAnalysesByResumeId(resumeId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain(json) } }
    }
    
    override suspend fun insertAnalysis(analysis: ResumeAnalysis) {
        queries.insertAnalysis(
            id = analysis.id,
            resumeId = analysis.resumeId,
            jobDescription = analysis.jobDescription,
            matchScore = analysis.matchScore.toLong(),
            missingKeywords = json.encodeToString(analysis.missingKeywords),
            recommendations = json.encodeToString(analysis.recommendations),
            createdAt = analysis.createdAt.toEpochMilliseconds()
        )
    }
    
    override suspend fun deleteAnalysis(id: String) {
        queries.deleteAnalysis(id)
    }
}

class CoverLetterRepositoryImpl(
    private val database: VwaTekDatabase
) : CoverLetterRepository {
    
    private val queries = database.vwaTekDatabaseQueries
    
    override fun getAllCoverLetters(): Flow<List<CoverLetter>> {
        return queries.selectAllCoverLetters()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun getCoverLetterById(id: String): CoverLetter? {
        return queries.selectCoverLetterById(id).executeAsOneOrNull()?.toDomain()
    }
    
    override suspend fun insertCoverLetter(coverLetter: CoverLetter) {
        queries.insertCoverLetter(
            id = coverLetter.id,
            resumeId = coverLetter.resumeId,
            jobTitle = coverLetter.jobTitle,
            companyName = coverLetter.companyName,
            content = coverLetter.content,
            tone = coverLetter.tone.name,
            createdAt = coverLetter.createdAt.toEpochMilliseconds()
        )
    }
    
    override suspend fun updateCoverLetter(id: String, content: String) {
        queries.updateCoverLetter(content = content, id = id)
    }
    
    override suspend fun deleteCoverLetter(id: String) {
        queries.deleteCoverLetter(id)
    }
}

class InterviewRepositoryImpl(
    private val database: VwaTekDatabase
) : InterviewRepository {
    
    private val queries = database.vwaTekDatabaseQueries
    
    override fun getAllSessions(): Flow<List<InterviewSession>> {
        return queries.selectAllInterviewSessions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun getSessionById(id: String): InterviewSession? {
        return queries.selectInterviewSessionById(id).executeAsOneOrNull()?.toDomain()
    }
    
    override suspend fun insertSession(session: InterviewSession) {
        queries.insertInterviewSession(
            id = session.id,
            resumeId = session.resumeId,
            jobTitle = session.jobTitle,
            jobDescription = session.jobDescription,
            status = session.status.name,
            createdAt = session.createdAt.toEpochMilliseconds(),
            completedAt = session.completedAt?.toEpochMilliseconds()
        )
    }
    
    override suspend fun updateSessionStatus(id: String, status: String, completedAt: Long?) {
        queries.updateInterviewSessionStatus(status = status, completedAt = completedAt, id = id)
    }
    
    override suspend fun deleteSession(id: String) {
        queries.deleteInterviewSession(id)
    }
    
    override fun getQuestionsBySessionId(sessionId: String): Flow<List<InterviewQuestion>> {
        return queries.selectQuestionsBySessionId(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun insertQuestion(question: InterviewQuestion) {
        queries.insertInterviewQuestion(
            id = question.id,
            sessionId = question.sessionId,
            question = question.question,
            userAnswer = question.userAnswer,
            aiFeedback = question.aiFeedback,
            questionOrder = question.questionOrder.toLong(),
            createdAt = question.createdAt.toEpochMilliseconds()
        )
    }
    
    override suspend fun updateQuestion(id: String, userAnswer: String?, aiFeedback: String?) {
        queries.updateInterviewQuestion(userAnswer = userAnswer, aiFeedback = aiFeedback, id = id)
    }
}

class SettingsRepositoryImpl(
    private val database: VwaTekDatabase
) : SettingsRepository {
    
    private val queries = database.vwaTekDatabaseQueries
    
    override suspend fun getSetting(key: String): String? {
        return queries.selectSetting(key).executeAsOneOrNull()
    }
    
    override suspend fun setSetting(key: String, value: String) {
        queries.insertOrReplaceSetting(key, value)
    }
    
    override suspend fun deleteSetting(key: String) {
        queries.deleteSetting(key)
    }
}

// Extension functions to convert database entities to domain models
private fun com.vwatek.apply.db.Resume.toDomain(): Resume {
    return Resume(
        id = id,
        userId = userId,
        name = name,
        content = content,
        industry = industry,
        sourceType = com.vwatek.apply.domain.model.ResumeSourceType.MANUAL, // Default for DB records
        fileName = null,
        fileType = null,
        originalFileData = null,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

private fun com.vwatek.apply.db.ResumeAnalysis.toDomain(json: Json): ResumeAnalysis {
    return ResumeAnalysis(
        id = id,
        resumeId = resumeId,
        jobDescription = jobDescription,
        matchScore = matchScore.toInt(),
        missingKeywords = json.decodeFromString(missingKeywords),
        recommendations = json.decodeFromString(recommendations),
        createdAt = Instant.fromEpochMilliseconds(createdAt)
    )
}

private fun com.vwatek.apply.db.CoverLetter.toDomain(): CoverLetter {
    return CoverLetter(
        id = id,
        resumeId = resumeId,
        jobTitle = jobTitle,
        companyName = companyName,
        content = content,
        tone = CoverLetterTone.valueOf(tone),
        createdAt = Instant.fromEpochMilliseconds(createdAt)
    )
}

private fun com.vwatek.apply.db.InterviewSession.toDomain(): InterviewSession {
    return InterviewSession(
        id = id,
        resumeId = resumeId,
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        status = InterviewStatus.valueOf(status),
        questions = emptyList(),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        completedAt = completedAt?.let { Instant.fromEpochMilliseconds(it) }
    )
}

private fun com.vwatek.apply.db.InterviewQuestion.toDomain(): InterviewQuestion {
    return InterviewQuestion(
        id = id,
        sessionId = sessionId,
        question = question,
        userAnswer = userAnswer,
        aiFeedback = aiFeedback,
        questionOrder = questionOrder.toInt(),
        createdAt = Instant.fromEpochMilliseconds(createdAt)
    )
}
