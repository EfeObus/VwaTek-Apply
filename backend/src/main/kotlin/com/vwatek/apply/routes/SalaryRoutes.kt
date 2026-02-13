package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.*
import com.vwatek.apply.domain.model.*
import com.vwatek.apply.services.AIService
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
data class SalaryInsightsResponse(
    val insights: SalaryInsights,
    val chartData: SalaryChartData?
)

@Serializable
data class SalaryChartData(
    val ranges: List<SalaryRangeData>,
    val trend: List<SalaryTrendData>
)

@Serializable
data class SalaryRangeData(
    val label: String,
    val value: Double,
    val percentile: Int
)

@Serializable
data class SalaryTrendData(
    val year: Int,
    val median: Double
)

@Serializable
data class OfferEvaluationResponse(
    val evaluation: OfferEvaluation
)

@Serializable
data class NegotiationSessionResponse(
    val session: NegotiationSessionDto
)

@Serializable
data class NegotiationSessionDto(
    val id: String,
    val userId: String,
    val offerId: String,
    val status: String,
    val messages: List<NegotiationMessageDto>,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class NegotiationMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val suggestedResponse: String?,
    val timestamp: String
)

@Serializable
data class SendMessageRequest(
    val content: String
)

fun Route.salaryRoutes(httpClient: HttpClient) {
    val aiService = AIService(httpClient)
    
    route("/salary") {
        
        // Check access (premium feature gate)
        fun ApplicationCall.checkSalaryAccess(): Boolean {
            val userId = request.headers["X-User-Id"] ?: return false
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            val tier = subscription?.let { SubscriptionTier.valueOf(it[SubscriptionsTable.tier]) }
                ?: SubscriptionTier.FREE
            return FeatureLimits.forTier(tier).salaryInsightsAccess
        }
        
        // Get salary insights for a job
        post("/insights") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            if (!call.checkSalaryAccess()) {
                call.respond(HttpStatusCode.Forbidden, mapOf(
                    "error" to "Salary Intelligence requires PRO or PREMIUM subscription",
                    "requiredTier" to "PRO"
                ))
                return@post
            }
            
            val request = call.receive<SalaryInsightsRequest>()
            
            // Get salary data from database
            val salaryData = transaction {
                SalaryDataTable.select {
                    (SalaryDataTable.province eq request.province) and
                    (SalaryDataTable.jobTitle.lowerCase() like "%${request.jobTitle.lowercase()}%")
                }
                    .orderBy(SalaryDataTable.yearOfData, SortOrder.DESC)
                    .limit(10)
                    .toList()
            }
            
            // If no data, try by NOC code
            val nocSalaryData = if (salaryData.isEmpty() && request.nocCode != null) {
                transaction {
                    SalaryDataTable.select {
                        (SalaryDataTable.province eq request.province) and
                        (SalaryDataTable.nocCode eq request.nocCode)
                    }
                        .orderBy(SalaryDataTable.yearOfData, SortOrder.DESC)
                        .limit(10)
                        .toList()
                }
            } else salaryData
            
            val dataToUse = nocSalaryData.ifEmpty { salaryData }
            
            // Calculate insights
            val insights = if (dataToUse.isNotEmpty()) {
                val mostRecent = dataToUse.first()
                val medianSalary = mostRecent[SalaryDataTable.medianSalary].toDouble()
                val lowSalary = mostRecent[SalaryDataTable.lowSalary].toDouble()
                val highSalary = mostRecent[SalaryDataTable.highSalary].toDouble()
                val avgSalary = mostRecent[SalaryDataTable.averageSalary].toDouble()
                
                // Calculate percentile if current salary provided
                val percentile = request.currentSalary?.let { current ->
                    when {
                        current <= lowSalary -> ((current / lowSalary) * 10).toInt()
                        current <= medianSalary -> 10 + ((current - lowSalary) / (medianSalary - lowSalary) * 40).toInt()
                        current <= highSalary -> 50 + ((current - medianSalary) / (highSalary - medianSalary) * 40).toInt()
                        else -> 90 + minOf(10, ((current - highSalary) / highSalary * 10).toInt())
                    }
                }
                
                // Get related jobs
                val relatedJobs = transaction {
                    SalaryDataTable.select {
                        (SalaryDataTable.province eq request.province) and
                        (SalaryDataTable.id neq mostRecent[SalaryDataTable.id])
                    }
                        .limit(5)
                        .map { row ->
                            RelatedJobSalary(
                                jobTitle = row[SalaryDataTable.jobTitle],
                                nocCode = row[SalaryDataTable.nocCode],
                                medianSalary = row[SalaryDataTable.medianSalary].toDouble(),
                                salaryDifference = row[SalaryDataTable.medianSalary].toDouble() - medianSalary
                            )
                        }
                }
                
                // Determine market trend
                val trend = if (dataToUse.size > 1) {
                    val previousYear = dataToUse.getOrNull(1)
                    if (previousYear != null) {
                        val change = medianSalary - previousYear[SalaryDataTable.medianSalary].toDouble()
                        when {
                            change > medianSalary * 0.02 -> MarketTrend.INCREASING
                            change < -medianSalary * 0.02 -> MarketTrend.DECREASING
                            else -> MarketTrend.STABLE
                        }
                    } else MarketTrend.STABLE
                } else MarketTrend.STABLE
                
                // Generate AI recommendations
                val recommendations = generateSalaryRecommendations(
                    request, medianSalary, percentile, trend, aiService
                )
                
                SalaryInsights(
                    jobTitle = request.jobTitle,
                    nocCode = request.nocCode,
                    location = "${request.city ?: ""} ${request.province}".trim(),
                    medianSalary = medianSalary,
                    salaryRange = SalaryRange(
                        low = lowSalary,
                        median = medianSalary,
                        high = highSalary,
                        average = avgSalary
                    ),
                    percentile = percentile,
                    marketTrend = trend,
                    comparisonToProvincialAverage = 0.0, // Would calculate from provincial data
                    comparisonToNationalAverage = 0.0,   // Would calculate from national data
                    relatedJobSalaries = relatedJobs,
                    recommendations = recommendations,
                    dataFreshness = when {
                        mostRecent[SalaryDataTable.yearOfData] >= Clock.System.now().epochSeconds / 31536000 + 1970 -> DataFreshness.CURRENT
                        mostRecent[SalaryDataTable.yearOfData] >= Clock.System.now().epochSeconds / 31536000 + 1969 -> DataFreshness.RECENT
                        else -> DataFreshness.DATED
                    },
                    lastUpdated = mostRecent[SalaryDataTable.updatedAt]
                )
            } else {
                // No data - generate AI-based estimate
                val aiInsights = generateAISalaryEstimate(request, aiService)
                aiInsights
            }
            
            // Save to history
            transaction {
                SalaryComparisonHistoryTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[SalaryComparisonHistoryTable.userId] = userId
                    it[jobTitle] = request.jobTitle
                    it[nocCode] = request.nocCode
                    it[province] = request.province
                    it[city] = request.city
                    it[currentSalary] = request.currentSalary?.toBigDecimal()
                    it[yearsExperience] = request.yearsExperience
                    it[insightsJson] = json.encodeToString(SalaryInsights.serializer(), insights)
                    it[createdAt] = Clock.System.now()
                }
            }
            
            // Chart data
            val chartData = SalaryChartData(
                ranges = listOf(
                    SalaryRangeData("10th Percentile", insights.salaryRange.low, 10),
                    SalaryRangeData("25th Percentile", insights.salaryRange.low + (insights.salaryRange.median - insights.salaryRange.low) * 0.5, 25),
                    SalaryRangeData("Median", insights.salaryRange.median, 50),
                    SalaryRangeData("75th Percentile", insights.salaryRange.median + (insights.salaryRange.high - insights.salaryRange.median) * 0.5, 75),
                    SalaryRangeData("90th Percentile", insights.salaryRange.high, 90)
                ),
                trend = emptyList() // Would populate from historical data
            )
            
            call.respond(SalaryInsightsResponse(insights = insights, chartData = chartData))
        }
        
        // Get salary history for user
        get("/history") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@get
            }
            
            val history = transaction {
                SalaryComparisonHistoryTable.select { SalaryComparisonHistoryTable.userId eq userId }
                    .orderBy(SalaryComparisonHistoryTable.createdAt, SortOrder.DESC)
                    .limit(20)
                    .map { row ->
                        mapOf(
                            "id" to row[SalaryComparisonHistoryTable.id],
                            "jobTitle" to row[SalaryComparisonHistoryTable.jobTitle],
                            "province" to row[SalaryComparisonHistoryTable.province],
                            "city" to row[SalaryComparisonHistoryTable.city],
                            "createdAt" to row[SalaryComparisonHistoryTable.createdAt].toString()
                        )
                    }
            }
            
            call.respond(mapOf("history" to history))
        }
        
        // Evaluate job offer
        post("/evaluate-offer") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            if (!call.checkSalaryAccess()) {
                call.respond(HttpStatusCode.Forbidden, mapOf(
                    "error" to "Offer Evaluation requires PRO or PREMIUM subscription",
                    "requiredTier" to "PRO"
                ))
                return@post
            }
            
            val request = call.receive<EvaluateOfferRequest>()
            val offer = request.offer
            
            // Get market data
            val marketData = transaction {
                SalaryDataTable.select {
                    (SalaryDataTable.province eq offer.province) and
                    (SalaryDataTable.jobTitle.lowerCase() like "%${offer.jobTitle.lowercase()}%")
                }
                    .orderBy(SalaryDataTable.yearOfData, SortOrder.DESC)
                    .firstOrNull()
            }
            
            val marketMedian = marketData?.get(SalaryDataTable.medianSalary)?.toDouble() ?: offer.baseSalary
            
            // Calculate percentile
            val salaryPercentile = if (marketData != null) {
                val low = marketData[SalaryDataTable.lowSalary].toDouble()
                val high = marketData[SalaryDataTable.highSalary].toDouble()
                when {
                    offer.baseSalary <= low -> 10
                    offer.baseSalary <= marketMedian -> 10 + ((offer.baseSalary - low) / (marketMedian - low) * 40).toInt()
                    offer.baseSalary <= high -> 50 + ((offer.baseSalary - marketMedian) / (high - marketMedian) * 40).toInt()
                    else -> 90
                }
            } else 50
            
            val comparisonToMarket = ((offer.baseSalary - marketMedian) / marketMedian) * 100
            
            // Calculate total compensation
            val estimatedBonus = offer.annualBonus?.let { offer.baseSalary * (it / 100) } ?: 0.0
            val estimatedStockValue = offer.stockOptions?.let { stock ->
                val vestedPerYear = stock.numberOfShares / (stock.vestingPeriodMonths / 12.0)
                val shareValue = stock.currentSharePrice ?: (stock.strikePrice ?: 0.0)
                vestedPerYear * maxOf(0.0, shareValue - (stock.strikePrice ?: 0.0))
            } ?: 0.0
            
            // Estimate benefits value (rough estimate)
            val benefitsValue = offer.benefits?.let { benefits ->
                var value = 0.0
                if (benefits.healthInsurance) value += 5000
                if (benefits.dentalInsurance) value += 1500
                if (benefits.visionInsurance) value += 500
                if (benefits.lifeInsurance) value += 1000
                benefits.rrspMatching?.let { value += offer.baseSalary * (it / 100) }
                benefits.professionalDevelopmentBudget?.let { value += it }
                value
            } ?: 0.0
            
            val totalFirstYear = offer.baseSalary + (offer.signingBonus ?: 0.0) + estimatedBonus + estimatedStockValue + benefitsValue
            val totalAnnualized = offer.baseSalary + estimatedBonus + estimatedStockValue + benefitsValue
            
            // Rating
            val rating = when {
                comparisonToMarket >= 20 -> OfferRating.EXCELLENT
                comparisonToMarket >= 5 -> OfferRating.GOOD
                comparisonToMarket >= -5 -> OfferRating.FAIR
                comparisonToMarket >= -15 -> OfferRating.BELOW_MARKET
                else -> OfferRating.POOR
            }
            
            // Generate negotiation opportunities with AI
            val negotiationOps = generateNegotiationOpportunities(offer, marketMedian, salaryPercentile, aiService)
            
            // Strengths and concerns
            val strengths = mutableListOf<String>()
            val concerns = mutableListOf<String>()
            
            if (comparisonToMarket > 0) strengths.add("Base salary ${comparisonToMarket.toInt()}% above market median")
            else if (comparisonToMarket < -5) concerns.add("Base salary ${(-comparisonToMarket).toInt()}% below market median")
            
            offer.signingBonus?.let { if (it > 0) strengths.add("Signing bonus of $${it.toInt()}") }
            offer.benefits?.let { benefits ->
                if (benefits.healthInsurance && benefits.dentalInsurance) strengths.add("Comprehensive health coverage")
                benefits.rrspMatching?.let { if (it >= 5) strengths.add("RRSP matching at ${it.toInt()}%") }
                benefits.paidTimeOffDays?.let { if (it >= 20) strengths.add("Generous PTO ($it days)") }
                benefits.paidTimeOffDays?.let { if (it < 15) concerns.add("Limited PTO ($it days)") }
            }
            
            if (offer.stockOptions == null && offer.annualBonus == null) {
                concerns.add("No equity or bonus compensation")
            }
            
            // AI recommendation
            val recommendation = generateOfferRecommendation(offer, rating, strengths, concerns, aiService)
            
            val evaluation = OfferEvaluation(
                offer = offer,
                marketAnalysis = MarketAnalysis(
                    marketMedian = marketMedian,
                    salaryPercentile = salaryPercentile,
                    comparisonToMarket = comparisonToMarket,
                    demandLevel = DemandLevel.MODERATE,
                    marketOutlook = MarketTrend.STABLE
                ),
                totalCompensation = TotalCompensation(
                    baseSalary = offer.baseSalary,
                    estimatedBonus = estimatedBonus,
                    estimatedStockValue = estimatedStockValue,
                    benefitsValue = benefitsValue,
                    totalFirstYear = totalFirstYear,
                    totalAnnualized = totalAnnualized
                ),
                overallRating = rating,
                strengths = strengths,
                concerns = concerns,
                negotiationOpportunities = negotiationOps,
                recommendation = recommendation
            )
            
            // Save offer to database
            transaction {
                JobOffersTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[JobOffersTable.userId] = userId
                    it[jobTitle] = offer.jobTitle
                    it[company] = offer.company
                    it[nocCode] = offer.nocCode
                    it[baseSalary] = offer.baseSalary.toBigDecimal()
                    it[signingBonus] = offer.signingBonus?.toBigDecimal()
                    it[annualBonus] = offer.annualBonus?.toBigDecimal()
                    it[stockOptionsJson] = offer.stockOptions?.let { json.encodeToString(StockOptions.serializer(), it) }
                    it[benefitsJson] = offer.benefits?.let { json.encodeToString(Benefits.serializer(), it) }
                    it[province] = offer.province
                    it[city] = offer.city
                    it[isRemote] = offer.isRemote
                    it[yearsExperienceRequired] = offer.yearsExperienceRequired
                    it[evaluationJson] = json.encodeToString(OfferEvaluation.serializer(), evaluation)
                    it[status] = "PENDING"
                    it[createdAt] = Clock.System.now()
                    it[updatedAt] = Clock.System.now()
                }
            }
            
            call.respond(OfferEvaluationResponse(evaluation = evaluation))
        }
        
        // Get user's saved offers
        get("/offers") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@get
            }
            
            val offers = transaction {
                JobOffersTable.select { JobOffersTable.userId eq userId }
                    .orderBy(JobOffersTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        mapOf(
                            "id" to row[JobOffersTable.id],
                            "jobTitle" to row[JobOffersTable.jobTitle],
                            "company" to row[JobOffersTable.company],
                            "baseSalary" to row[JobOffersTable.baseSalary].toDouble(),
                            "status" to row[JobOffersTable.status],
                            "createdAt" to row[JobOffersTable.createdAt].toString()
                        )
                    }
            }
            
            call.respond(mapOf("offers" to offers))
        }
        
        // Update offer status
        put("/offers/{id}/status") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@put
            }
            
            val offerId = call.parameters["id"]
            if (offerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Offer ID required"))
                return@put
            }
            
            @Serializable
            data class UpdateStatusRequest(val status: String)
            val request = call.receive<UpdateStatusRequest>()
            
            transaction {
                JobOffersTable.update({ (JobOffersTable.id eq offerId) and (JobOffersTable.userId eq userId) }) {
                    it[status] = request.status
                    it[updatedAt] = Clock.System.now()
                }
            }
            
            call.respond(mapOf("message" to "Status updated"))
        }
    }
    
    // Negotiation Coach routes (Premium feature)
    route("/negotiation") {
        
        fun ApplicationCall.checkNegotiationAccess(): Boolean {
            val userId = request.headers["X-User-Id"] ?: return false
            val subscription = transaction {
                SubscriptionsTable.select { SubscriptionsTable.userId eq userId }
                    .singleOrNull()
            }
            val tier = subscription?.let { SubscriptionTier.valueOf(it[SubscriptionsTable.tier]) }
                ?: SubscriptionTier.FREE
            return FeatureLimits.forTier(tier).negotiationCoachAccess
        }
        
        // Start negotiation session
        post("/sessions") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            if (!call.checkNegotiationAccess()) {
                call.respond(HttpStatusCode.Forbidden, mapOf(
                    "error" to "Negotiation Coach requires PREMIUM subscription",
                    "requiredTier" to "PREMIUM"
                ))
                return@post
            }
            
            @Serializable
            data class CreateSessionRequest(val offerId: String)
            val request = call.receive<CreateSessionRequest>()
            
            // Verify offer exists and belongs to user
            val offer = transaction {
                JobOffersTable.select {
                    (JobOffersTable.id eq request.offerId) and (JobOffersTable.userId eq userId)
                }.singleOrNull()
            }
            
            if (offer == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Offer not found"))
                return@post
            }
            
            val now = Clock.System.now()
            val sessionId = UUID.randomUUID().toString()
            
            // Create session
            transaction {
                NegotiationSessionsTable.insert {
                    it[id] = sessionId
                    it[NegotiationSessionsTable.userId] = userId
                    it[offerId] = request.offerId
                    it[status] = "ACTIVE"
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                
                // Add initial system message
                val evaluation = offer[JobOffersTable.evaluationJson]?.let {
                    json.decodeFromString(OfferEvaluation.serializer(), it)
                }
                
                val initialMessage = buildString {
                    append("Welcome to your Negotiation Coach session! I'm here to help you negotiate the best possible offer for the ${offer[JobOffersTable.jobTitle]} position at ${offer[JobOffersTable.company]}.\n\n")
                    evaluation?.let { eval ->
                        append("Based on my analysis, your offer is rated as ${eval.overallRating.name}. ")
                        if (eval.negotiationOpportunities.isNotEmpty()) {
                            append("I've identified ${eval.negotiationOpportunities.size} potential areas for negotiation.\n\n")
                            append("Let's start by discussing your priorities. What aspects of this offer are most important to you?")
                        }
                    } ?: append("Let's discuss your offer and identify the best negotiation strategy. What questions do you have?")
                }
                
                NegotiationMessagesTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[NegotiationMessagesTable.sessionId] = sessionId
                    it[role] = "COACH"
                    it[content] = initialMessage
                    it[timestamp] = now
                }
            }
            
            // Return session
            val messages = transaction {
                NegotiationMessagesTable.select { NegotiationMessagesTable.sessionId eq sessionId }
                    .orderBy(NegotiationMessagesTable.timestamp, SortOrder.ASC)
                    .map { row ->
                        NegotiationMessageDto(
                            id = row[NegotiationMessagesTable.id],
                            role = row[NegotiationMessagesTable.role],
                            content = row[NegotiationMessagesTable.content],
                            suggestedResponse = row[NegotiationMessagesTable.suggestedResponse],
                            timestamp = row[NegotiationMessagesTable.timestamp].toString()
                        )
                    }
            }
            
            call.respond(NegotiationSessionResponse(
                session = NegotiationSessionDto(
                    id = sessionId,
                    userId = userId,
                    offerId = request.offerId,
                    status = "ACTIVE",
                    messages = messages,
                    createdAt = now.toString(),
                    updatedAt = now.toString()
                )
            ))
        }
        
        // Get session
        get("/sessions/{id}") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@get
            }
            
            val sessionId = call.parameters["id"]
            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID required"))
                return@get
            }
            
            val session = transaction {
                NegotiationSessionsTable.select {
                    (NegotiationSessionsTable.id eq sessionId) and (NegotiationSessionsTable.userId eq userId)
                }.singleOrNull()
            }
            
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@get
            }
            
            val messages = transaction {
                NegotiationMessagesTable.select { NegotiationMessagesTable.sessionId eq sessionId }
                    .orderBy(NegotiationMessagesTable.timestamp, SortOrder.ASC)
                    .map { row ->
                        NegotiationMessageDto(
                            id = row[NegotiationMessagesTable.id],
                            role = row[NegotiationMessagesTable.role],
                            content = row[NegotiationMessagesTable.content],
                            suggestedResponse = row[NegotiationMessagesTable.suggestedResponse],
                            timestamp = row[NegotiationMessagesTable.timestamp].toString()
                        )
                    }
            }
            
            call.respond(NegotiationSessionResponse(
                session = NegotiationSessionDto(
                    id = session[NegotiationSessionsTable.id],
                    userId = userId,
                    offerId = session[NegotiationSessionsTable.offerId],
                    status = session[NegotiationSessionsTable.status],
                    messages = messages,
                    createdAt = session[NegotiationSessionsTable.createdAt].toString(),
                    updatedAt = session[NegotiationSessionsTable.updatedAt].toString()
                )
            ))
        }
        
        // Send message to coach
        post("/sessions/{id}/messages") {
            val userId = call.request.headers["X-User-Id"]
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
                return@post
            }
            
            if (!call.checkNegotiationAccess()) {
                call.respond(HttpStatusCode.Forbidden, mapOf(
                    "error" to "Negotiation Coach requires PREMIUM subscription",
                    "requiredTier" to "PREMIUM"
                ))
                return@post
            }
            
            val sessionId = call.parameters["id"]
            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID required"))
                return@post
            }
            
            val request = call.receive<SendMessageRequest>()
            
            // Verify session
            val session = transaction {
                NegotiationSessionsTable.select {
                    (NegotiationSessionsTable.id eq sessionId) and (NegotiationSessionsTable.userId eq userId)
                }.singleOrNull()
            }
            
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@post
            }
            
            // Get offer details for context
            val offer = transaction {
                JobOffersTable.select { JobOffersTable.id eq session[NegotiationSessionsTable.offerId] }
                    .singleOrNull()
            }
            
            // Get conversation history
            val history = transaction {
                NegotiationMessagesTable.select { NegotiationMessagesTable.sessionId eq sessionId }
                    .orderBy(NegotiationMessagesTable.timestamp, SortOrder.ASC)
                    .map { "${it[NegotiationMessagesTable.role]}: ${it[NegotiationMessagesTable.content]}" }
            }
            
            val now = Clock.System.now()
            
            // Save user message
            transaction {
                NegotiationMessagesTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[NegotiationMessagesTable.sessionId] = sessionId
                    it[role] = "USER"
                    it[content] = request.content
                    it[timestamp] = now
                }
            }
            
            // Generate AI response
            val coachResponse = try {
                val context = buildString {
                    append("You are a professional salary negotiation coach helping a job seeker. ")
                    append("Be supportive, strategic, and provide actionable advice. ")
                    offer?.let {
                        append("\n\nJob Details:\n")
                        append("- Position: ${it[JobOffersTable.jobTitle]} at ${it[JobOffersTable.company]}\n")
                        append("- Base Salary: $${it[JobOffersTable.baseSalary]}\n")
                        append("- Location: ${it[JobOffersTable.city] ?: ""} ${it[JobOffersTable.province]}\n")
                        it[JobOffersTable.evaluationJson]?.let { evalJson ->
                            val eval = json.decodeFromString(OfferEvaluation.serializer(), evalJson)
                            append("- Rating: ${eval.overallRating}\n")
                            append("- Market Comparison: ${eval.marketAnalysis.comparisonToMarket.toInt()}% vs median\n")
                        }
                    }
                    append("\n\nConversation History:\n${history.takeLast(10).joinToString("\n")}")
                    append("\n\nUser's latest message: ${request.content}")
                    append("\n\nProvide helpful negotiation coaching advice. If the user asks for a script or email, provide one. Keep responses concise but helpful.")
                }
                
                aiService.generateContent(context)
            } catch (e: Exception) {
                "I apologize, but I'm having trouble processing your request right now. Please try again, or let me know if you'd like to discuss a specific aspect of your negotiation."
            }
            
            // Save coach response
            transaction {
                NegotiationMessagesTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[NegotiationMessagesTable.sessionId] = sessionId
                    it[role] = "COACH"
                    it[content] = coachResponse
                    it[timestamp] = Clock.System.now()
                }
                
                NegotiationSessionsTable.update({ NegotiationSessionsTable.id eq sessionId }) {
                    it[updatedAt] = Clock.System.now()
                }
            }
            
            // Return updated session
            val messages = transaction {
                NegotiationMessagesTable.select { NegotiationMessagesTable.sessionId eq sessionId }
                    .orderBy(NegotiationMessagesTable.timestamp, SortOrder.ASC)
                    .map { row ->
                        NegotiationMessageDto(
                            id = row[NegotiationMessagesTable.id],
                            role = row[NegotiationMessagesTable.role],
                            content = row[NegotiationMessagesTable.content],
                            suggestedResponse = row[NegotiationMessagesTable.suggestedResponse],
                            timestamp = row[NegotiationMessagesTable.timestamp].toString()
                        )
                    }
            }
            
            call.respond(NegotiationSessionResponse(
                session = NegotiationSessionDto(
                    id = sessionId,
                    userId = userId,
                    offerId = session[NegotiationSessionsTable.offerId],
                    status = "ACTIVE",
                    messages = messages,
                    createdAt = session[NegotiationSessionsTable.createdAt].toString(),
                    updatedAt = Clock.System.now().toString()
                )
            ))
        }
    }
}

