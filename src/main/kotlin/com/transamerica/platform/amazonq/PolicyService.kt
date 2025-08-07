package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing insurance policies
 * Handles policy creation, updates, and business logic
 */
@Service
class PolicyService {

    /**
     * Creates a new insurance policy
     */
    fun createPolicy(request: PolicyCreationRequest): Policy {
        validatePolicyRequest(request)
        
        val policy = Policy(
            policyNumber = generatePolicyNumber(),
            customerId = request.customerId,
            productId = request.productId,
            faceAmount = request.faceAmount,
            premium = calculatePremium(request),
            issueDate = LocalDate.now(),
            status = PolicyStatus.ACTIVE
        )
        
        return savePolicy(policy)
    }

    /**
     * Calculates premium based on risk factors
     */
    fun calculatePremium(request: PolicyCreationRequest): BigDecimal {
        var basePremium = request.faceAmount.multiply(BigDecimal("0.001"))
        
        // Age factor
        val ageFactor = when {
            request.age < 30 -> BigDecimal("0.8")
            request.age < 40 -> BigDecimal("1.0")
            request.age < 50 -> BigDecimal("1.2")
            request.age < 60 -> BigDecimal("1.5")
            else -> BigDecimal("2.0")
        }
        
        // Risk factor
        val riskFactor = when (request.riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.9")
            RiskClass.STANDARD -> BigDecimal("1.0")
            RiskClass.SUBSTANDARD -> BigDecimal("1.3")
            RiskClass.DECLINED -> throw IllegalArgumentException("Cannot issue policy for declined risk")
        }
        
        // Smoking factor
        val smokingFactor = if (request.isSmoker) BigDecimal("1.5") else BigDecimal("1.0")
        
        basePremium = basePremium.multiply(ageFactor).multiply(riskFactor).multiply(smokingFactor)
        
        return basePremium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Updates policy status
     */
    fun updatePolicyStatus(policyNumber: String, newStatus: PolicyStatus): Policy {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        validateStatusTransition(policy.status, newStatus)
        
        val updatedPolicy = policy.copy(
            status = newStatus,
            lastModified = LocalDateTime.now()
        )
        
        return savePolicy(updatedPolicy)
    }

    /**
     * Processes premium payment
     */
    fun processPremiumPayment(policyNumber: String, amount: BigDecimal, paymentMethod: PaymentMethod): PaymentResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (policy.status != PolicyStatus.ACTIVE) {
            throw IllegalStateException("Cannot process payment for inactive policy")
        }
        
        val payment = Payment(
            policyNumber = policyNumber,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        return processPayment(payment)
    }

    /**
     * Calculates cash value for whole life policies
     */
    fun calculateCashValue(policyNumber: String): BigDecimal {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (!policy.hasCashValue()) {
            return BigDecimal.ZERO
        }
        
        val yearsInForce = java.time.Period.between(policy.issueDate, LocalDate.now()).years
        val premiumsPaid = policy.premium.multiply(BigDecimal(yearsInForce * 12))
        val interestRate = BigDecimal("0.04") // 4% annual interest
        
        // Simplified cash value calculation
        var cashValue = BigDecimal.ZERO
        for (year in 1..yearsInForce) {
            val yearlyPremium = policy.premium.multiply(BigDecimal("12"))
            val expenses = yearlyPremium.multiply(BigDecimal("0.15")) // 15% expenses
            val netPremium = yearlyPremium.subtract(expenses)
            
            cashValue = cashValue.add(netPremium)
            cashValue = cashValue.multiply(BigDecimal.ONE.add(interestRate))
        }
        
        return cashValue.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Processes policy loan request
     */
    fun processPolicyLoan(policyNumber: String, loanAmount: BigDecimal): LoanResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        val cashValue = calculateCashValue(policyNumber)
        val maxLoanAmount = cashValue.multiply(BigDecimal("0.9")) // 90% of cash value
        
        if (loanAmount > maxLoanAmount) {
            throw IllegalArgumentException("Loan amount exceeds maximum allowed: $maxLoanAmount")
        }
        
        val loan = PolicyLoan(
            policyNumber = policyNumber,
            loanAmount = loanAmount,
            interestRate = BigDecimal("0.055"), // 5.5% annual interest
            loanDate = LocalDateTime.now(),
            status = LoanStatus.ACTIVE
        )
        
        return processLoan(loan)
    }

    // Private helper methods
    private fun validatePolicyRequest(request: PolicyCreationRequest) {
        require(request.faceAmount > BigDecimal.ZERO) { "Face amount must be positive" }
        require(request.age >= 18) { "Minimum age is 18" }
        require(request.age <= 85) { "Maximum age is 85" }
        require(request.customerId.isNotBlank()) { "Customer ID is required" }
        require(request.productId.isNotBlank()) { "Product ID is required" }
    }

    private fun generatePolicyNumber(): String {
        return "POL-${System.currentTimeMillis()}"
    }

    private fun validateStatusTransition(currentStatus: PolicyStatus, newStatus: PolicyStatus) {
        val validTransitions = mapOf(
            PolicyStatus.PENDING to setOf(PolicyStatus.ACTIVE, PolicyStatus.DECLINED),
            PolicyStatus.ACTIVE to setOf(PolicyStatus.LAPSED, PolicyStatus.SURRENDERED, PolicyStatus.MATURED),
            PolicyStatus.LAPSED to setOf(PolicyStatus.ACTIVE, PolicyStatus.SURRENDERED),
            PolicyStatus.SURRENDERED to emptySet<PolicyStatus>(),
            PolicyStatus.MATURED to emptySet<PolicyStatus>(),
            PolicyStatus.DECLINED to emptySet<PolicyStatus>()
        )
        
        if (newStatus !in validTransitions[currentStatus].orEmpty()) {
            throw IllegalStateException("Invalid status transition from $currentStatus to $newStatus")
        }
    }

    private fun findPolicyByNumber(policyNumber: String): Policy? {
        // Mock implementation - would typically query database
        return null
    }

    private fun savePolicy(policy: Policy): Policy {
        // Mock implementation - would typically save to database
        return policy
    }

    private fun processPayment(payment: Payment): PaymentResult {
        // Mock implementation - would typically process through payment gateway
        return PaymentResult(
            paymentId = "PAY-${System.currentTimeMillis()}",
            status = PaymentStatus.COMPLETED,
            confirmationNumber = "CONF-${System.currentTimeMillis()}"
        )
    }

    private fun processLoan(loan: PolicyLoan): LoanResult {
        // Mock implementation - would typically process loan disbursement
        return LoanResult(
            loanId = "LOAN-${System.currentTimeMillis()}",
            status = LoanStatus.ACTIVE,
            disbursementDate = LocalDateTime.now()
        )
    }
}

// Data classes
data class PolicyCreationRequest(
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val riskClass: RiskClass,
    val isSmoker: Boolean
)

data class Policy(
    val policyNumber: String,
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val premium: BigDecimal,
    val issueDate: LocalDate,
    val status: PolicyStatus,
    val lastModified: LocalDateTime = LocalDateTime.now()
) {
    fun hasCashValue(): Boolean = productId.startsWith("WHOLE_LIFE")
}

data class Payment(
    val policyNumber: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    val status: PaymentStatus
)

data class PaymentResult(
    val paymentId: String,
    val status: PaymentStatus,
    val confirmationNumber: String
)

data class PolicyLoan(
    val policyNumber: String,
    val loanAmount: BigDecimal,
    val interestRate: BigDecimal,
    val loanDate: LocalDateTime,
    val status: LoanStatus
)

data class LoanResult(
    val loanId: String,
    val status: LoanStatus,
    val disbursementDate: LocalDateTime
)

// Enums
enum class PolicyStatus {
    PENDING, ACTIVE, LAPSED, SURRENDERED, MATURED, DECLINED
}

enum class RiskClass {
    PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class PaymentMethod {
    BANK_TRANSFER, CREDIT_CARD, CHECK, ACH
}

enum class PaymentStatus {
    PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class LoanStatus {
    ACTIVE, PAID_OFF, DEFAULTED
}

// Exceptions
class PolicyNotFoundException(message: String) : RuntimeException(message)package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing insurance policies
 * Handles policy creation, updates, and business logic
 */
@Service
class PolicyService {

