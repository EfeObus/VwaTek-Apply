import SwiftUI
import shared

/// Settings View for iOS
/// Manages app preferences, API keys, and user settings
struct SettingsView: View {
    @State private var geminiApiKey: String = ""
    @State private var openAiApiKey: String = ""
    @State private var showApiKeySheet = false
    
    // Notification preferences
    @State private var pushNotifications = true
    @State private var emailNotifications = true
    @State private var interviewReminders = true
    @State private var weeklyDigest = false
    
    // Appearance
    @State private var darkMode = false
    @State private var useFrench = false
    
    // Privacy
    @State private var analyticsEnabled = true
    @State private var crashReportingEnabled = true
    
    @State private var showExportAlert = false
    @State private var showDeleteAlert = false
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
            Group {
                if isLoading {
                    ProgressView()
                } else {
                    Form {
                        // AI Configuration
                        Section("AI Configuration") {
                            Button {
                                showApiKeySheet = true
                            } label: {
                                HStack {
                                    Label("Gemini API Key", systemImage: "key")
                                    Spacer()
                                    Text(geminiApiKey.isEmpty ? "Not set" : "••••\(geminiApiKey.suffix(4))")
                                        .foregroundColor(.secondary)
                                }
                            }
                            .foregroundColor(.primary)
                            
                            Button {
                                showApiKeySheet = true
                            } label: {
                                HStack {
                                    Label("OpenAI API Key", systemImage: "key")
                                    Spacer()
                                    Text(openAiApiKey.isEmpty ? "Not set" : "••••\(openAiApiKey.suffix(4))")
                                        .foregroundColor(.secondary)
                                }
                            }
                            .foregroundColor(.primary)
                        }
                        
                        // Language
                        Section("Language") {
                            Toggle(isOn: $useFrench) {
                                Label("French / Français", systemImage: "globe")
                            }
                            .onChange(of: useFrench) { newValue in
                                saveSetting(key: "locale", value: newValue ? "fr" : "en")
                            }
                        }
                        
                        // Notifications
                        Section("Notifications") {
                            Toggle(isOn: $pushNotifications) {
                                Label("Push Notifications", systemImage: "bell")
                            }
                            .onChange(of: pushNotifications) { newValue in
                                saveSetting(key: "push_notifications", value: String(newValue))
                            }
                            
                            Toggle(isOn: $emailNotifications) {
                                Label("Email Notifications", systemImage: "envelope")
                            }
                            .onChange(of: emailNotifications) { newValue in
                                saveSetting(key: "email_notifications", value: String(newValue))
                            }
                            
                            Toggle(isOn: $interviewReminders) {
                                Label("Interview Reminders", systemImage: "calendar.badge.clock")
                            }
                            .onChange(of: interviewReminders) { newValue in
                                saveSetting(key: "interview_reminders", value: String(newValue))
                            }
                            
                            Toggle(isOn: $weeklyDigest) {
                                Label("Weekly Digest", systemImage: "calendar")
                            }
                            .onChange(of: weeklyDigest) { newValue in
                                saveSetting(key: "weekly_digest", value: String(newValue))
                            }
                        }
                        
                        // Appearance
                        Section("Appearance") {
                            Toggle(isOn: $darkMode) {
                                Label("Dark Mode", systemImage: "moon")
                            }
                            .onChange(of: darkMode) { newValue in
                                saveSetting(key: "dark_mode", value: String(newValue))
                            }
                        }
                        
                        // Privacy
                        Section("Privacy & Data") {
                            Toggle(isOn: $analyticsEnabled) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Label("Analytics", systemImage: "chart.bar")
                                    Text("Help improve the app with anonymous data")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .onChange(of: analyticsEnabled) { newValue in
                                saveSetting(key: "analytics_enabled", value: String(newValue))
                            }
                            
                            Toggle(isOn: $crashReportingEnabled) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Label("Crash Reporting", systemImage: "ladybug")
                                    Text("Automatically report crashes")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .onChange(of: crashReportingEnabled) { newValue in
                                saveSetting(key: "crash_reporting_enabled", value: String(newValue))
                            }
                            
                            Button {
                                showExportAlert = true
                            } label: {
                                HStack {
                                    Label("Export My Data", systemImage: "arrow.down.doc")
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .foregroundColor(.primary)
                            
                            Button {
                                showDeleteAlert = true
                            } label: {
                                Label("Delete My Data", systemImage: "trash")
                            }
                            .foregroundColor(.red)
                        }
                        
                        // About
                        Section("About") {
                            HStack {
                                Label("App Version", systemImage: "info.circle")
                                Spacer()
                                Text("1.0.0 (Build 1)")
                                    .foregroundColor(.secondary)
                            }
                            
                            Link(destination: URL(string: "https://vwatek.com/privacy")!) {
                                HStack {
                                    Label("Privacy Policy", systemImage: "doc.text")
                                    Spacer()
                                    Image(systemName: "arrow.up.right")
                                        .font(.caption)
                                }
                            }
                            .foregroundColor(.primary)
                            
                            Link(destination: URL(string: "https://vwatek.com/terms")!) {
                                HStack {
                                    Label("Terms of Service", systemImage: "doc.plaintext")
                                    Spacer()
                                    Image(systemName: "arrow.up.right")
                                        .font(.caption)
                                }
                            }
                            .foregroundColor(.primary)
                            
                            Link(destination: URL(string: "mailto:support@vwatek.com")!) {
                                HStack {
                                    Label("Help & Support", systemImage: "questionmark.circle")
                                    Spacer()
                                    Image(systemName: "arrow.up.right")
                                        .font(.caption)
                                }
                            }
                            .foregroundColor(.primary)
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .onAppear {
                loadSettings()
            }
            .sheet(isPresented: $showApiKeySheet) {
                apiKeySheet
            }
            .alert("Export Data", isPresented: $showExportAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Export") {
                    // Trigger data export
                }
            } message: {
                Text("We'll prepare your data and send it to your email. This is PIPEDA compliant.")
            }
            .alert("Delete All Data", isPresented: $showDeleteAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Delete", role: .destructive) {
                    // Trigger data deletion
                }
            } message: {
                Text("This action cannot be undone. All your data will be permanently deleted from our servers.")
            }
        }
    }
    
    // MARK: - API Key Sheet
    
    private var apiKeySheet: some View {
        NavigationView {
            Form {
                Section {
                    Text("Enter your API keys to enable AI features. Gemini is the primary engine, OpenAI is optional backup.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Section("Gemini API Key (Primary)") {
                    SecureField("Enter Gemini API Key", text: $geminiApiKey)
                        .textContentType(.password)
                }
                
                Section("OpenAI API Key (Optional)") {
                    SecureField("Enter OpenAI API Key", text: $openAiApiKey)
                        .textContentType(.password)
                }
            }
            .navigationTitle("API Keys")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        showApiKeySheet = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveSetting(key: "gemini_api_key", value: geminiApiKey)
                        saveSetting(key: "openai_api_key", value: openAiApiKey)
                        showApiKeySheet = false
                    }
                }
            }
        }
    }
    
    // MARK: - Settings Persistence
    
    private func loadSettings() {
        geminiApiKey = SettingsHelper.shared.getSetting(key: "gemini_api_key") ?? ""
        openAiApiKey = SettingsHelper.shared.getSetting(key: "openai_api_key") ?? ""
        pushNotifications = SettingsHelper.shared.getSetting(key: "push_notifications") != "false"
        emailNotifications = SettingsHelper.shared.getSetting(key: "email_notifications") != "false"
        interviewReminders = SettingsHelper.shared.getSetting(key: "interview_reminders") != "false"
        weeklyDigest = SettingsHelper.shared.getSetting(key: "weekly_digest") == "true"
        darkMode = SettingsHelper.shared.getSetting(key: "dark_mode") == "true"
        useFrench = SettingsHelper.shared.getSetting(key: "locale") == "fr"
        analyticsEnabled = SettingsHelper.shared.getSetting(key: "analytics_enabled") != "false"
        crashReportingEnabled = SettingsHelper.shared.getSetting(key: "crash_reporting_enabled") != "false"
        isLoading = false
    }
    
    private func saveSetting(key: String, value: String) {
        SettingsHelper.shared.setSetting(key: key, value: value)
    }
}

#Preview {
    SettingsView()
}
