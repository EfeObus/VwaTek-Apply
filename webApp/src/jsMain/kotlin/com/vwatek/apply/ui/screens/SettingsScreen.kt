package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.vwatek.apply.domain.repository.SettingsRepository
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val settingsRepository = remember { GlobalContext.get().get<SettingsRepository>() }
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    var geminiApiKey by remember { mutableStateOf("") }
    var openAiApiKey by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load existing API keys
    LaunchedEffect(Unit) {
        val existingGeminiKey = settingsRepository.getSetting("gemini_api_key")
        val existingOpenAiKey = settingsRepository.getSetting("openai_api_key")
        if (existingGeminiKey != null) {
            geminiApiKey = existingGeminiKey
        }
        if (existingOpenAiKey != null) {
            openAiApiKey = existingOpenAiKey
        }
        isLoading = false
    }
    
    Div {
        // Header
        H1(attrs = { classes("mb-lg") }) { Text("Settings") }
        
        // API Configuration Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("AI API Configuration") }
            
            P(attrs = { classes("text-secondary", "mb-lg") }) {
                Text("Configure your AI API keys. Gemini is the primary AI engine. OpenAI serves as a fallback if Gemini fails.")
            }
            
            // Gemini API Key
            Div(attrs = { classes("form-group", "mb-lg") }) {
                Label(attrs = { classes("form-label") }) { Text("Gemini API Key (Primary)") }
                Input(InputType.Password) {
                    classes("form-input")
                    placeholder("Enter your Gemini API key")
                    value(geminiApiKey)
                    onInput { geminiApiKey = it.value }
                }
                P(attrs = { classes("form-helper") }) {
                    Text("Get your API key from ")
                    A(href = "https://aistudio.google.com/", attrs = {
                        attr("target", "_blank")
                        attr("rel", "noopener noreferrer")
                    }) {
                        Text("Google AI Studio")
                    }
                }
            }
            
            // OpenAI API Key
            Div(attrs = { classes("form-group", "mb-lg") }) {
                Label(attrs = { classes("form-label") }) { Text("OpenAI API Key (Fallback)") }
                Input(InputType.Password) {
                    classes("form-input")
                    placeholder("Enter your OpenAI API key (optional)")
                    value(openAiApiKey)
                    onInput { openAiApiKey = it.value }
                }
                P(attrs = { classes("form-helper") }) {
                    Text("Get your API key from ")
                    A(href = "https://platform.openai.com/api-keys", attrs = {
                        attr("target", "_blank")
                        attr("rel", "noopener noreferrer")
                    }) {
                        Text("OpenAI Platform")
                    }
                    Text(" - Used as fallback if Gemini API fails")
                }
            }
            
            // API Key Status
            Div(attrs = { classes("mb-lg") }) {
                H4(attrs = { classes("mb-sm") }) { Text("Status") }
                Div(attrs = { classes("flex", "gap-lg") }) {
                    Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                        Span(attrs = {
                            classes(if (geminiApiKey.isNotBlank()) "status-dot-success" else "status-dot-error")
                        })
                        Text("Gemini: ${if (geminiApiKey.isNotBlank()) "Configured" else "Not configured"}")
                    }
                    Div(attrs = { classes("flex", "items-center", "gap-sm") }) {
                        Span(attrs = {
                            classes(if (openAiApiKey.isNotBlank()) "status-dot-success" else "status-dot-warning")
                        })
                        Text("OpenAI: ${if (openAiApiKey.isNotBlank()) "Configured" else "Not configured (optional)"}")
                    }
                }
            }
            
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick {
                    scope.launch {
                        if (geminiApiKey.isNotBlank()) {
                            settingsRepository.setSetting("gemini_api_key", geminiApiKey)
                        }
                        if (openAiApiKey.isNotBlank()) {
                            settingsRepository.setSetting("openai_api_key", openAiApiKey)
                        }
                        savedMessage = "API keys saved successfully"
                        kotlinx.browser.window.setTimeout({
                            savedMessage = null
                        }, 3000)
                    }
                }
            }) {
                Text("Save API Keys")
            }
            
            savedMessage?.let { message ->
                P(attrs = { classes("text-success", "mt-md") }) {
                    Text(message)
                }
            }
        }
        
        // About Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("About VwaTek Apply") }
            
            Div(attrs = { classes("mb-md") }) {
                P(attrs = { classes("mb-sm") }) {
                    B { Text("Version: ") }
                    Text("1.0.0")
                }
                P(attrs = { classes("mb-sm") }) {
                    B { Text("Platform: ") }
                    Text("Web")
                }
                P {
                    B { Text("Powered by: ") }
                    Text("Gemini 3 Flash")
                }
            }
            
            P(attrs = { classes("text-secondary") }) {
                Text("VwaTek Apply is a professional AI career suite that helps you optimize resumes, generate cover letters, and prepare for interviews.")
            }
        }
        
        // Data Management Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Data Management") }
            
            P(attrs = { classes("text-secondary", "mb-md") }) {
                Text("All your data is stored locally in your browser. No data is sent to our servers.")
            }
            
            Div(attrs = { classes("flex", "gap-sm") }) {
                Button(attrs = {
                    classes("btn", "btn-outline")
                }) {
                    Text("Export Data")
                }
                Button(attrs = {
                    classes("btn", "btn-danger")
                    onClick {
                        if (kotlinx.browser.window.confirm("Are you sure you want to clear all data? This cannot be undone.")) {
                            // Clear all data
                            kotlinx.browser.window.localStorage.clear()
                            kotlinx.browser.window.location.reload()
                        }
                    }
                }) {
                    Text("Clear All Data")
                }
            }
        }
        
        // Privacy & Security Card
        Div(attrs = { classes("card") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Privacy & Security") }
            
            Div(attrs = { classes("mb-md") }) {
                H4(attrs = { classes("mb-sm") }) { Text("Data Storage") }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("All data is stored locally in your browser using IndexedDB with encryption. Your resumes, cover letters, and interview data never leave your device.")
                }
            }
            
            Div(attrs = { classes("mb-md") }) {
                H4(attrs = { classes("mb-sm") }) { Text("API Communication") }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("When using AI features, only the necessary content is sent to Google's Gemini API for processing. Your API key is stored securely in your browser.")
                }
            }
            
            Div {
                H4(attrs = { classes("mb-sm") }) { Text("No Account Required") }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("VwaTek Apply does not require account creation. You maintain full control over your data at all times.")
                }
            }
        }
    }
}
