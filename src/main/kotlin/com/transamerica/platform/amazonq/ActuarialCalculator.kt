private val mortalityRates = mapOf(
        20 to BigDecimal("0.0008"),
        30 to BigDecimal("0.0010"),
        40 to BigDecimal("0.0015"),
        50 to BigDecimal("0.0025"),
        60 to BigDecimal("0.0045"),
        70 to BigDecimal("0.0085"),
        80 to BigDecimal("0.0165"),
        90 to BigDecimal("0.0325")
    )

    override fun getMortalityRate(age: Int): BigDecimal {
        return mortalityRates.entries
            .minByOrNull { abs(it.key - age) }
            ?.value ?: BigDecimal("0.0500")
    }

    override fun getSurvivalProbability(age: Int): BigDecimal {
        return BigDecimal.ONE.subtract(getMortalityRate(age))
    }
}

data class GuidelinePremiumResult(
    val guidelineSinglePremium: BigDecimal,
    val guidelineLevelPremium: BigDecimal
)

enum class PaymentFrequency {
    ANNUAL, SEMI_ANNUAL, QUARTERLY, MONTHLY
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

/**
 * Actuarial calculator for insurance mathematics
 * Handles present value calculations, reserves, and statistical analysis
 */
@Service
class ActuarialCalculator {

    private val mathContext = MathContext(10, RoundingMode.HALF_UP)

    /**
     * Calculates net single premium for whole life insurance
     */
    fun calculateNetSinglePremium(age: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var presentValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until (120 - age)) {
            val currentAge = age + year
            val mortalityRate = mortalityTable.getMortalityRate(currentAge)
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val yearlyBenefit = faceAmount.multiply(mortalityRate).multiply(survivalProbability)
            val discountedBenefit = yearlyBenefit.divide(discountFactor.pow(year + 1), mathContext)

            presentValue = presentValue.add(discountedBenefit)
        }

