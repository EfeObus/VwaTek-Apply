import Foundation
import shared
import Combine

/// Wrapper class to make Kotlin SalaryIntelligenceManager observable in SwiftUI
/// Bridges the shared module's StateFlow-based API to @Published properties for SwiftUI
@MainActor
class SalaryIntelligenceManagerWrapper: ObservableObject {
    private let manager: SalaryIntelligenceManager
    private let subscriptionManager: SubscriptionManager
    
    // MARK: - Published State
    
    // Insights state
    @Published var isLoading: Bool = false
    @Published var salaryInsights: SalaryInsights? = nil
    @Published var offerEvaluation: OfferEvaluation? = nil
    @Published var chartData: SalaryChartData? = nil
    @Published var insightsError: String? = nil
    
    // Offers state
    @Published var offers: [OfferListItem] = []
    @Published var isLoadingOffers: Bool = true
    @Published var offersError: String? = nil
    
    // Negotiation state
    @Published var negotiationSession: NegotiationSessionResponse? = nil
    @Published var isNegotiating: Bool = false
    @Published var negotiationError: String? = nil
    
    // Feature access
    @Published var hasAccess: Bool = false
    
    private var insightsWatcher: Closeable?
    private var offersWatcher: Closeable?
    private var negotiationWatcher: Closeable?
    
    init() {
        self.manager = KoinHelperKt.getSalaryIntelligenceManager()
        self.subscriptionManager = KoinHelperKt.getSubscriptionManager()
        
        // Check feature access
        hasAccess = subscriptionManager.canUseFeature(feature: .salaryInsights)
        
        observeState()
    }
    
    deinit {
        insightsWatcher?.close()
        offersWatcher?.close()
        negotiationWatcher?.close()
    }
    
    // MARK: - State Observation
    
    private func observeState() {
        // Watch insights state
        insightsWatcher = FlowExtensionsKt.watch(manager.insightsState) { [weak self] (state: Any?) in
            guard let self = self else { return }
            Task { @MainActor in
                if let idle = state as? SalaryInsightsState.Idle {
                    self.isLoading = false
                    self.salaryInsights = nil
                    self.offerEvaluation = nil
                    self.chartData = nil
                    self.insightsError = nil
                } else if let loading = state as? SalaryInsightsState.Loading {
                    self.isLoading = true
                    self.insightsError = nil
                } else if let success = state as? SalaryInsightsState.Success {
                    self.isLoading = false
                    self.salaryInsights = success.insights.insights
                    self.chartData = success.insights.chartData
                    self.offerEvaluation = nil
                    self.insightsError = nil
                } else if let evaluated = state as? SalaryInsightsState.OfferEvaluated {
                    self.isLoading = false
                    self.offerEvaluation = evaluated.evaluation.evaluation
                    self.insightsError = nil
                } else if let error = state as? SalaryInsightsState.Error {
                    self.isLoading = false
                    self.insightsError = error.message
                }
            }
        }
        
        // Watch offers state
        offersWatcher = FlowExtensionsKt.watch(manager.offersState) { [weak self] (state: Any?) in
            guard let self = self else { return }
            Task { @MainActor in
                if let loading = state as? OffersState.Loading {
                    self.isLoadingOffers = true
                    self.offersError = nil
                } else if let success = state as? OffersState.Success {
                    self.isLoadingOffers = false
                    self.offers = success.offers
                    self.offersError = nil
                } else if let error = state as? OffersState.Error {
                    self.isLoadingOffers = false
                    self.offersError = error.message
                }
            }
        }
        
        // Watch negotiation state
        negotiationWatcher = FlowExtensionsKt.watch(manager.negotiationState) { [weak self] (state: Any?) in
            guard let self = self else { return }
            Task { @MainActor in
                if let idle = state as? NegotiationState.Idle {
                    self.isNegotiating = false
                    self.negotiationSession = nil
                    self.negotiationError = nil
                } else if let loading = state as? NegotiationState.Loading {
                    self.isNegotiating = true
                    self.negotiationError = nil
                } else if let active = state as? NegotiationState.Active {
                    self.isNegotiating = false
                    self.negotiationSession = active.session
                    self.negotiationError = nil
                } else if let error = state as? NegotiationState.Error {
                    self.isNegotiating = false
                    self.negotiationError = error.message
                }
            }
        }
    }
    
    // MARK: - Actions
    
    func searchSalary(jobTitle: String, province: String, city: String? = nil, yearsExperience: Int? = nil) {
        Task {
            let yearsExp: KotlinInt? = yearsExperience.map { KotlinInt(integerLiteral: $0) }
            try? await manager.searchSalary(jobTitle: jobTitle, province: province, city: city, yearsExperience: yearsExp)
        }
    }
    
    func evaluateOffer(offer: JobOffer) {
        Task {
            try? await manager.evaluateOffer(offer: offer)
        }
    }
    
    func loadOffers() {
        Task {
            try? await manager.loadOffers()
        }
    }
    
    func startNegotiation(offerId: String) {
        Task {
            try? await manager.startNegotiation(offerId: offerId)
        }
    }
    
    func sendMessage(sessionId: String, message: String) {
        Task {
            try? await manager.sendMessage(sessionId: sessionId, message: message)
        }
    }
    
    func clearInsights() {
        manager.clearInsights()
    }
    
    func endNegotiation() {
        manager.endNegotiation()
    }
    
    func refreshAccess() {
        hasAccess = subscriptionManager.canUseFeature(feature: .salaryInsights)
    }
}
