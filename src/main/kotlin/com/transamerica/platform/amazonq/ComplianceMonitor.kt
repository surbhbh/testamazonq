package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Compliance monitoring service for regulatory requirements
 */
@Service
class ComplianceMonitor {

    fun performComplianceCheck(portfolio: InsurancePortfolio): ComplianceResult {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NAIC Model Regulation checks
        violations.addAll(checkNAICCompliance(portfolio))
        
        // State-specific compliance
        violations.addAll(checkStateCompliance(portfolio))
        
        // Federal compliance (ERISA, etc.)
        violations.addAll(checkFederalCompliance(portfolio))
        
        val overallStatus = if (violations.any { it.severity == ViolationSeverity.CRITICAL }) {
            ComplianceStatus.NON_COMPLIANT
        } else if (violations.any { it.severity == ViolationSeverity.HIGH }) {
            ComplianceStatus.REQUIRES_ATTENTION
        } else if (violations.isNotEmpty()) {
            ComplianceStatus.MINOR_ISSUES
        } else {
            ComplianceStatus.COMPLIANT
        }
        
        return ComplianceResult(
            checkDate = LocalDateTime.now(),
            overallStatus = overallStatus,
            violations = violations,
            recommendedActions = generateRecommendedActions(violations),
            nextReviewDate = LocalDateTime.now().plusMonths(3)
        )
    }

    private fun checkNAICCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // Check reserve adequacy
        val reserveAdequacy = checkReserveAdequacy(portfolio)
        if (!reserveAdequacy.isAdequate) {
            violations.add(ComplianceViolation(
                regulationType = RegulationType.NAIC,
                violationType = ViolationType.RESERVE_ADEQUACY,
                severity = ViolationSeverity.HIGH,
                description = "Reserves below required minimum",
                requirement = "NAIC Model Regulation 820",
                currentValue = reserveAdequacy.currentReserves,
                requiredValue = reserveAdequacy.requiredReserves
            ))
        }
        
        return violations
    }

    private fun checkStateCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        portfolio.policies.groupBy { it.state }.forEach { (state, policies) ->
            violations.addAll(checkStateSpecificRules(state, policies))
        }
        
        return violations
    }

    private fun checkFederalCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // ERISA compliance for group policies
        val groupPolicies = portfolio.policies.filter { it.productType.contains("GROUP") }
        if (groupPolicies.isNotEmpty()) {
            violations.addAll(checkERISACompliance(groupPolicies))
        }
        
        return violations
    }

    private fun checkReserveAdequacy(portfolio: InsurancePortfolio): ReserveAdequacyResult {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val requiredReserves = totalExposure.multiply(BigDecimal("0.05")) // 5% minimum
        val currentReserves = calculateCurrentReserves(portfolio)
        
        return ReserveAdequacyResult(
            isAdequate = currentReserves >= requiredReserves,
            currentReserves = currentReserves,
            requiredReserves = requiredReserves
        )
    }

    private fun checkStateSpecificRules(state: String, policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        when (state) {
            "NY" -> violations.addAll(checkNewYorkRules(policies))
            "CA" -> violations.addAll(checkCaliforniaRules(policies))
            "TX" -> violations.addAll(checkTexasRules(policies))
        }
        
        return violations
    }

    private fun checkNewYorkRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NY Regulation 187 - Best Interest Standard
        val largePolicies = policies.filter { it.faceAmount > BigDecimal("2000000") }
        largePolicies.forEach { policy ->
            if (!hasBestInterestDocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.STATE,
                    violationType = ViolationType.BEST_INTEREST,
                    severity = ViolationSeverity.HIGH,
                    description = "Missing best interest documentation for large policy",
                    requirement = "NY Regulation 187",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun checkCaliforniaRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // California-specific compliance checks
        return emptyList()
    }

    private fun checkTexasRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // Texas-specific compliance checks
        return emptyList()
    }

    private fun checkERISACompliance(groupPolicies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        groupPolicies.forEach { policy ->
            if (!hasERISADocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.FEDERAL,
                    violationType = ViolationType.ERISA_DOCUMENTATION,
                    severity = ViolationSeverity.CRITICAL,
                    description = "Missing ERISA documentation for group policy",
                    requirement = "ERISA Section 104",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun calculateCurrentReserves(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified reserve calculation
        return portfolio.policies.sumOf { it.faceAmount }.multiply(BigDecimal("0.06"))
    }

    private fun hasBestInterestDocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 2 == 0
    }

    private fun hasERISADocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 3 == 0
    }

    private fun generateRecommendedActions(violations: List<ComplianceViolation>): List<String> {
        val actions = mutableListOf<String>()
        
        violations.groupBy { it.violationType }.forEach { (type, typeViolations) ->
            when (type) {
                ViolationType.RESERVE_ADEQUACY -> actions.add("Increase reserves to meet regulatory requirements")
                ViolationType.BEST_INTEREST -> actions.add("Complete best interest documentation for affected policies")
                ViolationType.ERISA_DOCUMENTATION -> actions.add("Obtain required ERISA documentation")
                else -> actions.add("Address ${type.name.lowercase()} compliance issues")
            }
        }
        
        return actions
    }
}

