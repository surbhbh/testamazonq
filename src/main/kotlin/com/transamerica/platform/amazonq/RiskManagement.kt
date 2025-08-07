package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Risk management service for insurance operations
 * Handles portfolio risk analysis, concentration limits, and regulatory compliance
 */
@Service
class RiskManagement {

    /**
     * Analyzes portfolio concentration risk
     */
    fun analyzeConcentrationRisk(portfolio: InsurancePortfolio): ConcentrationRiskAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        
        // Geographic concentration
        val geographicConcentration = analyzeGeographicConcentration(portfolio.policies, totalExposure)
        
        // Industry concentration
        val industryConcentration = analyzeIndustryConcentration(portfolio.policies, totalExposure)
        
        // Age band concentration
        val ageBandConcentration = analyzeAgeBandConcentration(portfolio.policies, totalExposure)
        
        // Product concentration
        val productConcentration = analyzeProductConcentration(portfolio.policies, totalExposure)
        
        // Large policy concentration
        val largePolicyConcentration = analyzeLargePolicyConcentration(portfolio.policies, totalExposure)
        
        val overallRiskScore = calculateOverallConcentrationRisk(
            geographicConcentration,
            industryConcentration,
            ageBandConcentration,
            productConcentration,
            largePolicyConcentration
        )
        
        return ConcentrationRiskAnalysis(
            totalExposure = totalExposure,
            geographicConcentration = geographicConcentration,
            industryConcentration = industryConcentration,
            ageBandConcentration = ageBandConcentration,
            productConcentration = productConcentration,
            largePolicyConcentration = largePolicyConcentration,
            overallRiskScore = overallRiskScore,
            riskLevel = determineRiskLevel(overallRiskScore),
            recommendations = generateConcentrationRecommendations(overallRiskScore, geographicConcentration, industryConcentration)
        )
    }

    /**
     * Calculates Value at Risk (VaR) for the portfolio
     */
    fun calculateValueAtRisk(portfolio: InsurancePortfolio, confidenceLevel: Double, timeHorizon: Int): ValueAtRiskResult {
        val returns = simulatePortfolioReturns(portfolio, 10000, timeHorizon)
        returns.sort()
        
        val varIndex = ((1 - confidenceLevel) * returns.size).toInt()
        val var95 = returns[varIndex]
        val var99 = returns[((1 - 0.99) * returns.size).toInt()]
        
        // Expected Shortfall (Conditional VaR)
        val expectedShortfall = returns.take(varIndex).average()
        
        return ValueAtRiskResult(
            confidenceLevel = confidenceLevel,
            timeHorizon = timeHorizon,
            valueAtRisk = BigDecimal(var95).setScale(2, RoundingMode.HALF_UP),
            var99 = BigDecimal(var99).setScale(2, RoundingMode.HALF_UP),
            expectedShortfall = BigDecimal(expectedShortfall).setScale(2, RoundingMode.HALF_UP),
            portfolioValue = portfolio.policies.sumOf { it.faceAmount }
        )
    }

    /**
     * Performs stress testing on the portfolio
     */
    fun performStressTesting(portfolio: InsurancePortfolio, stressScenarios: List<StressScenario>): StressTestResult {
        val baselineValue = portfolio.policies.sumOf { it.faceAmount }
        val scenarioResults = mutableListOf<ScenarioResult>()
        
        stressScenarios.forEach { scenario ->
            val stressedValue = applyStressScenario(portfolio, scenario)
            val impact = stressedValue.subtract(baselineValue)
            val impactPercentage = impact.divide(baselineValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            
            scenarioResults.add(
                ScenarioResult(
                    scenario = scenario,
                    baselineValue = baselineValue,
                    stressedValue = stressedValue,
                    impact = impact,
                    impactPercentage = impactPercentage
                )
            )
        }
        
        val worstCaseScenario = scenarioResults.minByOrNull { it.impact }
        val averageImpact = scenarioResults.map { it.impact }.fold(BigDecimal.ZERO) { acc, impact -> acc.add(impact) }
            .divide(BigDecimal(scenarioResults.size), 2, RoundingMode.HALF_UP)
        
        return StressTestResult(
            baselineValue = baselineValue,
            scenarioResults = scenarioResults,
            worstCaseScenario = worstCaseScenario,
            averageImpact = averageImpact,
            stressTestDate = LocalDateTime.now()
        )
    }

    /**
     * Monitors regulatory capital requirements
     */
    fun monitorCapitalRequirements(portfolio: InsurancePortfolio, capitalBase: BigDecimal): CapitalAdequacyResult {
        // Risk-Based Capital (RBC) calculation
        val c0Risk = calculateAssetRisk(portfolio) // Asset/affiliate risk
        val c1Risk = calculateInsuranceRisk(portfolio) // Insurance risk
        val c2Risk = calculateCreditRisk(portfolio) // Credit risk
        val c3Risk = calculateMarketRisk(portfolio) // Market risk
        val c4Risk = calculateBusinessRisk(portfolio) // Business risk
        
        val totalRisk = sqrt(
            (c0Risk + c1Risk).pow(2) + c2Risk.pow(2) + c3Risk.pow(2) + c4Risk.pow(2)
        )
        
        val authorizedControlLevel = totalRisk * 2.0
        val companyActionLevel = totalRisk * 1.5
        val regulatoryActionLevel = totalRisk * 1.0
        val mandatoryControlLevel = totalRisk * 0.7
        
        val rbcRatio = capitalBase.toDouble() / authorizedControlLevel
        
        val capitalAdequacy = when {
            rbcRatio >= 2.0 -> CapitalAdequacy.WELL_CAPITALIZED
            rbcRatio >= 1.5 -> CapitalAdequacy.ADEQUATELY_CAPITALIZED
            rbcRatio >= 1.0 -> CapitalAdequacy.COMPANY_ACTION_LEVEL
            rbcRatio >= 0.7 -> CapitalAdequacy.REGULATORY_ACTION_LEVEL
            else -> CapitalAdequacy.MANDATORY_CONTROL_LEVEL
        }
        
        return CapitalAdequacyResult(
            capitalBase = capitalBase,
            totalRiskBasedCapital = BigDecimal(totalRisk).setScale(2, RoundingMode.HALF_UP),
            rbcRatio = BigDecimal(rbcRatio).setScale(4, RoundingMode.HALF_UP),
            capitalAdequacy = capitalAdequacy,
            authorizedControlLevel = BigDecimal(authorizedControlLevel).setScale(2, RoundingMode.HALF_UP),
            companyActionLevel = BigDecimal(companyActionLevel).setScale(2, RoundingMode.HALF_UP),
            regulatoryActionLevel = BigDecimal(regulatoryActionLevel).setScale(2, RoundingMode.HALF_UP),
            mandatoryControlLevel = BigDecimal(mandatoryControlLevel).setScale(2, RoundingMode.HALF_UP),
            riskComponents = mapOf(
                "C0_ASSET_RISK" to BigDecimal(c0Risk).setScale(2, RoundingMode.HALF_UP),
                "C1_INSURANCE_RISK" to BigDecimal(c1Risk).setScale(2, RoundingMode.HALF_UP),
                "C2_CREDIT_RISK" to BigDecimal(c2Risk).setScale(2, RoundingMode.HALF_UP),
                "C3_MARKET_RISK" to BigDecimal(c3Risk).setScale(2, RoundingMode.HALF_UP),
                "C4_BUSINESS_RISK" to BigDecimal(c4Risk).setScale(2, RoundingMode.HALF_UP)
            )
        )
    }

    /**
     * Analyzes reinsurance needs and optimization
     */
    fun analyzeReinsuranceNeeds(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val largePolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        
        val excessExposure = largePolicies.sumOf { policy ->
            policy.faceAmount.subtract(retentionLimits.perLifeLimit).max(BigDecimal.ZERO)
        }
        
        val catastropheExposure = analyzeCatastropheExposure(portfolio)
        val aggregateExposure = totalExposure.subtract(retentionLimits.aggregateLimit).max(BigDecimal.ZERO)
        
        val recommendedQuotaShare = calculateOptimalQuotaShare(portfolio, retentionLimits)
        val recommendedSurplus = calculateOptimalSurplus(portfolio, retentionLimits)
        val recommendedCatastrophe = calculateCatastropheReinsurance(catastropheExposure)
        
        val totalReinsuranceCost = recommendedQuotaShare.cost
            .add(recommendedSurplus.cost)
            .add(recommendedCatastrophe.cost)
        
        val netRetention = totalExposure.subtract(excessExposure).subtract(aggregateExposure)
        val reinsuranceUtilization = excessExposure.add(aggregateExposure).divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        return ReinsuranceAnalysis(
            totalExposure = totalExposure,
            netRetention = netRetention,
            excessExposure = excessExposure,
            catastropheExposure = catastropheExposure,
            aggregateExposure = aggregateExposure,
            reinsuranceUtilization = reinsuranceUtilization,
            recommendedQuotaShare = recommendedQuotaShare,
            recommendedSurplus = recommendedSurplus,
            recommendedCatastrophe = recommendedCatastrophe,
            totalReinsuranceCost = totalReinsuranceCost,
            costEfficiencyRatio = totalReinsuranceCost.divide(excessExposure.add(aggregateExposure), 4, RoundingMode.HALF_UP)
        )
    }

    /**
     * Performs early warning system analysis
     */
    fun performEarlyWarningAnalysis(portfolio: InsurancePortfolio, financialMetrics: FinancialMetrics): EarlyWarningResult {
        val warnings = mutableListOf<EarlyWarning>()
        
        // Liquidity warnings
        if (financialMetrics.liquidityRatio < BigDecimal("1.2")) {
            warnings.add(EarlyWarning(
                type = WarningType.LIQUIDITY,
                severity = WarningSeverity.HIGH,
                description = "Liquidity ratio below minimum threshold",
                threshold = BigDecimal("1.2"),
                actualValue = financialMetrics.liquidityRatio
            ))
        }
        
        // Profitability warnings
        if (financialMetrics.returnOnEquity < BigDecimal("0.08")) {
            warnings.add(EarlyWarning(
                type = WarningType.PROFITABILITY,
                severity = WarningSeverity.MEDIUM,
                description = "Return on equity below target",
                threshold = BigDecimal("0.08"),
                actualValue = financialMetrics.returnOnEquity
            ))
        }
        
        // Growth warnings
        val growthRate = calculatePortfolioGrowthRate(portfolio)
        if (growthRate > BigDecimal("0.25")) {
            warnings.add(EarlyWarning(
                type = WarningType.GROWTH,
                severity = WarningSeverity.MEDIUM,
                description = "Portfolio growth rate exceeds prudent limits",
                threshold = BigDecimal("0.25"),
                actualValue = growthRate
            ))
        }
        
        // Concentration warnings
        val concentrationRisk = analyzeConcentrationRisk(portfolio)
        if (concentrationRisk.overallRiskScore > BigDecimal("0.75")) {
            warnings.add(EarlyWarning(
                type = WarningType.CONCENTRATION,
                severity = WarningSeverity.HIGH,
                description = "Portfolio concentration risk exceeds acceptable levels",
                threshold = BigDecimal("0.75"),
                actualValue = concentrationRisk.overallRiskScore
            ))
        }
        
        // Mortality experience warnings
        val mortalityRatio = calculateMortalityRatio(portfolio)
        if (mortalityRatio > BigDecimal("1.15")) {
            warnings.add(EarlyWarning(
                type = WarningType.MORTALITY,
                severity = WarningSeverity.HIGH,
                description = "Actual mortality exceeds expected by significant margin",
                threshold = BigDecimal("1.15"),
                actualValue = mortalityRatio
            ))
        }
        
        val overallRiskLevel = determineOverallRiskLevel(warnings)
        
        return EarlyWarningResult(
            analysisDate = LocalDateTime.now(),
            warnings = warnings,
            overallRiskLevel = overallRiskLevel,
            recommendedActions = generateRecommendedActions(warnings),
            nextReviewDate = LocalDateTime.now().plusMonths(1)
        )
    }

    /**
     * Calculates economic capital requirements
     */
    fun calculateEconomicCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): EconomicCapitalResult {
        // Insurance risk capital
        val insuranceRiskCapital = calculateInsuranceRiskCapital(portfolio, confidenceLevel)
        
        // Market risk capital
        val marketRiskCapital = calculateMarketRiskCapital(portfolio, confidenceLevel)
        
        // Credit risk capital
        val creditRiskCapital = calculateCreditRiskCapital(portfolio, confidenceLevel)
        
        // Operational risk capital
        val operationalRiskCapital = calculateOperationalRiskCapital(portfolio, confidenceLevel)
        
        // Diversification benefit
        val diversificationBenefit = calculateDiversificationBenefit(
            insuranceRiskCapital, marketRiskCapital, creditRiskCapital, operationalRiskCapital
        )
        
        val totalEconomicCapital = insuranceRiskCapital + marketRiskCapital + creditRiskCapital + operationalRiskCapital - diversificationBenefit
        
        return EconomicCapitalResult(
            confidenceLevel = confidenceLevel,
            insuranceRiskCapital = BigDecimal(insuranceRiskCapital).setScale(2, RoundingMode.HALF_UP),
            marketRiskCapital = BigDecimal(marketRiskCapital).setScale(2, RoundingMode.HALF_UP),
            creditRiskCapital = BigDecimal(creditRiskCapital).setScale(2, RoundingMode.HALF_UP),
            operationalRiskCapital = BigDecimal(operationalRiskCapital).setScale(2, RoundingMode.HALF_UP),
            diversificationBenefit = BigDecimal(diversificationBenefit).setScale(2, RoundingMode.HALF_UP),
            totalEconomicCapital = BigDecimal(totalEconomicCapital).setScale(2, RoundingMode.HALF_UP),
            capitalComponents = mapOf(
                "INSURANCE_RISK" to BigDecimal(insuranceRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "MARKET_RISK" to BigDecimal(marketRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "CREDIT_RISK" to BigDecimal(creditRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "OPERATIONAL_RISK" to BigDecimal(operationalRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP)
            )
        )
    }

    // Private helper methods
    private fun analyzeGeographicConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val stateExposure = policies.groupBy { it.state }
            .mapValues { (_, statePolicies) -> statePolicies.sumOf { it.faceAmount } }
        
        val maxStateExposure = stateExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxStateExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = stateExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = stateExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeIndustryConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val industryExposure = policies.groupBy { it.industry }
            .mapValues { (_, industryPolicies) -> industryPolicies.sumOf { it.faceAmount } }
        
        val maxIndustryExposure = industryExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxIndustryExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = industryExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = industryExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeAgeBandConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val ageBandExposure = policies.groupBy { getAgeBand(it.age) }
            .mapValues { (_, agePolicies) -> agePolicies.sumOf { it.faceAmount } }
        
        val maxAgeBandExposure = ageBandExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxAgeBandExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = ageBandExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = ageBandExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeProductConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val productExposure = policies.groupBy { it.productType }
            .mapValues { (_, productPolicies) -> productPolicies.sumOf { it.faceAmount } }
        
        val maxProductExposure = productExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxProductExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = productExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = productExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeLargePolicyConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val largePolicyThreshold = BigDecimal("1000000") // $1M threshold
        val largePolicies = policies.filter { it.faceAmount >= largePolicyThreshold }
        val largePolicyExposure = largePolicies.sumOf { it.faceAmount }
        
        val concentrationRatio = largePolicyExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        // For large policies, we calculate based on individual policy sizes
        val herfindahlIndex = policies.sumOf { policy ->
            val ratio = policy.faceAmount.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        val topPolicies = policies.sortedByDescending { it.faceAmount }.take(10)
            .map { "${it.policyNumber}" to it.faceAmount }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = topPolicies,
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun calculateOverallConcentrationRisk(vararg concentrations: ConcentrationMetric): BigDecimal {
        val weights = listOf(0.25, 0.20, 0.15, 0.20, 0.20) // Geographic, Industry, Age, Product, Large Policy
        
        return concentrations.mapIndexed { index, metric ->
            metric.riskScore.multiply(BigDecimal(weights[index]))
        }.fold(BigDecimal.ZERO) { acc, score -> acc.add(score) }
    }

    private fun calculateConcentrationRiskScore(concentrationRatio: BigDecimal, herfindahlIndex: BigDecimal): BigDecimal {
        val ratioScore = when {
            concentrationRatio > BigDecimal("0.30") -> BigDecimal("1.0")
            concentrationRatio > BigDecimal("0.20") -> BigDecimal("0.7")
            concentrationRatio > BigDecimal("0.10") -> BigDecimal("0.4")
            else -> BigDecimal("0.1")
        }
        
        val herfindahlScore = when {
            herfindahlIndex > BigDecimal("0.25") -> BigDecimal("1.0")
            herfindahlIndex > BigDecimal("0.15") -> BigDecimal("0.6")
            herfindahlIndex > BigDecimal("0.10") -> BigDecimal("0.3")
            else -> BigDecimal("0.1")
        }
        
        return ratioScore.add(herfindahlScore).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
    }

    private fun getAgeBand(age: Int): String {
        return when {
            age < 30 -> "Under 30"
            age < 40 -> "30-39"
            age < 50 -> "40-49"
            age < 60 -> "50-59"
            age < 70 -> "60-69"
            else -> "70+"
        }
    }

    private fun determineRiskLevel(riskScore: BigDecimal): RiskLevel {
        return when {
            riskScore > BigDecimal("0.80") -> RiskLevel.HIGH
            riskScore > BigDecimal("0.50") -> RiskLevel.MEDIUM
            riskScore > BigDecimal("0.25") -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateConcentrationRecommendations(overallRiskScore: BigDecimal, geographicConcentration: ConcentrationMetric, industryConcentration: ConcentrationMetric): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (overallRiskScore > BigDecimal("0.75")) {
            recommendations.add("Implement immediate risk mitigation strategies")
            recommendations.add("Consider reinsurance for concentrated exposures")
        }
        
        if (geographicConcentration.concentrationRatio > BigDecimal("0.25")) {
            recommendations.add("Diversify geographic exposure through targeted marketing")
        }
        
        if (industryConcentration.concentrationRatio > BigDecimal("0.20")) {
            recommendations.add("Reduce industry concentration through selective underwriting")
        }
        
        return recommendations
    }

    private fun simulatePortfolioReturns(portfolio: InsurancePortfolio, simulations: Int, timeHorizon: Int): MutableList<Double> {
        val returns = mutableListOf<Double>()
        val random = kotlin.random.Random.Default
        
        repeat(simulations) {
            var portfolioReturn = 0.0
            
            repeat(timeHorizon) {
                // Simulate various risk factors
                val mortalityShock = random.nextGaussian() * 0.05 // 5% volatility
                val interestRateShock = random.nextGaussian() * 0.02 // 2% volatility
                val lapseShock = random.nextGaussian() * 0.03 // 3% volatility
                
                val periodReturn = -0.02 + mortalityShock + interestRateShock + lapseShock
                portfolioReturn += periodReturn
            }
            
            returns.add(portfolioReturn)
        }
        
        return returns
    }

    private fun applyStressScenario(portfolio: InsurancePortfolio, scenario: StressScenario): BigDecimal {
        val baseValue = portfolio.policies.sumOf { it.faceAmount }
        
        return when (scenario.type) {
            StressScenarioType.MORTALITY_SHOCK -> {
                val mortalityIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.add(mortalityIncrease))
            }
            StressScenarioType.INTEREST_RATE_SHOCK -> {
                val rateChange = scenario.severity
                // Simplified duration-based calculation
                val duration = BigDecimal("8.5") // Average duration
                val priceChange = duration.multiply(rateChange).negate()
                baseValue.multiply(BigDecimal.ONE.add(priceChange))
            }
            StressScenarioType.LAPSE_SHOCK -> {
                val lapseIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.subtract(lapseIncrease.multiply(BigDecimal("0.5"))))
            }
            StressScenarioType.CATASTROPHE -> {
                val catastropheLoss = scenario.severity
                baseValue.subtract(baseValue.multiply(catastropheLoss))
            }
        }
    }

    private fun calculateAssetRisk(portfolio: InsurancePortfolio): Double {
        // Simplified asset risk calculation
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalAssets * 0.02 // 2% asset risk factor
    }

    private fun calculateInsuranceRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.05 // 5% insurance risk factor
    }

    private fun calculateCreditRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.01 // 1% credit risk factor
    }

    private fun calculateMarketRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.03 // 3% market risk factor
    }

    private fun calculateBusinessRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.015 // 1.5% business risk factor
    }

    private fun analyzeCatastropheExposure(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified catastrophe exposure calculation
        val highRiskStates = setOf("FL", "CA", "TX", "LA")
        return portfolio.policies
            .filter { it.state in highRiskStates }
            .sumOf { it.faceAmount }
    }

    private fun calculateOptimalQuotaShare(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val optimalCession = BigDecimal("0.20") // 20% quota share
        val cededAmount = totalExposure.multiply(optimalCession)
        val cost = cededAmount.multiply(BigDecimal("0.25")) // 25% commission
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.QUOTA_SHARE,
            cessionPercentage = optimalCession,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Optimal quota share for capital efficiency"
        )
    }

    private fun calculateOptimalSurplus(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val excessPolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        val cededAmount = excessPolicies.sumOf { it.faceAmount.subtract(retentionLimits.perLifeLimit) }
        val cost = cededAmount.multiply(BigDecimal("0.15")) // 15% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.SURPLUS,
            cessionPercentage = null,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Surplus reinsurance for large policies"
        )
    }

    private fun calculateCatastropheReinsurance(catastropheExposure: BigDecimal): ReinsuranceRecommendation {
        val cededAmount = catastropheExposure.multiply(BigDecimal("0.80")) // 80% cession
        val cost = cededAmount.multiply(BigDecimal("0.05")) // 5% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.CATASTROPHE,
            cessionPercentage = BigDecimal("0.80"),
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Catastrophe protection for geographic concentration"
        )
    }

    private fun calculatePortfolioGrowthRate(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified growth rate calculation
        val newPolicies = portfolio.policies.filter { 
            it.issueDate.isAfter(LocalDate.now().minusYears(1)) 
        }
        val newBusinessVolume = newPolicies.sumOf { it.faceAmount }
        val totalVolume = portfolio.policies.sumOf { it.faceAmount }
        
        return newBusinessVolume.divide(totalVolume, 4, RoundingMode.HALF_UP)
    }

    private fun calculateMortalityRatio(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified mortality ratio calculation
        // In practice, this would compare actual vs expected mortality
        return BigDecimal("1.05") // 105% of expected
    }

    private fun determineOverallRiskLevel(warnings: List<EarlyWarning>): RiskLevel {
        val highSeverityCount = warnings.count { it.severity == WarningSeverity.HIGH }
        val mediumSeverityCount = warnings.count { it.severity == WarningSeverity.MEDIUM }
        
        return when {
            highSeverityCount >= 2 -> RiskLevel.HIGH
            highSeverityCount >= 1 || mediumSeverityCount >= 3 -> RiskLevel.MEDIUM
            mediumSeverityCount >= 1 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateRecommendedActions(warnings: List<EarlyWarning>): List<String> {
        val actions = mutableListOf<String>()
        
        warnings.forEach { warning ->
            when (warning.type) {
                WarningType.LIQUIDITY -> actions.add("Improve liquidity position through asset rebalancing")
                WarningType.PROFITABILITY -> actions.add("Review pricing and expense management strategies")
                WarningType.GROWTH -> actions.add("Implement growth controls and enhanced underwriting")
                WarningType.CONCENTRATION -> actions.add("Diversify portfolio through targeted marketing")
                WarningType.MORTALITY -> actions.add("Review underwriting guidelines and mortality assumptions")
            }
        }
        
        return actions.distinct()
    }

    private fun calculateInsuranceRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.08 // 8% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalExposure * volatility * zScore
    }

    private fun calculateMarketRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.12 // 12% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalAssets * volatility * zScore
    }

    private fun calculateCreditRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val defaultRate = 0.02 // 2% default rate
        val lossGivenDefault = 0.40 // 40% loss given default
        return totalExposure * defaultRate * lossGivenDefault
    }

    private fun calculateOperationalRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalRevenue = portfolio.policies.sumOf { it.faceAmount }.toDouble() * 0.01 // 1% revenue assumption
        return totalRevenue * 0.15 // 15% of revenue for operational risk
    }

    private fun calculateDiversificationBenefit(vararg riskCapitals: Double): Double {
        val totalUndiversified = riskCapitals.sum()
        val correlationAdjustment = 0.85 // 85% correlation assumption
        return totalUndiversified * (1 - correlationAdjustment)
    }

    private fun BigDecimal.max(other: BigDecimal): BigDecimal {
        return if (this >= other) this else other
    }
}

