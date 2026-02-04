import SwiftUI
import shared

struct ResumeView: View {
    @StateObject private var viewModel = ResumeViewModelWrapper()
    @State private var showCreateSheet = false
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            Group {
                if viewModel.resumes.isEmpty && !viewModel.isLoading {
                    EmptyResumeView(
                        onCreateNew: { showCreateSheet = true },
                        onUpload: { /* Handle upload */ }
                    )
                } else if viewModel.isLoading {
                    ProgressView("Loading resumes...")
                } else {
                    ResumeListView(viewModel: viewModel)
                }
            }
            .navigationTitle("Resume")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: { showCreateSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateResumeSheet(onSave: { name, industry in
                    viewModel.createResume(name: name, content: "", industry: industry)
                    showCreateSheet = false
                })
            }
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
}

struct ResumeListView: View {
    @ObservedObject var viewModel: ResumeViewModelWrapper
    
    var body: some View {
        List {
            ForEach(viewModel.resumes, id: \.id) { resume in
                ResumeRow(resume: resume, viewModel: viewModel)
            }
        }
        .listStyle(.insetGrouped)
    }
}

struct EmptyResumeView: View {
    var onCreateNew: () -> Void
    var onUpload: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            Image(systemName: "doc.text")
                .font(.system(size: 70))
                .foregroundColor(.secondary.opacity(0.5))
            
            VStack(spacing: 8) {
                Text("No Resumes Yet")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("Create your first professional resume or upload an existing one")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            VStack(spacing: 12) {
                Button(action: onCreateNew) {
                    Label("Create New Resume", systemImage: "plus")
                        .frame(maxWidth: 250)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                
                Button(action: onUpload) {
                    Label("Upload Existing", systemImage: "arrow.up.doc")
                        .frame(maxWidth: 250)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .foregroundColor(.primary)
                        .cornerRadius(10)
                }
            }
            
            Spacer()
        }
    }
}

struct ResumeRow: View {
    let resume: Resume
    @ObservedObject var viewModel: ResumeViewModelWrapper
    @State private var showOptimizeSheet = false
    
    var body: some View {
        HStack(spacing: 12) {
            // Icon
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.blue.opacity(0.1))
                    .frame(width: 44, height: 44)
                
                Image(systemName: "doc.text.fill")
                    .foregroundColor(.blue)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(resume.name)
                    .font(.headline)
                
                HStack {
                    if let industry = resume.industry {
                        Text(industry)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                    
                    Text("Updated \(formatDate(resume.updatedAt))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Menu {
                Button(action: { 
                    viewModel.selectResume(id: resume.id)
                }) {
                    Label("Edit", systemImage: "pencil")
                }
                
                Button(action: { 
                    showOptimizeSheet = true
                }) {
                    Label("Optimize with AI", systemImage: "sparkles")
                }
                
                Divider()
                
                Button(role: .destructive, action: { 
                    viewModel.deleteResume(id: resume.id)
                }) {
                    Label("Delete", systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
        .sheet(isPresented: $showOptimizeSheet) {
            ResumeOptimizerSheet(resume: resume, viewModel: viewModel)
        }
    }
    
    private func formatDate(_ instant: Kotlinx_datetimeInstant) -> String {
        // Convert Kotlin Instant to Date for display
        let date = Date(timeIntervalSince1970: Double(instant.epochSeconds))
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: date)
    }
}

struct ResumeOptimizerSheet: View {
    @Environment(\.dismiss) private var dismiss
    let resume: Resume
    @ObservedObject var viewModel: ResumeViewModelWrapper
    @State private var jobDescription = ""
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(resume.name)
                        .font(.headline)
                } header: {
                    Text("Resume")
                }
                
                Section {
                    TextEditor(text: $jobDescription)
                        .frame(minHeight: 150)
                } header: {
                    Text("Job Description")
                } footer: {
                    Text("Paste the job description to optimize your resume for this position")
                }
                
                if viewModel.isOptimizing {
                    Section {
                        HStack {
                            ProgressView()
                            Text("Optimizing your resume...")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                if let optimized = viewModel.optimizedContent {
                    Section {
                        Text(optimized)
                            .textSelection(.enabled)
                    } header: {
                        Text("Optimized Content")
                    }
                }
                
                if viewModel.isAnalyzing {
                    Section {
                        HStack {
                            ProgressView()
                            Text("Analyzing your resume...")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                if let analysis = viewModel.analysis {
                    Section {
                        HStack {
                            Text("Match Score")
                            Spacer()
                            Text("\(analysis.matchScore)%")
                                .fontWeight(.bold)
                                .foregroundColor(analysis.matchScore >= 70 ? .green : .orange)
                        }
                        
                        if !analysis.missingKeywords.isEmpty {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Missing Keywords:")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                Text(analysis.missingKeywords.joined(separator: ", "))
                                    .font(.caption)
                            }
                        }
                    } header: {
                        Text("Analysis Results")
                    }
                }
            }
            .navigationTitle("Optimize Resume")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        viewModel.clearOptimizedContent()
                        viewModel.clearAnalysis()
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Menu {
                        Button(action: {
                            viewModel.analyzeResume(resume, jobDescription: jobDescription)
                        }) {
                            Label("Analyze", systemImage: "magnifyingglass")
                        }
                        
                        Button(action: {
                            viewModel.optimizeResume(resumeContent: resume.content, jobDescription: jobDescription)
                        }) {
                            Label("Optimize", systemImage: "sparkles")
                        }
                    } label: {
                        Image(systemName: "sparkles")
                    }
                    .disabled(jobDescription.isEmpty || viewModel.isOptimizing || viewModel.isAnalyzing)
                }
            }
        }
    }
}

struct CreateResumeSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var industry = ""
    
    var onSave: (String, String?) -> Void
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Resume Name", text: $name)
                    TextField("Industry (Optional)", text: $industry)
                } footer: {
                    Text("Give your resume a descriptive name to easily identify it later.")
                }
            }
            .navigationTitle("New Resume")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        onSave(name, industry.isEmpty ? nil : industry)
                    }
                    .disabled(name.isEmpty)
                }
            }
        }
    }
}

#Preview {
    // Note: Preview won't work without Koin initialization
    // ResumeView()
    Text("Resume Preview - Run on device/simulator")
}
