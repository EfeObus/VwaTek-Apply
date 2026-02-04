package com.vwatek.apply.android.di

import android.content.Context
import com.vwatek.apply.data.local.initDatabaseContext
import org.koin.dsl.module

fun androidModule(context: Context) = module {
    single { context.applicationContext }
    
    // Initialize database context
    single {
        initDatabaseContext(context)
        context
    }
}
