import SwiftUI
import shared
import StoreKit

/// Subscription View for iOS
/// Shows pricing tiers and allows users to upgrade via StoreKit
/// Wired to shared SubscriptionManager via SubscriptionManagerWrapper
struct SubscriptionView: View {
    @StateObject private var viewModel = SubscriptionManagerWrapper()
    @State private var selectedBillingPeriod: BillingPeriod = .yearly
    @State private var showManageBilling = false
    @State private var showError: String? = nil
    @State private var isPurchasing = false
    
    // StoreKit products
    @State private var products: [Product] = []
    
    var onDismiss: () -> Void = {}
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Demo mode banner
                    if viewModel.isDemoMode {
                        demoModeBanner
                    }
                    
                    // Current subscription banner
                    if viewModel.currentTierName != "FREE" {
                        currentSubscriptionBanner
                    }
                    
                    // Header
                    headerSection
                    
                    // Billing period toggle
                    billingPeriodToggle
                    
                    // Pricing cards
                    pricingCards
                    
                    // Feature comparison
                    featureComparisonSection
                    
                    // Fine print
                    Text("30-day money-back guarantee. Cancel anytime.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
            }
            .navigationTitle("Subscription")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") {
                        onDismiss()
                    }
                }
            }
            .onAppear {
                Task {
                    await loadProducts()
                }
            }
            .alert("Error", isPresented: .init(
                get: { showError != nil },
                set: { if !$0 { showError = nil } }
            )) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(showError ?? "An error occurred")
            }
            .overlay {
                if isPurchasing {
                    ProgressView("Processing purchase...")
                        .padding()
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                }
            }
        }
    }
    
    // MARK: - Components
    
    private var demoModeBanner: some View {
        HStack(spacing: 12) {
            Image(systemName: "star.fill")
                .foregroundColor(.green)
            VStack(alignment: .leading, spacing: 2) {
                Text("Demo Mode Active")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.2))
                Text(viewModel.demoModeMessage)
                    .font(.caption)
                    .foregroundColor(Color(red: 0.22, green: 0.56, blue: 0.24))
            }
            Spacer()
        }
        .padding()
        .background(Color.green.opacity(0.1))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.green, lineWidth: 1)
        )
        .cornerRadius(12)
    }
    
    private var currentSubscriptionBanner: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Current Plan")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(viewModel.currentTierName)
                    .font(.title2)
                    .fontWeight(.bold)
            }
            Spacer()
            Button("Manage Billing") {
                showManageBilling = true
            }
            .font(.subheadline)
        }
        .padding()
        .background(Color.accentColor.opacity(0.1))
        .cornerRadius(12)
    }
    
    private var headerSection: some View {
        VStack(spacing: 8) {
            Text("Choose Your Plan")
                .font(.title)
                .fontWeight(.bold)
            
            Text("Unlock powerful features to accelerate your job search")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
    }
    
    private var billingPeriodToggle: some View {
        HStack(spacing: 8) {
            BillingPeriodButton(
                title: "Monthly",
                isSelected: selectedBillingPeriod == .monthly
            ) {
                selectedBillingPeriod = .monthly
            }
            
            BillingPeriodButton(
                title: "Yearly",
                subtitle: "Save 17%",
                isSelected: selectedBillingPeriod == .yearly
            ) {
                selectedBillingPeriod = .yearly
            }
        }
        .padding(4)
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    private var pricingCards: some View {
        VStack(spacing: 16) {
            // Free Tier
            PricingCard(
                tier: .free,
                billingPeriod: selectedBillingPeriod,
                isCurrentPlan: viewModel.currentTierName == "FREE",
                isPopular: false,
                proMonthlyPrice: viewModel.proMonthlyPrice,
                proYearlyPrice: viewModel.proYearlyPrice,
                premiumMonthlyPrice: viewModel.premiumMonthlyPrice,
                premiumYearlyPrice: viewModel.premiumYearlyPrice,
                onSelect: { }
            )
            
            // Pro Tier
            PricingCard(
                tier: .pro,
                billingPeriod: selectedBillingPeriod,
                isCurrentPlan: viewModel.currentTierName == "PRO",
                isPopular: true,
                proMonthlyPrice: viewModel.proMonthlyPrice,
                proYearlyPrice: viewModel.proYearlyPrice,
                premiumMonthlyPrice: viewModel.premiumMonthlyPrice,
                premiumYearlyPrice: viewModel.premiumYearlyPrice,
                onSelect: {
                    Task { await purchaseProduct(tier: .pro) }
                }
            )
            
            // Premium Tier
            PricingCard(
                tier: .premium,
                billingPeriod: selectedBillingPeriod,
                isCurrentPlan: viewModel.currentTierName == "PREMIUM",
                isPopular: false,
                proMonthlyPrice: viewModel.proMonthlyPrice,
                proYearlyPrice: viewModel.proYearlyPrice,
                premiumMonthlyPrice: viewModel.premiumMonthlyPrice,
                premiumYearlyPrice: viewModel.premiumYearlyPrice,
                onSelect: {
                    Task { await purchaseProduct(tier: .premium) }
                }
            )
        }
    }
    
    private var featureComparisonSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Compare Plans")
                .font(.title3)
                .fontWeight(.bold)
            
            featureComparisonTable
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
    }
    
    private var featureComparisonTable: some View {
        VStack(spacing: 12) {
            // Header
            HStack {
                Text("Feature")
                    .font(.caption)
                    .fontWeight(.bold)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("Free")
                    .font(.caption)
                    .frame(width: 50)
                Text("Pro")
                    .font(.caption)
                    .frame(width: 50)
                Text("Premium")
                    .font(.caption)
                    .frame(width: 60)
            }
            .foregroundColor(.secondary)
            
            Divider()
            
            // Rows
            ComparisonRow(feature: "Resume builder", free: "3/mo", pro: "10/mo", premium: "∞")
            ComparisonRow(feature: "Cover letters", free: "3/mo", pro: "10/mo", premium: "∞")
            ComparisonRow(feature: "AI enhancements", free: "5/day", pro: "20/day", premium: "∞")
            ComparisonRow(feature: "Interview practice", free: "3/mo", pro: "10/mo", premium: "∞")
            ComparisonRow(feature: "Application tracker", free: "10", pro: "∞", premium: "∞")
            ComparisonRow(feature: "Salary insights", free: "✗", pro: "✓", premium: "✓")
            ComparisonRow(feature: "Negotiation coach", free: "✗", pro: "✗", premium: "✓")
            ComparisonRow(feature: "LinkedIn optimizer", free: "✗", pro: "✗", premium: "✓")
            ComparisonRow(feature: "Priority support", free: "✗", pro: "✗", premium: "✓")
        }
    }
    
    // MARK: - Data Loading & StoreKit
    
    private func loadProducts() async {
        do {
            let productIds = [
                "com.vwatek.apply.pro.monthly",
                "com.vwatek.apply.pro.yearly",
                "com.vwatek.apply.premium.monthly",
                "com.vwatek.apply.premium.yearly"
            ]
            products = try await Product.products(for: productIds)
        } catch {
            showError = "Failed to load products: \(error.localizedDescription)"
        }
    }
    
    private func purchaseProduct(tier: SubscriptionTier) async {
        let productId: String
        switch (tier, selectedBillingPeriod) {
        case (.pro, .monthly): productId = "com.vwatek.apply.pro.monthly"
        case (.pro, .yearly): productId = "com.vwatek.apply.pro.yearly"
        case (.premium, .monthly): productId = "com.vwatek.apply.premium.monthly"
        case (.premium, .yearly): productId = "com.vwatek.apply.premium.yearly"
        default: return
        }
        
        guard let product = products.first(where: { $0.id == productId }) else {
            showError = "Product not available. Please try again later."
            return
        }
        
        isPurchasing = true
        defer { isPurchasing = false }
        
        do {
            let result = try await product.purchase()
            
            switch result {
            case .success(let verification):
                switch verification {
                case .verified(let transaction):
                    // Finish the transaction
                    await transaction.finish()
                    // Refresh subscription state from the shared manager
                    await viewModel.refreshSubscription()
                case .unverified(_, let error):
                    showError = "Purchase verification failed: \(error.localizedDescription)"
                }
            case .userCancelled:
                // User cancelled, do nothing
                break
            case .pending:
                showError = "Purchase is pending approval."
            @unknown default:
                showError = "Unknown purchase result."
            }
        } catch {
            showError = "Purchase failed: \(error.localizedDescription)"
        }
    }
}

