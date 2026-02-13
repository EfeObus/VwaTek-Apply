import SwiftUI

/// Salary Insights View for iOS - Premium Feature
/// Phase 4: Premium & Monetization
struct SalaryInsightsView: View {
    @State private var jobTitle: String = ""
    @State private var location: String = ""
    @State private var yearsExperience: String = ""
    @State private var skills: String = ""
    
    @State private var isLoading = false
    @State private var hasAccess = false // Would be checked from subscription manager
    @State private var showPaywall = false
    
    @State private var salaryInsights: SalaryInsightsData? = nil
    @State private var errorMessage: String? = nil
    
    var onShowPaywall: () -> Void = {}
    
    var body: some View {
        NavigationView {
            Group {
                if !hasAccess {
                    // Show upgrade prompt
                    FeatureGatedView(
                        feature: .salaryInsights,
                        hasAccess: hasAccess,
                        requiredTier: .pro,
                        onUpgradeClick: { showPaywall = true }
                    ) { }
                } else {
                    ScrollView {
                        VStack(spacing: 24) {
                            // Search Form
                            searchFormCard
                            
                            // Results
                            if isLoading {
                                ProgressView()
                                    .padding()
                            } else if let insights = salaryInsights {
                                salaryResultsView(insights: insights)
                            } else if let error = errorMessage {
                                errorView(message: error)
                            } else {
                                emptyStateView
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Salary Insights")
            .sheet(isPresented: $showPaywall) {
                PaywallView(
                    feature: .salaryInsights,
                    requiredTier: .pro,
                    onDismiss: { showPaywall = false },
                    onUpgrade: { tier, period in
                        // Handle upgrade
                        showPaywall = false
                    }
                )
            }
        }
    }
    
    // MARK: - Components
    
    private var searchFormCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Search Salary Data")
                .font(.headline)
            
            VStack(spacing: 12) {
                HStack {
                    Image(systemName: "briefcase")
                        .foregroundColor(.secondary)
                    TextField("Job Title (e.g., Software Engineer)", text: $jobTitle)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "location")
                        .foregroundColor(.secondary)
                    TextField("Location (e.g., Toronto, ON)", text: $location)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "clock")
                        .foregroundColor(.secondary)
                    TextField("Years of Experience", text: $yearsExperience)
                        .keyboardType(.numberPad)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "tag")
                        .foregroundColor(.secondary)
                    TextField("Key Skills (comma separated)", text: $skills)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
            
            Button {
                searchSalary()
            } label: {
                HStack {
                    Image(systemName: "magnifyingglass")
                    Text("Get Salary Insights")
                }
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding()
                .background(jobTitle.isEmpty || location.isEmpty ? Color.gray : Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(jobTitle.isEmpty || location.isEmpty)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
    }
    
    private func salaryResultsView(insights: SalaryInsightsData) -> some View {
        VStack(spacing: 16) {
            // Salary Range Card
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(insights.jobTitle)
                        .font(.title2)
                        .fontWeight(.bold)
                    Text(insights.location)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    SalaryDataPoint(label: "Min", value: insights.minSalary)
                    Spacer()
                    SalaryDataPoint(label: "Median", value: insights.medianSalary, isHighlighted: true)
                    Spacer()
                    SalaryDataPoint(label: "Max", value: insights.maxSalary)
                }
                
                // Salary bar
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.accentColor.opacity(0.2))
                            .frame(height: 8)
                        
                        let medianPosition = CGFloat(insights.medianSalary - insights.minSalary) / CGFloat(insights.maxSalary - insights.minSalary)
                        Capsule()
                            .fill(Color.accentColor)
                            .frame(width: geometry.size.width * medianPosition, height: 8)
                    }
                }
                .frame(height: 8)
            }
            .padding()
            .background(Color.accentColor.opacity(0.1))
            .cornerRadius(16)
            
            // Market Data
            VStack(alignment: .leading, spacing: 12) {
                Text("Market Data")
                    .font(.headline)
                
                StatRow(label: "Data Points", value: "\(insights.dataPoints) salaries")
                StatRow(label: "Experience", value: "\(insights.yearsExperience) years")
                StatRow(label: "Confidence", value: "\(insights.confidence)%")
                
                if let percentile = insights.percentile {
                    StatRow(label: "Your Position", value: "\(percentile)th percentile")
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            
            // Insights
            if !insights.insights.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Key Insights")
                        .font(.headline)
                    
                    ForEach(insights.insights, id: \.self) { insight in
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "lightbulb.fill")
                                .foregroundColor(.yellow)
                            Text(insight)
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            }
            
            // Recommendations
            if !insights.recommendations.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Recommendations")
                        .font(.headline)
                    
                    ForEach(insights.recommendations, id: \.self) { recommendation in
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "star.fill")
                                .foregroundColor(.orange)
                            Text(recommendation)
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            }
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "dollarsign.circle")
                .font(.system(size: 64))
                .foregroundColor(.secondary.opacity(0.5))
            
            Text("Search for Salary Data")
                .font(.headline)
            
            Text("Enter a job title and location to see salary insights for your target role")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
    
    private func errorView(message: String) -> some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
            Text(message)
                .font(.subheadline)
        }
        .padding()
        .background(Color.red.opacity(0.1))
        .cornerRadius(12)
    }
    
    // MARK: - Actions
    
    private func searchSalary() {
        isLoading = true
        errorMessage = nil
        
        // Simulated API call - would be replaced with actual API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            salaryInsights = SalaryInsightsData(
                jobTitle: jobTitle,
                location: location,
                minSalary: 70000,
                medianSalary: 95000,
                maxSalary: 150000,
                dataPoints: 1234,
                yearsExperience: Int(yearsExperience) ?? 0,
                confidence: 87,
                percentile: 65,
                insights: [
                    "Salaries in \(location) are 12% above the national average",
                    "Skills like \(skills.isEmpty ? "cloud computing" : skills) can increase salary by 15%",
                    "Remote positions typically offer 8% higher compensation"
                ],
                recommendations: [
                    "Consider highlighting your experience with distributed systems",
                    "Certifications could increase your market value by 10-15%",
                    "Negotiating for equity can significantly increase total compensation"
                ]
            )
            isLoading = false
        }
    }
}

// MARK: - Supporting Views

struct SalaryDataPoint: View {
    let label: String
    let value: Int
    var isHighlighted: Bool = false
    
    var body: some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(formatSalary(value))
                .font(isHighlighted ? .title : .headline)
                .fontWeight(.bold)
        }
    }
    
    private func formatSalary(_ amount: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: amount)) ?? "$\(amount)"
    }
}

struct StatRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
        .font(.subheadline)
    }
}

// MARK: - Data Models

struct SalaryInsightsData {
    let jobTitle: String
    let location: String
    let minSalary: Int
    let medianSalary: Int
    let maxSalary: Int
    let dataPoints: Int
    let yearsExperience: Int
    let confidence: Int
    let percentile: Int?
    let insights: [String]
    let recommendations: [String]
}

#Preview {
    SalaryInsightsView()
}
