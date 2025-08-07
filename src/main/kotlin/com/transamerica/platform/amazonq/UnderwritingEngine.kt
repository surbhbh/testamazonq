package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow

/**
 * Underwriting engine for risk assessment and policy approval
 * Handles medical underwriting, financial underwriting, and risk classification
 */
@Service
class UnderwritingEngine {

    /**
     * Performs comprehensive underwriting assessment
     */
    fun performUnderwriting(application: InsuranceApplication): UnderwritingResult {
        val medicalAssessment = performMedicalUnderwriting(application.medicalInfo)
        val financialAssessment = performFinancialUnderwriting(application.financialInfo, application.requestedCoverage)
        val riskAssessment = assessOverallRisk(application, medicalAssessment, financialAssessment)
        
        val decision = makeUnderwritingDecision(medicalAssessment, financialAssessment, riskAssessment)
        
        return UnderwritingResult(
            applicationId = application.applicationId,
            decision = decision,
            riskClass = riskAssessment.riskClass,
            ratingFactors = riskAssessment.ratingFactors,
            medicalAssessment = medicalAssessment,
            financialAssessment = financialAssessment,
            recommendedPremium = calculatePremium(application.requestedCoverage, riskAssessment),
            conditions = determineConditions(decision, riskAssessment),
            underwriterId = getCurrentUnderwriterId(),
            underwritingDate = LocalDateTime.now()
        )
    }

    /**
     * Performs medical underwriting assessment
     */
    fun performMedicalUnderwriting(medicalInfo: MedicalInfo): MedicalAssessment {
        val riskFactors = mutableListOf<MedicalRiskFactor>()
        var riskScore = 0
        
        // Age factor
        val ageFactor = calculateAgeFactor(medicalInfo.age)
        riskScore += ageFactor.score
        riskFactors.add(MedicalRiskFactor("AGE", ageFactor.description, ageFactor.score))
        
        // Smoking status
        if (medicalInfo.isSmoker) {
            riskScore += 50
            riskFactors.add(MedicalRiskFactor("SMOKING", "Current smoker", 50))
        }
        
        // BMI assessment
        val bmiAssessment = assessBMI(medicalInfo.height, medicalInfo.weight)
        riskScore += bmiAssessment.score
        riskFactors.add(MedicalRiskFactor("BMI", bmiAssessment.description, bmiAssessment.score))
        
        // Medical history
        medicalInfo.medicalHistory.forEach { condition ->
            val conditionRisk = assessMedicalCondition(condition)
            riskScore += conditionRisk.score
            riskFactors.add(MedicalRiskFactor("MEDICAL_HISTORY", conditionRisk.description, conditionRisk.score))
        }
        
        // Family history
        medicalInfo.familyHistory.forEach { condition ->
            val familyRisk = assessFamilyHistory(condition)
            riskScore += familyRisk.score
            riskFactors.add(MedicalRiskFactor("FAMILY_HISTORY", familyRisk.description, familyRisk.score))
        }
        
        // Lifestyle factors
        val lifestyleRisk = assessLifestyle(medicalInfo.lifestyle)
        riskScore += lifestyleRisk.score
        riskFactors.add(MedicalRiskFactor("LIFESTYLE", lifestyleRisk.description, lifestyleRisk.score))
        
        val medicalExamRequired = determineMedicalExamRequirement(medicalInfo, riskScore)
        val riskClass = determineMedicalRiskClass(riskScore)
        
        return MedicalAssessment(
            riskScore = riskScore,
            riskClass = riskClass,
            riskFactors = riskFactors,
            medicalExamRequired = medicalExamRequired,
            additionalRequirements = determineAdditionalMedicalRequirements(riskScore, medicalInfo)
        )
    }

    /**
     * Performs financial underwriting assessment
     */
    fun performFinancialUnderwriting(financialInfo: FinancialInfo, requestedCoverage: BigDecimal): FinancialAssessment {
        val incomeMultiplier = calculateIncomeMultiplier(financialInfo.annualIncome)
        val maxCoverageByIncome = financialInfo.annualIncome.multiply(incomeMultiplier)
        
        val netWorthMultiplier = BigDecimal("0.25") // 25% of net worth
        val maxCoverageByNetWorth = financialInfo.netWorth.multiply(netWorthMultiplier)
        
        val maxRecommendedCoverage = maxOf(maxCoverageByIncome, maxCoverageByNetWorth)
        
        val financialJustification = when {
            requestedCoverage <= maxCoverageByIncome -> FinancialJustification.INCOME_REPLACEMENT
            requestedCoverage <= maxCoverageByNetWorth -> FinancialJustification.ESTATE_PLANNING
            requestedCoverage <= maxRecommendedCoverage -> FinancialJustification.BUSINESS_PROTECTION
            else -> FinancialJustification.INSUFFICIENT_JUSTIFICATION
        }
        
        val debtToIncomeRatio = financialInfo.totalDebt.divide(financialInfo.annualIncome, 4, java.math.RoundingMode.HALF_UP)
        val liquidityRatio = financialInfo.liquidAssets.divide(financialInfo.monthlyExpenses.multiply(BigDecimal("12")), 4, java.math.RoundingMode.HALF_UP)
        
        val financialStability = assessFinancialStability(debtToIncomeRatio, liquidityRatio, financialInfo.creditScore)
        
        return FinancialAssessment(
            maxRecommendedCoverage = maxRecommendedCoverage,
            financialJustification = financialJustification,
            debtToIncomeRatio = debtToIncomeRatio,
            liquidityRatio = liquidityRatio,
            financialStability = financialStability,
            additionalDocumentationRequired = requestedCoverage > maxRecommendedCoverage
        )
    }

    /**
     * Assesses overall risk combining medical and financial factors
     */
    fun assessOverallRisk(application: InsuranceApplication, medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): RiskAssessment {
        val ratingFactors = mutableMapOf<String, BigDecimal>()
        
        // Medical rating factors
        when (medicalAssessment.riskClass) {
            MedicalRiskClass.SUPER_PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.85")
            MedicalRiskClass.PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.95")
            MedicalRiskClass.STANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.00")
            MedicalRiskClass.SUBSTANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.25")
            MedicalRiskClass.DECLINED -> ratingFactors["MEDICAL"] = BigDecimal("999.99") // Effectively declined
        }
        
        // Financial rating factors
        when (financialAssessment.financialJustification) {
            FinancialJustification.INCOME_REPLACEMENT -> ratingFactors["FINANCIAL"] = BigDecimal("1.00")
            FinancialJustification.ESTATE_PLANNING -> ratingFactors["FINANCIAL"] = BigDecimal("1.05")
            FinancialJustification.BUSINESS_PROTECTION -> ratingFactors["FINANCIAL"] = BigDecimal("1.10")
            FinancialJustification.INSUFFICIENT_JUSTIFICATION -> ratingFactors["FINANCIAL"] = BigDecimal("999.99")
        }
        
        // Occupation rating
        val occupationFactor = assessOccupationRisk(application.applicantInfo.occupation)
        ratingFactors["OCCUPATION"] = occupationFactor
        
        // Lifestyle rating
        val lifestyleFactor = assessLifestyleRisk(application.medicalInfo.lifestyle)
        ratingFactors["LIFESTYLE"] = lifestyleFactor
        
        // Geographic rating
        val geographicFactor = assessGeographicRisk(application.applicantInfo.residenceState)
        ratingFactors["GEOGRAPHIC"] = geographicFactor
        
        val overallRiskClass = determineOverallRiskClass(ratingFactors)
        
        return RiskAssessment(
            riskClass = overallRiskClass,
            ratingFactors = ratingFactors,
            overallRiskScore = calculateOverallRiskScore(ratingFactors),
            riskNotes = generateRiskNotes(medicalAssessment, financialAssessment)
        )
    }

    /**
     * Calculates premium based on coverage amount and risk assessment
     */
    fun calculatePremium(coverageAmount: BigDecimal, riskAssessment: RiskAssessment): BigDecimal {
        val basePremiumRate = BigDecimal("0.001") // $1 per $1000 of coverage
        var premium = coverageAmount.multiply(basePremiumRate)
        
        // Apply rating factors
        riskAssessment.ratingFactors.values.forEach { factor ->
            premium = premium.multiply(factor)
        }
        
        // Apply minimum premium
        val minimumPremium = BigDecimal("100")
        if (premium < minimumPremium) {
            premium = minimumPremium
        }
        
        return premium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates mortality table lookup for actuarial calculations
     */
    fun lookupMortalityRate(age: Int, gender: Gender, riskClass: RiskClass): BigDecimal {
        // Simplified mortality table - in reality this would be much more complex
        val baseMortalityRate = when {
            age < 30 -> BigDecimal("0.0008")
            age < 40 -> BigDecimal("0.0012")
            age < 50 -> BigDecimal("0.0020")
            age < 60 -> BigDecimal("0.0035")
            age < 70 -> BigDecimal("0.0065")
            else -> BigDecimal("0.0120")
        }
        
        // Gender adjustment
        val genderFactor = if (gender == Gender.FEMALE) BigDecimal("0.85") else BigDecimal("1.00")
        
        // Risk class adjustment
        val riskFactor = when (riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.80")
            RiskClass.STANDARD -> BigDecimal("1.00")
            RiskClass.SUBSTANDARD -> BigDecimal("1.50")
            RiskClass.DECLINED -> BigDecimal("999.99")
        }
        
        return baseMortalityRate.multiply(genderFactor).multiply(riskFactor)
    }

    // Private helper methods
    private fun calculateAgeFactor(age: Int): RiskFactor {
        return when {
            age < 25 -> RiskFactor("Young adult", 5)
            age < 35 -> RiskFactor("Young adult - preferred", 0)
            age < 45 -> RiskFactor("Middle age - standard", 10)
            age < 55 -> RiskFactor("Middle age - increased risk", 20)
            age < 65 -> RiskFactor("Senior - higher risk", 35)
            else -> RiskFactor("Senior - high risk", 50)
        }
    }

    private fun assessBMI(heightInches: Int, weightPounds: Int): RiskFactor {
        val heightMeters = heightInches * 0.0254
        val weightKg = weightPounds * 0.453592
        val bmi = weightKg / (heightMeters.pow(2))
        
        return when {
            bmi < 18.5 -> RiskFactor("Underweight", 15)
            bmi < 25.0 -> RiskFactor("Normal weight", 0)
            bmi < 30.0 -> RiskFactor("Overweight", 10)
            bmi < 35.0 -> RiskFactor("Obese Class I", 25)
            bmi < 40.0 -> RiskFactor("Obese Class II", 40)
            else -> RiskFactor("Obese Class III", 60)
        }
    }

    private fun assessMedicalCondition(condition: MedicalCondition): RiskFactor {
        return when (condition.type) {
            "DIABETES" -> when (condition.severity) {
                "CONTROLLED" -> RiskFactor("Controlled diabetes", 30)
                "UNCONTROLLED" -> RiskFactor("Uncontrolled diabetes", 80)
                else -> RiskFactor("Diabetes", 50)
            }
            "HYPERTENSION" -> when (condition.severity) {
                "MILD" -> RiskFactor("Mild hypertension", 15)
                "MODERATE" -> RiskFactor("Moderate hypertension", 30)
                "SEVERE" -> RiskFactor("Severe hypertension", 60)
                else -> RiskFactor("Hypertension", 25)
            }
            "HEART_DISEASE" -> RiskFactor("Heart disease", 100)
            "CANCER" -> when (condition.yearsInRemission) {
                in 0..2 -> RiskFactor("Recent cancer history", 150)
                in 3..5 -> RiskFactor("Cancer history", 75)
                else -> RiskFactor("Remote cancer history", 25)
            }
            else -> RiskFactor("Other medical condition", 20)
        }
    }

    private fun assessFamilyHistory(condition: FamilyMedicalHistory): RiskFactor {
        val baseScore = when (condition.condition) {
            "HEART_DISEASE" -> 15
            "CANCER" -> 10
            "DIABETES" -> 8
            "STROKE" -> 12
            else -> 5
        }
        
        val relationshipMultiplier = when (condition.relationship) {
            "PARENT" -> 1.0
            "SIBLING" -> 0.8
            "GRANDPARENT" -> 0.5
            else -> 0.3
        }
        
        val ageMultiplier = if (condition.ageAtDiagnosis < 60) 1.5 else 1.0
        
        val adjustedScore = (baseScore * relationshipMultiplier * ageMultiplier).toInt()
        
        return RiskFactor("Family history of ${condition.condition}", adjustedScore)
    }

    private fun assessLifestyle(lifestyle: LifestyleInfo): RiskFactor {
        var score = 0
        val factors = mutableListOf<String>()
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            score += 25
            factors.add("heavy drinking")
        } else if (lifestyle.alcoholConsumption == "MODERATE") {
            score += 5
            factors.add("moderate drinking")
        }
        
        if (lifestyle.exerciseFrequency == "NEVER") {
            score += 15
            factors.add("sedentary lifestyle")
        } else if (lifestyle.exerciseFrequency == "REGULAR") {
            score -= 5
            factors.add("regular exercise")
        }
        
        lifestyle.hazardousActivities.forEach { activity ->
            when (activity) {
                "SKYDIVING" -> { score += 30; factors.add("skydiving") }
                "ROCK_CLIMBING" -> { score += 20; factors.add("rock climbing") }
                "MOTORCYCLE_RACING" -> { score += 40; factors.add("motorcycle racing") }
                "SCUBA_DIVING" -> { score += 15; factors.add("scuba diving") }
            }
        }
        
        val description = if (factors.isEmpty()) "Standard lifestyle" else "Lifestyle factors: ${factors.joinToString(", ")}"
        
        return RiskFactor(description, maxOf(0, score))
    }

