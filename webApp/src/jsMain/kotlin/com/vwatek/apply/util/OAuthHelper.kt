package com.vwatek.apply.util

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

/**
 * OAuth Helper for Google and LinkedIn authentication
 */
object OAuthHelper {
    
    // OAuth Configuration
    private const val LINKEDIN_CLIENT_ID = "86zpbbqqqa32et"
    private const val GOOGLE_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID" // User needs to set this
    
    /**
     * Initialize Google Sign-In
     */
    fun initializeGoogleSignIn(
        clientId: String = GOOGLE_CLIENT_ID,
        onSuccess: (userInfo: GoogleUserInfo) -> Unit,
        onError: (error: String) -> Unit
    ) {
        try {
            val google = js("window.google")
            if (google == null || google == undefined) {
                onError("Google Identity Services not loaded")
                return
            }
            
            val config = js("{}")
            config.client_id = clientId
            config.callback = { response: dynamic ->
                val credential = response.credential as? String
                if (credential != null) {
                    val userInfo = decodeGoogleToken(credential)
                    if (userInfo != null) {
                        onSuccess(userInfo)
                    } else {
                        onError("Failed to decode user info")
                    }
                } else {
                    onError("No credential received")
                }
            }
            config.auto_select = false
            config.cancel_on_tap_outside = true
            
            google.accounts.id.initialize(config)
        } catch (e: Exception) {
            onError("Failed to initialize Google Sign-In: ${e.message}")
        }
    }
    
    /**
     * Prompt Google Sign-In popup
     */
    fun promptGoogleSignIn() {
        try {
            val google = js("window.google")
            if (google != null && google != undefined) {
                google.accounts.id.prompt { _: dynamic -> }
            }
        } catch (e: Exception) {
            console.error("Google Sign-In prompt error: ${e.message}")
        }
    }
    
    /**
     * Render Google Sign-In button
     */
    fun renderGoogleButton(elementId: String, theme: String = "outline", size: String = "large") {
        try {
            val google = js("window.google")
            val element = js("document.getElementById(elementId)")
            
            if (google != null && google != undefined && element != null) {
                val options = js("{}")
                options.type = "standard"
                options.theme = theme
                options.size = size
                options.text = "continue_with"
                options.shape = "rectangular"
                options.logo_alignment = "left"
                
                google.accounts.id.renderButton(element, options)
            }
        } catch (e: Exception) {
            console.error("Google button render error: ${e.message}")
        }
    }
    
    /**
     * Decode Google JWT token to extract user info
     */
    fun decodeGoogleToken(credential: String): GoogleUserInfo? {
        return try {
            // JWT is in format: header.payload.signature
            val parts = credential.split(".")
            if (parts.size != 3) return null
            
            // Decode the payload (base64)
            val payload = parts[1]
            val decoded = js("atob(payload.replace(/-/g, '+').replace(/_/g, '/'))")
            val json = JSON.parse<dynamic>(decoded as String)
            
            GoogleUserInfo(
                id = json.sub as? String ?: "",
                email = json.email as? String ?: "",
                emailVerified = json.email_verified as? Boolean ?: false,
                name = json.name as? String ?: "",
                givenName = json.given_name as? String ?: "",
                familyName = json.family_name as? String ?: "",
                picture = json.picture as? String
            )
        } catch (e: Exception) {
            console.error("Token decode error: ${e.message}")
            null
        }
    }
    
    /**
     * Get LinkedIn authorization URL
     */
    fun getLinkedInAuthUrl(
        clientId: String = LINKEDIN_CLIENT_ID,
        redirectUri: String = "${window.location.origin}/linkedin-callback",
        state: String = generateRandomState()
    ): String {
        // Store state for verification
        window.localStorage.setItem("linkedin_oauth_state", state)
        
        val scopes = listOf("openid", "profile", "email").joinToString("%20")
        
        return buildString {
            append("https://www.linkedin.com/oauth/v2/authorization?")
            append("response_type=code")
            append("&client_id=$clientId")
            append("&redirect_uri=${js("encodeURIComponent(redirectUri)")}")
            append("&scope=$scopes")
            append("&state=$state")
        }
    }
    
