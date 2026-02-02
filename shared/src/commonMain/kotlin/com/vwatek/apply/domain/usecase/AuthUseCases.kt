package com.vwatek.apply.domain.usecase

import com.vwatek.apply.domain.model.Address
import com.vwatek.apply.domain.model.AuthState
import com.vwatek.apply.domain.model.LinkedInProfile
import com.vwatek.apply.domain.model.RegistrationData
import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.domain.model.User
import com.vwatek.apply.domain.model.FileUploadResult
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.GoogleUserData
import com.vwatek.apply.domain.repository.LinkedInRepository
import com.vwatek.apply.domain.repository.FileUploadRepository
import com.vwatek.apply.domain.repository.ResumeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Authentication Use Cases
class GetAuthStateUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<AuthState> = authRepository.getAuthState()
}

class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): User? = authRepository.getCurrentUser()
}

class RegisterWithEmailUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String? = null,
        street: String? = null,
        city: String? = null,
        state: String? = null,
        zipCode: String? = null,
        country: String? = null
    ): Result<User> {
        // Validation
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        if (!isValidPassword(password)) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, and number"))
        }
        if (firstName.isBlank()) {
            return Result.failure(IllegalArgumentException("First name is required"))
        }
        if (lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Last name is required"))
        }

        val address = if (street != null || city != null || state != null || zipCode != null || country != null) {
            Address(street, city, state, zipCode, country)
        } else null

        return authRepository.registerWithEmail(
            RegistrationData(
                email = email.lowercase().trim(),
                password = password,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phone = phone?.trim(),
                address = address
            )
        )
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        return emailRegex.matches(email)
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }
}

class LoginWithEmailUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, rememberMe: Boolean = true): Result<User> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email is required"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password is required"))
        }
        return authRepository.loginWithEmail(email.lowercase().trim(), password, rememberMe)
    }
}

class LoginWithGoogleUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        idToken: String,
        email: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        profilePicture: String? = null
    ): Result<User> {
        if (idToken.isBlank() && email.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Google authentication failed"))
        }
        
        val userInfo = if (!email.isNullOrBlank()) {
            GoogleUserData(
                email = email,
                firstName = firstName ?: "",
                lastName = lastName ?: "",
                profilePicture = profilePicture
            )
        } else null
        
        return authRepository.loginWithGoogle(idToken, userInfo)
    }
}

class LoginWithLinkedInUseCase(
    private val authRepository: AuthRepository,
    private val linkedInRepository: LinkedInRepository
) {
    suspend operator fun invoke(authCode: String): Result<User> {
        if (authCode.isBlank()) {
            return Result.failure(IllegalArgumentException("LinkedIn authentication failed"))
        }
        return authRepository.loginWithLinkedIn(authCode)
    }
}

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke() = authRepository.logout()
}

class UpdateProfileUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(user: User): Result<User> {
        return authRepository.updateProfile(user)
    }
}

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email is required"))
        }
        return authRepository.resetPassword(email.lowercase().trim())
    }
}

class CheckEmailAvailabilityUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Boolean {
        return authRepository.isEmailAvailable(email.lowercase().trim())
    }
}

// LinkedIn Import Use Cases
class GetLinkedInAuthUrlUseCase(private val linkedInRepository: LinkedInRepository) {
    suspend operator fun invoke(): String = linkedInRepository.getAuthorizationUrl()
}

