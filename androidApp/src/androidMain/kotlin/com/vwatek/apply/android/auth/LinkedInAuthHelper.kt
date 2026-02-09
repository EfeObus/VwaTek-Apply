package com.vwatek.apply.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Helper class for LinkedIn OAuth authentication using Custom Tabs
 */
class LinkedInAuthHelper(private val context: Context) {
    
    companion object {
        // LinkedIn OAuth configuration
        private const val LINKEDIN_CLIENT_ID = "86zpbbqqqa32et"
        private const val LINKEDIN_AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization"
        private const val LINKEDIN_REDIRECT_URI = "https://vwatek-backend-21443684777.us-central1.run.app/auth/linkedin/callback"
        
        // OAuth scopes
        private val SCOPES = listOf("openid", "profile", "email")
    }
    
    private var codeVerifier: String? = null
    
    /**
     * Starts the LinkedIn OAuth flow using Custom Tabs
     * @return The code verifier to be stored for the token exchange
     */
    fun startLinkedInAuth(): String {
        // Generate PKCE code verifier and challenge
        codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier!!)
        
        // Build the authorization URL
        val authUrl = buildAuthUrl(codeChallenge)
        
        // Open in Custom Tab
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
        
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
        
        return codeVerifier!!
    }
    
    /**
     * Starts LinkedIn OAuth without PKCE (for simpler flow)
     */
    fun startLinkedInAuthSimple() {
        val authUrl = Uri.Builder()
            .scheme("https")
            .authority("www.linkedin.com")
            .appendPath("oauth")
            .appendPath("v2")
            .appendPath("authorization")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", LINKEDIN_CLIENT_ID)
            .appendQueryParameter("redirect_uri", LINKEDIN_REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES.joinToString(" "))
            .appendQueryParameter("state", generateState())
            .build()
            .toString()
        
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    }
    
    private fun buildAuthUrl(codeChallenge: String): String {
        return Uri.Builder()
            .scheme("https")
            .authority("www.linkedin.com")
            .appendPath("oauth")
            .appendPath("v2")
            .appendPath("authorization")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", LINKEDIN_CLIENT_ID)
            .appendQueryParameter("redirect_uri", LINKEDIN_REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES.joinToString(" "))
            .appendQueryParameter("state", generateState())
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }
    
    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
    
    private fun generateState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Extracts the authorization code from a callback URI
     */
    fun extractAuthCode(uri: Uri): String? {
        return uri.getQueryParameter("code")
    }
    
    /**
     * Checks if the URI is a LinkedIn callback
     */
    fun isLinkedInCallback(uri: Uri): Boolean {
        return uri.toString().startsWith(LINKEDIN_REDIRECT_URI) ||
               uri.scheme == "vwatekapply" && uri.host == "linkedin-callback"
    }
}