    /**
     * Open LinkedIn OAuth in popup window
     * Uses a safer approach that doesn't throw cross-origin errors
     */
    fun openLinkedInPopup(
        onSuccess: (code: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val authUrl = getLinkedInAuthUrl()
        
        // Calculate popup position
        val width = 600
        val height = 700
        val left = (window.screen.width - width) / 2
        val top = (window.screen.height - height) / 2
        
        val features = "width=$width,height=$height,left=$left,top=$top,toolbar=no,menubar=no,scrollbars=yes"
        
        val popup = window.open(authUrl, "LinkedIn Login", features)
        
        if (popup == null) {
            onError("Popup blocked. Please allow popups for this site.")
            return
        }
        
        // Store interval ID for cleanup
        var intervalId: Int = 0
        var checkCount = 0
        val maxChecks = 600 // 5 minutes max (500ms * 600)
        
        // Poll for callback - using safe checks only
        intervalId = window.setInterval({
            checkCount++
            
            // Timeout after max checks
            if (checkCount > maxChecks) {
                window.clearInterval(intervalId)
                try { popup.close() } catch (e: dynamic) {}
                onError("Login timed out")
                return@setInterval
            }
            
            // First check if popup was closed
            val isClosed = try {
                popup.closed == true
            } catch (e: dynamic) {
                true // Assume closed if we can't check
            }
            
            if (isClosed) {
                window.clearInterval(intervalId)
                onError("Login cancelled")
                return@setInterval
            }
            
            // Safely try to read the URL only when same-origin
            val popupOrigin = try {
                js("popup.location.origin") as? String
            } catch (e: dynamic) {
                null // Cross-origin, can't access
            }
            
            // Only proceed if we successfully got origin AND it matches ours
            if (popupOrigin != null && popupOrigin == window.location.origin) {
                val popupHref = try {
                    js("popup.location.href") as? String
                } catch (e: dynamic) {
                    null
                }
                
                if (popupHref != null) {
                    when {
                        popupHref.contains("code=") -> {
                            val code = extractUrlParam(popupHref, "code")
                            val state = extractUrlParam(popupHref, "state")
                            
                            // Verify state
                            val storedState = window.localStorage.getItem("linkedin_oauth_state")
                            window.localStorage.removeItem("linkedin_oauth_state")
                            
                            window.clearInterval(intervalId)
                            try { popup.close() } catch (e: dynamic) {}
                            
                            if (code != null && state == storedState) {
                                onSuccess(code)
                            } else if (code != null) {
                                onError("State mismatch - potential CSRF attack")
                            } else {
                                onError("No authorization code received")
                            }
                        }
                        popupHref.contains("error=") -> {
                            val error = extractUrlParam(popupHref, "error_description") 
                                ?: extractUrlParam(popupHref, "error") 
                                ?: "Unknown error"
                            window.clearInterval(intervalId)
                            try { popup.close() } catch (e: dynamic) {}
                            onError(error)
                        }
                    }
                }
            }
            // If cross-origin (popupOrigin is null), silently continue polling
        }, 500)
    }
    
    /**
     * Handle LinkedIn callback (for redirect flow)
     */
    fun handleLinkedInCallback(): LinkedInCallbackResult {
        val url = window.location.href
        
        if (!url.contains("linkedin-callback") && !url.contains("code=") && !url.contains("error=")) {
            return LinkedInCallbackResult.NoCallback
        }
        
        val code = extractUrlParam(url, "code")
        val error = extractUrlParam(url, "error")
        val errorDescription = extractUrlParam(url, "error_description")
        val state = extractUrlParam(url, "state")
        
        // Verify state
        val storedState = window.localStorage.getItem("linkedin_oauth_state")
        window.localStorage.removeItem("linkedin_oauth_state")
        
        return when {
            error != null -> LinkedInCallbackResult.Error(errorDescription ?: error)
            code != null && state == storedState -> LinkedInCallbackResult.Success(code)
            code != null -> LinkedInCallbackResult.Error("State mismatch")
            else -> LinkedInCallbackResult.NoCallback
        }
    }
    
    /**
     * Extract URL parameter
     */
    private fun extractUrlParam(url: String, param: String): String? {
        val regex = Regex("$param=([^&]+)")
        val match = regex.find(url)
        return match?.groupValues?.get(1)?.let {
            js("decodeURIComponent(it)") as String
        }
    }
    
    /**
     * Generate random state for CSRF protection
     */
    private fun generateRandomState(): String {
        val array = js("new Uint8Array(16)")
        js("crypto.getRandomValues(array)")
        return (0 until 16).map { 
            val byte = array[it] as Int
            byte.toString(16).padStart(2, '0')
        }.joinToString("")
    }
}

/**
 * Google user info from decoded JWT
 */
data class GoogleUserInfo(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val name: String,
    val givenName: String,
    val familyName: String,
    val picture: String?
)

/**
 * LinkedIn callback result
 */
sealed class LinkedInCallbackResult {
    data class Success(val code: String) : LinkedInCallbackResult()
    data class Error(val error: String) : LinkedInCallbackResult()
    object NoCallback : LinkedInCallbackResult()
}

// JSON parsing
external object JSON {
    fun <T> parse(text: String): T
    fun stringify(value: dynamic): String
}

private external val console: dynamic
