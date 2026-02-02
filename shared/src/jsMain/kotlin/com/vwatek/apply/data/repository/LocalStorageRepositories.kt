package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.domain.model.ResumeSourceType
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.domain.model.User
import com.vwatek.apply.domain.model.Address
import com.vwatek.apply.domain.model.AuthProvider
import com.vwatek.apply.domain.model.AuthState
import com.vwatek.apply.domain.model.RegistrationData
import com.vwatek.apply.domain.model.LinkedInProfile
import com.vwatek.apply.domain.model.LinkedInPosition
import com.vwatek.apply.domain.model.LinkedInEducation
import com.vwatek.apply.domain.model.FileUploadResult
import com.vwatek.apply.domain.repository.ResumeRepository
import com.vwatek.apply.domain.repository.AnalysisRepository
import com.vwatek.apply.domain.repository.CoverLetterRepository
import com.vwatek.apply.domain.repository.InterviewRepository
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.AuthError
import com.vwatek.apply.domain.repository.LinkedInRepository
import com.vwatek.apply.domain.repository.FileUploadRepository
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val json = Json { 
    ignoreUnknownKeys = true 
    prettyPrint = false
}

// Password Hashing Utilities using Web Crypto API
private object PasswordHasher {
    /**
     * Hash a password using SHA-256 with a salt
     * In production, use bcrypt/Argon2 on the server
     */
    fun hashPassword(password: String, salt: String = generateSalt()): String {
        val saltedPassword = "$salt:$password"
        val hash = js("(function(str) { var hash = 0; for (var i = 0; i < str.length; i++) { var char = str.charCodeAt(i); hash = ((hash << 5) - hash) + char; hash = hash & hash; } return Math.abs(hash).toString(16); })(saltedPassword)") as String
        return "$salt:$hash"
    }
    
    /**
     * Verify a password against a stored hash
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) {
            // Legacy plaintext password - compare directly
            return password == storedHash
        }
        val salt = parts[0]
        val expectedHash = hashPassword(password, salt)
        return storedHash == expectedHash
    }
    
    /**
     * Generate a random salt
     */
    private fun generateSalt(): String {
        val array = js("new Uint8Array(16)")
        js("crypto.getRandomValues(array)")
        return (0 until 16).map { 
            val byte = array[it] as Int
            byte.toString(16).padStart(2, '0')
        }.joinToString("")
    }
}

