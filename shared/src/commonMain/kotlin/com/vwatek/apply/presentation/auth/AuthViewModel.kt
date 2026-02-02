package com.vwatek.apply.presentation.auth

import com.vwatek.apply.domain.model.AuthState
import com.vwatek.apply.domain.model.User
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
import com.vwatek.apply.domain.model.Resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthViewState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val registrationSuccess: Boolean = false,
    val passwordResetSent: Boolean = false,
    val linkedInAuthUrl: String? = null,
    val uploadedResume: Resume? = null,
    val uploadProgress: Float = 0f,
    val currentView: AuthView = AuthView.LOGIN
)

enum class AuthView {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    PROFILE
}

sealed class AuthIntent {
    // Navigation
    data class SwitchView(val view: AuthView) : AuthIntent()
    
    // Email Auth
    data class Login(val email: String, val password: String, val rememberMe: Boolean = true) : AuthIntent()
    data class Register(
        val email: String,
        val password: String,
        val confirmPassword: String,
        val firstName: String,
        val lastName: String,
        val phone: String? = null,
        val street: String? = null,
        val city: String? = null,
        val state: String? = null,
        val zipCode: String? = null,
        val country: String? = null
    ) : AuthIntent()
    data class ResetPassword(val email: String) : AuthIntent()
    
    // Social Auth
    data class LoginWithGoogle(val idToken: String) : AuthIntent()
    data class LoginWithLinkedIn(val authCode: String) : AuthIntent()
    object GetLinkedInAuthUrl : AuthIntent()
    
    // Google Sign-In with user info (from Google Identity Services)
    data class GoogleSignIn(
        val email: String,
        val firstName: String,
        val lastName: String,
        val profilePicture: String? = null
    ) : AuthIntent()
    
    // LinkedIn OAuth callback
    data class LinkedInCallback(val authCode: String) : AuthIntent()
    
    // Profile
    data class UpdateProfile(val user: User) : AuthIntent()
    object Logout : AuthIntent()
    
    // Resume Upload
    data class UploadResume(
        val fileData: ByteArray,
        val fileName: String,
        val fileType: String
    ) : AuthIntent()
    
    // LinkedIn Import
    data class ImportFromLinkedIn(val authCode: String) : AuthIntent()
    
    // Error handling
    object ClearError : AuthIntent()
    object ClearSuccess : AuthIntent()
}