// Data classes and enums
data class ComplianceResult(
    val checkDate: LocalDateTime,
    val overallStatus: ComplianceStatus,
    val violations: List<ComplianceViolation>,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class ComplianceViolation(
    val regulationType: RegulationType,
    val violationType: ViolationType,
    val severity: ViolationSeverity,
    val description: String,
    val requirement: String,
    val currentValue: BigDecimal,
    val requiredValue: BigDecimal
)

data class ReserveAdequacyResult(
    val isAdequate: Boolean,
    val currentReserves: BigDecimal,
    val requiredReserves: BigDecimal
)

enum class ComplianceStatus {
    COMPLIANT, MINOR_ISSUES, REQUIRES_ATTENTION, NON_COMPLIANT
}

enum class RegulationType {
    NAIC, STATE, FEDERAL, INTERNATIONAL
}

enum class ViolationType {
    RESERVE_ADEQUACY, CAPITAL_ADEQUACY, BEST_INTEREST, ERISA_DOCUMENTATION,
    SUITABILITY, DISCLOSURE, LICENSING, MARKET_CONDUCT
}

enum class ViolationSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Compliance monitoring service for regulatory requirements
 */
@Service
class ComplianceMonitor {

    fun performComplianceCheck(portfolio: InsurancePortfolio): ComplianceResult {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NAIC Model Regulation checks
        violations.addAll(checkNAICCompliance(portfolio))
        
        // State-specific compliance
        violations.addAll(checkStateCompliance(portfolio))
        
        // Federal compliance (ERISA, etc.)
        violations.addAll(checkFederalCompliance(portfolio))
        
        val overallStatus = if (violations.any { it.severity == ViolationSeverity.CRITICAL }) {
            ComplianceStatus.NON_COMPLIANT
        } else if (violations.any { it.severity == ViolationSeverity.HIGH }) {
            ComplianceStatus.REQUIRES_ATTENTION
        } else if (violations.isNotEmpty()) {
            ComplianceStatus.MINOR_ISSUES
        } else {
            ComplianceStatus.COMPLIANT
        }
        
        return ComplianceResult(
            checkDate = LocalDateTime.now(),
            overallStatus = overallStatus,
            violations = violations,
            recommendedActions = generateRecommendedActions(violations),
            nextReviewDate = LocalDateTime.now().plusMonths(3)
        )
    }

    private fun checkNAICCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // Check reserve adequacy
        val reserveAdequacy = checkReserveAdequacy(portfolio)
        if (!reserveAdequacy.isAdequate) {
            violations.add(ComplianceViolation(
                regulationType = RegulationType.NAIC,
                violationType = ViolationType.RESERVE_ADEQUACY,
                severity = ViolationSeverity.HIGH,
                description = "Reserves below required minimum",
                requirement = "NAIC Model Regulation 820",
                currentValue = reserveAdequacy.currentReserves,
                requiredValue = reserveAdequacy.requiredReserves
            ))
        }
        
        return violations
    }

    private fun checkStateCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        portfolio.policies.groupBy { it.state }.forEach { (state, policies) ->
            violations.addAll(checkStateSpecificRules(state, policies))
        }
        
        return violations
    }

    private fun checkFederalCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // ERISA compliance for group policies
        val groupPolicies = portfolio.policies.filter { it.productType.contains("GROUP") }
        if (groupPolicies.isNotEmpty()) {
            violations.addAll(checkERISACompliance(groupPolicies))
        }
        
        return violations
    }

    private fun checkReserveAdequacy(portfolio: InsurancePortfolio): ReserveAdequacyResult {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val requiredReserves = totalExposure.multiply(BigDecimal("0.05")) // 5% minimum
        val currentReserves = calculateCurrentReserves(portfolio)
        
        return ReserveAdequacyResult(
            isAdequate = currentReserves >= requiredReserves,
            currentReserves = currentReserves,
            requiredReserves = requiredReserves
        )
    }

    private fun checkStateSpecificRules(state: String, policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        when (state) {
            "NY" -> violations.addAll(checkNewYorkRules(policies))
            "CA" -> violations.addAll(checkCaliforniaRules(policies))
            "TX" -> violations.addAll(checkTexasRules(policies))
        }
        
        return violations
    }

    private fun checkNewYorkRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NY Regulation 187 - Best Interest Standard
        val largePolicies = policies.filter { it.faceAmount > BigDecimal("2000000") }
        largePolicies.forEach { policy ->
            if (!hasBestInterestDocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.STATE,
                    violationType = ViolationType.BEST_INTEREST,
                    severity = ViolationSeverity.HIGH,
                    description = "Missing best interest documentation for large policy",
                    requirement = "NY Regulation 187",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun checkCaliforniaRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // California-specific compliance checks
        return emptyList()
    }

    private fun checkTexasRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // Texas-specific compliance checks
        return emptyList()
    }

    private fun checkERISACompliance(groupPolicies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        groupPolicies.forEach { policy ->
            if (!hasERISADocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.FEDERAL,
                    violationType = ViolationType.ERISA_DOCUMENTATION,
                    severity = ViolationSeverity.CRITICAL,
                    description = "Missing ERISA documentation for group policy",
                    requirement = "ERISA Section 104",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun calculateCurrentReserves(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified reserve calculation
        return portfolio.policies.sumOf { it.faceAmount }.multiply(BigDecimal("0.06"))
    }

    private fun hasBestInterestDocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 2 == 0
    }

    private fun hasERISADocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 3 == 0
    }

    private fun generateRecommendedActions(violations: List<ComplianceViolation>): List<String> {
        val actions = mutableListOf<String>()
        
        violations.groupBy { it.violationType }.forEach { (type, typeViolations) ->
            when (type) {
                ViolationType.RESERVE_ADEQUACY -> actions.add("Increase reserves to meet regulatory requirements")
                ViolationType.BEST_INTEREST -> actions.add("Complete best interest documentation for affected policies")
                ViolationType.ERISA_DOCUMENTATION -> actions.add("Obtain required ERISA documentation")
                else -> actions.add("Address ${type.name.lowercase()} compliance issues")
            }
        }
        
        return actions
    }
}