        return presentValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates level annual premium for term life insurance
     */
    fun calculateLevelAnnualPremium(age: Int, term: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable, loadingFactor: BigDecimal): BigDecimal {
        val netSinglePremium = calculateTermNetSinglePremium(age, term, faceAmount, interestRate, mortalityTable)
        val annuityDue = calculateAnnuityDue(age, term, interestRate, mortalityTable)

        val netLevelPremium = netSinglePremium.divide(annuityDue, mathContext)
        val grossPremium = netLevelPremium.multiply(BigDecimal.ONE.add(loadingFactor))

        return grossPremium.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates cash value for whole life policy using prospective method
     */
    fun calculateCashValue(age: Int, policyYear: Int, faceAmount: BigDecimal, annualPremium: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        val currentAge = age + policyYear

        // Future benefits
        val futureBenefits = calculateNetSinglePremium(currentAge, faceAmount, interestRate, mortalityTable)

        // Future premiums
        val futurePremiums = annualPremium.multiply(calculateAnnuityDue(currentAge, 120 - currentAge, interestRate, mortalityTable))

        val cashValue = futureBenefits.subtract(futurePremiums)

        return maxOf(BigDecimal.ZERO, cashValue).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates policy reserves using retrospective method
     */
    fun calculatePolicyReserve(age: Int, policyYear: Int, faceAmount: BigDecimal, annualPremium: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var accumulatedValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until policyYear) {
            val yearAge = age + year
            val survivalProbability = mortalityTable.getSurvivalProbability(yearAge)

            // Add premium at beginning of year
            accumulatedValue = accumulatedValue.add(annualPremium)

            // Subtract cost of insurance
            val mortalityRate = mortalityTable.getMortalityRate(yearAge)
            val costOfInsurance = faceAmount.subtract(accumulatedValue).multiply(mortalityRate)
            accumulatedValue = accumulatedValue.subtract(costOfInsurance)

            // Apply interest
            accumulatedValue = accumulatedValue.multiply(discountFactor)
        }

        return accumulatedValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates annuity present value
     */
    fun calculateAnnuityPresentValue(age: Int, term: Int, annualPayment: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var presentValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 1..term) {
            val currentAge = age + year - 1
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val discountedPayment = annualPayment.multiply(survivalProbability)
                .divide(discountFactor.pow(year), mathContext)

            presentValue = presentValue.add(discountedPayment)
        }

        return presentValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates life expectancy using mortality table
     */
    fun calculateLifeExpectancy(age: Int, mortalityTable: MortalityTable): BigDecimal {
        var lifeExpectancy = BigDecimal.ZERO
        var survivalProbability = BigDecimal.ONE

        for (currentAge in age until 120) {
            val mortalityRate = mortalityTable.getMortalityRate(currentAge)
            val yearSurvival = BigDecimal.ONE.subtract(mortalityRate)

            lifeExpectancy = lifeExpectancy.add(survivalProbability)
            survivalProbability = survivalProbability.multiply(yearSurvival)

            if (survivalProbability < BigDecimal("0.001")) break
        }

        return lifeExpectancy.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates probability of survival from one age to another
     */
    fun calculateSurvivalProbability(fromAge: Int, toAge: Int, mortalityTable: MortalityTable): BigDecimal {
        if (toAge <= fromAge) return BigDecimal.ONE

        var survivalProbability = BigDecimal.ONE

        for (age in fromAge until toAge) {
            val mortalityRate = mortalityTable.getMortalityRate(age)
            val yearSurvival = BigDecimal.ONE.subtract(mortalityRate)
            survivalProbability = survivalProbability.multiply(yearSurvival)
        }

        return survivalProbability.setScale(6, RoundingMode.HALF_UP)
    }

    /**
     * Calculates disability income insurance premium
     */
    fun calculateDisabilityPremium(age: Int, monthlyBenefit: BigDecimal, benefitPeriod: Int, eliminationPeriod: Int, disabilityTable: DisabilityTable): BigDecimal {
        val disabilityRate = disabilityTable.getDisabilityRate(age)
        val averageClaimDuration = disabilityTable.getAverageClaimDuration(age, benefitPeriod)

        val expectedClaim = monthlyBenefit.multiply(BigDecimal(12))
            .multiply(averageClaimDuration)
            .multiply(disabilityRate)

        // Apply elimination period discount
        val eliminationDiscount = when (eliminationPeriod) {
            30 -> BigDecimal("1.00")
            60 -> BigDecimal("0.85")
            90 -> BigDecimal("0.75")
            180 -> BigDecimal("0.60")
            365 -> BigDecimal("0.45")
            else -> BigDecimal("1.00")
        }

        val adjustedClaim = expectedClaim.multiply(eliminationDiscount)
        val loadedPremium = adjustedClaim.multiply(BigDecimal("1.35")) // 35% loading

        return loadedPremium.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP) // Monthly premium
    }

    /**
     * Calculates long-term care insurance premium
     */
    fun calculateLongTermCarePremium(age: Int, dailyBenefit: BigDecimal, benefitPeriod: Int, inflationProtection: Boolean, ltcTable: LongTermCareTable): BigDecimal {
        val ltcRate = ltcTable.getLongTermCareRate(age)
        val averageStayDuration = ltcTable.getAverageStayDuration(age)

        var expectedBenefit = dailyBenefit.multiply(BigDecimal(365))
            .multiply(averageStayDuration)
            .multiply(ltcRate)

        // Apply benefit period cap
        val maxBenefit = dailyBenefit.multiply(BigDecimal(365)).multiply(BigDecimal(benefitPeriod))
        expectedBenefit = minOf(expectedBenefit, maxBenefit)

        // Apply inflation protection
        if (inflationProtection) {
            val inflationFactor = BigDecimal("1.25") // 25% increase for inflation protection
            expectedBenefit = expectedBenefit.multiply(inflationFactor)
        }

        val loadedPremium = expectedBenefit.multiply(BigDecimal("1.40")) // 40% loading

        return loadedPremium.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP) // Monthly premium
    }

    /**
     * Calculates modified endowment contract (MEC) limits
     */
    fun calculateMECLimit(age: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        // 7-pay test calculation
        val netSinglePremium = calculateNetSinglePremium(age, faceAmount, interestRate, mortalityTable)
        val sevenPayPremium = netSinglePremium.divide(calculateAnnuityDue(age, 7, interestRate, mortalityTable), mathContext)

        return sevenPayPremium.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates guideline premium test limits
     */
    fun calculateGuidelinePremiumTest(age: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): GuidelinePremiumResult {
        // Guideline Single Premium (GSP)
        val gsp = calculateNetSinglePremium(age, faceAmount, interestRate, mortalityTable)
            .multiply(BigDecimal("1.25")) // 25% corridor

        // Guideline Level Premium (GLP)
        val glp = calculateLevelAnnualPremium(age, 120 - age, faceAmount, interestRate, mortalityTable, BigDecimal("0.25"))

        return GuidelinePremiumResult(gsp, glp)
    }

    /**
     * Calculates surrender charges for universal life policies
     */
    fun calculateSurrenderCharges(policyYear: Int, premiumsPaid: BigDecimal, surrenderChargeSchedule: List<BigDecimal>): BigDecimal {
        if (policyYear >= surrenderChargeSchedule.size) return BigDecimal.ZERO

        val chargeRate = surrenderChargeSchedule[policyYear]
        return premiumsPaid.multiply(chargeRate).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates cost of insurance charges for universal life
     */
    fun calculateCostOfInsurance(age: Int, netAmountAtRisk: BigDecimal, mortalityTable: MortalityTable, mortalityMultiplier: BigDecimal): BigDecimal {
        val mortalityRate = mortalityTable.getMortalityRate(age)
        val adjustedRate = mortalityRate.multiply(mortalityMultiplier)

        return netAmountAtRisk.multiply(adjustedRate).divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates variable life insurance values
     */
    fun calculateVariableLifeValue(initialPremium: BigDecimal, investmentReturn: BigDecimal, years: Int, expenseRatio: BigDecimal): BigDecimal {
        val netReturn = investmentReturn.subtract(expenseRatio)
        val growthFactor = BigDecimal.ONE.add(netReturn)

        return initialPremium.multiply(growthFactor.pow(years)).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates annuity accumulation value
     */
    fun calculateAnnuityAccumulation(initialDeposit: BigDecimal, interestRate: BigDecimal, years: Int, annualDeposit: BigDecimal): BigDecimal {
        val growthFactor = BigDecimal.ONE.add(interestRate)

        // Initial deposit growth
        var totalValue = initialDeposit.multiply(growthFactor.pow(years))

        // Annual deposits growth
        for (year in 1..years) {
            val depositGrowth = annualDeposit.multiply(growthFactor.pow(years - year))
            totalValue = totalValue.add(depositGrowth)
        }

        return totalValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates immediate annuity payment
     */
    fun calculateImmediateAnnuityPayment(purchasePrice: BigDecimal, age: Int, interestRate: BigDecimal, mortalityTable: MortalityTable, paymentFrequency: PaymentFrequency): BigDecimal {
        val paymentsPerYear = when (paymentFrequency) {
            PaymentFrequency.ANNUAL -> 1
            PaymentFrequency.SEMI_ANNUAL -> 2
            PaymentFrequency.QUARTERLY -> 4
            PaymentFrequency.MONTHLY -> 12
        }

        val periodicRate = interestRate.divide(BigDecimal(paymentsPerYear), 6, RoundingMode.HALF_UP)
        val annuityFactor = calculateAnnuityFactor(age, periodicRate, paymentsPerYear, mortalityTable)

        return purchasePrice.divide(annuityFactor, 2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates pension plan contributions
     */
    fun calculatePensionContribution(currentAge: Int, retirementAge: Int, desiredIncome: BigDecimal, currentBalance: BigDecimal, interestRate: BigDecimal): BigDecimal {
        val yearsToRetirement = retirementAge - currentAge
        val growthFactor = BigDecimal.ONE.add(interestRate)

        // Future value of current balance
        val futureCurrentBalance = currentBalance.multiply(growthFactor.pow(yearsToRetirement))

        // Required total at retirement (assuming 20 years of payments)
        val requiredTotal = desiredIncome.multiply(calculateAnnuityPresentValue(retirementAge, 20, BigDecimal.ONE, interestRate, StandardMortalityTable()))

        // Additional amount needed
        val additionalNeeded = requiredTotal.subtract(futureCurrentBalance)

        // Annual contribution needed
        val annuityFactor = (growthFactor.pow(yearsToRetirement).subtract(BigDecimal.ONE)).divide(interestRate, mathContext)

        return additionalNeeded.divide(annuityFactor, 2, RoundingMode.HALF_UP)
    }

    // Private helper methods
    private fun calculateTermNetSinglePremium(age: Int, term: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var presentValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until term) {
            val currentAge = age + year
            val mortalityRate = mortalityTable.getMortalityRate(currentAge)
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val yearlyBenefit = faceAmount.multiply(mortalityRate).multiply(survivalProbability)
            val discountedBenefit = yearlyBenefit.divide(discountFactor.pow(year + 1), mathContext)

            presentValue = presentValue.add(discountedBenefit)
        }

        return presentValue
    }

    private fun calculateAnnuityDue(age: Int, term: Int, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var annuityValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until term) {
            val currentAge = age + year
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val discountedPayment = survivalProbability.divide(discountFactor.pow(year), mathContext)
            annuityValue = annuityValue.add(discountedPayment)
        }

        return annuityValue
    }

    private fun calculateAnnuityFactor(age: Int, periodicRate: BigDecimal, paymentsPerYear: Int, mortalityTable: MortalityTable): BigDecimal {
        var factor = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(periodicRate)

        for (period in 1..1200) { // Up to 100 years of payments
            val years = period.toDouble() / paymentsPerYear
            val currentAge = age + years.toInt()

            if (currentAge >= 120) break

            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)
            val discountedPayment = survivalProbability.divide(discountFactor.pow(period), mathContext)

            factor = factor.add(discountedPayment)

            if (discountedPayment < BigDecimal("0.0001")) break
        }

        return factor
    }

    private fun BigDecimal.pow(n: Int): BigDecimal {
        return this.pow(n, mathContext)
    }

    private fun minOf(a: BigDecimal, b: BigDecimal): BigDecimal {
        return if (a <= b) a else b
    }

    private fun maxOf(a: BigDecimal, b: BigDecimal): BigDecimal {
        return if (a >= b) a else b
    }
}

// Supporting classes and interfaces
interface MortalityTable {
    fun getMortalityRate(age: Int): BigDecimal
    fun getSurvivalProbability(age: Int): BigDecimal
}

interface DisabilityTable {
    fun getDisabilityRate(age: Int): BigDecimal
    fun getAverageClaimDuration(age: Int, benefitPeriod: Int): BigDecimal
}

interface LongTermCareTable {
    fun getLongTermCareRate(age: Int): BigDecimal
    fun getAverageStayDuration(age: Int): BigDecimal
}

class StandardMortalityTable : MortalityTable {
    private val mortalityRates = mapOf(
        20 to BigDecimal("0.0008"),
        30 to BigDecimal("0.0010"),
        40 to BigDecimal("0.0015"),
        50 to BigDecimal("0.0025"),
        60 to BigDecimal("0.0045"),
        70 to BigDecimal("0.0085"),
        80 to BigDecimal("0.0165"),
        90 to BigDecimal("0.0325")
    )

    override fun getMortalityRate(age: Int): BigDecimal {
        return mortalityRates.entries
            .minByOrNull { abs(it.key - age) }
            ?.value ?: BigDecimal("0.0500")
    }

    override fun getSurvivalProbability(age: Int): BigDecimal {
        return BigDecimal.ONE.subtract(getMortalityRate(age))
    }
}

data class GuidelinePremiumResult(
    val guidelineSinglePremium: BigDecimal,
    val guidelineLevelPremium: BigDecimal
)

enum class PaymentFrequency {
    ANNUAL, SEMI_ANNUAL, QUARTERLY, MONTHLY
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

/**
 * Actuarial calculator for insurance mathematics
 * Handles present value calculations, reserves, and statistical analysis
 */
@Service
class ActuarialCalculator {

    private val mathContext = MathContext(10, RoundingMode.HALF_UP)

    /**
     * Calculates net single premium for whole life insurance
     */
    fun calculateNetSinglePremium(age: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var presentValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until (120 - age)) {
            val currentAge = age + year
            val mortalityRate = mortalityTable.getMortalityRate(currentAge)
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val yearlyBenefit = faceAmount.multiply(mortalityRate).multiply(survivalProbability)
            val discountedBenefit = yearlyBenefit.divide(discountFactor.pow(year + 1), mathContext)

            presentValue = presentValue.add(discountedBenefit)
        }

        return presentValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates level annual premium for term life insurance
     */
    fun calculateLevelAnnualPremium(age: Int, term: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable, loadingFactor: BigDecimal): BigDecimal {
        val netSinglePremium = calculateTermNetSinglePremium(age, term, faceAmount, interestRate, mortalityTable)
        val annuityDue = calculateAnnuityDue(age, term, interestRate, mortalityTable)

        val netLevelPremium = netSinglePremium.divide(annuityDue, mathContext)
        val grossPremium = netLevelPremium.multiply(BigDecimal.ONE.add(loadingFactor))

        return grossPremium.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates cash value for whole life policy using prospective method
     */
    fun calculateCashValue(age: Int, policyYear: Int, faceAmount: BigDecimal, annualPremium: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        val currentAge = age + policyYear

        // Future benefits
        val futureBenefits = calculateNetSinglePremium(currentAge, faceAmount, interestRate, mortalityTable)

        // Future premiums
        val futurePremiums = annualPremium.multiply(calculateAnnuityDue(currentAge, 120 - currentAge, interestRate, mortalityTable))

        val cashValue = futureBenefits.subtract(futurePremiums)

        return maxOf(BigDecimal.ZERO, cashValue).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates policy reserves using retrospective method
     */
    fun calculatePolicyReserve(age: Int, policyYear: Int, faceAmount: BigDecimal, annualPremium: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var accumulatedValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until policyYear) {
            val yearAge = age + year
            val survivalProbability = mortalityTable.getSurvivalProbability(yearAge)

            // Add premium at beginning of year
            accumulatedValue = accumulatedValue.add(annualPremium)

            // Subtract cost of insurance
            val mortalityRate = mortalityTable.getMortalityRate(yearAge)
            val costOfInsurance = faceAmount.subtract(accumulatedValue).multiply(mortalityRate)
            accumulatedValue = accumulatedValue.subtract(costOfInsurance)

            // Apply interest
            accumulatedValue = accumulatedValue.multiply(discountFactor)
        }

        return accumulatedValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates annuity present value
     */
    fun calculateAnnuityPresentValue(age: Int, term: Int, annualPayment: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var presentValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 1..term) {
            val currentAge = age + year - 1
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val discountedPayment = annualPayment.multiply(survivalProbability)
                .divide(discountFactor.pow(year), mathContext)

            presentValue = presentValue.add(discountedPayment)
        }

        return presentValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates life expectancy using mortality table
     */
    fun calculateLifeExpectancy(age: Int, mortalityTable: MortalityTable): BigDecimal {
        var lifeExpectancy = BigDecimal.ZERO
        var survivalProbability = BigDecimal.ONE

        for (currentAge in age until 120) {
            val mortalityRate = mortalityTable.getMortalityRate(currentAge)
            val yearSurvival = BigDecimal.ONE.subtract(mortalityRate)

            lifeExpectancy = lifeExpectancy.add(survivalProbability)
            survivalProbability = survivalProbability.multiply(yearSurvival)

            if (survivalProbability < BigDecimal("0.001")) break
        }

        return lifeExpectancy.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates probability of survival from one age to another
     */
    fun calculateSurvivalProbability(fromAge: Int, toAge: Int, mortalityTable: MortalityTable): BigDecimal {
        if (toAge <= fromAge) return BigDecimal.ONE

        var survivalProbability = BigDecimal.ONE

        for (age in fromAge until toAge) {
            val mortalityRate = mortalityTable.getMortalityRate(age)
            val yearSurvival = BigDecimal.ONE.subtract(mortalityRate)
            survivalProbability = survivalProbability.multiply(yearSurvival)
        }

        return survivalProbability.setScale(6, RoundingMode.HALF_UP)
    }

    /**
     * Calculates disability income insurance premium
     */
    fun calculateDisabilityPremium(age: Int, monthlyBenefit: BigDecimal, benefitPeriod: Int, eliminationPeriod: Int, disabilityTable: DisabilityTable): BigDecimal {
        val disabilityRate = disabilityTable.getDisabilityRate(age)
        val averageClaimDuration = disabilityTable.getAverageClaimDuration(age, benefitPeriod)

        val expectedClaim = monthlyBenefit.multiply(BigDecimal(12))
            .multiply(averageClaimDuration)
            .multiply(disabilityRate)

        // Apply elimination period discount
        val eliminationDiscount = when (eliminationPeriod) {
            30 -> BigDecimal("1.00")
            60 -> BigDecimal("0.85")
            90 -> BigDecimal("0.75")
            180 -> BigDecimal("0.60")
            365 -> BigDecimal("0.45")
            else -> BigDecimal("1.00")
        }

        val adjustedClaim = expectedClaim.multiply(eliminationDiscount)
        val loadedPremium = adjustedClaim.multiply(BigDecimal("1.35")) // 35% loading

        return loadedPremium.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP) // Monthly premium
    }

    /**
     * Calculates long-term care insurance premium
     */
    fun calculateLongTermCarePremium(age: Int, dailyBenefit: BigDecimal, benefitPeriod: Int, inflationProtection: Boolean, ltcTable: LongTermCareTable): BigDecimal {
        val ltcRate = ltcTable.getLongTermCareRate(age)
        val averageStayDuration = ltcTable.getAverageStayDuration(age)

        var expectedBenefit = dailyBenefit.multiply(BigDecimal(365))
            .multiply(averageStayDuration)
            .multiply(ltcRate)

        // Apply benefit period cap
        val maxBenefit = dailyBenefit.multiply(BigDecimal(365)).multiply(BigDecimal(benefitPeriod))
        expectedBenefit = minOf(expectedBenefit, maxBenefit)

        // Apply inflation protection
        if (inflationProtection) {
            val inflationFactor = BigDecimal("1.25") // 25% increase for inflation protection
            expectedBenefit = expectedBenefit.multiply(inflationFactor)
        }

        val loadedPremium = expectedBenefit.multiply(BigDecimal("1.40")) // 40% loading

        return loadedPremium.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP) // Monthly premium
    }

    /**
     * Calculates modified endowment contract (MEC) limits
     */
    fun calculateMECLimit(age: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        // 7-pay test calculation
        val netSinglePremium = calculateNetSinglePremium(age, faceAmount, interestRate, mortalityTable)
        val sevenPayPremium = netSinglePremium.divide(calculateAnnuityDue(age, 7, interestRate, mortalityTable), mathContext)

        return sevenPayPremium.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates guideline premium test limits
     */
    fun calculateGuidelinePremiumTest(age: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): GuidelinePremiumResult {
        // Guideline Single Premium (GSP)
        val gsp = calculateNetSinglePremium(age, faceAmount, interestRate, mortalityTable)
            .multiply(BigDecimal("1.25")) // 25% corridor

        // Guideline Level Premium (GLP)
        val glp = calculateLevelAnnualPremium(age, 120 - age, faceAmount, interestRate, mortalityTable, BigDecimal("0.25"))

        return GuidelinePremiumResult(gsp, glp)
    }

    /**
     * Calculates surrender charges for universal life policies
     */
    fun calculateSurrenderCharges(policyYear: Int, premiumsPaid: BigDecimal, surrenderChargeSchedule: List<BigDecimal>): BigDecimal {
        if (policyYear >= surrenderChargeSchedule.size) return BigDecimal.ZERO

        val chargeRate = surrenderChargeSchedule[policyYear]
        return premiumsPaid.multiply(chargeRate).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates cost of insurance charges for universal life
     */
    fun calculateCostOfInsurance(age: Int, netAmountAtRisk: BigDecimal, mortalityTable: MortalityTable, mortalityMultiplier: BigDecimal): BigDecimal {
        val mortalityRate = mortalityTable.getMortalityRate(age)
        val adjustedRate = mortalityRate.multiply(mortalityMultiplier)

        return netAmountAtRisk.multiply(adjustedRate).divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates variable life insurance values
     */
    fun calculateVariableLifeValue(initialPremium: BigDecimal, investmentReturn: BigDecimal, years: Int, expenseRatio: BigDecimal): BigDecimal {
        val netReturn = investmentReturn.subtract(expenseRatio)
        val growthFactor = BigDecimal.ONE.add(netReturn)

        return initialPremium.multiply(growthFactor.pow(years)).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates annuity accumulation value
     */
    fun calculateAnnuityAccumulation(initialDeposit: BigDecimal, interestRate: BigDecimal, years: Int, annualDeposit: BigDecimal): BigDecimal {
        val growthFactor = BigDecimal.ONE.add(interestRate)

        // Initial deposit growth
        var totalValue = initialDeposit.multiply(growthFactor.pow(years))

        // Annual deposits growth
        for (year in 1..years) {
            val depositGrowth = annualDeposit.multiply(growthFactor.pow(years - year))
            totalValue = totalValue.add(depositGrowth)
        }

        return totalValue.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates immediate annuity payment
     */
    fun calculateImmediateAnnuityPayment(purchasePrice: BigDecimal, age: Int, interestRate: BigDecimal, mortalityTable: MortalityTable, paymentFrequency: PaymentFrequency): BigDecimal {
        val paymentsPerYear = when (paymentFrequency) {
            PaymentFrequency.ANNUAL -> 1
            PaymentFrequency.SEMI_ANNUAL -> 2
            PaymentFrequency.QUARTERLY -> 4
            PaymentFrequency.MONTHLY -> 12
        }

        val periodicRate = interestRate.divide(BigDecimal(paymentsPerYear), 6, RoundingMode.HALF_UP)
        val annuityFactor = calculateAnnuityFactor(age, periodicRate, paymentsPerYear, mortalityTable)

        return purchasePrice.divide(annuityFactor, 2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates pension plan contributions
     */
    fun calculatePensionContribution(currentAge: Int, retirementAge: Int, desiredIncome: BigDecimal, currentBalance: BigDecimal, interestRate: BigDecimal): BigDecimal {
        val yearsToRetirement = retirementAge - currentAge
        val growthFactor = BigDecimal.ONE.add(interestRate)

        // Future value of current balance
        val futureCurrentBalance = currentBalance.multiply(growthFactor.pow(yearsToRetirement))

        // Required total at retirement (assuming 20 years of payments)
        val requiredTotal = desiredIncome.multiply(calculateAnnuityPresentValue(retirementAge, 20, BigDecimal.ONE, interestRate, StandardMortalityTable()))

        // Additional amount needed
        val additionalNeeded = requiredTotal.subtract(futureCurrentBalance)

        // Annual contribution needed
        val annuityFactor = (growthFactor.pow(yearsToRetirement).subtract(BigDecimal.ONE)).divide(interestRate, mathContext)

        return additionalNeeded.divide(annuityFactor, 2, RoundingMode.HALF_UP)
    }

    // Private helper methods
    private fun calculateTermNetSinglePremium(age: Int, term: Int, faceAmount: BigDecimal, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var presentValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until term) {
            val currentAge = age + year
            val mortalityRate = mortalityTable.getMortalityRate(currentAge)
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val yearlyBenefit = faceAmount.multiply(mortalityRate).multiply(survivalProbability)
            val discountedBenefit = yearlyBenefit.divide(discountFactor.pow(year + 1), mathContext)

            presentValue = presentValue.add(discountedBenefit)
        }

        return presentValue
    }

    private fun calculateAnnuityDue(age: Int, term: Int, interestRate: BigDecimal, mortalityTable: MortalityTable): BigDecimal {
        var annuityValue = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(interestRate)

        for (year in 0 until term) {
            val currentAge = age + year
            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)

            val discountedPayment = survivalProbability.divide(discountFactor.pow(year), mathContext)
            annuityValue = annuityValue.add(discountedPayment)
        }

        return annuityValue
    }

    private fun calculateAnnuityFactor(age: Int, periodicRate: BigDecimal, paymentsPerYear: Int, mortalityTable: MortalityTable): BigDecimal {
        var factor = BigDecimal.ZERO
        val discountFactor = BigDecimal.ONE.add(periodicRate)

        for (period in 1..1200) { // Up to 100 years of payments
            val years = period.toDouble() / paymentsPerYear
            val currentAge = age + years.toInt()

            if (currentAge >= 120) break

            val survivalProbability = calculateSurvivalProbability(age, currentAge, mortalityTable)
            val discountedPayment = survivalProbability.divide(discountFactor.pow(period), mathContext)

            factor = factor.add(discountedPayment)

            if (discountedPayment < BigDecimal("0.0001")) break
        }

        return factor
    }

    private fun BigDecimal.pow(n: Int): BigDecimal {
        return this.pow(n, mathContext)
    }

    private fun minOf(a: BigDecimal, b: BigDecimal): BigDecimal {
        return if (a <= b) a else b
    }

    private fun maxOf(a: BigDecimal, b: BigDecimal): BigDecimal {
        return if (a >= b) a else b
    }
}

// Supporting classes and interfaces
interface MortalityTable {
    fun getMortalityRate(age: Int): BigDecimal
    fun getSurvivalProbability(age: Int): BigDecimal
}

interface DisabilityTable {
    fun getDisabilityRate(age: Int): BigDecimal
    fun getAverageClaimDuration(age: Int, benefitPeriod: Int): BigDecimal
}

interface LongTermCareTable {
    fun getLongTermCareRate(age: Int): BigDecimal
    fun getAverageStayDuration(age: Int): BigDecimal
}

class StandardMortalityTable : MortalityTable {
    private val mortalityRates = mapOf(
        20 to BigDecimal("0.0008"),
        30 to BigDecimal("0.0010"),
        40 to BigDecimal("0.0015"),
        50 to BigDecimal("0.0025"),
        60 to BigDecimal("0.0045"),
        70 to BigDecimal("0.0085"),
        80 to BigDecimal("0.0165"),
        90 to BigDecimal("0.0325")
    )

    override fun getMortalityRate(age: Int): BigDecimal {
        return mortalityRates.entries
            .minByOrNull { abs(it.key - age) }
            ?.value ?: BigDecimal("0.0500")
    }

    override fun getSurvivalProbability(age: Int): BigDecimal {
        return BigDecimal.ONE.subtract(getMortalityRate(age))
    }
}

data class GuidelinePremiumResult(
    val guidelineSinglePremium: BigDecimal,
    val guidelineLevelPremium: BigDecimal
)

enum class PaymentFrequency {
    ANNUAL, SEMI_ANNUAL, QUARTERLY, MONTHLY
}