// MARK: - Supporting Types

enum SubscriptionTier: String {
    case free = "FREE"
    case pro = "PRO"
    case premium = "PREMIUM"
}

enum BillingPeriod {
    case monthly
    case yearly
}

// MARK: - Supporting Views

struct BillingPeriodButton: View {
    let title: String
    var subtitle: String? = nil
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Text(title)
                    .fontWeight(isSelected ? .bold : .regular)
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.caption2)
                        .foregroundColor(isSelected ? .white.opacity(0.8) : .accentColor)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(isSelected ? Color.accentColor : Color.clear)
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(8)
        }
    }
}

struct PricingCard: View {
    let tier: SubscriptionTier
    let billingPeriod: BillingPeriod
    let isCurrentPlan: Bool
    let isPopular: Bool
    let proMonthlyPrice: Double
    let proYearlyPrice: Double
    let premiumMonthlyPrice: Double
    let premiumYearlyPrice: Double
    let onSelect: () -> Void
    
    private var pricing: (monthly: Double, yearly: Double) {
        switch tier {
        case .free: return (0, 0)
        case .pro: return (proMonthlyPrice, proYearlyPrice)
        case .premium: return (premiumMonthlyPrice, premiumYearlyPrice)
        }
    }
    
    private var displayPrice: Double {
        switch billingPeriod {
        case .monthly: return pricing.monthly
        case .yearly: return pricing.yearly / 12
        }
    }
    