// Data classes and enums for risk management
data class InsurancePortfolio(
    val portfolioId: String,
    val policies: List<PolicyInfo>,
    val asOfDate: LocalDate
)

data class PolicyInfo(
    val policyNumber: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val state: String,
    val industry: String,
    val productType: String,
    val issueDate: LocalDate
)

data class ConcentrationRiskAnalysis(
    val totalExposure: BigDecimal,
    val geographicConcentration: ConcentrationMetric,
    val industryConcentration: ConcentrationMetric,
    val ageBandConcentration: ConcentrationMetric,
    val productConcentration: ConcentrationMetric,
    val largePolicyConcentration: ConcentrationMetric,
    val overallRiskScore: BigDecimal,
    val riskLevel: RiskLevel,
    val recommendations: List<String>
)

data class ConcentrationMetric(
    val concentrationRatio: BigDecimal,
    val herfindahlIndex: BigDecimal,
    val topConcentrations: List<Pair<String, BigDecimal>>,
    val riskScore: BigDecimal
)

data class ValueAtRiskResult(
    val confidenceLevel: Double,
    val timeHorizon: Int,
    val valueAtRisk: BigDecimal,
    val var99: BigDecimal,
    val expectedShortfall: BigDecimal,
    val portfolioValue: BigDecimal
)

data class StressTestResult(
    val baselineValue: BigDecimal,
    val scenarioResults: List<ScenarioResult>,
    val worstCaseScenario: ScenarioResult?,
    val averageImpact: BigDecimal,
    val stressTestDate: LocalDateTime
)

data class ScenarioResult(
    val scenario: StressScenario,
    val baselineValue: BigDecimal,
    val stressedValue: BigDecimal,
    val impact: BigDecimal,
    val impactPercentage: BigDecimal
)

data class StressScenario(
    val name: String,
    val type: StressScenarioType,
    val severity: BigDecimal,
    val description: String
)

data class CapitalAdequacyResult(
    val capitalBase: BigDecimal,
    val totalRiskBasedCapital: BigDecimal,
    val rbcRatio: BigDecimal,
    val capitalAdequacy: CapitalAdequacy,
    val authorizedControlLevel: BigDecimal,
    val companyActionLevel: BigDecimal,
    val regulatoryActionLevel: BigDecimal,
    val mandatoryControlLevel: BigDecimal,
    val riskComponents: Map<String, BigDecimal>
)

data class RetentionLimits(
    val perLifeLimit: BigDecimal,
    val aggregateLimit: BigDecimal,
    val catastropheLimit: BigDecimal
)

data class ReinsuranceAnalysis(
    val totalExposure: BigDecimal,
    val netRetention: BigDecimal,
    val excessExposure: BigDecimal,
    val catastropheExposure: BigDecimal,
    val aggregateExposure: BigDecimal,
    val reinsuranceUtilization: BigDecimal,
    val recommendedQuotaShare: ReinsuranceRecommendation,
    val recommendedSurplus: ReinsuranceRecommendation,
    val recommendedCatastrophe: ReinsuranceRecommendation,
    val totalReinsuranceCost: BigDecimal,
    val costEfficiencyRatio: BigDecimal
)

data class ReinsuranceRecommendation(
    val type: ReinsuranceType,
    val cessionPercentage: BigDecimal?,
    val cededAmount: BigDecimal,
    val cost: BigDecimal,
    val rationale: String
)

data class FinancialMetrics(
    val liquidityRatio: BigDecimal,
    val returnOnEquity: BigDecimal,
    val debtToEquityRatio: BigDecimal,
    val expenseRatio: BigDecimal
)

