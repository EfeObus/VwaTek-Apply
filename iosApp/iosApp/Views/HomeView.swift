import SwiftUI

struct HomeView: View {
    let userName: String
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Welcome Card
                    WelcomeCard(userName: userName)
                    
                    // Quick Actions
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Quick Actions")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible())
                        ], spacing: 12) {
                            QuickActionCard(
                                icon: "doc.text.fill",
                                title: "Create Resume",
                                description: "Build a professional resume",
                                color: .blue
                            )
                            
                            QuickActionCard(
                                icon: "envelope.fill",
                                title: "Cover Letter",
                                description: "AI-generated letters",
                                color: .green
                            )
                            
                            QuickActionCard(
                                icon: "mic.fill",
                                title: "Mock Interview",
                                description: "Practice with AI",
                                color: .orange
                            )
                            
                            QuickActionCard(
                                icon: "arrow.up.doc.fill",
                                title: "Upload Resume",
                                description: "Import existing resume",
                                color: .purple
                            )
                        }
                        .padding(.horizontal)
                    }
                    
                    // Getting Started
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Getting Started")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        GettingStartedCard(
                            stepNumber: 1,
                            title: "Create or Upload Your Resume",
                            description: "Start by creating a professional resume or uploading an existing one.",
                            isCompleted: false
                        )
                        
                        GettingStartedCard(
                            stepNumber: 2,
                            title: "Generate Cover Letters",
                            description: "Use AI to generate tailored cover letters for specific job postings.",
                            isCompleted: false
                        )
                        
                        GettingStartedCard(
                            stepNumber: 3,
                            title: "Practice Interviews",
                            description: "Prepare for interviews with AI-powered mock interview sessions.",
                            isCompleted: false
                        )
                    }
                    
                    // Pro Tip
                    ProTipCard()
                }
                .padding(.vertical)
            }
            .navigationTitle("Home")
        }
    }
}

struct WelcomeCard: View {
    let userName: String
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Welcome back,")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.9))
                
                Text(userName.isEmpty ? "User" : userName)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("Ready to land your dream job?")
                    .font(.footnote)
                    .foregroundColor(.white.opacity(0.8))
            }
            
            Spacer()
            
            Image(systemName: "briefcase.fill")
                .font(.system(size: 50))
                .foregroundColor(.white.opacity(0.3))
        }
        .padding(20)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [.blue, .blue.opacity(0.8)]),
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .cornerRadius(16)
        .padding(.horizontal)
    }
}

struct QuickActionCard: View {
    let icon: String
    let title: String
    let description: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 30))
                .foregroundColor(color)
            
            VStack(spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

struct GettingStartedCard: View {
    let stepNumber: Int
    let title: String
    let description: String
    let isCompleted: Bool
    
    var body: some View {
        HStack(spacing: 12) {
            // Step number
            ZStack {
                Circle()
                    .fill(isCompleted ? Color.green : Color.blue)
                    .frame(width: 36, height: 36)
                
                if isCompleted {
                    Image(systemName: "checkmark")
                        .foregroundColor(.white)
                        .fontWeight(.bold)
                } else {
                    Text("\(stepNumber)")
                        .foregroundColor(.white)
                        .fontWeight(.bold)
                }
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

struct ProTipCard: View {
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "lightbulb.fill")
                .foregroundColor(.orange)
            
            VStack(alignment: .leading, spacing: 4) {
                Text("Pro Tip")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.orange)
                
                Text("Tailor your resume for each job application. Our AI can help you optimize your resume for specific job descriptions.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color.orange.opacity(0.1))
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

#Preview {
    HomeView(userName: "John")
}
