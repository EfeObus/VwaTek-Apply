package com.vwatek.apply

import com.vwatek.apply.di.platformModule
import com.vwatek.apply.di.sharedModule
import com.vwatek.apply.monitoring.SentryConfig
import com.vwatek.apply.monitoring.setupGlobalErrorHandler
import com.vwatek.apply.ui.App
import org.jetbrains.compose.web.renderComposable
import org.koin.core.context.startKoin

fun main() {
    // Initialize error tracking first to catch initialization errors
    SentryConfig.init()
    setupGlobalErrorHandler()
    
    initKoin()
    
    renderComposable(rootElementId = "root") {
        App()
    }
}

private fun initKoin() {
    startKoin {
        modules(sharedModule, platformModule())
    }
}

external object JSON {
    fun parse(text: String): dynamic
    fun stringify(value: dynamic): String
}