data class EarlyWarningResult(
    val analysisDate: LocalDateTime,
    val warnings: List<EarlyWarning>,
    val overallRiskLevel: RiskLevel,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class EarlyWarning(
    val type: WarningType,
    val severity: WarningSeverity,
    val description: String,
    val threshold: BigDecimal,
    val actualValue: BigDecimal
)

data class EconomicCapitalResult(
    val confidenceLevel: Double,
    val insuranceRiskCapital: BigDecimal,
    val marketRiskCapital: BigDecimal,
    val creditRiskCapital: BigDecimal,
    val operationalRiskCapital: BigDecimal,
    val diversificationBenefit: BigDecimal,
    val totalEconomicCapital: BigDecimal,
    val capitalComponents: Map<String, BigDecimal>
)

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

enum class StressScenarioType {
    MORTALITY_SHOCK, INTEREST_RATE_SHOCK, LAPSE_SHOCK, CATASTROPHE
}

enum class CapitalAdequacy {
    WELL_CAPITALIZED, ADEQUATELY_CAPITALIZED, COMPANY_ACTION_LEVEL, 
    REGULATORY_ACTION_LEVEL, MANDATORY_CONTROL_LEVEL
}

enum class ReinsuranceType {
    QUOTA_SHARE, SURPLUS, CATASTROPHE, STOP_LOSS
}

enum class WarningType {
    LIQUIDITY, PROFITABILITY, GROWTH, CONCENTRATION, MORTALITY
}

enum class WarningSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Risk management service for insurance operations
 * Handles portfolio risk analysis, concentration limits, and regulatory compliance
 */
@Service
class RiskManagement {

    /**
     * Analyzes portfolio concentration risk
     */
    fun analyzeConcentrationRisk(portfolio: InsurancePortfolio): ConcentrationRiskAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        
        // Geographic concentration
        val geographicConcentration = analyzeGeographicConcentration(portfolio.policies, totalExposure)
        
        // Industry concentration
        val industryConcentration = analyzeIndustryConcentration(portfolio.policies, totalExposure)
        
        // Age band concentration
        val ageBandConcentration = analyzeAgeBandConcentration(portfolio.policies, totalExposure)
        
        // Product concentration
        val productConcentration = analyzeProductConcentration(portfolio.policies, totalExposure)
        
        // Large policy concentration
        val largePolicyConcentration = analyzeLargePolicyConcentration(portfolio.policies, totalExposure)
        
        val overallRiskScore = calculateOverallConcentrationRisk(
            geographicConcentration,
            industryConcentration,
            ageBandConcentration,
            productConcentration,
            largePolicyConcentration
        )
        
        return ConcentrationRiskAnalysis(
            totalExposure = totalExposure,
            geographicConcentration = geographicConcentration,
            industryConcentration = industryConcentration,
            ageBandConcentration = ageBandConcentration,
            productConcentration = productConcentration,
            largePolicyConcentration = largePolicyConcentration,
            overallRiskScore = overallRiskScore,
            riskLevel = determineRiskLevel(overallRiskScore),
            recommendations = generateConcentrationRecommendations(overallRiskScore, geographicConcentration, industryConcentration)
        )
    }

    /**
     * Calculates Value at Risk (VaR) for the portfolio
     */
    fun calculateValueAtRisk(portfolio: InsurancePortfolio, confidenceLevel: Double, timeHorizon: Int): ValueAtRiskResult {
        val returns = simulatePortfolioReturns(portfolio, 10000, timeHorizon)
        returns.sort()
        
        val varIndex = ((1 - confidenceLevel) * returns.size).toInt()
        val var95 = returns[varIndex]
        val var99 = returns[((1 - 0.99) * returns.size).toInt()]
        
        // Expected Shortfall (Conditional VaR)
        val expectedShortfall = returns.take(varIndex).average()
        
        return ValueAtRiskResult(
            confidenceLevel = confidenceLevel,
            timeHorizon = timeHorizon,
            valueAtRisk = BigDecimal(var95).setScale(2, RoundingMode.HALF_UP),
            var99 = BigDecimal(var99).setScale(2, RoundingMode.HALF_UP),
            expectedShortfall = BigDecimal(expectedShortfall).setScale(2, RoundingMode.HALF_UP),
            portfolioValue = portfolio.policies.sumOf { it.faceAmount }
        )
    }

    /**
     * Performs stress testing on the portfolio
     */
    fun performStressTesting(portfolio: InsurancePortfolio, stressScenarios: List<StressScenario>): StressTestResult {
        val baselineValue = portfolio.policies.sumOf { it.faceAmount }
        val scenarioResults = mutableListOf<ScenarioResult>()
        
        stressScenarios.forEach { scenario ->
            val stressedValue = applyStressScenario(portfolio, scenario)
            val impact = stressedValue.subtract(baselineValue)
            val impactPercentage = impact.divide(baselineValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            
            scenarioResults.add(
                ScenarioResult(
                    scenario = scenario,
                    baselineValue = baselineValue,
                    stressedValue = stressedValue,
                    impact = impact,
                    impactPercentage = impactPercentage
                )
            )
        }
        
        val worstCaseScenario = scenarioResults.minByOrNull { it.impact }
        val averageImpact = scenarioResults.map { it.impact }.fold(BigDecimal.ZERO) { acc, impact -> acc.add(impact) }
            .divide(BigDecimal(scenarioResults.size), 2, RoundingMode.HALF_UP)
        
        return StressTestResult(
            baselineValue = baselineValue,
            scenarioResults = scenarioResults,
            worstCaseScenario = worstCaseScenario,
            averageImpact = averageImpact,
            stressTestDate = LocalDateTime.now()
        )
    }

    /**
     * Monitors regulatory capital requirements
     */
    fun monitorCapitalRequirements(portfolio: InsurancePortfolio, capitalBase: BigDecimal): CapitalAdequacyResult {
        // Risk-Based Capital (RBC) calculation
        val c0Risk = calculateAssetRisk(portfolio) // Asset/affiliate risk
        val c1Risk = calculateInsuranceRisk(portfolio) // Insurance risk
        val c2Risk = calculateCreditRisk(portfolio) // Credit risk
        val c3Risk = calculateMarketRisk(portfolio) // Market risk
        val c4Risk = calculateBusinessRisk(portfolio) // Business risk
        
        val totalRisk = sqrt(
            (c0Risk + c1Risk).pow(2) + c2Risk.pow(2) + c3Risk.pow(2) + c4Risk.pow(2)
        )
        
        val authorizedControlLevel = totalRisk * 2.0
        val companyActionLevel = totalRisk * 1.5
        val regulatoryActionLevel = totalRisk * 1.0
        val mandatoryControlLevel = totalRisk * 0.7
        
        val rbcRatio = capitalBase.toDouble() / authorizedControlLevel
        
        val capitalAdequacy = when {
            rbcRatio >= 2.0 -> CapitalAdequacy.WELL_CAPITALIZED
            rbcRatio >= 1.5 -> CapitalAdequacy.ADEQUATELY_CAPITALIZED
            rbcRatio >= 1.0 -> CapitalAdequacy.COMPANY_ACTION_LEVEL
            rbcRatio >= 0.7 -> CapitalAdequacy.REGULATORY_ACTION_LEVEL
            else -> CapitalAdequacy.MANDATORY_CONTROL_LEVEL
        }
        
        return CapitalAdequacyResult(
            capitalBase = capitalBase,
            totalRiskBasedCapital = BigDecimal(totalRisk).setScale(2, RoundingMode.HALF_UP),
            rbcRatio = BigDecimal(rbcRatio).setScale(4, RoundingMode.HALF_UP),
            capitalAdequacy = capitalAdequacy,
            authorizedControlLevel = BigDecimal(authorizedControlLevel).setScale(2, RoundingMode.HALF_UP),
            companyActionLevel = BigDecimal(companyActionLevel).setScale(2, RoundingMode.HALF_UP),
            regulatoryActionLevel = BigDecimal(regulatoryActionLevel).setScale(2, RoundingMode.HALF_UP),
            mandatoryControlLevel = BigDecimal(mandatoryControlLevel).setScale(2, RoundingMode.HALF_UP),
            riskComponents = mapOf(
                "C0_ASSET_RISK" to BigDecimal(c0Risk).setScale(2, RoundingMode.HALF_UP),
                "C1_INSURANCE_RISK" to BigDecimal(c1Risk).setScale(2, RoundingMode.HALF_UP),
                "C2_CREDIT_RISK" to BigDecimal(c2Risk).setScale(2, RoundingMode.HALF_UP),
                "C3_MARKET_RISK" to BigDecimal(c3Risk).setScale(2, RoundingMode.HALF_UP),
                "C4_BUSINESS_RISK" to BigDecimal(c4Risk).setScale(2, RoundingMode.HALF_UP)
            )
        )
    }

    /**
     * Analyzes reinsurance needs and optimization
     */
    fun analyzeReinsuranceNeeds(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val largePolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        
        val excessExposure = largePolicies.sumOf { policy ->
            policy.faceAmount.subtract(retentionLimits.perLifeLimit).max(BigDecimal.ZERO)
        }
        
        val catastropheExposure = analyzeCatastropheExposure(portfolio)
        val aggregateExposure = totalExposure.subtract(retentionLimits.aggregateLimit).max(BigDecimal.ZERO)
        
        val recommendedQuotaShare = calculateOptimalQuotaShare(portfolio, retentionLimits)
        val recommendedSurplus = calculateOptimalSurplus(portfolio, retentionLimits)
        val recommendedCatastrophe = calculateCatastropheReinsurance(catastropheExposure)
        
        val totalReinsuranceCost = recommendedQuotaShare.cost
            .add(recommendedSurplus.cost)
            .add(recommendedCatastrophe.cost)
        
        val netRetention = totalExposure.subtract(excessExposure).subtract(aggregateExposure)
        val reinsuranceUtilization = excessExposure.add(aggregateExposure).divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        return ReinsuranceAnalysis(
            totalExposure = totalExposure,
            netRetention = netRetention,
            excessExposure = excessExposure,
            catastropheExposure = catastropheExposure,
            aggregateExposure = aggregateExposure,
            reinsuranceUtilization = reinsuranceUtilization,
            recommendedQuotaShare = recommendedQuotaShare,
            recommendedSurplus = recommendedSurplus,
            recommendedCatastrophe = recommendedCatastrophe,
            totalReinsuranceCost = totalReinsuranceCost,
            costEfficiencyRatio = totalReinsuranceCost.divide(excessExposure.add(aggregateExposure), 4, RoundingMode.HALF_UP)
        )
    }

    /**
     * Performs early warning system analysis
     */
    fun performEarlyWarningAnalysis(portfolio: InsurancePortfolio, financialMetrics: FinancialMetrics): EarlyWarningResult {
        val warnings = mutableListOf<EarlyWarning>()
        
        // Liquidity warnings
        if (financialMetrics.liquidityRatio < BigDecimal("1.2")) {
            warnings.add(EarlyWarning(
                type = WarningType.LIQUIDITY,
                severity = WarningSeverity.HIGH,
                description = "Liquidity ratio below minimum threshold",
                threshold = BigDecimal("1.2"),
                actualValue = financialMetrics.liquidityRatio
            ))
        }
        
        // Profitability warnings
        if (financialMetrics.returnOnEquity < BigDecimal("0.08")) {
            warnings.add(EarlyWarning(
                type = WarningType.PROFITABILITY,
                severity = WarningSeverity.MEDIUM,
                description = "Return on equity below target",
                threshold = BigDecimal("0.08"),
                actualValue = financialMetrics.returnOnEquity
            ))
        }
        
        // Growth warnings
        val growthRate = calculatePortfolioGrowthRate(portfolio)
        if (growthRate > BigDecimal("0.25")) {
            warnings.add(EarlyWarning(
                type = WarningType.GROWTH,
                severity = WarningSeverity.MEDIUM,
                description = "Portfolio growth rate exceeds prudent limits",
                threshold = BigDecimal("0.25"),
                actualValue = growthRate
            ))
        }
        
        // Concentration warnings
        val concentrationRisk = analyzeConcentrationRisk(portfolio)
        if (concentrationRisk.overallRiskScore > BigDecimal("0.75")) {
            warnings.add(EarlyWarning(
                type = WarningType.CONCENTRATION,
                severity = WarningSeverity.HIGH,
                description = "Portfolio concentration risk exceeds acceptable levels",
                threshold = BigDecimal("0.75"),
                actualValue = concentrationRisk.overallRiskScore
            ))
        }
        
        // Mortality experience warnings
        val mortalityRatio = calculateMortalityRatio(portfolio)
        if (mortalityRatio > BigDecimal("1.15")) {
            warnings.add(EarlyWarning(
                type = WarningType.MORTALITY,
                severity = WarningSeverity.HIGH,
                description = "Actual mortality exceeds expected by significant margin",
                threshold = BigDecimal("1.15"),
                actualValue = mortalityRatio
            ))
        }
        
        val overallRiskLevel = determineOverallRiskLevel(warnings)
        
        return EarlyWarningResult(
            analysisDate = LocalDateTime.now(),
            warnings = warnings,
            overallRiskLevel = overallRiskLevel,
            recommendedActions = generateRecommendedActions(warnings),
            nextReviewDate = LocalDateTime.now().plusMonths(1)
        )
    }

    /**
     * Calculates economic capital requirements
     */
    fun calculateEconomicCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): EconomicCapitalResult {
        // Insurance risk capital
        val insuranceRiskCapital = calculateInsuranceRiskCapital(portfolio, confidenceLevel)
        
        // Market risk capital
        val marketRiskCapital = calculateMarketRiskCapital(portfolio, confidenceLevel)
        
        // Credit risk capital
        val creditRiskCapital = calculateCreditRiskCapital(portfolio, confidenceLevel)
        
        // Operational risk capital
        val operationalRiskCapital = calculateOperationalRiskCapital(portfolio, confidenceLevel)
        
        // Diversification benefit
        val diversificationBenefit = calculateDiversificationBenefit(
            insuranceRiskCapital, marketRiskCapital, creditRiskCapital, operationalRiskCapital
        )
        
        val totalEconomicCapital = insuranceRiskCapital + marketRiskCapital + creditRiskCapital + operationalRiskCapital - diversificationBenefit
        
        return EconomicCapitalResult(
            confidenceLevel = confidenceLevel,
            insuranceRiskCapital = BigDecimal(insuranceRiskCapital).setScale(2, RoundingMode.HALF_UP),
            marketRiskCapital = BigDecimal(marketRiskCapital).setScale(2, RoundingMode.HALF_UP),
            creditRiskCapital = BigDecimal(creditRiskCapital).setScale(2, RoundingMode.HALF_UP),
            operationalRiskCapital = BigDecimal(operationalRiskCapital).setScale(2, RoundingMode.HALF_UP),
            diversificationBenefit = BigDecimal(diversificationBenefit).setScale(2, RoundingMode.HALF_UP),
            totalEconomicCapital = BigDecimal(totalEconomicCapital).setScale(2, RoundingMode.HALF_UP),
            capitalComponents = mapOf(
                "INSURANCE_RISK" to BigDecimal(insuranceRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "MARKET_RISK" to BigDecimal(marketRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "CREDIT_RISK" to BigDecimal(creditRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "OPERATIONAL_RISK" to BigDecimal(operationalRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP)
            )
        )
    }

    // Private helper methods
    private fun analyzeGeographicConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val stateExposure = policies.groupBy { it.state }
            .mapValues { (_, statePolicies) -> statePolicies.sumOf { it.faceAmount } }
        
        val maxStateExposure = stateExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxStateExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = stateExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = stateExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeIndustryConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val industryExposure = policies.groupBy { it.industry }
            .mapValues { (_, industryPolicies) -> industryPolicies.sumOf { it.faceAmount } }
        
        val maxIndustryExposure = industryExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxIndustryExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = industryExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = industryExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeAgeBandConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val ageBandExposure = policies.groupBy { getAgeBand(it.age) }
            .mapValues { (_, agePolicies) -> agePolicies.sumOf { it.faceAmount } }
        
        val maxAgeBandExposure = ageBandExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxAgeBandExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = ageBandExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = ageBandExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeProductConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val productExposure = policies.groupBy { it.productType }
            .mapValues { (_, productPolicies) -> productPolicies.sumOf { it.faceAmount } }
        
        val maxProductExposure = productExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxProductExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = productExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = productExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeLargePolicyConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val largePolicyThreshold = BigDecimal("1000000") // $1M threshold
        val largePolicies = policies.filter { it.faceAmount >= largePolicyThreshold }
        val largePolicyExposure = largePolicies.sumOf { it.faceAmount }
        
        val concentrationRatio = largePolicyExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        // For large policies, we calculate based on individual policy sizes
        val herfindahlIndex = policies.sumOf { policy ->
            val ratio = policy.faceAmount.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        val topPolicies = policies.sortedByDescending { it.faceAmount }.take(10)
            .map { "${it.policyNumber}" to it.faceAmount }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = topPolicies,
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun calculateOverallConcentrationRisk(vararg concentrations: ConcentrationMetric): BigDecimal {
        val weights = listOf(0.25, 0.20, 0.15, 0.20, 0.20) // Geographic, Industry, Age, Product, Large Policy
        
        return concentrations.mapIndexed { index, metric ->
            metric.riskScore.multiply(BigDecimal(weights[index]))
        }.fold(BigDecimal.ZERO) { acc, score -> acc.add(score) }
    }

    private fun calculateConcentrationRiskScore(concentrationRatio: BigDecimal, herfindahlIndex: BigDecimal): BigDecimal {
        val ratioScore = when {
            concentrationRatio > BigDecimal("0.30") -> BigDecimal("1.0")
            concentrationRatio > BigDecimal("0.20") -> BigDecimal("0.7")
            concentrationRatio > BigDecimal("0.10") -> BigDecimal("0.4")
            else -> BigDecimal("0.1")
        }
        
        val herfindahlScore = when {
            herfindahlIndex > BigDecimal("0.25") -> BigDecimal("1.0")
            herfindahlIndex > BigDecimal("0.15") -> BigDecimal("0.6")
            herfindahlIndex > BigDecimal("0.10") -> BigDecimal("0.3")
            else -> BigDecimal("0.1")
        }
        
        return ratioScore.add(herfindahlScore).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
    }

    private fun getAgeBand(age: Int): String {
        return when {
            age < 30 -> "Under 30"
            age < 40 -> "30-39"
            age < 50 -> "40-49"
            age < 60 -> "50-59"
            age < 70 -> "60-69"
            else -> "70+"
        }
    }

    private fun determineRiskLevel(riskScore: BigDecimal): RiskLevel {
        return when {
            riskScore > BigDecimal("0.80") -> RiskLevel.HIGH
            riskScore > BigDecimal("0.50") -> RiskLevel.MEDIUM
            riskScore > BigDecimal("0.25") -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateConcentrationRecommendations(overallRiskScore: BigDecimal, geographicConcentration: ConcentrationMetric, industryConcentration: ConcentrationMetric): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (overallRiskScore > BigDecimal("0.75")) {
            recommendations.add("Implement immediate risk mitigation strategies")
            recommendations.add("Consider reinsurance for concentrated exposures")
        }
        
        if (geographicConcentration.concentrationRatio > BigDecimal("0.25")) {
            recommendations.add("Diversify geographic exposure through targeted marketing")
        }
        
        if (industryConcentration.concentrationRatio > BigDecimal("0.20")) {
            recommendations.add("Reduce industry concentration through selective underwriting")
        }
        
        return recommendations
    }

    private fun simulatePortfolioReturns(portfolio: InsurancePortfolio, simulations: Int, timeHorizon: Int): MutableList<Double> {
        val returns = mutableListOf<Double>()
        val random = kotlin.random.Random.Default
        
        repeat(simulations) {
            var portfolioReturn = 0.0
            
            repeat(timeHorizon) {
                // Simulate various risk factors
                val mortalityShock = random.nextGaussian() * 0.05 // 5% volatility
                val interestRateShock = random.nextGaussian() * 0.02 // 2% volatility
                val lapseShock = random.nextGaussian() * 0.03 // 3% volatility
                
                val periodReturn = -0.02 + mortalityShock + interestRateShock + lapseShock
                portfolioReturn += periodReturn
            }
            
            returns.add(portfolioReturn)
        }
        
        return returns
    }

    private fun applyStressScenario(portfolio: InsurancePortfolio, scenario: StressScenario): BigDecimal {
        val baseValue = portfolio.policies.sumOf { it.faceAmount }
        
        return when (scenario.type) {
            StressScenarioType.MORTALITY_SHOCK -> {
                val mortalityIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.add(mortalityIncrease))
            }
            StressScenarioType.INTEREST_RATE_SHOCK -> {
                val rateChange = scenario.severity
                // Simplified duration-based calculation
                val duration = BigDecimal("8.5") // Average duration
                val priceChange = duration.multiply(rateChange).negate()
                baseValue.multiply(BigDecimal.ONE.add(priceChange))
            }
            StressScenarioType.LAPSE_SHOCK -> {
                val lapseIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.subtract(lapseIncrease.multiply(BigDecimal("0.5"))))
            }
            StressScenarioType.CATASTROPHE -> {
                val catastropheLoss = scenario.severity
                baseValue.subtract(baseValue.multiply(catastropheLoss))
            }
        }
    }

    private fun calculateAssetRisk(portfolio: InsurancePortfolio): Double {
        // Simplified asset risk calculation
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalAssets * 0.02 // 2% asset risk factor
    }

    private fun calculateInsuranceRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.05 // 5% insurance risk factor
    }

    private fun calculateCreditRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.01 // 1% credit risk factor
    }

    private fun calculateMarketRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.03 // 3% market risk factor
    }

    private fun calculateBusinessRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.015 // 1.5% business risk factor
    }

    private fun analyzeCatastropheExposure(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified catastrophe exposure calculation
        val highRiskStates = setOf("FL", "CA", "TX", "LA")
        return portfolio.policies
            .filter { it.state in highRiskStates }
            .sumOf { it.faceAmount }
    }

    private fun calculateOptimalQuotaShare(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val optimalCession = BigDecimal("0.20") // 20% quota share
        val cededAmount = totalExposure.multiply(optimalCession)
        val cost = cededAmount.multiply(BigDecimal("0.25")) // 25% commission
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.QUOTA_SHARE,
            cessionPercentage = optimalCession,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Optimal quota share for capital efficiency"
        )
    }

    private fun calculateOptimalSurplus(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val excessPolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        val cededAmount = excessPolicies.sumOf { it.faceAmount.subtract(retentionLimits.perLifeLimit) }
        val cost = cededAmount.multiply(BigDecimal("0.15")) // 15% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.SURPLUS,
            cessionPercentage = null,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Surplus reinsurance for large policies"
        )
    }

    private fun calculateCatastropheReinsurance(catastropheExposure: BigDecimal): ReinsuranceRecommendation {
        val cededAmount = catastropheExposure.multiply(BigDecimal("0.80")) // 80% cession
        val cost = cededAmount.multiply(BigDecimal("0.05")) // 5% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.CATASTROPHE,
            cessionPercentage = BigDecimal("0.80"),
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Catastrophe protection for geographic concentration"
        )
    }

    private fun calculatePortfolioGrowthRate(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified growth rate calculation
        val newPolicies = portfolio.policies.filter { 
            it.issueDate.isAfter(LocalDate.now().minusYears(1)) 
        }
        val newBusinessVolume = newPolicies.sumOf { it.faceAmount }
        val totalVolume = portfolio.policies.sumOf { it.faceAmount }
        
        return newBusinessVolume.divide(totalVolume, 4, RoundingMode.HALF_UP)
    }

    private fun calculateMortalityRatio(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified mortality ratio calculation
        // In practice, this would compare actual vs expected mortality
        return BigDecimal("1.05") // 105% of expected
    }

    private fun determineOverallRiskLevel(warnings: List<EarlyWarning>): RiskLevel {
        val highSeverityCount = warnings.count { it.severity == WarningSeverity.HIGH }
        val mediumSeverityCount = warnings.count { it.severity == WarningSeverity.MEDIUM }
        
        return when {
            highSeverityCount >= 2 -> RiskLevel.HIGH
            highSeverityCount >= 1 || mediumSeverityCount >= 3 -> RiskLevel.MEDIUM
            mediumSeverityCount >= 1 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateRecommendedActions(warnings: List<EarlyWarning>): List<String> {
        val actions = mutableListOf<String>()
        
        warnings.forEach { warning ->
            when (warning.type) {
                WarningType.LIQUIDITY -> actions.add("Improve liquidity position through asset rebalancing")
                WarningType.PROFITABILITY -> actions.add("Review pricing and expense management strategies")
                WarningType.GROWTH -> actions.add("Implement growth controls and enhanced underwriting")
                WarningType.CONCENTRATION -> actions.add("Diversify portfolio through targeted marketing")
                WarningType.MORTALITY -> actions.add("Review underwriting guidelines and mortality assumptions")
            }
        }
        
        return actions.distinct()
    }

    private fun calculateInsuranceRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.08 // 8% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalExposure * volatility * zScore
    }

    private fun calculateMarketRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.12 // 12% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalAssets * volatility * zScore
    }

    private fun calculateCreditRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val defaultRate = 0.02 // 2% default rate
        val lossGivenDefault = 0.40 // 40% loss given default
        return totalExposure * defaultRate * lossGivenDefault
    }

    private fun calculateOperationalRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalRevenue = portfolio.policies.sumOf { it.faceAmount }.toDouble() * 0.01 // 1% revenue assumption
        return totalRevenue * 0.15 // 15% of revenue for operational risk
    }

    private fun calculateDiversificationBenefit(vararg riskCapitals: Double): Double {
        val totalUndiversified = riskCapitals.sum()
        val correlationAdjustment = 0.85 // 85% correlation assumption
        return totalUndiversified * (1 - correlationAdjustment)
    }

    private fun BigDecimal.max(other: BigDecimal): BigDecimal {
        return if (this >= other) this else other
    }
}

