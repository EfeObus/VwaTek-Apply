import SwiftUI
import shared

/// Salary Insights View for iOS - Premium Feature
/// Uses SalaryIntelligenceManager from shared module for real API data
struct SalaryInsightsView: View {
    @StateObject private var viewModel = SalaryIntelligenceManagerWrapper()
    
    @State private var jobTitle: String = ""
    @State private var province: String = ""
    @State private var city: String = ""
    @State private var yearsExperience: String = ""
    
    @State private var showPaywall = false
    @State private var showOfferEvaluation = false
    @State private var showError = false
    
    // Offer evaluation form
    @State private var offerJobTitle: String = ""
    @State private var offerCompany: String = ""
    @State private var offerBaseSalary: String = ""
    @State private var offerProvince: String = ""
    
    var onShowPaywall: () -> Void = {}
    
    var body: some View {
        NavigationView {
            Group {
                if !viewModel.hasAccess {
                    FeatureGatedView(
                        feature: .salaryInsights,
                        hasAccess: viewModel.hasAccess,
                        requiredTier: .pro,
                        onUpgradeClick: { showPaywall = true }
                    ) { }
                } else {
                    ScrollView {
                        VStack(spacing: 24) {
                            // Tab selector
                            Picker("View", selection: $showOfferEvaluation) {
                                Text("Salary Search").tag(false)
                                Text("Offer Evaluation").tag(true)
                            }
                            .pickerStyle(.segmented)
                            .padding(.horizontal)
                            
                            if showOfferEvaluation {
                                offerEvaluationSection
                            } else {
                                salarySearchSection
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
                        showPaywall = false
                        viewModel.refreshAccess()
                    }
                )
            }
            .onAppear {
                viewModel.refreshAccess()
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") { viewModel.insightsError = nil }
            } message: {
                Text(viewModel.insightsError ?? "An unknown error occurred")
            }
            .onChange(of: viewModel.insightsError) { error in
                if error != nil { showError = true }
            }
        }
    }
    
    // MARK: - Salary Search Section
    
    private var salarySearchSection: some View {
        VStack(spacing: 24) {
            searchFormCard
            
            if viewModel.isLoading {
                ProgressView("Loading salary data...")
                    .padding()
            } else if let insights = viewModel.salaryInsights {
                salaryResultsView(insights: insights)
            } else if let error = viewModel.insightsError {
                errorView(message: error)
            } else {
                emptyStateView
            }
        }
    }
    
    // MARK: - Offer Evaluation Section
    
    private var offerEvaluationSection: some View {
        VStack(spacing: 24) {
            offerFormCard
            
            if viewModel.isLoading {
                ProgressView("Evaluating offer...")
                    .padding()
            } else if let evaluation = viewModel.offerEvaluation {
                offerResultsView(evaluation: evaluation)
            } else if let error = viewModel.insightsError {
                errorView(message: error)
            } else {
                offerEmptyStateView
            }
        }
    }
    
    // MARK: - Components
    
    // MARK: - Search Form
    
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
                    Image(systemName: "map")
                        .foregroundColor(.secondary)
                    TextField("Province (e.g., Ontario)", text: $province)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "building.2")
                        .foregroundColor(.secondary)
                    TextField("City (optional)", text: $city)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "clock")
                        .foregroundColor(.secondary)
                    TextField("Years of Experience (optional)", text: $yearsExperience)
                        .keyboardType(.numberPad)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
            