    private var limits: (resumes: String, coverLetters: String, ai: String, interviews: String, salaryInsights: Bool, negotiationCoach: Bool, linkedInOptimizer: Bool, prioritySupport: Bool) {
        switch tier {
        case .free: return ("3/mo", "3/mo", "5/day", "3/mo", false, false, false, false)
        case .pro: return ("10/mo", "10/mo", "20/day", "10/mo", true, false, false, false)
        case .premium: return ("Unlimited", "Unlimited", "Unlimited", "Unlimited", true, true, true, true)
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Popular badge
            if isPopular {
                Text("MOST POPULAR")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            
            // Tier name
            Text(tier.rawValue)
                .font(.title2)
                .fontWeight(.bold)
            
            // Price
            HStack(alignment: .bottom) {
                if tier == .free {
                    Text("Free")
                        .font(.title)
                        .fontWeight(.bold)
                } else {
                    Text("CA$")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(String(format: "%.2f", displayPrice))
                        .font(.title)
                        .fontWeight(.bold)
                    Text("/mo")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            if billingPeriod == .yearly && tier != .free {
                Text("Billed CA$\(String(format: "%.2f", pricing.yearly)) yearly")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Features
            VStack(alignment: .leading, spacing: 8) {
                FeatureItem(icon: "doc.text", text: "\(limits.resumes) resume versions")
                FeatureItem(icon: "envelope", text: "\(limits.coverLetters) cover letters")
                FeatureItem(icon: "sparkles", text: "\(limits.ai) AI enhancements")
                FeatureItem(icon: "person.wave.2", text: "\(limits.interviews) interview sessions")
                
                if limits.salaryInsights {
                    FeatureItem(icon: "dollarsign.circle", text: "Salary insights & benchmarks")
                }
                if limits.negotiationCoach {
                    FeatureItem(icon: "brain.head.profile", text: "AI negotiation coach")
                }
                if limits.linkedInOptimizer {
                    FeatureItem(icon: "person.crop.circle", text: "LinkedIn profile optimizer")
                }
                if limits.prioritySupport {
                    FeatureItem(icon: "star.circle", text: "Priority support")
                }
            }
            
            // CTA Button
            Button(action: onSelect) {
                Text(isCurrentPlan ? "Current Plan" : (tier == .free ? "Free Forever" : "Upgrade to \(tier.rawValue)"))
                    .fontWeight(.bold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isPopular && !isCurrentPlan ? Color.accentColor : Color(.systemGray5))
                    .foregroundColor(isPopular && !isCurrentPlan ? .white : .primary)
                    .cornerRadius(12)
            }
            .disabled(isCurrentPlan || tier == .free)
        }
        .padding(20)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(isPopular ? Color.accentColor : (isCurrentPlan ? Color.secondary : Color.clear), lineWidth: 2)
        )
        .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
    }
}

struct FeatureItem: View {
    let icon: String
    let text: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.accentColor)
                .frame(width: 20)
            Text(text)
                .font(.subheadline)
        }
    }
}

struct ComparisonRow: View {
    let feature: String
    let free: String
    let pro: String
    let premium: String
    
    var body: some View {
        HStack {
            Text(feature)
                .font(.caption)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(free)
                .font(.caption)
                .foregroundColor(free == "✗" ? .secondary : .primary)
                .frame(width: 50)
            Text(pro)
                .font(.caption)
                .foregroundColor(pro == "✓" ? .accentColor : .primary)
                .frame(width: 50)
            Text(premium)
                .font(.caption)
                .foregroundColor((premium == "✓" || premium == "∞") ? .accentColor : .primary)
                .fontWeight(premium == "∞" ? .bold : .regular)
                .frame(width: 60)
        }
    }
}

#Preview {
    SubscriptionView()
}
