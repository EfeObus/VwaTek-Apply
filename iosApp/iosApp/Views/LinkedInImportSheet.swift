import SwiftUI

struct LinkedInImportSheet: View {
    let isLoading: Bool
    let onImport: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // LinkedIn Logo/Header
                VStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(red: 0.04, green: 0.40, blue: 0.76))
                            .frame(width: 80, height: 80)
                        
                        Text("in")
                            .font(.system(size: 48, weight: .bold))
                            .foregroundColor(.white)
                    }
                    
                    Text("Import from LinkedIn")
                        .font(.title2)
                        .fontWeight(.bold)
                }
                .padding(.top, 20)
                
                // Description
                Text("Import your LinkedIn profile to automatically create a professional resume with your:")
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 24)
                
                // Features list
                VStack(alignment: .leading, spacing: 12) {
                    FeatureRow(icon: "briefcase.fill", text: "Work experience")
                    FeatureRow(icon: "graduationcap.fill", text: "Education history")
                    FeatureRow(icon: "star.fill", text: "Skills and certifications")
                    FeatureRow(icon: "text.quote", text: "Professional summary")
                }
                .padding(.horizontal, 40)
                
                Spacer()
                
                // Privacy note
                Text("You'll be redirected to LinkedIn to authorize access to your profile. We only access your public profile information.")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                
                // Connect button
                Button(action: onImport) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        }
                        Text("Connect LinkedIn")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(red: 0.04, green: 0.40, blue: 0.76))
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(isLoading)
                .padding(.horizontal, 24)
                .padding(.bottom, 16)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onDismiss()
                    }
                    .disabled(isLoading)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

struct FeatureRow: View {
    let icon: String
    let text: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(Color(red: 0.04, green: 0.40, blue: 0.76))
                .frame(width: 24)
            
            Text(text)
                .font(.body)
        }
    }
}

#Preview {
    LinkedInImportSheet(
        isLoading: false,
        onImport: {},
        onDismiss: {}
    )
}