private suspend fun generateSalaryRecommendations(
    request: SalaryInsightsRequest,
    medianSalary: Double,
    percentile: Int?,
    trend: MarketTrend,
    aiService: AIService
): List<String> {
    return try {
        val prompt = buildString {
            append("Generate 3-5 brief, actionable salary recommendations for a ${request.jobTitle} in ${request.province}, Canada. ")
            append("Market median: $${medianSalary.toInt()}. ")
            percentile?.let { append("User's current salary is at the ${it}th percentile. ") }
            append("Market trend: ${trend.name}. ")
            request.yearsExperience?.let { append("Experience: $it years. ") }
            append("Provide practical advice in bullet points, no headers. Focus on Canadian job market specifics.")
        }
        
        val response = aiService.generateContent(prompt)
        response.split("\n")
            .filter { it.isNotBlank() }
            .map { it.trim().removePrefix("- ").removePrefix("• ") }
            .take(5)
    } catch (e: Exception) {
        listOf(
            "Research salary data for similar roles in your area",
            "Consider the total compensation package, not just base salary",
            "Document your achievements and unique value proposition",
            "Practice your negotiation talking points"
        )
    }
}

private suspend fun generateAISalaryEstimate(
    request: SalaryInsightsRequest,
    aiService: AIService
): SalaryInsights {
    val prompt = buildString {
        append("Estimate the salary range for a ${request.jobTitle} in ${request.province}, Canada. ")
        request.nocCode?.let { append("NOC code: $it. ") }
        request.city?.let { append("City: $it. ") }
        request.yearsExperience?.let { append("Years of experience: $it. ") }
        append("Provide realistic CAD salary estimates for 2024. ")
        append("Format: LOW|MEDIAN|HIGH (e.g., 50000|65000|85000)")
    }
    
    return try {
        val response = aiService.generateContent(prompt)
        val parts = response.replace(",", "").split("|").mapNotNull { it.trim().toDoubleOrNull() }
        
        val low = parts.getOrElse(0) { 50000.0 }
        val median = parts.getOrElse(1) { 70000.0 }
        val high = parts.getOrElse(2) { 90000.0 }
        
        SalaryInsights(
            jobTitle = request.jobTitle,
            nocCode = request.nocCode,
            location = "${request.city ?: ""} ${request.province}".trim(),
            medianSalary = median,
            salaryRange = SalaryRange(low = low, median = median, high = high, average = median),
            percentile = null,
            marketTrend = MarketTrend.STABLE,
            comparisonToProvincialAverage = 0.0,
            comparisonToNationalAverage = 0.0,
            relatedJobSalaries = emptyList(),
            recommendations = listOf(
                "This estimate is AI-generated; verify with current job postings",
                "Salary can vary significantly based on company size and benefits",
                "Consider negotiating based on your specific skills and experience"
            ),
            dataFreshness = DataFreshness.CURRENT,
            lastUpdated = Clock.System.now()
        )
    } catch (e: Exception) {
        SalaryInsights(
            jobTitle = request.jobTitle,
            nocCode = request.nocCode,
            location = "${request.city ?: ""} ${request.province}".trim(),
            medianSalary = 70000.0,
            salaryRange = SalaryRange(low = 50000.0, median = 70000.0, high = 95000.0, average = 70000.0),
            percentile = null,
            marketTrend = MarketTrend.STABLE,
            comparisonToProvincialAverage = 0.0,
            comparisonToNationalAverage = 0.0,
            relatedJobSalaries = emptyList(),
            recommendations = listOf("Unable to generate specific recommendations. Please try again."),
            dataFreshness = DataFreshness.DATED,
            lastUpdated = Clock.System.now()
        )
    }
}

