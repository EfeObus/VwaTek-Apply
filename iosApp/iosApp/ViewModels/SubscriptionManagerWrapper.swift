import Foundation
import shared
import Combine

/// SubscriptionManagerWrapper bridges the shared Kotlin SubscriptionManager to SwiftUI.
/// Observes subscription state via FlowExtensions.watch() and provides Swift-friendly methods.
///
/// Note: Uses String-based tier names to avoid conflicts with local SubscriptionTier enums
/// defined in SubscriptionView.swift and PaywallView.swift for SwiftUI convenience.
@MainActor
class SubscriptionManagerWrapper: ObservableObject {
    private let manager: SubscriptionManager
    
    // Published state
    @Published var isLoading: Bool = true
    @Published var currentTierName: String = "FREE"
    @Published var isDemoMode: Bool = true
    @Published var errorMessage: String? = nil
    
    // Feature limits
    @Published var resumeVersionsPerMonth: Int = 3
    @Published var aiEnhancementsPerDay: Int = 2
    @Published var coverLettersPerMonth: Int = 5
    @Published var interviewSessionsPerMonth: Int = 3
    @Published var salaryInsightsAccess: Bool = false
    @Published var negotiationCoachAccess: Bool = false
    @Published var linkedInOptimizerAccess: Bool = false
    
    // Pricing (from shared SubscriptionPricing)
    @Published var proMonthlyPrice: Double = 14.99
    @Published var proYearlyPrice: Double = 149.99
    @Published var premiumMonthlyPrice: Double = 29.99
    @Published var premiumYearlyPrice: Double = 299.99
    
    // Closeable watcher
    private var subscriptionWatcher: Closeable? = nil
    
    init() {
        self.manager = KoinHelperKt.getSubscriptionManager()
        observeState()
        
        Task {
            await refreshSubscription()
        }
    }
    
    deinit {
        subscriptionWatcher?.close()
    }
    
    // MARK: - State Observation
    
    private func observeState() {
        subscriptionWatcher = FlowExtensionsKt.watch(
            manager.subscriptionState,
            block: { [weak self] state in
                guard let self = self else { return }
                
                if state is SubscriptionState.Loading {
                    self.isLoading = true
                    self.errorMessage = nil
                } else if let success = state as? SubscriptionState.Success {
                    self.isLoading = false
                    self.currentTierName = success.tier.name
                    self.isDemoMode = success.isDemoMode
                    self.errorMessage = nil
                    
                    // Update limits
                    let limits = success.limits
                    self.resumeVersionsPerMonth = Int(limits.resumeVersionsPerMonth)
                    self.aiEnhancementsPerDay = Int(limits.aiEnhancementsPerDay)
                    self.coverLettersPerMonth = Int(limits.coverLettersPerMonth)
                    self.interviewSessionsPerMonth = Int(limits.interviewSessionsPerMonth)
                    self.salaryInsightsAccess = limits.salaryInsightsAccess
                    self.negotiationCoachAccess = limits.negotiationCoachAccess
                    self.linkedInOptimizerAccess = limits.linkedInOptimizerAccess
                    
                    // Update pricing if available
                    if let pricing = success.pricing {
                        self.updatePricing(pricing)
                    }
                } else if let error = state as? SubscriptionState.Error {
                    self.isLoading = false
                    self.errorMessage = error.message
                }
            }
        )
    }
    
    private func updatePricing(_ pricing: shared.SubscriptionPricing) {
        // Set pricing based on tier
        if pricing.tier == shared.SubscriptionTier.pro {
            proMonthlyPrice = pricing.monthlyPriceCad
            proYearlyPrice = pricing.yearlyPriceCad
        } else if pricing.tier == shared.SubscriptionTier.premium {
            premiumMonthlyPrice = pricing.monthlyPriceCad
            premiumYearlyPrice = pricing.yearlyPriceCad
        }
    }
    
    // MARK: - Public Methods
    
    func refreshSubscription() async {
        do {
            try await manager.refreshSubscription()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func refreshUsage() async {
        do {
            try await manager.refreshUsage()
        } catch {
            // Usage refresh failure is non-critical
        }
    }
    
    /// Check if a feature is available using the shared SubscriptionManager
    func canUseSalaryInsights() -> Bool {
        return manager.canUseFeature(feature: .salaryInsights)
    }
    
    func canUseNegotiationCoach() -> Bool {
        return manager.canUseFeature(feature: .negotiationCoach)
    }
    
    func canUseLinkedInOptimizer() -> Bool {
        return manager.canUseFeature(feature: .linkedinOptimizer)
    }
    
    /// Get the demo mode message
    var demoModeMessage: String {
        return SubscriptionManager.companion.DEMO_MODE_MESSAGE
    }
    
    /// Check if manager reports demo mode
    var isInDemoMode: Bool {
        return manager.isInDemoMode
    }
    
    // MARK: - Checkout Session
    
    /// Create a Stripe checkout session for upgrading
    /// Returns the checkout URL on success
    func createCheckoutSession(tier: shared.SubscriptionTier, billingPeriod: shared.BillingPeriod) async throws -> String {
        // Use URL scheme deep links for mobile checkout callbacks
        let successUrl = "vwatekapply://checkout/success"
        let cancelUrl = "vwatekapply://checkout/cancel"
        
        // Call the manager's createCheckoutSession method which returns URL or throws
        return try await manager.createCheckoutSession(
            tier: tier,
            billingPeriod: billingPeriod,
            successUrl: successUrl,
            cancelUrl: cancelUrl
        )
    }
}
