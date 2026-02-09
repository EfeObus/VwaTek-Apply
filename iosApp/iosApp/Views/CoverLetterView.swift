import SwiftUI
import shared

struct CoverLetterView: View {
    @StateObject private var viewModel = CoverLetterViewModelWrapper()
    @StateObject private var resumeViewModel = ResumeViewModelWrapper()
    @State private var companyName = ""
    @State private var positionTitle = ""
    @State private var jobDescription = ""
    @State private var additionalNotes = ""
    @State private var showError = false
    @State private var selectedResume: Resume? = nil
    @State private var showResumePicker = false
    @State private var selectedTone: CoverLetterTone = .professional
    @State private var showCoverLetterDetail = false
    @State private var selectedSavedLetter: CoverLetter? = nil
    
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
                    
                    // Resume Selection
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Select Resume")
                            .font(.headline)
                        
                        Button(action: { showResumePicker = true }) {
                            HStack {
                                Image(systemName: selectedResume != nil ? "doc.text.fill" : "doc.badge.plus")
                                    .foregroundColor(selectedResume != nil ? .blue : .secondary)
                                
                                Text(selectedResume?.name ?? "Choose a resume...")
                                    .foregroundColor(selectedResume != nil ? .primary : .secondary)
                                
                                Spacer()
                                
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(10)
                        }
                    }
                    
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
                    
                    // Tone Selection
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Tone")
                            .font(.headline)
                        
                        Picker("Tone", selection: $selectedTone) {
                            Text("Professional").tag(CoverLetterTone.professional)
                            Text("Enthusiastic").tag(CoverLetterTone.enthusiastic)
                            Text("Formal").tag(CoverLetterTone.formal)
                            Text("Creative").tag(CoverLetterTone.creative)
                        }
                        .pickerStyle(.segmented)
                    }
                    
                    // Resume selection status
                    if selectedResume == nil {
                        HStack(spacing: 12) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            
                            Text("Select a resume above to generate personalized cover letters")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(10)
                    } else {
                        HStack(spacing: 12) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            
                            Text("Using: \(selectedResume!.name)")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(10)
                    }
                    
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
                    
                    // Saved Cover Letters Section
                    if !viewModel.coverLetters.isEmpty {
                        Divider()
                            .padding(.vertical)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Saved Cover Letters")
                                .font(.headline)
                            
                            ForEach(viewModel.coverLetters, id: \.id) { letter in
                                CoverLetterRow(
                                    coverLetter: letter,
                                    onTap: {
                                        selectedSavedLetter = letter
                                        showCoverLetterDetail = true
                                    },
                                    onDelete: {
                                        viewModel.deleteCoverLetter(id: letter.id)
                                    }
                                )
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
            .sheet(isPresented: $showResumePicker) {
                ResumePickerSheet(
                    resumes: resumeViewModel.resumes,
                    selectedResume: $selectedResume,
                    isLoading: resumeViewModel.isLoading
                )
            }
            .sheet(isPresented: $showCoverLetterDetail) {
                if let letter = selectedSavedLetter {
                    CoverLetterDetailSheet(coverLetter: letter)
                }
            }
            .onAppear {
                resumeViewModel.loadResumes()
                viewModel.loadCoverLetters()
            }
        }
    }
    
    private var canGenerate: Bool {
        !companyName.isEmpty && !positionTitle.isEmpty && !jobDescription.isEmpty && selectedResume != nil
    }
    
    private func generateCoverLetter() {
        let resumeContent = selectedResume?.content ?? additionalNotes
        
        viewModel.generateCoverLetter(
            resumeContent: resumeContent,
            jobTitle: positionTitle,
            companyName: companyName,
            jobDescription: jobDescription,
            tone: selectedTone
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

// MARK: - Resume Picker Sheet
struct ResumePickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    let resumes: [Resume]
    @Binding var selectedResume: Resume?
    let isLoading: Bool
    
    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("Loading resumes...")
                } else if resumes.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "doc.text")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        
                        Text("No Resumes Yet")
                            .font(.headline)
                        
                        Text("Create a resume first in the Resume tab")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List(resumes, id: \.id) { resume in
                        Button(action: {
                            selectedResume = resume
                            dismiss()
                        }) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(resume.name)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    
                                    if let industry = resume.industry {
                                        Text(industry)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                
                                Spacer()
                                
                                if selectedResume?.id == resume.id {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Select Resume")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Cover Letter Row
struct CoverLetterRow: View {
    let coverLetter: CoverLetter
    let onTap: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // Icon
                Image(systemName: "envelope.fill")
                    .font(.title2)
                    .foregroundColor(.blue)
                    .frame(width: 44, height: 44)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(10)
                
                // Content
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(coverLetter.jobTitle) at \(coverLetter.companyName)")
                        .font(.headline)
                        .foregroundColor(.primary)
                        .lineLimit(1)
                    
                    HStack(spacing: 8) {
                        Text(toneDisplayName(coverLetter.tone))
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.secondary.opacity(0.2))
                            .cornerRadius(4)
                        
                        Text(formatDate(coverLetter.createdAt))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive, action: onDelete) {
                Label("Delete", systemImage: "trash")
            }
        }
        .contextMenu {
            Button(role: .destructive, action: onDelete) {
                Label("Delete", systemImage: "trash")
            }
        }
    }
    
    private func toneDisplayName(_ tone: CoverLetterTone) -> String {
        switch tone {
        case .professional: return "Professional"
        case .enthusiastic: return "Enthusiastic"
        case .formal: return "Formal"
        case .creative: return "Creative"
        default: return "Professional"
        }
    }
    
    private func formatDate(_ date: Kotlinx_datetimeInstant) -> String {
        let epochSeconds = date.epochSeconds
        let swiftDate = Date(timeIntervalSince1970: TimeInterval(epochSeconds))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: swiftDate)
    }
}