    /**
     * Creates a new insurance policy
     */
    fun createPolicy(request: PolicyCreationRequest): Policy {
        validatePolicyRequest(request)
        
        val policy = Policy(
            policyNumber = generatePolicyNumber(),
            customerId = request.customerId,
            productId = request.productId,
            faceAmount = request.faceAmount,
            premium = calculatePremium(request),
            issueDate = LocalDate.now(),
            status = PolicyStatus.ACTIVE
        )
        
        return savePolicy(policy)
    }

    /**
     * Calculates premium based on risk factors
     */
    fun calculatePremium(request: PolicyCreationRequest): BigDecimal {
        var basePremium = request.faceAmount.multiply(BigDecimal("0.001"))
        
        // Age factor
        val ageFactor = when {
            request.age < 30 -> BigDecimal("0.8")
            request.age < 40 -> BigDecimal("1.0")
            request.age < 50 -> BigDecimal("1.2")
            request.age < 60 -> BigDecimal("1.5")
            else -> BigDecimal("2.0")
        }
        
        // Risk factor
        val riskFactor = when (request.riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.9")
            RiskClass.STANDARD -> BigDecimal("1.0")
            RiskClass.SUBSTANDARD -> BigDecimal("1.3")
            RiskClass.DECLINED -> throw IllegalArgumentException("Cannot issue policy for declined risk")
        }
        
        // Smoking factor
        val smokingFactor = if (request.isSmoker) BigDecimal("1.5") else BigDecimal("1.0")
        
        basePremium = basePremium.multiply(ageFactor).multiply(riskFactor).multiply(smokingFactor)
        
        return basePremium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Updates policy status
     */
    fun updatePolicyStatus(policyNumber: String, newStatus: PolicyStatus): Policy {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        validateStatusTransition(policy.status, newStatus)
        
        val updatedPolicy = policy.copy(
            status = newStatus,
            lastModified = LocalDateTime.now()
        )
        
        return savePolicy(updatedPolicy)
    }

    /**
     * Processes premium payment
     */
    fun processPremiumPayment(policyNumber: String, amount: BigDecimal, paymentMethod: PaymentMethod): PaymentResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (policy.status != PolicyStatus.ACTIVE) {
            throw IllegalStateException("Cannot process payment for inactive policy")
        }
        
        val payment = Payment(
            policyNumber = policyNumber,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        return processPayment(payment)
    }

    /**
     * Calculates cash value for whole life policies
     */
    fun calculateCashValue(policyNumber: String): BigDecimal {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (!policy.hasCashValue()) {
            return BigDecimal.ZERO
        }
        
        val yearsInForce = java.time.Period.between(policy.issueDate, LocalDate.now()).years
        val premiumsPaid = policy.premium.multiply(BigDecimal(yearsInForce * 12))
        val interestRate = BigDecimal("0.04") // 4% annual interest
        
        // Simplified cash value calculation
        var cashValue = BigDecimal.ZERO
        for (year in 1..yearsInForce) {
            val yearlyPremium = policy.premium.multiply(BigDecimal("12"))
            val expenses = yearlyPremium.multiply(BigDecimal("0.15")) // 15% expenses
            val netPremium = yearlyPremium.subtract(expenses)
            
            cashValue = cashValue.add(netPremium)
            cashValue = cashValue.multiply(BigDecimal.ONE.add(interestRate))
        }
        
        return cashValue.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Processes policy loan request
     */
    fun processPolicyLoan(policyNumber: String, loanAmount: BigDecimal): LoanResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        val cashValue = calculateCashValue(policyNumber)
        val maxLoanAmount = cashValue.multiply(BigDecimal("0.9")) // 90% of cash value
        
        if (loanAmount > maxLoanAmount) {
            throw IllegalArgumentException("Loan amount exceeds maximum allowed: $maxLoanAmount")
        }
        
        val loan = PolicyLoan(
            policyNumber = policyNumber,
            loanAmount = loanAmount,
            interestRate = BigDecimal("0.055"), // 5.5% annual interest
            loanDate = LocalDateTime.now(),
            status = LoanStatus.ACTIVE
        )
        
        return processLoan(loan)
    }

    // Private helper methods
    private fun validatePolicyRequest(request: PolicyCreationRequest) {
        require(request.faceAmount > BigDecimal.ZERO) { "Face amount must be positive" }
        require(request.age >= 18) { "Minimum age is 18" }
        require(request.age <= 85) { "Maximum age is 85" }
        require(request.customerId.isNotBlank()) { "Customer ID is required" }
        require(request.productId.isNotBlank()) { "Product ID is required" }
    }

    private fun generatePolicyNumber(): String {
        return "POL-${System.currentTimeMillis()}"
    }

    private fun validateStatusTransition(currentStatus: PolicyStatus, newStatus: PolicyStatus) {
        val validTransitions = mapOf(
            PolicyStatus.PENDING to setOf(PolicyStatus.ACTIVE, PolicyStatus.DECLINED),
            PolicyStatus.ACTIVE to setOf(PolicyStatus.LAPSED, PolicyStatus.SURRENDERED, PolicyStatus.MATURED),
            PolicyStatus.LAPSED to setOf(PolicyStatus.ACTIVE, PolicyStatus.SURRENDERED),
            PolicyStatus.SURRENDERED to emptySet<PolicyStatus>(),
            PolicyStatus.MATURED to emptySet<PolicyStatus>(),
            PolicyStatus.DECLINED to emptySet<PolicyStatus>()
        )
        
        if (newStatus !in validTransitions[currentStatus].orEmpty()) {
            throw IllegalStateException("Invalid status transition from $currentStatus to $newStatus")
        }
    }

    private fun findPolicyByNumber(policyNumber: String): Policy? {
        // Mock implementation - would typically query database
        return null
    }

    private fun savePolicy(policy: Policy): Policy {
        // Mock implementation - would typically save to database
        return policy
    }

    private fun processPayment(payment: Payment): PaymentResult {
        // Mock implementation - would typically process through payment gateway
        return PaymentResult(
            paymentId = "PAY-${System.currentTimeMillis()}",
            status = PaymentStatus.COMPLETED,
            confirmationNumber = "CONF-${System.currentTimeMillis()}"
        )
    }

    private fun processLoan(loan: PolicyLoan): LoanResult {
        // Mock implementation - would typically process loan disbursement
        return LoanResult(
            loanId = "LOAN-${System.currentTimeMillis()}",
            status = LoanStatus.ACTIVE,
            disbursementDate = LocalDateTime.now()
        )
    }
}

// Data classes
data class PolicyCreationRequest(
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val riskClass: RiskClass,
    val isSmoker: Boolean
)

data class Policy(
    val policyNumber: String,
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val premium: BigDecimal,
    val issueDate: LocalDate,
    val status: PolicyStatus,
    val lastModified: LocalDateTime = LocalDateTime.now()
) {
    fun hasCashValue(): Boolean = productId.startsWith("WHOLE_LIFE")
}

data class Payment(
    val policyNumber: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    val status: PaymentStatus
)

data class PaymentResult(
    val paymentId: String,
    val status: PaymentStatus,
    val confirmationNumber: String
)

data class PolicyLoan(
    val policyNumber: String,
    val loanAmount: BigDecimal,
    val interestRate: BigDecimal,
    val loanDate: LocalDateTime,
    val status: LoanStatus
)

data class LoanResult(
    val loanId: String,
    val status: LoanStatus,
    val disbursementDate: LocalDateTime
)

// Enums
enum class PolicyStatus {
    PENDING, ACTIVE, LAPSED, SURRENDERED, MATURED, DECLINED
}

enum class RiskClass {
    PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class PaymentMethod {
    BANK_TRANSFER, CREDIT_CARD, CHECK, ACH
}

enum class PaymentStatus {
    PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class LoanStatus {
    ACTIVE, PAID_OFF, DEFAULTED
}

// Exceptions
class PolicyNotFoundException(message: String) : RuntimeException(message)package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing insurance policies
 * Handles policy creation, updates, and business logic
 */
@Service
class PolicyService {