    private fun determineMedicalExamRequirement(medicalInfo: MedicalInfo, riskScore: Int): Boolean {
        return riskScore > 50 || medicalInfo.age > 50 || medicalInfo.medicalHistory.isNotEmpty()
    }

    private fun determineAdditionalMedicalRequirements(riskScore: Int, medicalInfo: MedicalInfo): List<String> {
        val requirements = mutableListOf<String>()
        
        if (riskScore > 75) {
            requirements.add("Physician's statement")
            requirements.add("Medical records")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "HEART_DISEASE" }) {
            requirements.add("Cardiac stress test")
            requirements.add("EKG")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "DIABETES" }) {
            requirements.add("HbA1c test")
            requirements.add("Diabetic panel")
        }
        
        if (medicalInfo.age > 65) {
            requirements.add("Cognitive assessment")
        }
        
        return requirements
    }

    private fun determineMedicalRiskClass(riskScore: Int): MedicalRiskClass {
        return when {
            riskScore <= 10 -> MedicalRiskClass.SUPER_PREFERRED
            riskScore <= 25 -> MedicalRiskClass.PREFERRED
            riskScore <= 50 -> MedicalRiskClass.STANDARD
            riskScore <= 100 -> MedicalRiskClass.SUBSTANDARD
            else -> MedicalRiskClass.DECLINED
        }
    }

    private fun calculateIncomeMultiplier(annualIncome: BigDecimal): BigDecimal {
        return when {
            annualIncome < BigDecimal("50000") -> BigDecimal("10")
            annualIncome < BigDecimal("100000") -> BigDecimal("15")
            annualIncome < BigDecimal("250000") -> BigDecimal("20")
            annualIncome < BigDecimal("500000") -> BigDecimal("25")
            else -> BigDecimal("30")
        }
    }

    private fun assessFinancialStability(debtToIncomeRatio: BigDecimal, liquidityRatio: BigDecimal, creditScore: Int): FinancialStability {
        var stabilityScore = 0
        
        // Debt-to-income assessment
        when {
            debtToIncomeRatio < BigDecimal("0.20") -> stabilityScore += 20
            debtToIncomeRatio < BigDecimal("0.36") -> stabilityScore += 10
            debtToIncomeRatio < BigDecimal("0.50") -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        // Liquidity assessment
        when {
            liquidityRatio > BigDecimal("1.0") -> stabilityScore += 15
            liquidityRatio > BigDecimal("0.5") -> stabilityScore += 10
            liquidityRatio > BigDecimal("0.25") -> stabilityScore += 5
            else -> stabilityScore -= 5
        }
        
        // Credit score assessment
        when {
            creditScore >= 800 -> stabilityScore += 15
            creditScore >= 740 -> stabilityScore += 10
            creditScore >= 670 -> stabilityScore += 5
            creditScore >= 580 -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        return when {
            stabilityScore >= 30 -> FinancialStability.EXCELLENT
            stabilityScore >= 15 -> FinancialStability.GOOD
            stabilityScore >= 0 -> FinancialStability.FAIR
            else -> FinancialStability.POOR
        }
    }

    private fun assessOccupationRisk(occupation: String): BigDecimal {
        return when (occupation.uppercase()) {
            "TEACHER", "ACCOUNTANT", "ENGINEER", "LAWYER" -> BigDecimal("1.00")
            "POLICE_OFFICER", "FIREFIGHTER" -> BigDecimal("1.25")
            "PILOT", "CONSTRUCTION_WORKER" -> BigDecimal("1.50")
            "MINER", "LOGGER", "COMMERCIAL_FISHERMAN" -> BigDecimal("2.00")
            else -> BigDecimal("1.00")
        }
    }

    private fun assessLifestyleRisk(lifestyle: LifestyleInfo): BigDecimal {
        var factor = BigDecimal("1.00")
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            factor = factor.multiply(BigDecimal("1.15"))
        }
        
        if (lifestyle.hazardousActivities.isNotEmpty()) {
            factor = factor.multiply(BigDecimal("1.10"))
        }
        
        if (lifestyle.exerciseFrequency == "REGULAR") {
            factor = factor.multiply(BigDecimal("0.95"))
        }
        
        return factor
    }

    private fun assessGeographicRisk(state: String): BigDecimal {
        // Simplified geographic risk factors
        return when (state.uppercase()) {
            "CA", "FL", "TX" -> BigDecimal("1.05") // Higher cost states
            "NY", "NJ", "CT" -> BigDecimal("1.10") // High cost, high risk
            "WY", "MT", "ND" -> BigDecimal("0.95") // Lower risk states
            else -> BigDecimal("1.00")
        }
    }

    private fun determineOverallRiskClass(ratingFactors: Map<String, BigDecimal>): RiskClass {
        val overallFactor = ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
        
        return when {
            overallFactor < BigDecimal("0.90") -> RiskClass.PREFERRED
            overallFactor <= BigDecimal("1.10") -> RiskClass.STANDARD
            overallFactor <= BigDecimal("2.00") -> RiskClass.SUBSTANDARD
            else -> RiskClass.DECLINED
        }
    }

    private fun calculateOverallRiskScore(ratingFactors: Map<String, BigDecimal>): BigDecimal {
        return ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
    }

    private fun generateRiskNotes(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): List<String> {
        val notes = mutableListOf<String>()
        
        if (medicalAssessment.riskScore > 50) {
            notes.add("Elevated medical risk due to health conditions")
        }
        
        if (financialAssessment.debtToIncomeRatio > BigDecimal("0.40")) {
            notes.add("High debt-to-income ratio may affect financial stability")
        }
        
        if (financialAssessment.additionalDocumentationRequired) {
            notes.add("Additional financial documentation required for requested coverage amount")
        }
        
        return notes
    }

    private fun makeUnderwritingDecision(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment, riskAssessment: RiskAssessment): UnderwritingDecision {
        return when {
            medicalAssessment.riskClass == MedicalRiskClass.DECLINED -> UnderwritingDecision.DECLINE
            financialAssessment.financialJustification == FinancialJustification.INSUFFICIENT_JUSTIFICATION -> UnderwritingDecision.DECLINE
            riskAssessment.riskClass == RiskClass.DECLINED -> UnderwritingDecision.DECLINE
            medicalAssessment.riskClass == MedicalRiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            riskAssessment.riskClass == RiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            medicalAssessment.medicalExamRequired -> UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS
            else -> UnderwritingDecision.APPROVE_AS_APPLIED
        }
    }

    private fun determineConditions(decision: UnderwritingDecision, riskAssessment: RiskAssessment): List<String> {
        val conditions = mutableListOf<String>()
        
        when (decision) {
            UnderwritingDecision.APPROVE_WITH_RATING -> {
                conditions.add("Policy issued with ${riskAssessment.overallRiskScore}x rating")
            }
            UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS -> {
                conditions.add("Medical exam required before final decision")
            }
            else -> { /* No additional conditions */ }
        }
        
        return conditions
    }

    private fun getCurrentUnderwriterId(): String = "UW-${System.currentTimeMillis()}"
}

// Data classes and enums for underwriting
data class InsuranceApplication(
    val applicationId: String,
    val applicantInfo: ApplicantInfo,
    val medicalInfo: MedicalInfo,
    val financialInfo: FinancialInfo,
    val requestedCoverage: BigDecimal,
    val productType: String
)

data class ApplicantInfo(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val occupation: String,
    val residenceState: String
)

data class MedicalInfo(
    val age: Int,
    val height: Int, // inches
    val weight: Int, // pounds
    val isSmoker: Boolean,
    val medicalHistory: List<MedicalCondition>,
    val familyHistory: List<FamilyMedicalHistory>,
    val lifestyle: LifestyleInfo
)

data class MedicalCondition(
    val type: String,
    val diagnosisDate: LocalDate,
    val severity: String,
    val treatment: String,
    val yearsInRemission: Int = 0
)

data class FamilyMedicalHistory(
    val relationship: String,
    val condition: String,
    val ageAtDiagnosis: Int
)

data class LifestyleInfo(
    val alcoholConsumption: String, // NONE, LIGHT, MODERATE, HEAVY
    val exerciseFrequency: String, // NEVER, OCCASIONAL, REGULAR, FREQUENT
    val hazardousActivities: List<String>
)

data class FinancialInfo(
    val annualIncome: BigDecimal,
    val netWorth: BigDecimal,
    val liquidAssets: BigDecimal,
    val totalDebt: BigDecimal,
    val monthlyExpenses: BigDecimal,
    val creditScore: Int
)

data class UnderwritingResult(
    val applicationId: String,
    val decision: UnderwritingDecision,
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val medicalAssessment: MedicalAssessment,
    val financialAssessment: FinancialAssessment,
    val recommendedPremium: BigDecimal,
    val conditions: List<String>,
    val underwriterId: String,
    val underwritingDate: LocalDateTime
)

data class MedicalAssessment(
    val riskScore: Int,
    val riskClass: MedicalRiskClass,
    val riskFactors: List<MedicalRiskFactor>,
    val medicalExamRequired: Boolean,
    val additionalRequirements: List<String>
)

data class FinancialAssessment(
    val maxRecommendedCoverage: BigDecimal,
    val financialJustification: FinancialJustification,
    val debtToIncomeRatio: BigDecimal,
    val liquidityRatio: BigDecimal,
    val financialStability: FinancialStability,
    val additionalDocumentationRequired: Boolean
)

data class RiskAssessment(
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val overallRiskScore: BigDecimal,
    val riskNotes: List<String>
)

data class MedicalRiskFactor(
    val category: String,
    val description: String,
    val score: Int
)

data class RiskFactor(
    val description: String,
    val score: Int
)

enum class Gender { MALE, FEMALE, OTHER }

enum class UnderwritingDecision {
    APPROVE_AS_APPLIED, APPROVE_WITH_RATING, DECLINE, POSTPONE_PENDING_REQUIREMENTS
}

enum class MedicalRiskClass {
    SUPER_PREFERRED, PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class FinancialJustification {
    INCOME_REPLACEMENT, ESTATE_PLANNING, BUSINESS_PROTECTION, INSUFFICIENT_JUSTIFICATION
}

enum class FinancialStability {
    EXCELLENT, GOOD, FAIR, POOR
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow

/**
 * Underwriting engine for risk assessment and policy approval
 * Handles medical underwriting, financial underwriting, and risk classification
 */
@Service
class UnderwritingEngine {

