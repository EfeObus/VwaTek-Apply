import SwiftUI
import shared

/// Phase 12: LinkedIn Optimizer View for iOS
struct LinkedInOptimizerView: View {
    @StateObject private var viewModel = LinkedInOptimizerViewModelWrapper()
    @State private var profileUrl = ""
    @State private var targetRole = ""
    @State private var targetIndustry = ""
    @State private var selectedTab = 0
    @State private var showError = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Tab picker
                Picker("Tab", selection: $selectedTab) {
                    Text("Analyze").tag(0)
                    Text("Optimize").tag(1)
                    Text("History").tag(2)
                }
                .pickerStyle(.segmented)
                .padding()
                
                switch selectedTab {
                case 0:
                    analyzeTab
                case 1:
                    optimizeTab
                case 2:
                    historyTab
                default:
                    EmptyView()
                }
            }
            .navigationTitle("LinkedIn Optimizer")
            .onAppear {
                viewModel.loadHistory()
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") { }
            } message: {
                Text(viewModel.analysisError ?? viewModel.optimizationError ?? viewModel.historyError ?? "An unknown error occurred")
            }
            .onChange(of: viewModel.analysisError) { error in
                if error != nil { showError = true }
            }
            .onChange(of: viewModel.optimizationError) { error in
                if error != nil { showError = true }
            }
            .onChange(of: viewModel.historyError) { error in
                if error != nil { showError = true }
            }
        }
    }
    
    // MARK: - Analyze Tab
    
    private var analyzeTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Input card
                VStack(alignment: .leading, spacing: 12) {
                    Text("Analyze Your Profile")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Enter your LinkedIn profile URL to get AI-powered analysis and recommendations.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    HStack {
                        Image(systemName: "person.fill")
                            .foregroundColor(.secondary)
                        TextField("https://linkedin.com/in/yourprofile", text: $profileUrl)
                            .textFieldStyle(.plain)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    
                    Button(action: {
                        viewModel.analyzeProfile(profileUrl: profileUrl.isEmpty ? nil : profileUrl)
                    }) {
                        HStack {
                            if viewModel.isAnalyzing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            }
                            Text(viewModel.isAnalyzing ? "Analyzing..." : "Analyze Profile")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(red: 0.04, green: 0.4, blue: 0.76)) // LinkedIn blue
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .disabled(viewModel.isAnalyzing)
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
                
                // Results
                if let result = viewModel.analysisResult {
                    analysisResultView(result: result)
                }
                
                if let error = viewModel.analysisError {
                    errorCard(message: error)
                }
            }
            .padding()
        }
    }
    
    // MARK: - Analysis Results
    
    @ViewBuilder
    private func analysisResultView(result: LinkedInAnalysisResult) -> some View {
        let analysis = result.analysis
        
        // Overall Score
        VStack(spacing: 8) {
            HStack {
                Text("Overall Score")
                    .font(.title3)
                    .fontWeight(.semibold)
                Spacer()
                Text("\(analysis.overallScore)/100")
                    .font(.title3)
                    .fontWeight(.bold)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
                    .background(scoreColor(score: Int(analysis.overallScore)))
                    .foregroundColor(.white)
                    .cornerRadius(20)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        
        // Section Scores
        VStack(alignment: .leading, spacing: 12) {
            Text("Section Scores")
                .font(.title3)
                .fontWeight(.semibold)
            
            scoreRow(label: "Headline", score: Int(analysis.sectionScores.headline))
            scoreRow(label: "Summary", score: Int(analysis.sectionScores.summary))
            scoreRow(label: "Experience", score: Int(analysis.sectionScores.experience))
            scoreRow(label: "Education", score: Int(analysis.sectionScores.education))
            scoreRow(label: "Skills", score: Int(analysis.sectionScores.skills))
            scoreRow(label: "Completeness", score: Int(analysis.sectionScores.completeness))
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        
        // Strengths
        if !analysis.strengths.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Strengths")
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)
                
                ForEach(analysis.strengths, id: \.self) { strength in
                    HStack(alignment: .top) {
                        Text("✅")
                        Text(strength)
                            .font(.body)
                    }
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        }
        
        // Improvements
        if !analysis.improvements.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Improvements")
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundColor(.red)
                
                ForEach(Array(analysis.improvements.enumerated()), id: \.offset) { _, improvement in
                    VStack(alignment: .leading, spacing: 4) {
                        Text("⚠️ \(improvement.issue)")
                            .font(.body)
                            .fontWeight(.medium)
                        Text(improvement.suggestion)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 2)
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        }
        
        // Clear button
        Button("Clear Analysis") {
            viewModel.clearProfile()
        }
        .foregroundColor(.blue)
    }
    
    // MARK: - Optimize Tab
    
    private var optimizeTab: some View {
        ScrollView {
            VStack(spacing: 16) {
                if viewModel.analysisResult == nil {
                    VStack(spacing: 12) {
                        Image(systemName: "info.circle")
                            .font(.system(size: 40))
                            .foregroundColor(.blue)
                        Text("Analyze your profile first")
                            .font(.title3)
                            .fontWeight(.semibold)
                        Text("Go to the Analyze tab to analyze your LinkedIn profile before optimizing.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(32)
                } else if let result = viewModel.analysisResult {
                    // Optimization form
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Optimize Content")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        TextField("Target Role (optional)", text: $targetRole)
                            .textFieldStyle(.roundedBorder)
                        
                        TextField("Target Industry (optional)", text: $targetIndustry)
                            .textFieldStyle(.roundedBorder)
                        
                        Button(action: {
                            viewModel.getOptimizedContent(
                                profileId: result.profileId,
                                targetRole: targetRole.isEmpty ? nil : targetRole,
                                targetIndustry: targetIndustry.isEmpty ? nil : targetIndustry
                            )
                        }) {
                            HStack {
                                if viewModel.isGenerating {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                }
                                Text(viewModel.isGenerating ? "Generating..." : "Generate Optimizations")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(red: 0.04, green: 0.4, blue: 0.76))
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                        .disabled(viewModel.isGenerating)
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
                    
                    // Optimization results
                    if let content = viewModel.optimizedContent {
                        optimizationResultView(content: content)
                    }
                    
                    if let error = viewModel.optimizationError {
                        errorCard(message: error)
                    }
                }
            }
            .padding()
        }
    }
    
    @ViewBuilder
    private func optimizationResultView(content: OptimizedLinkedInContent) -> some View {
        if let headline = content.headline {
            optimizationCard(title: "Optimized Headline", text: headline)
        }
        
        if !content.headlineAlternatives.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Alternative Headlines")
                    .font(.title3)
                    .fontWeight(.semibold)
                ForEach(Array(content.headlineAlternatives.enumerated()), id: \.offset) { index, alt in
                    Text("\(index + 1). \(alt)")
                        .font(.body)
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        }
        
        if let summary = content.summary {
            optimizationCard(title: "Optimized Summary", text: summary)
        }
        
        if !content.skillSuggestions.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Suggested Skills")
                    .font(.title3)
                    .fontWeight(.semibold)
                ForEach(content.skillSuggestions, id: \.self) { skill in
                    HStack {
                        Text("•")
                            .fontWeight(.bold)
                        Text(skill)
                    }
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        }
        
        Button("Clear Optimizations") {
            viewModel.clearOptimization()
        }
        .foregroundColor(.blue)
    }
    
    // MARK: - History Tab
    
    private var historyTab: some View {
        Group {
            if viewModel.isLoadingHistory {
                VStack {
                    Spacer()
                    ProgressView("Loading history...")
                    Spacer()
                }
            } else if viewModel.history.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "clock")
                        .font(.system(size: 40))
                        .foregroundColor(.secondary)
                    Text("No analysis history yet")
                        .font(.body)
                        .foregroundColor(.secondary)
                    Button("Refresh") {
                        viewModel.loadHistory()
                    }
                    Spacer()
                }
            } else if let error = viewModel.historyError {
                VStack(spacing: 12) {
                    Spacer()
                    Text(error)
                        .foregroundColor(.red)
                    Button("Retry") {
                        viewModel.loadHistory()
                    }
                    Spacer()
                }
            } else {
                List(Array(viewModel.history.enumerated()), id: \.offset) { _, result in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(result.profile.headline ?? "LinkedIn Profile")
                                .font(.headline)
                            Text("Analyzed: \(String(result.analyzedAt.prefix(10)))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Text("\(result.analysis.overallScore)")
                            .font(.callout)
                            .fontWeight(.bold)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 2)
                            .background(scoreColor(score: Int(result.analysis.overallScore)))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                }
            }
        }
    }
    
    // MARK: - Helper Views
    
    private func scoreRow(label: String, score: Int) -> some View {
        HStack {
            Text(label)
                .frame(width: 100, alignment: .leading)
            ProgressView(value: Double(score), total: 100)
                .tint(scoreColor(score: score))
            Text("\(score)")
                .fontWeight(.bold)
                .frame(width: 30, alignment: .trailing)
        }
    }
    
    private func optimizationCard(title: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(Color(red: 0.04, green: 0.4, blue: 0.76))
            Text(text)
                .font(.body)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
    
    private func errorCard(message: String) -> some View {
        Text(message)
            .foregroundColor(.red)
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.red.opacity(0.1))
            .cornerRadius(12)
    }
    
    private func scoreColor(score: Int) -> Color {
        switch score {
        case 80...100: return .green
        case 60..<80: return .yellow
        case 40..<60: return .orange
        default: return .red
        }
    }
}
