package com.vwatek.apply.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.i18n.Locale
import com.vwatek.apply.i18n.LocaleManager
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
    
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Notification preferences
    var emailNotifications by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var weeklyDigest by remember { mutableStateOf(false) }
    var interviewReminders by remember { mutableStateOf(true) }
    
    // Appearance preferences
    var darkMode by remember { mutableStateOf(false) }
    var compactMode by remember { mutableStateOf(false) }
    
    // Language preferences
    var frenchEnabled by remember { mutableStateOf(false) }
    
    // Privacy preferences
    var analyticsEnabled by remember { mutableStateOf(true) }
    var crashReportingEnabled by remember { mutableStateOf(true) }
    
    // Load existing settings
    LaunchedEffect(Unit) {
        // Load notification preferences
        emailNotifications = settingsRepository.getSetting("email_notifications") != "false"
        pushNotifications = settingsRepository.getSetting("push_notifications") != "false"
        weeklyDigest = settingsRepository.getSetting("weekly_digest") == "true"
        interviewReminders = settingsRepository.getSetting("interview_reminders") != "false"
        
        // Load appearance preferences
        darkMode = settingsRepository.getSetting("dark_mode") == "true"
        compactMode = settingsRepository.getSetting("compact_mode") == "true"
        
        // Load language preferences
        frenchEnabled = settingsRepository.getSetting("french_enabled") == "true"
        
        // Load privacy preferences
        analyticsEnabled = settingsRepository.getSetting("analytics_enabled") != "false"
        crashReportingEnabled = settingsRepository.getSetting("crash_reporting_enabled") != "false"
        
        // Apply dark mode if enabled
        if (darkMode) {
            kotlinx.browser.document.documentElement?.classList?.add("dark-mode")
        }
        
        isLoading = false
    }
    
    Div {
        // Header
        H1(attrs = { classes("mb-lg") }) { Text("Settings") }
        
        // Error banner
        if (errorMessage != null) {
            Div(attrs = {
                classes("alert", "alert-error")
                style {
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "space-between")
                    property("padding", "12px 16px")
                    property("margin-bottom", "16px")
                    property("background", "#fef2f2")
                    property("border", "1px solid #fca5a5")
                    property("border-radius", "8px")
                    property("color", "#dc2626")
                }
            }) {
                Span { Text(errorMessage ?: "") }
                Button(attrs = {
                    style {
                        property("background", "none")
                        property("border", "none")
                        property("cursor", "pointer")
                        property("color", "#dc2626")
                        property("font-size", "1.2rem")
                    }
                    onClick { errorMessage = null }
                }) { Text("×") }
            }
        }
        
        if (isLoading) {
            Div(attrs = {
                style {
                    property("display", "flex")
                    property("justify-content", "center")
                    property("align-items", "center")
                    property("padding", "48px")
                }
            }) {
                Span(attrs = {
                    classes("spinner")
                    style {
                        property("width", "32px")
                        property("height", "32px")
                        property("border", "3px solid #e5e7eb")
                        property("border-top-color", "#6366f1")
                        property("border-radius", "50%")
                        property("animation", "spin 0.8s linear infinite")
                    }
                })
                Span(attrs = { style { property("margin-left", "12px") } }) { Text("Loading settings...") }
            }
        } else {
        // Notifications Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Notifications") }
            
            P(attrs = { classes("text-secondary", "mb-lg") }) {
                Text("Configure how and when you receive notifications about your job applications.")
            }
            
            Div(attrs = { classes("settings-toggles") }) {
                // Email Notifications
                SettingsToggle(
                    label = "Email Notifications",
                    description = "Receive important updates via email",
                    checked = emailNotifications,
                    onToggle = { checked ->
                        emailNotifications = checked
                        scope.launch {
                            settingsRepository.setSetting("email_notifications", checked.toString())
                        }
                    }
                )
                
                // Push Notifications
                SettingsToggle(
                    label = "Push Notifications",
                    description = "Browser push notifications for real-time alerts",
                    checked = pushNotifications,
                    onToggle = { checked ->
                        pushNotifications = checked
                        scope.launch {
                            settingsRepository.setSetting("push_notifications", checked.toString())
                        }
                    }
                )
                
                // Weekly Digest
                SettingsToggle(
                    label = "Weekly Digest",
                    description = "Receive a weekly summary of your activity",
                    checked = weeklyDigest,
                    onToggle = { checked ->
                        weeklyDigest = checked
                        scope.launch {
                            settingsRepository.setSetting("weekly_digest", checked.toString())
                        }
                    }
                )
                
                // Interview Reminders
                SettingsToggle(
                    label = "Interview Reminders",
                    description = "Get reminded before scheduled interviews",
                    checked = interviewReminders,
                    onToggle = { checked ->
                        interviewReminders = checked
                        scope.launch {
                            settingsRepository.setSetting("interview_reminders", checked.toString())
                        }
                    }
                )
            }
        }
        
        // Appearance Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Appearance") }
            
            P(attrs = { classes("text-secondary", "mb-lg") }) {
                Text("Customize the look and feel of the application.")
            }
            
            Div(attrs = { classes("settings-toggles") }) {
                // Dark Mode
                SettingsToggle(
                    label = "Dark Mode",
                    description = "Use dark color theme for reduced eye strain",
                    checked = darkMode,
                    onToggle = { checked ->
                        darkMode = checked
                        scope.launch {
                            settingsRepository.setSetting("dark_mode", checked.toString())
                        }
                        // Apply or remove dark mode class
                        if (checked) {
                            kotlinx.browser.document.documentElement?.classList?.add("dark-mode")
                        } else {
                            kotlinx.browser.document.documentElement?.classList?.remove("dark-mode")
                        }
                    }
                )
                
                // Compact Mode
                SettingsToggle(
                    label = "Compact Mode",
                    description = "Reduce spacing for more content on screen",
                    checked = compactMode,
                    onToggle = { checked ->
                        compactMode = checked
                        scope.launch {
                            settingsRepository.setSetting("compact_mode", checked.toString())
                        }
                        // Apply or remove compact mode class
                        if (checked) {
                            kotlinx.browser.document.documentElement?.classList?.add("compact-mode")
                        } else {
                            kotlinx.browser.document.documentElement?.classList?.remove("compact-mode")
                        }
                    }
                )
            }
        }
        
        // Language Card
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Language") }
            
            P(attrs = { classes("text-secondary", "mb-lg") }) {
                Text("Choose your preferred language for the interface.")
            }
            
            Div(attrs = { classes("settings-toggles") }) {
                SettingsToggle(
                    label = if (frenchEnabled) "Français" else "French Language",
                    description = if (frenchEnabled) "Activé" else "Enable French language",
                    checked = frenchEnabled,
                    onToggle = { checked ->
                        frenchEnabled = checked
                        LocaleManager.setLocale(if (checked) Locale.FRENCH else Locale.ENGLISH)
                        scope.launch {
                            settingsRepository.setSetting("french_enabled", checked.toString())
                        }
                    }
                )
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
        Div(attrs = { classes("card", "mb-lg") }) {
            H3(attrs = { classes("card-title", "mb-md") }) { Text("Privacy & Security") }
            
            Div(attrs = { classes("settings-toggles", "mb-lg") }) {
                SettingsToggle(
                    label = "Analytics",
                    description = "Help improve the app with anonymous usage data",
                    checked = analyticsEnabled,
                    onToggle = { checked ->
                        analyticsEnabled = checked
                        scope.launch {
                            settingsRepository.setSetting("analytics_enabled", checked.toString())
                        }
                    }
                )
                
                SettingsToggle(
                    label = "Crash Reporting",
                    description = "Automatically report crashes to help fix bugs",
                    checked = crashReportingEnabled,
                    onToggle = { checked ->
                        crashReportingEnabled = checked
                        scope.launch {
                            settingsRepository.setSetting("crash_reporting_enabled", checked.toString())
                        }
                    }
                )
            }
            
            Div(attrs = { classes("mb-md") }) {
                H4(attrs = { classes("mb-sm") }) { Text("Data Storage") }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("All data is stored locally in your browser using IndexedDB with encryption. Your resumes, cover letters, and interview data never leave your device.")
                }
            }
            
            Div(attrs = { classes("mb-md") }) {
                H4(attrs = { classes("mb-sm") }) { Text("API Communication") }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("When using AI features, only the necessary content is sent to our secure backend for processing. AI API keys are managed server-side and never stored in your browser.")
                }
            }
            
            Div {
                H4(attrs = { classes("mb-sm") }) { Text("No Account Required") }
                P(attrs = { classes("text-secondary", "text-sm") }) {
                    Text("VwaTek Apply does not require account creation. You maintain full control over your data at all times.")
                }
            }
            
            // Links
            Div(attrs = { classes("mt-lg", "pt-md", "border-top") }) {
                Div(attrs = { classes("flex", "gap-lg") }) {
                    A(href = "#privacy", attrs = { classes("text-primary") }) {
                        Text("Privacy Policy")
                    }
                    A(href = "#terms", attrs = { classes("text-primary") }) {
                        Text("Terms of Service")
                    }
                    A(href = "mailto:support@vwatek.com", attrs = { classes("text-primary") }) {
                        Text("Help & Support")
                    }
                }
            }
        }
        } // else - loading check
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Div(attrs = { 
        classes("settings-toggle-item")
        style {
            property("display", "flex")
            property("justify-content", "space-between")
            property("align-items", "flex-start")
            property("padding", "var(--spacing-md) 0")
            property("border-bottom", "1px solid var(--border-color)")
        }
    }) {
        Div(attrs = { style { property("flex", "1") } }) {
            Label(attrs = { 
                classes("form-label")
                style { property("margin-bottom", "var(--spacing-xs)") }
            }) { 
                Text(label) 
            }
            P(attrs = { classes("text-secondary", "text-sm") }) {
                Text(description)
            }
        }
        
        Label(attrs = {
            classes("toggle-switch")
            style {
                property("position", "relative")
                property("display", "inline-block")
                property("width", "50px")
                property("height", "26px")
                property("margin-left", "var(--spacing-md)")
            }
        }) {
            Input(InputType.Checkbox) {
                checked(checked)
                onInput { onToggle(it.value) }
                style {
                    property("opacity", "0")
                    property("width", "0")
                    property("height", "0")
                }
            }
            Span(attrs = {
                classes("toggle-slider")
                style {
                    property("position", "absolute")
                    property("cursor", "pointer")
                    property("top", "0")
                    property("left", "0")
                    property("right", "0")
                    property("bottom", "0")
                    property("background-color", if (checked) "var(--primary-color)" else "#ccc")
                    property("transition", "0.3s")
                    property("border-radius", "26px")
                }
            }) {
                Span(attrs = {
                    style {
                        property("position", "absolute")
                        property("content", "")
                        property("height", "20px")
                        property("width", "20px")
                        property("left", if (checked) "27px" else "3px")
                        property("bottom", "3px")
                        property("background-color", "white")
                        property("transition", "0.3s")
                        property("border-radius", "50%")
                    }
                })
            }
        }
    }
}
