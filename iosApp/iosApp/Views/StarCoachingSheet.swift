import SwiftUI
import shared

struct StarCoachingSheet: View {
    @ObservedObject var viewModel: InterviewViewModelWrapper
    @Binding var isPresented: Bool
    
    @State private var experience: String = ""
    @State private var jobContext: String = ""
    @State private var showCopiedAlert = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    if viewModel.starResponse == nil {
                        // Input Section
                        inputSection
                    } else {
                        // Result Section
                        resultSection
                    }
                }
                .padding()
            }
            .navigationTitle("STAR Coaching")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") {
                        viewModel.clearStarResponse()
                        isPresented = false
                    }
                }
                
                if viewModel.starResponse != nil {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Menu {
                            Button(action: copyToClipboard) {
                                Label("Copy to Clipboard", systemImage: "doc.on.doc")
                            }
                            
                            ShareLink(item: buildStarContent()) {
                                Label("Share", systemImage: "square.and.arrow.up")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                    }
                }
            }
            .alert("Copied!", isPresented: $showCopiedAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("STAR response copied to clipboard")
            }
        }
    }
    
    private var inputSection: some View {
        VStack(spacing: 20) {
            // Info Card
            VStack(alignment: .leading, spacing: 12) {
                Label("How it works", systemImage: "lightbulb.fill")
                    .font(.headline)
                    .foregroundColor(.orange)
                
                Text("Describe your experience and get AI-powered coaching to structure your answer using the STAR method.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.orange.opacity(0.1))
            .cornerRadius(12)
            
            // Experience Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Your Experience")
                    .font(.headline)
                
                TextEditor(text: $experience)
                    .frame(minHeight: 120)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(.systemGray4), lineWidth: 1)
                    )
                
                Text("Describe an accomplishment, challenge, or project you worked on")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Job Context Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Job Context")
                    .font(.headline)
                
                TextField("e.g., Software Engineer at a tech startup", text: $jobContext)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                
                Text("The role or industry context for the response")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Generate Button
            Button(action: {
                viewModel.getStarCoaching(experience: experience, jobContext: jobContext)
            }) {
                HStack {
                    if viewModel.isGettingStarCoaching {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        Text("Generating...")
                    } else {
                        Image(systemName: "star.fill")
                        Text("Generate STAR Response")
                    }
                }
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding()
                .background(canGenerate ? Color.orange : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
            .disabled(!canGenerate || viewModel.isGettingStarCoaching)
        }
    }
    
    private var resultSection: some View {
        VStack(spacing: 16) {
            // Header with actions
            HStack {
                Text("Your STAR Response")
                    .font(.headline)
                
                Spacer()
                
                Button(action: resetForm) {
                    Label("New", systemImage: "arrow.counterclockwise")
                        .font(.caption)
                }
            }
            
            if let starResponse = viewModel.starResponse {
                // STAR Cards
                StarResponseCard(
                    letter: "S",
                    title: "Situation",
                    content: starResponse.situation,
                    color: .green
                )
                
                StarResponseCard(
                    letter: "T",
                    title: "Task",
                    content: starResponse.task,
                    color: .blue
                )
                
                StarResponseCard(
                    letter: "A",
                    title: "Action",
                    content: starResponse.action,
                    color: .orange
                )
                
                StarResponseCard(
                    letter: "R",
                    title: "Result",
                    content: starResponse.result,
                    color: .purple
                )
                
                // Suggestions
                if !starResponse.suggestions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Label("Improvement Suggestions", systemImage: "lightbulb.fill")
                            .font(.headline)
                            .foregroundColor(.yellow)
                        
                        ForEach(Array(starResponse.suggestions.enumerated()), id: \.offset) { _, suggestion in
                            HStack(alignment: .top, spacing: 8) {
                                Image(systemName: "star.fill")
                                    .font(.caption)
                                    .foregroundColor(.yellow)
                                
                                Text(suggestion)
                                    .font(.subheadline)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color.yellow.opacity(0.1))
                    .cornerRadius(12)
                }
            }
        }
    }
    
    private var canGenerate: Bool {
        !experience.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !jobContext.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    private func resetForm() {
        viewModel.clearStarResponse()
        experience = ""
        jobContext = ""
    }
    
    private func copyToClipboard() {
        UIPasteboard.general.string = buildStarContent()
        showCopiedAlert = true
    }
    
    private func buildStarContent() -> String {
        guard let starResponse = viewModel.starResponse else { return "" }
        
        var content = """
        SITUATION:
        \(starResponse.situation)
        
        TASK:
        \(starResponse.task)
        
        ACTION:
        \(starResponse.action)
        
        RESULT:
        \(starResponse.result)
        """
        
        if !starResponse.suggestions.isEmpty {
            content += "\n\nSUGGESTIONS:\n"
            for suggestion in starResponse.suggestions {
                content += "• \(suggestion)\n"
            }
        }
        
        return content
    }
}

struct StarResponseCard: View {
    let letter: String
    let title: String
    let content: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(letter)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(width: 28, height: 28)
                    .background(color)
                    .cornerRadius(6)
                
                Text(title)
                    .font(.headline)
                    .foregroundColor(color)
            }
            
            Text(content)
                .font(.subheadline)
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(color.opacity(0.1))
        .cornerRadius(12)
    }
}

struct StarMethodItem: View {
    let letter: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 12) {
            Text(letter)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .background(Color.orange)
                .cornerRadius(4)
            
            VStack(alignment: .leading) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

#Preview {
    StarCoachingSheet(
        viewModel: InterviewViewModelWrapper(),
        isPresented: .constant(true)
    )
}
