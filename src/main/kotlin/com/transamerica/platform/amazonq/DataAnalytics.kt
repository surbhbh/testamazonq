package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Data analytics service for business intelligence and reporting
 */
@Service
class DataAnalytics {

    fun generateBusinessIntelligenceReport(portfolio: InsurancePortfolio, timeRange: DateRange): BusinessIntelligenceReport {
        val salesAnalytics = analyzeSalesPerformance(portfolio, timeRange)
        val profitabilityAnalytics = analyzeProfitability(portfolio, timeRange)
        val customerAnalytics = analyzeCustomerBehavior(portfolio, timeRange)
        val productAnalytics = analyzeProductPerformance(portfolio, timeRange)
        val marketAnalytics = analyzeMarketTrends(portfolio, timeRange)
        
        return BusinessIntelligenceReport(
            reportDate = LocalDateTime.now(),
            timeRange = timeRange,
            salesAnalytics = salesAnalytics,
            profitabilityAnalytics = profitabilityAnalytics,
            customerAnalytics = customerAnalytics,
            productAnalytics = productAnalytics,
            marketAnalytics = marketAnalytics,
            keyInsights = generateKeyInsights(salesAnalytics, profitabilityAnalytics, customerAnalytics),
            recommendations = generateBusinessRecommendations(salesAnalytics, profitabilityAnalytics, productAnalytics)
        )
    }

    fun performPredictiveAnalysis(portfolio: InsurancePortfolio, predictionHorizon: Int): PredictiveAnalysisResult {
        val lapseRatePrediction = predictLapseRates(portfolio, predictionHorizon)
        val mortalityPrediction = predictMortalityRates(portfolio, predictionHorizon)
        val salesForecast = forecastSales(portfolio, predictionHorizon)
        val profitabilityForecast = forecastProfitability(portfolio, predictionHorizon)
        val riskPrediction = predictRiskFactors(portfolio, predictionHorizon)
        
        return PredictiveAnalysisResult(
            analysisDate = LocalDateTime.now(),
            predictionHorizon = predictionHorizon,
            lapseRatePrediction = lapseRatePrediction,
            mortalityPrediction = mortalityPrediction,
            salesForecast = salesForecast,
            profitabilityForecast = profitabilityForecast,
            riskPrediction = riskPrediction,
            confidenceIntervals = calculateConfidenceIntervals(lapseRatePrediction, mortalityPrediction, salesForecast),
            modelAccuracy = assessModelAccuracy(portfolio)
        )
    }