class ImportLinkedInProfileUseCase(
    private val linkedInRepository: LinkedInRepository,
    private val resumeRepository: ResumeRepository
) {
    suspend operator fun invoke(authCode: String): Result<Resume> {
        return try {
            val tokenResult = linkedInRepository.exchangeCodeForToken(authCode)
            val accessToken = tokenResult.getOrThrow()
            
            val profileResult = linkedInRepository.getProfile(accessToken)
            val profile = profileResult.getOrThrow()
            
            val resume = linkedInRepository.importProfileAsResume(profile)
            resumeRepository.insertResume(resume)
            
            Result.success(resume)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ConvertLinkedInToResumeUseCase {
    @OptIn(ExperimentalUuidApi::class)
    operator fun invoke(profile: LinkedInProfile): Resume {
        val content = buildString {
            appendLine("${profile.firstName} ${profile.lastName}")
            profile.headline?.let { appendLine(it) }
            appendLine()
            
            profile.summary?.let {
                appendLine("SUMMARY")
                appendLine(it)
                appendLine()
            }
            
            if (profile.positions.isNotEmpty()) {
                appendLine("EXPERIENCE")
                profile.positions.forEach { position ->
                    appendLine("${position.title} at ${position.companyName}")
                    val dateRange = buildString {
                        position.startDate?.let { append(it) }
                        append(" - ")
                        if (position.isCurrent) append("Present") else position.endDate?.let { append(it) }
                    }
                    if (dateRange.isNotBlank()) appendLine(dateRange.trim())
                    position.location?.let { appendLine(it) }
                    position.description?.let { appendLine(it) }
                    appendLine()
                }
            }
            
            if (profile.education.isNotEmpty()) {
                appendLine("EDUCATION")
                profile.education.forEach { edu ->
                    appendLine(edu.schoolName)
                    val degreeField = listOfNotNull(edu.degree, edu.fieldOfStudy).joinToString(", ")
                    if (degreeField.isNotBlank()) appendLine(degreeField)
                    val years = listOfNotNull(edu.startYear, edu.endYear).joinToString(" - ")
                    if (years.isNotBlank()) appendLine(years)
                    appendLine()
                }
            }
            
            if (profile.skills.isNotEmpty()) {
                appendLine("SKILLS")
                appendLine(profile.skills.joinToString(", "))
            }
        }
        
        val now = Clock.System.now()
        return Resume(
            id = Uuid.random().toString(),
            userId = null,
            name = "${profile.firstName} ${profile.lastName} - LinkedIn",
            content = content,
            industry = null,
            sourceType = ResumeSourceType.LINKEDIN,
            fileName = null,
            fileType = null,
            originalFileData = null,
            createdAt = now,
            updatedAt = now
        )
    }
}

// File Upload Use Cases
class UploadResumeFileUseCase(
    private val fileUploadRepository: FileUploadRepository,
    private val resumeRepository: ResumeRepository
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        fileData: ByteArray,
        fileName: String,
        fileType: String,
        userId: String? = null
    ): Result<Resume> {
        return try {
            // Check file size
            val maxSize = fileUploadRepository.getMaxFileSizeBytes()
            if (fileData.size > maxSize) {
                return Result.failure(IllegalArgumentException("File size exceeds maximum allowed (${maxSize / 1024 / 1024}MB)"))
            }
            
            // Check file type
            val supportedTypes = fileUploadRepository.getSupportedFileTypes()
            val normalizedType = fileType.lowercase().removePrefix(".")
            if (!supportedTypes.any { it.equals(normalizedType, ignoreCase = true) }) {
                return Result.failure(IllegalArgumentException("Unsupported file type. Supported: ${supportedTypes.joinToString(", ")}"))
            }
            
            // Extract text from file
            val extractResult = fileUploadRepository.extractTextFromFile(fileData, normalizedType)
            val extractedContent = extractResult.getOrThrow()
            
            if (extractedContent.isBlank()) {
                return Result.failure(IllegalArgumentException("Could not extract text from file"))
            }
            
            // Create resume
            val now = Clock.System.now()
            val resume = Resume(
                id = Uuid.random().toString(),
                userId = userId,
                name = fileName.substringBeforeLast("."),
                content = extractedContent,
                industry = null,
                sourceType = ResumeSourceType.UPLOADED,
                fileName = fileName,
                fileType = normalizedType,
                originalFileData = null, // Don't store raw file data in memory
                createdAt = now,
                updatedAt = now
            )
            
            resumeRepository.insertResume(resume)
            Result.success(resume)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetSupportedFileTypesUseCase(private val fileUploadRepository: FileUploadRepository) {
    operator fun invoke(): List<String> = fileUploadRepository.getSupportedFileTypes()
}

class GetMaxFileSizeUseCase(private val fileUploadRepository: FileUploadRepository) {
    operator fun invoke(): Long = fileUploadRepository.getMaxFileSizeBytes()
}
