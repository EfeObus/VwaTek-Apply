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
    
    var apiKey by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load existing API key
    LaunchedEffect(Unit) {
        val existingKey = settingsRepository.getSetting("gemini_api_key")
        if (existingKey != null) {
            apiKey = existingKey
        }
        isLoading = false
    }
    
    Div {
        // Header
        H1(attrs = { classes("mb-lg") }) { Text("Settings") }
        
        // API Configuration Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("API Configuration") }
            
            Div(attrs = { classes("form-group") }) {
                Label(attrs = { classes("form-label") }) { Text("Gemini API Key") }
                Input(InputType.Password) {
                    classes("form-input")
                    placeholder("Enter your Gemini API key")
                    value(apiKey)
                    onInput { apiKey = it.value }
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
            
            Button(attrs = {
                classes("btn", "btn-primary")
                onClick {
                    scope.launch {
                        settingsRepository.setSetting("gemini_api_key", apiKey)
                        savedMessage = "API key saved successfully"
                        kotlinx.browser.window.setTimeout({
                            savedMessage = null
                        }, 3000)
                    }
                }
            }) {
                Text("Save API Key")
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