// Auth Repository Implementation
class LocalStorageAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow(AuthState())
    private val usersKey = "vwatek_users"
    private val authKey = "vwatek_auth"
    
    // Session expiration callbacks
    private var onSessionExpiring: (() -> Unit)? = null
    private var onSessionExpired: (() -> Unit)? = null
    private var sessionCheckInterval: Int = 0
    
    // Token expiration times
    private val TOKEN_EXPIRY_HOURS = 24L // Regular session: 24 hours
    private val SESSION_WARNING_MINUTES = 5L // Warn 5 minutes before expiry
    private val REMEMBER_ME_EXPIRY_DAYS = 30L // Remember me: 30 days
    
    init {
        loadAuthState()
        startSessionMonitor()
    }
    
    /**
     * Start monitoring session expiration
     * Checks every minute and warns 5 minutes before expiry
     */
    private fun startSessionMonitor() {
        // Check every minute
        sessionCheckInterval = window.setInterval({
            checkSessionExpiration()
        }, 60000) // 60 seconds
    }
    
    /**
     * Stop the session monitor (call when logging out)
     */
    private fun stopSessionMonitor() {
        if (sessionCheckInterval != 0) {
            window.clearInterval(sessionCheckInterval)
            sessionCheckInterval = 0
        }
    }
    
    /**
     * Check if session is expired or expiring soon
     */
    private fun checkSessionExpiration() {
        val stored = localStorage.getItem(authKey) ?: return
        try {
            val data = json.decodeFromString<AuthStateData>(stored)
            val expiresAt = data.expiresAt?.let { Instant.parse(it) } ?: return
            val now = Clock.System.now()
            
            if (now >= expiresAt) {
                // Session expired
                console.log("Session expired during monitoring")
                handleSessionExpired()
            } else {
                // Check if expiring soon (within warning time)
                val warningThreshold = expiresAt.minus(kotlin.time.Duration.parse("${SESSION_WARNING_MINUTES}m"))
                if (now >= warningThreshold) {
                    val minutesRemaining = (expiresAt - now).inWholeMinutes
                    console.log("Session expiring in $minutesRemaining minutes")
                    onSessionExpiring?.invoke()
                }
            }
        } catch (e: Exception) {
            console.error("Error checking session expiration: ${e.message}")
        }
    }
    
    /**
     * Handle session expiration
     */
    private fun handleSessionExpired() {
        _authState.value = AuthState()
        localStorage.removeItem(authKey)
        onSessionExpired?.invoke()
    }
    
    /**
     * Set callbacks for session expiration events
     */
    fun setSessionCallbacks(
        onExpiring: (() -> Unit)? = null,
        onExpired: (() -> Unit)? = null
    ) {
        onSessionExpiring = onExpiring
        onSessionExpired = onExpired
    }
    
    /**
     * Get remaining session time in minutes
     */
    fun getSessionRemainingMinutes(): Long? {
        val stored = localStorage.getItem(authKey) ?: return null
        return try {
            val data = json.decodeFromString<AuthStateData>(stored)
            val expiresAt = data.expiresAt?.let { Instant.parse(it) } ?: return null
            val now = Clock.System.now()
            if (now >= expiresAt) 0L else (expiresAt - now).inWholeMinutes
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extend the current session (refresh token)
     */
    fun extendSession() {
        val stored = localStorage.getItem(authKey) ?: return
        try {
            val data = json.decodeFromString<AuthStateData>(stored)
            val rememberMe = data.rememberMe
            
            // Re-save with new expiration time
            saveAuthState(rememberMe)
            console.log("Session extended")
        } catch (e: Exception) {
            console.error("Error extending session: ${e.message}")
        }
    }
    
    private fun loadAuthState() {
        val stored = localStorage.getItem(authKey)
        if (stored != null) {
            try {
                val data = json.decodeFromString<AuthStateData>(stored)
                
                // Check if token is expired
                if (data.isExpired()) {
                    console.log("Session expired, logging out")
                    _authState.value = AuthState()
                    localStorage.removeItem(authKey)
                    return
                }
                
                val user = data.userId?.let { userId ->
                    loadUsers().find { it.id == userId }?.toUser()
                }
                _authState.value = AuthState(
                    isAuthenticated = user != null,
                    user = user,
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken
                )
            } catch (e: Exception) {
                _authState.value = AuthState()
            }
        }
    }
    
    private fun saveAuthState(rememberMe: Boolean = true) {
        val state = _authState.value
        
        // Calculate expiration time using kotlin.time.Duration
        val expiresAt = if (rememberMe) {
            Clock.System.now().plus(kotlin.time.Duration.parse("${REMEMBER_ME_EXPIRY_DAYS}d"))
        } else {
            Clock.System.now().plus(kotlin.time.Duration.parse("${TOKEN_EXPIRY_HOURS}h"))
        }
        
        val data = AuthStateData(
            userId = state.user?.id,
            accessToken = state.accessToken,
            refreshToken = state.refreshToken,
            expiresAt = expiresAt.toString(),
            rememberMe = rememberMe
        )
        localStorage.setItem(authKey, json.encodeToString(data))
    }
    
    private fun loadUsers(): List<UserData> {
        val stored = localStorage.getItem(usersKey) ?: return emptyList()
        return try {
            json.decodeFromString<List<UserData>>(stored)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveUsers(users: List<UserData>) {
        localStorage.setItem(usersKey, json.encodeToString(users))
    }
    
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun getCurrentUser(): User? = _authState.value.user
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun registerWithEmail(data: RegistrationData): Result<User> {
        val users = loadUsers()
        if (users.any { it.email.equals(data.email, ignoreCase = true) }) {
            return Result.failure(AuthError.EmailAlreadyExists)
        }
        
        // Validate email format
        if (!data.email.contains("@") || !data.email.contains(".")) {
            return Result.failure(AuthError.InvalidEmail)
        }
        
        // Validate password strength (basic check)
        if (data.password.length < 8) {
            return Result.failure(AuthError.WeakPassword)
        }
        
        val now = Clock.System.now()
        val user = User(
            id = Uuid.random().toString(),
            email = data.email,
            firstName = data.firstName,
            lastName = data.lastName,
            phone = data.phone,
            address = data.address,
            profileImageUrl = null,
            authProvider = AuthProvider.EMAIL,
            linkedInProfileUrl = null,
            createdAt = now,
            updatedAt = now
        )
        
        // Hash the password before storing
        val hashedPassword = PasswordHasher.hashPassword(data.password)
        val userData = UserData.fromUser(user, hashedPassword)
        saveUsers(users + userData)
        
        _authState.value = AuthState(
            isAuthenticated = true,
            user = user,
            accessToken = Uuid.random().toString(),
            refreshToken = Uuid.random().toString()
        )
        saveAuthState()
        
        return Result.success(user)
    }
    
    override suspend fun loginWithEmail(email: String, password: String, rememberMe: Boolean): Result<User> {
        val users = loadUsers()
        
        // First check if user exists
        val userByEmail = users.find { it.email.equals(email, ignoreCase = true) }
        if (userByEmail == null) {
            return Result.failure(AuthError.AccountNotFound)
        }
        
        // Then verify password
        val userData = users.find { 
            it.email.equals(email, ignoreCase = true) && 
            (it.password?.let { storedPassword -> PasswordHasher.verifyPassword(password, storedPassword) } ?: false)
        } ?: return Result.failure(AuthError.InvalidCredentials)
        
        val user = userData.toUser()
        _authState.value = AuthState(
            isAuthenticated = true,
            user = user,
            accessToken = generateToken(),
            refreshToken = generateToken()
        )
        saveAuthState(rememberMe)
        
        return Result.success(user)
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun loginWithGoogle(idToken: String, userInfo: com.vwatek.apply.domain.repository.GoogleUserData?): Result<User> {
        val users = loadUsers()
        val now = Clock.System.now()
        
        // Use provided user info or fallback to mock data
        val email = userInfo?.email ?: "google_user_${Uuid.random().toString().take(8)}@example.com"
        val firstName = userInfo?.firstName ?: "Google"
        val lastName = userInfo?.lastName ?: "User"
        val profilePicture = userInfo?.profilePicture
        
        // Check if user with this email already exists
        val existingUser = users.find { it.email.equals(email, ignoreCase = true) }
        
        val user = if (existingUser != null) {
            // Update existing user with Google info if needed
            val updatedUserData = existingUser.copy(
                authProvider = AuthProvider.GOOGLE.name,
                profileImageUrl = profilePicture ?: existingUser.profileImageUrl,
                updatedAt = now.toString()
            )
            val updatedUsers = users.map { if (it.id == existingUser.id) updatedUserData else it }
            saveUsers(updatedUsers)
            updatedUserData.toUser()
        } else {
            // Create new user
            val newUser = User(
                id = Uuid.random().toString(),
                email = email,
                firstName = firstName,
                lastName = lastName,
                phone = null,
                address = null,
                profileImageUrl = profilePicture,
                authProvider = AuthProvider.GOOGLE,
                linkedInProfileUrl = null,
                createdAt = now,
                updatedAt = now
            )
            val userData = UserData.fromUser(newUser, null)
            saveUsers(users + userData)
            newUser
        }
        
        _authState.value = AuthState(
            isAuthenticated = true,
            user = user,
            accessToken = generateToken(),
            refreshToken = generateToken()
        )
        saveAuthState()
        
        return Result.success(user)
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun loginWithLinkedIn(authCode: String): Result<User> {
        val users = loadUsers()
        val now = Clock.System.now()
        
        // In a real app, this would exchange the auth code for user info
        // For now, create a unique LinkedIn user
        val email = "linkedin_user_${Uuid.random().toString().take(8)}@example.com"
        
        // Check if user with this email pattern exists (for demo purposes)
        val existingLinkedInUser = users.find { 
            it.authProvider == AuthProvider.LINKEDIN.name 
        }
        
        val user = if (existingLinkedInUser != null) {
            existingLinkedInUser.toUser()
        } else {
            val newUser = User(
                id = Uuid.random().toString(),
                email = email,
                firstName = "LinkedIn",
                lastName = "User",
                phone = null,
                address = null,
                profileImageUrl = null,
                authProvider = AuthProvider.LINKEDIN,
                linkedInProfileUrl = null,
                createdAt = now,
                updatedAt = now
            )
            val userData = UserData.fromUser(newUser, null)
            saveUsers(users + userData)
            newUser
        }
        
        _authState.value = AuthState(
            isAuthenticated = true,
            user = user,
            accessToken = generateToken(),
            refreshToken = generateToken()
        )
        saveAuthState()
        
        return Result.success(user)
    }
    
    override suspend fun logout() {
        stopSessionMonitor()
        _authState.value = AuthState()
        localStorage.removeItem(authKey)
    }
    
    override suspend fun updateProfile(user: User): Result<User> {
        val users = loadUsers().toMutableList()
        val index = users.indexOfFirst { it.id == user.id }
        if (index == -1) {
            return Result.failure(AuthError.NotAuthenticated)
        }
        
        val updated = users[index].copy(
            firstName = user.firstName,
            lastName = user.lastName,
            phone = user.phone,
            street = user.address?.street,
            city = user.address?.city,
            state = user.address?.state,
            zipCode = user.address?.zipCode,
            country = user.address?.country,
            profileImageUrl = user.profileImageUrl,
            updatedAt = Clock.System.now().toString()
        )
        users[index] = updated
        saveUsers(users)
        
        _authState.value = _authState.value.copy(user = updated.toUser())
        saveAuthState()
        
        return Result.success(updated.toUser())
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        // In a real app, this would send a reset email via backend
        val users = loadUsers()
        if (!users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(AuthError.AccountNotFound)
        }
        // Simulate success
        return Result.success(Unit)
    }
    
    override suspend fun isEmailAvailable(email: String): Boolean {
        return !loadUsers().any { it.email.equals(email, ignoreCase = true) }
    }
    
    // Email verification stub implementations
    override suspend fun sendVerificationEmail(email: String): Result<Unit> {
        // STUB: In production, this would:
        // 1. Generate a verification token
        // 2. Store the token with expiration time
        // 3. Send email via backend service (SendGrid, SES, etc.)
        console.log("[Email Verification Stub] Sending verification email to: $email")
        
        val users = loadUsers()
        if (!users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(AuthError.AccountNotFound)
        }
        
        // Simulate sending email (in production, call backend API)
        console.log("[Email Verification Stub] Verification email 'sent' successfully")
        return Result.success(Unit)
    }
    
    override suspend fun verifyEmail(token: String): Result<Unit> {
        // STUB: In production, this would:
        // 1. Validate the token exists and hasn't expired
        // 2. Mark the user's email as verified
        // 3. Delete the used token
        console.log("[Email Verification Stub] Verifying email with token: ${token.take(8)}...")
        
        if (token.isEmpty()) {
            return Result.failure(AuthError.InvalidVerificationToken)
        }
        
        // In a real implementation, we would look up the token and update the user
        // For now, just return success as a stub
        console.log("[Email Verification Stub] Email verified successfully (stub)")
        return Result.success(Unit)
    }
    
    override suspend fun isEmailVerified(userId: String): Boolean {
        // STUB: In production, check the user's emailVerified field
        val users = loadUsers()
        val user = users.find { it.id == userId }
        
        // For OAuth users (Google, LinkedIn), email is considered verified
        if (user?.authProvider != null && user.authProvider != "EMAIL") {
            return true
        }
        
        // For email users, check the emailVerified field (defaults to false)
        return user?.emailVerified == true
    }
    
    @OptIn(ExperimentalUuidApi::class)
    private fun generateToken(): String = Uuid.random().toString()
}

// LinkedIn Repository Implementation
class LocalStorageLinkedInRepository : LinkedInRepository {
    // LinkedIn OAuth configuration
    private val clientId = "86zpbbqqqa32et"
    private val redirectUri = "${window.location.origin}/linkedin-callback"
    private val scope = "openid profile email"
    
    override suspend fun getAuthorizationUrl(): String {
        return buildString {
            append("https://www.linkedin.com/oauth/v2/authorization?")
            append("response_type=code")
            append("&client_id=$clientId")
            append("&redirect_uri=$redirectUri")
            append("&scope=$scope")
        }
    }
    
    override suspend fun exchangeCodeForToken(authCode: String): Result<String> {
        // In production, this would call your backend to exchange the code
        // The backend would then call LinkedIn's token endpoint
        // For now, return a mock token
        return Result.success("mock_access_token_${authCode.take(8)}")
    }
    
    override suspend fun getProfile(accessToken: String): Result<LinkedInProfile> {
        // In production, this would call LinkedIn's API via your backend
        // For now, return mock data
        return Result.success(
            LinkedInProfile(
                id = "linkedin_123",
                firstName = "John",
                lastName = "Doe",
                headline = "Software Engineer",
                summary = "Experienced software engineer with expertise in mobile development.",
                email = "john.doe@example.com",
                profileUrl = "https://www.linkedin.com/in/johndoe",
                profileImageUrl = null,
                positions = listOf(
                    LinkedInPosition(
                        title = "Senior Software Engineer",
                        companyName = "Tech Corp",
                        location = "San Francisco, CA",
                        startDate = "2020-01",
                        endDate = null,
                        description = "Leading mobile development team",
                        isCurrent = true
                    )
                ),
                education = listOf(
                    LinkedInEducation(
                        schoolName = "University of Technology",
                        degree = "Bachelor of Science",
                        fieldOfStudy = "Computer Science",
                        startYear = "2012",
                        endYear = "2016"
                    )
                ),
                skills = listOf("Kotlin", "Java", "Swift", "React Native")
            )
        )
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun importProfileAsResume(profile: LinkedInProfile): Resume {
        val content = buildResumeFromProfile(profile)
        val now = Clock.System.now()
        
        return Resume(
            id = Uuid.random().toString(),
            userId = null,
            name = "${profile.firstName} ${profile.lastName} - LinkedIn Import",
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
    
    private fun buildResumeFromProfile(profile: LinkedInProfile): String {
        return buildString {
            appendLine("${profile.firstName} ${profile.lastName}")
            profile.headline?.let { appendLine(it) }
            profile.email?.let { appendLine(it) }
            appendLine()
            
            profile.summary?.let {
                appendLine("PROFESSIONAL SUMMARY")
                appendLine(it)
                appendLine()
            }
            
            if (profile.positions.isNotEmpty()) {
                appendLine("WORK EXPERIENCE")
                profile.positions.forEach { position ->
                    appendLine()
                    appendLine("${position.title}")
                    appendLine("${position.companyName}")
                    val dateRange = buildString {
                        position.startDate?.let { append(it) }
                        append(" - ")
                        if (position.isCurrent) append("Present") else position.endDate?.let { append(it) }
                    }
                    if (dateRange.trim() != "-") appendLine(dateRange.trim())
                    position.location?.let { appendLine(it) }
                    position.description?.let { 
                        appendLine()
                        appendLine(it) 
                    }
                }
                appendLine()
            }
            
            if (profile.education.isNotEmpty()) {
                appendLine("EDUCATION")
                profile.education.forEach { edu ->
                    appendLine()
                    appendLine(edu.schoolName)
                    val degreeField = listOfNotNull(edu.degree, edu.fieldOfStudy).joinToString(" in ")
                    if (degreeField.isNotBlank()) appendLine(degreeField)
                    val years = listOfNotNull(edu.startYear, edu.endYear).joinToString(" - ")
                    if (years.isNotBlank()) appendLine(years)
                }
                appendLine()
            }
            
            if (profile.skills.isNotEmpty()) {
                appendLine("SKILLS")
                appendLine(profile.skills.joinToString(" | "))
            }
        }
    }
}

// File Upload Repository Implementation
class LocalStorageFileUploadRepository : FileUploadRepository {
    private val supportedTypes = listOf("pdf", "docx", "doc", "txt")
    private val maxFileSizeBytes = 10L * 1024 * 1024 // 10MB
    
    override suspend fun uploadResume(
        fileData: ByteArray,
        fileName: String,
        fileType: String
    ): Result<FileUploadResult> {
        return try {
            if (fileData.size > maxFileSizeBytes) {
                return Result.failure(IllegalArgumentException("File too large"))
            }
            
            val normalizedType = fileType.lowercase().removePrefix(".")
            if (normalizedType !in supportedTypes) {
                return Result.failure(IllegalArgumentException("Unsupported file type"))
            }
            
            val extractedContent = extractTextFromFile(fileData, normalizedType).getOrThrow()
            
            Result.success(
                FileUploadResult(
                    success = true,
                    fileName = fileName,
                    fileType = normalizedType,
                    extractedContent = extractedContent,
                    errorMessage = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun extractTextFromFile(fileData: ByteArray, fileType: String): Result<String> {
        return try {
            val text = when (fileType.lowercase()) {
                "txt" -> fileData.decodeToString()
                "pdf" -> extractTextFromPdf(fileData)
                "docx", "doc" -> extractTextFromDocx(fileData)
                else -> return Result.failure(IllegalArgumentException("Unsupported file type: $fileType"))
            }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSupportedFileTypes(): List<String> = supportedTypes
    
    override fun getMaxFileSizeBytes(): Long = maxFileSizeBytes
    
    private fun extractTextFromPdf(fileData: ByteArray): String {
        // For browser-based PDF parsing, we'd use pdf.js or similar library
        // For now, return a placeholder indicating PDF parsing needs JS interop
        return "PDF parsing requires pdf.js integration. Content length: ${fileData.size} bytes"
    }
    
    private fun extractTextFromDocx(fileData: ByteArray): String {
        // For browser-based DOCX parsing, we'd use a library like docx.js
        // For now, return a placeholder
        return "DOCX parsing requires docx.js integration. Content length: ${fileData.size} bytes"
    }
}

// Resume Repository Implementation
class LocalStorageResumeRepository : ResumeRepository {
    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    private val _versions = MutableStateFlow<List<ResumeVersion>>(emptyList())
    private val storageKey = "vwatek_resumes"
    private val versionsStorageKey = "vwatek_resume_versions"
    
    init {
        loadFromStorage()
        loadVersionsFromStorage()
    }
    
    private fun loadFromStorage() {
        val stored = localStorage.getItem(storageKey)
        if (stored != null) {
            try {
                val data = json.decodeFromString<List<ResumeData>>(stored)
                _resumes.value = data.map { it.toResume() }
            } catch (e: Exception) {
                _resumes.value = emptyList()
            }
        }
    }
    
    private fun loadVersionsFromStorage() {
        val stored = localStorage.getItem(versionsStorageKey)
        if (stored != null) {
            try {
                val data = json.decodeFromString<List<ResumeVersionData>>(stored)
                _versions.value = data.map { it.toResumeVersion() }
            } catch (e: Exception) {
                _versions.value = emptyList()
            }
        }
    }
    
    private fun saveToStorage() {
        val data = _resumes.value.map { ResumeData.fromResume(it) }
        localStorage.setItem(storageKey, json.encodeToString(data))
    }
    
    private fun saveVersionsToStorage() {
        val data = _versions.value.map { ResumeVersionData.fromResumeVersion(it) }
        localStorage.setItem(versionsStorageKey, json.encodeToString(data))
    }
    
    override fun getAllResumes(): Flow<List<Resume>> = _resumes.asStateFlow()
    
    override suspend fun getResumeById(id: String): Resume? = _resumes.value.find { it.id == id }
    
    override suspend fun getResumesByUserId(userId: String): List<Resume> = 
        _resumes.value.filter { it.userId == userId }
    
    override suspend fun insertResume(resume: Resume) {
        _resumes.value = _resumes.value + resume
        saveToStorage()
    }
    
    override suspend fun updateResume(resume: Resume) {
        _resumes.value = _resumes.value.map { if (it.id == resume.id) resume else it }
        saveToStorage()
    }
    
    override suspend fun deleteResume(id: String) {
        _resumes.value = _resumes.value.filter { it.id != id }
        saveToStorage()
        // Also delete associated versions
        deleteVersionsByResumeId(id)
    }
    
    // Version control methods
    override fun getVersionsByResumeId(resumeId: String): Flow<List<ResumeVersion>> = 
        _versions.map { list -> list.filter { it.resumeId == resumeId }.sortedByDescending { it.versionNumber } }
    
    override suspend fun getVersionById(id: String): ResumeVersion? = _versions.value.find { it.id == id }
    
    override suspend fun insertVersion(version: ResumeVersion) {
        _versions.value = _versions.value + version
        saveVersionsToStorage()
    }
    
    override suspend fun deleteVersion(id: String) {
        _versions.value = _versions.value.filter { it.id != id }
        saveVersionsToStorage()
    }
    
    override suspend fun deleteVersionsByResumeId(resumeId: String) {
        _versions.value = _versions.value.filter { it.resumeId != resumeId }
        saveVersionsToStorage()
    }
}

// Analysis Repository Implementation
class LocalStorageAnalysisRepository : AnalysisRepository {
    private val _analyses = MutableStateFlow<List<ResumeAnalysis>>(emptyList())
    private val storageKey = "vwatek_analyses"
    
    init {
        loadFromStorage()
    }
    
    private fun loadFromStorage() {
        val stored = localStorage.getItem(storageKey)
        if (stored != null) {
            try {
                val data = json.decodeFromString<List<AnalysisData>>(stored)
                _analyses.value = data.map { it.toAnalysis() }
            } catch (e: Exception) {
                _analyses.value = emptyList()
            }
        }
    }
    
    private fun saveToStorage() {
        val data = _analyses.value.map { AnalysisData.fromAnalysis(it) }
        localStorage.setItem(storageKey, json.encodeToString(data))
    }
    
    override fun getAnalysesByResumeId(resumeId: String): Flow<List<ResumeAnalysis>> = 
        _analyses.map { list -> list.filter { it.resumeId == resumeId } }
    
    override suspend fun insertAnalysis(analysis: ResumeAnalysis) {
        _analyses.value = _analyses.value + analysis
        saveToStorage()
    }
    
    override suspend fun deleteAnalysis(id: String) {
        _analyses.value = _analyses.value.filter { it.id != id }
        saveToStorage()
    }
}

// Cover Letter Repository Implementation
class LocalStorageCoverLetterRepository : CoverLetterRepository {
    private val _coverLetters = MutableStateFlow<List<CoverLetter>>(emptyList())
    private val storageKey = "vwatek_cover_letters"
    
    init {
        loadFromStorage()
    }
    
    private fun loadFromStorage() {
        val stored = localStorage.getItem(storageKey)
        if (stored != null) {
            try {
                val data = json.decodeFromString<List<CoverLetterData>>(stored)
                _coverLetters.value = data.map { it.toCoverLetter() }
            } catch (e: Exception) {
                _coverLetters.value = emptyList()
            }
        }
    }
    
    private fun saveToStorage() {
        val data = _coverLetters.value.map { CoverLetterData.fromCoverLetter(it) }
        localStorage.setItem(storageKey, json.encodeToString(data))
    }
    
    override fun getAllCoverLetters(): Flow<List<CoverLetter>> = _coverLetters.asStateFlow()
    
    override suspend fun getCoverLetterById(id: String): CoverLetter? = _coverLetters.value.find { it.id == id }
    
    override suspend fun insertCoverLetter(coverLetter: CoverLetter) {
        _coverLetters.value = _coverLetters.value + coverLetter
        saveToStorage()
    }
    
    override suspend fun updateCoverLetter(id: String, content: String) {
        _coverLetters.value = _coverLetters.value.map { 
            if (it.id == id) it.copy(content = content) else it 
        }
        saveToStorage()
    }
    
    override suspend fun deleteCoverLetter(id: String) {
        _coverLetters.value = _coverLetters.value.filter { it.id != id }
        saveToStorage()
    }
}

// Interview Repository Implementation
class LocalStorageInterviewRepository : InterviewRepository {
    private val _sessions = MutableStateFlow<List<InterviewSession>>(emptyList())
    private val _questions = MutableStateFlow<List<InterviewQuestion>>(emptyList())
    private val sessionsKey = "vwatek_interviews"
    private val questionsKey = "vwatek_questions"
    
    init {
        loadFromStorage()
    }
    
    private fun loadFromStorage() {
        val storedSessions = localStorage.getItem(sessionsKey)
        if (storedSessions != null) {
            try {
                val data = json.decodeFromString<List<InterviewSessionData>>(storedSessions)
                _sessions.value = data.map { it.toSession() }
            } catch (e: Exception) {
                _sessions.value = emptyList()
            }
        }
        
        val storedQuestions = localStorage.getItem(questionsKey)
        if (storedQuestions != null) {
            try {
                val data = json.decodeFromString<List<InterviewQuestionData>>(storedQuestions)
                _questions.value = data.map { it.toQuestion() }
            } catch (e: Exception) {
                _questions.value = emptyList()
            }
        }
    }
    
    private fun saveSessions() {
        val data = _sessions.value.map { InterviewSessionData.fromSession(it) }
        localStorage.setItem(sessionsKey, json.encodeToString(data))
    }
    
    private fun saveQuestions() {
        val data = _questions.value.map { InterviewQuestionData.fromQuestion(it) }
        localStorage.setItem(questionsKey, json.encodeToString(data))
    }
    
    override fun getAllSessions(): Flow<List<InterviewSession>> = _sessions.asStateFlow()
    
    override suspend fun getSessionById(id: String): InterviewSession? {
        val session = _sessions.value.find { it.id == id } ?: return null
        val questions = _questions.value.filter { it.sessionId == id }
        return session.copy(questions = questions)
    }
    
    override suspend fun insertSession(session: InterviewSession) {
        _sessions.value = _sessions.value + session
        saveSessions()
    }
    
    override suspend fun updateSessionStatus(id: String, status: String, completedAt: Long?) {
        _sessions.value = _sessions.value.map { 
            if (it.id == id) {
                it.copy(
                    status = InterviewStatus.valueOf(status),
                    completedAt = completedAt?.let { ts -> Instant.fromEpochMilliseconds(ts) }
                )
            } else it 
        }
        saveSessions()
    }
    
    override suspend fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filter { it.id != id }
        _questions.value = _questions.value.filter { it.sessionId != id }
        saveSessions()
        saveQuestions()
    }
    
    override fun getQuestionsBySessionId(sessionId: String): Flow<List<InterviewQuestion>> =
        _questions.map { list -> list.filter { it.sessionId == sessionId } }
    
    override suspend fun insertQuestion(question: InterviewQuestion) {
        _questions.value = _questions.value + question
        saveQuestions()
    }
    
    override suspend fun updateQuestion(id: String, userAnswer: String?, aiFeedback: String?) {
        _questions.value = _questions.value.map { 
            if (it.id == id) it.copy(userAnswer = userAnswer, aiFeedback = aiFeedback) else it 
        }
        saveQuestions()
    }
}

// Settings Repository Implementation
class LocalStorageSettingsRepository : SettingsRepository {
    private val storageKey = "vwatek_settings"
    
    private fun loadAll(): MutableMap<String, String> {
        val stored = localStorage.getItem(storageKey) ?: return mutableMapOf()
        return try {
            json.decodeFromString<MutableMap<String, String>>(stored)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
    
    private fun saveAll(settings: Map<String, String>) {
        localStorage.setItem(storageKey, json.encodeToString(settings))
    }
    
    override suspend fun getSetting(key: String): String? = loadAll()[key]
    
    override suspend fun setSetting(key: String, value: String) {
        val all = loadAll()
        all[key] = value
        saveAll(all)
    }
    
    override suspend fun deleteSetting(key: String) {
        val all = loadAll()
        all.remove(key)
        saveAll(all)
    }
}

// Data classes for serialization
@Serializable
private data class AuthStateData(
    val userId: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: String? = null, // ISO timestamp when token expires
    val rememberMe: Boolean = true
) {
    fun isExpired(): Boolean {
        val exp = expiresAt ?: return false
        return try {
            val expireTime = Instant.parse(exp)
            Clock.System.now() > expireTime
        } catch (e: Exception) {
            false
        }
    }
}

@Serializable
private data class UserData(
    val id: String,
    val email: String,
    val password: String?,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val street: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val country: String?,
    val profileImageUrl: String?,
    val authProvider: String,
    val linkedInProfileUrl: String?,
    val emailVerified: Boolean = false,
    val createdAt: String,
    val updatedAt: String
) {
    fun toUser() = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        address = if (street != null || city != null || state != null || zipCode != null || country != null) {
            Address(street, city, state, zipCode, country)
        } else null,
        profileImageUrl = profileImageUrl,
        authProvider = AuthProvider.valueOf(authProvider),
        linkedInProfileUrl = linkedInProfileUrl,
        emailVerified = emailVerified,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt)
    )
    companion object {
        fun fromUser(u: User, password: String? = null) = UserData(
            id = u.id,
            email = u.email,
            password = password,
            firstName = u.firstName,
            lastName = u.lastName,
            phone = u.phone,
            street = u.address?.street,
            city = u.address?.city,
            state = u.address?.state,
            zipCode = u.address?.zipCode,
            country = u.address?.country,
            profileImageUrl = u.profileImageUrl,
            authProvider = u.authProvider.name,
            linkedInProfileUrl = u.linkedInProfileUrl,
            emailVerified = u.emailVerified,
            createdAt = u.createdAt.toString(),
            updatedAt = u.updatedAt.toString()
        )
    }
}

@Serializable
private data class ResumeData(
    val id: String,
    val userId: String? = null,
    val name: String,
    val content: String,
    val industry: String?,
    val sourceType: String = "MANUAL",
    val fileName: String? = null,
    val fileType: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val currentVersionId: String? = null
) {
    fun toResume() = Resume(
        id = id,
        userId = userId,
        name = name,
        content = content,
        industry = industry,
        sourceType = try { ResumeSourceType.valueOf(sourceType) } catch (e: Exception) { ResumeSourceType.MANUAL },
        fileName = fileName,
        fileType = fileType,
        originalFileData = null,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
        currentVersionId = currentVersionId
    )
    companion object {
        fun fromResume(r: Resume) = ResumeData(
            id = r.id,
            userId = r.userId,
            name = r.name,
            content = r.content,
            industry = r.industry,
            sourceType = r.sourceType.name,
            fileName = r.fileName,
            fileType = r.fileType,
            createdAt = r.createdAt.toString(),
            updatedAt = r.updatedAt.toString(),
            currentVersionId = r.currentVersionId
        )
    }
}

@Serializable
private data class ResumeVersionData(
    val id: String,
    val resumeId: String,
    val versionNumber: Int,
    val content: String,
    val changeDescription: String,
    val createdAt: String
) {
    fun toResumeVersion() = ResumeVersion(
        id = id,
        resumeId = resumeId,
        versionNumber = versionNumber,
        content = content,
        changeDescription = changeDescription,
        createdAt = Instant.parse(createdAt)
    )
    companion object {
        fun fromResumeVersion(v: ResumeVersion) = ResumeVersionData(
            id = v.id,
            resumeId = v.resumeId,
            versionNumber = v.versionNumber,
            content = v.content,
            changeDescription = v.changeDescription,
            createdAt = v.createdAt.toString()
        )
    }
}

@Serializable
private data class AnalysisData(
    val id: String,
    val resumeId: String,
    val jobDescription: String,
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>,
    val createdAt: String
) {
    fun toAnalysis() = ResumeAnalysis(
        id = id,
        resumeId = resumeId,
        jobDescription = jobDescription,
        matchScore = matchScore,
        missingKeywords = missingKeywords,
        recommendations = recommendations,
        createdAt = Instant.parse(createdAt)
    )
    companion object {
        fun fromAnalysis(a: ResumeAnalysis) = AnalysisData(
            id = a.id,
            resumeId = a.resumeId,
            jobDescription = a.jobDescription,
            matchScore = a.matchScore,
            missingKeywords = a.missingKeywords,
            recommendations = a.recommendations,
            createdAt = a.createdAt.toString()
        )
    }
}

@Serializable
private data class CoverLetterData(
    val id: String,
    val resumeId: String?,
    val jobTitle: String,
    val companyName: String,
    val content: String,
    val tone: String,
    val createdAt: String
) {
    fun toCoverLetter() = CoverLetter(
        id = id,
        resumeId = resumeId,
        jobTitle = jobTitle,
        companyName = companyName,
        content = content,
        tone = CoverLetterTone.valueOf(tone),
        createdAt = Instant.parse(createdAt)
    )
    companion object {
        fun fromCoverLetter(c: CoverLetter) = CoverLetterData(
            id = c.id,
            resumeId = c.resumeId,
            jobTitle = c.jobTitle,
            companyName = c.companyName,
            content = c.content,
            tone = c.tone.name,
            createdAt = c.createdAt.toString()
        )
    }
}

@Serializable
private data class InterviewQuestionData(
    val id: String,
    val sessionId: String,
    val question: String,
    val userAnswer: String?,
    val aiFeedback: String?,
    val questionOrder: Int,
    val createdAt: String
) {
    fun toQuestion() = InterviewQuestion(
        id = id,
        sessionId = sessionId,
        question = question,
        userAnswer = userAnswer,
        aiFeedback = aiFeedback,
        questionOrder = questionOrder,
        createdAt = Instant.parse(createdAt)
    )
    companion object {
        fun fromQuestion(q: InterviewQuestion) = InterviewQuestionData(
            id = q.id,
            sessionId = q.sessionId,
            question = q.question,
            userAnswer = q.userAnswer,
            aiFeedback = q.aiFeedback,
            questionOrder = q.questionOrder,
            createdAt = q.createdAt.toString()
        )
    }
}

@Serializable
private data class InterviewSessionData(
    val id: String,
    val resumeId: String?,
    val jobTitle: String,
    val jobDescription: String,
    val status: String,
    val createdAt: String,
    val completedAt: String?
) {
    fun toSession() = InterviewSession(
        id = id,
        resumeId = resumeId,
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        status = InterviewStatus.valueOf(status),
        questions = emptyList(),
        createdAt = Instant.parse(createdAt),
        completedAt = completedAt?.let { Instant.parse(it) }
    )
    companion object {
        fun fromSession(s: InterviewSession) = InterviewSessionData(
            id = s.id,
            resumeId = s.resumeId,
            jobTitle = s.jobTitle,
            jobDescription = s.jobDescription,
            status = s.status.name,
            createdAt = s.createdAt.toString(),
            completedAt = s.completedAt?.toString()
        )
    }
}
