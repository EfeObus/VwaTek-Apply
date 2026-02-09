package com.vwatek.apply.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vwatek.apply.domain.model.User
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.presentation.auth.AuthIntent
import com.vwatek.apply.presentation.auth.AuthViewModel
import com.vwatek.apply.presentation.auth.AuthViewState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    authState: AuthViewState
) {
    val context = LocalContext.current
    val settingsRepository: SettingsRepository = koinInject()
    val scope = rememberCoroutineScope()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showApiSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val initials = "${authState.user?.firstName?.firstOrNull() ?: ""}${authState.user?.lastName?.firstOrNull() ?: ""}"
                        Text(
                            text = initials.uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${authState.user?.firstName ?: ""} ${authState.user?.lastName ?: ""}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = authState.user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                if (authState.user?.emailVerified == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Account section
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileListItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    onClick = { isEditing = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ProfileListItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your security credentials",
                    onClick = { showChangePasswordDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // AI Settings section
        Text(
            text = "AI Configuration",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileListItem(
                    icon = Icons.Default.Build,
                    title = "API Keys",
                    subtitle = "Configure Gemini and OpenAI API keys",
                    onClick = { showApiSettingsDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Preferences section
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileListItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage push and email notifications",
                    onClick = { showNotificationsDialog = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ProfileListItem(
                    icon = Icons.Default.Settings,
                    title = "Appearance",
                    subtitle = "Dark mode, theme settings",
                    onClick = { showAppearanceDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Support section
        Text(
            text = "Support",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileListItem(
                    icon = Icons.Default.Info,
                    title = "Help Center",
                    subtitle = "FAQs and support articles",
                    onClick = { showHelpDialog = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ProfileListItem(
                    icon = Icons.Default.Email,
                    title = "Send Feedback",
                    subtitle = "Help us improve the app",
                    onClick = { showFeedbackDialog = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ProfileListItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version 1.0.0 • VwaTek Inc.",
                    onClick = { showAboutDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Logout button
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "VwaTek Apply v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(AuthIntent.Logout)
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (isEditing) {
        EditProfileDialog(
            user = authState.user,
            onDismiss = { isEditing = false },
            onSave = { updatedUser ->
                viewModel.onIntent(AuthIntent.UpdateProfile(updatedUser))
                isEditing = false
            }
        )
    }
    
    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSave = { currentPassword, newPassword ->
                viewModel.onIntent(AuthIntent.ChangePassword(currentPassword, newPassword))
                showChangePasswordDialog = false
            }
        )
    }
    
    // Notifications Settings Dialog
    if (showNotificationsDialog) {
        NotificationsDialog(
            onDismiss = { showNotificationsDialog = false }
        )
    }
    
    // Appearance Settings Dialog
    if (showAppearanceDialog) {
        AppearanceDialog(
            onDismiss = { showAppearanceDialog = false }
        )
    }
    
    // Help Center Dialog
    if (showHelpDialog) {
        HelpCenterDialog(
            onDismiss = { showHelpDialog = false },
            onOpenWebsite = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vwatek.com/help"))
                context.startActivity(intent)
            }
        )
    }
    
    // Feedback Dialog
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSendEmail = { feedback ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@vwatek.com")
                    putExtra(Intent.EXTRA_SUBJECT, "VwaTek Apply Feedback")
                    putExtra(Intent.EXTRA_TEXT, feedback)
                }
                context.startActivity(Intent.createChooser(intent, "Send Feedback"))
                showFeedbackDialog = false
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onOpenWebsite = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vwatek.com"))
                context.startActivity(intent)
            },
            onOpenPrivacy = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vwatek.com/privacy"))
                context.startActivity(intent)
            },
            onOpenTerms = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vwatek.com/terms"))
                context.startActivity(intent)
            }
        )
    }
    
    // API Settings Dialog
    if (showApiSettingsDialog) {
        ApiSettingsDialog(
            settingsRepository = settingsRepository,
            scope = scope,
            onDismiss = { showApiSettingsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    user?.let { currentUser ->
                        onSave(
                            currentUser.copy(
                                firstName = firstName,
                                lastName = lastName,
                                phone = phone.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    
    val passwordsMatch = newPassword == confirmPassword
    val isValid = currentPassword.isNotBlank() && newPassword.length >= 8 && passwordsMatch
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPassword) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                if (showCurrentPassword) Icons.Default.Close else Icons.Default.Lock,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.Close else Icons.Default.Lock,
                                contentDescription = null
                            )
                        }
                    },
                    supportingText = {
                        if (newPassword.isNotEmpty() && newPassword.length < 8) {
                            Text("Password must be at least 8 characters", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                            Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentPassword, newPassword) },
                enabled = isValid
            ) {
                Text("Change Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NotificationsDialog(
    onDismiss: () -> Unit
) {
    var pushNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(true) }
    var applicationUpdates by remember { mutableStateOf(true) }
    var interviewReminders by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Settings") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Push Notifications") },
                    supportingContent = { Text("Receive push notifications on your device") },
                    trailingContent = {
                        Switch(
                            checked = pushNotifications,
                            onCheckedChange = { pushNotifications = it }
                        )
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Email Notifications") },
                    supportingContent = { Text("Receive updates via email") },
                    trailingContent = {
                        Switch(
                            checked = emailNotifications,
                            onCheckedChange = { emailNotifications = it }
                        )
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Notification Types",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Application Updates") },
                    trailingContent = {
                        Switch(
                            checked = applicationUpdates,
                            onCheckedChange = { applicationUpdates = it }
                        )
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Interview Reminders") },
                    trailingContent = {
                        Switch(
                            checked = interviewReminders,
                            onCheckedChange = { interviewReminders = it }
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AppearanceDialog(
    onDismiss: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf("system") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                listOf(
                    "system" to "System Default",
                    "light" to "Light",
                    "dark" to "Dark"
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == value,
                            onClick = { selectedTheme = value }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HelpCenterDialog(
    onDismiss: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Help Center") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Welcome to VwaTek Apply Help Center!")
                
                Text(
                    text = "Frequently Asked Questions:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Text("• How do I create a resume?\n   Tap the + button on the Resume tab to create or upload your resume.")
                Text("• How do I generate a cover letter?\n   Go to Cover Letters and tap Generate to create a tailored cover letter.")
                Text("• How do I practice interviews?\n   Visit the Interview tab to start AI-powered mock interviews.")
                
                Text(
                    text = "For more help, visit our website or contact support@vwatek.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onOpenWebsite()
                onDismiss()
            }) {
                Text("Visit Website")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSendEmail: (String) -> Unit
) {
    var feedback by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Email, contentDescription = null) },
        title = { Text("Send Feedback") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("We'd love to hear from you! Your feedback helps us improve the app.")
                
                Text(
                    text = "Rate your experience",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Rate $star stars",
                                tint = if (star <= rating) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Your Feedback") },
                    placeholder = { Text("Tell us what you think...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val fullFeedback = "Rating: $rating/5 stars\n\n$feedback"
                    onSendEmail(fullFeedback)
                },
                enabled = feedback.isNotBlank()
            ) {
                Text("Send Feedback")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiSettingsDialog(
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit
) {
    var geminiApiKey by remember { mutableStateOf("") }
    var openAiApiKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    
    // Load existing keys
    LaunchedEffect(Unit) {
        val existingGeminiKey = settingsRepository.getSetting("gemini_api_key")
        val existingOpenAiKey = settingsRepository.getSetting("openai_api_key")
        if (existingGeminiKey != null) geminiApiKey = existingGeminiKey
        if (existingOpenAiKey != null) openAiApiKey = existingOpenAiKey
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Build, contentDescription = null) },
        title = { Text("AI API Keys") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configure your AI API keys. The app uses centralized keys by default, but you can add your own for priority access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { geminiApiKey = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("Enter your Gemini API key (optional)") },
                        singleLine = true,
                        visualTransformation = if (showGeminiKey) 
                            androidx.compose.ui.text.input.VisualTransformation.None 
                        else 
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    if (showGeminiKey) Icons.Default.Close else Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = openAiApiKey,
                        onValueChange = { openAiApiKey = it },
                        label = { Text("OpenAI API Key (Fallback)") },
                        placeholder = { Text("Enter your OpenAI API key (optional)") },
                        singleLine = true,
                        visualTransformation = if (showOpenAiKey) 
                            androidx.compose.ui.text.input.VisualTransformation.None 
                        else 
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                                Icon(
                                    if (showOpenAiKey) Icons.Default.Close else Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "💡 Leave blank to use the app's centralized API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        if (geminiApiKey.isNotBlank()) {
                            settingsRepository.setSetting("gemini_api_key", geminiApiKey)
                        } else {
                            settingsRepository.deleteSetting("gemini_api_key")
                        }
                        if (openAiApiKey.isNotBlank()) {
                            settingsRepository.setSetting("openai_api_key", openAiApiKey)
                        } else {
                            settingsRepository.deleteSetting("openai_api_key")
                        }
                        isSaving = false
                        onDismiss()
                    }
                },
                enabled = !isLoading && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Info, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        title = { Text("About VwaTek Apply") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Logo and Version
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VwaTek Apply",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Version 1.0.0 (Build 2026.02)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider()
                
                Text(
                    text = "Your AI-powered job application companion. Create stunning resumes, generate tailored cover letters, and ace your interviews with AI assistance.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider()
                
                // Company Info
                Text(
                    text = "Developed by VwaTek Inc.",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "© 2026 VwaTek Inc. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Links
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenWebsite) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visit Website")
                    }
                    TextButton(onClick = onOpenPrivacy) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy")
                    }
                    TextButton(onClick = onOpenTerms) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Terms of Service")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
