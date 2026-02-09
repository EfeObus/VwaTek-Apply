package com.vwatek.apply.android.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Helper class for Google Sign-In using Android Credential Manager.
 * This provides a modern, secure approach to Google authentication on Android.
 */
class GoogleSignInHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleSignInHelper"
        
        // Web Client ID from Google Cloud Console
        // This should be the OAuth 2.0 Client ID for Web application type
        const val WEB_CLIENT_ID = "21443684777-vp58jf5fq1k7lvqk8m5eo5v5dv8m1h9t.apps.googleusercontent.com"
    }
    
    private val credentialManager = CredentialManager.create(context)
    
    /**
     * Result of Google Sign-In attempt
     */
    sealed class SignInResult {
        data class Success(
            val idToken: String,
            val email: String,
            val displayName: String?,
            val givenName: String?,
            val familyName: String?,
            val profilePictureUri: String?
        ) : SignInResult()
        
        data class Error(val message: String, val exception: Exception? = null) : SignInResult()
        object Cancelled : SignInResult()
        object NoCredentials : SignInResult()
    }
    
    /**
     * Initiates Google Sign-In flow using Credential Manager.
     * This is the recommended approach for Android 14+ and works on older versions too.
     */
    suspend fun signIn(): SignInResult = withContext(Dispatchers.IO) {
        try {
            // Generate a nonce for security
            val nonce = generateNonce()
            
            // Configure Google ID request
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all accounts, not just authorized ones
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true) // Auto-select if only one account
                .setNonce(nonce)
                .build()
            
            // Build the credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            // Request credentials
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            
            handleSignInResponse(result)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user")
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No credentials available")
            SignInResult.NoCredentials
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential exception: ${e.message}", e)
            SignInResult.Error("Failed to get credentials: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            SignInResult.Error("Unexpected error: ${e.message}", e)
        }
    }
    
    /**
     * Try to sign in with previously authorized account (silent sign-in).
     */
    suspend fun signInWithExistingAccount(): SignInResult = withContext(Dispatchers.IO) {
        try {
            val nonce = generateNonce()
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true) // Only show authorized accounts
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .setNonce(nonce)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            
            handleSignInResponse(result)
        } catch (e: NoCredentialException) {
            SignInResult.NoCredentials
        } catch (e: Exception) {
            SignInResult.Error("Silent sign-in failed: ${e.message}", e)
        }
    }
    
    /**
     * Signs out the user by clearing saved credentials.
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            // Credential Manager doesn't have explicit sign-out,
            // but we can clear any local state here if needed
            Log.d(TAG, "Sign out completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
        }
    }
    
    private fun handleSignInResponse(result: GetCredentialResponse): SignInResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        
                        Log.d(TAG, "Successfully obtained Google ID token")
                        
                        SignInResult.Success(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id, // The email is the ID for Google
                            displayName = googleIdTokenCredential.displayName,
                            givenName = googleIdTokenCredential.givenName,
                            familyName = googleIdTokenCredential.familyName,
                            profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse Google ID token credential", e)
                        SignInResult.Error("Failed to parse credentials: ${e.message}", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    SignInResult.Error("Unexpected credential type")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential class: ${credential.javaClass.name}")
                SignInResult.Error("Unexpected credential type")
            }
        }
    }
    
    /**
     * Generates a secure random nonce for the authentication request.
     */
    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }
}
