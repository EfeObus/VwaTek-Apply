package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.presentation.auth.AuthIntent
import com.vwatek.apply.presentation.auth.AuthView
import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.util.OAuthHelper
import com.vwatek.apply.util.LinkedInCallbackResult
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext

@Composable
fun AuthScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onLogoutSuccess: () -> Unit = {}
) {
    val viewModel = remember { GlobalContext.get().get<AuthViewModel>() }
    val state by viewModel.state.collectAsState()
    var googleError by remember { mutableStateOf<String?>(null) }
    var linkedInError by remember { mutableStateOf<String?>(null) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isLinkedInLoading by remember { mutableStateOf(false) }
    
    // Initialize Google Sign-In when component mounts
    LaunchedEffect(Unit) {
        // Check for LinkedIn callback in URL
        val url = window.location.href
        if (url.contains("code=") || url.contains("error=")) {
            when (val result = OAuthHelper.handleLinkedInCallback()) {
                is LinkedInCallbackResult.Success -> {
                    // Exchange authorization code for tokens via backend
                    // For now, just show success
                    console.log("LinkedIn auth code received: ${result.code}")
                    // Clean up URL
                    window.history.replaceState(null, "", window.location.pathname)
                }
                is LinkedInCallbackResult.Error -> {
                    linkedInError = result.error
                    window.history.replaceState(null, "", window.location.pathname)
                }
                LinkedInCallbackResult.NoCallback -> {
                    // Not a callback, do nothing
                }
            }
        }
        
        // Initialize Google Sign-In
        try {
            OAuthHelper.initializeGoogleSignIn(
                clientId = "21443684777-b3fbd6nd22ggk7shckddina56lm4rq7a.apps.googleusercontent.com",
                onSuccess = { userInfo ->
                    isGoogleLoading = false
                    // Create user from Google info
                    viewModel.onIntent(AuthIntent.GoogleSignIn(
                        email = userInfo.email,
                        firstName = userInfo.givenName.ifEmpty { userInfo.name.split(" ").firstOrNull() ?: "" },
                        lastName = userInfo.familyName.ifEmpty { userInfo.name.split(" ").lastOrNull() ?: "" },
                        profilePicture = userInfo.picture
                    ))
                },
                onError = { error ->
                    isGoogleLoading = false
                    googleError = error
                }
            )
        } catch (e: Exception) {
            console.log("Google Sign-In initialization failed: ${e.message}")
        }
    }
    
    // Navigate on successful login
    if (state.isAuthenticated && state.user != null) {
        onLoginSuccess()
    }
    
    Div(attrs = { classes("auth-container") }) {
        // Back button
        Button(attrs = {
            classes("btn", "btn-secondary", "mb-lg")
            onClick { onNavigateBack() }
            style {
                property("position", "absolute")
                property("top", "var(--spacing-lg)")
                property("left", "var(--spacing-lg)")
            }
        }) {
            Text("Back to App")
        }
        
        Div(attrs = { classes("auth-card") }) {
            // Logo
            Div(attrs = { 
                classes("text-center", "mb-lg")
            }) {
                Img(
                    src = "logo.png",
                    alt = "VwaTek Apply",
                    attrs = {
                        style {
                            property("height", "60px")
                            property("width", "auto")
                            property("margin-bottom", "var(--spacing-md)")
                        }
                    }
                )
                H1 { Text("VwaTek Apply") }
                P(attrs = { classes("text-secondary") }) {
                    Text("AI-Powered Job Application Assistant")
                }
            }
            
            when (state.currentView) {
                AuthView.LOGIN -> LoginForm(
                    isLoading = state.isLoading,
                    error = state.error ?: googleError ?: linkedInError,
                    isGoogleLoading = isGoogleLoading,
                    isLinkedInLoading = isLinkedInLoading,
                    onLogin = { email, password, rememberMe ->
                        viewModel.onIntent(AuthIntent.Login(email, password, rememberMe))
                    },
                    onSwitchToRegister = {
                        viewModel.onIntent(AuthIntent.SwitchView(AuthView.REGISTER))
                    },
                    onSwitchToForgotPassword = {
                        viewModel.onIntent(AuthIntent.SwitchView(AuthView.FORGOT_PASSWORD))
                    },
                    onGoogleLogin = { 
                        googleError = null
                        isGoogleLoading = true
                        OAuthHelper.promptGoogleSignIn()
                    },
                    onLinkedInLogin = {
                        linkedInError = null
                        isLinkedInLoading = true
                        OAuthHelper.openLinkedInPopup(
                            onSuccess = { code ->
                                isLinkedInLoading = false
                                console.log("LinkedIn auth successful, code: $code")
                                // In production, exchange code for tokens via backend
                                viewModel.onIntent(AuthIntent.LinkedInCallback(code))
                            },
                            onError = { error ->
                                isLinkedInLoading = false
                                linkedInError = error
                            }
                        )
                    },
                    onClearError = { 
                        viewModel.onIntent(AuthIntent.ClearError)
                        googleError = null
                        linkedInError = null
                    }
                )
                
                AuthView.REGISTER -> RegisterForm(
                    isLoading = state.isLoading,
                    error = state.error,
                    registrationSuccess = state.registrationSuccess,
                    onRegister = { data ->
                        viewModel.onIntent(data)
                    },
                    onSwitchToLogin = {
                        viewModel.onIntent(AuthIntent.SwitchView(AuthView.LOGIN))
                    },
                    onClearError = { viewModel.onIntent(AuthIntent.ClearError) },
                    onClearSuccess = { viewModel.onIntent(AuthIntent.ClearSuccess) }
                )
                
                AuthView.FORGOT_PASSWORD -> ForgotPasswordForm(
                    isLoading = state.isLoading,
                    error = state.error,
                    resetSent = state.passwordResetSent,
                    onResetPassword = { email ->
                        viewModel.onIntent(AuthIntent.ResetPassword(email))
                    },
                    onSwitchToLogin = {
                        viewModel.onIntent(AuthIntent.SwitchView(AuthView.LOGIN))
                    },
                    onClearError = { viewModel.onIntent(AuthIntent.ClearError) },
                    onClearSuccess = { viewModel.onIntent(AuthIntent.ClearSuccess) }
                )
                
                AuthView.PROFILE -> ProfileView(
                    user = state.user,
                    onLogout = { 
                        viewModel.onIntent(AuthIntent.Logout)
                        onLogoutSuccess()
                    },
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    error: String?,
    isGoogleLoading: Boolean,
    isLinkedInLoading: Boolean,
    onLogin: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onSwitchToRegister: () -> Unit,
    onSwitchToForgotPassword: () -> Unit,
    onGoogleLogin: () -> Unit,
    onLinkedInLogin: () -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    
    H2(attrs = { classes("text-center", "mb-lg") }) { Text("Sign In") }
    
    // Error message
    error?.let {
        Div(attrs = { classes("alert", "alert-error", "mb-md") }) {
            Text(it)
            Button(attrs = {
                classes("btn", "btn-sm")
                onClick { onClearError() }
            }) {
                Text("✕")
            }
        }
    }
    
    // Social Login Buttons
    Div(attrs = { classes("social-login-buttons", "mb-lg") }) {
        Button(attrs = {
            classes("btn", "btn-social", "btn-google")
            if (isGoogleLoading) attr("disabled", "true")
            onClick { onGoogleLogin() }
        }) {
            if (isGoogleLoading) {
                Span(attrs = { classes("spinner-sm", "mr-sm") })
                Text("Connecting...")
            } else {
                RawHtml("""
                    <svg width="20" height="20" viewBox="0 0 24 24">
                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                """)
                Text("Continue with Google")
            }
        }
        
        Button(attrs = {
            classes("btn", "btn-social", "btn-linkedin")
            if (isLinkedInLoading) attr("disabled", "true")
            onClick { onLinkedInLogin() }
        }) {
            if (isLinkedInLoading) {
                Span(attrs = { classes("spinner-sm", "mr-sm") })
                Text("Connecting...")
            } else {
                RawHtml("""
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="#0A66C2">
                        <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
                    </svg>
                """)
                Text("Continue with LinkedIn")
            }
        }
    }
    
    // Divider
    Div(attrs = { classes("auth-divider", "mb-lg") }) {
        Span { Text("or sign in with email") }
    }
    
    // Email/Password Form
    Form(attrs = {
        onSubmit { 
            it.preventDefault()
            if (email.isNotBlank() && password.isNotBlank()) {
                onLogin(email, password, rememberMe)
            }
        }
    }) {
        Div(attrs = { classes("form-group") }) {
            Label(attrs = { classes("form-label") }) { Text("Email Address") }
            Input(InputType.Email) {
                classes("form-input")
                placeholder("you@example.com")
                value(email)
                onInput { email = it.value }
                required()
            }
        }
        
        Div(attrs = { classes("form-group") }) {
            Label(attrs = { classes("form-label") }) { Text("Password") }
            Input(InputType.Password) {
                classes("form-input")
                placeholder("Enter your password")
                value(password)
                onInput { password = it.value }
                required()
            }
        }
        
        Div(attrs = { classes("flex", "justify-between", "items-center", "mb-lg") }) {
            Label(attrs = { classes("flex", "items-center", "gap-sm") }) {
                Input(InputType.Checkbox) {
                    checked(rememberMe)
                    onInput { rememberMe = it.value }
                }
                Text("Remember me")
            }
            A(attrs = {
                classes("text-primary")
                onClick { onSwitchToForgotPassword() }
                style { property("cursor", "pointer") }
            }) {
                Text("Forgot password?")
            }
        }
        
        Button(attrs = {
            classes("btn", "btn-primary", "btn-lg", "w-full")
            if (isLoading) attr("disabled", "true")
        }) {
            if (isLoading) {
                Span(attrs = { classes("spinner-sm") })
                Text(" Signing in...")
            } else {
                Text("Sign In")
            }
        }
    }
    
    // Register Link
    P(attrs = { classes("text-center", "mt-lg") }) {
        Text("Don't have an account? ")
        A(attrs = {
            classes("text-primary")
            onClick { onSwitchToRegister() }
            style { property("cursor", "pointer") }
        }) {
            Text("Create one")
        }
    }
}

@Composable
private fun RegisterForm(
    isLoading: Boolean,
    error: String?,
    registrationSuccess: Boolean,
    onRegister: (AuthIntent.Register) -> Unit,
    onSwitchToLogin: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var addressState by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var showAddressFields by remember { mutableStateOf(false) }
    
    H2(attrs = { classes("text-center", "mb-lg") }) { Text("Create Account") }
    
    // Success message
    if (registrationSuccess) {
        Div(attrs = { classes("alert", "alert-success", "mb-md") }) {
            Text("Registration successful! You are now logged in.")
            Button(attrs = {
                classes("btn", "btn-sm")
                onClick { onClearSuccess() }
            }) {
                Text("X")
            }
        }
    }
    
    // Error message
    error?.let {
        Div(attrs = { classes("alert", "alert-error", "mb-md") }) {
            Text(it)
            Button(attrs = {
                classes("btn", "btn-sm")
                onClick { onClearError() }
            }) {
                Text("X")
            }
        }
    }
    
    Form(attrs = {
        onSubmit { 
            it.preventDefault()
            onRegister(AuthIntent.Register(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                firstName = firstName,
                lastName = lastName,
                phone = phone.ifBlank { null },
                street = street.ifBlank { null },
                city = city.ifBlank { null },
                state = addressState.ifBlank { null },
                zipCode = zipCode.ifBlank { null },
                country = country.ifBlank { null }
            ))
        }
    }) {
        // Name fields
        Div(attrs = { classes("grid", "grid-2", "gap-md") }) {
            Div(attrs = { classes("form-group") }) {
                Label(attrs = { classes("form-label") }) { Text("First Name *") }
                Input(InputType.Text) {
                    classes("form-input")
                    placeholder("John")
                    value(firstName)
                    onInput { firstName = it.value }
                    required()
                }
            }
            Div(attrs = { classes("form-group") }) {
                Label(attrs = { classes("form-label") }) { Text("Last Name *") }
                Input(InputType.Text) {
                    classes("form-input")
                    placeholder("Doe")
                    value(lastName)
                    onInput { lastName = it.value }
                    required()
                }
            }
        }
        
        // Email
        Div(attrs = { classes("form-group") }) {
            Label(attrs = { classes("form-label") }) { Text("Email Address *") }
            Input(InputType.Email) {
                classes("form-input")
                placeholder("you@example.com")
                value(email)
                onInput { email = it.value }
                required()
            }
        }
        
        // Phone
        Div(attrs = { classes("form-group") }) {
            Label(attrs = { classes("form-label") }) { Text("Phone Number") }
            Input(InputType.Tel) {
                classes("form-input")
                placeholder("+1 (555) 123-4567")
                value(phone)
                onInput { phone = it.value }
            }
        }
        
        // Password fields
        Div(attrs = { classes("grid", "grid-2", "gap-md") }) {
            Div(attrs = { classes("form-group") }) {
                Label(attrs = { classes("form-label") }) { Text("Password *") }
                Input(InputType.Password) {
                    classes("form-input")
                    placeholder("Min 8 characters")
                    value(password)
                    onInput { password = it.value }
                    required()
                    attr("minlength", "8")
                }
            }
            Div(attrs = { classes("form-group") }) {
                Label(attrs = { classes("form-label") }) { Text("Confirm Password *") }
                Input(InputType.Password) {
                    classes("form-input")
                    placeholder("Confirm password")
                    value(confirmPassword)
                    onInput { confirmPassword = it.value }
                    required()
                }
            }
        }
        
        P(attrs = { classes("form-helper", "mb-md") }) {
            Text("Password must be at least 8 characters with uppercase, lowercase, and number")
        }
        
        // Address toggle
        Button(attrs = {
            classes("btn", "btn-secondary", "btn-sm", "mb-md")
            attr("type", "button")
            onClick { showAddressFields = !showAddressFields }
        }) {
            Text(if (showAddressFields) "Hide Address Fields" else "Add Address (Optional)")
        }
        
        // Address fields
        if (showAddressFields) {
            Div(attrs = { classes("address-section", "mb-md") }) {
                Div(attrs = { classes("form-group") }) {
                    Label(attrs = { classes("form-label") }) { Text("Street Address") }
                    Input(InputType.Text) {
                        classes("form-input")
                        placeholder("123 Main St")
                        value(street)
                        onInput { street = it.value }
                    }
                }
                
                Div(attrs = { classes("grid", "grid-2", "gap-md") }) {
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("City") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("New York")
                            value(city)
                            onInput { city = it.value }
                        }
                    }
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("State/Province") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("NY")
                            value(addressState)
                            onInput { addressState = it.value }
                        }
                    }
                }
                
                Div(attrs = { classes("grid", "grid-2", "gap-md") }) {
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("ZIP/Postal Code") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("10001")
                            value(zipCode)
                            onInput { zipCode = it.value }
                        }
                    }
                    Div(attrs = { classes("form-group") }) {
                        Label(attrs = { classes("form-label") }) { Text("Country") }
                        Input(InputType.Text) {
                            classes("form-input")
                            placeholder("United States")
                            value(country)
                            onInput { country = it.value }
                        }
                    }
                }
            }
        }
        
        Button(attrs = {
            classes("btn", "btn-primary", "btn-lg", "w-full")
            if (isLoading) attr("disabled", "true")
        }) {
            if (isLoading) {
                Span(attrs = { classes("spinner-sm") })
                Text(" Creating account...")
            } else {
                Text("Create Account")
            }
        }
    }
    
    // Login Link
    P(attrs = { classes("text-center", "mt-lg") }) {
        Text("Already have an account? ")
        A(attrs = {
            classes("text-primary")
            onClick { onSwitchToLogin() }
            style { property("cursor", "pointer") }
        }) {
            Text("Sign in")
        }
    }
}