    /**
     * Performs comprehensive underwriting assessment
     */
    fun performUnderwriting(application: InsuranceApplication): UnderwritingResult {
        val medicalAssessment = performMedicalUnderwriting(application.medicalInfo)
        val financialAssessment = performFinancialUnderwriting(application.financialInfo, application.requestedCoverage)
        val riskAssessment = assessOverallRisk(application, medicalAssessment, financialAssessment)
        
        val decision = makeUnderwritingDecision(medicalAssessment, financialAssessment, riskAssessment)
        
        return UnderwritingResult(
            applicationId = application.applicationId,
            decision = decision,
            riskClass = riskAssessment.riskClass,
            ratingFactors = riskAssessment.ratingFactors,
            medicalAssessment = medicalAssessment,
            financialAssessment = financialAssessment,
            recommendedPremium = calculatePremium(application.requestedCoverage, riskAssessment),
            conditions = determineConditions(decision, riskAssessment),
            underwriterId = getCurrentUnderwriterId(),
            underwritingDate = LocalDateTime.now()
        )
    }

    /**
     * Performs medical underwriting assessment
     */
    fun performMedicalUnderwriting(medicalInfo: MedicalInfo): MedicalAssessment {
        val riskFactors = mutableListOf<MedicalRiskFactor>()
        var riskScore = 0
        
        // Age factor
        val ageFactor = calculateAgeFactor(medicalInfo.age)
        riskScore += ageFactor.score
        riskFactors.add(MedicalRiskFactor("AGE", ageFactor.description, ageFactor.score))
        
        // Smoking status
        if (medicalInfo.isSmoker) {
            riskScore += 50
            riskFactors.add(MedicalRiskFactor("SMOKING", "Current smoker", 50))
        }
        
        // BMI assessment
        val bmiAssessment = assessBMI(medicalInfo.height, medicalInfo.weight)
        riskScore += bmiAssessment.score
        riskFactors.add(MedicalRiskFactor("BMI", bmiAssessment.description, bmiAssessment.score))
        
        // Medical history
        medicalInfo.medicalHistory.forEach { condition ->
            val conditionRisk = assessMedicalCondition(condition)
            riskScore += conditionRisk.score
            riskFactors.add(MedicalRiskFactor("MEDICAL_HISTORY", conditionRisk.description, conditionRisk.score))
        }
        
        // Family history
        medicalInfo.familyHistory.forEach { condition ->
            val familyRisk = assessFamilyHistory(condition)
            riskScore += familyRisk.score
            riskFactors.add(MedicalRiskFactor("FAMILY_HISTORY", familyRisk.description, familyRisk.score))
        }
        
        // Lifestyle factors
        val lifestyleRisk = assessLifestyle(medicalInfo.lifestyle)
        riskScore += lifestyleRisk.score
        riskFactors.add(MedicalRiskFactor("LIFESTYLE", lifestyleRisk.description, lifestyleRisk.score))
        
        val medicalExamRequired = determineMedicalExamRequirement(medicalInfo, riskScore)
        val riskClass = determineMedicalRiskClass(riskScore)
        
        return MedicalAssessment(
            riskScore = riskScore,
            riskClass = riskClass,
            riskFactors = riskFactors,
            medicalExamRequired = medicalExamRequired,
            additionalRequirements = determineAdditionalMedicalRequirements(riskScore, medicalInfo)
        )
    }

    /**
     * Performs financial underwriting assessment
     */
    fun performFinancialUnderwriting(financialInfo: FinancialInfo, requestedCoverage: BigDecimal): FinancialAssessment {
        val incomeMultiplier = calculateIncomeMultiplier(financialInfo.annualIncome)
        val maxCoverageByIncome = financialInfo.annualIncome.multiply(incomeMultiplier)
        
        val netWorthMultiplier = BigDecimal("0.25") // 25% of net worth
        val maxCoverageByNetWorth = financialInfo.netWorth.multiply(netWorthMultiplier)
        
        val maxRecommendedCoverage = maxOf(maxCoverageByIncome, maxCoverageByNetWorth)
        
        val financialJustification = when {
            requestedCoverage <= maxCoverageByIncome -> FinancialJustification.INCOME_REPLACEMENT
            requestedCoverage <= maxCoverageByNetWorth -> FinancialJustification.ESTATE_PLANNING
            requestedCoverage <= maxRecommendedCoverage -> FinancialJustification.BUSINESS_PROTECTION
            else -> FinancialJustification.INSUFFICIENT_JUSTIFICATION
        }
        
        val debtToIncomeRatio = financialInfo.totalDebt.divide(financialInfo.annualIncome, 4, java.math.RoundingMode.HALF_UP)
        val liquidityRatio = financialInfo.liquidAssets.divide(financialInfo.monthlyExpenses.multiply(BigDecimal("12")), 4, java.math.RoundingMode.HALF_UP)
        
        val financialStability = assessFinancialStability(debtToIncomeRatio, liquidityRatio, financialInfo.creditScore)
        
        return FinancialAssessment(
            maxRecommendedCoverage = maxRecommendedCoverage,
            financialJustification = financialJustification,
            debtToIncomeRatio = debtToIncomeRatio,
            liquidityRatio = liquidityRatio,
            financialStability = financialStability,
            additionalDocumentationRequired = requestedCoverage > maxRecommendedCoverage
        )
    }

    /**
     * Assesses overall risk combining medical and financial factors
     */
    fun assessOverallRisk(application: InsuranceApplication, medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): RiskAssessment {
        val ratingFactors = mutableMapOf<String, BigDecimal>()
        
        // Medical rating factors
        when (medicalAssessment.riskClass) {
            MedicalRiskClass.SUPER_PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.85")
            MedicalRiskClass.PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.95")
            MedicalRiskClass.STANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.00")
            MedicalRiskClass.SUBSTANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.25")
            MedicalRiskClass.DECLINED -> ratingFactors["MEDICAL"] = BigDecimal("999.99") // Effectively declined
        }
        
        // Financial rating factors
        when (financialAssessment.financialJustification) {
            FinancialJustification.INCOME_REPLACEMENT -> ratingFactors["FINANCIAL"] = BigDecimal("1.00")
            FinancialJustification.ESTATE_PLANNING -> ratingFactors["FINANCIAL"] = BigDecimal("1.05")
            FinancialJustification.BUSINESS_PROTECTION -> ratingFactors["FINANCIAL"] = BigDecimal("1.10")
            FinancialJustification.INSUFFICIENT_JUSTIFICATION -> ratingFactors["FINANCIAL"] = BigDecimal("999.99")
        }
        
        // Occupation rating
        val occupationFactor = assessOccupationRisk(application.applicantInfo.occupation)
        ratingFactors["OCCUPATION"] = occupationFactor
        
        // Lifestyle rating
        val lifestyleFactor = assessLifestyleRisk(application.medicalInfo.lifestyle)
        ratingFactors["LIFESTYLE"] = lifestyleFactor
        
        // Geographic rating
        val geographicFactor = assessGeographicRisk(application.applicantInfo.residenceState)
        ratingFactors["GEOGRAPHIC"] = geographicFactor
        
        val overallRiskClass = determineOverallRiskClass(ratingFactors)
        
        return RiskAssessment(
            riskClass = overallRiskClass,
            ratingFactors = ratingFactors,
            overallRiskScore = calculateOverallRiskScore(ratingFactors),
            riskNotes = generateRiskNotes(medicalAssessment, financialAssessment)
        )
    }

    /**
     * Calculates premium based on coverage amount and risk assessment
     */
    fun calculatePremium(coverageAmount: BigDecimal, riskAssessment: RiskAssessment): BigDecimal {
        val basePremiumRate = BigDecimal("0.001") // $1 per $1000 of coverage
        var premium = coverageAmount.multiply(basePremiumRate)
        
        // Apply rating factors
        riskAssessment.ratingFactors.values.forEach { factor ->
            premium = premium.multiply(factor)
        }
        
        // Apply minimum premium
        val minimumPremium = BigDecimal("100")
        if (premium < minimumPremium) {
            premium = minimumPremium
        }
        
        return premium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates mortality table lookup for actuarial calculations
     */
    fun lookupMortalityRate(age: Int, gender: Gender, riskClass: RiskClass): BigDecimal {
        // Simplified mortality table - in reality this would be much more complex
        val baseMortalityRate = when {
            age < 30 -> BigDecimal("0.0008")
            age < 40 -> BigDecimal("0.0012")
            age < 50 -> BigDecimal("0.0020")
            age < 60 -> BigDecimal("0.0035")
            age < 70 -> BigDecimal("0.0065")
            else -> BigDecimal("0.0120")
        }
        
        // Gender adjustment
        val genderFactor = if (gender == Gender.FEMALE) BigDecimal("0.85") else BigDecimal("1.00")
        
        // Risk class adjustment
        val riskFactor = when (riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.80")
            RiskClass.STANDARD -> BigDecimal("1.00")
            RiskClass.SUBSTANDARD -> BigDecimal("1.50")
            RiskClass.DECLINED -> BigDecimal("999.99")
        }
        
        return baseMortalityRate.multiply(genderFactor).multiply(riskFactor)
    }

    // Private helper methods
    private fun calculateAgeFactor(age: Int): RiskFactor {
        return when {
            age < 25 -> RiskFactor("Young adult", 5)
            age < 35 -> RiskFactor("Young adult - preferred", 0)
            age < 45 -> RiskFactor("Middle age - standard", 10)
            age < 55 -> RiskFactor("Middle age - increased risk", 20)
            age < 65 -> RiskFactor("Senior - higher risk", 35)
            else -> RiskFactor("Senior - high risk", 50)
        }
    }

    private fun assessBMI(heightInches: Int, weightPounds: Int): RiskFactor {
        val heightMeters = heightInches * 0.0254
        val weightKg = weightPounds * 0.453592
        val bmi = weightKg / (heightMeters.pow(2))
        
        return when {
            bmi < 18.5 -> RiskFactor("Underweight", 15)
            bmi < 25.0 -> RiskFactor("Normal weight", 0)
            bmi < 30.0 -> RiskFactor("Overweight", 10)
            bmi < 35.0 -> RiskFactor("Obese Class I", 25)
            bmi < 40.0 -> RiskFactor("Obese Class II", 40)
            else -> RiskFactor("Obese Class III", 60)
        }
    }

    private fun assessMedicalCondition(condition: MedicalCondition): RiskFactor {
        return when (condition.type) {
            "DIABETES" -> when (condition.severity) {
                "CONTROLLED" -> RiskFactor("Controlled diabetes", 30)
                "UNCONTROLLED" -> RiskFactor("Uncontrolled diabetes", 80)
                else -> RiskFactor("Diabetes", 50)
            }
            "HYPERTENSION" -> when (condition.severity) {
                "MILD" -> RiskFactor("Mild hypertension", 15)
                "MODERATE" -> RiskFactor("Moderate hypertension", 30)
                "SEVERE" -> RiskFactor("Severe hypertension", 60)
                else -> RiskFactor("Hypertension", 25)
            }
            "HEART_DISEASE" -> RiskFactor("Heart disease", 100)
            "CANCER" -> when (condition.yearsInRemission) {
                in 0..2 -> RiskFactor("Recent cancer history", 150)
                in 3..5 -> RiskFactor("Cancer history", 75)
                else -> RiskFactor("Remote cancer history", 25)
            }
            else -> RiskFactor("Other medical condition", 20)
        }
    }

    private fun assessFamilyHistory(condition: FamilyMedicalHistory): RiskFactor {
        val baseScore = when (condition.condition) {
            "HEART_DISEASE" -> 15
            "CANCER" -> 10
            "DIABETES" -> 8
            "STROKE" -> 12
            else -> 5
        }
        
        val relationshipMultiplier = when (condition.relationship) {
            "PARENT" -> 1.0
            "SIBLING" -> 0.8
            "GRANDPARENT" -> 0.5
            else -> 0.3
        }
        
        val ageMultiplier = if (condition.ageAtDiagnosis < 60) 1.5 else 1.0
        
        val adjustedScore = (baseScore * relationshipMultiplier * ageMultiplier).toInt()
        
        return RiskFactor("Family history of ${condition.condition}", adjustedScore)
    }

    private fun assessLifestyle(lifestyle: LifestyleInfo): RiskFactor {
        var score = 0
        val factors = mutableListOf<String>()
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            score += 25
            factors.add("heavy drinking")
        } else if (lifestyle.alcoholConsumption == "MODERATE") {
            score += 5
            factors.add("moderate drinking")
        }
        
        if (lifestyle.exerciseFrequency == "NEVER") {
            score += 15
            factors.add("sedentary lifestyle")
        } else if (lifestyle.exerciseFrequency == "REGULAR") {
            score -= 5
            factors.add("regular exercise")
        }
        
        lifestyle.hazardousActivities.forEach { activity ->
            when (activity) {
                "SKYDIVING" -> { score += 30; factors.add("skydiving") }
                "ROCK_CLIMBING" -> { score += 20; factors.add("rock climbing") }
                "MOTORCYCLE_RACING" -> { score += 40; factors.add("motorcycle racing") }
                "SCUBA_DIVING" -> { score += 15; factors.add("scuba diving") }
            }
        }
        
        val description = if (factors.isEmpty()) "Standard lifestyle" else "Lifestyle factors: ${factors.joinToString(", ")}"
        
        return RiskFactor(description, maxOf(0, score))
    }