// Data classes and enums for risk management
data class InsurancePortfolio(
    val portfolioId: String,
    val policies: List<PolicyInfo>,
    val asOfDate: LocalDate
)

data class PolicyInfo(
    val policyNumber: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val state: String,
    val industry: String,
    val productType: String,
    val issueDate: LocalDate
)

data class ConcentrationRiskAnalysis(
    val totalExposure: BigDecimal,
    val geographicConcentration: ConcentrationMetric,
    val industryConcentration: ConcentrationMetric,
    val ageBandConcentration: ConcentrationMetric,
    val productConcentration: ConcentrationMetric,
    val largePolicyConcentration: ConcentrationMetric,
    val overallRiskScore: BigDecimal,
    val riskLevel: RiskLevel,
    val recommendations: List<String>
)

data class ConcentrationMetric(
    val concentrationRatio: BigDecimal,
    val herfindahlIndex: BigDecimal,
    val topConcentrations: List<Pair<String, BigDecimal>>,
    val riskScore: BigDecimal
)

data class ValueAtRiskResult(
    val confidenceLevel: Double,
    val timeHorizon: Int,
    val valueAtRisk: BigDecimal,
    val var99: BigDecimal,
    val expectedShortfall: BigDecimal,
    val portfolioValue: BigDecimal
)

data class StressTestResult(
    val baselineValue: BigDecimal,
    val scenarioResults: List<ScenarioResult>,
    val worstCaseScenario: ScenarioResult?,
    val averageImpact: BigDecimal,
    val stressTestDate: LocalDateTime
)

data class ScenarioResult(
    val scenario: StressScenario,
    val baselineValue: BigDecimal,
    val stressedValue: BigDecimal,
    val impact: BigDecimal,
    val impactPercentage: BigDecimal
)

data class StressScenario(
    val name: String,
    val type: StressScenarioType,
    val severity: BigDecimal,
    val description: String
)

data class CapitalAdequacyResult(
    val capitalBase: BigDecimal,
    val totalRiskBasedCapital: BigDecimal,
    val rbcRatio: BigDecimal,
    val capitalAdequacy: CapitalAdequacy,
    val authorizedControlLevel: BigDecimal,
    val companyActionLevel: BigDecimal,
    val regulatoryActionLevel: BigDecimal,
    val mandatoryControlLevel: BigDecimal,
    val riskComponents: Map<String, BigDecimal>
)

data class RetentionLimits(
    val perLifeLimit: BigDecimal,
    val aggregateLimit: BigDecimal,
    val catastropheLimit: BigDecimal
)

data class ReinsuranceAnalysis(
    val totalExposure: BigDecimal,
    val netRetention: BigDecimal,
    val excessExposure: BigDecimal,
    val catastropheExposure: BigDecimal,
    val aggregateExposure: BigDecimal,
    val reinsuranceUtilization: BigDecimal,
    val recommendedQuotaShare: ReinsuranceRecommendation,
    val recommendedSurplus: ReinsuranceRecommendation,
    val recommendedCatastrophe: ReinsuranceRecommendation,
    val totalReinsuranceCost: BigDecimal,
    val costEfficiencyRatio: BigDecimal
)

data class ReinsuranceRecommendation(
    val type: ReinsuranceType,
    val cessionPercentage: BigDecimal?,
    val cededAmount: BigDecimal,
    val cost: BigDecimal,
    val rationale: String
)

data class FinancialMetrics(
    val liquidityRatio: BigDecimal,
    val returnOnEquity: BigDecimal,
    val debtToEquityRatio: BigDecimal,
    val expenseRatio: BigDecimal
)

