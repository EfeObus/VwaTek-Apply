import SwiftUI
import shared

struct CoverLetterView: View {
    @StateObject private var viewModel = CoverLetterViewModelWrapper()
    @State private var companyName = ""
    @State private var positionTitle = ""
    @State private var jobDescription = ""
    @State private var additionalNotes = ""
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header card
                    HStack(spacing: 16) {
                        Image(systemName: "sparkles")
                            .font(.system(size: 35))
                            .foregroundColor(.blue)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("AI Cover Letter Generator")
                                .font(.headline)
                            
                            Text("Create tailored cover letters in seconds")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(12)
                    
                    // Input form
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Job Details")
                            .font(.headline)
                        
                        VStack(spacing: 12) {
                            // Company name
                            HStack {
                                Image(systemName: "building.2.fill")
                                    .foregroundColor(.secondary)
                                TextField("Company Name", text: $companyName)
                            }
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(10)
                            
                            // Position title
                            HStack {
                                Image(systemName: "briefcase.fill")
                                    .foregroundColor(.secondary)
                                TextField("Position Title", text: $positionTitle)
                            }
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(10)
                            
                            // Job description
                            VStack(alignment: .leading) {
                                Text("Job Description")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                TextEditor(text: $jobDescription)
                                    .frame(minHeight: 150)
                                    .padding(8)
                                    .background(Color(.secondarySystemBackground))
                                    .cornerRadius(10)
                            }
                            
                            // Additional notes
                            VStack(alignment: .leading) {
                                Text("Additional Notes (Optional)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                TextEditor(text: $additionalNotes)
                                    .frame(minHeight: 80)
                                    .padding(8)
                                    .background(Color(.secondarySystemBackground))
                                    .cornerRadius(10)
                            }
                        }
                    }
                    
                    // Resume info
                    HStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.orange)
                        
                        Text("Please select a resume first to generate personalized cover letters")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(10)
                    
                    // Generate button
                    Button(action: generateCoverLetter) {
                        HStack {
                            if viewModel.isGenerating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            }
                            
                            Image(systemName: "sparkles")
                            Text(viewModel.isGenerating ? "Generating..." : "Generate Cover Letter")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(canGenerate ? Color.blue : Color.gray)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .disabled(!canGenerate || viewModel.isGenerating)
                    
                    // Generated letter
                    if let coverLetter = viewModel.generatedCoverLetter {
                        Divider()
                            .padding(.vertical)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Generated Cover Letter")
                                    .font(.headline)
                                
                                Spacer()
                                
                                Button(action: { copyToClipboard(coverLetter.content) }) {
                                    Image(systemName: "doc.on.doc")
                                }
                                
                                ShareLink(item: coverLetter.content) {
                                    Image(systemName: "square.and.arrow.up")
                                }
                            }
                            
                            Text(coverLetter.content)
                                .padding()
                                .background(Color(.secondarySystemBackground))
                                .cornerRadius(10)
                                .textSelection(.enabled)
                            
                            HStack(spacing: 12) {
                                Button(action: { regenerate() }) {
                                    Label("Regenerate", systemImage: "arrow.clockwise")
                                        .frame(maxWidth: .infinity)
                                        .padding()
                                        .background(Color(.secondarySystemBackground))
                                        .cornerRadius(10)
                                }
                                
                                Button(action: saveLetter) {
                                    Label("Save", systemImage: "square.and.arrow.down")
                                        .frame(maxWidth: .infinity)
                                        .padding()
                                        .background(Color.blue)
                                        .foregroundColor(.white)
                                        .cornerRadius(10)
                                }
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Cover Letter")
            .alert("Error", isPresented: $showError) {
                Button("OK") {
                    viewModel.clearError()
                }
            } message: {
                Text(viewModel.error ?? "An unknown error occurred")
            }
            .onChange(of: viewModel.error) { error in
                showError = error != nil
            }
        }
    }
    
    private var canGenerate: Bool {
        !companyName.isEmpty && !positionTitle.isEmpty && !jobDescription.isEmpty
    }
    
    private func generateCoverLetter() {
        // Use a placeholder resume content if none selected
        // In a real app, you would get this from the selected resume
        let resumeContent = additionalNotes.isEmpty 
            ? "Experienced professional with relevant skills and background."
            : additionalNotes
        
        viewModel.generateCoverLetter(
            resumeContent: resumeContent,
            jobTitle: positionTitle,
            companyName: companyName,
            jobDescription: jobDescription,
            tone: .professional
        )
    }
    
    private func regenerate() {
        viewModel.clearGenerated()
        generateCoverLetter()
    }
    
    private func copyToClipboard(_ text: String) {
        UIPasteboard.general.string = text
    }
    
    private func saveLetter() {
        // Letter is automatically saved when generated
    }
}

#Preview {
    // Note: Preview won't work without Koin initialization
    // CoverLetterView()
    Text("CoverLetter Preview - Run on device/simulator")
}