    private fun determineMedicalExamRequirement(medicalInfo: MedicalInfo, riskScore: Int): Boolean {
        return riskScore > 50 || medicalInfo.age > 50 || medicalInfo.medicalHistory.isNotEmpty()
    }

    private fun determineAdditionalMedicalRequirements(riskScore: Int, medicalInfo: MedicalInfo): List<String> {
        val requirements = mutableListOf<String>()
        
        if (riskScore > 75) {
            requirements.add("Physician's statement")
            requirements.add("Medical records")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "HEART_DISEASE" }) {
            requirements.add("Cardiac stress test")
            requirements.add("EKG")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "DIABETES" }) {
            requirements.add("HbA1c test")
            requirements.add("Diabetic panel")
        }
        
        if (medicalInfo.age > 65) {
            requirements.add("Cognitive assessment")
        }
        
        return requirements
    }

    private fun determineMedicalRiskClass(riskScore: Int): MedicalRiskClass {
        return when {
            riskScore <= 10 -> MedicalRiskClass.SUPER_PREFERRED
            riskScore <= 25 -> MedicalRiskClass.PREFERRED
            riskScore <= 50 -> MedicalRiskClass.STANDARD
            riskScore <= 100 -> MedicalRiskClass.SUBSTANDARD
            else -> MedicalRiskClass.DECLINED
        }
    }

    private fun calculateIncomeMultiplier(annualIncome: BigDecimal): BigDecimal {
        return when {
            annualIncome < BigDecimal("50000") -> BigDecimal("10")
            annualIncome < BigDecimal("100000") -> BigDecimal("15")
            annualIncome < BigDecimal("250000") -> BigDecimal("20")
            annualIncome < BigDecimal("500000") -> BigDecimal("25")
            else -> BigDecimal("30")
        }
    }

    private fun assessFinancialStability(debtToIncomeRatio: BigDecimal, liquidityRatio: BigDecimal, creditScore: Int): FinancialStability {
        var stabilityScore = 0
        
        // Debt-to-income assessment
        when {
            debtToIncomeRatio < BigDecimal("0.20") -> stabilityScore += 20
            debtToIncomeRatio < BigDecimal("0.36") -> stabilityScore += 10
            debtToIncomeRatio < BigDecimal("0.50") -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        // Liquidity assessment
        when {
            liquidityRatio > BigDecimal("1.0") -> stabilityScore += 15
            liquidityRatio > BigDecimal("0.5") -> stabilityScore += 10
            liquidityRatio > BigDecimal("0.25") -> stabilityScore += 5
            else -> stabilityScore -= 5
        }
        
        // Credit score assessment
        when {
            creditScore >= 800 -> stabilityScore += 15
            creditScore >= 740 -> stabilityScore += 10
            creditScore >= 670 -> stabilityScore += 5
            creditScore >= 580 -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        return when {
            stabilityScore >= 30 -> FinancialStability.EXCELLENT
            stabilityScore >= 15 -> FinancialStability.GOOD
            stabilityScore >= 0 -> FinancialStability.FAIR
            else -> FinancialStability.POOR
        }
    }

    private fun assessOccupationRisk(occupation: String): BigDecimal {
        return when (occupation.uppercase()) {
            "TEACHER", "ACCOUNTANT", "ENGINEER", "LAWYER" -> BigDecimal("1.00")
            "POLICE_OFFICER", "FIREFIGHTER" -> BigDecimal("1.25")
            "PILOT", "CONSTRUCTION_WORKER" -> BigDecimal("1.50")
            "MINER", "LOGGER", "COMMERCIAL_FISHERMAN" -> BigDecimal("2.00")
            else -> BigDecimal("1.00")
        }
    }

    private fun assessLifestyleRisk(lifestyle: LifestyleInfo): BigDecimal {
        var factor = BigDecimal("1.00")
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            factor = factor.multiply(BigDecimal("1.15"))
        }
        
        if (lifestyle.hazardousActivities.isNotEmpty()) {
            factor = factor.multiply(BigDecimal("1.10"))
        }
        
        if (lifestyle.exerciseFrequency == "REGULAR") {
            factor = factor.multiply(BigDecimal("0.95"))
        }
        
        return factor
    }

    private fun assessGeographicRisk(state: String): BigDecimal {
        // Simplified geographic risk factors
        return when (state.uppercase()) {
            "CA", "FL", "TX" -> BigDecimal("1.05") // Higher cost states
            "NY", "NJ", "CT" -> BigDecimal("1.10") // High cost, high risk
            "WY", "MT", "ND" -> BigDecimal("0.95") // Lower risk states
            else -> BigDecimal("1.00")
        }
    }

    private fun determineOverallRiskClass(ratingFactors: Map<String, BigDecimal>): RiskClass {
        val overallFactor = ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
        
        return when {
            overallFactor < BigDecimal("0.90") -> RiskClass.PREFERRED
            overallFactor <= BigDecimal("1.10") -> RiskClass.STANDARD
            overallFactor <= BigDecimal("2.00") -> RiskClass.SUBSTANDARD
            else -> RiskClass.DECLINED
        }
    }

    private fun calculateOverallRiskScore(ratingFactors: Map<String, BigDecimal>): BigDecimal {
        return ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
    }

    private fun generateRiskNotes(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): List<String> {
        val notes = mutableListOf<String>()
        
        if (medicalAssessment.riskScore > 50) {
            notes.add("Elevated medical risk due to health conditions")
        }
        
        if (financialAssessment.debtToIncomeRatio > BigDecimal("0.40")) {
            notes.add("High debt-to-income ratio may affect financial stability")
        }
        
        if (financialAssessment.additionalDocumentationRequired) {
            notes.add("Additional financial documentation required for requested coverage amount")
        }
        
        return notes
    }

    private fun makeUnderwritingDecision(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment, riskAssessment: RiskAssessment): UnderwritingDecision {
        return when {
            medicalAssessment.riskClass == MedicalRiskClass.DECLINED -> UnderwritingDecision.DECLINE
            financialAssessment.financialJustification == FinancialJustification.INSUFFICIENT_JUSTIFICATION -> UnderwritingDecision.DECLINE
            riskAssessment.riskClass == RiskClass.DECLINED -> UnderwritingDecision.DECLINE
            medicalAssessment.riskClass == MedicalRiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            riskAssessment.riskClass == RiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            medicalAssessment.medicalExamRequired -> UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS
            else -> UnderwritingDecision.APPROVE_AS_APPLIED
        }
    }

    private fun determineConditions(decision: UnderwritingDecision, riskAssessment: RiskAssessment): List<String> {
        val conditions = mutableListOf<String>()
        
        when (decision) {
            UnderwritingDecision.APPROVE_WITH_RATING -> {
                conditions.add("Policy issued with ${riskAssessment.overallRiskScore}x rating")
            }
            UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS -> {
                conditions.add("Medical exam required before final decision")
            }
            else -> { /* No additional conditions */ }
        }
        
        return conditions
    }

    private fun getCurrentUnderwriterId(): String = "UW-${System.currentTimeMillis()}"
}

// Data classes and enums for underwriting
data class InsuranceApplication(
    val applicationId: String,
    val applicantInfo: ApplicantInfo,
    val medicalInfo: MedicalInfo,
    val financialInfo: FinancialInfo,
    val requestedCoverage: BigDecimal,
    val productType: String
)

data class ApplicantInfo(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val occupation: String,
    val residenceState: String
)

data class MedicalInfo(
    val age: Int,
    val height: Int, // inches
    val weight: Int, // pounds
    val isSmoker: Boolean,
    val medicalHistory: List<MedicalCondition>,
    val familyHistory: List<FamilyMedicalHistory>,
    val lifestyle: LifestyleInfo
)

data class MedicalCondition(
    val type: String,
    val diagnosisDate: LocalDate,
    val severity: String,
    val treatment: String,
    val yearsInRemission: Int = 0
)

data class FamilyMedicalHistory(
    val relationship: String,
    val condition: String,
    val ageAtDiagnosis: Int
)

data class LifestyleInfo(
    val alcoholConsumption: String, // NONE, LIGHT, MODERATE, HEAVY
    val exerciseFrequency: String, // NEVER, OCCASIONAL, REGULAR, FREQUENT
    val hazardousActivities: List<String>
)

data class FinancialInfo(
    val annualIncome: BigDecimal,
    val netWorth: BigDecimal,
    val liquidAssets: BigDecimal,
    val totalDebt: BigDecimal,
    val monthlyExpenses: BigDecimal,
    val creditScore: Int
)

data class UnderwritingResult(
    val applicationId: String,
    val decision: UnderwritingDecision,
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val medicalAssessment: MedicalAssessment,
    val financialAssessment: FinancialAssessment,
    val recommendedPremium: BigDecimal,
    val conditions: List<String>,
    val underwriterId: String,
    val underwritingDate: LocalDateTime
)

data class MedicalAssessment(
    val riskScore: Int,
    val riskClass: MedicalRiskClass,
    val riskFactors: List<MedicalRiskFactor>,
    val medicalExamRequired: Boolean,
    val additionalRequirements: List<String>
)

data class FinancialAssessment(
    val maxRecommendedCoverage: BigDecimal,
    val financialJustification: FinancialJustification,
    val debtToIncomeRatio: BigDecimal,
    val liquidityRatio: BigDecimal,
    val financialStability: FinancialStability,
    val additionalDocumentationRequired: Boolean
)

data class RiskAssessment(
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val overallRiskScore: BigDecimal,
    val riskNotes: List<String>
)

data class MedicalRiskFactor(
    val category: String,
    val description: String,
    val score: Int
)

data class RiskFactor(
    val description: String,
    val score: Int
)

enum class Gender { MALE, FEMALE, OTHER }

enum class UnderwritingDecision {
    APPROVE_AS_APPLIED, APPROVE_WITH_RATING, DECLINE, POSTPONE_PENDING_REQUIREMENTS
}

enum class MedicalRiskClass {
    SUPER_PREFERRED, PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class FinancialJustification {
    INCOME_REPLACEMENT, ESTATE_PLANNING, BUSINESS_PROTECTION, INSUFFICIENT_JUSTIFICATION
}

enum class FinancialStability {
    EXCELLENT, GOOD, FAIR, POOR
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow

/**
 * Underwriting engine for risk assessment and policy approval
 * Handles medical underwriting, financial underwriting, and risk classification
 */
@Service
class UnderwritingEngine {