data class EarlyWarningResult(
    val analysisDate: LocalDateTime,
    val warnings: List<EarlyWarning>,
    val overallRiskLevel: RiskLevel,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class EarlyWarning(
    val type: WarningType,
    val severity: WarningSeverity,
    val description: String,
    val threshold: BigDecimal,
    val actualValue: BigDecimal
)

data class EconomicCapitalResult(
    val confidenceLevel: Double,
    val insuranceRiskCapital: BigDecimal,
    val marketRiskCapital: BigDecimal,
    val creditRiskCapital: BigDecimal,
    val operationalRiskCapital: BigDecimal,
    val diversificationBenefit: BigDecimal,
    val totalEconomicCapital: BigDecimal,
    val capitalComponents: Map<String, BigDecimal>
)

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

enum class StressScenarioType {
    MORTALITY_SHOCK, INTEREST_RATE_SHOCK, LAPSE_SHOCK, CATASTROPHE
}

enum class CapitalAdequacy {
    WELL_CAPITALIZED, ADEQUATELY_CAPITALIZED, COMPANY_ACTION_LEVEL, 
    REGULATORY_ACTION_LEVEL, MANDATORY_CONTROL_LEVEL
}

enum class ReinsuranceType {
    QUOTA_SHARE, SURPLUS, CATASTROPHE, STOP_LOSS
}

enum class WarningType {
    LIQUIDITY, PROFITABILITY, GROWTH, CONCENTRATION, MORTALITY
}

enum class WarningSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Risk management service for insurance operations
 * Handles portfolio risk analysis, concentration limits, and regulatory compliance
 */
@Service
class RiskManagement {

    /**
     * Analyzes portfolio concentration risk
     */
    fun analyzeConcentrationRisk(portfolio: InsurancePortfolio): ConcentrationRiskAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        
        // Geographic concentration
        val geographicConcentration = analyzeGeographicConcentration(portfolio.policies, totalExposure)
        
        // Industry concentration
        val industryConcentration = analyzeIndustryConcentration(portfolio.policies, totalExposure)
        
        // Age band concentration
        val ageBandConcentration = analyzeAgeBandConcentration(portfolio.policies, totalExposure)
        
        // Product concentration
        val productConcentration = analyzeProductConcentration(portfolio.policies, totalExposure)
        
        // Large policy concentration
        val largePolicyConcentration = analyzeLargePolicyConcentration(portfolio.policies, totalExposure)
        
        val overallRiskScore = calculateOverallConcentrationRisk(
            geographicConcentration,
            industryConcentration,
            ageBandConcentration,
            productConcentration,
            largePolicyConcentration
        )
        
        return ConcentrationRiskAnalysis(
            totalExposure = totalExposure,
            geographicConcentration = geographicConcentration,
            industryConcentration = industryConcentration,
            ageBandConcentration = ageBandConcentration,
            productConcentration = productConcentration,
            largePolicyConcentration = largePolicyConcentration,
            overallRiskScore = overallRiskScore,
            riskLevel = determineRiskLevel(overallRiskScore),
            recommendations = generateConcentrationRecommendations(overallRiskScore, geographicConcentration, industryConcentration)
        )
    }

    /**
     * Calculates Value at Risk (VaR) for the portfolio
     */
    fun calculateValueAtRisk(portfolio: InsurancePortfolio, confidenceLevel: Double, timeHorizon: Int): ValueAtRiskResult {
        val returns = simulatePortfolioReturns(portfolio, 10000, timeHorizon)
        returns.sort()
        
        val varIndex = ((1 - confidenceLevel) * returns.size).toInt()
        val var95 = returns[varIndex]
        val var99 = returns[((1 - 0.99) * returns.size).toInt()]
        
        // Expected Shortfall (Conditional VaR)
        val expectedShortfall = returns.take(varIndex).average()
        
        return ValueAtRiskResult(
            confidenceLevel = confidenceLevel,
            timeHorizon = timeHorizon,
            valueAtRisk = BigDecimal(var95).setScale(2, RoundingMode.HALF_UP),
            var99 = BigDecimal(var99).setScale(2, RoundingMode.HALF_UP),
            expectedShortfall = BigDecimal(expectedShortfall).setScale(2, RoundingMode.HALF_UP),
            portfolioValue = portfolio.policies.sumOf { it.faceAmount }
        )
    }

    /**
     * Performs stress testing on the portfolio
     */
    fun performStressTesting(portfolio: InsurancePortfolio, stressScenarios: List<StressScenario>): StressTestResult {
        val baselineValue = portfolio.policies.sumOf { it.faceAmount }
        val scenarioResults = mutableListOf<ScenarioResult>()
        
        stressScenarios.forEach { scenario ->
            val stressedValue = applyStressScenario(portfolio, scenario)
            val impact = stressedValue.subtract(baselineValue)
            val impactPercentage = impact.divide(baselineValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            
            scenarioResults.add(
                ScenarioResult(
                    scenario = scenario,
                    baselineValue = baselineValue,
                    stressedValue = stressedValue,
                    impact = impact,
                    impactPercentage = impactPercentage
                )
            )
        }
        
        val worstCaseScenario = scenarioResults.minByOrNull { it.impact }
        val averageImpact = scenarioResults.map { it.impact }.fold(BigDecimal.ZERO) { acc, impact -> acc.add(impact) }
            .divide(BigDecimal(scenarioResults.size), 2, RoundingMode.HALF_UP)
        
        return StressTestResult(
            baselineValue = baselineValue,
            scenarioResults = scenarioResults,
            worstCaseScenario = worstCaseScenario,
            averageImpact = averageImpact,
            stressTestDate = LocalDateTime.now()
        )
    }

    /**
     * Monitors regulatory capital requirements
     */
    fun monitorCapitalRequirements(portfolio: InsurancePortfolio, capitalBase: BigDecimal): CapitalAdequacyResult {
        // Risk-Based Capital (RBC) calculation
        val c0Risk = calculateAssetRisk(portfolio) // Asset/affiliate risk
        val c1Risk = calculateInsuranceRisk(portfolio) // Insurance risk
        val c2Risk = calculateCreditRisk(portfolio) // Credit risk
        val c3Risk = calculateMarketRisk(portfolio) // Market risk
        val c4Risk = calculateBusinessRisk(portfolio) // Business risk
        
        val totalRisk = sqrt(
            (c0Risk + c1Risk).pow(2) + c2Risk.pow(2) + c3Risk.pow(2) + c4Risk.pow(2)
        )
        
        val authorizedControlLevel = totalRisk * 2.0
        val companyActionLevel = totalRisk * 1.5
        val regulatoryActionLevel = totalRisk * 1.0
        val mandatoryControlLevel = totalRisk * 0.7
        
        val rbcRatio = capitalBase.toDouble() / authorizedControlLevel
        
        val capitalAdequacy = when {
            rbcRatio >= 2.0 -> CapitalAdequacy.WELL_CAPITALIZED
            rbcRatio >= 1.5 -> CapitalAdequacy.ADEQUATELY_CAPITALIZED
            rbcRatio >= 1.0 -> CapitalAdequacy.COMPANY_ACTION_LEVEL
            rbcRatio >= 0.7 -> CapitalAdequacy.REGULATORY_ACTION_LEVEL
            else -> CapitalAdequacy.MANDATORY_CONTROL_LEVEL
        }
        
        return CapitalAdequacyResult(
            capitalBase = capitalBase,
            totalRiskBasedCapital = BigDecimal(totalRisk).setScale(2, RoundingMode.HALF_UP),
            rbcRatio = BigDecimal(rbcRatio).setScale(4, RoundingMode.HALF_UP),
            capitalAdequacy = capitalAdequacy,
            authorizedControlLevel = BigDecimal(authorizedControlLevel).setScale(2, RoundingMode.HALF_UP),
            companyActionLevel = BigDecimal(companyActionLevel).setScale(2, RoundingMode.HALF_UP),
            regulatoryActionLevel = BigDecimal(regulatoryActionLevel).setScale(2, RoundingMode.HALF_UP),
            mandatoryControlLevel = BigDecimal(mandatoryControlLevel).setScale(2, RoundingMode.HALF_UP),
            riskComponents = mapOf(
                "C0_ASSET_RISK" to BigDecimal(c0Risk).setScale(2, RoundingMode.HALF_UP),
                "C1_INSURANCE_RISK" to BigDecimal(c1Risk).setScale(2, RoundingMode.HALF_UP),
                "C2_CREDIT_RISK" to BigDecimal(c2Risk).setScale(2, RoundingMode.HALF_UP),
                "C3_MARKET_RISK" to BigDecimal(c3Risk).setScale(2, RoundingMode.HALF_UP),
                "C4_BUSINESS_RISK" to BigDecimal(c4Risk).setScale(2, RoundingMode.HALF_UP)
            )
        )
    }

    /**
     * Analyzes reinsurance needs and optimization
     */
    fun analyzeReinsuranceNeeds(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val largePolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        
        val excessExposure = largePolicies.sumOf { policy ->
            policy.faceAmount.subtract(retentionLimits.perLifeLimit).max(BigDecimal.ZERO)
        }
        
        val catastropheExposure = analyzeCatastropheExposure(portfolio)
        val aggregateExposure = totalExposure.subtract(retentionLimits.aggregateLimit).max(BigDecimal.ZERO)
        
        val recommendedQuotaShare = calculateOptimalQuotaShare(portfolio, retentionLimits)
        val recommendedSurplus = calculateOptimalSurplus(portfolio, retentionLimits)
        val recommendedCatastrophe = calculateCatastropheReinsurance(catastropheExposure)
        
        val totalReinsuranceCost = recommendedQuotaShare.cost
            .add(recommendedSurplus.cost)
            .add(recommendedCatastrophe.cost)
        
        val netRetention = totalExposure.subtract(excessExposure).subtract(aggregateExposure)
        val reinsuranceUtilization = excessExposure.add(aggregateExposure).divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        return ReinsuranceAnalysis(
            totalExposure = totalExposure,
            netRetention = netRetention,
            excessExposure = excessExposure,
            catastropheExposure = catastropheExposure,
            aggregateExposure = aggregateExposure,
            reinsuranceUtilization = reinsuranceUtilization,
            recommendedQuotaShare = recommendedQuotaShare,
            recommendedSurplus = recommendedSurplus,
            recommendedCatastrophe = recommendedCatastrophe,
            totalReinsuranceCost = totalReinsuranceCost,
            costEfficiencyRatio = totalReinsuranceCost.divide(excessExposure.add(aggregateExposure), 4, RoundingMode.HALF_UP)
        )
    }

    /**
     * Performs early warning system analysis
     */
    fun performEarlyWarningAnalysis(portfolio: InsurancePortfolio, financialMetrics: FinancialMetrics): EarlyWarningResult {
        val warnings = mutableListOf<EarlyWarning>()
        
        // Liquidity warnings
        if (financialMetrics.liquidityRatio < BigDecimal("1.2")) {
            warnings.add(EarlyWarning(
                type = WarningType.LIQUIDITY,
                severity = WarningSeverity.HIGH,
                description = "Liquidity ratio below minimum threshold",
                threshold = BigDecimal("1.2"),
                actualValue = financialMetrics.liquidityRatio
            ))
        }
        
        // Profitability warnings
        if (financialMetrics.returnOnEquity < BigDecimal("0.08")) {
            warnings.add(EarlyWarning(
                type = WarningType.PROFITABILITY,
                severity = WarningSeverity.MEDIUM,
                description = "Return on equity below target",
                threshold = BigDecimal("0.08"),
                actualValue = financialMetrics.returnOnEquity
            ))
        }
        
        // Growth warnings
        val growthRate = calculatePortfolioGrowthRate(portfolio)
        if (growthRate > BigDecimal("0.25")) {
            warnings.add(EarlyWarning(
                type = WarningType.GROWTH,
                severity = WarningSeverity.MEDIUM,
                description = "Portfolio growth rate exceeds prudent limits",
                threshold = BigDecimal("0.25"),
                actualValue = growthRate
            ))
        }
        
        // Concentration warnings
        val concentrationRisk = analyzeConcentrationRisk(portfolio)
        if (concentrationRisk.overallRiskScore > BigDecimal("0.75")) {
            warnings.add(EarlyWarning(
                type = WarningType.CONCENTRATION,
                severity = WarningSeverity.HIGH,
                description = "Portfolio concentration risk exceeds acceptable levels",
                threshold = BigDecimal("0.75"),
                actualValue = concentrationRisk.overallRiskScore
            ))
        }
        
        // Mortality experience warnings
        val mortalityRatio = calculateMortalityRatio(portfolio)
        if (mortalityRatio > BigDecimal("1.15")) {
            warnings.add(EarlyWarning(
                type = WarningType.MORTALITY,
                severity = WarningSeverity.HIGH,
                description = "Actual mortality exceeds expected by significant margin",
                threshold = BigDecimal("1.15"),
                actualValue = mortalityRatio
            ))
        }
        
        val overallRiskLevel = determineOverallRiskLevel(warnings)
        
        return EarlyWarningResult(
            analysisDate = LocalDateTime.now(),
            warnings = warnings,
            overallRiskLevel = overallRiskLevel,
            recommendedActions = generateRecommendedActions(warnings),
            nextReviewDate = LocalDateTime.now().plusMonths(1)
        )
    }

    /**
     * Calculates economic capital requirements
     */
    fun calculateEconomicCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): EconomicCapitalResult {
        // Insurance risk capital
        val insuranceRiskCapital = calculateInsuranceRiskCapital(portfolio, confidenceLevel)
        
        // Market risk capital
        val marketRiskCapital = calculateMarketRiskCapital(portfolio, confidenceLevel)
        
        // Credit risk capital
        val creditRiskCapital = calculateCreditRiskCapital(portfolio, confidenceLevel)
        
        // Operational risk capital
        val operationalRiskCapital = calculateOperationalRiskCapital(portfolio, confidenceLevel)
        
        // Diversification benefit
        val diversificationBenefit = calculateDiversificationBenefit(
            insuranceRiskCapital, marketRiskCapital, creditRiskCapital, operationalRiskCapital
        )
        
        val totalEconomicCapital = insuranceRiskCapital + marketRiskCapital + creditRiskCapital + operationalRiskCapital - diversificationBenefit
        
        return EconomicCapitalResult(
            confidenceLevel = confidenceLevel,
            insuranceRiskCapital = BigDecimal(insuranceRiskCapital).setScale(2, RoundingMode.HALF_UP),
            marketRiskCapital = BigDecimal(marketRiskCapital).setScale(2, RoundingMode.HALF_UP),
            creditRiskCapital = BigDecimal(creditRiskCapital).setScale(2, RoundingMode.HALF_UP),
            operationalRiskCapital = BigDecimal(operationalRiskCapital).setScale(2, RoundingMode.HALF_UP),
            diversificationBenefit = BigDecimal(diversificationBenefit).setScale(2, RoundingMode.HALF_UP),
            totalEconomicCapital = BigDecimal(totalEconomicCapital).setScale(2, RoundingMode.HALF_UP),
            capitalComponents = mapOf(
                "INSURANCE_RISK" to BigDecimal(insuranceRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "MARKET_RISK" to BigDecimal(marketRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "CREDIT_RISK" to BigDecimal(creditRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "OPERATIONAL_RISK" to BigDecimal(operationalRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP)
            )
        )
    }

    // Private helper methods
    private fun analyzeGeographicConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val stateExposure = policies.groupBy { it.state }
            .mapValues { (_, statePolicies) -> statePolicies.sumOf { it.faceAmount } }
        
        val maxStateExposure = stateExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxStateExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = stateExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = stateExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeIndustryConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val industryExposure = policies.groupBy { it.industry }
            .mapValues { (_, industryPolicies) -> industryPolicies.sumOf { it.faceAmount } }
        
        val maxIndustryExposure = industryExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxIndustryExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = industryExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = industryExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeAgeBandConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val ageBandExposure = policies.groupBy { getAgeBand(it.age) }
            .mapValues { (_, agePolicies) -> agePolicies.sumOf { it.faceAmount } }
        
        val maxAgeBandExposure = ageBandExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxAgeBandExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = ageBandExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = ageBandExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeProductConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val productExposure = policies.groupBy { it.productType }
            .mapValues { (_, productPolicies) -> productPolicies.sumOf { it.faceAmount } }
        
        val maxProductExposure = productExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxProductExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = productExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = productExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeLargePolicyConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val largePolicyThreshold = BigDecimal("1000000") // $1M threshold
        val largePolicies = policies.filter { it.faceAmount >= largePolicyThreshold }
        val largePolicyExposure = largePolicies.sumOf { it.faceAmount }
        
        val concentrationRatio = largePolicyExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        // For large policies, we calculate based on individual policy sizes
        val herfindahlIndex = policies.sumOf { policy ->
            val ratio = policy.faceAmount.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        val topPolicies = policies.sortedByDescending { it.faceAmount }.take(10)
            .map { "${it.policyNumber}" to it.faceAmount }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = topPolicies,
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun calculateOverallConcentrationRisk(vararg concentrations: ConcentrationMetric): BigDecimal {
        val weights = listOf(0.25, 0.20, 0.15, 0.20, 0.20) // Geographic, Industry, Age, Product, Large Policy
        
        return concentrations.mapIndexed { index, metric ->
            metric.riskScore.multiply(BigDecimal(weights[index]))
        }.fold(BigDecimal.ZERO) { acc, score -> acc.add(score) }
    }

    private fun calculateConcentrationRiskScore(concentrationRatio: BigDecimal, herfindahlIndex: BigDecimal): BigDecimal {
        val ratioScore = when {
            concentrationRatio > BigDecimal("0.30") -> BigDecimal("1.0")
            concentrationRatio > BigDecimal("0.20") -> BigDecimal("0.7")
            concentrationRatio > BigDecimal("0.10") -> BigDecimal("0.4")
            else -> BigDecimal("0.1")
        }
        
        val herfindahlScore = when {
            herfindahlIndex > BigDecimal("0.25") -> BigDecimal("1.0")
            herfindahlIndex > BigDecimal("0.15") -> BigDecimal("0.6")
            herfindahlIndex > BigDecimal("0.10") -> BigDecimal("0.3")
            else -> BigDecimal("0.1")
        }
        
        return ratioScore.add(herfindahlScore).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
    }

    private fun getAgeBand(age: Int): String {
        return when {
            age < 30 -> "Under 30"
            age < 40 -> "30-39"
            age < 50 -> "40-49"
            age < 60 -> "50-59"
            age < 70 -> "60-69"
            else -> "70+"
        }
    }

    private fun determineRiskLevel(riskScore: BigDecimal): RiskLevel {
        return when {
            riskScore > BigDecimal("0.80") -> RiskLevel.HIGH
            riskScore > BigDecimal("0.50") -> RiskLevel.MEDIUM
            riskScore > BigDecimal("0.25") -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateConcentrationRecommendations(overallRiskScore: BigDecimal, geographicConcentration: ConcentrationMetric, industryConcentration: ConcentrationMetric): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (overallRiskScore > BigDecimal("0.75")) {
            recommendations.add("Implement immediate risk mitigation strategies")
            recommendations.add("Consider reinsurance for concentrated exposures")
        }
        
        if (geographicConcentration.concentrationRatio > BigDecimal("0.25")) {
            recommendations.add("Diversify geographic exposure through targeted marketing")
        }
        
        if (industryConcentration.concentrationRatio > BigDecimal("0.20")) {
            recommendations.add("Reduce industry concentration through selective underwriting")
        }
        
        return recommendations
    }

    private fun simulatePortfolioReturns(portfolio: InsurancePortfolio, simulations: Int, timeHorizon: Int): MutableList<Double> {
        val returns = mutableListOf<Double>()
        val random = kotlin.random.Random.Default
        
        repeat(simulations) {
            var portfolioReturn = 0.0
            
            repeat(timeHorizon) {
                // Simulate various risk factors
                val mortalityShock = random.nextGaussian() * 0.05 // 5% volatility
                val interestRateShock = random.nextGaussian() * 0.02 // 2% volatility
                val lapseShock = random.nextGaussian() * 0.03 // 3% volatility
                
                val periodReturn = -0.02 + mortalityShock + interestRateShock + lapseShock
                portfolioReturn += periodReturn
            }
            
            returns.add(portfolioReturn)
        }
        
        return returns
    }

    private fun applyStressScenario(portfolio: InsurancePortfolio, scenario: StressScenario): BigDecimal {
        val baseValue = portfolio.policies.sumOf { it.faceAmount }
        
        return when (scenario.type) {
            StressScenarioType.MORTALITY_SHOCK -> {
                val mortalityIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.add(mortalityIncrease))
            }
            StressScenarioType.INTEREST_RATE_SHOCK -> {
                val rateChange = scenario.severity
                // Simplified duration-based calculation
                val duration = BigDecimal("8.5") // Average duration
                val priceChange = duration.multiply(rateChange).negate()
                baseValue.multiply(BigDecimal.ONE.add(priceChange))
            }
            StressScenarioType.LAPSE_SHOCK -> {
                val lapseIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.subtract(lapseIncrease.multiply(BigDecimal("0.5"))))
            }
            StressScenarioType.CATASTROPHE -> {
                val catastropheLoss = scenario.severity
                baseValue.subtract(baseValue.multiply(catastropheLoss))
            }
        }
    }

    private fun calculateAssetRisk(portfolio: InsurancePortfolio): Double {
        // Simplified asset risk calculation
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalAssets * 0.02 // 2% asset risk factor
    }

    private fun calculateInsuranceRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.05 // 5% insurance risk factor
    }

    private fun calculateCreditRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.01 // 1% credit risk factor
    }

    private fun calculateMarketRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.03 // 3% market risk factor
    }

    private fun calculateBusinessRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.015 // 1.5% business risk factor
    }

    private fun analyzeCatastropheExposure(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified catastrophe exposure calculation
        val highRiskStates = setOf("FL", "CA", "TX", "LA")
        return portfolio.policies
            .filter { it.state in highRiskStates }
            .sumOf { it.faceAmount }
    }

    private fun calculateOptimalQuotaShare(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val optimalCession = BigDecimal("0.20") // 20% quota share
        val cededAmount = totalExposure.multiply(optimalCession)
        val cost = cededAmount.multiply(BigDecimal("0.25")) // 25% commission
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.QUOTA_SHARE,
            cessionPercentage = optimalCession,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Optimal quota share for capital efficiency"
        )
    }

    private fun calculateOptimalSurplus(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val excessPolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        val cededAmount = excessPolicies.sumOf { it.faceAmount.subtract(retentionLimits.perLifeLimit) }
        val cost = cededAmount.multiply(BigDecimal("0.15")) // 15% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.SURPLUS,
            cessionPercentage = null,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Surplus reinsurance for large policies"
        )
    }

    private fun calculateCatastropheReinsurance(catastropheExposure: BigDecimal): ReinsuranceRecommendation {
        val cededAmount = catastropheExposure.multiply(BigDecimal("0.80")) // 80% cession
        val cost = cededAmount.multiply(BigDecimal("0.05")) // 5% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.CATASTROPHE,
            cessionPercentage = BigDecimal("0.80"),
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Catastrophe protection for geographic concentration"
        )
    }

    private fun calculatePortfolioGrowthRate(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified growth rate calculation
        val newPolicies = portfolio.policies.filter { 
            it.issueDate.isAfter(LocalDate.now().minusYears(1)) 
        }
        val newBusinessVolume = newPolicies.sumOf { it.faceAmount }
        val totalVolume = portfolio.policies.sumOf { it.faceAmount }
        
        return newBusinessVolume.divide(totalVolume, 4, RoundingMode.HALF_UP)
    }

    private fun calculateMortalityRatio(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified mortality ratio calculation
        // In practice, this would compare actual vs expected mortality
        return BigDecimal("1.05") // 105% of expected
    }

    private fun determineOverallRiskLevel(warnings: List<EarlyWarning>): RiskLevel {
        val highSeverityCount = warnings.count { it.severity == WarningSeverity.HIGH }
        val mediumSeverityCount = warnings.count { it.severity == WarningSeverity.MEDIUM }
        
        return when {
            highSeverityCount >= 2 -> RiskLevel.HIGH
            highSeverityCount >= 1 || mediumSeverityCount >= 3 -> RiskLevel.MEDIUM
            mediumSeverityCount >= 1 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateRecommendedActions(warnings: List<EarlyWarning>): List<String> {
        val actions = mutableListOf<String>()
        
        warnings.forEach { warning ->
            when (warning.type) {
                WarningType.LIQUIDITY -> actions.add("Improve liquidity position through asset rebalancing")
                WarningType.PROFITABILITY -> actions.add("Review pricing and expense management strategies")
                WarningType.GROWTH -> actions.add("Implement growth controls and enhanced underwriting")
                WarningType.CONCENTRATION -> actions.add("Diversify portfolio through targeted marketing")
                WarningType.MORTALITY -> actions.add("Review underwriting guidelines and mortality assumptions")
            }
        }
        
        return actions.distinct()
    }

    private fun calculateInsuranceRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.08 // 8% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalExposure * volatility * zScore
    }

    private fun calculateMarketRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.12 // 12% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalAssets * volatility * zScore
    }

    private fun calculateCreditRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val defaultRate = 0.02 // 2% default rate
        val lossGivenDefault = 0.40 // 40% loss given default
        return totalExposure * defaultRate * lossGivenDefault
    }

    private fun calculateOperationalRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalRevenue = portfolio.policies.sumOf { it.faceAmount }.toDouble() * 0.01 // 1% revenue assumption
        return totalRevenue * 0.15 // 15% of revenue for operational risk
    }

    private fun calculateDiversificationBenefit(vararg riskCapitals: Double): Double {
        val totalUndiversified = riskCapitals.sum()
        val correlationAdjustment = 0.85 // 85% correlation assumption
        return totalUndiversified * (1 - correlationAdjustment)
    }

    private fun BigDecimal.max(other: BigDecimal): BigDecimal {
        return if (this >= other) this else other
    }
}