@Composable
private fun ForgotPasswordForm(
    isLoading: Boolean,
    error: String?,
    resetSent: Boolean,
    onResetPassword: (email: String) -> Unit,
    onSwitchToLogin: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    
    H2(attrs = { classes("text-center", "mb-lg") }) { Text("Reset Password") }
    
    if (resetSent) {
        Div(attrs = { classes("alert", "alert-success", "mb-md") }) {
            Text("Password reset instructions have been sent to your email.")
            Button(attrs = {
                classes("btn", "btn-sm")
                onClick { onClearSuccess() }
            }) {
                Text("X")
            }
        }
    }
    
    error?.let {
        Div(attrs = { classes("alert", "alert-error", "mb-md") }) {
            Text(it)
            Button(attrs = {
                classes("btn", "btn-sm")
                onClick { onClearError() }
            }) {
                Text("X")
            }
        }
    }
    
    P(attrs = { classes("text-secondary", "text-center", "mb-lg") }) {
        Text("Enter your email address and we'll send you instructions to reset your password.")
    }
    
    Form(attrs = {
        onSubmit { 
            it.preventDefault()
            if (email.isNotBlank()) {
                onResetPassword(email)
            }
        }
    }) {
        Div(attrs = { classes("form-group") }) {
            Label(attrs = { classes("form-label") }) { Text("Email Address") }
            Input(InputType.Email) {
                classes("form-input")
                placeholder("you@example.com")
                value(email)
                onInput { email = it.value }
                required()
            }
        }
        
        Button(attrs = {
            classes("btn", "btn-primary", "btn-lg", "w-full")
            if (isLoading) attr("disabled", "true")
        }) {
            if (isLoading) {
                Span(attrs = { classes("spinner-sm") })
                Text(" Sending...")
            } else {
                Text("Send Reset Instructions")
            }
        }
    }
    
    P(attrs = { classes("text-center", "mt-lg") }) {
        Text("Remember your password? ")
        A(attrs = {
            classes("text-primary")
            onClick { onSwitchToLogin() }
            style { property("cursor", "pointer") }
        }) {
            Text("Sign in")
        }
    }
}

