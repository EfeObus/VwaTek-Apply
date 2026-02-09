import SwiftUI
import shared

struct ProfileView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @State private var showLogoutAlert = false
    @State private var showEditSheet = false
    @State private var showApiSettingsSheet = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Profile header
                    VStack(spacing: 12) {
                        // Avatar
                        ZStack {
                            Circle()
                                .fill(Color.blue)
                                .frame(width: 100, height: 100)
                            
                            Text(viewModel.initials)
                                .font(.largeTitle)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                        
                        VStack(spacing: 4) {
                            Text(viewModel.fullName)
                                .font(.title2)
                                .fontWeight(.bold)
                            
                            Text(viewModel.userEmail)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            HStack(spacing: 4) {
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundColor(.green)
                                    .font(.caption)
                                
                                Text("Verified")
                                    .font(.caption)
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(16)
                    
                    // Account section
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Account")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(spacing: 0) {
                            ProfileListItem(
                                icon: "person.fill",
                                title: "Edit Profile",
                                subtitle: "Update your personal information"
                            ) {
                                showEditSheet = true
                            }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "lock.fill",
                                title: "Change Password",
                                subtitle: "Update your security credentials"
                            ) { }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "link",
                                title: "Connected Accounts",
                                subtitle: "Manage Google and LinkedIn"
                            ) { }
                        }
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    
                    // Preferences section
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Preferences")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(spacing: 0) {
                            ProfileListItem(
                                icon: "bell.fill",
                                title: "Notifications",
                                subtitle: "Manage push and email notifications"
                            ) { }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "paintbrush.fill",
                                title: "Appearance",
                                subtitle: "Dark mode, theme settings"
                            ) { }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "globe",
                                title: "Language",
                                subtitle: "English (US)"
                            ) { }
                        }
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    
                    // AI Configuration section
                    VStack(alignment: .leading, spacing: 8) {
                        Text("AI Configuration")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(spacing: 0) {
                            ProfileListItem(
                                icon: "key.fill",
                                title: "API Keys",
                                subtitle: "Configure optional custom API keys"
                            ) {
                                showApiSettingsSheet = true
                            }
                        }
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    
                    // Support section
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Support")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        VStack(spacing: 0) {
                            ProfileListItem(
                                icon: "questionmark.circle.fill",
                                title: "Help Center",
                                subtitle: "FAQs and support articles"
                            ) { }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "bubble.left.fill",
                                title: "Send Feedback",
                                subtitle: "Help us improve the app"
                            ) { }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "info.circle.fill",
                                title: "About",
                                subtitle: "Version 1.0.0"
                            ) { }
                        }
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    
                    // Logout button
                    Button(action: { showLogoutAlert = true }) {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                            Text("Log Out")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .foregroundColor(.red)
                        .cornerRadius(12)
                    }
                    
                    // Version
                    Text("VwaTek Apply v1.0.0")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
            }
            .navigationTitle("Profile")
            .alert("Log Out", isPresented: $showLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Log Out", role: .destructive) {
                    viewModel.logout()
                }
            } message: {
                Text("Are you sure you want to log out?")
            }
            .sheet(isPresented: $showEditSheet) {
                EditProfileSheet(
                    viewModel: viewModel,
                    onDismiss: { showEditSheet = false }
                )
            }
            .sheet(isPresented: $showApiSettingsSheet) {
                ApiSettingsSheet()
            }
        }
    }
}

struct ProfileListItem: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .frame(width: 32)
                    .foregroundColor(.secondary)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.body)
                        .foregroundColor(.primary)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
        }
    }
}

struct EditProfileSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: AuthViewModelWrapper
    var onDismiss: () -> Void
    
    @State private var firstName: String = ""
    @State private var lastName: String = ""
    @State private var phone: String = ""
    @State private var isSaving: Bool = false
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("First Name", text: $firstName)
                    TextField("Last Name", text: $lastName)
                    TextField("Phone", text: $phone)
                        .keyboardType(.phonePad)
                }
                
                Section {
                    HStack {
                        Text("Email")
                        Spacer()
                        Text(viewModel.userEmail)
                            .foregroundColor(.secondary)
                    }
                } footer: {
                    Text("Email cannot be changed")
                }
            }
            .navigationTitle("Edit Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onDismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        isSaving = true
                        viewModel.updateProfile(
                            firstName: firstName,
                            lastName: lastName,
                            phone: phone.isEmpty ? nil : phone
                        )
                        // Give time for state to update
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            isSaving = false
                            onDismiss()
                        }
                    }
                    .disabled(firstName.isEmpty || isSaving)
                }
            }
            .onAppear {
                firstName = viewModel.userName
                lastName = viewModel.userLastName
                phone = viewModel.userPhone
            }
        }
    }
}

struct ApiSettingsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var geminiApiKey = ""
    @State private var openAiApiKey = ""
    @State private var isSaving = false
    @State private var showSuccessMessage = false
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack(spacing: 12) {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(.blue)
                        
                        Text("Leave blank to use the app's centralized API. Only enter your own keys if you want to use a personal account.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
                
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Gemini API Key")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        SecureField("Enter Gemini API key (optional)", text: $geminiApiKey)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("OpenAI API Key")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        SecureField("Enter OpenAI API key (optional)", text: $openAiApiKey)
                    }
                } header: {
                    Text("API Keys")
                } footer: {
                    Text("Get your API keys from Google AI Studio or OpenAI Platform")
                }
                
                if showSuccessMessage {
                    Section {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Settings saved successfully")
                                .foregroundColor(.green)
                        }
                    }
                }
            }
            .navigationTitle("API Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveSettings()
                    }
                    .disabled(isSaving)
                }
            }
            .onAppear {
                loadSettings()
            }
        }
    }
    
    private func loadSettings() {
        // Load settings from shared Koin SettingsRepository
        geminiApiKey = SettingsHelper.shared.getSetting(key: "gemini_api_key") ?? ""
        openAiApiKey = SettingsHelper.shared.getSetting(key: "openai_api_key") ?? ""
    }
    
    private func saveSettings() {
        isSaving = true
        
        // Save to shared Koin SettingsRepository
        if !geminiApiKey.isEmpty {
            SettingsHelper.shared.setSetting(key: "gemini_api_key", value: geminiApiKey)
        } else {
            SettingsHelper.shared.deleteSetting(key: "gemini_api_key")
        }
        
        if !openAiApiKey.isEmpty {
            SettingsHelper.shared.setSetting(key: "openai_api_key", value: openAiApiKey)
        } else {
            SettingsHelper.shared.deleteSetting(key: "openai_api_key")
        }
        
        // Show success message
        withAnimation {
            showSuccessMessage = true
        }
        
        isSaving = false
        
        // Auto-dismiss after a short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            dismiss()
        }
    }
}

#Preview {
    ProfileView(viewModel: AuthViewModelWrapper())
}