    /**
     * Creates a new insurance policy
     */
    fun createPolicy(request: PolicyCreationRequest): Policy {
        validatePolicyRequest(request)
        
        val policy = Policy(
            policyNumber = generatePolicyNumber(),
            customerId = request.customerId,
            productId = request.productId,
            faceAmount = request.faceAmount,
            premium = calculatePremium(request),
            issueDate = LocalDate.now(),
            status = PolicyStatus.ACTIVE
        )
        
        return savePolicy(policy)
    }

    /**
     * Calculates premium based on risk factors
     */
    fun calculatePremium(request: PolicyCreationRequest): BigDecimal {
        var basePremium = request.faceAmount.multiply(BigDecimal("0.001"))
        
        // Age factor
        val ageFactor = when {
            request.age < 30 -> BigDecimal("0.8")
            request.age < 40 -> BigDecimal("1.0")
            request.age < 50 -> BigDecimal("1.2")
            request.age < 60 -> BigDecimal("1.5")
            else -> BigDecimal("2.0")
        }
        
        // Risk factor
        val riskFactor = when (request.riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.9")
            RiskClass.STANDARD -> BigDecimal("1.0")
            RiskClass.SUBSTANDARD -> BigDecimal("1.3")
            RiskClass.DECLINED -> throw IllegalArgumentException("Cannot issue policy for declined risk")
        }
        
        // Smoking factor
        val smokingFactor = if (request.isSmoker) BigDecimal("1.5") else BigDecimal("1.0")
        
        basePremium = basePremium.multiply(ageFactor).multiply(riskFactor).multiply(smokingFactor)
        
        return basePremium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Updates policy status
     */
    fun updatePolicyStatus(policyNumber: String, newStatus: PolicyStatus): Policy {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        validateStatusTransition(policy.status, newStatus)
        
        val updatedPolicy = policy.copy(
            status = newStatus,
            lastModified = LocalDateTime.now()
        )
        
        return savePolicy(updatedPolicy)
    }

    /**
     * Processes premium payment
     */
    fun processPremiumPayment(policyNumber: String, amount: BigDecimal, paymentMethod: PaymentMethod): PaymentResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (policy.status != PolicyStatus.ACTIVE) {
            throw IllegalStateException("Cannot process payment for inactive policy")
        }
        
        val payment = Payment(
            policyNumber = policyNumber,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        return processPayment(payment)
    }

    /**
     * Calculates cash value for whole life policies
     */
    fun calculateCashValue(policyNumber: String): BigDecimal {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (!policy.hasCashValue()) {
            return BigDecimal.ZERO
        }
        
        val yearsInForce = java.time.Period.between(policy.issueDate, LocalDate.now()).years
        val premiumsPaid = policy.premium.multiply(BigDecimal(yearsInForce * 12))
        val interestRate = BigDecimal("0.04") // 4% annual interest
        
        // Simplified cash value calculation
        var cashValue = BigDecimal.ZERO
        for (year in 1..yearsInForce) {
            val yearlyPremium = policy.premium.multiply(BigDecimal("12"))
            val expenses = yearlyPremium.multiply(BigDecimal("0.15")) // 15% expenses
            val netPremium = yearlyPremium.subtract(expenses)
            
            cashValue = cashValue.add(netPremium)
            cashValue = cashValue.multiply(BigDecimal.ONE.add(interestRate))
        }
        
        return cashValue.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Processes policy loan request
     */
    fun processPolicyLoan(policyNumber: String, loanAmount: BigDecimal): LoanResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        val cashValue = calculateCashValue(policyNumber)
        val maxLoanAmount = cashValue.multiply(BigDecimal("0.9")) // 90% of cash value
        
        if (loanAmount > maxLoanAmount) {
            throw IllegalArgumentException("Loan amount exceeds maximum allowed: $maxLoanAmount")
        }
        
        val loan = PolicyLoan(
            policyNumber = policyNumber,
            loanAmount = loanAmount,
            interestRate = BigDecimal("0.055"), // 5.5% annual interest
            loanDate = LocalDateTime.now(),
            status = LoanStatus.ACTIVE
        )
        
        return processLoan(loan)
    }

    // Private helper methods
    private fun validatePolicyRequest(request: PolicyCreationRequest) {
        require(request.faceAmount > BigDecimal.ZERO) { "Face amount must be positive" }
        require(request.age >= 18) { "Minimum age is 18" }
        require(request.age <= 85) { "Maximum age is 85" }
        require(request.customerId.isNotBlank()) { "Customer ID is required" }
        require(request.productId.isNotBlank()) { "Product ID is required" }
    }

    private fun generatePolicyNumber(): String {
        return "POL-${System.currentTimeMillis()}"
    }

    private fun validateStatusTransition(currentStatus: PolicyStatus, newStatus: PolicyStatus) {
        val validTransitions = mapOf(
            PolicyStatus.PENDING to setOf(PolicyStatus.ACTIVE, PolicyStatus.DECLINED),
            PolicyStatus.ACTIVE to setOf(PolicyStatus.LAPSED, PolicyStatus.SURRENDERED, PolicyStatus.MATURED),
            PolicyStatus.LAPSED to setOf(PolicyStatus.ACTIVE, PolicyStatus.SURRENDERED),
            PolicyStatus.SURRENDERED to emptySet<PolicyStatus>(),
            PolicyStatus.MATURED to emptySet<PolicyStatus>(),
            PolicyStatus.DECLINED to emptySet<PolicyStatus>()
        )
        
        if (newStatus !in validTransitions[currentStatus].orEmpty()) {
            throw IllegalStateException("Invalid status transition from $currentStatus to $newStatus")
        }
    }

    private fun findPolicyByNumber(policyNumber: String): Policy? {
        // Mock implementation - would typically query database
        return null
    }

    private fun savePolicy(policy: Policy): Policy {
        // Mock implementation - would typically save to database
        return policy
    }

    private fun processPayment(payment: Payment): PaymentResult {
        // Mock implementation - would typically process through payment gateway
        return PaymentResult(
            paymentId = "PAY-${System.currentTimeMillis()}",
            status = PaymentStatus.COMPLETED,
            confirmationNumber = "CONF-${System.currentTimeMillis()}"
        )
    }

    private fun processLoan(loan: PolicyLoan): LoanResult {
        // Mock implementation - would typically process loan disbursement
        return LoanResult(
            loanId = "LOAN-${System.currentTimeMillis()}",
            status = LoanStatus.ACTIVE,
            disbursementDate = LocalDateTime.now()
        )
    }
}

// Data classes
data class PolicyCreationRequest(
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val riskClass: RiskClass,
    val isSmoker: Boolean
)

data class Policy(
    val policyNumber: String,
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val premium: BigDecimal,
    val issueDate: LocalDate,
    val status: PolicyStatus,
    val lastModified: LocalDateTime = LocalDateTime.now()
) {
    fun hasCashValue(): Boolean = productId.startsWith("WHOLE_LIFE")
}

data class Payment(
    val policyNumber: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    val status: PaymentStatus
)

data class PaymentResult(
    val paymentId: String,
    val status: PaymentStatus,
    val confirmationNumber: String
)

data class PolicyLoan(
    val policyNumber: String,
    val loanAmount: BigDecimal,
    val interestRate: BigDecimal,
    val loanDate: LocalDateTime,
    val status: LoanStatus
)

data class LoanResult(
    val loanId: String,
    val status: LoanStatus,
    val disbursementDate: LocalDateTime
)

// Enums
enum class PolicyStatus {
    PENDING, ACTIVE, LAPSED, SURRENDERED, MATURED, DECLINED
}

enum class RiskClass {
    PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class PaymentMethod {
    BANK_TRANSFER, CREDIT_CARD, CHECK, ACH
}

enum class PaymentStatus {
    PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class LoanStatus {
    ACTIVE, PAID_OFF, DEFAULTED
}

// Exceptions
class PolicyNotFoundException(message: String) : RuntimeException(message)package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing insurance policies
 * Handles policy creation, updates, and business logic
 */
@Service
class PolicyService {

