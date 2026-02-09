import SwiftUI
import shared

struct OptimizerView: View {
    @StateObject private var viewModel = ResumeViewModelWrapper()
    @State private var selectedTab = 0
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Tab Picker
                Picker("Optimizer Section", selection: $selectedTab) {
                    Text("ATS Analysis").tag(0)
                    Text("Section Rewriter").tag(1)
                }
                .pickerStyle(.segmented)
                .padding()
                
                // Tab Content
                if selectedTab == 0 {
                    ATSAnalysisView(viewModel: viewModel)
                } else {
                    SectionRewriterView(viewModel: viewModel)
                }
            }
            .navigationTitle("Resume Optimizer")
        }
    }
}

// MARK: - ATS Analysis View
struct ATSAnalysisView: View {
    @ObservedObject var viewModel: ResumeViewModelWrapper
    @State private var selectedResumeId: String? = nil
    @State private var jobDescription = ""
    @State private var targetKeywords = ""
    
    var selectedResume: Resume? {
        viewModel.resumes.first { $0.id == selectedResumeId }
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Instructions Card
                VStack(alignment: .leading, spacing: 8) {
                    Label("How it works", systemImage: "info.circle.fill")
                        .font(.headline)
                        .foregroundColor(.blue)
                    
                    Text("Analyze your resume for ATS (Applicant Tracking System) compatibility. Get actionable insights to improve your chances of passing automated screening.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.blue.opacity(0.1))
                .cornerRadius(12)
                .padding(.horizontal)
                
                // Resume Selection
                VStack(alignment: .leading, spacing: 8) {
                    Text("Select Resume")
                        .font(.headline)
                    
                    if viewModel.resumes.isEmpty {
                        HStack {
                            Image(systemName: "exclamationmark.triangle")
                                .foregroundColor(.orange)
                            Text("No resumes found. Please create a resume first.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(8)
                    } else {
                        Menu {
                            ForEach(viewModel.resumes, id: \.id) { resume in
                                Button(action: { selectedResumeId = resume.id }) {
                                    HStack {
                                        Text(resume.name)
                                        if selectedResumeId == resume.id {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            HStack {
                                Text(selectedResume?.name ?? "Select a resume...")
                                    .foregroundColor(selectedResume == nil ? .secondary : .primary)
                                Spacer()
                                Image(systemName: "chevron.down")
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(10)
                        }
                    }
                }
                .padding(.horizontal)
                
                // Job Description
                VStack(alignment: .leading, spacing: 8) {
                    Text("Target Job Description")
                        .font(.headline)
                    
                    Text("(Optional) Paste the job description for targeted analysis")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    TextEditor(text: $jobDescription)
                        .frame(minHeight: 120)
                        .padding(8)
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                
                // Target Keywords
                VStack(alignment: .leading, spacing: 8) {
                    Text("Target Keywords")
                        .font(.headline)
                    
                    Text("(Optional) Comma-separated keywords to check")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    TextField("e.g., Python, Machine Learning, AWS", text: $targetKeywords)
                        .textFieldStyle(.roundedBorder)
                }
                .padding(.horizontal)
                
                // Analyze Button
                Button(action: {
                    if let resume = selectedResume {
                        viewModel.performATSAnalysis(
                            resume,
                            jobDescription: jobDescription.isEmpty ? nil : jobDescription
                        )
                    }
                }) {
                    HStack {
                        if viewModel.isATSAnalyzing {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Image(systemName: "magnifyingglass")
                        }
                        Text(viewModel.isATSAnalyzing ? "Analyzing..." : "Analyze Resume")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(selectedResumeId == nil ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(selectedResumeId == nil || viewModel.isATSAnalyzing)
                .padding(.horizontal)
                
                // ATS Results
                if let atsAnalysis = viewModel.atsAnalysis {
                    ATSResultsView(analysis: atsAnalysis, onClear: { viewModel.clearATSAnalysis() })
                        .padding(.horizontal)
                }
                
                Spacer(minLength: 40)
            }
            .padding(.top)
        }
        .onAppear {
            viewModel.loadResumes()
        }
    }
}

// MARK: - ATS Results View
struct ATSResultsView: View {
    let analysis: ATSAnalysis
    let onClear: () -> Void
    
    var scoreColor: Color {
        if analysis.overallScore >= 80 { return .green }
        else if analysis.overallScore >= 60 { return .orange }
        else { return .red }
    }
    
    // Combine all issues
    var allIssues: [ATSIssue] {
        Array(analysis.formattingIssues) + Array(analysis.structureIssues)
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Analysis Results")
                    .font(.headline)
                Spacer()
                Button(action: onClear) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
            
            // Score Circle
            HStack {
                Spacer()
                VStack {
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.2), lineWidth: 12)
                            .frame(width: 100, height: 100)
                        
                        Circle()
                            .trim(from: 0, to: Double(analysis.overallScore) / 100.0)
                            .stroke(scoreColor, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                            .frame(width: 100, height: 100)
                            .rotationEffect(.degrees(-90))
                        
                        VStack(spacing: 2) {
                            Text("\(analysis.overallScore)")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(scoreColor)
                            Text("/ 100")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Text("ATS Score")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding(.vertical)
            
            // Score Breakdown
            VStack(alignment: .leading, spacing: 8) {
                Label("Score Breakdown", systemImage: "chart.bar.fill")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                ScoreRow(title: "Formatting", score: Int(analysis.formattingScore))
                ScoreRow(title: "Keywords", score: Int(analysis.keywordScore))
                ScoreRow(title: "Structure", score: Int(analysis.structureScore))
                ScoreRow(title: "Readability", score: Int(analysis.readabilityScore))
            }
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(8)
            
            // Issues
            if !allIssues.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Issues Found (\(allIssues.count))", systemImage: "exclamationmark.triangle.fill")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                    
                    ForEach(Array(allIssues.enumerated()), id: \.offset) { _, issue in
                        ATSIssueRow(issue: issue)
                    }
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(8)
            }
            
            // Recommendations
            if !analysis.recommendations.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Recommendations", systemImage: "lightbulb.fill")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.green)
                    
                    ForEach(Array(analysis.recommendations.enumerated()), id: \.offset) { _, rec in
                        ATSRecommendationRow(recommendation: rec)
                    }
                }
                .padding()
                .background(Color.green.opacity(0.1))
                .cornerRadius(8)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
}

struct ScoreRow: View {
    let title: String
    let score: Int
    
    var scoreColor: Color {
        if score >= 80 { return .green }
        else if score >= 60 { return .orange }
        else { return .red }
    }
    
    var body: some View {
        HStack {
            Text(title)
                .font(.subheadline)
            Spacer()
            Text("\(score)%")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(scoreColor)
        }
    }
}

struct ATSIssueRow: View {
    let issue: ATSIssue
    
    var severityColor: Color {
        switch issue.severity {
        case .high: return .red
        case .medium: return .orange
        case .low: return .yellow
        default: return .gray
        }
    }
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Circle()
                .fill(severityColor)
                .frame(width: 8, height: 8)
                .padding(.top, 6)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(issue.description_)
                    .font(.subheadline)
                if !issue.suggestion.isEmpty {
                    Text("Suggestion: \(issue.suggestion)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}

struct ATSRecommendationRow: View {
    let recommendation: ATSRecommendation
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.green)
                .font(.caption)
                .padding(.top, 2)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(recommendation.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(recommendation.description_)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - Section Rewriter View
struct SectionRewriterView: View {
    @ObservedObject var viewModel: ResumeViewModelWrapper
    
    @State private var selectedSectionType = "SUMMARY"
    @State private var sectionContent = ""
    @State private var targetRole = ""
    @State private var targetIndustry = ""
    @State private var selectedStyle = "professional"
    
    let sectionTypes = [
        ("SUMMARY", "Professional Summary"),
        ("EXPERIENCE", "Work Experience"),
        ("SKILLS", "Skills"),
        ("EDUCATION", "Education")
    ]
    
    let writingStyles = [
        ("professional", "Professional - Corporate, formal tone"),
        ("confident", "Confident - Bold, assertive language"),
        ("results-driven", "Results-Driven - Focus on achievements"),
        ("innovative", "Innovative - Creative, forward-thinking")
    ]
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Instructions Card
                VStack(alignment: .leading, spacing: 8) {
                    Label("Section Rewriter", systemImage: "pencil.and.outline")
                        .font(.headline)
                        .foregroundColor(.purple)
                    
                    Text("Paste any section of your resume and get AI-powered rewrites optimized for impact and clarity.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.purple.opacity(0.1))
                .cornerRadius(12)
                .padding(.horizontal)
                
                // Section Type Picker
                VStack(alignment: .leading, spacing: 8) {
                    Text("Section Type")
                        .font(.headline)
                    
                    Menu {
                        ForEach(sectionTypes, id: \.0) { type in
                            Button(action: { selectedSectionType = type.0 }) {
                                HStack {
                                    Text(type.1)
                                    if selectedSectionType == type.0 {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(sectionTypes.first { $0.0 == selectedSectionType }?.1 ?? "Select type...")
                            Spacer()
                            Image(systemName: "chevron.down")
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                    }
                }
                .padding(.horizontal)
                
                // Section Content
                VStack(alignment: .leading, spacing: 8) {
                    Text("Current Content")
                        .font(.headline)
                    
                    Text("Paste the current content of this section")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    TextEditor(text: $sectionContent)
                        .frame(minHeight: 150)
                        .padding(8)
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                
                // Target Role
                VStack(alignment: .leading, spacing: 8) {
                    Text("Target Role (Optional)")
                        .font(.headline)
                    
                    TextField("e.g., Senior Software Engineer", text: $targetRole)
                        .textFieldStyle(.roundedBorder)
                }
                .padding(.horizontal)
                
                // Target Industry
                VStack(alignment: .leading, spacing: 8) {
                    Text("Target Industry (Optional)")
                        .font(.headline)
                    
                    TextField("e.g., FinTech, Healthcare", text: $targetIndustry)
                        .textFieldStyle(.roundedBorder)
                }
                .padding(.horizontal)
                
                // Writing Style
                VStack(alignment: .leading, spacing: 8) {
                    Text("Writing Style")
                        .font(.headline)
                    
                    Menu {
                        ForEach(writingStyles, id: \.0) { style in
                            Button(action: { selectedStyle = style.0 }) {
                                HStack {
                                    Text(style.1)
                                    if selectedStyle == style.0 {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(writingStyles.first { $0.0 == selectedStyle }?.1 ?? "Select style...")
                            Spacer()
                            Image(systemName: "chevron.down")
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                    }
                }
                .padding(.horizontal)
                
                // Rewrite Button
                Button(action: {
                    viewModel.rewriteSection(
                        sectionType: selectedSectionType,
                        sectionContent: sectionContent,
                        targetRole: targetRole.isEmpty ? nil : targetRole,
                        targetIndustry: targetIndustry.isEmpty ? nil : targetIndustry,
                        style: selectedStyle
                    )
                }) {
                    HStack {
                        if viewModel.isRewritingSection {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Image(systemName: "sparkles")
                        }
                        Text(viewModel.isRewritingSection ? "Rewriting..." : "Rewrite Section")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(sectionContent.isEmpty ? Color.gray : Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(sectionContent.isEmpty || viewModel.isRewritingSection)
                .padding(.horizontal)
                
                // Rewrite Results
                if let result = viewModel.sectionRewriteResult {
                    SectionRewriteResultsView(result: result, onClear: { viewModel.clearSectionRewrite() })
                        .padding(.horizontal)
                }
                
                Spacer(minLength: 40)
            }
            .padding(.top)
        }
    }
}

// MARK: - Section Rewrite Results View
struct SectionRewriteResultsView: View {
    let result: SectionRewriteResult
    let onClear: () -> Void
    
    @State private var copied = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Rewritten Content")
                    .font(.headline)
                Spacer()
                Button(action: onClear) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
            
            // Main Rewritten Content
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Improved Version")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Spacer()
                    Button(action: {
                        UIPasteboard.general.string = result.rewrittenContent
                        copied = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            copied = false
                        }
                    }) {
                        Label(copied ? "Copied!" : "Copy", systemImage: copied ? "checkmark" : "doc.on.doc")
                            .font(.caption)
                    }
                }
                
                Text(result.rewrittenContent)
                    .font(.body)
                    .textSelection(.enabled)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(8)
            }
            
            // Changes Made
            if !result.changes.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Changes Made", systemImage: "arrow.triangle.2.circlepath")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.blue)
                    
                    ForEach(result.changes, id: \.self) { change in
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                            Text(change)
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(Color.blue.opacity(0.1))
                .cornerRadius(8)
            }
            
            // Keywords Incorporated
            if !result.keywords.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Keywords Added", systemImage: "key.fill")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.green)
                    
                    FlowLayout(spacing: 8) {
                        ForEach(result.keywords, id: \.self) { keyword in
                            Text(keyword)
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.green.opacity(0.2))
                                .foregroundColor(.green)
                                .cornerRadius(12)
                        }
                    }
                }
                .padding()
                .background(Color.green.opacity(0.1))
                .cornerRadius(8)
            }
            
            // Tips
            if !result.tips.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Additional Tips", systemImage: "lightbulb.fill")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                    
                    ForEach(result.tips, id: \.self) { tip in
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "lightbulb")
                                .foregroundColor(.orange)
                                .font(.caption)
                            Text(tip)
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(8)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
}

// Simple FlowLayout for tags
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        var height: CGFloat = 0
        var x: CGFloat = 0
        var rowHeight: CGFloat = 0
        
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > width && x > 0 {
                x = 0
                height += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        height += rowHeight
        return CGSize(width: width, height: height)
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x: CGFloat = bounds.minX
        var y: CGFloat = bounds.minY
        var rowHeight: CGFloat = 0
        
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX && x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

#Preview {
    OptimizerView()
}