    /**
     * Performs comprehensive underwriting assessment
     */
    fun performUnderwriting(application: InsuranceApplication): UnderwritingResult {
        val medicalAssessment = performMedicalUnderwriting(application.medicalInfo)
        val financialAssessment = performFinancialUnderwriting(application.financialInfo, application.requestedCoverage)
        val riskAssessment = assessOverallRisk(application, medicalAssessment, financialAssessment)
        
        val decision = makeUnderwritingDecision(medicalAssessment, financialAssessment, riskAssessment)
        
        return UnderwritingResult(
            applicationId = application.applicationId,
            decision = decision,
            riskClass = riskAssessment.riskClass,
            ratingFactors = riskAssessment.ratingFactors,
            medicalAssessment = medicalAssessment,
            financialAssessment = financialAssessment,
            recommendedPremium = calculatePremium(application.requestedCoverage, riskAssessment),
            conditions = determineConditions(decision, riskAssessment),
            underwriterId = getCurrentUnderwriterId(),
            underwritingDate = LocalDateTime.now()
        )
    }

    /**
     * Performs medical underwriting assessment
     */
    fun performMedicalUnderwriting(medicalInfo: MedicalInfo): MedicalAssessment {
        val riskFactors = mutableListOf<MedicalRiskFactor>()
        var riskScore = 0
        
        // Age factor
        val ageFactor = calculateAgeFactor(medicalInfo.age)
        riskScore += ageFactor.score
        riskFactors.add(MedicalRiskFactor("AGE", ageFactor.description, ageFactor.score))
        
        // Smoking status
        if (medicalInfo.isSmoker) {
            riskScore += 50
            riskFactors.add(MedicalRiskFactor("SMOKING", "Current smoker", 50))
        }
        
        // BMI assessment
        val bmiAssessment = assessBMI(medicalInfo.height, medicalInfo.weight)
        riskScore += bmiAssessment.score
        riskFactors.add(MedicalRiskFactor("BMI", bmiAssessment.description, bmiAssessment.score))
        
        // Medical history
        medicalInfo.medicalHistory.forEach { condition ->
            val conditionRisk = assessMedicalCondition(condition)
            riskScore += conditionRisk.score
            riskFactors.add(MedicalRiskFactor("MEDICAL_HISTORY", conditionRisk.description, conditionRisk.score))
        }
        
        // Family history
        medicalInfo.familyHistory.forEach { condition ->
            val familyRisk = assessFamilyHistory(condition)
            riskScore += familyRisk.score
            riskFactors.add(MedicalRiskFactor("FAMILY_HISTORY", familyRisk.description, familyRisk.score))
        }
        
        // Lifestyle factors
        val lifestyleRisk = assessLifestyle(medicalInfo.lifestyle)
        riskScore += lifestyleRisk.score
        riskFactors.add(MedicalRiskFactor("LIFESTYLE", lifestyleRisk.description, lifestyleRisk.score))
        
        val medicalExamRequired = determineMedicalExamRequirement(medicalInfo, riskScore)
        val riskClass = determineMedicalRiskClass(riskScore)
        
        return MedicalAssessment(
            riskScore = riskScore,
            riskClass = riskClass,
            riskFactors = riskFactors,
            medicalExamRequired = medicalExamRequired,
            additionalRequirements = determineAdditionalMedicalRequirements(riskScore, medicalInfo)
        )
    }

    /**
     * Performs financial underwriting assessment
     */
    fun performFinancialUnderwriting(financialInfo: FinancialInfo, requestedCoverage: BigDecimal): FinancialAssessment {
        val incomeMultiplier = calculateIncomeMultiplier(financialInfo.annualIncome)
        val maxCoverageByIncome = financialInfo.annualIncome.multiply(incomeMultiplier)
        
        val netWorthMultiplier = BigDecimal("0.25") // 25% of net worth
        val maxCoverageByNetWorth = financialInfo.netWorth.multiply(netWorthMultiplier)
        
        val maxRecommendedCoverage = maxOf(maxCoverageByIncome, maxCoverageByNetWorth)
        
        val financialJustification = when {
            requestedCoverage <= maxCoverageByIncome -> FinancialJustification.INCOME_REPLACEMENT
            requestedCoverage <= maxCoverageByNetWorth -> FinancialJustification.ESTATE_PLANNING
            requestedCoverage <= maxRecommendedCoverage -> FinancialJustification.BUSINESS_PROTECTION
            else -> FinancialJustification.INSUFFICIENT_JUSTIFICATION
        }
        
        val debtToIncomeRatio = financialInfo.totalDebt.divide(financialInfo.annualIncome, 4, java.math.RoundingMode.HALF_UP)
        val liquidityRatio = financialInfo.liquidAssets.divide(financialInfo.monthlyExpenses.multiply(BigDecimal("12")), 4, java.math.RoundingMode.HALF_UP)
        
        val financialStability = assessFinancialStability(debtToIncomeRatio, liquidityRatio, financialInfo.creditScore)
        
        return FinancialAssessment(
            maxRecommendedCoverage = maxRecommendedCoverage,
            financialJustification = financialJustification,
            debtToIncomeRatio = debtToIncomeRatio,
            liquidityRatio = liquidityRatio,
            financialStability = financialStability,
            additionalDocumentationRequired = requestedCoverage > maxRecommendedCoverage
        )
    }

    /**
     * Assesses overall risk combining medical and financial factors
     */
    fun assessOverallRisk(application: InsuranceApplication, medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): RiskAssessment {
        val ratingFactors = mutableMapOf<String, BigDecimal>()
        
        // Medical rating factors
        when (medicalAssessment.riskClass) {
            MedicalRiskClass.SUPER_PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.85")
            MedicalRiskClass.PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.95")
            MedicalRiskClass.STANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.00")
            MedicalRiskClass.SUBSTANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.25")
            MedicalRiskClass.DECLINED -> ratingFactors["MEDICAL"] = BigDecimal("999.99") // Effectively declined
        }
        
        // Financial rating factors
        when (financialAssessment.financialJustification) {
            FinancialJustification.INCOME_REPLACEMENT -> ratingFactors["FINANCIAL"] = BigDecimal("1.00")
            FinancialJustification.ESTATE_PLANNING -> ratingFactors["FINANCIAL"] = BigDecimal("1.05")
            FinancialJustification.BUSINESS_PROTECTION -> ratingFactors["FINANCIAL"] = BigDecimal("1.10")
            FinancialJustification.INSUFFICIENT_JUSTIFICATION -> ratingFactors["FINANCIAL"] = BigDecimal("999.99")
        }
        
        // Occupation rating
        val occupationFactor = assessOccupationRisk(application.applicantInfo.occupation)
        ratingFactors["OCCUPATION"] = occupationFactor
        
        // Lifestyle rating
        val lifestyleFactor = assessLifestyleRisk(application.medicalInfo.lifestyle)
        ratingFactors["LIFESTYLE"] = lifestyleFactor
        
        // Geographic rating
        val geographicFactor = assessGeographicRisk(application.applicantInfo.residenceState)
        ratingFactors["GEOGRAPHIC"] = geographicFactor
        
        val overallRiskClass = determineOverallRiskClass(ratingFactors)
        
        return RiskAssessment(
            riskClass = overallRiskClass,
            ratingFactors = ratingFactors,
            overallRiskScore = calculateOverallRiskScore(ratingFactors),
            riskNotes = generateRiskNotes(medicalAssessment, financialAssessment)
        )
    }

    /**
     * Calculates premium based on coverage amount and risk assessment
     */
    fun calculatePremium(coverageAmount: BigDecimal, riskAssessment: RiskAssessment): BigDecimal {
        val basePremiumRate = BigDecimal("0.001") // $1 per $1000 of coverage
        var premium = coverageAmount.multiply(basePremiumRate)
        
        // Apply rating factors
        riskAssessment.ratingFactors.values.forEach { factor ->
            premium = premium.multiply(factor)
        }
        
        // Apply minimum premium
        val minimumPremium = BigDecimal("100")
        if (premium < minimumPremium) {
            premium = minimumPremium
        }
        
        return premium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates mortality table lookup for actuarial calculations
     */
    fun lookupMortalityRate(age: Int, gender: Gender, riskClass: RiskClass): BigDecimal {
        // Simplified mortality table - in reality this would be much more complex
        val baseMortalityRate = when {
            age < 30 -> BigDecimal("0.0008")
            age < 40 -> BigDecimal("0.0012")
            age < 50 -> BigDecimal("0.0020")
            age < 60 -> BigDecimal("0.0035")
            age < 70 -> BigDecimal("0.0065")
            else -> BigDecimal("0.0120")
        }
        
        // Gender adjustment
        val genderFactor = if (gender == Gender.FEMALE) BigDecimal("0.85") else BigDecimal("1.00")
        
        // Risk class adjustment
        val riskFactor = when (riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.80")
            RiskClass.STANDARD -> BigDecimal("1.00")
            RiskClass.SUBSTANDARD -> BigDecimal("1.50")
            RiskClass.DECLINED -> BigDecimal("999.99")
        }
        
        return baseMortalityRate.multiply(genderFactor).multiply(riskFactor)
    }

    // Private helper methods
    private fun calculateAgeFactor(age: Int): RiskFactor {
        return when {
            age < 25 -> RiskFactor("Young adult", 5)
            age < 35 -> RiskFactor("Young adult - preferred", 0)
            age < 45 -> RiskFactor("Middle age - standard", 10)
            age < 55 -> RiskFactor("Middle age - increased risk", 20)
            age < 65 -> RiskFactor("Senior - higher risk", 35)
            else -> RiskFactor("Senior - high risk", 50)
        }
    }

    private fun assessBMI(heightInches: Int, weightPounds: Int): RiskFactor {
        val heightMeters = heightInches * 0.0254
        val weightKg = weightPounds * 0.453592
        val bmi = weightKg / (heightMeters.pow(2))
        
        return when {
            bmi < 18.5 -> RiskFactor("Underweight", 15)
            bmi < 25.0 -> RiskFactor("Normal weight", 0)
            bmi < 30.0 -> RiskFactor("Overweight", 10)
            bmi < 35.0 -> RiskFactor("Obese Class I", 25)
            bmi < 40.0 -> RiskFactor("Obese Class II", 40)
            else -> RiskFactor("Obese Class III", 60)
        }
    }

    private fun assessMedicalCondition(condition: MedicalCondition): RiskFactor {
        return when (condition.type) {
            "DIABETES" -> when (condition.severity) {
                "CONTROLLED" -> RiskFactor("Controlled diabetes", 30)
                "UNCONTROLLED" -> RiskFactor("Uncontrolled diabetes", 80)
                else -> RiskFactor("Diabetes", 50)
            }
            "HYPERTENSION" -> when (condition.severity) {
                "MILD" -> RiskFactor("Mild hypertension", 15)
                "MODERATE" -> RiskFactor("Moderate hypertension", 30)
                "SEVERE" -> RiskFactor("Severe hypertension", 60)
                else -> RiskFactor("Hypertension", 25)
            }
            "HEART_DISEASE" -> RiskFactor("Heart disease", 100)
            "CANCER" -> when (condition.yearsInRemission) {
                in 0..2 -> RiskFactor("Recent cancer history", 150)
                in 3..5 -> RiskFactor("Cancer history", 75)
                else -> RiskFactor("Remote cancer history", 25)
            }
            else -> RiskFactor("Other medical condition", 20)
        }
    }

    private fun assessFamilyHistory(condition: FamilyMedicalHistory): RiskFactor {
        val baseScore = when (condition.condition) {
            "HEART_DISEASE" -> 15
            "CANCER" -> 10
            "DIABETES" -> 8
            "STROKE" -> 12
            else -> 5
        }
        
        val relationshipMultiplier = when (condition.relationship) {
            "PARENT" -> 1.0
            "SIBLING" -> 0.8
            "GRANDPARENT" -> 0.5
            else -> 0.3
        }
        
        val ageMultiplier = if (condition.ageAtDiagnosis < 60) 1.5 else 1.0
        
        val adjustedScore = (baseScore * relationshipMultiplier * ageMultiplier).toInt()
        
        return RiskFactor("Family history of ${condition.condition}", adjustedScore)
    }

    private fun assessLifestyle(lifestyle: LifestyleInfo): RiskFactor {
        var score = 0
        val factors = mutableListOf<String>()
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            score += 25
            factors.add("heavy drinking")
        } else if (lifestyle.alcoholConsumption == "MODERATE") {
            score += 5
            factors.add("moderate drinking")
        }
        
        if (lifestyle.exerciseFrequency == "NEVER") {
            score += 15
            factors.add("sedentary lifestyle")
        } else if (lifestyle.exerciseFrequency == "REGULAR") {
            score -= 5
            factors.add("regular exercise")
        }
        
        lifestyle.hazardousActivities.forEach { activity ->
            when (activity) {
                "SKYDIVING" -> { score += 30; factors.add("skydiving") }
                "ROCK_CLIMBING" -> { score += 20; factors.add("rock climbing") }
                "MOTORCYCLE_RACING" -> { score += 40; factors.add("motorcycle racing") }
                "SCUBA_DIVING" -> { score += 15; factors.add("scuba diving") }
            }
        }
        
        val description = if (factors.isEmpty()) "Standard lifestyle" else "Lifestyle factors: ${factors.joinToString(", ")}"
        
        return RiskFactor(description, maxOf(0, score))
    }