            Button {
                viewModel.searchSalary(
                    jobTitle: jobTitle,
                    province: province,
                    city: city.isEmpty ? nil : city,
                    yearsExperience: Int(yearsExperience)
                )
            } label: {
                HStack {
                    Image(systemName: "magnifyingglass")
                    Text("Get Salary Insights")
                }
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding()
                .background(jobTitle.isEmpty || province.isEmpty ? Color.gray : Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(jobTitle.isEmpty || province.isEmpty)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
    }
    
    // MARK: - Offer Form
    
    private var offerFormCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Evaluate a Job Offer")
                .font(.headline)
            
            VStack(spacing: 12) {
                HStack {
                    Image(systemName: "briefcase")
                        .foregroundColor(.secondary)
                    TextField("Job Title", text: $offerJobTitle)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "building.2")
                        .foregroundColor(.secondary)
                    TextField("Company", text: $offerCompany)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "dollarsign.circle")
                        .foregroundColor(.secondary)
                    TextField("Base Salary (CAD)", text: $offerBaseSalary)
                        .keyboardType(.decimalPad)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                HStack {
                    Image(systemName: "map")
                        .foregroundColor(.secondary)
                    TextField("Province", text: $offerProvince)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
            
            Button {
                guard let salary = Double(offerBaseSalary) else { return }
                let offer = JobOffer(
                    jobTitle: offerJobTitle,
                    company: offerCompany,
                    nocCode: nil,
                    baseSalary: salary,
                    signingBonus: nil,
                    annualBonus: nil,
                    stockOptions: nil,
                    benefits: nil,
                    province: offerProvince,
                    city: nil,
                    isRemote: false,
                    yearsExperienceRequired: nil
                )
                viewModel.evaluateOffer(offer: offer)
            } label: {
                HStack {
                    Image(systemName: "chart.bar.doc.horizontal")
                    Text("Evaluate Offer")
                }
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding()
                .background(offerJobTitle.isEmpty || offerCompany.isEmpty || offerBaseSalary.isEmpty ? Color.gray : Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(offerJobTitle.isEmpty || offerCompany.isEmpty || offerBaseSalary.isEmpty)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
    }
    
    // MARK: - Salary Results
    
    private func salaryResultsView(insights: SalaryInsights) -> some View {
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
                    SalaryDataPointView(label: "Low", value: insights.salaryRange.low)
                    Spacer()
                    SalaryDataPointView(label: "Median", value: insights.medianSalary, isHighlighted: true)
                    Spacer()
                    SalaryDataPointView(label: "High", value: insights.salaryRange.high)
                }
                
                // Salary bar
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.accentColor.opacity(0.2))
                            .frame(height: 8)
                        
                        let range = insights.salaryRange.high - insights.salaryRange.low
                        let medianPos = range > 0
                            ? CGFloat(insights.medianSalary - insights.salaryRange.low) / CGFloat(range)
                            : 0.5
                        Capsule()
                            .fill(Color.accentColor)
                            .frame(width: geometry.size.width * medianPos, height: 8)
                    }
                }
                .frame(height: 8)
                
