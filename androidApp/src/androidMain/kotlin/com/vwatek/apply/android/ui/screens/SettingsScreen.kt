package com.vwatek.apply.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.i18n.LocaleManager
import com.vwatek.apply.i18n.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Settings Screen for Android
 * Manages app preferences, API keys, and user settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val settingsRepository: SettingsRepository = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // API Keys
    var geminiApiKey by remember { mutableStateOf("") }
    var openAiApiKey by remember { mutableStateOf("") }
    var showApiKeys by remember { mutableStateOf(false) }
    
    // Notification preferences
    var pushNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(true) }
    var interviewReminders by remember { mutableStateOf(true) }
    var weeklyDigest by remember { mutableStateOf(false) }
    
    // Appearance
    var darkMode by remember { mutableStateOf(false) }
    var currentLocale by remember { mutableStateOf(Locale.ENGLISH) }
    
    // Privacy
    var analyticsEnabled by remember { mutableStateOf(true) }
    var crashReportingEnabled by remember { mutableStateOf(true) }
    
    // Load settings
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        geminiApiKey = settingsRepository.getSetting("gemini_api_key") ?: ""
        openAiApiKey = settingsRepository.getSetting("openai_api_key") ?: ""
        pushNotifications = settingsRepository.getSetting("push_notifications") != "false"
        emailNotifications = settingsRepository.getSetting("email_notifications") != "false"
        interviewReminders = settingsRepository.getSetting("interview_reminders") != "false"
        weeklyDigest = settingsRepository.getSetting("weekly_digest") == "true"
        darkMode = settingsRepository.getSetting("dark_mode") == "true"
        analyticsEnabled = settingsRepository.getSetting("analytics_enabled") != "false"
        crashReportingEnabled = settingsRepository.getSetting("crash_reporting_enabled") != "false"
        currentLocale = if (settingsRepository.getSetting("locale") == "fr") Locale.FRENCH else Locale.ENGLISH
        isLoading = false
    }
    
    fun saveSetting(key: String, value: String) {
        scope.launch {
            settingsRepository.setSetting(key, value)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // AI Configuration Section
                SettingsSection(title = "AI Configuration") {
                    // Gemini API Key
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "Gemini API Key",
                        subtitle = if (geminiApiKey.isNotEmpty()) "••••••••${geminiApiKey.takeLast(4)}" else "Not configured",
                        onClick = { showApiKeys = true }
                    )
                    
                    // OpenAI API Key
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "OpenAI API Key (Backup)",
                        subtitle = if (openAiApiKey.isNotEmpty()) "••••••••${openAiApiKey.takeLast(4)}" else "Not configured",
                        onClick = { showApiKeys = true }
                    )
                }
                
                HorizontalDivider()
                
                // Language Section
                SettingsSection(title = "Language") {
                    SettingsItemSwitch(
                        icon = Icons.Default.Language,
                        title = "French / Français",
                        subtitle = if (currentLocale == Locale.FRENCH) "Activé" else "Enable French language",
                        checked = currentLocale == Locale.FRENCH,
                        onCheckedChange = { isFrench ->
                            currentLocale = if (isFrench) Locale.FRENCH else Locale.ENGLISH
                            LocaleManager.setLocale(currentLocale)
                            saveSetting("locale", if (isFrench) "fr" else "en")
                        }
                    )
                }
                
                HorizontalDivider()
                
                // Notifications Section
                SettingsSection(title = "Notifications") {
                    SettingsItemSwitch(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        subtitle = "Receive push notifications",
                        checked = pushNotifications,
                        onCheckedChange = { 
                            pushNotifications = it
                            saveSetting("push_notifications", it.toString())
                        }
                    )
                    
                    SettingsItemSwitch(
                        icon = Icons.Default.Email,
                        title = "Email Notifications",
                        subtitle = "Receive email updates",
                        checked = emailNotifications,
                        onCheckedChange = { 
                            emailNotifications = it
                            saveSetting("email_notifications", it.toString())
                        }
                    )
                    
                    SettingsItemSwitch(
                        icon = Icons.Default.Event,
                        title = "Interview Reminders",
                        subtitle = "Get reminded before interviews",
                        checked = interviewReminders,
                        onCheckedChange = { 
                            interviewReminders = it
                            saveSetting("interview_reminders", it.toString())
                        }
                    )
                    
                    SettingsItemSwitch(
                        icon = Icons.Default.CalendarMonth,
                        title = "Weekly Digest",
                        subtitle = "Receive weekly job search summary",
                        checked = weeklyDigest,
                        onCheckedChange = { 
                            weeklyDigest = it
                            saveSetting("weekly_digest", it.toString())
                        }
                    )
                }
                
                HorizontalDivider()
                
                // Appearance Section
                SettingsSection(title = "Appearance") {
                    SettingsItemSwitch(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Use dark theme",
                        checked = darkMode,
                        onCheckedChange = { 
                            darkMode = it
                            saveSetting("dark_mode", it.toString())
                        }
                    )
                }
                
                HorizontalDivider()
                
                // Privacy Section
                SettingsSection(title = "Privacy & Data") {
                    SettingsItemSwitch(
                        icon = Icons.Default.Analytics,
                        title = "Analytics",
                        subtitle = "Help improve the app with anonymous usage data",
                        checked = analyticsEnabled,
                        onCheckedChange = { 
                            analyticsEnabled = it
                            saveSetting("analytics_enabled", it.toString())
                        }
                    )
                    
                    SettingsItemSwitch(
                        icon = Icons.Default.BugReport,
                        title = "Crash Reporting",
                        subtitle = "Automatically report crashes",
                        checked = crashReportingEnabled,
                        onCheckedChange = { 
                            crashReportingEnabled = it
                            saveSetting("crash_reporting_enabled", it.toString())
                        }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "Export My Data",
                        subtitle = "Download all your data (PIPEDA compliant)",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Data export requested. You'll receive an email shortly.")
                            }
                        }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete My Data",
                        subtitle = "Permanently delete all your data",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("This action cannot be undone. Please confirm via email.")
                            }
                        },
                        isDestructive = true
                    )
                }
                
                HorizontalDivider()
                
                // About Section
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "1.0.0 (Build 1)",
                        onClick = { }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        onClick = { }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Gavel,
                        title = "Terms of Service",
                        subtitle = "View terms and conditions",
                        onClick = { }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = "Help & Support",
                        subtitle = "Get help or report issues",
                        onClick = { }
                    )
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // API Keys Dialog
    if (showApiKeys) {
        AlertDialog(
            onDismissRequest = { showApiKeys = false },
            title = { Text("AI API Keys") },
            text = {
                Column {
                    Text(
                        text = "Enter your API keys to enable AI features. Gemini is the primary engine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { geminiApiKey = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = openAiApiKey,
                        onValueChange = { openAiApiKey = it },
                        label = { Text("OpenAI API Key (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveSetting("gemini_api_key", geminiApiKey)
                        saveSetting("openai_api_key", openAiApiKey)
                        showApiKeys = false
                        scope.launch {
                            snackbarHostState.showSnackbar("API keys saved")
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeys = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsItemSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
