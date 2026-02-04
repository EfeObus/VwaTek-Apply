package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.LinkedInProfile
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.domain.repository.LinkedInRepository
import kotlinx.datetime.Clock

class AndroidLinkedInRepository : LinkedInRepository {
    
    override suspend fun getAuthorizationUrl(): String {
        // In production, this would return a real LinkedIn OAuth URL
        return "https://www.linkedin.com/oauth/v2/authorization"
    }
    
    override suspend fun exchangeCodeForToken(authCode: String): Result<String> {
        // In production, exchange auth code for access token
        return Result.success("mock_access_token")
    }
    
    override suspend fun getProfile(accessToken: String): Result<LinkedInProfile> {
        // In production, fetch real profile from LinkedIn API
        return Result.success(
            LinkedInProfile(
                id = "linkedin_123",
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@linkedin.com",
                headline = "Software Engineer",
                summary = "Experienced software developer",
                profileImageUrl = null,
                positions = emptyList(),
                education = emptyList()
            )
        )
    }
    
    override suspend fun importProfileAsResume(profile: LinkedInProfile): Resume {
        val content = buildString {
            appendLine("# ${profile.firstName} ${profile.lastName}")
            appendLine()
            profile.headline?.let { appendLine("**$it**") }
            appendLine()
            profile.summary?.let {
                appendLine("## Summary")
                appendLine(it)
                appendLine()
            }
            if (profile.positions.isNotEmpty()) {
                appendLine("## Experience")
                profile.positions.forEach { pos ->
                    appendLine("### ${pos.title} at ${pos.companyName}")
                    appendLine("${pos.startDate} - ${pos.endDate ?: "Present"}")
                    pos.description?.let { appendLine(it) }
                    appendLine()
                }
            }
            if (profile.education.isNotEmpty()) {
                appendLine("## Education")
                profile.education.forEach { edu ->
                    appendLine("### ${edu.schoolName}")
                    edu.degree?.let { appendLine(it) }
                    edu.fieldOfStudy?.let { appendLine(it) }
                    appendLine()
                }
            }
        }
        
        return Resume(
            id = java.util.UUID.randomUUID().toString(),
            userId = null,
            name = "${profile.firstName} ${profile.lastName} - LinkedIn Import",
            content = content,
            industry = null,
            sourceType = ResumeSourceType.LINKEDIN,
            fileName = null,
            fileType = null,
            originalFileData = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
}