// Data classes and enums for risk management
data class InsurancePortfolio(
    val portfolioId: String,
    val policies: List<PolicyInfo>,
    val asOfDate: LocalDate
)

data class PolicyInfo(
    val policyNumber: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val state: String,
    val industry: String,
    val productType: String,
    val issueDate: LocalDate
)

data class ConcentrationRiskAnalysis(
    val totalExposure: BigDecimal,
    val geographicConcentration: ConcentrationMetric,
    val industryConcentration: ConcentrationMetric,
    val ageBandConcentration: ConcentrationMetric,
    val productConcentration: ConcentrationMetric,
    val largePolicyConcentration: ConcentrationMetric,
    val overallRiskScore: BigDecimal,
    val riskLevel: RiskLevel,
    val recommendations: List<String>
)

data class ConcentrationMetric(
    val concentrationRatio: BigDecimal,
    val herfindahlIndex: BigDecimal,
    val topConcentrations: List<Pair<String, BigDecimal>>,
    val riskScore: BigDecimal
)

data class ValueAtRiskResult(
    val confidenceLevel: Double,
    val timeHorizon: Int,
    val valueAtRisk: BigDecimal,
    val var99: BigDecimal,
    val expectedShortfall: BigDecimal,
    val portfolioValue: BigDecimal
)

data class StressTestResult(
    val baselineValue: BigDecimal,
    val scenarioResults: List<ScenarioResult>,
    val worstCaseScenario: ScenarioResult?,
    val averageImpact: BigDecimal,
    val stressTestDate: LocalDateTime
)

data class ScenarioResult(
    val scenario: StressScenario,
    val baselineValue: BigDecimal,
    val stressedValue: BigDecimal,
    val impact: BigDecimal,
    val impactPercentage: BigDecimal
)

data class StressScenario(
    val name: String,
    val type: StressScenarioType,
    val severity: BigDecimal,
    val description: String
)

data class CapitalAdequacyResult(
    val capitalBase: BigDecimal,
    val totalRiskBasedCapital: BigDecimal,
    val rbcRatio: BigDecimal,
    val capitalAdequacy: CapitalAdequacy,
    val authorizedControlLevel: BigDecimal,
    val companyActionLevel: BigDecimal,
    val regulatoryActionLevel: BigDecimal,
    val mandatoryControlLevel: BigDecimal,
    val riskComponents: Map<String, BigDecimal>
)

data class RetentionLimits(
    val perLifeLimit: BigDecimal,
    val aggregateLimit: BigDecimal,
    val catastropheLimit: BigDecimal
)

data class ReinsuranceAnalysis(
    val totalExposure: BigDecimal,
    val netRetention: BigDecimal,
    val excessExposure: BigDecimal,
    val catastropheExposure: BigDecimal,
    val aggregateExposure: BigDecimal,
    val reinsuranceUtilization: BigDecimal,
    val recommendedQuotaShare: ReinsuranceRecommendation,
    val recommendedSurplus: ReinsuranceRecommendation,
    val recommendedCatastrophe: ReinsuranceRecommendation,
    val totalReinsuranceCost: BigDecimal,
    val costEfficiencyRatio: BigDecimal
)

data class ReinsuranceRecommendation(
    val type: ReinsuranceType,
    val cessionPercentage: BigDecimal?,
    val cededAmount: BigDecimal,
    val cost: BigDecimal,
    val rationale: String
)

data class FinancialMetrics(
    val liquidityRatio: BigDecimal,
    val returnOnEquity: BigDecimal,
    val debtToEquityRatio: BigDecimal,
    val expenseRatio: BigDecimal
)

