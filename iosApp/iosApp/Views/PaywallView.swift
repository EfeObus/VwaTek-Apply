import SwiftUI

/// Paywall View for iOS
/// Shows when a user tries to access a premium feature
/// Phase 4: Premium & Monetization
struct PaywallView: View {
    let feature: PremiumFeature
    let requiredTier: SubscriptionTier
    var onDismiss: () -> Void = {}
    var onUpgrade: (SubscriptionTier, BillingPeriod) -> Void = { _, _ in }
    
    @State private var selectedBillingPeriod: BillingPeriod = .yearly
    
    private var featureInfo: FeatureInfo {
        switch feature {
        case .salaryInsights:
            return FeatureInfo(
                title: "Salary Insights",
                description: "Get real-time salary data and market comparisons for your target roles",
                icon: "dollarsign.circle.fill",
                benefits: [
                    "Real-time salary benchmarks",
                    "Cost of living adjustments",
                    "Industry-specific data",
                    "Compare multiple locations"
                ]
            )
        case .negotiationCoach:
            return FeatureInfo(
                title: "AI Negotiation Coach",
                description: "Get personalized negotiation strategies powered by AI",
                icon: "brain.head.profile",
                benefits: [
                    "Personalized negotiation scripts",
                    "Counter-offer strategies",
                    "Real-time coaching chat",
                    "Role-play practice scenarios"
                ]
            )
        case .linkedInOptimizer:
            return FeatureInfo(
                title: "LinkedIn Optimizer",
                description: "Optimize your LinkedIn profile to attract more recruiters",
                icon: "person.crop.circle",
                benefits: [
                    "Profile analysis & scoring",
                    "AI-optimized headlines",
                    "Keyword optimization",
                    "Section-by-section improvements"
                ]
            )
        case .unlimitedApplications:
            return FeatureInfo(
                title: "Unlimited Applications",
                description: "Track unlimited job applications without restrictions",
                icon: "briefcase.fill",
                benefits: [
                    "Unlimited application tracking",
                    "Advanced analytics",
                    "Custom application stages",
                    "Bulk operations"
                ]
            )
        case .unlimitedAIEnhancements:
            return FeatureInfo(
                title: "Unlimited AI Enhancements",
                description: "Use AI to enhance your documents without daily limits",
                icon: "sparkles",
                benefits: [
                    "Unlimited AI suggestions",
                    "Priority AI processing",
                    "Advanced AI models",
                    "Custom enhancement styles"
                ]
            )
        case .unlimitedResumes:
            return FeatureInfo(
                title: "Unlimited Resumes",
                description: "Create unlimited resume versions for different roles",
                icon: "doc.text.fill",
                benefits: [
                    "Unlimited resume versions",
                    "Role-specific optimization",
                    "A/B testing versions",
                    "Version history"
                ]
            )
        case .unlimitedCoverLetters:
            return FeatureInfo(
                title: "Unlimited Cover Letters",
                description: "Generate unlimited cover letters tailored to each job",
                icon: "envelope.fill",
                benefits: [
                    "Unlimited cover letters",
                    "Company-specific tailoring",
                    "Multiple tones & styles",
                    "Quick regeneration"
                ]
            )
        case .unlimitedInterviews:
            return FeatureInfo(
                title: "Unlimited Interview Practice",
                description: "Practice interviews with AI without session limits",
                icon: "person.wave.2.fill",
                benefits: [
                    "Unlimited practice sessions",
                    "Industry-specific questions",
                    "Detailed feedback",
                    "Progress tracking"
                ]
            )
        }
    }
    
    private var pricing: (monthly: Double, yearly: Double) {
        switch requiredTier {
        case .pro: return (14.99, 149.99)
        case .premium: return (29.99, 299.99)
        case .free: return (0, 0)
        }
    }
    