    private fun determineMedicalExamRequirement(medicalInfo: MedicalInfo, riskScore: Int): Boolean {
        return riskScore > 50 || medicalInfo.age > 50 || medicalInfo.medicalHistory.isNotEmpty()
    }

    private fun determineAdditionalMedicalRequirements(riskScore: Int, medicalInfo: MedicalInfo): List<String> {
        val requirements = mutableListOf<String>()
        
        if (riskScore > 75) {
            requirements.add("Physician's statement")
            requirements.add("Medical records")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "HEART_DISEASE" }) {
            requirements.add("Cardiac stress test")
            requirements.add("EKG")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "DIABETES" }) {
            requirements.add("HbA1c test")
            requirements.add("Diabetic panel")
        }
        
        if (medicalInfo.age > 65) {
            requirements.add("Cognitive assessment")
        }
        
        return requirements
    }

    private fun determineMedicalRiskClass(riskScore: Int): MedicalRiskClass {
        return when {
            riskScore <= 10 -> MedicalRiskClass.SUPER_PREFERRED
            riskScore <= 25 -> MedicalRiskClass.PREFERRED
            riskScore <= 50 -> MedicalRiskClass.STANDARD
            riskScore <= 100 -> MedicalRiskClass.SUBSTANDARD
            else -> MedicalRiskClass.DECLINED
        }
    }

    private fun calculateIncomeMultiplier(annualIncome: BigDecimal): BigDecimal {
        return when {
            annualIncome < BigDecimal("50000") -> BigDecimal("10")
            annualIncome < BigDecimal("100000") -> BigDecimal("15")
            annualIncome < BigDecimal("250000") -> BigDecimal("20")
            annualIncome < BigDecimal("500000") -> BigDecimal("25")
            else -> BigDecimal("30")
        }
    }

    private fun assessFinancialStability(debtToIncomeRatio: BigDecimal, liquidityRatio: BigDecimal, creditScore: Int): FinancialStability {
        var stabilityScore = 0
        
        // Debt-to-income assessment
        when {
            debtToIncomeRatio < BigDecimal("0.20") -> stabilityScore += 20
            debtToIncomeRatio < BigDecimal("0.36") -> stabilityScore += 10
            debtToIncomeRatio < BigDecimal("0.50") -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        // Liquidity assessment
        when {
            liquidityRatio > BigDecimal("1.0") -> stabilityScore += 15
            liquidityRatio > BigDecimal("0.5") -> stabilityScore += 10
            liquidityRatio > BigDecimal("0.25") -> stabilityScore += 5
            else -> stabilityScore -= 5
        }
        
        // Credit score assessment
        when {
            creditScore >= 800 -> stabilityScore += 15
            creditScore >= 740 -> stabilityScore += 10
            creditScore >= 670 -> stabilityScore += 5
            creditScore >= 580 -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        return when {
            stabilityScore >= 30 -> FinancialStability.EXCELLENT
            stabilityScore >= 15 -> FinancialStability.GOOD
            stabilityScore >= 0 -> FinancialStability.FAIR
            else -> FinancialStability.POOR
        }
    }

    private fun assessOccupationRisk(occupation: String): BigDecimal {
        return when (occupation.uppercase()) {
            "TEACHER", "ACCOUNTANT", "ENGINEER", "LAWYER" -> BigDecimal("1.00")
            "POLICE_OFFICER", "FIREFIGHTER" -> BigDecimal("1.25")
            "PILOT", "CONSTRUCTION_WORKER" -> BigDecimal("1.50")
            "MINER", "LOGGER", "COMMERCIAL_FISHERMAN" -> BigDecimal("2.00")
            else -> BigDecimal("1.00")
        }
    }

    private fun assessLifestyleRisk(lifestyle: LifestyleInfo): BigDecimal {
        var factor = BigDecimal("1.00")
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            factor = factor.multiply(BigDecimal("1.15"))
        }
        
        if (lifestyle.hazardousActivities.isNotEmpty()) {
            factor = factor.multiply(BigDecimal("1.10"))
        }
        
        if (lifestyle.exerciseFrequency == "REGULAR") {
            factor = factor.multiply(BigDecimal("0.95"))
        }
        
        return factor
    }

    private fun assessGeographicRisk(state: String): BigDecimal {
        // Simplified geographic risk factors
        return when (state.uppercase()) {
            "CA", "FL", "TX" -> BigDecimal("1.05") // Higher cost states
            "NY", "NJ", "CT" -> BigDecimal("1.10") // High cost, high risk
            "WY", "MT", "ND" -> BigDecimal("0.95") // Lower risk states
            else -> BigDecimal("1.00")
        }
    }

    private fun determineOverallRiskClass(ratingFactors: Map<String, BigDecimal>): RiskClass {
        val overallFactor = ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
        
        return when {
            overallFactor < BigDecimal("0.90") -> RiskClass.PREFERRED
            overallFactor <= BigDecimal("1.10") -> RiskClass.STANDARD
            overallFactor <= BigDecimal("2.00") -> RiskClass.SUBSTANDARD
            else -> RiskClass.DECLINED
        }
    }

    private fun calculateOverallRiskScore(ratingFactors: Map<String, BigDecimal>): BigDecimal {
        return ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
    }

    private fun generateRiskNotes(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): List<String> {
        val notes = mutableListOf<String>()
        
        if (medicalAssessment.riskScore > 50) {
            notes.add("Elevated medical risk due to health conditions")
        }
        
        if (financialAssessment.debtToIncomeRatio > BigDecimal("0.40")) {
            notes.add("High debt-to-income ratio may affect financial stability")
        }
        
        if (financialAssessment.additionalDocumentationRequired) {
            notes.add("Additional financial documentation required for requested coverage amount")
        }
        
        return notes
    }

    private fun makeUnderwritingDecision(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment, riskAssessment: RiskAssessment): UnderwritingDecision {
        return when {
            medicalAssessment.riskClass == MedicalRiskClass.DECLINED -> UnderwritingDecision.DECLINE
            financialAssessment.financialJustification == FinancialJustification.INSUFFICIENT_JUSTIFICATION -> UnderwritingDecision.DECLINE
            riskAssessment.riskClass == RiskClass.DECLINED -> UnderwritingDecision.DECLINE
            medicalAssessment.riskClass == MedicalRiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            riskAssessment.riskClass == RiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            medicalAssessment.medicalExamRequired -> UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS
            else -> UnderwritingDecision.APPROVE_AS_APPLIED
        }
    }

    private fun determineConditions(decision: UnderwritingDecision, riskAssessment: RiskAssessment): List<String> {
        val conditions = mutableListOf<String>()
        
        when (decision) {
            UnderwritingDecision.APPROVE_WITH_RATING -> {
                conditions.add("Policy issued with ${riskAssessment.overallRiskScore}x rating")
            }
            UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS -> {
                conditions.add("Medical exam required before final decision")
            }
            else -> { /* No additional conditions */ }
        }
        
        return conditions
    }

    private fun getCurrentUnderwriterId(): String = "UW-${System.currentTimeMillis()}"
}

// Data classes and enums for underwriting
data class InsuranceApplication(
    val applicationId: String,
    val applicantInfo: ApplicantInfo,
    val medicalInfo: MedicalInfo,
    val financialInfo: FinancialInfo,
    val requestedCoverage: BigDecimal,
    val productType: String
)

data class ApplicantInfo(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val occupation: String,
    val residenceState: String
)

data class MedicalInfo(
    val age: Int,
    val height: Int, // inches
    val weight: Int, // pounds
    val isSmoker: Boolean,
    val medicalHistory: List<MedicalCondition>,
    val familyHistory: List<FamilyMedicalHistory>,
    val lifestyle: LifestyleInfo
)

data class MedicalCondition(
    val type: String,
    val diagnosisDate: LocalDate,
    val severity: String,
    val treatment: String,
    val yearsInRemission: Int = 0
)

data class FamilyMedicalHistory(
    val relationship: String,
    val condition: String,
    val ageAtDiagnosis: Int
)

data class LifestyleInfo(
    val alcoholConsumption: String, // NONE, LIGHT, MODERATE, HEAVY
    val exerciseFrequency: String, // NEVER, OCCASIONAL, REGULAR, FREQUENT
    val hazardousActivities: List<String>
)

data class FinancialInfo(
    val annualIncome: BigDecimal,
    val netWorth: BigDecimal,
    val liquidAssets: BigDecimal,
    val totalDebt: BigDecimal,
    val monthlyExpenses: BigDecimal,
    val creditScore: Int
)

data class UnderwritingResult(
    val applicationId: String,
    val decision: UnderwritingDecision,
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val medicalAssessment: MedicalAssessment,
    val financialAssessment: FinancialAssessment,
    val recommendedPremium: BigDecimal,
    val conditions: List<String>,
    val underwriterId: String,
    val underwritingDate: LocalDateTime
)

data class MedicalAssessment(
    val riskScore: Int,
    val riskClass: MedicalRiskClass,
    val riskFactors: List<MedicalRiskFactor>,
    val medicalExamRequired: Boolean,
    val additionalRequirements: List<String>
)

data class FinancialAssessment(
    val maxRecommendedCoverage: BigDecimal,
    val financialJustification: FinancialJustification,
    val debtToIncomeRatio: BigDecimal,
    val liquidityRatio: BigDecimal,
    val financialStability: FinancialStability,
    val additionalDocumentationRequired: Boolean
)

data class RiskAssessment(
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val overallRiskScore: BigDecimal,
    val riskNotes: List<String>
)

data class MedicalRiskFactor(
    val category: String,
    val description: String,
    val score: Int
)

data class RiskFactor(
    val description: String,
    val score: Int
)

enum class Gender { MALE, FEMALE, OTHER }

enum class UnderwritingDecision {
    APPROVE_AS_APPLIED, APPROVE_WITH_RATING, DECLINE, POSTPONE_PENDING_REQUIREMENTS
}

enum class MedicalRiskClass {
    SUPER_PREFERRED, PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class FinancialJustification {
    INCOME_REPLACEMENT, ESTATE_PLANNING, BUSINESS_PROTECTION, INSUFFICIENT_JUSTIFICATION
}

enum class FinancialStability {
    EXCELLENT, GOOD, FAIR, POOR
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow

/**
 * Underwriting engine for risk assessment and policy approval
 * Handles medical underwriting, financial underwriting, and risk classification
 */
@Service
class UnderwritingEngine {

