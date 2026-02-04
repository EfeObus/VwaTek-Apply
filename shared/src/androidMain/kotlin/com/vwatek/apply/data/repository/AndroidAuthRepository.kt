package com.vwatek.apply.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vwatek.apply.domain.model.AuthState
import com.vwatek.apply.domain.model.AuthProvider
import com.vwatek.apply.domain.model.User
import com.vwatek.apply.domain.model.RegistrationData
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.GoogleUserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AndroidAuthRepository(
    private val context: Context
) : AuthRepository {
    
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "vwatek_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _authState = MutableStateFlow(loadAuthState())
    
    private fun loadAuthState(): AuthState {
        val userJson = prefs.getString("current_user", null)
        val user = userJson?.let { 
            try {
                json.decodeFromString<User>(it)
            } catch (e: Exception) {
                null
            }
        }
        return AuthState(
            isAuthenticated = user != null,
            user = user
        )
    }
    
    private fun saveUser(user: User?) {
        if (user != null) {
            prefs.edit().putString("current_user", json.encodeToString(user)).apply()
        } else {
            prefs.edit().remove("current_user").apply()
        }
        _authState.value = AuthState(
            isAuthenticated = user != null,
            user = user
        )
    }
    
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun getCurrentUser(): User? = _authState.value.user
    
    override suspend fun registerWithEmail(data: RegistrationData): Result<User> {
        // For now, create user locally (in production, this would call the backend)
        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            email = data.email,
            firstName = data.firstName,
            lastName = data.lastName,
            phone = data.phone,
            address = data.address,
            profileImageUrl = null,
            authProvider = AuthProvider.EMAIL,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        saveUser(user)
        return Result.success(user)
    }
    
    override suspend fun loginWithEmail(email: String, password: String, rememberMe: Boolean): Result<User> {
        // For demo, auto-login with provided credentials
        // In production, this would validate against backend
        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            email = email,
            firstName = email.substringBefore("@"),
            lastName = "",
            phone = null,
            address = null,
            profileImageUrl = null,
            authProvider = AuthProvider.EMAIL,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        saveUser(user)
        return Result.success(user)
    }
    
    override suspend fun loginWithGoogle(idToken: String, userInfo: GoogleUserData?): Result<User> {
        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            email = userInfo?.email ?: "google_user@gmail.com",
            firstName = userInfo?.firstName ?: "Google",
            lastName = userInfo?.lastName ?: "User",
            phone = null,
            address = null,
            profileImageUrl = userInfo?.profilePicture,
            authProvider = AuthProvider.GOOGLE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        saveUser(user)
        return Result.success(user)
    }
    
    override suspend fun loginWithLinkedIn(authCode: String): Result<User> {
        val user = User(
            id = java.util.UUID.randomUUID().toString(),
            email = "linkedin_user@linkedin.com",
            firstName = "LinkedIn",
            lastName = "User",
            phone = null,
            address = null,
            profileImageUrl = null,
            authProvider = AuthProvider.LINKEDIN,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        saveUser(user)
        return Result.success(user)
    }
    
    override suspend fun logout() {
        saveUser(null)
    }
    
    override suspend fun updateProfile(user: User): Result<User> {
        val updatedUser = user.copy(updatedAt = Clock.System.now())
        saveUser(updatedUser)
        return Result.success(updatedUser)
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        // In production, this would trigger a password reset email
        return Result.success(Unit)
    }
    
    override suspend fun isEmailAvailable(email: String): Boolean {
        // In production, this would check against backend
        return true
    }
    
    override suspend fun sendVerificationEmail(email: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun verifyEmail(token: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun isEmailVerified(userId: String): Boolean {
        return true
    }
}
