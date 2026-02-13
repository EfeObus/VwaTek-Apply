package com.vwatek.apply.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Salary Intelligence Models for VwaTek Apply
 * Provides salary insights based on Statistics Canada data and market analysis
 */

/**
 * Salary data entry from Statistics Canada or market surveys
 */
@Serializable
data class SalaryData(
    val id: String,
    val nocCode: String,
    val jobTitle: String,
    val province: String,
    val city: String? = null,
    val medianSalary: Double,
    val lowSalary: Double,        // 10th percentile
    val highSalary: Double,       // 90th percentile
    val averageSalary: Double,     
    val sampleSize: Int,
    val dataSource: SalaryDataSource,
    val currency: String = "CAD",
    val yearOfData: Int,
    val quarterOfData: Int? = null,
    val updatedAt: Instant
)

/**
 * Source of salary data
 */
@Serializable
enum class SalaryDataSource {
    STATISTICS_CANADA,
    JOB_BANK_CANADA,
    MARKET_SURVEY,
    USER_REPORTED
}

/**
 * Salary comparison request
 */
@Serializable
data class SalaryComparisonRequest(
    val jobTitle: String,
    val nocCode: String? = null,
    val province: String,
    val city: String? = null,
    val currentSalary: Double? = null,
    val yearsExperience: Int? = null
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
    val percentile: Int?,             // User's current salary percentile (if provided)
    val marketTrend: MarketTrend,
    val comparisonToProvincialAverage: Double,  // Percentage difference
    val comparisonToNationalAverage: Double,    // Percentage difference
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
    val low: Double,      // 10th percentile
    val median: Double,   // 50th percentile
    val high: Double,     // 90th percentile
    val average: Double
)

/**
 * Market trend indicator
 */
@Serializable
enum class MarketTrend {
    INCREASING,
    STABLE,
    DECREASING
}

/**
 * Data freshness indicator
 */
@Serializable
enum class DataFreshness {
    CURRENT,      // Less than 6 months old
    RECENT,       // 6-12 months old
    DATED         // More than 12 months old
}

/**
 * Related job salary comparison
 */
@Serializable
data class RelatedJobSalary(
    val jobTitle: String,
    val nocCode: String,
    val medianSalary: Double,
    val salaryDifference: Double  // Difference from requested job
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
    val annualBonus: Double? = null,       // Target bonus percentage
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
    val rrspMatching: Double? = null,        // RRSP matching percentage
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
 * Market analysis for the offer
 */
@Serializable
data class MarketAnalysis(
    val marketMedian: Double,
    val salaryPercentile: Int,          // Where this offer falls
    val comparisonToMarket: Double,     // Percentage above/below market
    val demandLevel: DemandLevel,
    val marketOutlook: MarketTrend
)

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
 * Total compensation breakdown
 */
@Serializable
data class TotalCompensation(
    val baseSalary: Double,
    val estimatedBonus: Double,
    val estimatedStockValue: Double,
    val benefitsValue: Double,
    val totalFirstYear: Double,
    val totalAnnualized: Double       // After initial bonuses
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
 * Negotiation priority level
 */
@Serializable
enum class NegotiationPriority {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Negotiation coaching session
 */
@Serializable
data class NegotiationSession(
    val id: String,
    val userId: String,
    val offer: JobOffer,
    val evaluation: OfferEvaluation,
    val conversation: List<NegotiationMessage>,
    val status: NegotiationSessionStatus,
    val createdAt: Instant,
    val updatedAt: Instant
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
 * Message in negotiation coaching conversation
 */
@Serializable
data class NegotiationMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val suggestedResponse: String? = null,   // For coach suggestions
    val timestamp: Instant
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
 * Request for offer evaluation
 */
@Serializable
data class EvaluateOfferRequest(
    val offer: JobOffer
)

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