private suspend fun generateNegotiationOpportunities(
    offer: JobOffer,
    marketMedian: Double,
    percentile: Int,
    aiService: AIService
): List<NegotiationOpportunity> {
    val opportunities = mutableListOf<NegotiationOpportunity>()
    
    // Base salary opportunity
    if (percentile < 60) {
        val targetIncrease = when {
            percentile < 30 -> 15
            percentile < 50 -> 10
            else -> 5
        }
        opportunities.add(
            NegotiationOpportunity(
                area = NegotiationArea.BASE_SALARY,
                currentValue = "$${offer.baseSalary.toInt()}",
                suggestedTarget = "$${(offer.baseSalary * (1 + targetIncrease / 100.0)).toInt()}",
                marketJustification = "Your offer is at the ${percentile}th percentile. Market median is $${marketMedian.toInt()}.",
                priority = if (percentile < 40) NegotiationPriority.HIGH else NegotiationPriority.MEDIUM
            )
        )
    }
    
    // Signing bonus if none offered
    if (offer.signingBonus == null || offer.signingBonus == 0.0) {
        opportunities.add(
            NegotiationOpportunity(
                area = NegotiationArea.SIGNING_BONUS,
                currentValue = "$0",
                suggestedTarget = "$${(offer.baseSalary * 0.05).toInt()} - $${(offer.baseSalary * 0.10).toInt()}",
                marketJustification = "Signing bonuses of 5-10% are common for professional roles",
                priority = NegotiationPriority.MEDIUM
            )
        )
    }
    
    // Remote work
    if (!offer.isRemote) {
        opportunities.add(
            NegotiationOpportunity(
                area = NegotiationArea.REMOTE_WORK,
                currentValue = "On-site",
                suggestedTarget = "Hybrid (2-3 days remote)",
                marketJustification = "Flexible work arrangements are increasingly standard",
                priority = NegotiationPriority.LOW
            )
        )
    }
    
    // PTO
    offer.benefits?.paidTimeOffDays?.let { days ->
        if (days < 20) {
            opportunities.add(
                NegotiationOpportunity(
                    area = NegotiationArea.PTO,
                    currentValue = "$days days",
                    suggestedTarget = "20-25 days",
                    marketJustification = "20+ days PTO is standard for professional roles in Canada",
                    priority = NegotiationPriority.MEDIUM
                )
            )
        }
    }
    
    return opportunities.take(4)
}

private suspend fun generateOfferRecommendation(
    offer: JobOffer,
    rating: OfferRating,
    strengths: List<String>,
    concerns: List<String>,
    aiService: AIService
): String {
    return when (rating) {
        OfferRating.EXCELLENT -> "This is an excellent offer that exceeds market rates. Consider accepting, but there may still be room for minor improvements in benefits or perks."
        OfferRating.GOOD -> "This is a solid offer above market median. You could accept as-is, or attempt modest negotiations on specific areas like signing bonus or PTO."
        OfferRating.FAIR -> "This offer is at market rate. It's reasonable but leaves room for negotiation. Focus on 1-2 key areas that matter most to you."
        OfferRating.BELOW_MARKET -> "This offer is below market median. We strongly recommend negotiating, especially on base salary. Prepare strong justification for your requested increase."
        OfferRating.POOR -> "This offer is significantly below market. Consider whether this role is right for you, and if you proceed, prepare assertive but professional negotiation."
    }
}
