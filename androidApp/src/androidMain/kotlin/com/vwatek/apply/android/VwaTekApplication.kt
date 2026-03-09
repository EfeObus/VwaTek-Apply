package com.vwatek.apply.android

import android.app.Application
import android.util.Log
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

    companion object {
        private const val TAG = "VwaTekApplication"

        /** Whether Firebase was successfully initialized. */
        var firebaseAvailable: Boolean = false
            private set

        /**
         * Log a non-fatal exception to Crashlytics for debugging.
         */
        fun logException(throwable: Throwable, message: String? = null) {
            if (!firebaseAvailable) return
            try {
                message?.let { FirebaseCrashlytics.getInstance().log(it) }
                FirebaseCrashlytics.getInstance().recordException(throwable)
            } catch (e: Exception) {
                Log.w(TAG, "Crashlytics not available", e)
            }
        }

        /**
         * Log a custom message to Crashlytics.
         */
        fun logMessage(message: String) {
            if (!firebaseAvailable) return
            try {
                FirebaseCrashlytics.getInstance().log(message)
            } catch (e: Exception) {
                Log.w(TAG, "Crashlytics not available", e)
            }
        }

        /**
         * Set user identifier for crash reports (call after authentication).
         */
        fun setUserId(userId: String?) {
            if (!firebaseAvailable) return
            try {
                FirebaseCrashlytics.getInstance().setUserId(userId ?: "")
            } catch (e: Exception) {
                Log.w(TAG, "Crashlytics not available", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Try to initialize Firebase — it may fail if google-services.json
        // has placeholder values (e.g. during local development).
        firebaseAvailable = try {
            FirebaseApp.initializeApp(this)
            // Configure Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            }
            Log.i(TAG, "Firebase initialized successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped — invalid config: ${e.message}")
            false
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
}