// MARK: - Cover Letter Detail Sheet
struct CoverLetterDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let coverLetter: CoverLetter
    @State private var showCopiedToast = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(coverLetter.jobTitle) at \(coverLetter.companyName)")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        HStack(spacing: 12) {
                            Text(toneDisplayName(coverLetter.tone))
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.blue.opacity(0.2))
                                .cornerRadius(6)
                            
                            Text("Created \(formatDate(coverLetter.createdAt))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Divider()
                    
                    // Content
                    Text(coverLetter.content)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                        .textSelection(.enabled)
                    
                    // Actions
                    HStack(spacing: 12) {
                        Button(action: copyToClipboard) {
                            Label("Copy", systemImage: "doc.on.doc")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(.secondarySystemBackground))
                                .cornerRadius(10)
                        }
                        
                        ShareLink(item: coverLetter.content) {
                            Label("Share", systemImage: "square.and.arrow.up")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Cover Letter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .overlay(alignment: .bottom) {
                if showCopiedToast {
                    Text("Copied to clipboard!")
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                        .shadow(radius: 4)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .padding(.bottom, 20)
                }
            }
        }
    }
    
    private func copyToClipboard() {
        UIPasteboard.general.string = coverLetter.content
        withAnimation {
            showCopiedToast = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation {
                showCopiedToast = false
            }
        }
    }
    
    private func toneDisplayName(_ tone: CoverLetterTone) -> String {
        switch tone {
        case .professional: return "Professional"
        case .enthusiastic: return "Enthusiastic"
        case .formal: return "Formal"
        case .creative: return "Creative"
        default: return "Professional"
        }
    }
    
    private func formatDate(_ date: Kotlinx_datetimeInstant) -> String {
        let epochSeconds = date.epochSeconds
        let swiftDate = Date(timeIntervalSince1970: TimeInterval(epochSeconds))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: swiftDate)
    }
}

#Preview {
    // Note: Preview won't work without Koin initialization
    // CoverLetterView()
    Text("CoverLetter Preview - Run on device/simulator")
}