// Data classes and enums
data class ComplianceResult(
    val checkDate: LocalDateTime,
    val overallStatus: ComplianceStatus,
    val violations: List<ComplianceViolation>,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class ComplianceViolation(
    val regulationType: RegulationType,
    val violationType: ViolationType,
    val severity: ViolationSeverity,
    val description: String,
    val requirement: String,
    val currentValue: BigDecimal,
    val requiredValue: BigDecimal
)

data class ReserveAdequacyResult(
    val isAdequate: Boolean,
    val currentReserves: BigDecimal,
    val requiredReserves: BigDecimal
)

enum class ComplianceStatus {
    COMPLIANT, MINOR_ISSUES, REQUIRES_ATTENTION, NON_COMPLIANT
}

enum class RegulationType {
    NAIC, STATE, FEDERAL, INTERNATIONAL
}

enum class ViolationType {
    RESERVE_ADEQUACY, CAPITAL_ADEQUACY, BEST_INTEREST, ERISA_DOCUMENTATION,
    SUITABILITY, DISCLOSURE, LICENSING, MARKET_CONDUCT
}

enum class ViolationSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Compliance monitoring service for regulatory requirements
 */
@Service
class ComplianceMonitor {

    fun performComplianceCheck(portfolio: InsurancePortfolio): ComplianceResult {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NAIC Model Regulation checks
        violations.addAll(checkNAICCompliance(portfolio))
        
        // State-specific compliance
        violations.addAll(checkStateCompliance(portfolio))
        
        // Federal compliance (ERISA, etc.)
        violations.addAll(checkFederalCompliance(portfolio))
        
        val overallStatus = if (violations.any { it.severity == ViolationSeverity.CRITICAL }) {
            ComplianceStatus.NON_COMPLIANT
        } else if (violations.any { it.severity == ViolationSeverity.HIGH }) {
            ComplianceStatus.REQUIRES_ATTENTION
        } else if (violations.isNotEmpty()) {
            ComplianceStatus.MINOR_ISSUES
        } else {
            ComplianceStatus.COMPLIANT
        }
        
        return ComplianceResult(
            checkDate = LocalDateTime.now(),
            overallStatus = overallStatus,
            violations = violations,
            recommendedActions = generateRecommendedActions(violations),
            nextReviewDate = LocalDateTime.now().plusMonths(3)
        )
    }

    private fun checkNAICCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // Check reserve adequacy
        val reserveAdequacy = checkReserveAdequacy(portfolio)
        if (!reserveAdequacy.isAdequate) {
            violations.add(ComplianceViolation(
                regulationType = RegulationType.NAIC,
                violationType = ViolationType.RESERVE_ADEQUACY,
                severity = ViolationSeverity.HIGH,
                description = "Reserves below required minimum",
                requirement = "NAIC Model Regulation 820",
                currentValue = reserveAdequacy.currentReserves,
                requiredValue = reserveAdequacy.requiredReserves
            ))
        }
        
        return violations
    }

    private fun checkStateCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        portfolio.policies.groupBy { it.state }.forEach { (state, policies) ->
            violations.addAll(checkStateSpecificRules(state, policies))
        }
        
        return violations
    }

    private fun checkFederalCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // ERISA compliance for group policies
        val groupPolicies = portfolio.policies.filter { it.productType.contains("GROUP") }
        if (groupPolicies.isNotEmpty()) {
            violations.addAll(checkERISACompliance(groupPolicies))
        }
        
        return violations
    }

    private fun checkReserveAdequacy(portfolio: InsurancePortfolio): ReserveAdequacyResult {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val requiredReserves = totalExposure.multiply(BigDecimal("0.05")) // 5% minimum
        val currentReserves = calculateCurrentReserves(portfolio)
        
        return ReserveAdequacyResult(
            isAdequate = currentReserves >= requiredReserves,
            currentReserves = currentReserves,
            requiredReserves = requiredReserves
        )
    }

    private fun checkStateSpecificRules(state: String, policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        when (state) {
            "NY" -> violations.addAll(checkNewYorkRules(policies))
            "CA" -> violations.addAll(checkCaliforniaRules(policies))
            "TX" -> violations.addAll(checkTexasRules(policies))
        }
        
        return violations
    }

    private fun checkNewYorkRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NY Regulation 187 - Best Interest Standard
        val largePolicies = policies.filter { it.faceAmount > BigDecimal("2000000") }
        largePolicies.forEach { policy ->
            if (!hasBestInterestDocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.STATE,
                    violationType = ViolationType.BEST_INTEREST,
                    severity = ViolationSeverity.HIGH,
                    description = "Missing best interest documentation for large policy",
                    requirement = "NY Regulation 187",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun checkCaliforniaRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // California-specific compliance checks
        return emptyList()
    }

    private fun checkTexasRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // Texas-specific compliance checks
        return emptyList()
    }

    private fun checkERISACompliance(groupPolicies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        groupPolicies.forEach { policy ->
            if (!hasERISADocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.FEDERAL,
                    violationType = ViolationType.ERISA_DOCUMENTATION,
                    severity = ViolationSeverity.CRITICAL,
                    description = "Missing ERISA documentation for group policy",
                    requirement = "ERISA Section 104",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun calculateCurrentReserves(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified reserve calculation
        return portfolio.policies.sumOf { it.faceAmount }.multiply(BigDecimal("0.06"))
    }

    private fun hasBestInterestDocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 2 == 0
    }

    private fun hasERISADocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 3 == 0
    }

    private fun generateRecommendedActions(violations: List<ComplianceViolation>): List<String> {
        val actions = mutableListOf<String>()
        
        violations.groupBy { it.violationType }.forEach { (type, typeViolations) ->
            when (type) {
                ViolationType.RESERVE_ADEQUACY -> actions.add("Increase reserves to meet regulatory requirements")
                ViolationType.BEST_INTEREST -> actions.add("Complete best interest documentation for affected policies")
                ViolationType.ERISA_DOCUMENTATION -> actions.add("Obtain required ERISA documentation")
                else -> actions.add("Address ${type.name.lowercase()} compliance issues")
            }
        }
        
        return actions
    }
}

