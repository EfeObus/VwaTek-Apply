package com.vwatek.apply.android

import android.app.Application
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
