import SwiftUI
import shared

struct ProfileView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @State private var showLogoutAlert = false
    @State private var showEditSheet = false
    @State private var showApiSettingsSheet = false
    @State private var showHelpSheet = false
    @State private var showFeedbackSheet = false
    @State private var showAboutSheet = false
    
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
                            ) { showHelpSheet = true }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "bubble.left.fill",
                                title: "Send Feedback",
                                subtitle: "Help us improve the app"
                            ) { showFeedbackSheet = true }
                            
                            Divider().padding(.leading, 56)
                            
                            ProfileListItem(
                                icon: "info.circle.fill",
                                title: "About",
                                subtitle: "Version 1.0.0"
                            ) { showAboutSheet = true }
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
            .sheet(isPresented: $showHelpSheet) {
                HelpCenterSheet(onDismiss: { showHelpSheet = false })
            }
            .sheet(isPresented: $showFeedbackSheet) {
                FeedbackSheet(onDismiss: { showFeedbackSheet = false })
            }
            .sheet(isPresented: $showAboutSheet) {
                AboutSheet(onDismiss: { showAboutSheet = false })
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

// MARK: - Help Center Sheet
struct HelpCenterSheet: View {
    let onDismiss: () -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "questionmark.circle.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.blue)
                        
                        Text("Help Center")
                            .font(.title)
                            .fontWeight(.bold)
                        
                        Text("Find answers to common questions")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical)
                    
                    // FAQ Section
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Frequently Asked Questions")
                            .font(.headline)
                        
                        FAQItem(
                            question: "How do I create a resume?",
                            answer: "Tap the + button on the Resume tab to create a new resume or upload an existing one. You can also import from LinkedIn."
                        )
                        
                        FAQItem(
                            question: "How do I generate a cover letter?",
                            answer: "Go to the Cover Letters tab and tap Generate. Enter the job details and our AI will create a tailored cover letter for you."
                        )
                        
                        FAQItem(
                            question: "How do I practice interviews?",
                            answer: "Visit the Interview tab to start AI-powered mock interviews. Select your industry and question type to begin practicing."
                        )
                        
                        FAQItem(
                            question: "How does the resume optimizer work?",
                            answer: "The optimizer analyzes your resume against job descriptions and provides an ATS compatibility score with specific improvement suggestions."
                        )
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    
                    // Contact Section
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Need More Help?")
                            .font(.headline)
                        
                        Button(action: {
                            if let url = URL(string: "https://vwatekapply.com/help") {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            HStack {
                                Image(systemName: "globe")
                                Text("Visit Help Website")
                                Spacer()
                                Image(systemName: "chevron.right")
                            }
                            .padding()
                            .background(Color.blue.opacity(0.1))
                            .foregroundColor(.blue)
                            .cornerRadius(10)
                        }
                        
                        Button(action: {
                            if let url = URL(string: "mailto:support@vwatekapply.com") {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            HStack {
                                Image(systemName: "envelope")
                                Text("Email Support")
                                Spacer()
                                Image(systemName: "chevron.right")
                            }
                            .padding()
                            .background(Color.green.opacity(0.1))
                            .foregroundColor(.green)
                            .cornerRadius(10)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                }
                .padding()
            }
            .navigationTitle("Help Center")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct FAQItem: View {
    let question: String
    let answer: String
    @State private var isExpanded = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(action: { withAnimation { isExpanded.toggle() } }) {
                HStack {
                    Text(question)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
            }
            
            if isExpanded {
                Text(answer)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 4)
            }
            
            Divider()
        }
    }
}

// MARK: - Feedback Sheet
struct FeedbackSheet: View {
    let onDismiss: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var feedback = ""
    @State private var rating = 0
    @State private var feedbackType = "Bug Report"
    
    let feedbackTypes = ["Bug Report", "Feature Request", "General Feedback", "Other"]
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(spacing: 8) {
                        Image(systemName: "bubble.left.and.bubble.right.fill")
                            .font(.system(size: 50))
                            .foregroundColor(.blue)
                        
                        Text("We'd love to hear from you!")
                            .font(.headline)
                        
                        Text("Your feedback helps us improve VwaTek Apply")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical)
                }
                
                Section("Rate Your Experience") {
                    HStack(spacing: 8) {
                        ForEach(1...5, id: \.self) { star in
                            Button(action: { rating = star }) {
                                Image(systemName: star <= rating ? "star.fill" : "star")
                                    .font(.title2)
                                    .foregroundColor(star <= rating ? .yellow : .gray)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 8)
                }
                
                Section("Feedback Type") {
                    Picker("Type", selection: $feedbackType) {
                        ForEach(feedbackTypes, id: \.self) { type in
                            Text(type).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                
                Section("Your Feedback") {
                    TextEditor(text: $feedback)
                        .frame(minHeight: 120)
                }
                
                Section {
                    Button(action: sendFeedback) {
                        HStack {
                            Image(systemName: "paperplane.fill")
                            Text("Send Feedback")
                        }
                        .frame(maxWidth: .infinity)
                        .foregroundColor(.white)
                    }
                    .listRowBackground(Color.blue)
                }
            }
            .navigationTitle("Send Feedback")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
    
    private func sendFeedback() {
        let subject = "VwaTek Apply Feedback - \(feedbackType)"
        let body = """
        Rating: \(rating)/5 stars
        Type: \(feedbackType)
        
        Feedback:
        \(feedback)
        
        ---
        App Version: 1.0.0
        iOS Version: \(UIDevice.current.systemVersion)
        Device: \(UIDevice.current.model)
        """
        
if let url = URL(string: "mailto:support@vwatekapply.com?subject=\(subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")&body=\(body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ")") {
            UIApplication.shared.open(url)
        }
        dismiss()
    }
}

// MARK: - About Sheet
struct AboutSheet: View {
    let onDismiss: () -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // App Logo and Name
                    VStack(spacing: 12) {
                        Image(systemName: "doc.text.fill")
                            .font(.system(size: 80))
                            .foregroundColor(.blue)
                        
                        Text("VwaTek Apply")
                            .font(.title)
                            .fontWeight(.bold)
                        
                        Text("Version 1.0.0 (Build 2026.02)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 20)
                    
                    // Description
                    Text("Your AI-powered job application companion. Create stunning resumes, generate tailored cover letters, and ace your interviews with AI assistance.")
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)
                    
                    Divider()
                    
                    // Company Info
                    VStack(spacing: 8) {
                        Text("Developed by VwaTek Inc.")
                            .font(.headline)
                        
                        Text("© 2026 VwaTek Inc. All rights reserved.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    // Links
                    VStack(spacing: 12) {
                        AboutLinkButton(
                            icon: "globe",
                            title: "Visit Website",
                            color: .blue
                        ) {
                            if let url = URL(string: "https://vwatekapply.com") {
                                UIApplication.shared.open(url)
                            }
                        }
                        
                        AboutLinkButton(
                            icon: "shield.fill",
                            title: "Privacy Policy",
                            color: .green
                        ) {
                            if let url = URL(string: "https://vwatekapply.com/privacy") {
                                UIApplication.shared.open(url)
                            }
                        }
                        
                        AboutLinkButton(
                            icon: "doc.text.fill",
                            title: "Terms of Service",
                            color: .orange
                        ) {
                            if let url = URL(string: "https://vwatekapply.com/terms") {
                                UIApplication.shared.open(url)
                            }
                        }
                        
                        AboutLinkButton(
                            icon: "star.fill",
                            title: "Rate on App Store",
                            color: .yellow
                        ) {
                            // App Store link would go here
                            if let url = URL(string: "https://apps.apple.com/app/vwatek-apply") {
                                UIApplication.shared.open(url)
                            }
                        }
                    }
                    .padding(.horizontal)
                    
                    Spacer()
                    
                    // Footer
                    Text("Made with ❤️ for job seekers everywhere")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.bottom)
                }
            }
            .navigationTitle("About")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct AboutLinkButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                    .frame(width: 24)
                
                Text(title)
                    .foregroundColor(.primary)
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
                    .font(.caption)
            }
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        }
    }
}

#Preview {
    ProfileView(viewModel: AuthViewModelWrapper())
}