// Data classes and enums
data class ComplianceResult(
    val checkDate: LocalDateTime,
    val overallStatus: ComplianceStatus,
    val violations: List<ComplianceViolation>,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class ComplianceViolation(
    val regulationType: RegulationType,
    val violationType: ViolationType,
    val severity: ViolationSeverity,
    val description: String,
    val requirement: String,
    val currentValue: BigDecimal,
    val requiredValue: BigDecimal
)

data class ReserveAdequacyResult(
    val isAdequate: Boolean,
    val currentReserves: BigDecimal,
    val requiredReserves: BigDecimal
)

enum class ComplianceStatus {
    COMPLIANT, MINOR_ISSUES, REQUIRES_ATTENTION, NON_COMPLIANT
}

enum class RegulationType {
    NAIC, STATE, FEDERAL, INTERNATIONAL
}

enum class ViolationType {
    RESERVE_ADEQUACY, CAPITAL_ADEQUACY, BEST_INTEREST, ERISA_DOCUMENTATION,
    SUITABILITY, DISCLOSURE, LICENSING, MARKET_CONDUCT
}

enum class ViolationSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Compliance monitoring service for regulatory requirements
 */
@Service
class ComplianceMonitor {

    fun performComplianceCheck(portfolio: InsurancePortfolio): ComplianceResult {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NAIC Model Regulation checks
        violations.addAll(checkNAICCompliance(portfolio))
        
        // State-specific compliance
        violations.addAll(checkStateCompliance(portfolio))
        
        // Federal compliance (ERISA, etc.)
        violations.addAll(checkFederalCompliance(portfolio))
        
        val overallStatus = if (violations.any { it.severity == ViolationSeverity.CRITICAL }) {
            ComplianceStatus.NON_COMPLIANT
        } else if (violations.any { it.severity == ViolationSeverity.HIGH }) {
            ComplianceStatus.REQUIRES_ATTENTION
        } else if (violations.isNotEmpty()) {
            ComplianceStatus.MINOR_ISSUES
        } else {
            ComplianceStatus.COMPLIANT
        }
        
        return ComplianceResult(
            checkDate = LocalDateTime.now(),
            overallStatus = overallStatus,
            violations = violations,
            recommendedActions = generateRecommendedActions(violations),
            nextReviewDate = LocalDateTime.now().plusMonths(3)
        )
    }

    private fun checkNAICCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // Check reserve adequacy
        val reserveAdequacy = checkReserveAdequacy(portfolio)
        if (!reserveAdequacy.isAdequate) {
            violations.add(ComplianceViolation(
                regulationType = RegulationType.NAIC,
                violationType = ViolationType.RESERVE_ADEQUACY,
                severity = ViolationSeverity.HIGH,
                description = "Reserves below required minimum",
                requirement = "NAIC Model Regulation 820",
                currentValue = reserveAdequacy.currentReserves,
                requiredValue = reserveAdequacy.requiredReserves
            ))
        }
        
        return violations
    }

    private fun checkStateCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        portfolio.policies.groupBy { it.state }.forEach { (state, policies) ->
            violations.addAll(checkStateSpecificRules(state, policies))
        }
        
        return violations
    }

    private fun checkFederalCompliance(portfolio: InsurancePortfolio): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // ERISA compliance for group policies
        val groupPolicies = portfolio.policies.filter { it.productType.contains("GROUP") }
        if (groupPolicies.isNotEmpty()) {
            violations.addAll(checkERISACompliance(groupPolicies))
        }
        
        return violations
    }

    private fun checkReserveAdequacy(portfolio: InsurancePortfolio): ReserveAdequacyResult {
        val totalExposure = portfolio.policies.sumOf { it.faceAmount }
        val requiredReserves = totalExposure.multiply(BigDecimal("0.05")) // 5% minimum
        val currentReserves = calculateCurrentReserves(portfolio)
        
        return ReserveAdequacyResult(
            isAdequate = currentReserves >= requiredReserves,
            currentReserves = currentReserves,
            requiredReserves = requiredReserves
        )
    }

    private fun checkStateSpecificRules(state: String, policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        when (state) {
            "NY" -> violations.addAll(checkNewYorkRules(policies))
            "CA" -> violations.addAll(checkCaliforniaRules(policies))
            "TX" -> violations.addAll(checkTexasRules(policies))
        }
        
        return violations
    }

    private fun checkNewYorkRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        // NY Regulation 187 - Best Interest Standard
        val largePolicies = policies.filter { it.faceAmount > BigDecimal("2000000") }
        largePolicies.forEach { policy ->
            if (!hasBestInterestDocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.STATE,
                    violationType = ViolationType.BEST_INTEREST,
                    severity = ViolationSeverity.HIGH,
                    description = "Missing best interest documentation for large policy",
                    requirement = "NY Regulation 187",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun checkCaliforniaRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // California-specific compliance checks
        return emptyList()
    }

    private fun checkTexasRules(policies: List<PolicyInfo>): List<ComplianceViolation> {
        // Texas-specific compliance checks
        return emptyList()
    }

    private fun checkERISACompliance(groupPolicies: List<PolicyInfo>): List<ComplianceViolation> {
        val violations = mutableListOf<ComplianceViolation>()
        
        groupPolicies.forEach { policy ->
            if (!hasERISADocumentation(policy)) {
                violations.add(ComplianceViolation(
                    regulationType = RegulationType.FEDERAL,
                    violationType = ViolationType.ERISA_DOCUMENTATION,
                    severity = ViolationSeverity.CRITICAL,
                    description = "Missing ERISA documentation for group policy",
                    requirement = "ERISA Section 104",
                    currentValue = BigDecimal.ZERO,
                    requiredValue = BigDecimal.ONE
                ))
            }
        }
        
        return violations
    }

    private fun calculateCurrentReserves(portfolio: InsurancePortfolio): BigDecimal {
        // Simplified reserve calculation
        return portfolio.policies.sumOf { it.faceAmount }.multiply(BigDecimal("0.06"))
    }

    private fun hasBestInterestDocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 2 == 0
    }

    private fun hasERISADocumentation(policy: PolicyInfo): Boolean {
        // Mock implementation
        return policy.policyNumber.hashCode() % 3 == 0
    }

    private fun generateRecommendedActions(violations: List<ComplianceViolation>): List<String> {
        val actions = mutableListOf<String>()
        
        violations.groupBy { it.violationType }.forEach { (type, typeViolations) ->
            when (type) {
                ViolationType.RESERVE_ADEQUACY -> actions.add("Increase reserves to meet regulatory requirements")
                ViolationType.BEST_INTEREST -> actions.add("Complete best interest documentation for affected policies")
                ViolationType.ERISA_DOCUMENTATION -> actions.add("Obtain required ERISA documentation")
                else -> actions.add("Address ${type.name.lowercase()} compliance issues")
            }
        }
        
        return actions
    }
}

// Data classes and enums
data class ComplianceResult(
    val checkDate: LocalDateTime,
    val overallStatus: ComplianceStatus,
    val violations: List<ComplianceViolation>,
    val recommendedActions: List<String>,
    val nextReviewDate: LocalDateTime
)

data class ComplianceViolation(
    val regulationType: RegulationType,
    val violationType: ViolationType,
    val severity: ViolationSeverity,
    val description: String,
    val requirement: String,
    val currentValue: BigDecimal,
    val requiredValue: BigDecimal
)

data class ReserveAdequacyResult(
    val isAdequate: Boolean,
    val currentReserves: BigDecimal,
    val requiredReserves: BigDecimal
)

enum class ComplianceStatus {
    COMPLIANT, MINOR_ISSUES, REQUIRES_ATTENTION, NON_COMPLIANT
}

enum class RegulationType {
    NAIC, STATE, FEDERAL, INTERNATIONAL
}

enum class ViolationType {
    RESERVE_ADEQUACY, CAPITAL_ADEQUACY, BEST_INTEREST, ERISA_DOCUMENTATION,
    SUITABILITY, DISCLOSURE, LICENSING, MARKET_CONDUCT
}

enum class ViolationSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