    var body: some View {
        VStack(spacing: 24) {
            // Feature icon
            ZStack {
                LinearGradient(
                    colors: [.blue, .purple],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(width: 80, height: 80)
                .cornerRadius(20)
                
                Image(systemName: featureInfo.icon)
                    .font(.system(size: 36))
                    .foregroundColor(.white)
            }
            
            // Feature title and description
            VStack(spacing: 8) {
                Text(featureInfo.title)
                    .font(.title2)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text(featureInfo.description)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            // Tier badge
            HStack(spacing: 8) {
                Image(systemName: "lock.fill")
                    .font(.caption)
                Text("Requires \(requiredTier.rawValue) plan")
                    .font(.caption)
                    .fontWeight(.bold)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(Color.accentColor.opacity(0.1))
            .foregroundColor(.accentColor)
            .cornerRadius(8)
            
            // Billing period toggle
            HStack(spacing: 8) {
                PaywallBillingOption(
                    title: "Monthly",
                    price: "$\(String(format: "%.2f", pricing.monthly))/mo",
                    isSelected: selectedBillingPeriod == .monthly
                ) {
                    selectedBillingPeriod = .monthly
                }
                
                PaywallBillingOption(
                    title: "Yearly",
                    price: "$\(String(format: "%.2f", pricing.yearly / 12))/mo",
                    savings: "Save 17%",
                    isSelected: selectedBillingPeriod == .yearly
                ) {
                    selectedBillingPeriod = .yearly
                }
            }
            .padding(4)
            .background(Color(.systemGray6))
            .cornerRadius(12)
            
            // Benefits list
            VStack(alignment: .leading, spacing: 12) {
                ForEach(featureInfo.benefits, id: \.self) { benefit in
                    HStack(spacing: 12) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.accentColor)
                        Text(benefit)
                            .font(.subheadline)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
            
            Spacer()
            
            // CTA Button
            Button {
                onUpgrade(requiredTier, selectedBillingPeriod)
            } label: {
                Text("Upgrade to \(requiredTier.rawValue)")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentColor)
                    .cornerRadius(16)
            }
            
            // Maybe later button
            Button("Maybe Later") {
                onDismiss()
            }
            .foregroundColor(.secondary)
            
            // Fine print
            Text("30-day money-back guarantee • Cancel anytime")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(24)
    }
}

// MARK: - Supporting Types

enum PremiumFeature {
    case salaryInsights
    case negotiationCoach
    case linkedInOptimizer
    case unlimitedApplications
    case unlimitedAIEnhancements
    case unlimitedResumes
    case unlimitedCoverLetters
    case unlimitedInterviews
}

struct FeatureInfo {
    let title: String
    let description: String
    let icon: String
    let benefits: [String]
}

// MARK: - Supporting Views

struct PaywallBillingOption: View {
    let title: String
    let price: String
    var savings: String? = nil
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(isSelected ? .white.opacity(0.8) : .secondary)
                Text(price)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(isSelected ? .white : .primary)
                if let savings = savings {
                    Text(savings)
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(isSelected ? .white.opacity(0.8) : .accentColor)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(isSelected ? Color.accentColor : Color.clear)
            .cornerRadius(8)
        }
    }
}

/// Inline paywall banner for displaying within screens
struct PaywallBanner: View {
    let feature: PremiumFeature
    let requiredTier: SubscriptionTier
    var onUpgradeClick: () -> Void = {}
    
    private var featureTitle: String {
        switch feature {
        case .salaryInsights: return "Salary Insights"
        case .negotiationCoach: return "Negotiation Coach"
        case .linkedInOptimizer: return "LinkedIn Optimizer"
        case .unlimitedApplications: return "Unlimited Applications"
        case .unlimitedAIEnhancements: return "Unlimited AI"
        case .unlimitedResumes: return "Unlimited Resumes"
        case .unlimitedCoverLetters: return "Unlimited Cover Letters"
        case .unlimitedInterviews: return "Unlimited Interviews"
        }
    }
    
    var body: some View {
        HStack {
            Image(systemName: "lock.fill")
                .foregroundColor(.accentColor)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(featureTitle)
                    .font(.subheadline)
                    .fontWeight(.bold)
                Text("Upgrade to \(requiredTier.rawValue) to unlock")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Button("Upgrade") {
                onUpgradeClick()
            }
            .font(.caption)
            .fontWeight(.bold)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        .padding()
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
    }
}

/// Feature gated content wrapper
struct FeatureGatedView<Content: View>: View {
    let feature: PremiumFeature
    let hasAccess: Bool
    let requiredTier: SubscriptionTier
    var onUpgradeClick: () -> Void = {}
    @ViewBuilder let content: () -> Content
    
    private var featureInfo: (title: String, description: String, icon: String) {
        switch feature {
        case .salaryInsights:
            return ("Salary Insights", "Get real-time salary data and market comparisons", "dollarsign.circle.fill")
        case .negotiationCoach:
            return ("Negotiation Coach", "Get personalized negotiation strategies", "brain.head.profile")
        case .linkedInOptimizer:
            return ("LinkedIn Optimizer", "Optimize your LinkedIn profile", "person.crop.circle")
        case .unlimitedApplications:
            return ("Unlimited Applications", "Track unlimited job applications", "briefcase.fill")
        case .unlimitedAIEnhancements:
            return ("Unlimited AI", "Use AI without daily limits", "sparkles")
        case .unlimitedResumes:
            return ("Unlimited Resumes", "Create unlimited resume versions", "doc.text.fill")
        case .unlimitedCoverLetters:
            return ("Unlimited Cover Letters", "Generate unlimited cover letters", "envelope.fill")
        case .unlimitedInterviews:
            return ("Unlimited Interviews", "Practice without session limits", "person.wave.2.fill")
        }
    }
    
    var body: some View {
        if hasAccess {
            content()
        } else {
            VStack(spacing: 24) {
                Spacer()
                
                Image(systemName: featureInfo.icon)
                    .font(.system(size: 64))
                    .foregroundColor(.secondary.opacity(0.5))
                
                VStack(spacing: 8) {
                    Text(featureInfo.title)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text(featureInfo.description)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                
                Button {
                    onUpgradeClick()
                } label: {
                    HStack {
                        Image(systemName: "lock.fill")
                        Text("Upgrade to \(requiredTier.rawValue)")
                    }
                    .fontWeight(.semibold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                
                Spacer()
            }
        }
    }
}

#Preview("Paywall") {
    PaywallView(
        feature: .salaryInsights,
        requiredTier: .pro
    )
}

#Preview("Paywall Banner") {
    PaywallBanner(
        feature: .negotiationCoach,
        requiredTier: .premium
    )
    .padding()
}