    /**
     * Performs comprehensive underwriting assessment
     */
    fun performUnderwriting(application: InsuranceApplication): UnderwritingResult {
        val medicalAssessment = performMedicalUnderwriting(application.medicalInfo)
        val financialAssessment = performFinancialUnderwriting(application.financialInfo, application.requestedCoverage)
        val riskAssessment = assessOverallRisk(application, medicalAssessment, financialAssessment)
        
        val decision = makeUnderwritingDecision(medicalAssessment, financialAssessment, riskAssessment)
        
        return UnderwritingResult(
            applicationId = application.applicationId,
            decision = decision,
            riskClass = riskAssessment.riskClass,
            ratingFactors = riskAssessment.ratingFactors,
            medicalAssessment = medicalAssessment,
            financialAssessment = financialAssessment,
            recommendedPremium = calculatePremium(application.requestedCoverage, riskAssessment),
            conditions = determineConditions(decision, riskAssessment),
            underwriterId = getCurrentUnderwriterId(),
            underwritingDate = LocalDateTime.now()
        )
    }

    /**
     * Performs medical underwriting assessment
     */
    fun performMedicalUnderwriting(medicalInfo: MedicalInfo): MedicalAssessment {
        val riskFactors = mutableListOf<MedicalRiskFactor>()
        var riskScore = 0
        
        // Age factor
        val ageFactor = calculateAgeFactor(medicalInfo.age)
        riskScore += ageFactor.score
        riskFactors.add(MedicalRiskFactor("AGE", ageFactor.description, ageFactor.score))
        
        // Smoking status
        if (medicalInfo.isSmoker) {
            riskScore += 50
            riskFactors.add(MedicalRiskFactor("SMOKING", "Current smoker", 50))
        }
        
        // BMI assessment
        val bmiAssessment = assessBMI(medicalInfo.height, medicalInfo.weight)
        riskScore += bmiAssessment.score
        riskFactors.add(MedicalRiskFactor("BMI", bmiAssessment.description, bmiAssessment.score))
        
        // Medical history
        medicalInfo.medicalHistory.forEach { condition ->
            val conditionRisk = assessMedicalCondition(condition)
            riskScore += conditionRisk.score
            riskFactors.add(MedicalRiskFactor("MEDICAL_HISTORY", conditionRisk.description, conditionRisk.score))
        }
        
        // Family history
        medicalInfo.familyHistory.forEach { condition ->
            val familyRisk = assessFamilyHistory(condition)
            riskScore += familyRisk.score
            riskFactors.add(MedicalRiskFactor("FAMILY_HISTORY", familyRisk.description, familyRisk.score))
        }
        
        // Lifestyle factors
        val lifestyleRisk = assessLifestyle(medicalInfo.lifestyle)
        riskScore += lifestyleRisk.score
        riskFactors.add(MedicalRiskFactor("LIFESTYLE", lifestyleRisk.description, lifestyleRisk.score))
        
        val medicalExamRequired = determineMedicalExamRequirement(medicalInfo, riskScore)
        val riskClass = determineMedicalRiskClass(riskScore)
        
        return MedicalAssessment(
            riskScore = riskScore,
            riskClass = riskClass,
            riskFactors = riskFactors,
            medicalExamRequired = medicalExamRequired,
            additionalRequirements = determineAdditionalMedicalRequirements(riskScore, medicalInfo)
        )
    }

    /**
     * Performs financial underwriting assessment
     */
    fun performFinancialUnderwriting(financialInfo: FinancialInfo, requestedCoverage: BigDecimal): FinancialAssessment {
        val incomeMultiplier = calculateIncomeMultiplier(financialInfo.annualIncome)
        val maxCoverageByIncome = financialInfo.annualIncome.multiply(incomeMultiplier)
        
        val netWorthMultiplier = BigDecimal("0.25") // 25% of net worth
        val maxCoverageByNetWorth = financialInfo.netWorth.multiply(netWorthMultiplier)
        
        val maxRecommendedCoverage = maxOf(maxCoverageByIncome, maxCoverageByNetWorth)
        
        val financialJustification = when {
            requestedCoverage <= maxCoverageByIncome -> FinancialJustification.INCOME_REPLACEMENT
            requestedCoverage <= maxCoverageByNetWorth -> FinancialJustification.ESTATE_PLANNING
            requestedCoverage <= maxRecommendedCoverage -> FinancialJustification.BUSINESS_PROTECTION
            else -> FinancialJustification.INSUFFICIENT_JUSTIFICATION
        }
        
        val debtToIncomeRatio = financialInfo.totalDebt.divide(financialInfo.annualIncome, 4, java.math.RoundingMode.HALF_UP)
        val liquidityRatio = financialInfo.liquidAssets.divide(financialInfo.monthlyExpenses.multiply(BigDecimal("12")), 4, java.math.RoundingMode.HALF_UP)
        
        val financialStability = assessFinancialStability(debtToIncomeRatio, liquidityRatio, financialInfo.creditScore)
        
        return FinancialAssessment(
            maxRecommendedCoverage = maxRecommendedCoverage,
            financialJustification = financialJustification,
            debtToIncomeRatio = debtToIncomeRatio,
            liquidityRatio = liquidityRatio,
            financialStability = financialStability,
            additionalDocumentationRequired = requestedCoverage > maxRecommendedCoverage
        )
    }

    /**
     * Assesses overall risk combining medical and financial factors
     */
    fun assessOverallRisk(application: InsuranceApplication, medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): RiskAssessment {
        val ratingFactors = mutableMapOf<String, BigDecimal>()
        
        // Medical rating factors
        when (medicalAssessment.riskClass) {
            MedicalRiskClass.SUPER_PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.85")
            MedicalRiskClass.PREFERRED -> ratingFactors["MEDICAL"] = BigDecimal("0.95")
            MedicalRiskClass.STANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.00")
            MedicalRiskClass.SUBSTANDARD -> ratingFactors["MEDICAL"] = BigDecimal("1.25")
            MedicalRiskClass.DECLINED -> ratingFactors["MEDICAL"] = BigDecimal("999.99") // Effectively declined
        }
        
        // Financial rating factors
        when (financialAssessment.financialJustification) {
            FinancialJustification.INCOME_REPLACEMENT -> ratingFactors["FINANCIAL"] = BigDecimal("1.00")
            FinancialJustification.ESTATE_PLANNING -> ratingFactors["FINANCIAL"] = BigDecimal("1.05")
            FinancialJustification.BUSINESS_PROTECTION -> ratingFactors["FINANCIAL"] = BigDecimal("1.10")
            FinancialJustification.INSUFFICIENT_JUSTIFICATION -> ratingFactors["FINANCIAL"] = BigDecimal("999.99")
        }
        
        // Occupation rating
        val occupationFactor = assessOccupationRisk(application.applicantInfo.occupation)
        ratingFactors["OCCUPATION"] = occupationFactor
        
        // Lifestyle rating
        val lifestyleFactor = assessLifestyleRisk(application.medicalInfo.lifestyle)
        ratingFactors["LIFESTYLE"] = lifestyleFactor
        
        // Geographic rating
        val geographicFactor = assessGeographicRisk(application.applicantInfo.residenceState)
        ratingFactors["GEOGRAPHIC"] = geographicFactor
        
        val overallRiskClass = determineOverallRiskClass(ratingFactors)
        
        return RiskAssessment(
            riskClass = overallRiskClass,
            ratingFactors = ratingFactors,
            overallRiskScore = calculateOverallRiskScore(ratingFactors),
            riskNotes = generateRiskNotes(medicalAssessment, financialAssessment)
        )
    }

    /**
     * Calculates premium based on coverage amount and risk assessment
     */
    fun calculatePremium(coverageAmount: BigDecimal, riskAssessment: RiskAssessment): BigDecimal {
        val basePremiumRate = BigDecimal("0.001") // $1 per $1000 of coverage
        var premium = coverageAmount.multiply(basePremiumRate)
        
        // Apply rating factors
        riskAssessment.ratingFactors.values.forEach { factor ->
            premium = premium.multiply(factor)
        }
        
        // Apply minimum premium
        val minimumPremium = BigDecimal("100")
        if (premium < minimumPremium) {
            premium = minimumPremium
        }
        
        return premium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates mortality table lookup for actuarial calculations
     */
    fun lookupMortalityRate(age: Int, gender: Gender, riskClass: RiskClass): BigDecimal {
        // Simplified mortality table - in reality this would be much more complex
        val baseMortalityRate = when {
            age < 30 -> BigDecimal("0.0008")
            age < 40 -> BigDecimal("0.0012")
            age < 50 -> BigDecimal("0.0020")
            age < 60 -> BigDecimal("0.0035")
            age < 70 -> BigDecimal("0.0065")
            else -> BigDecimal("0.0120")
        }
        
        // Gender adjustment
        val genderFactor = if (gender == Gender.FEMALE) BigDecimal("0.85") else BigDecimal("1.00")
        
        // Risk class adjustment
        val riskFactor = when (riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.80")
            RiskClass.STANDARD -> BigDecimal("1.00")
            RiskClass.SUBSTANDARD -> BigDecimal("1.50")
            RiskClass.DECLINED -> BigDecimal("999.99")
        }
        
        return baseMortalityRate.multiply(genderFactor).multiply(riskFactor)
    }

    // Private helper methods
    private fun calculateAgeFactor(age: Int): RiskFactor {
        return when {
            age < 25 -> RiskFactor("Young adult", 5)
            age < 35 -> RiskFactor("Young adult - preferred", 0)
            age < 45 -> RiskFactor("Middle age - standard", 10)
            age < 55 -> RiskFactor("Middle age - increased risk", 20)
            age < 65 -> RiskFactor("Senior - higher risk", 35)
            else -> RiskFactor("Senior - high risk", 50)
        }
    }

    private fun assessBMI(heightInches: Int, weightPounds: Int): RiskFactor {
        val heightMeters = heightInches * 0.0254
        val weightKg = weightPounds * 0.453592
        val bmi = weightKg / (heightMeters.pow(2))
        
        return when {
            bmi < 18.5 -> RiskFactor("Underweight", 15)
            bmi < 25.0 -> RiskFactor("Normal weight", 0)
            bmi < 30.0 -> RiskFactor("Overweight", 10)
            bmi < 35.0 -> RiskFactor("Obese Class I", 25)
            bmi < 40.0 -> RiskFactor("Obese Class II", 40)
            else -> RiskFactor("Obese Class III", 60)
        }
    }

    private fun assessMedicalCondition(condition: MedicalCondition): RiskFactor {
        return when (condition.type) {
            "DIABETES" -> when (condition.severity) {
                "CONTROLLED" -> RiskFactor("Controlled diabetes", 30)
                "UNCONTROLLED" -> RiskFactor("Uncontrolled diabetes", 80)
                else -> RiskFactor("Diabetes", 50)
            }
            "HYPERTENSION" -> when (condition.severity) {
                "MILD" -> RiskFactor("Mild hypertension", 15)
                "MODERATE" -> RiskFactor("Moderate hypertension", 30)
                "SEVERE" -> RiskFactor("Severe hypertension", 60)
                else -> RiskFactor("Hypertension", 25)
            }
            "HEART_DISEASE" -> RiskFactor("Heart disease", 100)
            "CANCER" -> when (condition.yearsInRemission) {
                in 0..2 -> RiskFactor("Recent cancer history", 150)
                in 3..5 -> RiskFactor("Cancer history", 75)
                else -> RiskFactor("Remote cancer history", 25)
            }
            else -> RiskFactor("Other medical condition", 20)
        }
    }

    private fun assessFamilyHistory(condition: FamilyMedicalHistory): RiskFactor {
        val baseScore = when (condition.condition) {
            "HEART_DISEASE" -> 15
            "CANCER" -> 10
            "DIABETES" -> 8
            "STROKE" -> 12
            else -> 5
        }
        
        val relationshipMultiplier = when (condition.relationship) {
            "PARENT" -> 1.0
            "SIBLING" -> 0.8
            "GRANDPARENT" -> 0.5
            else -> 0.3
        }
        
        val ageMultiplier = if (condition.ageAtDiagnosis < 60) 1.5 else 1.0
        
        val adjustedScore = (baseScore * relationshipMultiplier * ageMultiplier).toInt()
        
        return RiskFactor("Family history of ${condition.condition}", adjustedScore)
    }

    private fun assessLifestyle(lifestyle: LifestyleInfo): RiskFactor {
        var score = 0
        val factors = mutableListOf<String>()
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            score += 25
            factors.add("heavy drinking")
        } else if (lifestyle.alcoholConsumption == "MODERATE") {
            score += 5
            factors.add("moderate drinking")
        }
        
        if (lifestyle.exerciseFrequency == "NEVER") {
            score += 15
            factors.add("sedentary lifestyle")
        } else if (lifestyle.exerciseFrequency == "REGULAR") {
            score -= 5
            factors.add("regular exercise")
        }
        
        lifestyle.hazardousActivities.forEach { activity ->
            when (activity) {
                "SKYDIVING" -> { score += 30; factors.add("skydiving") }
                "ROCK_CLIMBING" -> { score += 20; factors.add("rock climbing") }
                "MOTORCYCLE_RACING" -> { score += 40; factors.add("motorcycle racing") }
                "SCUBA_DIVING" -> { score += 15; factors.add("scuba diving") }
            }
        }
        
        val description = if (factors.isEmpty()) "Standard lifestyle" else "Lifestyle factors: ${factors.joinToString(", ")}"
        
        return RiskFactor(description, maxOf(0, score))
    }