data class EarlyWarningResult(
    val analysisDate: LocalDateTime,
    val warnings: List<EarlyWarning>,
    val overallRiskLevel: RiskLevel,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class EarlyWarning(
    val type: WarningType,
    val severity: WarningSeverity,
    val description: String,
    val threshold: BigDecimal,
    val actualValue: BigDecimal
)

data class EconomicCapitalResult(
    val confidenceLevel: Double,
    val insuranceRiskCapital: BigDecimal,
    val marketRiskCapital: BigDecimal,
    val creditRiskCapital: BigDecimal,
    val operationalRiskCapital: BigDecimal,
    val diversificationBenefit: BigDecimal,
    val totalEconomicCapital: BigDecimal,
    val capitalComponents: Map<String, BigDecimal>
)

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

enum class StressScenarioType {
    MORTALITY_SHOCK, INTEREST_RATE_SHOCK, LAPSE_SHOCK, CATASTROPHE
}

enum class CapitalAdequacy {
    WELL_CAPITALIZED, ADEQUATELY_CAPITALIZED, COMPANY_ACTION_LEVEL, 
    REGULATORY_ACTION_LEVEL, MANDATORY_CONTROL_LEVEL
}

enum class ReinsuranceType {
    QUOTA_SHARE, SURPLUS, CATASTROPHE, STOP_LOSS
}

enum class WarningType {
    LIQUIDITY, PROFITABILITY, GROWTH, CONCENTRATION, MORTALITY
}

enum class WarningSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Risk management service for insurance operations
 * Handles portfolio risk analysis, concentration limits, and regulatory compliance
 */
@Service
class RiskManagement {

    /**
     * Analyzes portfolio concentration risk
     */
    fun analyzeConcentrationRisk(portfolio: InsurancePortfolio): ConcentrationRiskAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        
        // Geographic concentration
        val geographicConcentration = analyzeGeographicConcentration(portfolio.policies, totalExposure)
        
        // Industry concentration
        val industryConcentration = analyzeIndustryConcentration(portfolio.policies, totalExposure)
        
        // Age band concentration
        val ageBandConcentration = analyzeAgeBandConcentration(portfolio.policies, totalExposure)
        
        // Product concentration
        val productConcentration = analyzeProductConcentration(portfolio.policies, totalExposure)
        
        // Large policy concentration
        val largePolicyConcentration = analyzeLargePolicyConcentration(portfolio.policies, totalExposure)
        
        val overallRiskScore = calculateOverallConcentrationRisk(
            geographicConcentration,
            industryConcentration,
            ageBandConcentration,
            productConcentration,
            largePolicyConcentration
        )
        
        return ConcentrationRiskAnalysis(
            totalExposure = totalExposure,
            geographicConcentration = geographicConcentration,
            industryConcentration = industryConcentration,
            ageBandConcentration = ageBandConcentration,
            productConcentration = productConcentration,
            largePolicyConcentration = largePolicyConcentration,
            overallRiskScore = overallRiskScore,
            riskLevel = determineRiskLevel(overallRiskScore),
            recommendations = generateConcentrationRecommendations(overallRiskScore, geographicConcentration, industryConcentration)
        )
    }

    /**
     * Calculates Value at Risk (VaR) for the portfolio
     */
    fun calculateValueAtRisk(portfolio: InsurancePortfolio, confidenceLevel: Double, timeHorizon: Int): ValueAtRiskResult {
        val returns = simulatePortfolioReturns(portfolio, 10000, timeHorizon)
        returns.sort()
        
        val varIndex = ((1 - confidenceLevel) * returns.size).toInt()
        val var95 = returns[varIndex]
        val var99 = returns[((1 - 0.99) * returns.size).toInt()]
        
        // Expected Shortfall (Conditional VaR)
        val expectedShortfall = returns.take(varIndex).average()
        
        return ValueAtRiskResult(
            confidenceLevel = confidenceLevel,
            timeHorizon = timeHorizon,
            valueAtRisk = BigDecimal(var95).setScale(2, RoundingMode.HALF_UP),
            var99 = BigDecimal(var99).setScale(2, RoundingMode.HALF_UP),
            expectedShortfall = BigDecimal(expectedShortfall).setScale(2, RoundingMode.HALF_UP),
            portfolioValue = portfolio.policies.sumOf { it.faceAmount }
        )
    }

    /**
     * Performs stress testing on the portfolio
     */
    fun performStressTesting(portfolio: InsurancePortfolio, stressScenarios: List<StressScenario>): StressTestResult {
        val baselineValue = portfolio.policies.sumOf { it.faceAmount }
        val scenarioResults = mutableListOf<ScenarioResult>()
        
        stressScenarios.forEach { scenario ->
            val stressedValue = applyStressScenario(portfolio, scenario)
            val impact = stressedValue.subtract(baselineValue)
            val impactPercentage = impact.divide(baselineValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
            
            scenarioResults.add(
                ScenarioResult(
                    scenario = scenario,
                    baselineValue = baselineValue,
                    stressedValue = stressedValue,
                    impact = impact,
                    impactPercentage = impactPercentage
                )
            )
        }
        
        val worstCaseScenario = scenarioResults.minByOrNull { it.impact }
        val averageImpact = scenarioResults.map { it.impact }.fold(BigDecimal.ZERO) { acc, impact -> acc.add(impact) }
            .divide(BigDecimal(scenarioResults.size), 2, RoundingMode.HALF_UP)
        
        return StressTestResult(
            baselineValue = baselineValue,
            scenarioResults = scenarioResults,
            worstCaseScenario = worstCaseScenario,
            averageImpact = averageImpact,
            stressTestDate = LocalDateTime.now()
        )
    }

    /**
     * Monitors regulatory capital requirements
     */
    fun monitorCapitalRequirements(portfolio: InsurancePortfolio, capitalBase: BigDecimal): CapitalAdequacyResult {
        // Risk-Based Capital (RBC) calculation
        val c0Risk = calculateAssetRisk(portfolio) // Asset/affiliate risk
        val c1Risk = calculateInsuranceRisk(portfolio) // Insurance risk
        val c2Risk = calculateCreditRisk(portfolio) // Credit risk
        val c3Risk = calculateMarketRisk(portfolio) // Market risk
        val c4Risk = calculateBusinessRisk(portfolio) // Business risk
        
        val totalRisk = sqrt(
            (c0Risk + c1Risk).pow(2) + c2Risk.pow(2) + c3Risk.pow(2) + c4Risk.pow(2)
        )
        
        val authorizedControlLevel = totalRisk * 2.0
        val companyActionLevel = totalRisk * 1.5
        val regulatoryActionLevel = totalRisk * 1.0
        val mandatoryControlLevel = totalRisk * 0.7
        
        val rbcRatio = capitalBase.toDouble() / authorizedControlLevel
        
        val capitalAdequacy = when {
            rbcRatio >= 2.0 -> CapitalAdequacy.WELL_CAPITALIZED
            rbcRatio >= 1.5 -> CapitalAdequacy.ADEQUATELY_CAPITALIZED
            rbcRatio >= 1.0 -> CapitalAdequacy.COMPANY_ACTION_LEVEL
            rbcRatio >= 0.7 -> CapitalAdequacy.REGULATORY_ACTION_LEVEL
            else -> CapitalAdequacy.MANDATORY_CONTROL_LEVEL
        }
        
        return CapitalAdequacyResult(
            capitalBase = capitalBase,
            totalRiskBasedCapital = BigDecimal(totalRisk).setScale(2, RoundingMode.HALF_UP),
            rbcRatio = BigDecimal(rbcRatio).setScale(4, RoundingMode.HALF_UP),
            capitalAdequacy = capitalAdequacy,
            authorizedControlLevel = BigDecimal(authorizedControlLevel).setScale(2, RoundingMode.HALF_UP),
            companyActionLevel = BigDecimal(companyActionLevel).setScale(2, RoundingMode.HALF_UP),
            regulatoryActionLevel = BigDecimal(regulatoryActionLevel).setScale(2, RoundingMode.HALF_UP),
            mandatoryControlLevel = BigDecimal(mandatoryControlLevel).setScale(2, RoundingMode.HALF_UP),
            riskComponents = mapOf(
                "C0_ASSET_RISK" to BigDecimal(c0Risk).setScale(2, RoundingMode.HALF_UP),
                "C1_INSURANCE_RISK" to BigDecimal(c1Risk).setScale(2, RoundingMode.HALF_UP),
                "C2_CREDIT_RISK" to BigDecimal(c2Risk).setScale(2, RoundingMode.HALF_UP),
                "C3_MARKET_RISK" to BigDecimal(c3Risk).setScale(2, RoundingMode.HALF_UP),
                "C4_BUSINESS_RISK" to BigDecimal(c4Risk).setScale(2, RoundingMode.HALF_UP)
            )
        )
    }

    /**
     * Analyzes reinsurance needs and optimization
     */
    fun analyzeReinsuranceNeeds(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceAnalysis {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val largePolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        
        val excessExposure = largePolicies.sumOf { policy ->
            policy.faceAmount.subtract(retentionLimits.perLifeLimit).max(BigDecimal.ZERO)
        }
        
        val catastropheExposure = analyzeCatastropheExposure(portfolio)
        val aggregateExposure = totalExposure.subtract(retentionLimits.aggregateLimit).max(BigDecimal.ZERO)
        
        val recommendedQuotaShare = calculateOptimalQuotaShare(portfolio, retentionLimits)
        val recommendedSurplus = calculateOptimalSurplus(portfolio, retentionLimits)
        val recommendedCatastrophe = calculateCatastropheReinsurance(catastropheExposure)
        
        val totalReinsuranceCost = recommendedQuotaShare.cost
            .add(recommendedSurplus.cost)
            .add(recommendedCatastrophe.cost)
        
        val netRetention = totalExposure.subtract(excessExposure).subtract(aggregateExposure)
        val reinsuranceUtilization = excessExposure.add(aggregateExposure).divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        return ReinsuranceAnalysis(
            totalExposure = totalExposure,
            netRetention = netRetention,
            excessExposure = excessExposure,
            catastropheExposure = catastropheExposure,
            aggregateExposure = aggregateExposure,
            reinsuranceUtilization = reinsuranceUtilization,
            recommendedQuotaShare = recommendedQuotaShare,
            recommendedSurplus = recommendedSurplus,
            recommendedCatastrophe = recommendedCatastrophe,
            totalReinsuranceCost = totalReinsuranceCost,
            costEfficiencyRatio = totalReinsuranceCost.divide(excessExposure.add(aggregateExposure), 4, RoundingMode.HALF_UP)
        )
    }

    /**
     * Performs early warning system analysis
     */
    fun performEarlyWarningAnalysis(portfolio: InsurancePortfolio, financialMetrics: FinancialMetrics): EarlyWarningResult {
        val warnings = mutableListOf<EarlyWarning>()
        
        // Liquidity warnings
        if (financialMetrics.liquidityRatio < BigDecimal("1.2")) {
            warnings.add(EarlyWarning(
                type = WarningType.LIQUIDITY,
                severity = WarningSeverity.HIGH,
                description = "Liquidity ratio below minimum threshold",
                threshold = BigDecimal("1.2"),
                actualValue = financialMetrics.liquidityRatio
            ))
        }
        
        // Profitability warnings
        if (financialMetrics.returnOnEquity < BigDecimal("0.08")) {
            warnings.add(EarlyWarning(
                type = WarningType.PROFITABILITY,
                severity = WarningSeverity.MEDIUM,
                description = "Return on equity below target",
                threshold = BigDecimal("0.08"),
                actualValue = financialMetrics.returnOnEquity
            ))
        }
        
        // Growth warnings
        val growthRate = calculatePortfolioGrowthRate(portfolio)
        if (growthRate > BigDecimal("0.25")) {
            warnings.add(EarlyWarning(
                type = WarningType.GROWTH,
                severity = WarningSeverity.MEDIUM,
                description = "Portfolio growth rate exceeds prudent limits",
                threshold = BigDecimal("0.25"),
                actualValue = growthRate
            ))
        }
        
        // Concentration warnings
        val concentrationRisk = analyzeConcentrationRisk(portfolio)
        if (concentrationRisk.overallRiskScore > BigDecimal("0.75")) {
            warnings.add(EarlyWarning(
                type = WarningType.CONCENTRATION,
                severity = WarningSeverity.HIGH,
                description = "Portfolio concentration risk exceeds acceptable levels",
                threshold = BigDecimal("0.75"),
                actualValue = concentrationRisk.overallRiskScore
            ))
        }
        
        // Mortality experience warnings
        val mortalityRatio = calculateMortalityRatio(portfolio)
        if (mortalityRatio > BigDecimal("1.15")) {
            warnings.add(EarlyWarning(
                type = WarningType.MORTALITY,
                severity = WarningSeverity.HIGH,
                description = "Actual mortality exceeds expected by significant margin",
                threshold = BigDecimal("1.15"),
                actualValue = mortalityRatio
            ))
        }
        
        val overallRiskLevel = determineOverallRiskLevel(warnings)
        
        return EarlyWarningResult(
            analysisDate = LocalDateTime.now(),
            warnings = warnings,
            overallRiskLevel = overallRiskLevel,
            recommendedActions = generateRecommendedActions(warnings),
            nextReviewDate = LocalDateTime.now().plusMonths(1)
        )
    }

    /**
     * Calculates economic capital requirements
     */
    fun calculateEconomicCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): EconomicCapitalResult {
        // Insurance risk capital
        val insuranceRiskCapital = calculateInsuranceRiskCapital(portfolio, confidenceLevel)
        
        // Market risk capital
        val marketRiskCapital = calculateMarketRiskCapital(portfolio, confidenceLevel)
        
        // Credit risk capital
        val creditRiskCapital = calculateCreditRiskCapital(portfolio, confidenceLevel)
        
        // Operational risk capital
        val operationalRiskCapital = calculateOperationalRiskCapital(portfolio, confidenceLevel)
        
        // Diversification benefit
        val diversificationBenefit = calculateDiversificationBenefit(
            insuranceRiskCapital, marketRiskCapital, creditRiskCapital, operationalRiskCapital
        )
        
        val totalEconomicCapital = insuranceRiskCapital + marketRiskCapital + creditRiskCapital + operationalRiskCapital - diversificationBenefit
        
        return EconomicCapitalResult(
            confidenceLevel = confidenceLevel,
            insuranceRiskCapital = BigDecimal(insuranceRiskCapital).setScale(2, RoundingMode.HALF_UP),
            marketRiskCapital = BigDecimal(marketRiskCapital).setScale(2, RoundingMode.HALF_UP),
            creditRiskCapital = BigDecimal(creditRiskCapital).setScale(2, RoundingMode.HALF_UP),
            operationalRiskCapital = BigDecimal(operationalRiskCapital).setScale(2, RoundingMode.HALF_UP),
            diversificationBenefit = BigDecimal(diversificationBenefit).setScale(2, RoundingMode.HALF_UP),
            totalEconomicCapital = BigDecimal(totalEconomicCapital).setScale(2, RoundingMode.HALF_UP),
            capitalComponents = mapOf(
                "INSURANCE_RISK" to BigDecimal(insuranceRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "MARKET_RISK" to BigDecimal(marketRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "CREDIT_RISK" to BigDecimal(creditRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP),
                "OPERATIONAL_RISK" to BigDecimal(operationalRiskCapital / totalEconomicCapital).setScale(4, RoundingMode.HALF_UP)
            )
        )
    }

    // Private helper methods
    private fun analyzeGeographicConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val stateExposure = policies.groupBy { it.state }
            .mapValues { (_, statePolicies) -> statePolicies.sumOf { it.faceAmount } }
        
        val maxStateExposure = stateExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxStateExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = stateExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = stateExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeIndustryConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val industryExposure = policies.groupBy { it.industry }
            .mapValues { (_, industryPolicies) -> industryPolicies.sumOf { it.faceAmount } }
        
        val maxIndustryExposure = industryExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxIndustryExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = industryExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = industryExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeAgeBandConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val ageBandExposure = policies.groupBy { getAgeBand(it.age) }
            .mapValues { (_, agePolicies) -> agePolicies.sumOf { it.faceAmount } }
        
        val maxAgeBandExposure = ageBandExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxAgeBandExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = ageBandExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = ageBandExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeProductConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val productExposure = policies.groupBy { it.productType }
            .mapValues { (_, productPolicies) -> productPolicies.sumOf { it.faceAmount } }
        
        val maxProductExposure = productExposure.values.maxOrNull() ?: BigDecimal.ZERO
        val concentrationRatio = maxProductExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        val herfindahlIndex = productExposure.values.sumOf { exposure ->
            val ratio = exposure.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = productExposure.toList().sortedByDescending { it.second }.take(5),
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun analyzeLargePolicyConcentration(policies: List<PolicyInfo>, totalExposure: BigDecimal): ConcentrationMetric {
        val largePolicyThreshold = BigDecimal("1000000") // $1M threshold
        val largePolicies = policies.filter { it.faceAmount >= largePolicyThreshold }
        val largePolicyExposure = largePolicies.sumOf { it.faceAmount }
        
        val concentrationRatio = largePolicyExposure.divide(totalExposure, 4, RoundingMode.HALF_UP)
        
        // For large policies, we calculate based on individual policy sizes
        val herfindahlIndex = policies.sumOf { policy ->
            val ratio = policy.faceAmount.divide(totalExposure, 6, RoundingMode.HALF_UP)
            ratio.multiply(ratio)
        }
        
        val topPolicies = policies.sortedByDescending { it.faceAmount }.take(10)
            .map { "${it.policyNumber}" to it.faceAmount }
        
        return ConcentrationMetric(
            concentrationRatio = concentrationRatio,
            herfindahlIndex = herfindahlIndex,
            topConcentrations = topPolicies,
            riskScore = calculateConcentrationRiskScore(concentrationRatio, herfindahlIndex)
        )
    }

    private fun calculateOverallConcentrationRisk(vararg concentrations: ConcentrationMetric): BigDecimal {
        val weights = listOf(0.25, 0.20, 0.15, 0.20, 0.20) // Geographic, Industry, Age, Product, Large Policy
        
        return concentrations.mapIndexed { index, metric ->
            metric.riskScore.multiply(BigDecimal(weights[index]))
        }.fold(BigDecimal.ZERO) { acc, score -> acc.add(score) }
    }

    private fun calculateConcentrationRiskScore(concentrationRatio: BigDecimal, herfindahlIndex: BigDecimal): BigDecimal {
        val ratioScore = when {
            concentrationRatio > BigDecimal("0.30") -> BigDecimal("1.0")
            concentrationRatio > BigDecimal("0.20") -> BigDecimal("0.7")
            concentrationRatio > BigDecimal("0.10") -> BigDecimal("0.4")
            else -> BigDecimal("0.1")
        }
        
        val herfindahlScore = when {
            herfindahlIndex > BigDecimal("0.25") -> BigDecimal("1.0")
            herfindahlIndex > BigDecimal("0.15") -> BigDecimal("0.6")
            herfindahlIndex > BigDecimal("0.10") -> BigDecimal("0.3")
            else -> BigDecimal("0.1")
        }
        
        return ratioScore.add(herfindahlScore).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
    }

    private fun getAgeBand(age: Int): String {
        return when {
            age < 30 -> "Under 30"
            age < 40 -> "30-39"
            age < 50 -> "40-49"
            age < 60 -> "50-59"
            age < 70 -> "60-69"
            else -> "70+"
        }
    }

    private fun determineRiskLevel(riskScore: BigDecimal): RiskLevel {
        return when {
            riskScore > BigDecimal("0.80") -> RiskLevel.HIGH
            riskScore > BigDecimal("0.50") -> RiskLevel.MEDIUM
            riskScore > BigDecimal("0.25") -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateConcentrationRecommendations(overallRiskScore: BigDecimal, geographicConcentration: ConcentrationMetric, industryConcentration: ConcentrationMetric): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (overallRiskScore > BigDecimal("0.75")) {
            recommendations.add("Implement immediate risk mitigation strategies")
            recommendations.add("Consider reinsurance for concentrated exposures")
        }
        
        if (geographicConcentration.concentrationRatio > BigDecimal("0.25")) {
            recommendations.add("Diversify geographic exposure through targeted marketing")
        }
        
        if (industryConcentration.concentrationRatio > BigDecimal("0.20")) {
            recommendations.add("Reduce industry concentration through selective underwriting")
        }
        
        return recommendations
    }

    private fun simulatePortfolioReturns(portfolio: InsurancePortfolio, simulations: Int, timeHorizon: Int): MutableList<Double> {
        val returns = mutableListOf<Double>()
        val random = kotlin.random.Random.Default
        
        repeat(simulations) {
            var portfolioReturn = 0.0
            
            repeat(timeHorizon) {
                // Simulate various risk factors
                val mortalityShock = random.nextGaussian() * 0.05 // 5% volatility
                val interestRateShock = random.nextGaussian() * 0.02 // 2% volatility
                val lapseShock = random.nextGaussian() * 0.03 // 3% volatility
                
                val periodReturn = -0.02 + mortalityShock + interestRateShock + lapseShock
                portfolioReturn += periodReturn
            }
            
            returns.add(portfolioReturn)
        }
        
        return returns
    }

    private fun applyStressScenario(portfolio: InsurancePortfolio, scenario: StressScenario): BigDecimal {
        val baseValue = portfolio.policies.sumOf { it.faceAmount }
        
        return when (scenario.type) {
            StressScenarioType.MORTALITY_SHOCK -> {
                val mortalityIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.add(mortalityIncrease))
            }
            StressScenarioType.INTEREST_RATE_SHOCK -> {
                val rateChange = scenario.severity
                // Simplified duration-based calculation
                val duration = BigDecimal("8.5") // Average duration
                val priceChange = duration.multiply(rateChange).negate()
                baseValue.multiply(BigDecimal.ONE.add(priceChange))
            }
            StressScenarioType.LAPSE_SHOCK -> {
                val lapseIncrease = scenario.severity
                baseValue.multiply(BigDecimal.ONE.subtract(lapseIncrease.multiply(BigDecimal("0.5"))))
            }
            StressScenarioType.CATASTROPHE -> {
                val catastropheLoss = scenario.severity
                baseValue.subtract(baseValue.multiply(catastropheLoss))
            }
        }
    }

    private fun calculateAssetRisk(portfolio: InsurancePortfolio): Double {
        // Simplified asset risk calculation
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalAssets * 0.02 // 2% asset risk factor
    }

    private fun calculateInsuranceRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.05 // 5% insurance risk factor
    }

    private fun calculateCreditRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.01 // 1% credit risk factor
    }

    private fun calculateMarketRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.03 // 3% market risk factor
    }

    private fun calculateBusinessRisk(portfolio: InsurancePortfolio): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        return totalExposure * 0.015 // 1.5% business risk factor
    }

    private fun analyzeCatastropheExposure(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified catastrophe exposure calculation
        val highRiskStates = setOf("FL", "CA", "TX", "LA")
        return portfolio.policies
            .filter { it.state in highRiskStates }
            .sumOf { it.faceAmount }
    }

    private fun calculateOptimalQuotaShare(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val optimalCession = BigDecimal("0.20") // 20% quota share
        val cededAmount = totalExposure.multiply(optimalCession)
        val cost = cededAmount.multiply(BigDecimal("0.25")) // 25% commission
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.QUOTA_SHARE,
            cessionPercentage = optimalCession,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Optimal quota share for capital efficiency"
        )
    }

    private fun calculateOptimalSurplus(portfolio: InsurancePortfolio, retentionLimits: RetentionLimits): ReinsuranceRecommendation {
        val excessPolicies = portfolio.policies.filter { it.faceAmount > retentionLimits.perLifeLimit }
        val cededAmount = excessPolicies.sumOf { it.faceAmount.subtract(retentionLimits.perLifeLimit) }
        val cost = cededAmount.multiply(BigDecimal("0.15")) // 15% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.SURPLUS,
            cessionPercentage = null,
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Surplus reinsurance for large policies"
        )
    }

    private fun calculateCatastropheReinsurance(catastropheExposure: BigDecimal): ReinsuranceRecommendation {
        val cededAmount = catastropheExposure.multiply(BigDecimal("0.80")) // 80% cession
        val cost = cededAmount.multiply(BigDecimal("0.05")) // 5% rate
        
        return ReinsuranceRecommendation(
            type = ReinsuranceType.CATASTROPHE,
            cessionPercentage = BigDecimal("0.80"),
            cededAmount = cededAmount,
            cost = cost,
            rationale = "Catastrophe protection for geographic concentration"
        )
    }

    private fun calculatePortfolioGrowthRate(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified growth rate calculation
        val newPolicies = portfolio.policies.filter { 
            it.issueDate.isAfter(LocalDate.now().minusYears(1)) 
        }
        val newBusinessVolume = newPolicies.sumOf { it.faceAmount }
        val totalVolume = portfolio.policies.sumOf { it.faceAmount }
        
        return newBusinessVolume.divide(totalVolume, 4, RoundingMode.HALF_UP)
    }

    private fun calculateMortalityRatio(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified mortality ratio calculation
        // In practice, this would compare actual vs expected mortality
        return BigDecimal("1.05") // 105% of expected
    }

    private fun determineOverallRiskLevel(warnings: List<EarlyWarning>): RiskLevel {
        val highSeverityCount = warnings.count { it.severity == WarningSeverity.HIGH }
        val mediumSeverityCount = warnings.count { it.severity == WarningSeverity.MEDIUM }
        
        return when {
            highSeverityCount >= 2 -> RiskLevel.HIGH
            highSeverityCount >= 1 || mediumSeverityCount >= 3 -> RiskLevel.MEDIUM
            mediumSeverityCount >= 1 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun generateRecommendedActions(warnings: List<EarlyWarning>): List<String> {
        val actions = mutableListOf<String>()
        
        warnings.forEach { warning ->
            when (warning.type) {
                WarningType.LIQUIDITY -> actions.add("Improve liquidity position through asset rebalancing")
                WarningType.PROFITABILITY -> actions.add("Review pricing and expense management strategies")
                WarningType.GROWTH -> actions.add("Implement growth controls and enhanced underwriting")
                WarningType.CONCENTRATION -> actions.add("Diversify portfolio through targeted marketing")
                WarningType.MORTALITY -> actions.add("Review underwriting guidelines and mortality assumptions")
            }
        }
        
        return actions.distinct()
    }

    private fun calculateInsuranceRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.08 // 8% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalExposure * volatility * zScore
    }

    private fun calculateMarketRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalAssets = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val volatility = 0.12 // 12% volatility
        val zScore = when (confidenceLevel) {
            0.95 -> 1.645
            0.99 -> 2.326
            0.999 -> 3.090
            else -> 1.645
        }
        return totalAssets * volatility * zScore
    }

    private fun calculateCreditRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }.toDouble()
        val defaultRate = 0.02 // 2% default rate
        val lossGivenDefault = 0.40 // 40% loss given default
        return totalExposure * defaultRate * lossGivenDefault
    }

    private fun calculateOperationalRiskCapital(portfolio: InsurancePortfolio, confidenceLevel: Double): Double {
        val totalRevenue = portfolio.policies.sumOf { it.faceAmount }.toDouble() * 0.01 // 1% revenue assumption
        return totalRevenue * 0.15 // 15% of revenue for operational risk
    }

    private fun calculateDiversificationBenefit(vararg riskCapitals: Double): Double {
        val totalUndiversified = riskCapitals.sum()
        val correlationAdjustment = 0.85 // 85% correlation assumption
        return totalUndiversified * (1 - correlationAdjustment)
    }

    private fun BigDecimal.max(other: BigDecimal): BigDecimal {
        return if (this >= other) this else other
    }
}