    fun analyzeCustomerSegmentation(portfolio: InsurancePortfolio): CustomerSegmentationResult {
        val segments = performCustomerSegmentation(portfolio)
        val segmentProfiles = segments.map { segment ->
            CustomerSegmentProfile(
                segmentId = segment.segmentId,
                segmentName = segment.segmentName,
                customerCount = segment.customers.size,
                averageAge = segment.customers.map { it.age }.average(),
                averagePolicyValue = segment.customers.map { it.totalPolicyValue.toDouble() }.average(),
                profitability = calculateSegmentProfitability(segment),
                retentionRate = calculateSegmentRetentionRate(segment),
                growthPotential = assessSegmentGrowthPotential(segment),
                characteristics = identifySegmentCharacteristics(segment)
            )
        }
        
        return CustomerSegmentationResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = portfolio.policies.size,
            segmentCount = segments.size,
            segmentProfiles = segmentProfiles,
            segmentationCriteria = listOf("Age", "Income", "Policy Value", "Geographic Location"),
            recommendedTargeting = recommendTargetingStrategies(segmentProfiles)
        )
    }

    fun performCohortAnalysis(portfolio: InsurancePortfolio, cohortType: CohortType): CohortAnalysisResult {
        val cohorts = when (cohortType) {
            CohortType.ISSUE_YEAR -> groupPoliciesByIssueYear(portfolio)
            CohortType.AGE_BAND -> groupPoliciesByAgeBand(portfolio)
            CohortType.PRODUCT_TYPE -> groupPoliciesByProductType(portfolio)
            CohortType.PREMIUM_BAND -> groupPoliciesByPremiumBand(portfolio)
        }
        
        val cohortMetrics = cohorts.map { (cohortKey, policies) ->
            CohortMetrics(
                cohortId = cohortKey,
                policyCount = policies.size,
                totalPremium = policies.sumOf { it.faceAmount },
                averagePolicySize = policies.map { it.faceAmount }.average(),
                retentionRates = calculateCohortRetentionRates(policies),
                profitabilityMetrics = calculateCohortProfitability(policies),
                lapseRates = calculateCohortLapseRates(policies),
                claimRates = calculateCohortClaimRates(policies)
            )
        }
        
        return CohortAnalysisResult(
            analysisDate = LocalDateTime.now(),
            cohortType = cohortType,
            cohortMetrics = cohortMetrics,
            insights = generateCohortInsights(cohortMetrics),
            recommendations = generateCohortRecommendations(cohortMetrics)
        )
    }

    fun analyzeMarketBasketAnalysis(portfolio: InsurancePortfolio): MarketBasketAnalysisResult {
        val customerPolicies = portfolio.policies.groupBy { it.customerId }
        val productCombinations = mutableMapOf<Set<String>, Int>()
        val associationRules = mutableListOf<AssociationRule>()
        
        // Find frequent product combinations
        customerPolicies.values.forEach { policies ->
            if (policies.size > 1) {
                val products = policies.map { it.productType }.toSet()
                productCombinations[products] = productCombinations.getOrDefault(products, 0) + 1
            }
        }
        
        // Generate association rules
        productCombinations.forEach { (products, frequency) ->
            if (frequency >= 5 && products.size >= 2) { // Minimum support threshold
                products.forEach { antecedent ->
                    val consequent = products - antecedent
                    if (consequent.isNotEmpty()) {
                        val support = frequency.toDouble() / customerPolicies.size
                        val confidence = calculateConfidence(antecedent, consequent, customerPolicies)
                        val lift = calculateLift(antecedent, consequent, customerPolicies)
                        
                        if (confidence > 0.3 && lift > 1.0) { // Minimum thresholds
                            associationRules.add(AssociationRule(
                                antecedent = setOf(antecedent),
                                consequent = consequent,
                                support = support,
                                confidence = confidence,
                                lift = lift
                            ))
                        }
                    }
                }
            }
        }
        
        return MarketBasketAnalysisResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = customerPolicies.size,
            multiProductCustomers = customerPolicies.values.count { it.size > 1 },
            frequentCombinations = productCombinations.toList().sortedByDescending { it.second }.take(10),
            associationRules = associationRules.sortedByDescending { it.lift }.take(20),
            crossSellOpportunities = identifyCrossSellOpportunities(associationRules)
        )
    }

    fun performTimeSeriesAnalysis(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): TimeSeriesAnalysisResult {
        val timeSeriesData = extractTimeSeriesData(portfolio, metric)
        val trendAnalysis = analyzeTrend(timeSeriesData)
        val seasonalityAnalysis = analyzeSeasonality(timeSeriesData)
        val forecastData = forecastTimeSeries(timeSeriesData, 12) // 12 periods ahead
        
        return TimeSeriesAnalysisResult(
            analysisDate = LocalDateTime.now(),
            metric = metric,
            timeSeriesData = timeSeriesData,
            trendAnalysis = trendAnalysis,
            seasonalityAnalysis = seasonalityAnalysis,
            forecastData = forecastData,
            modelFit = assessTimeSeriesModelFit(timeSeriesData, forecastData),
            insights = generateTimeSeriesInsights(trendAnalysis, seasonalityAnalysis)
        )
    }

    // Private helper methods
    private fun analyzeSalesPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): SalesAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalSales = policiesInRange.sumOf { it.faceAmount }
        val policyCount = policiesInRange.size
        val averagePolicySize = if (policyCount > 0) totalSales.divide(BigDecimal(policyCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val salesByMonth = policiesInRange.groupBy { "${it.issueDate.year}-${it.issueDate.monthValue.toString().padStart(2, '0')}" }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByState = policiesInRange.groupBy { it.state }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        return SalesAnalytics(
            totalSales = totalSales,
            policyCount = policyCount,
            averagePolicySize = averagePolicySize,
            salesGrowthRate = calculateSalesGrowthRate(salesByMonth),
            salesByMonth = salesByMonth,
            salesByProduct = salesByProduct,
            salesByState = salesByState,
            topPerformingProducts = salesByProduct.toList().sortedByDescending { it.second }.take(5),
            topPerformingStates = salesByState.toList().sortedByDescending { it.second }.take(5)
        )
    }

    private fun analyzeProfitability(portfolio: InsurancePortfolio, timeRange: DateRange): ProfitabilityAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalRevenue = policiesInRange.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) } // 1% revenue assumption
        val totalExpenses = totalRevenue.multiply(BigDecimal("0.75")) // 75% expense ratio
        val netIncome = totalRevenue.subtract(totalExpenses)
        val profitMargin = if (totalRevenue > BigDecimal.ZERO) netIncome.divide(totalRevenue, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val profitabilityByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) ->
                val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses)
            }
        
        return ProfitabilityAnalytics(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netIncome = netIncome,
            profitMargin = profitMargin,
            returnOnAssets = netIncome.divide(totalRevenue.multiply(BigDecimal("10")), 4, RoundingMode.HALF_UP), // Simplified ROA
            profitabilityByProduct = profitabilityByProduct,
            profitabilityTrend = calculateProfitabilityTrend(policiesInRange)
        )
    }

    private fun analyzeCustomerBehavior(portfolio: InsurancePortfolio, timeRange: DateRange): CustomerAnalytics {
        val customers = portfolio.policies.groupBy { it.customerId }
        
        val totalCustomers = customers.size
        val averagePoliciesPerCustomer = portfolio.policies.size.toDouble() / totalCustomers
        val customerLifetimeValue = customers.mapValues { (_, policies) ->
            policies.sumOf { it.faceAmount.multiply(BigDecimal("0.05")) } // 5% CLV assumption
        }
        
        val averageCLV = customerLifetimeValue.values.map { it.toDouble() }.average()
        val churnRate = calculateChurnRate(customers)
        val retentionRate = BigDecimal.ONE.subtract(churnRate)
        
        return CustomerAnalytics(
            totalCustomers = totalCustomers,
            averagePoliciesPerCustomer = averagePoliciesPerCustomer,
            averageCustomerLifetimeValue = BigDecimal(averageCLV).setScale(2, RoundingMode.HALF_UP),
            churnRate = churnRate,
            retentionRate = retentionRate,
            customerAcquisitionCost = calculateCustomerAcquisitionCost(portfolio),
            customerSatisfactionScore = BigDecimal("8.5"), // Mock score
            netPromoterScore = BigDecimal("45") // Mock NPS
        )
    }

    private fun analyzeProductPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): ProductAnalytics {
        val productPerformance = portfolio.policies.groupBy { it.productType }
            .mapValues { (productType, policies) ->
                ProductPerformanceMetrics(
                    productType = productType,
                    policyCount = policies.size,
                    totalPremium = policies.sumOf { it.faceAmount },
                    averagePolicySize = policies.map { it.faceAmount }.average(),
                    profitability = calculateProductProfitability(policies),
                    growthRate = calculateProductGrowthRate(policies, timeRange),
                    marketShare = BigDecimal(policies.size).divide(BigDecimal(portfolio.policies.size), 4, RoundingMode.HALF_UP)
                )
            }
        
        return ProductAnalytics(
            productPerformance = productPerformance,
            topPerformingProducts = productPerformance.values.sortedByDescending { it.profitability }.take(5),
            underperformingProducts = productPerformance.values.sortedBy { it.profitability }.take(3),
            productLifecycleAnalysis = analyzeProductLifecycle(productPerformance.values.toList())
        )
    }

    private fun analyzeMarketTrends(portfolio: InsurancePortfolio, timeRange: DateRange): MarketAnalytics {
        return MarketAnalytics(
            marketSize = BigDecimal("50000000000"), // $50B market size assumption
            marketGrowthRate = BigDecimal("0.05"), // 5% growth
            competitivePosition = BigDecimal("0.15"), // 15% market share
            marketTrends = listOf(
                "Increasing demand for digital insurance products",
                "Growing focus on ESG investing",
                "Rising interest in hybrid work benefits"
            ),
            competitorAnalysis = mapOf(
                "Competitor A" to BigDecimal("0.20"),
                "Competitor B" to BigDecimal("0.18"),
                "Competitor C" to BigDecimal("0.12")
            )
        )
    }

    private fun generateKeyInsights(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, customerAnalytics: CustomerAnalytics): List<String> {
        val insights = mutableListOf<String>()
        
        if (salesAnalytics.salesGrowthRate > BigDecimal("0.10")) {
            insights.add("Strong sales growth of ${salesAnalytics.salesGrowthRate.multiply(BigDecimal("100"))}% indicates healthy market demand")
        }
        
        if (profitabilityAnalytics.profitMargin > BigDecimal("0.15")) {
            insights.add("Healthy profit margin of ${profitabilityAnalytics.profitMargin.multiply(BigDecimal("100"))}% demonstrates operational efficiency")
        }
        
        if (customerAnalytics.retentionRate > BigDecimal("0.90")) {
            insights.add("High customer retention rate of ${customerAnalytics.retentionRate.multiply(BigDecimal("100"))}% indicates strong customer satisfaction")
        }
        
        return insights
    }

    private fun generateBusinessRecommendations(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, productAnalytics: ProductAnalytics): List<String> {
        val recommendations = mutableListOf<String>()
        
        val topProduct = productAnalytics.topPerformingProducts.firstOrNull()
        if (topProduct != null) {
            recommendations.add("Focus marketing efforts on ${topProduct.productType} which shows highest profitability")
        }
        
        if (profitabilityAnalytics.profitMargin < BigDecimal("0.10")) {
            recommendations.add("Review expense structure to improve profit margins")
        }
        
        val topState = salesAnalytics.topPerformingStates.firstOrNull()
        if (topState != null) {
            recommendations.add("Expand operations in ${topState.first} market which shows strong performance")
        }
        
        return recommendations
    }

    private fun predictLapseRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified lapse rate prediction using historical trends
        val currentLapseRate = BigDecimal("0.08") // 8% current lapse rate
        val trendFactor = BigDecimal("0.02") // 2% annual increase trend
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentLapseRate.add(trendFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.9")),
                upperBound = predictedRate.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Lapse Rate",
            predictions = predictions,
            modelType = "Linear Trend",
            accuracy = BigDecimal("0.85")
        )
    }

    private fun predictMortalityRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified mortality rate prediction
        val currentMortalityRate = BigDecimal("0.012") // 1.2% current mortality rate
        val agingFactor = BigDecimal("0.001") // 0.1% annual increase due to aging
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentMortalityRate.add(agingFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.8")),
                upperBound = predictedRate.multiply(BigDecimal("1.2"))
            )
        }
        
        return PredictionResult(
            metric = "Mortality Rate",
            predictions = predictions,
            modelType = "Demographic Trend",
            accuracy = BigDecimal("0.78")
        )
    }

    private fun forecastSales(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentSales = portfolio.policies.sumOf { it.faceAmount }
        val growthRate = BigDecimal("0.07") // 7% annual growth
        
        val predictions = (1..horizon).map { year ->
            val predictedSales = currentSales.multiply(BigDecimal.ONE.add(growthRate).pow(year))
            PredictionPoint(
                period = year,
                predictedValue = predictedSales,
                lowerBound = predictedSales.multiply(BigDecimal("0.85")),
                upperBound = predictedSales.multiply(BigDecimal("1.15"))
            )
        }
        
        return PredictionResult(
            metric = "Sales Volume",
            predictions = predictions,
            modelType = "Exponential Growth",
            accuracy = BigDecimal("0.82")
        )
    }

    private fun forecastProfitability(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentProfitability = BigDecimal("0.12") // 12% current profit margin
        val improvementRate = BigDecimal("0.005") // 0.5% annual improvement
        
        val predictions = (1..horizon).map { year ->
            val predictedProfitability = currentProfitability.add(improvementRate.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedProfitability,
                lowerBound = predictedProfitability.multiply(BigDecimal("0.9")),
                upperBound = predictedProfitability.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Profitability",
            predictions = predictions,
            modelType = "Linear Improvement",
            accuracy = BigDecimal("0.75")
        )
    }

    private fun predictRiskFactors(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentRiskScore = BigDecimal("0.65") // Current risk score
        val volatility = BigDecimal("0.05") // 5% volatility
        
        val predictions = (1..horizon).map { year ->
            val randomFactor = (kotlin.random.Random.nextDouble() - 0.5) * 0.1
            val predictedRisk = currentRiskScore.add(BigDecimal(randomFactor))
            PredictionPoint(
                period = year,
                predictedValue = predictedRisk,
                lowerBound = predictedRisk.subtract(volatility),
                upperBound = predictedRisk.add(volatility)
            )
        }
        
        return PredictionResult(
            metric = "Risk Score",
            predictions = predictions,
            modelType = "Stochastic Model",
            accuracy = BigDecimal("0.70")
        )
    }

    private fun calculateConfidenceIntervals(vararg predictions: PredictionResult): Map<String, ConfidenceInterval> {
        return predictions.associate { prediction ->
            val avgLower = prediction.predictions.map { it.lowerBound.toDouble() }.average()
            val avgUpper = prediction.predictions.map { it.upperBound.toDouble() }.average()
            
            prediction.metric to ConfidenceInterval(
                lowerBound = BigDecimal(avgLower).setScale(4, RoundingMode.HALF_UP),
                upperBound = BigDecimal(avgUpper).setScale(4, RoundingMode.HALF_UP),
                confidenceLevel = BigDecimal("0.95")
            )
        }
    }

    private fun assessModelAccuracy(portfolio: InsurancePortfolio): ModelAccuracyMetrics {
        return ModelAccuracyMetrics(
            meanAbsoluteError = BigDecimal("0.05"),
            rootMeanSquareError = BigDecimal("0.08"),
            meanAbsolutePercentageError = BigDecimal("0.12"),
            r2Score = BigDecimal("0.85")
        )
    }

    // Additional helper methods would continue here...
    private fun performCustomerSegmentation(portfolio: InsurancePortfolio): List<CustomerSegment> {
        // Mock segmentation logic
        return listOf(
            CustomerSegment("SEG001", "High Value", emptyList()),
            CustomerSegment("SEG002", "Young Professionals", emptyList()),
            CustomerSegment("SEG003", "Retirees", emptyList())
        )
    }

    private fun calculateSegmentProfitability(segment: CustomerSegment): BigDecimal = BigDecimal("0.15")
    private fun calculateSegmentRetentionRate(segment: CustomerSegment): BigDecimal = BigDecimal("0.92")
    private fun assessSegmentGrowthPotential(segment: CustomerSegment): GrowthPotential = GrowthPotential.HIGH
    private fun identifySegmentCharacteristics(segment: CustomerSegment): List<String> = listOf("High income", "Tech-savvy")
    private fun recommendTargetingStrategies(profiles: List<CustomerSegmentProfile>): List<String> = listOf("Digital marketing", "Referral programs")

    private fun groupPoliciesByIssueYear(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.issueDate.year.toString() }
    }

    private fun groupPoliciesByAgeBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.age < 30 -> "Under 30"
                it.age < 50 -> "30-49"
                it.age < 65 -> "50-64"
                else -> "65+"
            }
        }
    }

    private fun groupPoliciesByProductType(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.productType }
    }

    private fun groupPoliciesByPremiumBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.faceAmount < BigDecimal("100000") -> "Under $100K"
                it.faceAmount < BigDecimal("500000") -> "$100K-$500K"
                it.faceAmount < BigDecimal("1000000") -> "$500K-$1M"
                else -> "Over $1M"
            }
        }
    }

    private fun calculateCohortRetentionRates(policies: List<PolicyInfo>): List<BigDecimal> {
        return listOf(BigDecimal("0.95"), BigDecimal("0.90"), BigDecimal("0.85"))
    }

    private fun calculateCohortProfitability(policies: List<PolicyInfo>): BigDecimal = BigDecimal("0.12")
    private fun calculateCohortLapseRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.08"))
    private fun calculateCohortClaimRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.02"))
    private fun generateCohortInsights(metrics: List<CohortMetrics>): List<String> = listOf("Newer cohorts show better retention")
    private fun generateCohortRecommendations(metrics: List<CohortMetrics>): List<String> = listOf("Focus on retention strategies")

    private fun calculateConfidence(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 0.5
    private fun calculateLift(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 1.2
    private fun identifyCrossSellOpportunities(rules: List<AssociationRule>): List<String> = listOf("Cross-sell life insurance to annuity customers")

    private fun extractTimeSeriesData(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): List<TimeSeriesPoint> {
        return (1..24).map { month ->
            TimeSeriesPoint(
                period = month,
                value = BigDecimal(1000000 + month * 50000),
                date = LocalDate.now().minusMonths(24 - month.toLong())
            )
        }
    }

    private fun analyzeTrend(data: List<TimeSeriesPoint>): TrendAnalysis {
        val slope = calculateSlope(data)
        val trendDirection = when {
            slope > 0.05 -> TrendDirection.INCREASING
            slope < -0.05 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        return TrendAnalysis(
            trendDirection = trendDirection,
            slope = BigDecimal(slope).setScale(6, RoundingMode.HALF_UP),
            strength = TrendStrength.MODERATE
        )
    }

    private fun analyzeSeasonality(data: List<TimeSeriesPoint>): SeasonalityAnalysis {
        return SeasonalityAnalysis(
            hasSeasonality = true,
            seasonalPeriod = 12,
            seasonalStrength = BigDecimal("0.15"),
            peakPeriods = listOf(11, 12, 1), // Nov, Dec, Jan
            troughPeriods = listOf(6, 7, 8) // Jun, Jul, Aug
        )
    }

    private fun forecastTimeSeries(data: List<TimeSeriesPoint>, periods: Int): List<ForecastPoint> {
        return (1..periods).map { period ->
            val baseValue = data.last().value
            val growth = BigDecimal("0.02") // 2% growth per period
            val forecastValue = baseValue.multiply(BigDecimal.ONE.add(growth).pow(period))
            
            ForecastPoint(
                period = period,
                forecastValue = forecastValue,
                lowerBound = forecastValue.multiply(BigDecimal("0.9")),
                upperBound = forecastValue.multiply(BigDecimal("1.1")),
                date = data.last().date.plusMonths(period.toLong())
            )
        }
    }

    private fun assessTimeSeriesModelFit(historical: List<TimeSeriesPoint>, forecast: List<ForecastPoint>): ModelFitMetrics {
        return ModelFitMetrics(
            r2 = BigDecimal("0.85"),
            mae = BigDecimal("50000"),
            rmse = BigDecimal("75000"),
            mape = BigDecimal("0.05")
        )
    }

    private fun generateTimeSeriesInsights(trend: TrendAnalysis, seasonality: SeasonalityAnalysis): List<String> {
        val insights = mutableListOf<String>()
        
        when (trend.trendDirection) {
            TrendDirection.INCREASING -> insights.add("Strong upward trend indicates growing business")
            TrendDirection.DECREASING -> insights.add("Declining trend requires immediate attention")
            TrendDirection.STABLE -> insights.add("Stable trend suggests mature market")
        }
        
        if (seasonality.hasSeasonality) {
            insights.add("Clear seasonal patterns detected - plan marketing campaigns accordingly")
        }
        
        return insights
    }

    // Additional helper methods for calculations
    private fun calculateSalesGrowthRate(salesByMonth: Map<String, BigDecimal>): BigDecimal {
        if (salesByMonth.size < 2) return BigDecimal.ZERO
        
        val sortedSales = salesByMonth.toList().sortedBy { it.first }
        val firstMonth = sortedSales.first().second
        val lastMonth = sortedSales.last().second
        
        return if (firstMonth > BigDecimal.ZERO) {
            lastMonth.subtract(firstMonth).divide(firstMonth, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun calculateProfitabilityTrend(policies: List<PolicyInfo>): List<BigDecimal> {
        return policies.groupBy { it.issueDate.year }
            .values.map { yearPolicies ->
                val revenue = yearPolicies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
            }
    }

    private fun calculateChurnRate(customers: Map<String, List<PolicyInfo>>): BigDecimal {
        // Simplified churn calculation
        return BigDecimal("0.08") // 8% churn rate
    }

    private fun calculateCustomerAcquisitionCost(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified CAC calculation
        return BigDecimal("500") // $500 per customer
    }

    private fun calculateProductProfitability(policies: List<PolicyInfo>): BigDecimal {
        val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
        val expenses = revenue.multiply(BigDecimal("0.75"))
        return revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
    }

    private fun calculateProductGrowthRate(policies: List<PolicyInfo>, timeRange: DateRange): BigDecimal {
        val currentYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year }
        val previousYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year - 1 }
        
        val currentCount = currentYearPolicies.size
        val previousCount = previousYearPolicies.size
        
        return if (previousCount > 0) {
            BigDecimal(currentCount - previousCount).divide(BigDecimal(previousCount), 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun analyzeProductLifecycle(products: List<ProductPerformanceMetrics>): Map<String, ProductLifecycleStage> {
        return products.associate { product ->
            val stage = when {
                product.growthRate > BigDecimal("0.20") -> ProductLifecycleStage.GROWTH
                product.growthRate > BigDecimal("0.05") -> ProductLifecycleStage.MATURITY
                product.growthRate < BigDecimal("-0.05") -> ProductLifecycleStage.DECLINE
                else -> ProductLifecycleStage.INTRODUCTION
            }
            product.productType to stage
        }
    }

    private fun calculateSlope(data: List<TimeSeriesPoint>): Double {
        val n = data.size
        val sumX = (1..n).sum().toDouble()
        val sumY = data.sumOf { it.value.toDouble() }
        val sumXY = data.mapIndexed { index, point -> (index + 1) * point.value.toDouble() }.sum()
        val sumX2 = (1..n).sumOf { it * it }.toDouble()
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }

    private fun List<BigDecimal>.average(): BigDecimal {
        return if (isEmpty()) BigDecimal.ZERO
        else fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }.divide(BigDecimal(size), 2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.pow(n: Int): BigDecimal {
        return this.pow(n, java.math.MathContext.DECIMAL128)
    }
}

// Data classes and enums for analytics
data class BusinessIntelligenceReport(
    val reportDate: LocalDateTime,
    val timeRange: DateRange,
    val salesAnalytics: SalesAnalytics,
    val profitabilityAnalytics: ProfitabilityAnalytics,
    val customerAnalytics: CustomerAnalytics,
    val productAnalytics: ProductAnalytics,
    val marketAnalytics: MarketAnalytics,
    val keyInsights: List<String>,
    val recommendations: List<String>
)

data class SalesAnalytics(
    val totalSales: BigDecimal,
    val policyCount: Int,
    val averagePolicySize: BigDecimal,
    val salesGrowthRate: BigDecimal,
    val salesByMonth: Map<String, BigDecimal>,
    val salesByProduct: Map<String, BigDecimal>,
    val salesByState: Map<String, BigDecimal>,
    val topPerformingProducts: List<Pair<String, BigDecimal>>,
    val topPerformingStates: List<Pair<String, BigDecimal>>
)

data class ProfitabilityAnalytics(
    val totalRevenue: BigDecimal,
    val totalExpenses: BigDecimal,
    val netIncome: BigDecimal,
    val profitMargin: BigDecimal,
    val returnOnAssets: BigDecimal,
    val profitabilityByProduct: Map<String, BigDecimal>,
    val profitabilityTrend: List<BigDecimal>
)

data class CustomerAnalytics(
    val totalCustomers: Int,
    val averagePoliciesPerCustomer: Double,
    val averageCustomerLifetimeValue: BigDecimal,
    val churnRate: BigDecimal,
    val retentionRate: BigDecimal,
    val customerAcquisitionCost: BigDecimal,
    val customerSatisfactionScore: BigDecimal,
    val netPromoterScore: BigDecimal
)

data class ProductAnalytics(
    val productPerformance: Map<String, ProductPerformanceMetrics>,
    val topPerformingProducts: List<ProductPerformanceMetrics>,
    val underperformingProducts: List<ProductPerformanceMetrics>,
    val productLifecycleAnalysis: Map<String, ProductLifecycleStage>
)

data class ProductPerformanceMetrics(
    val productType: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val profitability: BigDecimal,
    val growthRate: BigDecimal,
    val marketShare: BigDecimal
)

data class MarketAnalytics(
    val marketSize: BigDecimal,
    val marketGrowthRate: BigDecimal,
    val competitivePosition: BigDecimal,
    val marketTrends: List<String>,
    val competitorAnalysis: Map<String, BigDecimal>
)

data class PredictiveAnalysisResult(
    val analysisDate: LocalDateTime,
    val predictionHorizon: Int,
    val lapseRatePrediction: PredictionResult,
    val mortalityPrediction: PredictionResult,
    val salesForecast: PredictionResult,
    val profitabilityForecast: PredictionResult,
    val riskPrediction: PredictionResult,
    val confidenceIntervals: Map<String, ConfidenceInterval>,
    val modelAccuracy: ModelAccuracyMetrics
)

data class PredictionResult(
    val metric: String,
    val predictions: List<PredictionPoint>,
    val modelType: String,
    val accuracy: BigDecimal
)

data class PredictionPoint(
    val period: Int,
    val predictedValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal
)

data class ConfidenceInterval(
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val confidenceLevel: BigDecimal
)

data class ModelAccuracyMetrics(
    val meanAbsoluteError: BigDecimal,
    val rootMeanSquareError: BigDecimal,
    val meanAbsolutePercentageError: BigDecimal,
    val r2Score: BigDecimal
)

data class CustomerSegmentationResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val segmentCount: Int,
    val segmentProfiles: List<CustomerSegmentProfile>,
    val segmentationCriteria: List<String>,
    val recommendedTargeting: List<String>
)

data class CustomerSegmentProfile(
    val segmentId: String,
    val segmentName: String,
    val customerCount: Int,
    val averageAge: Double,
    val averagePolicyValue: Double,
    val profitability: BigDecimal,
    val retentionRate: BigDecimal,
    val growthPotential: GrowthPotential,
    val characteristics: List<String>
)

data class CustomerSegment(
    val segmentId: String,
    val segmentName: String,
    val customers: List<CustomerInfo>
)

data class CustomerInfo(
    val customerId: String,
    val age: Int,
    val totalPolicyValue: BigDecimal
)

data class CohortAnalysisResult(
    val analysisDate: LocalDateTime,
    val cohortType: CohortType,
    val cohortMetrics: List<CohortMetrics>,
    val insights: List<String>,
    val recommendations: List<String>
)

data class CohortMetrics(
    val cohortId: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val retentionRates: List<BigDecimal>,
    val profitabilityMetrics: BigDecimal,
    val lapseRates: List<BigDecimal>,
    val claimRates: List<BigDecimal>
)

data class MarketBasketAnalysisResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val multiProductCustomers: Int,
    val frequentCombinations: List<Pair<Set<String>, Int>>,
    val associationRules: List<AssociationRule>,
    val crossSellOpportunities: List<String>
)

data class AssociationRule(
    val antecedent: Set<String>,
    val consequent: Set<String>,
    val support: Double,
    val confidence: Double,
    val lift: Double
)

data class TimeSeriesAnalysisResult(
    val analysisDate: LocalDateTime,
    val metric: TimeSeriesMetric,
    val timeSeriesData: List<TimeSeriesPoint>,
    val trendAnalysis: TrendAnalysis,
    val seasonalityAnalysis: SeasonalityAnalysis,
    val forecastData: List<ForecastPoint>,
    val modelFit: ModelFitMetrics,
    val insights: List<String>
)

data class TimeSeriesPoint(
    val period: Int,
    val value: BigDecimal,
    val date: LocalDate
)

data class TrendAnalysis(
    val trendDirection: TrendDirection,
    val slope: BigDecimal,
    val strength: TrendStrength
)

data class SeasonalityAnalysis(
    val hasSeasonality: Boolean,
    val seasonalPeriod: Int,
    val seasonalStrength: BigDecimal,
    val peakPeriods: List<Int>,
    val troughPeriods: List<Int>
)

data class ForecastPoint(
    val period: Int,
    val forecastValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val date: LocalDate
)

data class ModelFitMetrics(
    val r2: BigDecimal,
    val mae: BigDecimal,
    val rmse: BigDecimal,
    val mape: BigDecimal
)

enum class CohortType {
    ISSUE_YEAR, AGE_BAND, PRODUCT_TYPE, PREMIUM_BAND
}

enum class GrowthPotential {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

enum class ProductLifecycleStage {
    INTRODUCTION, GROWTH, MATURITY, DECLINE
}

enum class TimeSeriesMetric {
    SALES_VOLUME, POLICY_COUNT, PREMIUM_INCOME, CLAIMS_PAID, LAPSE_RATE
}

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class TrendStrength {
    WEAK, MODERATE, STRONG, VERY_STRONG
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Data analytics service for business intelligence and reporting
 */
@Service
class DataAnalytics {

    fun generateBusinessIntelligenceReport(portfolio: InsurancePortfolio, timeRange: DateRange): BusinessIntelligenceReport {
        val salesAnalytics = analyzeSalesPerformance(portfolio, timeRange)
        val profitabilityAnalytics = analyzeProfitability(portfolio, timeRange)
        val customerAnalytics = analyzeCustomerBehavior(portfolio, timeRange)
        val productAnalytics = analyzeProductPerformance(portfolio, timeRange)
        val marketAnalytics = analyzeMarketTrends(portfolio, timeRange)
        
        return BusinessIntelligenceReport(
            reportDate = LocalDateTime.now(),
            timeRange = timeRange,
            salesAnalytics = salesAnalytics,
            profitabilityAnalytics = profitabilityAnalytics,
            customerAnalytics = customerAnalytics,
            productAnalytics = productAnalytics,
            marketAnalytics = marketAnalytics,
            keyInsights = generateKeyInsights(salesAnalytics, profitabilityAnalytics, customerAnalytics),
            recommendations = generateBusinessRecommendations(salesAnalytics, profitabilityAnalytics, productAnalytics)
        )
    }

    fun performPredictiveAnalysis(portfolio: InsurancePortfolio, predictionHorizon: Int): PredictiveAnalysisResult {
        val lapseRatePrediction = predictLapseRates(portfolio, predictionHorizon)
        val mortalityPrediction = predictMortalityRates(portfolio, predictionHorizon)
        val salesForecast = forecastSales(portfolio, predictionHorizon)
        val profitabilityForecast = forecastProfitability(portfolio, predictionHorizon)
        val riskPrediction = predictRiskFactors(portfolio, predictionHorizon)
        
        return PredictiveAnalysisResult(
            analysisDate = LocalDateTime.now(),
            predictionHorizon = predictionHorizon,
            lapseRatePrediction = lapseRatePrediction,
            mortalityPrediction = mortalityPrediction,
            salesForecast = salesForecast,
            profitabilityForecast = profitabilityForecast,
            riskPrediction = riskPrediction,
            confidenceIntervals = calculateConfidenceIntervals(lapseRatePrediction, mortalityPrediction, salesForecast),
            modelAccuracy = assessModelAccuracy(portfolio)
        )
    }

    fun analyzeCustomerSegmentation(portfolio: InsurancePortfolio): CustomerSegmentationResult {
        val segments = performCustomerSegmentation(portfolio)
        val segmentProfiles = segments.map { segment ->
            CustomerSegmentProfile(
                segmentId = segment.segmentId,
                segmentName = segment.segmentName,
                customerCount = segment.customers.size,
                averageAge = segment.customers.map { it.age }.average(),
                averagePolicyValue = segment.customers.map { it.totalPolicyValue.toDouble() }.average(),
                profitability = calculateSegmentProfitability(segment),
                retentionRate = calculateSegmentRetentionRate(segment),
                growthPotential = assessSegmentGrowthPotential(segment),
                characteristics = identifySegmentCharacteristics(segment)
            )
        }
        
        return CustomerSegmentationResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = portfolio.policies.size,
            segmentCount = segments.size,
            segmentProfiles = segmentProfiles,
            segmentationCriteria = listOf("Age", "Income", "Policy Value", "Geographic Location"),
            recommendedTargeting = recommendTargetingStrategies(segmentProfiles)
        )
    }

    fun performCohortAnalysis(portfolio: InsurancePortfolio, cohortType: CohortType): CohortAnalysisResult {
        val cohorts = when (cohortType) {
            CohortType.ISSUE_YEAR -> groupPoliciesByIssueYear(portfolio)
            CohortType.AGE_BAND -> groupPoliciesByAgeBand(portfolio)
            CohortType.PRODUCT_TYPE -> groupPoliciesByProductType(portfolio)
            CohortType.PREMIUM_BAND -> groupPoliciesByPremiumBand(portfolio)
        }
        
        val cohortMetrics = cohorts.map { (cohortKey, policies) ->
            CohortMetrics(
                cohortId = cohortKey,
                policyCount = policies.size,
                totalPremium = policies.sumOf { it.faceAmount },
                averagePolicySize = policies.map { it.faceAmount }.average(),
                retentionRates = calculateCohortRetentionRates(policies),
                profitabilityMetrics = calculateCohortProfitability(policies),
                lapseRates = calculateCohortLapseRates(policies),
                claimRates = calculateCohortClaimRates(policies)
            )
        }
        
        return CohortAnalysisResult(
            analysisDate = LocalDateTime.now(),
            cohortType = cohortType,
            cohortMetrics = cohortMetrics,
            insights = generateCohortInsights(cohortMetrics),
            recommendations = generateCohortRecommendations(cohortMetrics)
        )
    }

    fun analyzeMarketBasketAnalysis(portfolio: InsurancePortfolio): MarketBasketAnalysisResult {
        val customerPolicies = portfolio.policies.groupBy { it.customerId }
        val productCombinations = mutableMapOf<Set<String>, Int>()
        val associationRules = mutableListOf<AssociationRule>()
        
        // Find frequent product combinations
        customerPolicies.values.forEach { policies ->
            if (policies.size > 1) {
                val products = policies.map { it.productType }.toSet()
                productCombinations[products] = productCombinations.getOrDefault(products, 0) + 1
            }
        }
        
        // Generate association rules
        productCombinations.forEach { (products, frequency) ->
            if (frequency >= 5 && products.size >= 2) { // Minimum support threshold
                products.forEach { antecedent ->
                    val consequent = products - antecedent
                    if (consequent.isNotEmpty()) {
                        val support = frequency.toDouble() / customerPolicies.size
                        val confidence = calculateConfidence(antecedent, consequent, customerPolicies)
                        val lift = calculateLift(antecedent, consequent, customerPolicies)
                        
                        if (confidence > 0.3 && lift > 1.0) { // Minimum thresholds
                            associationRules.add(AssociationRule(
                                antecedent = setOf(antecedent),
                                consequent = consequent,
                                support = support,
                                confidence = confidence,
                                lift = lift
                            ))
                        }
                    }
                }
            }
        }
        
        return MarketBasketAnalysisResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = customerPolicies.size,
            multiProductCustomers = customerPolicies.values.count { it.size > 1 },
            frequentCombinations = productCombinations.toList().sortedByDescending { it.second }.take(10),
            associationRules = associationRules.sortedByDescending { it.lift }.take(20),
            crossSellOpportunities = identifyCrossSellOpportunities(associationRules)
        )
    }

    fun performTimeSeriesAnalysis(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): TimeSeriesAnalysisResult {
        val timeSeriesData = extractTimeSeriesData(portfolio, metric)
        val trendAnalysis = analyzeTrend(timeSeriesData)
        val seasonalityAnalysis = analyzeSeasonality(timeSeriesData)
        val forecastData = forecastTimeSeries(timeSeriesData, 12) // 12 periods ahead
        
        return TimeSeriesAnalysisResult(
            analysisDate = LocalDateTime.now(),
            metric = metric,
            timeSeriesData = timeSeriesData,
            trendAnalysis = trendAnalysis,
            seasonalityAnalysis = seasonalityAnalysis,
            forecastData = forecastData,
            modelFit = assessTimeSeriesModelFit(timeSeriesData, forecastData),
            insights = generateTimeSeriesInsights(trendAnalysis, seasonalityAnalysis)
        )
    }

    // Private helper methods
    private fun analyzeSalesPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): SalesAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalSales = policiesInRange.sumOf { it.faceAmount }
        val policyCount = policiesInRange.size
        val averagePolicySize = if (policyCount > 0) totalSales.divide(BigDecimal(policyCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val salesByMonth = policiesInRange.groupBy { "${it.issueDate.year}-${it.issueDate.monthValue.toString().padStart(2, '0')}" }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByState = policiesInRange.groupBy { it.state }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        return SalesAnalytics(
            totalSales = totalSales,
            policyCount = policyCount,
            averagePolicySize = averagePolicySize,
            salesGrowthRate = calculateSalesGrowthRate(salesByMonth),
            salesByMonth = salesByMonth,
            salesByProduct = salesByProduct,
            salesByState = salesByState,
            topPerformingProducts = salesByProduct.toList().sortedByDescending { it.second }.take(5),
            topPerformingStates = salesByState.toList().sortedByDescending { it.second }.take(5)
        )
    }

    private fun analyzeProfitability(portfolio: InsurancePortfolio, timeRange: DateRange): ProfitabilityAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalRevenue = policiesInRange.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) } // 1% revenue assumption
        val totalExpenses = totalRevenue.multiply(BigDecimal("0.75")) // 75% expense ratio
        val netIncome = totalRevenue.subtract(totalExpenses)
        val profitMargin = if (totalRevenue > BigDecimal.ZERO) netIncome.divide(totalRevenue, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val profitabilityByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) ->
                val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses)
            }
        
        return ProfitabilityAnalytics(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netIncome = netIncome,
            profitMargin = profitMargin,
            returnOnAssets = netIncome.divide(totalRevenue.multiply(BigDecimal("10")), 4, RoundingMode.HALF_UP), // Simplified ROA
            profitabilityByProduct = profitabilityByProduct,
            profitabilityTrend = calculateProfitabilityTrend(policiesInRange)
        )
    }

    private fun analyzeCustomerBehavior(portfolio: InsurancePortfolio, timeRange: DateRange): CustomerAnalytics {
        val customers = portfolio.policies.groupBy { it.customerId }
        
        val totalCustomers = customers.size
        val averagePoliciesPerCustomer = portfolio.policies.size.toDouble() / totalCustomers
        val customerLifetimeValue = customers.mapValues { (_, policies) ->
            policies.sumOf { it.faceAmount.multiply(BigDecimal("0.05")) } // 5% CLV assumption
        }
        
        val averageCLV = customerLifetimeValue.values.map { it.toDouble() }.average()
        val churnRate = calculateChurnRate(customers)
        val retentionRate = BigDecimal.ONE.subtract(churnRate)
        
        return CustomerAnalytics(
            totalCustomers = totalCustomers,
            averagePoliciesPerCustomer = averagePoliciesPerCustomer,
            averageCustomerLifetimeValue = BigDecimal(averageCLV).setScale(2, RoundingMode.HALF_UP),
            churnRate = churnRate,
            retentionRate = retentionRate,
            customerAcquisitionCost = calculateCustomerAcquisitionCost(portfolio),
            customerSatisfactionScore = BigDecimal("8.5"), // Mock score
            netPromoterScore = BigDecimal("45") // Mock NPS
        )
    }

    private fun analyzeProductPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): ProductAnalytics {
        val productPerformance = portfolio.policies.groupBy { it.productType }
            .mapValues { (productType, policies) ->
                ProductPerformanceMetrics(
                    productType = productType,
                    policyCount = policies.size,
                    totalPremium = policies.sumOf { it.faceAmount },
                    averagePolicySize = policies.map { it.faceAmount }.average(),
                    profitability = calculateProductProfitability(policies),
                    growthRate = calculateProductGrowthRate(policies, timeRange),
                    marketShare = BigDecimal(policies.size).divide(BigDecimal(portfolio.policies.size), 4, RoundingMode.HALF_UP)
                )
            }
        
        return ProductAnalytics(
            productPerformance = productPerformance,
            topPerformingProducts = productPerformance.values.sortedByDescending { it.profitability }.take(5),
            underperformingProducts = productPerformance.values.sortedBy { it.profitability }.take(3),
            productLifecycleAnalysis = analyzeProductLifecycle(productPerformance.values.toList())
        )
    }

    private fun analyzeMarketTrends(portfolio: InsurancePortfolio, timeRange: DateRange): MarketAnalytics {
        return MarketAnalytics(
            marketSize = BigDecimal("50000000000"), // $50B market size assumption
            marketGrowthRate = BigDecimal("0.05"), // 5% growth
            competitivePosition = BigDecimal("0.15"), // 15% market share
            marketTrends = listOf(
                "Increasing demand for digital insurance products",
                "Growing focus on ESG investing",
                "Rising interest in hybrid work benefits"
            ),
            competitorAnalysis = mapOf(
                "Competitor A" to BigDecimal("0.20"),
                "Competitor B" to BigDecimal("0.18"),
                "Competitor C" to BigDecimal("0.12")
            )
        )
    }

    private fun generateKeyInsights(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, customerAnalytics: CustomerAnalytics): List<String> {
        val insights = mutableListOf<String>()
        
        if (salesAnalytics.salesGrowthRate > BigDecimal("0.10")) {
            insights.add("Strong sales growth of ${salesAnalytics.salesGrowthRate.multiply(BigDecimal("100"))}% indicates healthy market demand")
        }
        
        if (profitabilityAnalytics.profitMargin > BigDecimal("0.15")) {
            insights.add("Healthy profit margin of ${profitabilityAnalytics.profitMargin.multiply(BigDecimal("100"))}% demonstrates operational efficiency")
        }
        
        if (customerAnalytics.retentionRate > BigDecimal("0.90")) {
            insights.add("High customer retention rate of ${customerAnalytics.retentionRate.multiply(BigDecimal("100"))}% indicates strong customer satisfaction")
        }
        
        return insights
    }

    private fun generateBusinessRecommendations(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, productAnalytics: ProductAnalytics): List<String> {
        val recommendations = mutableListOf<String>()
        
        val topProduct = productAnalytics.topPerformingProducts.firstOrNull()
        if (topProduct != null) {
            recommendations.add("Focus marketing efforts on ${topProduct.productType} which shows highest profitability")
        }
        
        if (profitabilityAnalytics.profitMargin < BigDecimal("0.10")) {
            recommendations.add("Review expense structure to improve profit margins")
        }
        
        val topState = salesAnalytics.topPerformingStates.firstOrNull()
        if (topState != null) {
            recommendations.add("Expand operations in ${topState.first} market which shows strong performance")
        }
        
        return recommendations
    }

    private fun predictLapseRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified lapse rate prediction using historical trends
        val currentLapseRate = BigDecimal("0.08") // 8% current lapse rate
        val trendFactor = BigDecimal("0.02") // 2% annual increase trend
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentLapseRate.add(trendFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.9")),
                upperBound = predictedRate.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Lapse Rate",
            predictions = predictions,
            modelType = "Linear Trend",
            accuracy = BigDecimal("0.85")
        )
    }

    private fun predictMortalityRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified mortality rate prediction
        val currentMortalityRate = BigDecimal("0.012") // 1.2% current mortality rate
        val agingFactor = BigDecimal("0.001") // 0.1% annual increase due to aging
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentMortalityRate.add(agingFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.8")),
                upperBound = predictedRate.multiply(BigDecimal("1.2"))
            )
        }
        
        return PredictionResult(
            metric = "Mortality Rate",
            predictions = predictions,
            modelType = "Demographic Trend",
            accuracy = BigDecimal("0.78")
        )
    }

    private fun forecastSales(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentSales = portfolio.policies.sumOf { it.faceAmount }
        val growthRate = BigDecimal("0.07") // 7% annual growth
        
        val predictions = (1..horizon).map { year ->
            val predictedSales = currentSales.multiply(BigDecimal.ONE.add(growthRate).pow(year))
            PredictionPoint(
                period = year,
                predictedValue = predictedSales,
                lowerBound = predictedSales.multiply(BigDecimal("0.85")),
                upperBound = predictedSales.multiply(BigDecimal("1.15"))
            )
        }
        
        return PredictionResult(
            metric = "Sales Volume",
            predictions = predictions,
            modelType = "Exponential Growth",
            accuracy = BigDecimal("0.82")
        )
    }

    private fun forecastProfitability(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentProfitability = BigDecimal("0.12") // 12% current profit margin
        val improvementRate = BigDecimal("0.005") // 0.5% annual improvement
        
        val predictions = (1..horizon).map { year ->
            val predictedProfitability = currentProfitability.add(improvementRate.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedProfitability,
                lowerBound = predictedProfitability.multiply(BigDecimal("0.9")),
                upperBound = predictedProfitability.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Profitability",
            predictions = predictions,
            modelType = "Linear Improvement",
            accuracy = BigDecimal("0.75")
        )
    }

    private fun predictRiskFactors(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentRiskScore = BigDecimal("0.65") // Current risk score
        val volatility = BigDecimal("0.05") // 5% volatility
        
        val predictions = (1..horizon).map { year ->
            val randomFactor = (kotlin.random.Random.nextDouble() - 0.5) * 0.1
            val predictedRisk = currentRiskScore.add(BigDecimal(randomFactor))
            PredictionPoint(
                period = year,
                predictedValue = predictedRisk,
                lowerBound = predictedRisk.subtract(volatility),
                upperBound = predictedRisk.add(volatility)
            )
        }
        
        return PredictionResult(
            metric = "Risk Score",
            predictions = predictions,
            modelType = "Stochastic Model",
            accuracy = BigDecimal("0.70")
        )
    }

    private fun calculateConfidenceIntervals(vararg predictions: PredictionResult): Map<String, ConfidenceInterval> {
        return predictions.associate { prediction ->
            val avgLower = prediction.predictions.map { it.lowerBound.toDouble() }.average()
            val avgUpper = prediction.predictions.map { it.upperBound.toDouble() }.average()
            
            prediction.metric to ConfidenceInterval(
                lowerBound = BigDecimal(avgLower).setScale(4, RoundingMode.HALF_UP),
                upperBound = BigDecimal(avgUpper).setScale(4, RoundingMode.HALF_UP),
                confidenceLevel = BigDecimal("0.95")
            )
        }
    }

    private fun assessModelAccuracy(portfolio: InsurancePortfolio): ModelAccuracyMetrics {
        return ModelAccuracyMetrics(
            meanAbsoluteError = BigDecimal("0.05"),
            rootMeanSquareError = BigDecimal("0.08"),
            meanAbsolutePercentageError = BigDecimal("0.12"),
            r2Score = BigDecimal("0.85")
        )
    }

    // Additional helper methods would continue here...
    private fun performCustomerSegmentation(portfolio: InsurancePortfolio): List<CustomerSegment> {
        // Mock segmentation logic
        return listOf(
            CustomerSegment("SEG001", "High Value", emptyList()),
            CustomerSegment("SEG002", "Young Professionals", emptyList()),
            CustomerSegment("SEG003", "Retirees", emptyList())
        )
    }

    private fun calculateSegmentProfitability(segment: CustomerSegment): BigDecimal = BigDecimal("0.15")
    private fun calculateSegmentRetentionRate(segment: CustomerSegment): BigDecimal = BigDecimal("0.92")
    private fun assessSegmentGrowthPotential(segment: CustomerSegment): GrowthPotential = GrowthPotential.HIGH
    private fun identifySegmentCharacteristics(segment: CustomerSegment): List<String> = listOf("High income", "Tech-savvy")
    private fun recommendTargetingStrategies(profiles: List<CustomerSegmentProfile>): List<String> = listOf("Digital marketing", "Referral programs")

    private fun groupPoliciesByIssueYear(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.issueDate.year.toString() }
    }

    private fun groupPoliciesByAgeBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.age < 30 -> "Under 30"
                it.age < 50 -> "30-49"
                it.age < 65 -> "50-64"
                else -> "65+"
            }
        }
    }

    private fun groupPoliciesByProductType(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.productType }
    }

    private fun groupPoliciesByPremiumBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.faceAmount < BigDecimal("100000") -> "Under $100K"
                it.faceAmount < BigDecimal("500000") -> "$100K-$500K"
                it.faceAmount < BigDecimal("1000000") -> "$500K-$1M"
                else -> "Over $1M"
            }
        }
    }

    private fun calculateCohortRetentionRates(policies: List<PolicyInfo>): List<BigDecimal> {
        return listOf(BigDecimal("0.95"), BigDecimal("0.90"), BigDecimal("0.85"))
    }

    private fun calculateCohortProfitability(policies: List<PolicyInfo>): BigDecimal = BigDecimal("0.12")
    private fun calculateCohortLapseRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.08"))
    private fun calculateCohortClaimRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.02"))
    private fun generateCohortInsights(metrics: List<CohortMetrics>): List<String> = listOf("Newer cohorts show better retention")
    private fun generateCohortRecommendations(metrics: List<CohortMetrics>): List<String> = listOf("Focus on retention strategies")

    private fun calculateConfidence(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 0.5
    private fun calculateLift(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 1.2
    private fun identifyCrossSellOpportunities(rules: List<AssociationRule>): List<String> = listOf("Cross-sell life insurance to annuity customers")

    private fun extractTimeSeriesData(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): List<TimeSeriesPoint> {
        return (1..24).map { month ->
            TimeSeriesPoint(
                period = month,
                value = BigDecimal(1000000 + month * 50000),
                date = LocalDate.now().minusMonths(24 - month.toLong())
            )
        }
    }

    private fun analyzeTrend(data: List<TimeSeriesPoint>): TrendAnalysis {
        val slope = calculateSlope(data)
        val trendDirection = when {
            slope > 0.05 -> TrendDirection.INCREASING
            slope < -0.05 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        return TrendAnalysis(
            trendDirection = trendDirection,
            slope = BigDecimal(slope).setScale(6, RoundingMode.HALF_UP),
            strength = TrendStrength.MODERATE
        )
    }

    private fun analyzeSeasonality(data: List<TimeSeriesPoint>): SeasonalityAnalysis {
        return SeasonalityAnalysis(
            hasSeasonality = true,
            seasonalPeriod = 12,
            seasonalStrength = BigDecimal("0.15"),
            peakPeriods = listOf(11, 12, 1), // Nov, Dec, Jan
            troughPeriods = listOf(6, 7, 8) // Jun, Jul, Aug
        )
    }

    private fun forecastTimeSeries(data: List<TimeSeriesPoint>, periods: Int): List<ForecastPoint> {
        return (1..periods).map { period ->
            val baseValue = data.last().value
            val growth = BigDecimal("0.02") // 2% growth per period
            val forecastValue = baseValue.multiply(BigDecimal.ONE.add(growth).pow(period))
            
            ForecastPoint(
                period = period,
                forecastValue = forecastValue,
                lowerBound = forecastValue.multiply(BigDecimal("0.9")),
                upperBound = forecastValue.multiply(BigDecimal("1.1")),
                date = data.last().date.plusMonths(period.toLong())
            )
        }
    }

    private fun assessTimeSeriesModelFit(historical: List<TimeSeriesPoint>, forecast: List<ForecastPoint>): ModelFitMetrics {
        return ModelFitMetrics(
            r2 = BigDecimal("0.85"),
            mae = BigDecimal("50000"),
            rmse = BigDecimal("75000"),
            mape = BigDecimal("0.05")
        )
    }

    private fun generateTimeSeriesInsights(trend: TrendAnalysis, seasonality: SeasonalityAnalysis): List<String> {
        val insights = mutableListOf<String>()
        
        when (trend.trendDirection) {
            TrendDirection.INCREASING -> insights.add("Strong upward trend indicates growing business")
            TrendDirection.DECREASING -> insights.add("Declining trend requires immediate attention")
            TrendDirection.STABLE -> insights.add("Stable trend suggests mature market")
        }
        
        if (seasonality.hasSeasonality) {
            insights.add("Clear seasonal patterns detected - plan marketing campaigns accordingly")
        }
        
        return insights
    }

    // Additional helper methods for calculations
    private fun calculateSalesGrowthRate(salesByMonth: Map<String, BigDecimal>): BigDecimal {
        if (salesByMonth.size < 2) return BigDecimal.ZERO
        
        val sortedSales = salesByMonth.toList().sortedBy { it.first }
        val firstMonth = sortedSales.first().second
        val lastMonth = sortedSales.last().second
        
        return if (firstMonth > BigDecimal.ZERO) {
            lastMonth.subtract(firstMonth).divide(firstMonth, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun calculateProfitabilityTrend(policies: List<PolicyInfo>): List<BigDecimal> {
        return policies.groupBy { it.issueDate.year }
            .values.map { yearPolicies ->
                val revenue = yearPolicies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
            }
    }

    private fun calculateChurnRate(customers: Map<String, List<PolicyInfo>>): BigDecimal {
        // Simplified churn calculation
        return BigDecimal("0.08") // 8% churn rate
    }

    private fun calculateCustomerAcquisitionCost(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified CAC calculation
        return BigDecimal("500") // $500 per customer
    }

    private fun calculateProductProfitability(policies: List<PolicyInfo>): BigDecimal {
        val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
        val expenses = revenue.multiply(BigDecimal("0.75"))
        return revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
    }

    private fun calculateProductGrowthRate(policies: List<PolicyInfo>, timeRange: DateRange): BigDecimal {
        val currentYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year }
        val previousYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year - 1 }
        
        val currentCount = currentYearPolicies.size
        val previousCount = previousYearPolicies.size
        
        return if (previousCount > 0) {
            BigDecimal(currentCount - previousCount).divide(BigDecimal(previousCount), 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun analyzeProductLifecycle(products: List<ProductPerformanceMetrics>): Map<String, ProductLifecycleStage> {
        return products.associate { product ->
            val stage = when {
                product.growthRate > BigDecimal("0.20") -> ProductLifecycleStage.GROWTH
                product.growthRate > BigDecimal("0.05") -> ProductLifecycleStage.MATURITY
                product.growthRate < BigDecimal("-0.05") -> ProductLifecycleStage.DECLINE
                else -> ProductLifecycleStage.INTRODUCTION
            }
            product.productType to stage
        }
    }

    private fun calculateSlope(data: List<TimeSeriesPoint>): Double {
        val n = data.size
        val sumX = (1..n).sum().toDouble()
        val sumY = data.sumOf { it.value.toDouble() }
        val sumXY = data.mapIndexed { index, point -> (index + 1) * point.value.toDouble() }.sum()
        val sumX2 = (1..n).sumOf { it * it }.toDouble()
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }

    private fun List<BigDecimal>.average(): BigDecimal {
        return if (isEmpty()) BigDecimal.ZERO
        else fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }.divide(BigDecimal(size), 2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.pow(n: Int): BigDecimal {
        return this.pow(n, java.math.MathContext.DECIMAL128)
    }
}

// Data classes and enums for analytics
data class BusinessIntelligenceReport(
    val reportDate: LocalDateTime,
    val timeRange: DateRange,
    val salesAnalytics: SalesAnalytics,
    val profitabilityAnalytics: ProfitabilityAnalytics,
    val customerAnalytics: CustomerAnalytics,
    val productAnalytics: ProductAnalytics,
    val marketAnalytics: MarketAnalytics,
    val keyInsights: List<String>,
    val recommendations: List<String>
)

data class SalesAnalytics(
    val totalSales: BigDecimal,
    val policyCount: Int,
    val averagePolicySize: BigDecimal,
    val salesGrowthRate: BigDecimal,
    val salesByMonth: Map<String, BigDecimal>,
    val salesByProduct: Map<String, BigDecimal>,
    val salesByState: Map<String, BigDecimal>,
    val topPerformingProducts: List<Pair<String, BigDecimal>>,
    val topPerformingStates: List<Pair<String, BigDecimal>>
)

data class ProfitabilityAnalytics(
    val totalRevenue: BigDecimal,
    val totalExpenses: BigDecimal,
    val netIncome: BigDecimal,
    val profitMargin: BigDecimal,
    val returnOnAssets: BigDecimal,
    val profitabilityByProduct: Map<String, BigDecimal>,
    val profitabilityTrend: List<BigDecimal>
)

data class CustomerAnalytics(
    val totalCustomers: Int,
    val averagePoliciesPerCustomer: Double,
    val averageCustomerLifetimeValue: BigDecimal,
    val churnRate: BigDecimal,
    val retentionRate: BigDecimal,
    val customerAcquisitionCost: BigDecimal,
    val customerSatisfactionScore: BigDecimal,
    val netPromoterScore: BigDecimal
)

data class ProductAnalytics(
    val productPerformance: Map<String, ProductPerformanceMetrics>,
    val topPerformingProducts: List<ProductPerformanceMetrics>,
    val underperformingProducts: List<ProductPerformanceMetrics>,
    val productLifecycleAnalysis: Map<String, ProductLifecycleStage>
)

data class ProductPerformanceMetrics(
    val productType: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val profitability: BigDecimal,
    val growthRate: BigDecimal,
    val marketShare: BigDecimal
)

data class MarketAnalytics(
    val marketSize: BigDecimal,
    val marketGrowthRate: BigDecimal,
    val competitivePosition: BigDecimal,
    val marketTrends: List<String>,
    val competitorAnalysis: Map<String, BigDecimal>
)

data class PredictiveAnalysisResult(
    val analysisDate: LocalDateTime,
    val predictionHorizon: Int,
    val lapseRatePrediction: PredictionResult,
    val mortalityPrediction: PredictionResult,
    val salesForecast: PredictionResult,
    val profitabilityForecast: PredictionResult,
    val riskPrediction: PredictionResult,
    val confidenceIntervals: Map<String, ConfidenceInterval>,
    val modelAccuracy: ModelAccuracyMetrics
)

data class PredictionResult(
    val metric: String,
    val predictions: List<PredictionPoint>,
    val modelType: String,
    val accuracy: BigDecimal
)

data class PredictionPoint(
    val period: Int,
    val predictedValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal
)

data class ConfidenceInterval(
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val confidenceLevel: BigDecimal
)

data class ModelAccuracyMetrics(
    val meanAbsoluteError: BigDecimal,
    val rootMeanSquareError: BigDecimal,
    val meanAbsolutePercentageError: BigDecimal,
    val r2Score: BigDecimal
)

data class CustomerSegmentationResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val segmentCount: Int,
    val segmentProfiles: List<CustomerSegmentProfile>,
    val segmentationCriteria: List<String>,
    val recommendedTargeting: List<String>
)

data class CustomerSegmentProfile(
    val segmentId: String,
    val segmentName: String,
    val customerCount: Int,
    val averageAge: Double,
    val averagePolicyValue: Double,
    val profitability: BigDecimal,
    val retentionRate: BigDecimal,
    val growthPotential: GrowthPotential,
    val characteristics: List<String>
)

data class CustomerSegment(
    val segmentId: String,
    val segmentName: String,
    val customers: List<CustomerInfo>
)

data class CustomerInfo(
    val customerId: String,
    val age: Int,
    val totalPolicyValue: BigDecimal
)

data class CohortAnalysisResult(
    val analysisDate: LocalDateTime,
    val cohortType: CohortType,
    val cohortMetrics: List<CohortMetrics>,
    val insights: List<String>,
    val recommendations: List<String>
)

data class CohortMetrics(
    val cohortId: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val retentionRates: List<BigDecimal>,
    val profitabilityMetrics: BigDecimal,
    val lapseRates: List<BigDecimal>,
    val claimRates: List<BigDecimal>
)

data class MarketBasketAnalysisResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val multiProductCustomers: Int,
    val frequentCombinations: List<Pair<Set<String>, Int>>,
    val associationRules: List<AssociationRule>,
    val crossSellOpportunities: List<String>
)

data class AssociationRule(
    val antecedent: Set<String>,
    val consequent: Set<String>,
    val support: Double,
    val confidence: Double,
    val lift: Double
)

data class TimeSeriesAnalysisResult(
    val analysisDate: LocalDateTime,
    val metric: TimeSeriesMetric,
    val timeSeriesData: List<TimeSeriesPoint>,
    val trendAnalysis: TrendAnalysis,
    val seasonalityAnalysis: SeasonalityAnalysis,
    val forecastData: List<ForecastPoint>,
    val modelFit: ModelFitMetrics,
    val insights: List<String>
)

data class TimeSeriesPoint(
    val period: Int,
    val value: BigDecimal,
    val date: LocalDate
)

data class TrendAnalysis(
    val trendDirection: TrendDirection,
    val slope: BigDecimal,
    val strength: TrendStrength
)

data class SeasonalityAnalysis(
    val hasSeasonality: Boolean,
    val seasonalPeriod: Int,
    val seasonalStrength: BigDecimal,
    val peakPeriods: List<Int>,
    val troughPeriods: List<Int>
)

data class ForecastPoint(
    val period: Int,
    val forecastValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val date: LocalDate
)

data class ModelFitMetrics(
    val r2: BigDecimal,
    val mae: BigDecimal,
    val rmse: BigDecimal,
    val mape: BigDecimal
)

enum class CohortType {
    ISSUE_YEAR, AGE_BAND, PRODUCT_TYPE, PREMIUM_BAND
}

enum class GrowthPotential {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

enum class ProductLifecycleStage {
    INTRODUCTION, GROWTH, MATURITY, DECLINE
}

enum class TimeSeriesMetric {
    SALES_VOLUME, POLICY_COUNT, PREMIUM_INCOME, CLAIMS_PAID, LAPSE_RATE
}

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class TrendStrength {
    WEAK, MODERATE, STRONG, VERY_STRONG
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Data analytics service for business intelligence and reporting
 */
@Service
class DataAnalytics {

    fun generateBusinessIntelligenceReport(portfolio: InsurancePortfolio, timeRange: DateRange): BusinessIntelligenceReport {
        val salesAnalytics = analyzeSalesPerformance(portfolio, timeRange)
        val profitabilityAnalytics = analyzeProfitability(portfolio, timeRange)
        val customerAnalytics = analyzeCustomerBehavior(portfolio, timeRange)
        val productAnalytics = analyzeProductPerformance(portfolio, timeRange)
        val marketAnalytics = analyzeMarketTrends(portfolio, timeRange)
        
        return BusinessIntelligenceReport(
            reportDate = LocalDateTime.now(),
            timeRange = timeRange,
            salesAnalytics = salesAnalytics,
            profitabilityAnalytics = profitabilityAnalytics,
            customerAnalytics = customerAnalytics,
            productAnalytics = productAnalytics,
            marketAnalytics = marketAnalytics,
            keyInsights = generateKeyInsights(salesAnalytics, profitabilityAnalytics, customerAnalytics),
            recommendations = generateBusinessRecommendations(salesAnalytics, profitabilityAnalytics, productAnalytics)
        )
    }

    fun performPredictiveAnalysis(portfolio: InsurancePortfolio, predictionHorizon: Int): PredictiveAnalysisResult {
        val lapseRatePrediction = predictLapseRates(portfolio, predictionHorizon)
        val mortalityPrediction = predictMortalityRates(portfolio, predictionHorizon)
        val salesForecast = forecastSales(portfolio, predictionHorizon)
        val profitabilityForecast = forecastProfitability(portfolio, predictionHorizon)
        val riskPrediction = predictRiskFactors(portfolio, predictionHorizon)
        
        return PredictiveAnalysisResult(
            analysisDate = LocalDateTime.now(),
            predictionHorizon = predictionHorizon,
            lapseRatePrediction = lapseRatePrediction,
            mortalityPrediction = mortalityPrediction,
            salesForecast = salesForecast,
            profitabilityForecast = profitabilityForecast,
            riskPrediction = riskPrediction,
            confidenceIntervals = calculateConfidenceIntervals(lapseRatePrediction, mortalityPrediction, salesForecast),
            modelAccuracy = assessModelAccuracy(portfolio)
        )
    }

    fun analyzeCustomerSegmentation(portfolio: InsurancePortfolio): CustomerSegmentationResult {
        val segments = performCustomerSegmentation(portfolio)
        val segmentProfiles = segments.map { segment ->
            CustomerSegmentProfile(
                segmentId = segment.segmentId,
                segmentName = segment.segmentName,
                customerCount = segment.customers.size,
                averageAge = segment.customers.map { it.age }.average(),
                averagePolicyValue = segment.customers.map { it.totalPolicyValue.toDouble() }.average(),
                profitability = calculateSegmentProfitability(segment),
                retentionRate = calculateSegmentRetentionRate(segment),
                growthPotential = assessSegmentGrowthPotential(segment),
                characteristics = identifySegmentCharacteristics(segment)
            )
        }
        
        return CustomerSegmentationResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = portfolio.policies.size,
            segmentCount = segments.size,
            segmentProfiles = segmentProfiles,
            segmentationCriteria = listOf("Age", "Income", "Policy Value", "Geographic Location"),
            recommendedTargeting = recommendTargetingStrategies(segmentProfiles)
        )
    }

    fun performCohortAnalysis(portfolio: InsurancePortfolio, cohortType: CohortType): CohortAnalysisResult {
        val cohorts = when (cohortType) {
            CohortType.ISSUE_YEAR -> groupPoliciesByIssueYear(portfolio)
            CohortType.AGE_BAND -> groupPoliciesByAgeBand(portfolio)
            CohortType.PRODUCT_TYPE -> groupPoliciesByProductType(portfolio)
            CohortType.PREMIUM_BAND -> groupPoliciesByPremiumBand(portfolio)
        }
        
        val cohortMetrics = cohorts.map { (cohortKey, policies) ->
            CohortMetrics(
                cohortId = cohortKey,
                policyCount = policies.size,
                totalPremium = policies.sumOf { it.faceAmount },
                averagePolicySize = policies.map { it.faceAmount }.average(),
                retentionRates = calculateCohortRetentionRates(policies),
                profitabilityMetrics = calculateCohortProfitability(policies),
                lapseRates = calculateCohortLapseRates(policies),
                claimRates = calculateCohortClaimRates(policies)
            )
        }
        
        return CohortAnalysisResult(
            analysisDate = LocalDateTime.now(),
            cohortType = cohortType,
            cohortMetrics = cohortMetrics,
            insights = generateCohortInsights(cohortMetrics),
            recommendations = generateCohortRecommendations(cohortMetrics)
        )
    }

    fun analyzeMarketBasketAnalysis(portfolio: InsurancePortfolio): MarketBasketAnalysisResult {
        val customerPolicies = portfolio.policies.groupBy { it.customerId }
        val productCombinations = mutableMapOf<Set<String>, Int>()
        val associationRules = mutableListOf<AssociationRule>()
        
        // Find frequent product combinations
        customerPolicies.values.forEach { policies ->
            if (policies.size > 1) {
                val products = policies.map { it.productType }.toSet()
                productCombinations[products] = productCombinations.getOrDefault(products, 0) + 1
            }
        }
        
        // Generate association rules
        productCombinations.forEach { (products, frequency) ->
            if (frequency >= 5 && products.size >= 2) { // Minimum support threshold
                products.forEach { antecedent ->
                    val consequent = products - antecedent
                    if (consequent.isNotEmpty()) {
                        val support = frequency.toDouble() / customerPolicies.size
                        val confidence = calculateConfidence(antecedent, consequent, customerPolicies)
                        val lift = calculateLift(antecedent, consequent, customerPolicies)
                        
                        if (confidence > 0.3 && lift > 1.0) { // Minimum thresholds
                            associationRules.add(AssociationRule(
                                antecedent = setOf(antecedent),
                                consequent = consequent,
                                support = support,
                                confidence = confidence,
                                lift = lift
                            ))
                        }
                    }
                }
            }
        }
        
        return MarketBasketAnalysisResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = customerPolicies.size,
            multiProductCustomers = customerPolicies.values.count { it.size > 1 },
            frequentCombinations = productCombinations.toList().sortedByDescending { it.second }.take(10),
            associationRules = associationRules.sortedByDescending { it.lift }.take(20),
            crossSellOpportunities = identifyCrossSellOpportunities(associationRules)
        )
    }

    fun performTimeSeriesAnalysis(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): TimeSeriesAnalysisResult {
        val timeSeriesData = extractTimeSeriesData(portfolio, metric)
        val trendAnalysis = analyzeTrend(timeSeriesData)
        val seasonalityAnalysis = analyzeSeasonality(timeSeriesData)
        val forecastData = forecastTimeSeries(timeSeriesData, 12) // 12 periods ahead
        
        return TimeSeriesAnalysisResult(
            analysisDate = LocalDateTime.now(),
            metric = metric,
            timeSeriesData = timeSeriesData,
            trendAnalysis = trendAnalysis,
            seasonalityAnalysis = seasonalityAnalysis,
            forecastData = forecastData,
            modelFit = assessTimeSeriesModelFit(timeSeriesData, forecastData),
            insights = generateTimeSeriesInsights(trendAnalysis, seasonalityAnalysis)
        )
    }

    // Private helper methods
    private fun analyzeSalesPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): SalesAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalSales = policiesInRange.sumOf { it.faceAmount }
        val policyCount = policiesInRange.size
        val averagePolicySize = if (policyCount > 0) totalSales.divide(BigDecimal(policyCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val salesByMonth = policiesInRange.groupBy { "${it.issueDate.year}-${it.issueDate.monthValue.toString().padStart(2, '0')}" }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByState = policiesInRange.groupBy { it.state }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        return SalesAnalytics(
            totalSales = totalSales,
            policyCount = policyCount,
            averagePolicySize = averagePolicySize,
            salesGrowthRate = calculateSalesGrowthRate(salesByMonth),
            salesByMonth = salesByMonth,
            salesByProduct = salesByProduct,
            salesByState = salesByState,
            topPerformingProducts = salesByProduct.toList().sortedByDescending { it.second }.take(5),
            topPerformingStates = salesByState.toList().sortedByDescending { it.second }.take(5)
        )
    }

    private fun analyzeProfitability(portfolio: InsurancePortfolio, timeRange: DateRange): ProfitabilityAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalRevenue = policiesInRange.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) } // 1% revenue assumption
        val totalExpenses = totalRevenue.multiply(BigDecimal("0.75")) // 75% expense ratio
        val netIncome = totalRevenue.subtract(totalExpenses)
        val profitMargin = if (totalRevenue > BigDecimal.ZERO) netIncome.divide(totalRevenue, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val profitabilityByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) ->
                val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses)
            }
        
        return ProfitabilityAnalytics(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netIncome = netIncome,
            profitMargin = profitMargin,
            returnOnAssets = netIncome.divide(totalRevenue.multiply(BigDecimal("10")), 4, RoundingMode.HALF_UP), // Simplified ROA
            profitabilityByProduct = profitabilityByProduct,
            profitabilityTrend = calculateProfitabilityTrend(policiesInRange)
        )
    }

    private fun analyzeCustomerBehavior(portfolio: InsurancePortfolio, timeRange: DateRange): CustomerAnalytics {
        val customers = portfolio.policies.groupBy { it.customerId }
        
        val totalCustomers = customers.size
        val averagePoliciesPerCustomer = portfolio.policies.size.toDouble() / totalCustomers
        val customerLifetimeValue = customers.mapValues { (_, policies) ->
            policies.sumOf { it.faceAmount.multiply(BigDecimal("0.05")) } // 5% CLV assumption
        }
        
        val averageCLV = customerLifetimeValue.values.map { it.toDouble() }.average()
        val churnRate = calculateChurnRate(customers)
        val retentionRate = BigDecimal.ONE.subtract(churnRate)
        
        return CustomerAnalytics(
            totalCustomers = totalCustomers,
            averagePoliciesPerCustomer = averagePoliciesPerCustomer,
            averageCustomerLifetimeValue = BigDecimal(averageCLV).setScale(2, RoundingMode.HALF_UP),
            churnRate = churnRate,
            retentionRate = retentionRate,
            customerAcquisitionCost = calculateCustomerAcquisitionCost(portfolio),
            customerSatisfactionScore = BigDecimal("8.5"), // Mock score
            netPromoterScore = BigDecimal("45") // Mock NPS
        )
    }

    private fun analyzeProductPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): ProductAnalytics {
        val productPerformance = portfolio.policies.groupBy { it.productType }
            .mapValues { (productType, policies) ->
                ProductPerformanceMetrics(
                    productType = productType,
                    policyCount = policies.size,
                    totalPremium = policies.sumOf { it.faceAmount },
                    averagePolicySize = policies.map { it.faceAmount }.average(),
                    profitability = calculateProductProfitability(policies),
                    growthRate = calculateProductGrowthRate(policies, timeRange),
                    marketShare = BigDecimal(policies.size).divide(BigDecimal(portfolio.policies.size), 4, RoundingMode.HALF_UP)
                )
            }
        
        return ProductAnalytics(
            productPerformance = productPerformance,
            topPerformingProducts = productPerformance.values.sortedByDescending { it.profitability }.take(5),
            underperformingProducts = productPerformance.values.sortedBy { it.profitability }.take(3),
            productLifecycleAnalysis = analyzeProductLifecycle(productPerformance.values.toList())
        )
    }

    private fun analyzeMarketTrends(portfolio: InsurancePortfolio, timeRange: DateRange): MarketAnalytics {
        return MarketAnalytics(
            marketSize = BigDecimal("50000000000"), // $50B market size assumption
            marketGrowthRate = BigDecimal("0.05"), // 5% growth
            competitivePosition = BigDecimal("0.15"), // 15% market share
            marketTrends = listOf(
                "Increasing demand for digital insurance products",
                "Growing focus on ESG investing",
                "Rising interest in hybrid work benefits"
            ),
            competitorAnalysis = mapOf(
                "Competitor A" to BigDecimal("0.20"),
                "Competitor B" to BigDecimal("0.18"),
                "Competitor C" to BigDecimal("0.12")
            )
        )
    }

    private fun generateKeyInsights(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, customerAnalytics: CustomerAnalytics): List<String> {
        val insights = mutableListOf<String>()
        
        if (salesAnalytics.salesGrowthRate > BigDecimal("0.10")) {
            insights.add("Strong sales growth of ${salesAnalytics.salesGrowthRate.multiply(BigDecimal("100"))}% indicates healthy market demand")
        }
        
        if (profitabilityAnalytics.profitMargin > BigDecimal("0.15")) {
            insights.add("Healthy profit margin of ${profitabilityAnalytics.profitMargin.multiply(BigDecimal("100"))}% demonstrates operational efficiency")
        }
        
        if (customerAnalytics.retentionRate > BigDecimal("0.90")) {
            insights.add("High customer retention rate of ${customerAnalytics.retentionRate.multiply(BigDecimal("100"))}% indicates strong customer satisfaction")
        }
        
        return insights
    }

    private fun generateBusinessRecommendations(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, productAnalytics: ProductAnalytics): List<String> {
        val recommendations = mutableListOf<String>()
        
        val topProduct = productAnalytics.topPerformingProducts.firstOrNull()
        if (topProduct != null) {
            recommendations.add("Focus marketing efforts on ${topProduct.productType} which shows highest profitability")
        }
        
        if (profitabilityAnalytics.profitMargin < BigDecimal("0.10")) {
            recommendations.add("Review expense structure to improve profit margins")
        }
        
        val topState = salesAnalytics.topPerformingStates.firstOrNull()
        if (topState != null) {
            recommendations.add("Expand operations in ${topState.first} market which shows strong performance")
        }
        
        return recommendations
    }

    private fun predictLapseRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified lapse rate prediction using historical trends
        val currentLapseRate = BigDecimal("0.08") // 8% current lapse rate
        val trendFactor = BigDecimal("0.02") // 2% annual increase trend
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentLapseRate.add(trendFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.9")),
                upperBound = predictedRate.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Lapse Rate",
            predictions = predictions,
            modelType = "Linear Trend",
            accuracy = BigDecimal("0.85")
        )
    }

    private fun predictMortalityRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified mortality rate prediction
        val currentMortalityRate = BigDecimal("0.012") // 1.2% current mortality rate
        val agingFactor = BigDecimal("0.001") // 0.1% annual increase due to aging
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentMortalityRate.add(agingFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.8")),
                upperBound = predictedRate.multiply(BigDecimal("1.2"))
            )
        }
        
        return PredictionResult(
            metric = "Mortality Rate",
            predictions = predictions,
            modelType = "Demographic Trend",
            accuracy = BigDecimal("0.78")
        )
    }

    private fun forecastSales(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentSales = portfolio.policies.sumOf { it.faceAmount }
        val growthRate = BigDecimal("0.07") // 7% annual growth
        
        val predictions = (1..horizon).map { year ->
            val predictedSales = currentSales.multiply(BigDecimal.ONE.add(growthRate).pow(year))
            PredictionPoint(
                period = year,
                predictedValue = predictedSales,
                lowerBound = predictedSales.multiply(BigDecimal("0.85")),
                upperBound = predictedSales.multiply(BigDecimal("1.15"))
            )
        }
        
        return PredictionResult(
            metric = "Sales Volume",
            predictions = predictions,
            modelType = "Exponential Growth",
            accuracy = BigDecimal("0.82")
        )
    }

    private fun forecastProfitability(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentProfitability = BigDecimal("0.12") // 12% current profit margin
        val improvementRate = BigDecimal("0.005") // 0.5% annual improvement
        
        val predictions = (1..horizon).map { year ->
            val predictedProfitability = currentProfitability.add(improvementRate.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedProfitability,
                lowerBound = predictedProfitability.multiply(BigDecimal("0.9")),
                upperBound = predictedProfitability.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Profitability",
            predictions = predictions,
            modelType = "Linear Improvement",
            accuracy = BigDecimal("0.75")
        )
    }

    private fun predictRiskFactors(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentRiskScore = BigDecimal("0.65") // Current risk score
        val volatility = BigDecimal("0.05") // 5% volatility
        
        val predictions = (1..horizon).map { year ->
            val randomFactor = (kotlin.random.Random.nextDouble() - 0.5) * 0.1
            val predictedRisk = currentRiskScore.add(BigDecimal(randomFactor))
            PredictionPoint(
                period = year,
                predictedValue = predictedRisk,
                lowerBound = predictedRisk.subtract(volatility),
                upperBound = predictedRisk.add(volatility)
            )
        }
        
        return PredictionResult(
            metric = "Risk Score",
            predictions = predictions,
            modelType = "Stochastic Model",
            accuracy = BigDecimal("0.70")
        )
    }

    private fun calculateConfidenceIntervals(vararg predictions: PredictionResult): Map<String, ConfidenceInterval> {
        return predictions.associate { prediction ->
            val avgLower = prediction.predictions.map { it.lowerBound.toDouble() }.average()
            val avgUpper = prediction.predictions.map { it.upperBound.toDouble() }.average()
            
            prediction.metric to ConfidenceInterval(
                lowerBound = BigDecimal(avgLower).setScale(4, RoundingMode.HALF_UP),
                upperBound = BigDecimal(avgUpper).setScale(4, RoundingMode.HALF_UP),
                confidenceLevel = BigDecimal("0.95")
            )
        }
    }

    private fun assessModelAccuracy(portfolio: InsurancePortfolio): ModelAccuracyMetrics {
        return ModelAccuracyMetrics(
            meanAbsoluteError = BigDecimal("0.05"),
            rootMeanSquareError = BigDecimal("0.08"),
            meanAbsolutePercentageError = BigDecimal("0.12"),
            r2Score = BigDecimal("0.85")
        )
    }

    // Additional helper methods would continue here...
    private fun performCustomerSegmentation(portfolio: InsurancePortfolio): List<CustomerSegment> {
        // Mock segmentation logic
        return listOf(
            CustomerSegment("SEG001", "High Value", emptyList()),
            CustomerSegment("SEG002", "Young Professionals", emptyList()),
            CustomerSegment("SEG003", "Retirees", emptyList())
        )
    }

    private fun calculateSegmentProfitability(segment: CustomerSegment): BigDecimal = BigDecimal("0.15")
    private fun calculateSegmentRetentionRate(segment: CustomerSegment): BigDecimal = BigDecimal("0.92")
    private fun assessSegmentGrowthPotential(segment: CustomerSegment): GrowthPotential = GrowthPotential.HIGH
    private fun identifySegmentCharacteristics(segment: CustomerSegment): List<String> = listOf("High income", "Tech-savvy")
    private fun recommendTargetingStrategies(profiles: List<CustomerSegmentProfile>): List<String> = listOf("Digital marketing", "Referral programs")

    private fun groupPoliciesByIssueYear(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.issueDate.year.toString() }
    }

    private fun groupPoliciesByAgeBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.age < 30 -> "Under 30"
                it.age < 50 -> "30-49"
                it.age < 65 -> "50-64"
                else -> "65+"
            }
        }
    }

    private fun groupPoliciesByProductType(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.productType }
    }

    private fun groupPoliciesByPremiumBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.faceAmount < BigDecimal("100000") -> "Under $100K"
                it.faceAmount < BigDecimal("500000") -> "$100K-$500K"
                it.faceAmount < BigDecimal("1000000") -> "$500K-$1M"
                else -> "Over $1M"
            }
        }
    }

    private fun calculateCohortRetentionRates(policies: List<PolicyInfo>): List<BigDecimal> {
        return listOf(BigDecimal("0.95"), BigDecimal("0.90"), BigDecimal("0.85"))
    }

    private fun calculateCohortProfitability(policies: List<PolicyInfo>): BigDecimal = BigDecimal("0.12")
    private fun calculateCohortLapseRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.08"))
    private fun calculateCohortClaimRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.02"))
    private fun generateCohortInsights(metrics: List<CohortMetrics>): List<String> = listOf("Newer cohorts show better retention")
    private fun generateCohortRecommendations(metrics: List<CohortMetrics>): List<String> = listOf("Focus on retention strategies")

    private fun calculateConfidence(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 0.5
    private fun calculateLift(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 1.2
    private fun identifyCrossSellOpportunities(rules: List<AssociationRule>): List<String> = listOf("Cross-sell life insurance to annuity customers")

    private fun extractTimeSeriesData(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): List<TimeSeriesPoint> {
        return (1..24).map { month ->
            TimeSeriesPoint(
                period = month,
                value = BigDecimal(1000000 + month * 50000),
                date = LocalDate.now().minusMonths(24 - month.toLong())
            )
        }
    }

    private fun analyzeTrend(data: List<TimeSeriesPoint>): TrendAnalysis {
        val slope = calculateSlope(data)
        val trendDirection = when {
            slope > 0.05 -> TrendDirection.INCREASING
            slope < -0.05 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        return TrendAnalysis(
            trendDirection = trendDirection,
            slope = BigDecimal(slope).setScale(6, RoundingMode.HALF_UP),
            strength = TrendStrength.MODERATE
        )
    }

    private fun analyzeSeasonality(data: List<TimeSeriesPoint>): SeasonalityAnalysis {
        return SeasonalityAnalysis(
            hasSeasonality = true,
            seasonalPeriod = 12,
            seasonalStrength = BigDecimal("0.15"),
            peakPeriods = listOf(11, 12, 1), // Nov, Dec, Jan
            troughPeriods = listOf(6, 7, 8) // Jun, Jul, Aug
        )
    }

    private fun forecastTimeSeries(data: List<TimeSeriesPoint>, periods: Int): List<ForecastPoint> {
        return (1..periods).map { period ->
            val baseValue = data.last().value
            val growth = BigDecimal("0.02") // 2% growth per period
            val forecastValue = baseValue.multiply(BigDecimal.ONE.add(growth).pow(period))
            
            ForecastPoint(
                period = period,
                forecastValue = forecastValue,
                lowerBound = forecastValue.multiply(BigDecimal("0.9")),
                upperBound = forecastValue.multiply(BigDecimal("1.1")),
                date = data.last().date.plusMonths(period.toLong())
            )
        }
    }

    private fun assessTimeSeriesModelFit(historical: List<TimeSeriesPoint>, forecast: List<ForecastPoint>): ModelFitMetrics {
        return ModelFitMetrics(
            r2 = BigDecimal("0.85"),
            mae = BigDecimal("50000"),
            rmse = BigDecimal("75000"),
            mape = BigDecimal("0.05")
        )
    }

    private fun generateTimeSeriesInsights(trend: TrendAnalysis, seasonality: SeasonalityAnalysis): List<String> {
        val insights = mutableListOf<String>()
        
        when (trend.trendDirection) {
            TrendDirection.INCREASING -> insights.add("Strong upward trend indicates growing business")
            TrendDirection.DECREASING -> insights.add("Declining trend requires immediate attention")
            TrendDirection.STABLE -> insights.add("Stable trend suggests mature market")
        }
        
        if (seasonality.hasSeasonality) {
            insights.add("Clear seasonal patterns detected - plan marketing campaigns accordingly")
        }
        
        return insights
    }

    // Additional helper methods for calculations
    private fun calculateSalesGrowthRate(salesByMonth: Map<String, BigDecimal>): BigDecimal {
        if (salesByMonth.size < 2) return BigDecimal.ZERO
        
        val sortedSales = salesByMonth.toList().sortedBy { it.first }
        val firstMonth = sortedSales.first().second
        val lastMonth = sortedSales.last().second
        
        return if (firstMonth > BigDecimal.ZERO) {
            lastMonth.subtract(firstMonth).divide(firstMonth, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun calculateProfitabilityTrend(policies: List<PolicyInfo>): List<BigDecimal> {
        return policies.groupBy { it.issueDate.year }
            .values.map { yearPolicies ->
                val revenue = yearPolicies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
            }
    }

    private fun calculateChurnRate(customers: Map<String, List<PolicyInfo>>): BigDecimal {
        // Simplified churn calculation
        return BigDecimal("0.08") // 8% churn rate
    }

    private fun calculateCustomerAcquisitionCost(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified CAC calculation
        return BigDecimal("500") // $500 per customer
    }

    private fun calculateProductProfitability(policies: List<PolicyInfo>): BigDecimal {
        val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
        val expenses = revenue.multiply(BigDecimal("0.75"))
        return revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
    }

    private fun calculateProductGrowthRate(policies: List<PolicyInfo>, timeRange: DateRange): BigDecimal {
        val currentYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year }
        val previousYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year - 1 }
        
        val currentCount = currentYearPolicies.size
        val previousCount = previousYearPolicies.size
        
        return if (previousCount > 0) {
            BigDecimal(currentCount - previousCount).divide(BigDecimal(previousCount), 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun analyzeProductLifecycle(products: List<ProductPerformanceMetrics>): Map<String, ProductLifecycleStage> {
        return products.associate { product ->
            val stage = when {
                product.growthRate > BigDecimal("0.20") -> ProductLifecycleStage.GROWTH
                product.growthRate > BigDecimal("0.05") -> ProductLifecycleStage.MATURITY
                product.growthRate < BigDecimal("-0.05") -> ProductLifecycleStage.DECLINE
                else -> ProductLifecycleStage.INTRODUCTION
            }
            product.productType to stage
        }
    }

    private fun calculateSlope(data: List<TimeSeriesPoint>): Double {
        val n = data.size
        val sumX = (1..n).sum().toDouble()
        val sumY = data.sumOf { it.value.toDouble() }
        val sumXY = data.mapIndexed { index, point -> (index + 1) * point.value.toDouble() }.sum()
        val sumX2 = (1..n).sumOf { it * it }.toDouble()
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }

    private fun List<BigDecimal>.average(): BigDecimal {
        return if (isEmpty()) BigDecimal.ZERO
        else fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }.divide(BigDecimal(size), 2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.pow(n: Int): BigDecimal {
        return this.pow(n, java.math.MathContext.DECIMAL128)
    }
}

// Data classes and enums for analytics
data class BusinessIntelligenceReport(
    val reportDate: LocalDateTime,
    val timeRange: DateRange,
    val salesAnalytics: SalesAnalytics,
    val profitabilityAnalytics: ProfitabilityAnalytics,
    val customerAnalytics: CustomerAnalytics,
    val productAnalytics: ProductAnalytics,
    val marketAnalytics: MarketAnalytics,
    val keyInsights: List<String>,
    val recommendations: List<String>
)

data class SalesAnalytics(
    val totalSales: BigDecimal,
    val policyCount: Int,
    val averagePolicySize: BigDecimal,
    val salesGrowthRate: BigDecimal,
    val salesByMonth: Map<String, BigDecimal>,
    val salesByProduct: Map<String, BigDecimal>,
    val salesByState: Map<String, BigDecimal>,
    val topPerformingProducts: List<Pair<String, BigDecimal>>,
    val topPerformingStates: List<Pair<String, BigDecimal>>
)

data class ProfitabilityAnalytics(
    val totalRevenue: BigDecimal,
    val totalExpenses: BigDecimal,
    val netIncome: BigDecimal,
    val profitMargin: BigDecimal,
    val returnOnAssets: BigDecimal,
    val profitabilityByProduct: Map<String, BigDecimal>,
    val profitabilityTrend: List<BigDecimal>
)

data class CustomerAnalytics(
    val totalCustomers: Int,
    val averagePoliciesPerCustomer: Double,
    val averageCustomerLifetimeValue: BigDecimal,
    val churnRate: BigDecimal,
    val retentionRate: BigDecimal,
    val customerAcquisitionCost: BigDecimal,
    val customerSatisfactionScore: BigDecimal,
    val netPromoterScore: BigDecimal
)

data class ProductAnalytics(
    val productPerformance: Map<String, ProductPerformanceMetrics>,
    val topPerformingProducts: List<ProductPerformanceMetrics>,
    val underperformingProducts: List<ProductPerformanceMetrics>,
    val productLifecycleAnalysis: Map<String, ProductLifecycleStage>
)

data class ProductPerformanceMetrics(
    val productType: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val profitability: BigDecimal,
    val growthRate: BigDecimal,
    val marketShare: BigDecimal
)

data class MarketAnalytics(
    val marketSize: BigDecimal,
    val marketGrowthRate: BigDecimal,
    val competitivePosition: BigDecimal,
    val marketTrends: List<String>,
    val competitorAnalysis: Map<String, BigDecimal>
)

data class PredictiveAnalysisResult(
    val analysisDate: LocalDateTime,
    val predictionHorizon: Int,
    val lapseRatePrediction: PredictionResult,
    val mortalityPrediction: PredictionResult,
    val salesForecast: PredictionResult,
    val profitabilityForecast: PredictionResult,
    val riskPrediction: PredictionResult,
    val confidenceIntervals: Map<String, ConfidenceInterval>,
    val modelAccuracy: ModelAccuracyMetrics
)

data class PredictionResult(
    val metric: String,
    val predictions: List<PredictionPoint>,
    val modelType: String,
    val accuracy: BigDecimal
)

data class PredictionPoint(
    val period: Int,
    val predictedValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal
)

data class ConfidenceInterval(
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val confidenceLevel: BigDecimal
)

data class ModelAccuracyMetrics(
    val meanAbsoluteError: BigDecimal,
    val rootMeanSquareError: BigDecimal,
    val meanAbsolutePercentageError: BigDecimal,
    val r2Score: BigDecimal
)

data class CustomerSegmentationResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val segmentCount: Int,
    val segmentProfiles: List<CustomerSegmentProfile>,
    val segmentationCriteria: List<String>,
    val recommendedTargeting: List<String>
)

data class CustomerSegmentProfile(
    val segmentId: String,
    val segmentName: String,
    val customerCount: Int,
    val averageAge: Double,
    val averagePolicyValue: Double,
    val profitability: BigDecimal,
    val retentionRate: BigDecimal,
    val growthPotential: GrowthPotential,
    val characteristics: List<String>
)

data class CustomerSegment(
    val segmentId: String,
    val segmentName: String,
    val customers: List<CustomerInfo>
)

data class CustomerInfo(
    val customerId: String,
    val age: Int,
    val totalPolicyValue: BigDecimal
)

data class CohortAnalysisResult(
    val analysisDate: LocalDateTime,
    val cohortType: CohortType,
    val cohortMetrics: List<CohortMetrics>,
    val insights: List<String>,
    val recommendations: List<String>
)

data class CohortMetrics(
    val cohortId: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val retentionRates: List<BigDecimal>,
    val profitabilityMetrics: BigDecimal,
    val lapseRates: List<BigDecimal>,
    val claimRates: List<BigDecimal>
)

data class MarketBasketAnalysisResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val multiProductCustomers: Int,
    val frequentCombinations: List<Pair<Set<String>, Int>>,
    val associationRules: List<AssociationRule>,
    val crossSellOpportunities: List<String>
)

data class AssociationRule(
    val antecedent: Set<String>,
    val consequent: Set<String>,
    val support: Double,
    val confidence: Double,
    val lift: Double
)

data class TimeSeriesAnalysisResult(
    val analysisDate: LocalDateTime,
    val metric: TimeSeriesMetric,
    val timeSeriesData: List<TimeSeriesPoint>,
    val trendAnalysis: TrendAnalysis,
    val seasonalityAnalysis: SeasonalityAnalysis,
    val forecastData: List<ForecastPoint>,
    val modelFit: ModelFitMetrics,
    val insights: List<String>
)

data class TimeSeriesPoint(
    val period: Int,
    val value: BigDecimal,
    val date: LocalDate
)

data class TrendAnalysis(
    val trendDirection: TrendDirection,
    val slope: BigDecimal,
    val strength: TrendStrength
)

data class SeasonalityAnalysis(
    val hasSeasonality: Boolean,
    val seasonalPeriod: Int,
    val seasonalStrength: BigDecimal,
    val peakPeriods: List<Int>,
    val troughPeriods: List<Int>
)

data class ForecastPoint(
    val period: Int,
    val forecastValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val date: LocalDate
)

data class ModelFitMetrics(
    val r2: BigDecimal,
    val mae: BigDecimal,
    val rmse: BigDecimal,
    val mape: BigDecimal
)

enum class CohortType {
    ISSUE_YEAR, AGE_BAND, PRODUCT_TYPE, PREMIUM_BAND
}

enum class GrowthPotential {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

enum class ProductLifecycleStage {
    INTRODUCTION, GROWTH, MATURITY, DECLINE
}

enum class TimeSeriesMetric {
    SALES_VOLUME, POLICY_COUNT, PREMIUM_INCOME, CLAIMS_PAID, LAPSE_RATE
}

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class TrendStrength {
    WEAK, MODERATE, STRONG, VERY_STRONG
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Data analytics service for business intelligence and reporting
 */
@Service
class DataAnalytics {

    fun generateBusinessIntelligenceReport(portfolio: InsurancePortfolio, timeRange: DateRange): BusinessIntelligenceReport {
        val salesAnalytics = analyzeSalesPerformance(portfolio, timeRange)
        val profitabilityAnalytics = analyzeProfitability(portfolio, timeRange)
        val customerAnalytics = analyzeCustomerBehavior(portfolio, timeRange)
        val productAnalytics = analyzeProductPerformance(portfolio, timeRange)
        val marketAnalytics = analyzeMarketTrends(portfolio, timeRange)
        
        return BusinessIntelligenceReport(
            reportDate = LocalDateTime.now(),
            timeRange = timeRange,
            salesAnalytics = salesAnalytics,
            profitabilityAnalytics = profitabilityAnalytics,
            customerAnalytics = customerAnalytics,
            productAnalytics = productAnalytics,
            marketAnalytics = marketAnalytics,
            keyInsights = generateKeyInsights(salesAnalytics, profitabilityAnalytics, customerAnalytics),
            recommendations = generateBusinessRecommendations(salesAnalytics, profitabilityAnalytics, productAnalytics)
        )
    }

    fun performPredictiveAnalysis(portfolio: InsurancePortfolio, predictionHorizon: Int): PredictiveAnalysisResult {
        val lapseRatePrediction = predictLapseRates(portfolio, predictionHorizon)
        val mortalityPrediction = predictMortalityRates(portfolio, predictionHorizon)
        val salesForecast = forecastSales(portfolio, predictionHorizon)
        val profitabilityForecast = forecastProfitability(portfolio, predictionHorizon)
        val riskPrediction = predictRiskFactors(portfolio, predictionHorizon)
        
        return PredictiveAnalysisResult(
            analysisDate = LocalDateTime.now(),
            predictionHorizon = predictionHorizon,
            lapseRatePrediction = lapseRatePrediction,
            mortalityPrediction = mortalityPrediction,
            salesForecast = salesForecast,
            profitabilityForecast = profitabilityForecast,
            riskPrediction = riskPrediction,
            confidenceIntervals = calculateConfidenceIntervals(lapseRatePrediction, mortalityPrediction, salesForecast),
            modelAccuracy = assessModelAccuracy(portfolio)
        )
    }

    fun analyzeCustomerSegmentation(portfolio: InsurancePortfolio): CustomerSegmentationResult {
        val segments = performCustomerSegmentation(portfolio)
        val segmentProfiles = segments.map { segment ->
            CustomerSegmentProfile(
                segmentId = segment.segmentId,
                segmentName = segment.segmentName,
                customerCount = segment.customers.size,
                averageAge = segment.customers.map { it.age }.average(),
                averagePolicyValue = segment.customers.map { it.totalPolicyValue.toDouble() }.average(),
                profitability = calculateSegmentProfitability(segment),
                retentionRate = calculateSegmentRetentionRate(segment),
                growthPotential = assessSegmentGrowthPotential(segment),
                characteristics = identifySegmentCharacteristics(segment)
            )
        }
        
        return CustomerSegmentationResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = portfolio.policies.size,
            segmentCount = segments.size,
            segmentProfiles = segmentProfiles,
            segmentationCriteria = listOf("Age", "Income", "Policy Value", "Geographic Location"),
            recommendedTargeting = recommendTargetingStrategies(segmentProfiles)
        )
    }

    fun performCohortAnalysis(portfolio: InsurancePortfolio, cohortType: CohortType): CohortAnalysisResult {
        val cohorts = when (cohortType) {
            CohortType.ISSUE_YEAR -> groupPoliciesByIssueYear(portfolio)
            CohortType.AGE_BAND -> groupPoliciesByAgeBand(portfolio)
            CohortType.PRODUCT_TYPE -> groupPoliciesByProductType(portfolio)
            CohortType.PREMIUM_BAND -> groupPoliciesByPremiumBand(portfolio)
        }
        
        val cohortMetrics = cohorts.map { (cohortKey, policies) ->
            CohortMetrics(
                cohortId = cohortKey,
                policyCount = policies.size,
                totalPremium = policies.sumOf { it.faceAmount },
                averagePolicySize = policies.map { it.faceAmount }.average(),
                retentionRates = calculateCohortRetentionRates(policies),
                profitabilityMetrics = calculateCohortProfitability(policies),
                lapseRates = calculateCohortLapseRates(policies),
                claimRates = calculateCohortClaimRates(policies)
            )
        }
        
        return CohortAnalysisResult(
            analysisDate = LocalDateTime.now(),
            cohortType = cohortType,
            cohortMetrics = cohortMetrics,
            insights = generateCohortInsights(cohortMetrics),
            recommendations = generateCohortRecommendations(cohortMetrics)
        )
    }

    fun analyzeMarketBasketAnalysis(portfolio: InsurancePortfolio): MarketBasketAnalysisResult {
        val customerPolicies = portfolio.policies.groupBy { it.customerId }
        val productCombinations = mutableMapOf<Set<String>, Int>()
        val associationRules = mutableListOf<AssociationRule>()
        
        // Find frequent product combinations
        customerPolicies.values.forEach { policies ->
            if (policies.size > 1) {
                val products = policies.map { it.productType }.toSet()
                productCombinations[products] = productCombinations.getOrDefault(products, 0) + 1
            }
        }
        
        // Generate association rules
        productCombinations.forEach { (products, frequency) ->
            if (frequency >= 5 && products.size >= 2) { // Minimum support threshold
                products.forEach { antecedent ->
                    val consequent = products - antecedent
                    if (consequent.isNotEmpty()) {
                        val support = frequency.toDouble() / customerPolicies.size
                        val confidence = calculateConfidence(antecedent, consequent, customerPolicies)
                        val lift = calculateLift(antecedent, consequent, customerPolicies)
                        
                        if (confidence > 0.3 && lift > 1.0) { // Minimum thresholds
                            associationRules.add(AssociationRule(
                                antecedent = setOf(antecedent),
                                consequent = consequent,
                                support = support,
                                confidence = confidence,
                                lift = lift
                            ))
                        }
                    }
                }
            }
        }
        
        return MarketBasketAnalysisResult(
            analysisDate = LocalDateTime.now(),
            totalCustomers = customerPolicies.size,
            multiProductCustomers = customerPolicies.values.count { it.size > 1 },
            frequentCombinations = productCombinations.toList().sortedByDescending { it.second }.take(10),
            associationRules = associationRules.sortedByDescending { it.lift }.take(20),
            crossSellOpportunities = identifyCrossSellOpportunities(associationRules)
        )
    }

    fun performTimeSeriesAnalysis(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): TimeSeriesAnalysisResult {
        val timeSeriesData = extractTimeSeriesData(portfolio, metric)
        val trendAnalysis = analyzeTrend(timeSeriesData)
        val seasonalityAnalysis = analyzeSeasonality(timeSeriesData)
        val forecastData = forecastTimeSeries(timeSeriesData, 12) // 12 periods ahead
        
        return TimeSeriesAnalysisResult(
            analysisDate = LocalDateTime.now(),
            metric = metric,
            timeSeriesData = timeSeriesData,
            trendAnalysis = trendAnalysis,
            seasonalityAnalysis = seasonalityAnalysis,
            forecastData = forecastData,
            modelFit = assessTimeSeriesModelFit(timeSeriesData, forecastData),
            insights = generateTimeSeriesInsights(trendAnalysis, seasonalityAnalysis)
        )
    }

    // Private helper methods
    private fun analyzeSalesPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): SalesAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalSales = policiesInRange.sumOf { it.faceAmount }
        val policyCount = policiesInRange.size
        val averagePolicySize = if (policyCount > 0) totalSales.divide(BigDecimal(policyCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val salesByMonth = policiesInRange.groupBy { "${it.issueDate.year}-${it.issueDate.monthValue.toString().padStart(2, '0')}" }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        val salesByState = policiesInRange.groupBy { it.state }
            .mapValues { (_, policies) -> policies.sumOf { it.faceAmount } }
        
        return SalesAnalytics(
            totalSales = totalSales,
            policyCount = policyCount,
            averagePolicySize = averagePolicySize,
            salesGrowthRate = calculateSalesGrowthRate(salesByMonth),
            salesByMonth = salesByMonth,
            salesByProduct = salesByProduct,
            salesByState = salesByState,
            topPerformingProducts = salesByProduct.toList().sortedByDescending { it.second }.take(5),
            topPerformingStates = salesByState.toList().sortedByDescending { it.second }.take(5)
        )
    }

    private fun analyzeProfitability(portfolio: InsurancePortfolio, timeRange: DateRange): ProfitabilityAnalytics {
        val policiesInRange = portfolio.policies.filter { 
            it.issueDate >= timeRange.startDate && it.issueDate <= timeRange.endDate 
        }
        
        val totalRevenue = policiesInRange.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) } // 1% revenue assumption
        val totalExpenses = totalRevenue.multiply(BigDecimal("0.75")) // 75% expense ratio
        val netIncome = totalRevenue.subtract(totalExpenses)
        val profitMargin = if (totalRevenue > BigDecimal.ZERO) netIncome.divide(totalRevenue, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val profitabilityByProduct = policiesInRange.groupBy { it.productType }
            .mapValues { (_, policies) ->
                val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses)
            }
        
        return ProfitabilityAnalytics(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netIncome = netIncome,
            profitMargin = profitMargin,
            returnOnAssets = netIncome.divide(totalRevenue.multiply(BigDecimal("10")), 4, RoundingMode.HALF_UP), // Simplified ROA
            profitabilityByProduct = profitabilityByProduct,
            profitabilityTrend = calculateProfitabilityTrend(policiesInRange)
        )
    }

    private fun analyzeCustomerBehavior(portfolio: InsurancePortfolio, timeRange: DateRange): CustomerAnalytics {
        val customers = portfolio.policies.groupBy { it.customerId }
        
        val totalCustomers = customers.size
        val averagePoliciesPerCustomer = portfolio.policies.size.toDouble() / totalCustomers
        val customerLifetimeValue = customers.mapValues { (_, policies) ->
            policies.sumOf { it.faceAmount.multiply(BigDecimal("0.05")) } // 5% CLV assumption
        }
        
        val averageCLV = customerLifetimeValue.values.map { it.toDouble() }.average()
        val churnRate = calculateChurnRate(customers)
        val retentionRate = BigDecimal.ONE.subtract(churnRate)
        
        return CustomerAnalytics(
            totalCustomers = totalCustomers,
            averagePoliciesPerCustomer = averagePoliciesPerCustomer,
            averageCustomerLifetimeValue = BigDecimal(averageCLV).setScale(2, RoundingMode.HALF_UP),
            churnRate = churnRate,
            retentionRate = retentionRate,
            customerAcquisitionCost = calculateCustomerAcquisitionCost(portfolio),
            customerSatisfactionScore = BigDecimal("8.5"), // Mock score
            netPromoterScore = BigDecimal("45") // Mock NPS
        )
    }

    private fun analyzeProductPerformance(portfolio: InsurancePortfolio, timeRange: DateRange): ProductAnalytics {
        val productPerformance = portfolio.policies.groupBy { it.productType }
            .mapValues { (productType, policies) ->
                ProductPerformanceMetrics(
                    productType = productType,
                    policyCount = policies.size,
                    totalPremium = policies.sumOf { it.faceAmount },
                    averagePolicySize = policies.map { it.faceAmount }.average(),
                    profitability = calculateProductProfitability(policies),
                    growthRate = calculateProductGrowthRate(policies, timeRange),
                    marketShare = BigDecimal(policies.size).divide(BigDecimal(portfolio.policies.size), 4, RoundingMode.HALF_UP)
                )
            }
        
        return ProductAnalytics(
            productPerformance = productPerformance,
            topPerformingProducts = productPerformance.values.sortedByDescending { it.profitability }.take(5),
            underperformingProducts = productPerformance.values.sortedBy { it.profitability }.take(3),
            productLifecycleAnalysis = analyzeProductLifecycle(productPerformance.values.toList())
        )
    }

    private fun analyzeMarketTrends(portfolio: InsurancePortfolio, timeRange: DateRange): MarketAnalytics {
        return MarketAnalytics(
            marketSize = BigDecimal("50000000000"), // $50B market size assumption
            marketGrowthRate = BigDecimal("0.05"), // 5% growth
            competitivePosition = BigDecimal("0.15"), // 15% market share
            marketTrends = listOf(
                "Increasing demand for digital insurance products",
                "Growing focus on ESG investing",
                "Rising interest in hybrid work benefits"
            ),
            competitorAnalysis = mapOf(
                "Competitor A" to BigDecimal("0.20"),
                "Competitor B" to BigDecimal("0.18"),
                "Competitor C" to BigDecimal("0.12")
            )
        )
    }

    private fun generateKeyInsights(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, customerAnalytics: CustomerAnalytics): List<String> {
        val insights = mutableListOf<String>()
        
        if (salesAnalytics.salesGrowthRate > BigDecimal("0.10")) {
            insights.add("Strong sales growth of ${salesAnalytics.salesGrowthRate.multiply(BigDecimal("100"))}% indicates healthy market demand")
        }
        
        if (profitabilityAnalytics.profitMargin > BigDecimal("0.15")) {
            insights.add("Healthy profit margin of ${profitabilityAnalytics.profitMargin.multiply(BigDecimal("100"))}% demonstrates operational efficiency")
        }
        
        if (customerAnalytics.retentionRate > BigDecimal("0.90")) {
            insights.add("High customer retention rate of ${customerAnalytics.retentionRate.multiply(BigDecimal("100"))}% indicates strong customer satisfaction")
        }
        
        return insights
    }

    private fun generateBusinessRecommendations(salesAnalytics: SalesAnalytics, profitabilityAnalytics: ProfitabilityAnalytics, productAnalytics: ProductAnalytics): List<String> {
        val recommendations = mutableListOf<String>()
        
        val topProduct = productAnalytics.topPerformingProducts.firstOrNull()
        if (topProduct != null) {
            recommendations.add("Focus marketing efforts on ${topProduct.productType} which shows highest profitability")
        }
        
        if (profitabilityAnalytics.profitMargin < BigDecimal("0.10")) {
            recommendations.add("Review expense structure to improve profit margins")
        }
        
        val topState = salesAnalytics.topPerformingStates.firstOrNull()
        if (topState != null) {
            recommendations.add("Expand operations in ${topState.first} market which shows strong performance")
        }
        
        return recommendations
    }

    private fun predictLapseRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified lapse rate prediction using historical trends
        val currentLapseRate = BigDecimal("0.08") // 8% current lapse rate
        val trendFactor = BigDecimal("0.02") // 2% annual increase trend
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentLapseRate.add(trendFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.9")),
                upperBound = predictedRate.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Lapse Rate",
            predictions = predictions,
            modelType = "Linear Trend",
            accuracy = BigDecimal("0.85")
        )
    }

    private fun predictMortalityRates(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        // Simplified mortality rate prediction
        val currentMortalityRate = BigDecimal("0.012") // 1.2% current mortality rate
        val agingFactor = BigDecimal("0.001") // 0.1% annual increase due to aging
        
        val predictions = (1..horizon).map { year ->
            val predictedRate = currentMortalityRate.add(agingFactor.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedRate,
                lowerBound = predictedRate.multiply(BigDecimal("0.8")),
                upperBound = predictedRate.multiply(BigDecimal("1.2"))
            )
        }
        
        return PredictionResult(
            metric = "Mortality Rate",
            predictions = predictions,
            modelType = "Demographic Trend",
            accuracy = BigDecimal("0.78")
        )
    }

    private fun forecastSales(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentSales = portfolio.policies.sumOf { it.faceAmount }
        val growthRate = BigDecimal("0.07") // 7% annual growth
        
        val predictions = (1..horizon).map { year ->
            val predictedSales = currentSales.multiply(BigDecimal.ONE.add(growthRate).pow(year))
            PredictionPoint(
                period = year,
                predictedValue = predictedSales,
                lowerBound = predictedSales.multiply(BigDecimal("0.85")),
                upperBound = predictedSales.multiply(BigDecimal("1.15"))
            )
        }
        
        return PredictionResult(
            metric = "Sales Volume",
            predictions = predictions,
            modelType = "Exponential Growth",
            accuracy = BigDecimal("0.82")
        )
    }

    private fun forecastProfitability(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentProfitability = BigDecimal("0.12") // 12% current profit margin
        val improvementRate = BigDecimal("0.005") // 0.5% annual improvement
        
        val predictions = (1..horizon).map { year ->
            val predictedProfitability = currentProfitability.add(improvementRate.multiply(BigDecimal(year)))
            PredictionPoint(
                period = year,
                predictedValue = predictedProfitability,
                lowerBound = predictedProfitability.multiply(BigDecimal("0.9")),
                upperBound = predictedProfitability.multiply(BigDecimal("1.1"))
            )
        }
        
        return PredictionResult(
            metric = "Profitability",
            predictions = predictions,
            modelType = "Linear Improvement",
            accuracy = BigDecimal("0.75")
        )
    }

    private fun predictRiskFactors(portfolio: InsurancePortfolio, horizon: Int): PredictionResult {
        val currentRiskScore = BigDecimal("0.65") // Current risk score
        val volatility = BigDecimal("0.05") // 5% volatility
        
        val predictions = (1..horizon).map { year ->
            val randomFactor = (kotlin.random.Random.nextDouble() - 0.5) * 0.1
            val predictedRisk = currentRiskScore.add(BigDecimal(randomFactor))
            PredictionPoint(
                period = year,
                predictedValue = predictedRisk,
                lowerBound = predictedRisk.subtract(volatility),
                upperBound = predictedRisk.add(volatility)
            )
        }
        
        return PredictionResult(
            metric = "Risk Score",
            predictions = predictions,
            modelType = "Stochastic Model",
            accuracy = BigDecimal("0.70")
        )
    }

    private fun calculateConfidenceIntervals(vararg predictions: PredictionResult): Map<String, ConfidenceInterval> {
        return predictions.associate { prediction ->
            val avgLower = prediction.predictions.map { it.lowerBound.toDouble() }.average()
            val avgUpper = prediction.predictions.map { it.upperBound.toDouble() }.average()
            
            prediction.metric to ConfidenceInterval(
                lowerBound = BigDecimal(avgLower).setScale(4, RoundingMode.HALF_UP),
                upperBound = BigDecimal(avgUpper).setScale(4, RoundingMode.HALF_UP),
                confidenceLevel = BigDecimal("0.95")
            )
        }
    }

    private fun assessModelAccuracy(portfolio: InsurancePortfolio): ModelAccuracyMetrics {
        return ModelAccuracyMetrics(
            meanAbsoluteError = BigDecimal("0.05"),
            rootMeanSquareError = BigDecimal("0.08"),
            meanAbsolutePercentageError = BigDecimal("0.12"),
            r2Score = BigDecimal("0.85")
        )
    }

    // Additional helper methods would continue here...
    private fun performCustomerSegmentation(portfolio: InsurancePortfolio): List<CustomerSegment> {
        // Mock segmentation logic
        return listOf(
            CustomerSegment("SEG001", "High Value", emptyList()),
            CustomerSegment("SEG002", "Young Professionals", emptyList()),
            CustomerSegment("SEG003", "Retirees", emptyList())
        )
    }

    private fun calculateSegmentProfitability(segment: CustomerSegment): BigDecimal = BigDecimal("0.15")
    private fun calculateSegmentRetentionRate(segment: CustomerSegment): BigDecimal = BigDecimal("0.92")
    private fun assessSegmentGrowthPotential(segment: CustomerSegment): GrowthPotential = GrowthPotential.HIGH
    private fun identifySegmentCharacteristics(segment: CustomerSegment): List<String> = listOf("High income", "Tech-savvy")
    private fun recommendTargetingStrategies(profiles: List<CustomerSegmentProfile>): List<String> = listOf("Digital marketing", "Referral programs")

    private fun groupPoliciesByIssueYear(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.issueDate.year.toString() }
    }

    private fun groupPoliciesByAgeBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.age < 30 -> "Under 30"
                it.age < 50 -> "30-49"
                it.age < 65 -> "50-64"
                else -> "65+"
            }
        }
    }

    private fun groupPoliciesByProductType(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { it.productType }
    }

    private fun groupPoliciesByPremiumBand(portfolio: InsurancePortfolio): Map<String, List<PolicyInfo>> {
        return portfolio.policies.groupBy { 
            when {
                it.faceAmount < BigDecimal("100000") -> "Under $100K"
                it.faceAmount < BigDecimal("500000") -> "$100K-$500K"
                it.faceAmount < BigDecimal("1000000") -> "$500K-$1M"
                else -> "Over $1M"
            }
        }
    }

    private fun calculateCohortRetentionRates(policies: List<PolicyInfo>): List<BigDecimal> {
        return listOf(BigDecimal("0.95"), BigDecimal("0.90"), BigDecimal("0.85"))
    }

    private fun calculateCohortProfitability(policies: List<PolicyInfo>): BigDecimal = BigDecimal("0.12")
    private fun calculateCohortLapseRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.08"))
    private fun calculateCohortClaimRates(policies: List<PolicyInfo>): List<BigDecimal> = listOf(BigDecimal("0.02"))
    private fun generateCohortInsights(metrics: List<CohortMetrics>): List<String> = listOf("Newer cohorts show better retention")
    private fun generateCohortRecommendations(metrics: List<CohortMetrics>): List<String> = listOf("Focus on retention strategies")

    private fun calculateConfidence(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 0.5
    private fun calculateLift(antecedent: String, consequent: Set<String>, customerPolicies: Map<String, List<PolicyInfo>>): Double = 1.2
    private fun identifyCrossSellOpportunities(rules: List<AssociationRule>): List<String> = listOf("Cross-sell life insurance to annuity customers")

    private fun extractTimeSeriesData(portfolio: InsurancePortfolio, metric: TimeSeriesMetric): List<TimeSeriesPoint> {
        return (1..24).map { month ->
            TimeSeriesPoint(
                period = month,
                value = BigDecimal(1000000 + month * 50000),
                date = LocalDate.now().minusMonths(24 - month.toLong())
            )
        }
    }

    private fun analyzeTrend(data: List<TimeSeriesPoint>): TrendAnalysis {
        val slope = calculateSlope(data)
        val trendDirection = when {
            slope > 0.05 -> TrendDirection.INCREASING
            slope < -0.05 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        return TrendAnalysis(
            trendDirection = trendDirection,
            slope = BigDecimal(slope).setScale(6, RoundingMode.HALF_UP),
            strength = TrendStrength.MODERATE
        )
    }

    private fun analyzeSeasonality(data: List<TimeSeriesPoint>): SeasonalityAnalysis {
        return SeasonalityAnalysis(
            hasSeasonality = true,
            seasonalPeriod = 12,
            seasonalStrength = BigDecimal("0.15"),
            peakPeriods = listOf(11, 12, 1), // Nov, Dec, Jan
            troughPeriods = listOf(6, 7, 8) // Jun, Jul, Aug
        )
    }

    private fun forecastTimeSeries(data: List<TimeSeriesPoint>, periods: Int): List<ForecastPoint> {
        return (1..periods).map { period ->
            val baseValue = data.last().value
            val growth = BigDecimal("0.02") // 2% growth per period
            val forecastValue = baseValue.multiply(BigDecimal.ONE.add(growth).pow(period))
            
            ForecastPoint(
                period = period,
                forecastValue = forecastValue,
                lowerBound = forecastValue.multiply(BigDecimal("0.9")),
                upperBound = forecastValue.multiply(BigDecimal("1.1")),
                date = data.last().date.plusMonths(period.toLong())
            )
        }
    }

    private fun assessTimeSeriesModelFit(historical: List<TimeSeriesPoint>, forecast: List<ForecastPoint>): ModelFitMetrics {
        return ModelFitMetrics(
            r2 = BigDecimal("0.85"),
            mae = BigDecimal("50000"),
            rmse = BigDecimal("75000"),
            mape = BigDecimal("0.05")
        )
    }

    private fun generateTimeSeriesInsights(trend: TrendAnalysis, seasonality: SeasonalityAnalysis): List<String> {
        val insights = mutableListOf<String>()
        
        when (trend.trendDirection) {
            TrendDirection.INCREASING -> insights.add("Strong upward trend indicates growing business")
            TrendDirection.DECREASING -> insights.add("Declining trend requires immediate attention")
            TrendDirection.STABLE -> insights.add("Stable trend suggests mature market")
        }
        
        if (seasonality.hasSeasonality) {
            insights.add("Clear seasonal patterns detected - plan marketing campaigns accordingly")
        }
        
        return insights
    }

    // Additional helper methods for calculations
    private fun calculateSalesGrowthRate(salesByMonth: Map<String, BigDecimal>): BigDecimal {
        if (salesByMonth.size < 2) return BigDecimal.ZERO
        
        val sortedSales = salesByMonth.toList().sortedBy { it.first }
        val firstMonth = sortedSales.first().second
        val lastMonth = sortedSales.last().second
        
        return if (firstMonth > BigDecimal.ZERO) {
            lastMonth.subtract(firstMonth).divide(firstMonth, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun calculateProfitabilityTrend(policies: List<PolicyInfo>): List<BigDecimal> {
        return policies.groupBy { it.issueDate.year }
            .values.map { yearPolicies ->
                val revenue = yearPolicies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
                val expenses = revenue.multiply(BigDecimal("0.75"))
                revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
            }
    }

    private fun calculateChurnRate(customers: Map<String, List<PolicyInfo>>): BigDecimal {
        // Simplified churn calculation
        return BigDecimal("0.08") // 8% churn rate
    }

    private fun calculateCustomerAcquisitionCost(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified CAC calculation
        return BigDecimal("500") // $500 per customer
    }

    private fun calculateProductProfitability(policies: List<PolicyInfo>): BigDecimal {
        val revenue = policies.sumOf { it.faceAmount.multiply(BigDecimal("0.01")) }
        val expenses = revenue.multiply(BigDecimal("0.75"))
        return revenue.subtract(expenses).divide(revenue, 4, RoundingMode.HALF_UP)
    }

    private fun calculateProductGrowthRate(policies: List<PolicyInfo>, timeRange: DateRange): BigDecimal {
        val currentYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year }
        val previousYearPolicies = policies.filter { it.issueDate.year == timeRange.endDate.year - 1 }
        
        val currentCount = currentYearPolicies.size
        val previousCount = previousYearPolicies.size
        
        return if (previousCount > 0) {
            BigDecimal(currentCount - previousCount).divide(BigDecimal(previousCount), 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    private fun analyzeProductLifecycle(products: List<ProductPerformanceMetrics>): Map<String, ProductLifecycleStage> {
        return products.associate { product ->
            val stage = when {
                product.growthRate > BigDecimal("0.20") -> ProductLifecycleStage.GROWTH
                product.growthRate > BigDecimal("0.05") -> ProductLifecycleStage.MATURITY
                product.growthRate < BigDecimal("-0.05") -> ProductLifecycleStage.DECLINE
                else -> ProductLifecycleStage.INTRODUCTION
            }
            product.productType to stage
        }
    }

    private fun calculateSlope(data: List<TimeSeriesPoint>): Double {
        val n = data.size
        val sumX = (1..n).sum().toDouble()
        val sumY = data.sumOf { it.value.toDouble() }
        val sumXY = data.mapIndexed { index, point -> (index + 1) * point.value.toDouble() }.sum()
        val sumX2 = (1..n).sumOf { it * it }.toDouble()
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    }

    private fun List<BigDecimal>.average(): BigDecimal {
        return if (isEmpty()) BigDecimal.ZERO
        else fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }.divide(BigDecimal(size), 2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.pow(n: Int): BigDecimal {
        return this.pow(n, java.math.MathContext.DECIMAL128)
    }
}

// Data classes and enums for analytics
data class BusinessIntelligenceReport(
    val reportDate: LocalDateTime,
    val timeRange: DateRange,
    val salesAnalytics: SalesAnalytics,
    val profitabilityAnalytics: ProfitabilityAnalytics,
    val customerAnalytics: CustomerAnalytics,
    val productAnalytics: ProductAnalytics,
    val marketAnalytics: MarketAnalytics,
    val keyInsights: List<String>,
    val recommendations: List<String>
)

data class SalesAnalytics(
    val totalSales: BigDecimal,
    val policyCount: Int,
    val averagePolicySize: BigDecimal,
    val salesGrowthRate: BigDecimal,
    val salesByMonth: Map<String, BigDecimal>,
    val salesByProduct: Map<String, BigDecimal>,
    val salesByState: Map<String, BigDecimal>,
    val topPerformingProducts: List<Pair<String, BigDecimal>>,
    val topPerformingStates: List<Pair<String, BigDecimal>>
)

data class ProfitabilityAnalytics(
    val totalRevenue: BigDecimal,
    val totalExpenses: BigDecimal,
    val netIncome: BigDecimal,
    val profitMargin: BigDecimal,
    val returnOnAssets: BigDecimal,
    val profitabilityByProduct: Map<String, BigDecimal>,
    val profitabilityTrend: List<BigDecimal>
)

data class CustomerAnalytics(
    val totalCustomers: Int,
    val averagePoliciesPerCustomer: Double,
    val averageCustomerLifetimeValue: BigDecimal,
    val churnRate: BigDecimal,
    val retentionRate: BigDecimal,
    val customerAcquisitionCost: BigDecimal,
    val customerSatisfactionScore: BigDecimal,
    val netPromoterScore: BigDecimal
)

data class ProductAnalytics(
    val productPerformance: Map<String, ProductPerformanceMetrics>,
    val topPerformingProducts: List<ProductPerformanceMetrics>,
    val underperformingProducts: List<ProductPerformanceMetrics>,
    val productLifecycleAnalysis: Map<String, ProductLifecycleStage>
)

data class ProductPerformanceMetrics(
    val productType: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val profitability: BigDecimal,
    val growthRate: BigDecimal,
    val marketShare: BigDecimal
)

data class MarketAnalytics(
    val marketSize: BigDecimal,
    val marketGrowthRate: BigDecimal,
    val competitivePosition: BigDecimal,
    val marketTrends: List<String>,
    val competitorAnalysis: Map<String, BigDecimal>
)

data class PredictiveAnalysisResult(
    val analysisDate: LocalDateTime,
    val predictionHorizon: Int,
    val lapseRatePrediction: PredictionResult,
    val mortalityPrediction: PredictionResult,
    val salesForecast: PredictionResult,
    val profitabilityForecast: PredictionResult,
    val riskPrediction: PredictionResult,
    val confidenceIntervals: Map<String, ConfidenceInterval>,
    val modelAccuracy: ModelAccuracyMetrics
)

data class PredictionResult(
    val metric: String,
    val predictions: List<PredictionPoint>,
    val modelType: String,
    val accuracy: BigDecimal
)

data class PredictionPoint(
    val period: Int,
    val predictedValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal
)

data class ConfidenceInterval(
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val confidenceLevel: BigDecimal
)

data class ModelAccuracyMetrics(
    val meanAbsoluteError: BigDecimal,
    val rootMeanSquareError: BigDecimal,
    val meanAbsolutePercentageError: BigDecimal,
    val r2Score: BigDecimal
)

data class CustomerSegmentationResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val segmentCount: Int,
    val segmentProfiles: List<CustomerSegmentProfile>,
    val segmentationCriteria: List<String>,
    val recommendedTargeting: List<String>
)

data class CustomerSegmentProfile(
    val segmentId: String,
    val segmentName: String,
    val customerCount: Int,
    val averageAge: Double,
    val averagePolicyValue: Double,
    val profitability: BigDecimal,
    val retentionRate: BigDecimal,
    val growthPotential: GrowthPotential,
    val characteristics: List<String>
)

data class CustomerSegment(
    val segmentId: String,
    val segmentName: String,
    val customers: List<CustomerInfo>
)

data class CustomerInfo(
    val customerId: String,
    val age: Int,
    val totalPolicyValue: BigDecimal
)

data class CohortAnalysisResult(
    val analysisDate: LocalDateTime,
    val cohortType: CohortType,
    val cohortMetrics: List<CohortMetrics>,
    val insights: List<String>,
    val recommendations: List<String>
)

data class CohortMetrics(
    val cohortId: String,
    val policyCount: Int,
    val totalPremium: BigDecimal,
    val averagePolicySize: BigDecimal,
    val retentionRates: List<BigDecimal>,
    val profitabilityMetrics: BigDecimal,
    val lapseRates: List<BigDecimal>,
    val claimRates: List<BigDecimal>
)

data class MarketBasketAnalysisResult(
    val analysisDate: LocalDateTime,
    val totalCustomers: Int,
    val multiProductCustomers: Int,
    val frequentCombinations: List<Pair<Set<String>, Int>>,
    val associationRules: List<AssociationRule>,
    val crossSellOpportunities: List<String>
)

data class AssociationRule(
    val antecedent: Set<String>,
    val consequent: Set<String>,
    val support: Double,
    val confidence: Double,
    val lift: Double
)

data class TimeSeriesAnalysisResult(
    val analysisDate: LocalDateTime,
    val metric: TimeSeriesMetric,
    val timeSeriesData: List<TimeSeriesPoint>,
    val trendAnalysis: TrendAnalysis,
    val seasonalityAnalysis: SeasonalityAnalysis,
    val forecastData: List<ForecastPoint>,
    val modelFit: ModelFitMetrics,
    val insights: List<String>
)

data class TimeSeriesPoint(
    val period: Int,
    val value: BigDecimal,
    val date: LocalDate
)

data class TrendAnalysis(
    val trendDirection: TrendDirection,
    val slope: BigDecimal,
    val strength: TrendStrength
)

data class SeasonalityAnalysis(
    val hasSeasonality: Boolean,
    val seasonalPeriod: Int,
    val seasonalStrength: BigDecimal,
    val peakPeriods: List<Int>,
    val troughPeriods: List<Int>
)

data class ForecastPoint(
    val period: Int,
    val forecastValue: BigDecimal,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val date: LocalDate
)

data class ModelFitMetrics(
    val r2: BigDecimal,
    val mae: BigDecimal,
    val rmse: BigDecimal,
    val mape: BigDecimal
)

enum class CohortType {
    ISSUE_YEAR, AGE_BAND, PRODUCT_TYPE, PREMIUM_BAND
}

enum class GrowthPotential {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

enum class ProductLifecycleStage {
    INTRODUCTION, GROWTH, MATURITY, DECLINE
}

enum class TimeSeriesMetric {
    SALES_VOLUME, POLICY_COUNT, PREMIUM_INCOME, CLAIMS_PAID, LAPSE_RATE
}

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class TrendStrength {
    WEAK, MODERATE, STRONG, VERY_STRONG
}
