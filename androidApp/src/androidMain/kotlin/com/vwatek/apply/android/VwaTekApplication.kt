package com.vwatek.apply.android

import android.app.Application
import android.util.Log
import com.vwatek.apply.android.di.androidModule
import com.vwatek.apply.di.platformModule
import com.vwatek.apply.di.sharedModule
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.i18n.Locale
import com.vwatek.apply.i18n.LocaleManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VwaTekApplication : Application() {

    companion object {
        private const val TAG = "VwaTekApplication"
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@VwaTekApplication)
            modules(
                androidModule(this@VwaTekApplication),
                platformModule(),
                sharedModule
            )
        }

        // Restore saved locale preference
        try {
            val settingsRepository = GlobalContext.get().get<SettingsRepository>()
            kotlinx.coroutines.runBlocking {
                val savedLocale = settingsRepository.getSetting("locale")
                if (savedLocale == "fr") {
                    LocaleManager.setLocale(Locale.FRENCH)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore locale: ${e.message}")
        }
    }
}