// Data classes and enums for risk management
data class InsurancePortfolio(
    val portfolioId: String,
    val policies: List<PolicyInfo>,
    val asOfDate: LocalDate
)

data class PolicyInfo(
    val policyNumber: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val state: String,
    val industry: String,
    val productType: String,
    val issueDate: LocalDate
)

data class ConcentrationRiskAnalysis(
    val totalExposure: BigDecimal,
    val geographicConcentration: ConcentrationMetric,
    val industryConcentration: ConcentrationMetric,
    val ageBandConcentration: ConcentrationMetric,
    val productConcentration: ConcentrationMetric,
    val largePolicyConcentration: ConcentrationMetric,
    val overallRiskScore: BigDecimal,
    val riskLevel: RiskLevel,
    val recommendations: List<String>
)

data class ConcentrationMetric(
    val concentrationRatio: BigDecimal,
    val herfindahlIndex: BigDecimal,
    val topConcentrations: List<Pair<String, BigDecimal>>,
    val riskScore: BigDecimal
)

data class ValueAtRiskResult(
    val confidenceLevel: Double,
    val timeHorizon: Int,
    val valueAtRisk: BigDecimal,
    val var99: BigDecimal,
    val expectedShortfall: BigDecimal,
    val portfolioValue: BigDecimal
)

data class StressTestResult(
    val baselineValue: BigDecimal,
    val scenarioResults: List<ScenarioResult>,
    val worstCaseScenario: ScenarioResult?,
    val averageImpact: BigDecimal,
    val stressTestDate: LocalDateTime
)

data class ScenarioResult(
    val scenario: StressScenario,
    val baselineValue: BigDecimal,
    val stressedValue: BigDecimal,
    val impact: BigDecimal,
    val impactPercentage: BigDecimal
)

data class StressScenario(
    val name: String,
    val type: StressScenarioType,
    val severity: BigDecimal,
    val description: String
)

data class CapitalAdequacyResult(
    val capitalBase: BigDecimal,
    val totalRiskBasedCapital: BigDecimal,
    val rbcRatio: BigDecimal,
    val capitalAdequacy: CapitalAdequacy,
    val authorizedControlLevel: BigDecimal,
    val companyActionLevel: BigDecimal,
    val regulatoryActionLevel: BigDecimal,
    val mandatoryControlLevel: BigDecimal,
    val riskComponents: Map<String, BigDecimal>
)

data class RetentionLimits(
    val perLifeLimit: BigDecimal,
    val aggregateLimit: BigDecimal,
    val catastropheLimit: BigDecimal
)

data class ReinsuranceAnalysis(
    val totalExposure: BigDecimal,
    val netRetention: BigDecimal,
    val excessExposure: BigDecimal,
    val catastropheExposure: BigDecimal,
    val aggregateExposure: BigDecimal,
    val reinsuranceUtilization: BigDecimal,
    val recommendedQuotaShare: ReinsuranceRecommendation,
    val recommendedSurplus: ReinsuranceRecommendation,
    val recommendedCatastrophe: ReinsuranceRecommendation,
    val totalReinsuranceCost: BigDecimal,
    val costEfficiencyRatio: BigDecimal
)

data class ReinsuranceRecommendation(
    val type: ReinsuranceType,
    val cessionPercentage: BigDecimal?,
    val cededAmount: BigDecimal,
    val cost: BigDecimal,
    val rationale: String
)

data class FinancialMetrics(
    val liquidityRatio: BigDecimal,
    val returnOnEquity: BigDecimal,
    val debtToEquityRatio: BigDecimal,
    val expenseRatio: BigDecimal
)

data class EarlyWarningResult(
    val analysisDate: LocalDateTime,
    val warnings: List<EarlyWarning>,
    val overallRiskLevel: RiskLevel,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class EarlyWarning(
    val type: WarningType,
    val severity: WarningSeverity,
    val description: String,
    val threshold: BigDecimal,
    val actualValue: BigDecimal
)

data class EconomicCapitalResult(
    val confidenceLevel: Double,
    val insuranceRiskCapital: BigDecimal,
    val marketRiskCapital: BigDecimal,
    val creditRiskCapital: BigDecimal,
    val operationalRiskCapital: BigDecimal,
    val diversificationBenefit: BigDecimal,
    val totalEconomicCapital: BigDecimal,
    val capitalComponents: Map<String, BigDecimal>
)

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

enum class StressScenarioType {
    MORTALITY_SHOCK, INTEREST_RATE_SHOCK, LAPSE_SHOCK, CATASTROPHE
}

enum class CapitalAdequacy {
    WELL_CAPITALIZED, ADEQUATELY_CAPITALIZED, COMPANY_ACTION_LEVEL, 
    REGULATORY_ACTION_LEVEL, MANDATORY_CONTROL_LEVEL
}

enum class ReinsuranceType {
    QUOTA_SHARE, SURPLUS, CATASTROPHE, STOP_LOSS
}

enum class WarningType {
    LIQUIDITY, PROFITABILITY, GROWTH, CONCENTRATION, MORTALITY
}

enum class WarningSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
