package com.vwatek.apply

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.di.platformModule
import com.vwatek.apply.di.sharedModule
import com.vwatek.apply.monitoring.SentryConfig
import com.vwatek.apply.monitoring.setupGlobalErrorHandler
import com.vwatek.apply.ui.App
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.koin.core.context.startKoin

fun main() {
    // Initialize error tracking first to catch initialization errors
    SentryConfig.init()
    setupGlobalErrorHandler()
    
    initKoin()
    initializeDefaultApiKeys()
    
    renderComposable(rootElementId = "root") {
        App()
    }
}

private fun initKoin() {
    startKoin {
        modules(sharedModule, platformModule())
    }
}

/**
 * Initialize default API keys if not already configured.
 * Keys should be configured via the Settings screen or by setting
 * window.VWATEK_CONFIG before the app loads.
 */
private fun initializeDefaultApiKeys() {
    val storageKey = "vwatek_settings"
    val existing = localStorage.getItem(storageKey)
    
    // Check if there's a global config object injected (e.g., from index.html)
    val globalConfig: dynamic = js("window.VWATEK_CONFIG")
    
    // Parse existing settings or create empty object
    val settings: dynamic = if (existing != null) {
        try {
            JSON.parse(existing)
        } catch (e: Exception) {
            js("{}")
        }
    } else {
        js("{}")
    }
    
    var modified = false
    
    // Pre-populate Gemini API key from global config if not already set
    val geminiKey = settings["gemini_api_key"] as? String
    if (geminiKey.isNullOrBlank() && globalConfig != null && globalConfig != undefined) {
        val configGeminiKey = globalConfig.GEMINI_API_KEY as? String
        if (!configGeminiKey.isNullOrBlank()) {
            settings["gemini_api_key"] = configGeminiKey
            modified = true
        }
    }
    
    // Pre-populate OpenAI API key from global config if not already set
    val openaiKey = settings["openai_api_key"] as? String
    if (openaiKey.isNullOrBlank() && globalConfig != null && globalConfig != undefined) {
        val configOpenAiKey = globalConfig.OPENAI_API_KEY as? String
        if (!configOpenAiKey.isNullOrBlank()) {
            settings["openai_api_key"] = configOpenAiKey
            modified = true
        }
    }
    
    if (modified) {
        localStorage.setItem(storageKey, JSON.stringify(settings))
    }
}

external object JSON {
    fun parse(text: String): dynamic
    fun stringify(value: dynamic): String
}