                // Market Trend
                HStack {
                    Image(systemName: trendIcon(for: insights.marketTrend))
                        .foregroundColor(trendColor(for: insights.marketTrend))
                    Text("Market trend: \(trendLabel(for: insights.marketTrend))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding()
            .background(Color.accentColor.opacity(0.1))
            .cornerRadius(16)
            
            // Market Comparison
            VStack(alignment: .leading, spacing: 12) {
                Text("Market Comparison")
                    .font(.headline)
                
                if let percentile = insights.percentile {
                    StatRow(label: "Your Position", value: "\(percentile)th percentile")
                }
                StatRow(label: "vs Provincial Avg", value: formatPercentage(insights.comparisonToProvincialAverage))
                StatRow(label: "vs National Avg", value: formatPercentage(insights.comparisonToNationalAverage))
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            
            // Related Job Salaries
            if !insights.relatedJobSalaries.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Related Job Salaries")
                        .font(.headline)
                    
                    ForEach(insights.relatedJobSalaries, id: \.jobTitle) { related in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(related.jobTitle)
                                    .font(.subheadline)
                                Text("NOC: \(related.nocCode)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing) {
                                Text(formatSalary(related.medianSalary))
                                    .fontWeight(.medium)
                                Text(formatPercentage(related.salaryDifference))
                                    .font(.caption)
                                    .foregroundColor(related.salaryDifference >= 0 ? .green : .red)
                            }
                        }
                        .padding(.vertical, 4)
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
    
    // MARK: - Offer Evaluation Results
    
    private func offerResultsView(evaluation: OfferEvaluation) -> some View {
        VStack(spacing: 16) {
            // Overall Rating
            VStack(spacing: 12) {
                Text("Offer Rating")
                    .font(.headline)
                
                Text(evaluation.overallRating.name)
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(ratingColor(for: evaluation.overallRating))
                
                Text(evaluation.recommendation)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            
            // Compensation
            VStack(alignment: .leading, spacing: 12) {
                Text("Compensation Analysis")
                    .font(.headline)
                
                StatRow(label: "Base Salary", value: formatSalary(evaluation.offer.baseSalary))
                StatRow(label: "Market Median", value: formatSalary(evaluation.marketAnalysis.marketMedian))
                StatRow(label: "vs Market", value: formatPercentage(evaluation.marketAnalysis.comparisonToMarket))
                StatRow(label: "Total (1st Year)", value: formatSalary(evaluation.totalCompensation.totalFirstYear))
                StatRow(label: "Total (Annual)", value: formatSalary(evaluation.totalCompensation.totalAnnualized))
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            
            // Strengths
            if !evaluation.strengths.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Strengths")
                        .font(.headline)
                    
                    ForEach(evaluation.strengths, id: \.self) { strength in
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text(strength)
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            }
            
            // Concerns
            if !evaluation.concerns.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Concerns")
                        .font(.headline)
                    
                    ForEach(evaluation.concerns, id: \.self) { concern in
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            Text(concern)
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            }
            
            // Negotiation Opportunities
            if !evaluation.negotiationOpportunities.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Negotiation Opportunities")
                        .font(.headline)
                    
                    ForEach(Array(evaluation.negotiationOpportunities.enumerated()), id: \.offset) { _, opportunity in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(opportunity.area.name)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                Spacer()
                                Text(opportunity.priority.name)
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(priorityColor(for: opportunity.priority).opacity(0.2))
                                    .foregroundColor(priorityColor(for: opportunity.priority))
                                    .cornerRadius(8)
                            }
                            
                            HStack {
                                VStack(alignment: .leading) {
                                    Text("Current: \(opportunity.currentValue)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text("Target: \(opportunity.suggestedTarget)")
                                        .font(.caption)
                                        .foregroundColor(.accentColor)
                                }
                            }
                            
                            Text(opportunity.marketJustification)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
            }
        }
    }
    
    // MARK: - Empty States
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "dollarsign.circle")
                .font(.system(size: 64))
                .foregroundColor(.secondary.opacity(0.5))
            
            Text("Search for Salary Data")
                .font(.headline)
            
            Text("Enter a job title and province to see salary insights based on Canadian market data")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
    
    private var offerEmptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "chart.bar.doc.horizontal")
                .font(.system(size: 64))
                .foregroundColor(.secondary.opacity(0.5))
            
            Text("Evaluate a Job Offer")
                .font(.headline)
            
            Text("Enter your offer details to get a comprehensive market analysis and negotiation tips")
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
    
    // MARK: - Helpers
    
    private func formatSalary(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "CAD"
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: amount)) ?? "$\(Int(amount))"
    }
    
    private func formatPercentage(_ value: Double) -> String {
        let sign = value >= 0 ? "+" : ""
        return "\(sign)\(String(format: "%.1f", value))%"
    }
    
    private func trendIcon(for trend: MarketTrend) -> String {
        switch trend {
        case .increasing: return "arrow.up.right"
        case .stable: return "arrow.right"
        case .decreasing: return "arrow.down.right"
        default: return "arrow.right"
        }
    }
    
    private func trendColor(for trend: MarketTrend) -> Color {
        switch trend {
        case .increasing: return .green
        case .stable: return .blue
        case .decreasing: return .red
        default: return .secondary
        }
    }
    
    private func trendLabel(for trend: MarketTrend) -> String {
        switch trend {
        case .increasing: return "Increasing"
        case .stable: return "Stable"
        case .decreasing: return "Decreasing"
        default: return "Unknown"
        }
    }
    
    private func ratingColor(for rating: OfferRating) -> Color {
        switch rating {
        case .excellent: return .green
        case .good: return .blue
        case .fair: return .orange
        case .belowMarket: return .red
        case .poor: return .red
        default: return .secondary
        }
    }
    
    private func priorityColor(for priority: NegotiationPriority) -> Color {
        switch priority {
        case .high: return .red
        case .medium: return .orange
        case .low: return .blue
        default: return .secondary
        }
    }
}

// MARK: - Supporting Views

struct SalaryDataPointView: View {
    let label: String
    let value: Double
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
    
    private func formatSalary(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "CAD"
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: amount)) ?? "$\(Int(amount))"
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

#Preview {
    SalaryInsightsView()
}
