import SwiftUI

struct ProfileView: View {
    @ObservedObject var viewModel: AuthViewModelWrapper
    @State private var showLogoutAlert = false
    @State private var showEditSheet = false
    
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
                            
                            Text(initials)
                                .font(.largeTitle)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                        
                        VStack(spacing: 4) {
                            Text(viewModel.userName)
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
                    firstName: viewModel.userName,
                    onSave: { _, _ in
                        showEditSheet = false
                    }
                )
            }
        }
    }
    
    private var initials: String {
        let components = viewModel.userName.split(separator: " ")
        let firstInitial = components.first?.first.map(String.init) ?? ""
        let lastInitial = components.count > 1 ? components.last?.first.map(String.init) ?? "" : ""
        return (firstInitial + lastInitial).uppercased()
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
    @State var firstName: String
    @State private var lastName = ""
    @State private var phone = ""
    
    var onSave: (String, String) -> Void
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("First Name", text: $firstName)
                    TextField("Last Name", text: $lastName)
                    TextField("Phone", text: $phone)
                        .keyboardType(.phonePad)
                }
            }
            .navigationTitle("Edit Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(firstName, lastName)
                    }
                    .disabled(firstName.isEmpty)
                }
            }
        }
    }
}

#Preview {
    ProfileView(viewModel: AuthViewModelWrapper())
}
