package com.vwatek.apply.domain.usecase

import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.domain.repository.InterviewRepository
import com.vwatek.apply.data.api.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

class GetAllInterviewSessionsUseCase(
    private val repository: InterviewRepository
) {
    operator fun invoke(): Flow<List<InterviewSession>> = repository.getAllSessions()
}

class GetInterviewSessionByIdUseCase(
    private val repository: InterviewRepository
) {
    suspend operator fun invoke(id: String): InterviewSession? = repository.getSessionById(id)
}

class StartInterviewSessionUseCase(
    private val repository: InterviewRepository,
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        resumeContent: String?,
        jobTitle: String,
        jobDescription: String
    ): Result<InterviewSession> {
        return try {
            val session = geminiService.startMockInterview(
                resumeContent = resumeContent,
                jobTitle = jobTitle,
                jobDescription = jobDescription
            )
            repository.insertSession(session)
            session.questions.forEach { question ->
                repository.insertQuestion(question)
            }
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SubmitInterviewAnswerUseCase(
    private val repository: InterviewRepository,
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        questionId: String,
        userAnswer: String,
        question: String,
        jobTitle: String
    ): Result<String> {
        return try {
            val feedback = geminiService.getInterviewFeedback(
                question = question,
                answer = userAnswer,
                jobTitle = jobTitle
            )
            repository.updateQuestion(questionId, userAnswer, feedback)
            Result.success(feedback)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CompleteInterviewSessionUseCase(
    private val repository: InterviewRepository
) {
    suspend operator fun invoke(sessionId: String) {
        val completedAt = Clock.System.now().toEpochMilliseconds()
        repository.updateSessionStatus(sessionId, InterviewStatus.COMPLETED.name, completedAt)
    }
}

class DeleteInterviewSessionUseCase(
    private val repository: InterviewRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteSession(id)
}

class GetStarCoachingUseCase(
    private val geminiService: GeminiService
) {
    suspend operator fun invoke(
        experience: String,
        jobContext: String
    ): Result<StarResponse> {
        return try {
            val response = geminiService.getStarCoaching(experience, jobContext)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StarResponse(
    val situation: String,
    val task: String,
    val action: String,
    val result: String,
    val suggestions: List<String>
)