class AuthViewModel(
    private val getAuthState: GetAuthStateUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val registerWithEmail: RegisterWithEmailUseCase,
    private val loginWithEmail: LoginWithEmailUseCase,
    private val loginWithGoogle: LoginWithGoogleUseCase,
    private val loginWithLinkedIn: LoginWithLinkedInUseCase,
    private val logout: LogoutUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val checkEmailAvailability: CheckEmailAvailabilityUseCase,
    private val getLinkedInAuthUrl: GetLinkedInAuthUrlUseCase,
    private val importLinkedInProfile: ImportLinkedInProfileUseCase,
    private val uploadResumeFile: UploadResumeFileUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _state = MutableStateFlow(AuthViewState())
    val state: StateFlow<AuthViewState> = _state.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        scope.launch {
            getAuthState().collect { authState ->
                _state.value = _state.value.copy(
                    isAuthenticated = authState.isAuthenticated,
                    user = authState.user
                )
            }
        }
    }
    
    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SwitchView -> switchView(intent.view)
            is AuthIntent.Login -> login(intent.email, intent.password, intent.rememberMe)
            is AuthIntent.Register -> register(intent)
            is AuthIntent.ResetPassword -> resetUserPassword(intent.email)
            is AuthIntent.LoginWithGoogle -> loginGoogle(intent.idToken)
            is AuthIntent.LoginWithLinkedIn -> loginLinkedIn(intent.authCode)
            AuthIntent.GetLinkedInAuthUrl -> fetchLinkedInAuthUrl()
            is AuthIntent.UpdateProfile -> updateUserProfile(intent.user)
            AuthIntent.Logout -> performLogout()
            is AuthIntent.UploadResume -> uploadResume(intent.fileData, intent.fileName, intent.fileType)
            is AuthIntent.ImportFromLinkedIn -> importFromLinkedIn(intent.authCode)
            is AuthIntent.GoogleSignIn -> handleGoogleSignIn(intent)
            is AuthIntent.LinkedInCallback -> handleLinkedInCallback(intent.authCode)
            AuthIntent.ClearError -> _state.value = _state.value.copy(error = null)
            AuthIntent.ClearSuccess -> _state.value = _state.value.copy(
                registrationSuccess = false,
                passwordResetSent = false,
                uploadedResume = null
            )
        }
    }
    
    private fun handleGoogleSignIn(intent: AuthIntent.GoogleSignIn) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Create or login user with Google info - pass all user details
            loginWithGoogle(
                idToken = intent.email, // Use email as identifier
                email = intent.email,
                firstName = intent.firstName,
                lastName = intent.lastName,
                profilePicture = intent.profilePicture
            )
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = user,
                        currentView = AuthView.PROFILE
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Google sign-in failed"
                    )
                }
        }
    }
    
    private fun handleLinkedInCallback(authCode: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            loginWithLinkedIn(authCode)
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = user,
                        currentView = AuthView.PROFILE
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "LinkedIn sign-in failed"
                    )
                }
        }
    }
    
    private fun switchView(view: AuthView) {
        _state.value = _state.value.copy(
            currentView = view,
            error = null,
            registrationSuccess = false,
            passwordResetSent = false
        )
    }
    
    private fun login(email: String, password: String, rememberMe: Boolean = true) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            loginWithEmail(email, password, rememberMe)
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = user,
                        currentView = AuthView.PROFILE
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Login failed"
                    )
                }
        }
    }
    
    private fun register(intent: AuthIntent.Register) {
        if (intent.password != intent.confirmPassword) {
            _state.value = _state.value.copy(error = "Passwords do not match")
            return
        }
        
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            registerWithEmail(
                email = intent.email,
                password = intent.password,
                firstName = intent.firstName,
                lastName = intent.lastName,
                phone = intent.phone,
                street = intent.street,
                city = intent.city,
                state = intent.state,
                zipCode = intent.zipCode,
                country = intent.country
            )
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        registrationSuccess = true,
                        isAuthenticated = true,
                        user = user
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Registration failed"
                    )
                }
        }
    }
    
    private fun resetUserPassword(email: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            resetPassword(email)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        passwordResetSent = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to send reset email"
                    )
                }
        }
    }
    
    private fun loginGoogle(idToken: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            loginWithGoogle(idToken)
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = user
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Google login failed"
                    )
                }
        }
    }
    
    private fun loginLinkedIn(authCode: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            loginWithLinkedIn(authCode)
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        user = user
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "LinkedIn login failed"
                    )
                }
        }
    }
    
    private fun fetchLinkedInAuthUrl() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val url = getLinkedInAuthUrl()
                _state.value = _state.value.copy(
                    isLoading = false,
                    linkedInAuthUrl = url
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to get LinkedIn authorization URL"
                )
            }
        }
    }
    
    private fun updateUserProfile(user: User) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            updateProfile(user)
                .onSuccess { updatedUser ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        user = updatedUser
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Profile update failed"
                    )
                }
        }
    }
    
    private fun performLogout() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            logout()
            _state.value = AuthViewState()
        }
    }
    
    private fun uploadResume(fileData: ByteArray, fileName: String, fileType: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, uploadProgress = 0f)
            
            uploadResumeFile(fileData, fileName, fileType, _state.value.user?.id)
                .onSuccess { resume ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        uploadedResume = resume,
                        uploadProgress = 1f
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Resume upload failed",
                        uploadProgress = 0f
                    )
                }
        }
    }
    
    private fun importFromLinkedIn(authCode: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            importLinkedInProfile(authCode)
                .onSuccess { resume ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        uploadedResume = resume
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "LinkedIn import failed"
                    )
                }
        }
    }
}
