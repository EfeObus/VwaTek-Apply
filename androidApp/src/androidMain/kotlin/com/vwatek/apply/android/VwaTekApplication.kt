package com.vwatek.apply.android

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vwatek.apply.android.di.androidModule
import com.vwatek.apply.di.platformModule
import com.vwatek.apply.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VwaTekApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            // Disable collection in debug builds
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            
            // Set custom keys for better crash context
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        }
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@VwaTekApplication)
            modules(
                androidModule(this@VwaTekApplication),
                platformModule(),
                sharedModule
            )
        }
    }
    
    companion object {
        /**
         * Log a non-fatal exception to Crashlytics for debugging
         */
        fun logException(throwable: Throwable, message: String? = null) {
            message?.let { FirebaseCrashlytics.getInstance().log(it) }
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
        
        /**
         * Log a custom message to Crashlytics
         */
        fun logMessage(message: String) {
            FirebaseCrashlytics.getInstance().log(message)
        }
        
        /**
         * Set user identifier for crash reports (call after authentication)
         */
        fun setUserId(userId: String?) {
            FirebaseCrashlytics.getInstance().setUserId(userId ?: "")
        }
    }
}