    private fun determineMedicalExamRequirement(medicalInfo: MedicalInfo, riskScore: Int): Boolean {
        return riskScore > 50 || medicalInfo.age > 50 || medicalInfo.medicalHistory.isNotEmpty()
    }

    private fun determineAdditionalMedicalRequirements(riskScore: Int, medicalInfo: MedicalInfo): List<String> {
        val requirements = mutableListOf<String>()
        
        if (riskScore > 75) {
            requirements.add("Physician's statement")
            requirements.add("Medical records")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "HEART_DISEASE" }) {
            requirements.add("Cardiac stress test")
            requirements.add("EKG")
        }
        
        if (medicalInfo.medicalHistory.any { it.type == "DIABETES" }) {
            requirements.add("HbA1c test")
            requirements.add("Diabetic panel")
        }
        
        if (medicalInfo.age > 65) {
            requirements.add("Cognitive assessment")
        }
        
        return requirements
    }

    private fun determineMedicalRiskClass(riskScore: Int): MedicalRiskClass {
        return when {
            riskScore <= 10 -> MedicalRiskClass.SUPER_PREFERRED
            riskScore <= 25 -> MedicalRiskClass.PREFERRED
            riskScore <= 50 -> MedicalRiskClass.STANDARD
            riskScore <= 100 -> MedicalRiskClass.SUBSTANDARD
            else -> MedicalRiskClass.DECLINED
        }
    }

    private fun calculateIncomeMultiplier(annualIncome: BigDecimal): BigDecimal {
        return when {
            annualIncome < BigDecimal("50000") -> BigDecimal("10")
            annualIncome < BigDecimal("100000") -> BigDecimal("15")
            annualIncome < BigDecimal("250000") -> BigDecimal("20")
            annualIncome < BigDecimal("500000") -> BigDecimal("25")
            else -> BigDecimal("30")
        }
    }

    private fun assessFinancialStability(debtToIncomeRatio: BigDecimal, liquidityRatio: BigDecimal, creditScore: Int): FinancialStability {
        var stabilityScore = 0
        
        // Debt-to-income assessment
        when {
            debtToIncomeRatio < BigDecimal("0.20") -> stabilityScore += 20
            debtToIncomeRatio < BigDecimal("0.36") -> stabilityScore += 10
            debtToIncomeRatio < BigDecimal("0.50") -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        // Liquidity assessment
        when {
            liquidityRatio > BigDecimal("1.0") -> stabilityScore += 15
            liquidityRatio > BigDecimal("0.5") -> stabilityScore += 10
            liquidityRatio > BigDecimal("0.25") -> stabilityScore += 5
            else -> stabilityScore -= 5
        }
        
        // Credit score assessment
        when {
            creditScore >= 800 -> stabilityScore += 15
            creditScore >= 740 -> stabilityScore += 10
            creditScore >= 670 -> stabilityScore += 5
            creditScore >= 580 -> stabilityScore += 0
            else -> stabilityScore -= 10
        }
        
        return when {
            stabilityScore >= 30 -> FinancialStability.EXCELLENT
            stabilityScore >= 15 -> FinancialStability.GOOD
            stabilityScore >= 0 -> FinancialStability.FAIR
            else -> FinancialStability.POOR
        }
    }

    private fun assessOccupationRisk(occupation: String): BigDecimal {
        return when (occupation.uppercase()) {
            "TEACHER", "ACCOUNTANT", "ENGINEER", "LAWYER" -> BigDecimal("1.00")
            "POLICE_OFFICER", "FIREFIGHTER" -> BigDecimal("1.25")
            "PILOT", "CONSTRUCTION_WORKER" -> BigDecimal("1.50")
            "MINER", "LOGGER", "COMMERCIAL_FISHERMAN" -> BigDecimal("2.00")
            else -> BigDecimal("1.00")
        }
    }

    private fun assessLifestyleRisk(lifestyle: LifestyleInfo): BigDecimal {
        var factor = BigDecimal("1.00")
        
        if (lifestyle.alcoholConsumption == "HEAVY") {
            factor = factor.multiply(BigDecimal("1.15"))
        }
        
        if (lifestyle.hazardousActivities.isNotEmpty()) {
            factor = factor.multiply(BigDecimal("1.10"))
        }
        
        if (lifestyle.exerciseFrequency == "REGULAR") {
            factor = factor.multiply(BigDecimal("0.95"))
        }
        
        return factor
    }

    private fun assessGeographicRisk(state: String): BigDecimal {
        // Simplified geographic risk factors
        return when (state.uppercase()) {
            "CA", "FL", "TX" -> BigDecimal("1.05") // Higher cost states
            "NY", "NJ", "CT" -> BigDecimal("1.10") // High cost, high risk
            "WY", "MT", "ND" -> BigDecimal("0.95") // Lower risk states
            else -> BigDecimal("1.00")
        }
    }

    private fun determineOverallRiskClass(ratingFactors: Map<String, BigDecimal>): RiskClass {
        val overallFactor = ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
        
        return when {
            overallFactor < BigDecimal("0.90") -> RiskClass.PREFERRED
            overallFactor <= BigDecimal("1.10") -> RiskClass.STANDARD
            overallFactor <= BigDecimal("2.00") -> RiskClass.SUBSTANDARD
            else -> RiskClass.DECLINED
        }
    }

    private fun calculateOverallRiskScore(ratingFactors: Map<String, BigDecimal>): BigDecimal {
        return ratingFactors.values.fold(BigDecimal.ONE) { acc, factor -> acc.multiply(factor) }
    }

    private fun generateRiskNotes(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment): List<String> {
        val notes = mutableListOf<String>()
        
        if (medicalAssessment.riskScore > 50) {
            notes.add("Elevated medical risk due to health conditions")
        }
        
        if (financialAssessment.debtToIncomeRatio > BigDecimal("0.40")) {
            notes.add("High debt-to-income ratio may affect financial stability")
        }
        
        if (financialAssessment.additionalDocumentationRequired) {
            notes.add("Additional financial documentation required for requested coverage amount")
        }
        
        return notes
    }

    private fun makeUnderwritingDecision(medicalAssessment: MedicalAssessment, financialAssessment: FinancialAssessment, riskAssessment: RiskAssessment): UnderwritingDecision {
        return when {
            medicalAssessment.riskClass == MedicalRiskClass.DECLINED -> UnderwritingDecision.DECLINE
            financialAssessment.financialJustification == FinancialJustification.INSUFFICIENT_JUSTIFICATION -> UnderwritingDecision.DECLINE
            riskAssessment.riskClass == RiskClass.DECLINED -> UnderwritingDecision.DECLINE
            medicalAssessment.riskClass == MedicalRiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            riskAssessment.riskClass == RiskClass.SUBSTANDARD -> UnderwritingDecision.APPROVE_WITH_RATING
            medicalAssessment.medicalExamRequired -> UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS
            else -> UnderwritingDecision.APPROVE_AS_APPLIED
        }
    }

    private fun determineConditions(decision: UnderwritingDecision, riskAssessment: RiskAssessment): List<String> {
        val conditions = mutableListOf<String>()
        
        when (decision) {
            UnderwritingDecision.APPROVE_WITH_RATING -> {
                conditions.add("Policy issued with ${riskAssessment.overallRiskScore}x rating")
            }
            UnderwritingDecision.POSTPONE_PENDING_REQUIREMENTS -> {
                conditions.add("Medical exam required before final decision")
            }
            else -> { /* No additional conditions */ }
        }
        
        return conditions
    }

    private fun getCurrentUnderwriterId(): String = "UW-${System.currentTimeMillis()}"
}

// Data classes and enums for underwriting
data class InsuranceApplication(
    val applicationId: String,
    val applicantInfo: ApplicantInfo,
    val medicalInfo: MedicalInfo,
    val financialInfo: FinancialInfo,
    val requestedCoverage: BigDecimal,
    val productType: String
)

data class ApplicantInfo(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: Gender,
    val occupation: String,
    val residenceState: String
)

data class MedicalInfo(
    val age: Int,
    val height: Int, // inches
    val weight: Int, // pounds
    val isSmoker: Boolean,
    val medicalHistory: List<MedicalCondition>,
    val familyHistory: List<FamilyMedicalHistory>,
    val lifestyle: LifestyleInfo
)

data class MedicalCondition(
    val type: String,
    val diagnosisDate: LocalDate,
    val severity: String,
    val treatment: String,
    val yearsInRemission: Int = 0
)

data class FamilyMedicalHistory(
    val relationship: String,
    val condition: String,
    val ageAtDiagnosis: Int
)

data class LifestyleInfo(
    val alcoholConsumption: String, // NONE, LIGHT, MODERATE, HEAVY
    val exerciseFrequency: String, // NEVER, OCCASIONAL, REGULAR, FREQUENT
    val hazardousActivities: List<String>
)

data class FinancialInfo(
    val annualIncome: BigDecimal,
    val netWorth: BigDecimal,
    val liquidAssets: BigDecimal,
    val totalDebt: BigDecimal,
    val monthlyExpenses: BigDecimal,
    val creditScore: Int
)

data class UnderwritingResult(
    val applicationId: String,
    val decision: UnderwritingDecision,
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val medicalAssessment: MedicalAssessment,
    val financialAssessment: FinancialAssessment,
    val recommendedPremium: BigDecimal,
    val conditions: List<String>,
    val underwriterId: String,
    val underwritingDate: LocalDateTime
)

data class MedicalAssessment(
    val riskScore: Int,
    val riskClass: MedicalRiskClass,
    val riskFactors: List<MedicalRiskFactor>,
    val medicalExamRequired: Boolean,
    val additionalRequirements: List<String>
)

data class FinancialAssessment(
    val maxRecommendedCoverage: BigDecimal,
    val financialJustification: FinancialJustification,
    val debtToIncomeRatio: BigDecimal,
    val liquidityRatio: BigDecimal,
    val financialStability: FinancialStability,
    val additionalDocumentationRequired: Boolean
)

data class RiskAssessment(
    val riskClass: RiskClass,
    val ratingFactors: Map<String, BigDecimal>,
    val overallRiskScore: BigDecimal,
    val riskNotes: List<String>
)

data class MedicalRiskFactor(
    val category: String,
    val description: String,
    val score: Int
)

data class RiskFactor(
    val description: String,
    val score: Int
)

enum class Gender { MALE, FEMALE, OTHER }

enum class UnderwritingDecision {
    APPROVE_AS_APPLIED, APPROVE_WITH_RATING, DECLINE, POSTPONE_PENDING_REQUIREMENTS
}

enum class MedicalRiskClass {
    SUPER_PREFERRED, PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class FinancialJustification {
    INCOME_REPLACEMENT, ESTATE_PLANNING, BUSINESS_PROTECTION, INSUFFICIENT_JUSTIFICATION
}

enum class FinancialStability {
    EXCELLENT, GOOD, FAIR, POOR
}