@Composable
private fun ProfileView(
    user: com.vwatek.apply.domain.model.User?,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { GlobalContext.get().get<AuthViewModel>() }
    val state by viewModel.state.collectAsState()
    
    var showChangePassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    user?.let { u ->
        H2(attrs = { classes("text-center", "mb-lg") }) { Text("Profile") }
        
        // Avatar
        Div(attrs = { 
            classes("text-center", "mb-lg")
        }) {
            Div(attrs = {
                style {
                    property("width", "80px")
                    property("height", "80px")
                    property("border-radius", "50%")
                    property("background", "var(--primary-color)")
                    property("color", "white")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("font-size", "2rem")
                    property("font-weight", "600")
                    property("margin", "0 auto var(--spacing-md)")
                }
            }) {
                Text(u.firstName.first().toString().uppercase())
            }
            H3 { Text("${u.firstName} ${u.lastName}") }
            P(attrs = { classes("text-secondary") }) { Text(u.email) }
        }
        
        // Profile details
        Div(attrs = { classes("profile-details", "mb-lg") }) {
            val userPhone = u.phone
            if (userPhone != null) {
                ProfileField("Phone", userPhone)
            }
            u.address?.let { addr ->
                val addressStr = listOfNotNull(
                    addr.street, addr.city, addr.state, addr.zipCode, addr.country
                ).joinToString(", ")
                if (addressStr.isNotBlank()) {
                    ProfileField("Address", addressStr)
                }
            }
            ProfileField("Auth Provider", u.authProvider.name)
        }
        
        // Change Password Section
        Div(attrs = { classes("profile-section", "mb-lg") }) {
            Button(attrs = {
                classes("btn", "btn-outline", "w-full")
                onClick { showChangePassword = !showChangePassword }
            }) {
                Text(if (showChangePassword) "Hide Change Password" else "Change Password")
            }
            
            if (showChangePassword) {
                Div(attrs = { 
                    classes("card", "mt-md")
                    style { property("padding", "var(--spacing-lg)") }
                }) {
                    H4(attrs = { classes("mb-md") }) { Text("Change Password") }
                    
                    // Success message
                    passwordMessage?.let { msg ->
                        Div(attrs = { classes("alert", "alert-success", "mb-md") }) {
                            Text(msg)
                        }
                    }
                    
                    // Error message
                    passwordError?.let { err ->
                        Div(attrs = { classes("alert", "alert-error", "mb-md") }) {
                            Text(err)
                        }
                    }
                    
                    Form(attrs = {
                        onSubmit { e ->
                            e.preventDefault()
                            passwordError = null
                            passwordMessage = null
                            
                            if (newPassword.length < 8) {
                                passwordError = "Password must be at least 8 characters"
                                return@onSubmit
                            }
                            if (newPassword != confirmPassword) {
                                passwordError = "Passwords do not match"
                                return@onSubmit
                            }
                            
                            viewModel.onIntent(AuthIntent.ChangePassword(currentPassword, newPassword))
                            passwordMessage = "Password changed successfully"
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    }) {
                        Div(attrs = { classes("form-group", "mb-md") }) {
                            Label(attrs = { classes("form-label") }) { Text("Current Password") }
                            Input(InputType.Password) {
                                classes("form-input")
                                placeholder("Enter current password")
                                value(currentPassword)
                                onInput { currentPassword = it.value }
                                required()
                            }
                        }
                        
                        Div(attrs = { classes("form-group", "mb-md") }) {
                            Label(attrs = { classes("form-label") }) { Text("New Password") }
                            Input(InputType.Password) {
                                classes("form-input")
                                placeholder("Min 8 characters")
                                value(newPassword)
                                onInput { newPassword = it.value }
                                required()
                                attr("minlength", "8")
                            }
                            P(attrs = { classes("form-helper") }) {
                                Text("Must be at least 8 characters with uppercase, lowercase, and number")
                            }
                        }
                        
                        Div(attrs = { classes("form-group", "mb-md") }) {
                            Label(attrs = { classes("form-label") }) { Text("Confirm New Password") }
                            Input(InputType.Password) {
                                classes("form-input")
                                placeholder("Confirm new password")
                                value(confirmPassword)
                                onInput { confirmPassword = it.value }
                                required()
                            }
                            if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                                P(attrs = { classes("text-error", "text-sm", "mt-xs") }) {
                                    Text("Passwords do not match")
                                }
                            }
                        }
                        
                        Button(attrs = {
                            classes("btn", "btn-primary")
                            if (state.isLoading) attr("disabled", "true")
                        }) {
                            if (state.isLoading) {
                                Span(attrs = { classes("spinner-sm") })
                                Text(" Saving...")
                            } else {
                                Text("Update Password")
                            }
                        }
                    }
                }
            }
        }
        
        Div(attrs = { classes("flex", "gap-md", "justify-center") }) {
            Button(attrs = {
                classes("btn", "btn-secondary")
                onClick { onNavigateBack() }
            }) {
                Text("Back to App")
            }
            Button(attrs = {
                classes("btn", "btn-danger")
                onClick { onLogout() }
            }) {
                Text("Sign Out")
            }
        }
    } ?: run {
        P(attrs = { classes("text-center") }) {
            Text("No user data available")
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Div(attrs = { 
        classes("profile-field")
        style {
            property("padding", "var(--spacing-sm) 0")
            property("border-bottom", "1px solid var(--border-color)")
        }
    }) {
        Label(attrs = { 
            classes("text-secondary")
            style { property("font-size", "0.85rem") }
        }) { 
            Text(label) 
        }
        P(attrs = { 
            style { 
                property("font-weight", "500")
                property("margin-top", "var(--spacing-xs)")
            }
        }) { 
            Text(value) 
        }
    }
}

@Composable
private fun RawHtml(html: String) {
    Span(attrs = {
        ref { element ->
            element.innerHTML = html
            onDispose { }
        }
        style {
            property("display", "flex")
            property("align-items", "center")
            property("margin-right", "var(--spacing-sm)")
        }
    })
}
