package com.vwatek.apply.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Request for salary insights
 */
@Serializable
data class SalaryInsightsRequest(
    val jobTitle: String,
    val nocCode: String? = null,
    val province: String,
    val city: String? = null,
    val yearsExperience: Int? = null,
    val currentSalary: Double? = null
)

/**
 * Salary insights response
 */
@Serializable
data class SalaryInsights(
    val jobTitle: String,
    val nocCode: String?,
    val location: String,
    val medianSalary: Double,
    val salaryRange: SalaryRange,
    val percentile: Int?,
    val marketTrend: MarketTrend,
    val comparisonToProvincialAverage: Double,
    val comparisonToNationalAverage: Double,
    val relatedJobSalaries: List<RelatedJobSalary>,
    val recommendations: List<String>,
    val dataFreshness: DataFreshness,
    val lastUpdated: Instant
)

/**
 * Salary range with percentiles
 */
@Serializable
data class SalaryRange(
    val low: Double,
    val median: Double,
    val high: Double,
    val average: Double
)

/**
 * Market trend indicator
 */
@Serializable
enum class MarketTrend {
    INCREASING,
    STABLE,
    DECREASING,
    GROWING,
    DECLINING
}

/**
 * Data freshness indicator
 */
@Serializable
enum class DataFreshness {
    CURRENT,
    RECENT,
    DATED
}

/**
 * Related job salary comparison
 */
@Serializable
data class RelatedJobSalary(
    val jobTitle: String,
    val nocCode: String,
    val medianSalary: Double,
    val salaryDifference: Double
)

/**
 * Job offer for evaluation
 */
@Serializable
data class JobOffer(
    val jobTitle: String,
    val company: String,
    val nocCode: String? = null,
    val baseSalary: Double,
    val signingBonus: Double? = null,
    val annualBonus: Double? = null,
    val stockOptions: StockOptions? = null,
    val benefits: Benefits? = null,
    val province: String,
    val city: String? = null,
    val isRemote: Boolean = false,
    val yearsExperienceRequired: Int? = null
)

/**
 * Stock options details
 */
@Serializable
data class StockOptions(
    val numberOfShares: Int,
    val strikePrice: Double? = null,
    val vestingPeriodMonths: Int = 48,
    val cliffMonths: Int = 12,
    val currentSharePrice: Double? = null
)

/**
 * Benefits summary
 */
@Serializable
data class Benefits(
    val healthInsurance: Boolean = false,
    val dentalInsurance: Boolean = false,
    val visionInsurance: Boolean = false,
    val lifeInsurance: Boolean = false,
    val rrspMatching: Double? = null,
    val pensionPlan: Boolean = false,
    val paidTimeOffDays: Int? = null,
    val sickDays: Int? = null,
    val parentalLeaveWeeks: Int? = null,
    val remoteWorkAllowed: Boolean = false,
    val professionalDevelopmentBudget: Double? = null,
    val gymMembership: Boolean = false,
    val stockPurchasePlan: Boolean = false,
    val otherBenefits: List<String> = emptyList()
)

/**
 * Overall offer rating
 */
@Serializable
enum class OfferRating {
    EXCELLENT,
    GOOD,
    FAIR,
    BELOW_MARKET,
    POOR
}

/**
 * Negotiation priority level
 */
@Serializable
enum class NegotiationPriority {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Areas open for negotiation
 */
@Serializable
enum class NegotiationArea {
    BASE_SALARY,
    SIGNING_BONUS,
    ANNUAL_BONUS,
    STOCK_OPTIONS,
    PTO,
    REMOTE_WORK,
    START_DATE,
    TITLE,
    PROFESSIONAL_DEVELOPMENT,
    RELOCATION
}

/**
 * Negotiation opportunity
 */
@Serializable
data class NegotiationOpportunity(
    val area: NegotiationArea,
    val currentValue: String,
    val suggestedTarget: String,
    val marketJustification: String,
    val priority: NegotiationPriority
)

/**
 * Negotiation session status
 */
@Serializable
enum class NegotiationSessionStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

/**
 * Demand level for the job type
 */
@Serializable
enum class DemandLevel {
    HIGH,
    MODERATE,
    LOW
}

/**
 * Market analysis for the offer
 */
@Serializable
data class MarketAnalysis(
    val marketMedian: Double,
    val salaryPercentile: Int,
    val comparisonToMarket: Double,
    val demandLevel: DemandLevel,
    val marketOutlook: MarketTrend
)

/**
 * Total compensation breakdown
 */
@Serializable
data class TotalCompensation(
    val baseSalary: Double,
    val estimatedBonus: Double,
    val estimatedStockValue: Double,
    val benefitsValue: Double,
    val totalFirstYear: Double,
    val totalAnnualized: Double
)

/**
 * Offer evaluation result
 */
@Serializable
data class OfferEvaluation(
    val offer: JobOffer,
    val marketAnalysis: MarketAnalysis,
    val totalCompensation: TotalCompensation,
    val overallRating: OfferRating,
    val strengths: List<String>,
    val concerns: List<String>,
    val negotiationOpportunities: List<NegotiationOpportunity>,
    val recommendation: String
)

/**
 * Request for offer evaluation
 */
@Serializable
data class EvaluateOfferRequest(
    val offer: JobOffer
)

/**
 * Message role in conversation
 */
@Serializable
enum class MessageRole {
    USER,
    COACH,
    SYSTEM
}

/**
 * Salary comparison history entry
 */
@Serializable
data class SalaryComparisonHistory(
    val id: String,
    val userId: String,
    val request: SalaryInsightsRequest,
    val insights: SalaryInsights,
    val createdAt: Instant
)