    /**
     * Creates a new insurance policy
     */
    fun createPolicy(request: PolicyCreationRequest): Policy {
        validatePolicyRequest(request)
        
        val policy = Policy(
            policyNumber = generatePolicyNumber(),
            customerId = request.customerId,
            productId = request.productId,
            faceAmount = request.faceAmount,
            premium = calculatePremium(request),
            issueDate = LocalDate.now(),
            status = PolicyStatus.ACTIVE
        )
        
        return savePolicy(policy)
    }

    /**
     * Calculates premium based on risk factors
     */
    fun calculatePremium(request: PolicyCreationRequest): BigDecimal {
        var basePremium = request.faceAmount.multiply(BigDecimal("0.001"))
        
        // Age factor
        val ageFactor = when {
            request.age < 30 -> BigDecimal("0.8")
            request.age < 40 -> BigDecimal("1.0")
            request.age < 50 -> BigDecimal("1.2")
            request.age < 60 -> BigDecimal("1.5")
            else -> BigDecimal("2.0")
        }
        
        // Risk factor
        val riskFactor = when (request.riskClass) {
            RiskClass.PREFERRED -> BigDecimal("0.9")
            RiskClass.STANDARD -> BigDecimal("1.0")
            RiskClass.SUBSTANDARD -> BigDecimal("1.3")
            RiskClass.DECLINED -> throw IllegalArgumentException("Cannot issue policy for declined risk")
        }
        
        // Smoking factor
        val smokingFactor = if (request.isSmoker) BigDecimal("1.5") else BigDecimal("1.0")
        
        basePremium = basePremium.multiply(ageFactor).multiply(riskFactor).multiply(smokingFactor)
        
        return basePremium.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Updates policy status
     */
    fun updatePolicyStatus(policyNumber: String, newStatus: PolicyStatus): Policy {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        validateStatusTransition(policy.status, newStatus)
        
        val updatedPolicy = policy.copy(
            status = newStatus,
            lastModified = LocalDateTime.now()
        )
        
        return savePolicy(updatedPolicy)
    }

    /**
     * Processes premium payment
     */
    fun processPremiumPayment(policyNumber: String, amount: BigDecimal, paymentMethod: PaymentMethod): PaymentResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (policy.status != PolicyStatus.ACTIVE) {
            throw IllegalStateException("Cannot process payment for inactive policy")
        }
        
        val payment = Payment(
            policyNumber = policyNumber,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        return processPayment(payment)
    }

    /**
     * Calculates cash value for whole life policies
     */
    fun calculateCashValue(policyNumber: String): BigDecimal {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        if (!policy.hasCashValue()) {
            return BigDecimal.ZERO
        }
        
        val yearsInForce = java.time.Period.between(policy.issueDate, LocalDate.now()).years
        val premiumsPaid = policy.premium.multiply(BigDecimal(yearsInForce * 12))
        val interestRate = BigDecimal("0.04") // 4% annual interest
        
        // Simplified cash value calculation
        var cashValue = BigDecimal.ZERO
        for (year in 1..yearsInForce) {
            val yearlyPremium = policy.premium.multiply(BigDecimal("12"))
            val expenses = yearlyPremium.multiply(BigDecimal("0.15")) // 15% expenses
            val netPremium = yearlyPremium.subtract(expenses)
            
            cashValue = cashValue.add(netPremium)
            cashValue = cashValue.multiply(BigDecimal.ONE.add(interestRate))
        }
        
        return cashValue.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Processes policy loan request
     */
    fun processPolicyLoan(policyNumber: String, loanAmount: BigDecimal): LoanResult {
        val policy = findPolicyByNumber(policyNumber)
            ?: throw PolicyNotFoundException("Policy not found: $policyNumber")
        
        val cashValue = calculateCashValue(policyNumber)
        val maxLoanAmount = cashValue.multiply(BigDecimal("0.9")) // 90% of cash value
        
        if (loanAmount > maxLoanAmount) {
            throw IllegalArgumentException("Loan amount exceeds maximum allowed: $maxLoanAmount")
        }
        
        val loan = PolicyLoan(
            policyNumber = policyNumber,
            loanAmount = loanAmount,
            interestRate = BigDecimal("0.055"), // 5.5% annual interest
            loanDate = LocalDateTime.now(),
            status = LoanStatus.ACTIVE
        )
        
        return processLoan(loan)
    }

    // Private helper methods
    private fun validatePolicyRequest(request: PolicyCreationRequest) {
        require(request.faceAmount > BigDecimal.ZERO) { "Face amount must be positive" }
        require(request.age >= 18) { "Minimum age is 18" }
        require(request.age <= 85) { "Maximum age is 85" }
        require(request.customerId.isNotBlank()) { "Customer ID is required" }
        require(request.productId.isNotBlank()) { "Product ID is required" }
    }

    private fun generatePolicyNumber(): String {
        return "POL-${System.currentTimeMillis()}"
    }

    private fun validateStatusTransition(currentStatus: PolicyStatus, newStatus: PolicyStatus) {
        val validTransitions = mapOf(
            PolicyStatus.PENDING to setOf(PolicyStatus.ACTIVE, PolicyStatus.DECLINED),
            PolicyStatus.ACTIVE to setOf(PolicyStatus.LAPSED, PolicyStatus.SURRENDERED, PolicyStatus.MATURED),
            PolicyStatus.LAPSED to setOf(PolicyStatus.ACTIVE, PolicyStatus.SURRENDERED),
            PolicyStatus.SURRENDERED to emptySet<PolicyStatus>(),
            PolicyStatus.MATURED to emptySet<PolicyStatus>(),
            PolicyStatus.DECLINED to emptySet<PolicyStatus>()
        )
        
        if (newStatus !in validTransitions[currentStatus].orEmpty()) {
            throw IllegalStateException("Invalid status transition from $currentStatus to $newStatus")
        }
    }

    private fun findPolicyByNumber(policyNumber: String): Policy? {
        // Mock implementation - would typically query database
        return null
    }

    private fun savePolicy(policy: Policy): Policy {
        // Mock implementation - would typically save to database
        return policy
    }

    private fun processPayment(payment: Payment): PaymentResult {
        // Mock implementation - would typically process through payment gateway
        return PaymentResult(
            paymentId = "PAY-${System.currentTimeMillis()}",
            status = PaymentStatus.COMPLETED,
            confirmationNumber = "CONF-${System.currentTimeMillis()}"
        )
    }

    private fun processLoan(loan: PolicyLoan): LoanResult {
        // Mock implementation - would typically process loan disbursement
        return LoanResult(
            loanId = "LOAN-${System.currentTimeMillis()}",
            status = LoanStatus.ACTIVE,
            disbursementDate = LocalDateTime.now()
        )
    }
}

// Data classes
data class PolicyCreationRequest(
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val age: Int,
    val riskClass: RiskClass,
    val isSmoker: Boolean
)

data class Policy(
    val policyNumber: String,
    val customerId: String,
    val productId: String,
    val faceAmount: BigDecimal,
    val premium: BigDecimal,
    val issueDate: LocalDate,
    val status: PolicyStatus,
    val lastModified: LocalDateTime = LocalDateTime.now()
) {
    fun hasCashValue(): Boolean = productId.startsWith("WHOLE_LIFE")
}

data class Payment(
    val policyNumber: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    val status: PaymentStatus
)

data class PaymentResult(
    val paymentId: String,
    val status: PaymentStatus,
    val confirmationNumber: String
)

data class PolicyLoan(
    val policyNumber: String,
    val loanAmount: BigDecimal,
    val interestRate: BigDecimal,
    val loanDate: LocalDateTime,
    val status: LoanStatus
)

data class LoanResult(
    val loanId: String,
    val status: LoanStatus,
    val disbursementDate: LocalDateTime
)

// Enums
enum class PolicyStatus {
    PENDING, ACTIVE, LAPSED, SURRENDERED, MATURED, DECLINED
}

enum class RiskClass {
    PREFERRED, STANDARD, SUBSTANDARD, DECLINED
}

enum class PaymentMethod {
    BANK_TRANSFER, CREDIT_CARD, CHECK, ACH
}

enum class PaymentStatus {
    PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class LoanStatus {
    ACTIVE, PAID_OFF, DEFAULTED
}

// Exceptions
class PolicyNotFoundException(message: String) : RuntimeException(message)
