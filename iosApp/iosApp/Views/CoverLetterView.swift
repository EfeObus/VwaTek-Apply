import SwiftUI

struct CoverLetterView: View {
    @State private var companyName = ""
    @State private var positionTitle = ""
    @State private var jobDescription = ""
    @State private var additionalNotes = ""
    @State private var generatedLetter: String? = nil
    @State private var isGenerating = false
    
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
                            if isGenerating {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            }
                            
                            Image(systemName: "sparkles")
                            Text(isGenerating ? "Generating..." : "Generate Cover Letter")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(canGenerate ? Color.blue : Color.gray)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .disabled(!canGenerate || isGenerating)
                    
                    // Generated letter
                    if let letter = generatedLetter {
                        Divider()
                            .padding(.vertical)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Generated Cover Letter")
                                    .font(.headline)
                                
                                Spacer()
                                
                                Button(action: copyToClipboard) {
                                    Image(systemName: "doc.on.doc")
                                }
                                
                                ShareLink(item: letter) {
                                    Image(systemName: "square.and.arrow.up")
                                }
                            }
                            
                            Text(letter)
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
        }
    }
    
    private var canGenerate: Bool {
        !companyName.isEmpty && !positionTitle.isEmpty && !jobDescription.isEmpty
    }
    
    private func generateCoverLetter() {
        isGenerating = true
        
        // Simulate API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            generatedLetter = """
            Dear Hiring Manager,
            
            I am writing to express my strong interest in the \(positionTitle) position at \(companyName). With my background and skills, I am confident that I would be a valuable addition to your team.
            
            [This is a sample generated cover letter. In the actual app, this would be generated by AI based on your resume and the job description.]
            
            I am particularly excited about this opportunity because of \(companyName)'s reputation for innovation and excellence in the industry. I believe my experience aligns well with the requirements outlined in the job description.
            
            Thank you for considering my application. I look forward to the opportunity to discuss how my skills and experiences can contribute to \(companyName)'s continued success.
            
            Sincerely,
            [Your Name]
            """
            isGenerating = false
        }
    }
    
    private func regenerate() {
        generateCoverLetter()
    }
    
    private func copyToClipboard() {
        if let letter = generatedLetter {
            UIPasteboard.general.string = letter
        }
    }
    
    private func saveLetter() {
        // Save to local storage
    }
}

#Preview {
    CoverLetterView()
}
