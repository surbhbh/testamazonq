package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Service for processing insurance claims
 * Handles claim submission, review, approval, and payment
 */
@Service
class ClaimsProcessor {

    /**
     * Submits a new insurance claim
     */
    fun submitClaim(request: ClaimSubmissionRequest): Claim {
        validateClaimRequest(request)
        
        val claim = Claim(
            claimId = generateClaimId(),
            policyNumber = request.policyNumber,
            claimType = request.claimType,
            claimAmount = request.claimAmount,
            dateOfLoss = request.dateOfLoss,
            dateReported = LocalDate.now(),
            status = ClaimStatus.SUBMITTED,
            claimantInfo = request.claimantInfo,
            description = request.description
        )
        
        // Determine if investigation is required
        val requiresInvestigation = shouldInvestigate(claim)
        if (requiresInvestigation) {
            claim.investigationRequired = true
            claim.status = ClaimStatus.UNDER_INVESTIGATION
        }
        
        return saveClaim(claim)
    }

    /**
     * Reviews a submitted claim
     */
    fun reviewClaim(claimId: String, reviewerId: String, decision: ReviewDecision): ClaimReviewResult {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        val review = ClaimReview(
            reviewId = UUID.randomUUID().toString(),
            claimId = claimId,
            reviewerId = reviewerId,
            reviewDate = LocalDateTime.now(),
            decision = decision,
            comments = decision.comments
        )
        
        // Update claim status based on review decision
        claim.status = when (decision.type) {
            ReviewDecisionType.APPROVE -> ClaimStatus.APPROVED
            ReviewDecisionType.DENY -> ClaimStatus.DENIED
            ReviewDecisionType.INVESTIGATE -> ClaimStatus.UNDER_INVESTIGATION
            ReviewDecisionType.REQUEST_INFO -> ClaimStatus.PENDING_INFORMATION
        }
        
        claim.reviews.add(review)
        saveClaim(claim)
        
        return ClaimReviewResult(
            claimId = claimId,
            reviewId = review.reviewId,
            newStatus = claim.status,
            nextAction = determineNextAction(claim)
        )
    }

    /**
     * Processes claim payment
     */
    fun processClaimPayment(claimId: String, paymentMethod: PaymentMethod): ClaimPayment {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        if (claim.status != ClaimStatus.APPROVED) {
            throw IllegalStateException("Cannot process payment for non-approved claim")
        }
        
        val payment = ClaimPayment(
            paymentId = generatePaymentId(),
            claimId = claimId,
            amount = claim.claimAmount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        // Process payment through payment gateway
        val paymentResult = processPaymentThroughGateway(payment)
        payment.status = paymentResult.status
        payment.confirmationNumber = paymentResult.confirmationNumber
        
        if (payment.status == PaymentStatus.COMPLETED) {
            claim.status = ClaimStatus.PAID
            claim.paidAmount = payment.amount
            claim.paidDate = payment.paymentDate
        }
        
        saveClaim(claim)
        savePayment(payment)
        
        return payment
    }

    /**
     * Calculates claim reserves for financial reporting
     */
    fun calculateClaimReserves(claimType: ClaimType, claimAmount: BigDecimal): BigDecimal {
        val reservePercentage = when (claimType) {
            ClaimType.DEATH_BENEFIT -> BigDecimal("1.00") // 100% reserve
            ClaimType.DISABILITY -> BigDecimal("0.85") // 85% reserve
            ClaimType.CRITICAL_ILLNESS -> BigDecimal("0.90") // 90% reserve
            ClaimType.ACCIDENT -> BigDecimal("0.75") // 75% reserve
            ClaimType.SURRENDER -> BigDecimal("1.00") // 100% reserve
        }
        
        return claimAmount.multiply(reservePercentage).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates claim analytics report
     */
    fun generateClaimAnalytics(startDate: LocalDate, endDate: LocalDate): ClaimAnalytics {
        val claims = findClaimsByDateRange(startDate, endDate)
        
        val totalClaims = claims.size
        val totalClaimAmount = claims.sumOf { it.claimAmount }
        val averageClaimAmount = if (totalClaims > 0) totalClaimAmount.divide(BigDecimal(totalClaims), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val claimsByType = claims.groupBy { it.claimType }
            .mapValues { (_, claimList) -> claimList.size }
        
        val claimsByStatus = claims.groupBy { it.status }
            .mapValues { (_, claimList) -> claimList.size }
        
        val averageProcessingTime = calculateAverageProcessingTime(claims)
        
        return ClaimAnalytics(
            reportPeriod = DateRange(startDate, endDate),
            totalClaims = totalClaims,
            totalClaimAmount = totalClaimAmount,
            averageClaimAmount = averageClaimAmount,
            claimsByType = claimsByType,
            claimsByStatus = claimsByStatus,
            averageProcessingTime = averageProcessingTime,
            fraudDetectionStats = calculateFraudStats(claims)
        )
    }

    /**
     * Detects potential fraud indicators
     */
    fun detectFraudIndicators(claim: Claim): List<FraudIndicator> {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check for large claim amounts
        if (claim.claimAmount > BigDecimal("100000")) {
            indicators.add(FraudIndicator.LARGE_CLAIM_AMOUNT)
        }
        
        // Check for claims soon after policy issue
        val policy = findPolicyByNumber(claim.policyNumber)
        if (policy != null) {
            val daysSinceIssue = java.time.Period.between(policy.issueDate, claim.dateOfLoss).days
            if (daysSinceIssue < 730) { // Less than 2 years
                indicators.add(FraudIndicator.EARLY_CLAIM)
            }
        }
        
        // Check for multiple claims from same claimant
        val claimantClaims = findClaimsByClaimant(claim.claimantInfo.name)
        if (claimantClaims.size > 2) {
            indicators.add(FraudIndicator.MULTIPLE_CLAIMS)
        }
        
        // Check for weekend/holiday claims
        val dayOfWeek = claim.dateOfLoss.dayOfWeek
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            indicators.add(FraudIndicator.WEEKEND_INCIDENT)
        }
        
        return indicators
    }

    /**
     * Processes claim investigation results
     */
    fun processInvestigationResults(claimId: String, investigationResult: InvestigationResult): Claim {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        claim.investigationResult = investigationResult
        claim.investigationCompleted = true
        
        // Update claim status based on investigation findings
        claim.status = when (investigationResult.recommendation) {
            InvestigationRecommendation.APPROVE -> ClaimStatus.APPROVED
            InvestigationRecommendation.DENY -> ClaimStatus.DENIED
            InvestigationRecommendation.FURTHER_REVIEW -> ClaimStatus.UNDER_REVIEW
            InvestigationRecommendation.REFER_TO_SIU -> ClaimStatus.SIU_REFERRAL
        }
        
        return saveClaim(claim)
    }

    // Private helper methods
    private fun validateClaimRequest(request: ClaimSubmissionRequest) {
        require(request.policyNumber.isNotBlank()) { "Policy number is required" }
        require(request.claimAmount > BigDecimal.ZERO) { "Claim amount must be positive" }
        require(request.dateOfLoss <= LocalDate.now()) { "Date of loss cannot be in the future" }
        require(request.claimantInfo.name.isNotBlank()) { "Claimant name is required" }
    }

    private fun shouldInvestigate(claim: Claim): Boolean {
        // Investigation required for large claims or fraud indicators
        return claim.claimAmount > BigDecimal("50000") || 
               detectFraudIndicators(claim).isNotEmpty()
    }

    private fun determineNextAction(claim: Claim): String {
        return when (claim.status) {
            ClaimStatus.APPROVED -> "Process payment"
            ClaimStatus.DENIED -> "Send denial letter"
            ClaimStatus.UNDER_INVESTIGATION -> "Assign investigator"
            ClaimStatus.PENDING_INFORMATION -> "Request additional documentation"
            else -> "Continue processing"
        }
    }

    private fun calculateAverageProcessingTime(claims: List<Claim>): Double {
        val completedClaims = claims.filter { it.status in setOf(ClaimStatus.PAID, ClaimStatus.DENIED) }
        if (completedClaims.isEmpty()) return 0.0
        
        val totalDays = completedClaims.sumOf { claim ->
            val endDate = claim.paidDate ?: claim.lastModified.toLocalDate()
            java.time.Period.between(claim.dateReported, endDate).days
        }
        
        return totalDays.toDouble() / completedClaims.size
    }

    private fun calculateFraudStats(claims: List<Claim>): FraudDetectionStats {
        val suspiciousClaims = claims.count { detectFraudIndicators(it).isNotEmpty() }
        val confirmedFraud = claims.count { it.status == ClaimStatus.DENIED && it.investigationResult?.fraudConfirmed == true }
        
        return FraudDetectionStats(
            suspiciousClaims = suspiciousClaims,
            confirmedFraud = confirmedFraud,
            fraudRate = if (claims.isNotEmpty()) confirmedFraud.toDouble() / claims.size else 0.0
        )
    }

    private fun generateClaimId(): String = "CLM-${System.currentTimeMillis()}"
    private fun generatePaymentId(): String = "PAY-${System.currentTimeMillis()}"

    // Mock database operations
    private fun findClaimById(claimId: String): Claim? = null
    private fun findPolicyByNumber(policyNumber: String): Policy? = null
    private fun findClaimsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Claim> = emptyList()
    private fun findClaimsByClaimant(claimantName: String): List<Claim> = emptyList()
    private fun saveClaim(claim: Claim): Claim = claim
    private fun savePayment(payment: ClaimPayment): ClaimPayment = payment
    private fun processPaymentThroughGateway(payment: ClaimPayment): PaymentResult = 
        PaymentResult("PAY-${System.currentTimeMillis()}", PaymentStatus.COMPLETED, "CONF-${System.currentTimeMillis()}")
}

// Data classes and enums
data class ClaimSubmissionRequest(
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val claimantInfo: ClaimantInfo,
    val description: String
)

data class Claim(
    val claimId: String,
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val dateReported: LocalDate,
    var status: ClaimStatus,
    val claimantInfo: ClaimantInfo,
    val description: String,
    var investigationRequired: Boolean = false,
    var investigationCompleted: Boolean = false,
    var investigationResult: InvestigationResult? = null,
    val reviews: MutableList<ClaimReview> = mutableListOf(),
    var paidAmount: BigDecimal? = null,
    var paidDate: LocalDateTime? = null,
    val lastModified: LocalDateTime = LocalDateTime.now()
)

data class ClaimantInfo(
    val name: String,
    val relationship: String,
    val contactInfo: ContactInfo
)

data class ContactInfo(
    val phone: String,
    val email: String,
    val address: Address
)

data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String
)

data class ClaimReview(
    val reviewId: String,
    val claimId: String,
    val reviewerId: String,
    val reviewDate: LocalDateTime,
    val decision: ReviewDecision,
    val comments: String
)

data class ReviewDecision(
    val type: ReviewDecisionType,
    val comments: String
)

data class ClaimReviewResult(
    val claimId: String,
    val reviewId: String,
    val newStatus: ClaimStatus,
    val nextAction: String
)

data class ClaimPayment(
    val paymentId: String,
    val claimId: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    var status: PaymentStatus,
    var confirmationNumber: String? = null
)

data class ClaimAnalytics(
    val reportPeriod: DateRange,
    val totalClaims: Int,
    val totalClaimAmount: BigDecimal,
    val averageClaimAmount: BigDecimal,
    val claimsByType: Map<ClaimType, Int>,
    val claimsByStatus: Map<ClaimStatus, Int>,
    val averageProcessingTime: Double,
    val fraudDetectionStats: FraudDetectionStats
)

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class InvestigationResult(
    val investigatorId: String,
    val completionDate: LocalDateTime,
    val findings: String,
    val recommendation: InvestigationRecommendation,
    val fraudConfirmed: Boolean = false
)

data class FraudDetectionStats(
    val suspiciousClaims: Int,
    val confirmedFraud: Int,
    val fraudRate: Double
)

enum class ClaimType {
    DEATH_BENEFIT, DISABILITY, CRITICAL_ILLNESS, ACCIDENT, SURRENDER
}

enum class ClaimStatus {
    SUBMITTED, UNDER_REVIEW, UNDER_INVESTIGATION, PENDING_INFORMATION, 
    APPROVED, DENIED, PAID, SIU_REFERRAL
}

enum class ReviewDecisionType {
    APPROVE, DENY, INVESTIGATE, REQUEST_INFO
}

enum class InvestigationRecommendation {
    APPROVE, DENY, FURTHER_REVIEW, REFER_TO_SIU
}

enum class FraudIndicator {
    LARGE_CLAIM_AMOUNT, EARLY_CLAIM, MULTIPLE_CLAIMS, WEEKEND_INCIDENT, 
    INCONSISTENT_STATEMENTS, SUSPICIOUS_DOCUMENTATION
}

class ClaimNotFoundException(message: String) : RuntimeException(message)package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Service for processing insurance claims
 * Handles claim submission, review, approval, and payment
 */
@Service
class ClaimsProcessor {

    /**
     * Submits a new insurance claim
     */
    fun submitClaim(request: ClaimSubmissionRequest): Claim {
        validateClaimRequest(request)
        
        val claim = Claim(
            claimId = generateClaimId(),
            policyNumber = request.policyNumber,
            claimType = request.claimType,
            claimAmount = request.claimAmount,
            dateOfLoss = request.dateOfLoss,
            dateReported = LocalDate.now(),
            status = ClaimStatus.SUBMITTED,
            claimantInfo = request.claimantInfo,
            description = request.description
        )
        
        // Determine if investigation is required
        val requiresInvestigation = shouldInvestigate(claim)
        if (requiresInvestigation) {
            claim.investigationRequired = true
            claim.status = ClaimStatus.UNDER_INVESTIGATION
        }
        
        return saveClaim(claim)
    }

    /**
     * Reviews a submitted claim
     */
    fun reviewClaim(claimId: String, reviewerId: String, decision: ReviewDecision): ClaimReviewResult {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        val review = ClaimReview(
            reviewId = UUID.randomUUID().toString(),
            claimId = claimId,
            reviewerId = reviewerId,
            reviewDate = LocalDateTime.now(),
            decision = decision,
            comments = decision.comments
        )
        
        // Update claim status based on review decision
        claim.status = when (decision.type) {
            ReviewDecisionType.APPROVE -> ClaimStatus.APPROVED
            ReviewDecisionType.DENY -> ClaimStatus.DENIED
            ReviewDecisionType.INVESTIGATE -> ClaimStatus.UNDER_INVESTIGATION
            ReviewDecisionType.REQUEST_INFO -> ClaimStatus.PENDING_INFORMATION
        }
        
        claim.reviews.add(review)
        saveClaim(claim)
        
        return ClaimReviewResult(
            claimId = claimId,
            reviewId = review.reviewId,
            newStatus = claim.status,
            nextAction = determineNextAction(claim)
        )
    }

    /**
     * Processes claim payment
     */
    fun processClaimPayment(claimId: String, paymentMethod: PaymentMethod): ClaimPayment {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        if (claim.status != ClaimStatus.APPROVED) {
            throw IllegalStateException("Cannot process payment for non-approved claim")
        }
        
        val payment = ClaimPayment(
            paymentId = generatePaymentId(),
            claimId = claimId,
            amount = claim.claimAmount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        // Process payment through payment gateway
        val paymentResult = processPaymentThroughGateway(payment)
        payment.status = paymentResult.status
        payment.confirmationNumber = paymentResult.confirmationNumber
        
        if (payment.status == PaymentStatus.COMPLETED) {
            claim.status = ClaimStatus.PAID
            claim.paidAmount = payment.amount
            claim.paidDate = payment.paymentDate
        }
        
        saveClaim(claim)
        savePayment(payment)
        
        return payment
    }

    /**
     * Calculates claim reserves for financial reporting
     */
    fun calculateClaimReserves(claimType: ClaimType, claimAmount: BigDecimal): BigDecimal {
        val reservePercentage = when (claimType) {
            ClaimType.DEATH_BENEFIT -> BigDecimal("1.00") // 100% reserve
            ClaimType.DISABILITY -> BigDecimal("0.85") // 85% reserve
            ClaimType.CRITICAL_ILLNESS -> BigDecimal("0.90") // 90% reserve
            ClaimType.ACCIDENT -> BigDecimal("0.75") // 75% reserve
            ClaimType.SURRENDER -> BigDecimal("1.00") // 100% reserve
        }
        
        return claimAmount.multiply(reservePercentage).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates claim analytics report
     */
    fun generateClaimAnalytics(startDate: LocalDate, endDate: LocalDate): ClaimAnalytics {
        val claims = findClaimsByDateRange(startDate, endDate)
        
        val totalClaims = claims.size
        val totalClaimAmount = claims.sumOf { it.claimAmount }
        val averageClaimAmount = if (totalClaims > 0) totalClaimAmount.divide(BigDecimal(totalClaims), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val claimsByType = claims.groupBy { it.claimType }
            .mapValues { (_, claimList) -> claimList.size }
        
        val claimsByStatus = claims.groupBy { it.status }
            .mapValues { (_, claimList) -> claimList.size }
        
        val averageProcessingTime = calculateAverageProcessingTime(claims)
        
        return ClaimAnalytics(
            reportPeriod = DateRange(startDate, endDate),
            totalClaims = totalClaims,
            totalClaimAmount = totalClaimAmount,
            averageClaimAmount = averageClaimAmount,
            claimsByType = claimsByType,
            claimsByStatus = claimsByStatus,
            averageProcessingTime = averageProcessingTime,
            fraudDetectionStats = calculateFraudStats(claims)
        )
    }

    /**
     * Detects potential fraud indicators
     */
    fun detectFraudIndicators(claim: Claim): List<FraudIndicator> {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check for large claim amounts
        if (claim.claimAmount > BigDecimal("100000")) {
            indicators.add(FraudIndicator.LARGE_CLAIM_AMOUNT)
        }
        
        // Check for claims soon after policy issue
        val policy = findPolicyByNumber(claim.policyNumber)
        if (policy != null) {
            val daysSinceIssue = java.time.Period.between(policy.issueDate, claim.dateOfLoss).days
            if (daysSinceIssue < 730) { // Less than 2 years
                indicators.add(FraudIndicator.EARLY_CLAIM)
            }
        }
        
        // Check for multiple claims from same claimant
        val claimantClaims = findClaimsByClaimant(claim.claimantInfo.name)
        if (claimantClaims.size > 2) {
            indicators.add(FraudIndicator.MULTIPLE_CLAIMS)
        }
        
        // Check for weekend/holiday claims
        val dayOfWeek = claim.dateOfLoss.dayOfWeek
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            indicators.add(FraudIndicator.WEEKEND_INCIDENT)
        }
        
        return indicators
    }

    /**
     * Processes claim investigation results
     */
    fun processInvestigationResults(claimId: String, investigationResult: InvestigationResult): Claim {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        claim.investigationResult = investigationResult
        claim.investigationCompleted = true
        
        // Update claim status based on investigation findings
        claim.status = when (investigationResult.recommendation) {
            InvestigationRecommendation.APPROVE -> ClaimStatus.APPROVED
            InvestigationRecommendation.DENY -> ClaimStatus.DENIED
            InvestigationRecommendation.FURTHER_REVIEW -> ClaimStatus.UNDER_REVIEW
            InvestigationRecommendation.REFER_TO_SIU -> ClaimStatus.SIU_REFERRAL
        }
        
        return saveClaim(claim)
    }

    // Private helper methods
    private fun validateClaimRequest(request: ClaimSubmissionRequest) {
        require(request.policyNumber.isNotBlank()) { "Policy number is required" }
        require(request.claimAmount > BigDecimal.ZERO) { "Claim amount must be positive" }
        require(request.dateOfLoss <= LocalDate.now()) { "Date of loss cannot be in the future" }
        require(request.claimantInfo.name.isNotBlank()) { "Claimant name is required" }
    }

    private fun shouldInvestigate(claim: Claim): Boolean {
        // Investigation required for large claims or fraud indicators
        return claim.claimAmount > BigDecimal("50000") || 
               detectFraudIndicators(claim).isNotEmpty()
    }

    private fun determineNextAction(claim: Claim): String {
        return when (claim.status) {
            ClaimStatus.APPROVED -> "Process payment"
            ClaimStatus.DENIED -> "Send denial letter"
            ClaimStatus.UNDER_INVESTIGATION -> "Assign investigator"
            ClaimStatus.PENDING_INFORMATION -> "Request additional documentation"
            else -> "Continue processing"
        }
    }

    private fun calculateAverageProcessingTime(claims: List<Claim>): Double {
        val completedClaims = claims.filter { it.status in setOf(ClaimStatus.PAID, ClaimStatus.DENIED) }
        if (completedClaims.isEmpty()) return 0.0
        
        val totalDays = completedClaims.sumOf { claim ->
            val endDate = claim.paidDate ?: claim.lastModified.toLocalDate()
            java.time.Period.between(claim.dateReported, endDate).days
        }
        
        return totalDays.toDouble() / completedClaims.size
    }

    private fun calculateFraudStats(claims: List<Claim>): FraudDetectionStats {
        val suspiciousClaims = claims.count { detectFraudIndicators(it).isNotEmpty() }
        val confirmedFraud = claims.count { it.status == ClaimStatus.DENIED && it.investigationResult?.fraudConfirmed == true }
        
        return FraudDetectionStats(
            suspiciousClaims = suspiciousClaims,
            confirmedFraud = confirmedFraud,
            fraudRate = if (claims.isNotEmpty()) confirmedFraud.toDouble() / claims.size else 0.0
        )
    }

    private fun generateClaimId(): String = "CLM-${System.currentTimeMillis()}"
    private fun generatePaymentId(): String = "PAY-${System.currentTimeMillis()}"

    // Mock database operations
    private fun findClaimById(claimId: String): Claim? = null
    private fun findPolicyByNumber(policyNumber: String): Policy? = null
    private fun findClaimsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Claim> = emptyList()
    private fun findClaimsByClaimant(claimantName: String): List<Claim> = emptyList()
    private fun saveClaim(claim: Claim): Claim = claim
    private fun savePayment(payment: ClaimPayment): ClaimPayment = payment
    private fun processPaymentThroughGateway(payment: ClaimPayment): PaymentResult = 
        PaymentResult("PAY-${System.currentTimeMillis()}", PaymentStatus.COMPLETED, "CONF-${System.currentTimeMillis()}")
}

// Data classes and enums
data class ClaimSubmissionRequest(
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val claimantInfo: ClaimantInfo,
    val description: String
)

data class Claim(
    val claimId: String,
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val dateReported: LocalDate,
    var status: ClaimStatus,
    val claimantInfo: ClaimantInfo,
    val description: String,
    var investigationRequired: Boolean = false,
    var investigationCompleted: Boolean = false,
    var investigationResult: InvestigationResult? = null,
    val reviews: MutableList<ClaimReview> = mutableListOf(),
    var paidAmount: BigDecimal? = null,
    var paidDate: LocalDateTime? = null,
    val lastModified: LocalDateTime = LocalDateTime.now()
)

data class ClaimantInfo(
    val name: String,
    val relationship: String,
    val contactInfo: ContactInfo
)

data class ContactInfo(
    val phone: String,
    val email: String,
    val address: Address
)

data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String
)

data class ClaimReview(
    val reviewId: String,
    val claimId: String,
    val reviewerId: String,
    val reviewDate: LocalDateTime,
    val decision: ReviewDecision,
    val comments: String
)

data class ReviewDecision(
    val type: ReviewDecisionType,
    val comments: String
)

data class ClaimReviewResult(
    val claimId: String,
    val reviewId: String,
    val newStatus: ClaimStatus,
    val nextAction: String
)

data class ClaimPayment(
    val paymentId: String,
    val claimId: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    var status: PaymentStatus,
    var confirmationNumber: String? = null
)

data class ClaimAnalytics(
    val reportPeriod: DateRange,
    val totalClaims: Int,
    val totalClaimAmount: BigDecimal,
    val averageClaimAmount: BigDecimal,
    val claimsByType: Map<ClaimType, Int>,
    val claimsByStatus: Map<ClaimStatus, Int>,
    val averageProcessingTime: Double,
    val fraudDetectionStats: FraudDetectionStats
)

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class InvestigationResult(
    val investigatorId: String,
    val completionDate: LocalDateTime,
    val findings: String,
    val recommendation: InvestigationRecommendation,
    val fraudConfirmed: Boolean = false
)

data class FraudDetectionStats(
    val suspiciousClaims: Int,
    val confirmedFraud: Int,
    val fraudRate: Double
)

enum class ClaimType {
    DEATH_BENEFIT, DISABILITY, CRITICAL_ILLNESS, ACCIDENT, SURRENDER
}

enum class ClaimStatus {
    SUBMITTED, UNDER_REVIEW, UNDER_INVESTIGATION, PENDING_INFORMATION, 
    APPROVED, DENIED, PAID, SIU_REFERRAL
}

enum class ReviewDecisionType {
    APPROVE, DENY, INVESTIGATE, REQUEST_INFO
}

enum class InvestigationRecommendation {
    APPROVE, DENY, FURTHER_REVIEW, REFER_TO_SIU
}

enum class FraudIndicator {
    LARGE_CLAIM_AMOUNT, EARLY_CLAIM, MULTIPLE_CLAIMS, WEEKEND_INCIDENT, 
    INCONSISTENT_STATEMENTS, SUSPICIOUS_DOCUMENTATION
}

class ClaimNotFoundException(message: String) : RuntimeException(message)package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Service for processing insurance claims
 * Handles claim submission, review, approval, and payment
 */
@Service
class ClaimsProcessor {

    /**
     * Submits a new insurance claim
     */
    fun submitClaim(request: ClaimSubmissionRequest): Claim {
        validateClaimRequest(request)
        
        val claim = Claim(
            claimId = generateClaimId(),
            policyNumber = request.policyNumber,
            claimType = request.claimType,
            claimAmount = request.claimAmount,
            dateOfLoss = request.dateOfLoss,
            dateReported = LocalDate.now(),
            status = ClaimStatus.SUBMITTED,
            claimantInfo = request.claimantInfo,
            description = request.description
        )
        
        // Determine if investigation is required
        val requiresInvestigation = shouldInvestigate(claim)
        if (requiresInvestigation) {
            claim.investigationRequired = true
            claim.status = ClaimStatus.UNDER_INVESTIGATION
        }
        
        return saveClaim(claim)
    }

    /**
     * Reviews a submitted claim
     */
    fun reviewClaim(claimId: String, reviewerId: String, decision: ReviewDecision): ClaimReviewResult {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        val review = ClaimReview(
            reviewId = UUID.randomUUID().toString(),
            claimId = claimId,
            reviewerId = reviewerId,
            reviewDate = LocalDateTime.now(),
            decision = decision,
            comments = decision.comments
        )
        
        // Update claim status based on review decision
        claim.status = when (decision.type) {
            ReviewDecisionType.APPROVE -> ClaimStatus.APPROVED
            ReviewDecisionType.DENY -> ClaimStatus.DENIED
            ReviewDecisionType.INVESTIGATE -> ClaimStatus.UNDER_INVESTIGATION
            ReviewDecisionType.REQUEST_INFO -> ClaimStatus.PENDING_INFORMATION
        }
        
        claim.reviews.add(review)
        saveClaim(claim)
        
        return ClaimReviewResult(
            claimId = claimId,
            reviewId = review.reviewId,
            newStatus = claim.status,
            nextAction = determineNextAction(claim)
        )
    }

    /**
     * Processes claim payment
     */
    fun processClaimPayment(claimId: String, paymentMethod: PaymentMethod): ClaimPayment {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        if (claim.status != ClaimStatus.APPROVED) {
            throw IllegalStateException("Cannot process payment for non-approved claim")
        }
        
        val payment = ClaimPayment(
            paymentId = generatePaymentId(),
            claimId = claimId,
            amount = claim.claimAmount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        // Process payment through payment gateway
        val paymentResult = processPaymentThroughGateway(payment)
        payment.status = paymentResult.status
        payment.confirmationNumber = paymentResult.confirmationNumber
        
        if (payment.status == PaymentStatus.COMPLETED) {
            claim.status = ClaimStatus.PAID
            claim.paidAmount = payment.amount
            claim.paidDate = payment.paymentDate
        }
        
        saveClaim(claim)
        savePayment(payment)
        
        return payment
    }

    /**
     * Calculates claim reserves for financial reporting
     */
    fun calculateClaimReserves(claimType: ClaimType, claimAmount: BigDecimal): BigDecimal {
        val reservePercentage = when (claimType) {
            ClaimType.DEATH_BENEFIT -> BigDecimal("1.00") // 100% reserve
            ClaimType.DISABILITY -> BigDecimal("0.85") // 85% reserve
            ClaimType.CRITICAL_ILLNESS -> BigDecimal("0.90") // 90% reserve
            ClaimType.ACCIDENT -> BigDecimal("0.75") // 75% reserve
            ClaimType.SURRENDER -> BigDecimal("1.00") // 100% reserve
        }
        
        return claimAmount.multiply(reservePercentage).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates claim analytics report
     */
    fun generateClaimAnalytics(startDate: LocalDate, endDate: LocalDate): ClaimAnalytics {
        val claims = findClaimsByDateRange(startDate, endDate)
        
        val totalClaims = claims.size
        val totalClaimAmount = claims.sumOf { it.claimAmount }
        val averageClaimAmount = if (totalClaims > 0) totalClaimAmount.divide(BigDecimal(totalClaims), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val claimsByType = claims.groupBy { it.claimType }
            .mapValues { (_, claimList) -> claimList.size }
        
        val claimsByStatus = claims.groupBy { it.status }
            .mapValues { (_, claimList) -> claimList.size }
        
        val averageProcessingTime = calculateAverageProcessingTime(claims)
        
        return ClaimAnalytics(
            reportPeriod = DateRange(startDate, endDate),
            totalClaims = totalClaims,
            totalClaimAmount = totalClaimAmount,
            averageClaimAmount = averageClaimAmount,
            claimsByType = claimsByType,
            claimsByStatus = claimsByStatus,
            averageProcessingTime = averageProcessingTime,
            fraudDetectionStats = calculateFraudStats(claims)
        )
    }

    /**
     * Detects potential fraud indicators
     */
    fun detectFraudIndicators(claim: Claim): List<FraudIndicator> {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check for large claim amounts
        if (claim.claimAmount > BigDecimal("100000")) {
            indicators.add(FraudIndicator.LARGE_CLAIM_AMOUNT)
        }
        
        // Check for claims soon after policy issue
        val policy = findPolicyByNumber(claim.policyNumber)
        if (policy != null) {
            val daysSinceIssue = java.time.Period.between(policy.issueDate, claim.dateOfLoss).days
            if (daysSinceIssue < 730) { // Less than 2 years
                indicators.add(FraudIndicator.EARLY_CLAIM)
            }
        }
        
        // Check for multiple claims from same claimant
        val claimantClaims = findClaimsByClaimant(claim.claimantInfo.name)
        if (claimantClaims.size > 2) {
            indicators.add(FraudIndicator.MULTIPLE_CLAIMS)
        }
        
        // Check for weekend/holiday claims
        val dayOfWeek = claim.dateOfLoss.dayOfWeek
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            indicators.add(FraudIndicator.WEEKEND_INCIDENT)
        }
        
        return indicators
    }

    /**
     * Processes claim investigation results
     */
    fun processInvestigationResults(claimId: String, investigationResult: InvestigationResult): Claim {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        claim.investigationResult = investigationResult
        claim.investigationCompleted = true
        
        // Update claim status based on investigation findings
        claim.status = when (investigationResult.recommendation) {
            InvestigationRecommendation.APPROVE -> ClaimStatus.APPROVED
            InvestigationRecommendation.DENY -> ClaimStatus.DENIED
            InvestigationRecommendation.FURTHER_REVIEW -> ClaimStatus.UNDER_REVIEW
            InvestigationRecommendation.REFER_TO_SIU -> ClaimStatus.SIU_REFERRAL
        }
        
        return saveClaim(claim)
    }

    // Private helper methods
    private fun validateClaimRequest(request: ClaimSubmissionRequest) {
        require(request.policyNumber.isNotBlank()) { "Policy number is required" }
        require(request.claimAmount > BigDecimal.ZERO) { "Claim amount must be positive" }
        require(request.dateOfLoss <= LocalDate.now()) { "Date of loss cannot be in the future" }
        require(request.claimantInfo.name.isNotBlank()) { "Claimant name is required" }
    }

    private fun shouldInvestigate(claim: Claim): Boolean {
        // Investigation required for large claims or fraud indicators
        return claim.claimAmount > BigDecimal("50000") || 
               detectFraudIndicators(claim).isNotEmpty()
    }

    private fun determineNextAction(claim: Claim): String {
        return when (claim.status) {
            ClaimStatus.APPROVED -> "Process payment"
            ClaimStatus.DENIED -> "Send denial letter"
            ClaimStatus.UNDER_INVESTIGATION -> "Assign investigator"
            ClaimStatus.PENDING_INFORMATION -> "Request additional documentation"
            else -> "Continue processing"
        }
    }

    private fun calculateAverageProcessingTime(claims: List<Claim>): Double {
        val completedClaims = claims.filter { it.status in setOf(ClaimStatus.PAID, ClaimStatus.DENIED) }
        if (completedClaims.isEmpty()) return 0.0
        
        val totalDays = completedClaims.sumOf { claim ->
            val endDate = claim.paidDate ?: claim.lastModified.toLocalDate()
            java.time.Period.between(claim.dateReported, endDate).days
        }
        
        return totalDays.toDouble() / completedClaims.size
    }

    private fun calculateFraudStats(claims: List<Claim>): FraudDetectionStats {
        val suspiciousClaims = claims.count { detectFraudIndicators(it).isNotEmpty() }
        val confirmedFraud = claims.count { it.status == ClaimStatus.DENIED && it.investigationResult?.fraudConfirmed == true }
        
        return FraudDetectionStats(
            suspiciousClaims = suspiciousClaims,
            confirmedFraud = confirmedFraud,
            fraudRate = if (claims.isNotEmpty()) confirmedFraud.toDouble() / claims.size else 0.0
        )
    }

    private fun generateClaimId(): String = "CLM-${System.currentTimeMillis()}"
    private fun generatePaymentId(): String = "PAY-${System.currentTimeMillis()}"

    // Mock database operations
    private fun findClaimById(claimId: String): Claim? = null
    private fun findPolicyByNumber(policyNumber: String): Policy? = null
    private fun findClaimsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Claim> = emptyList()
    private fun findClaimsByClaimant(claimantName: String): List<Claim> = emptyList()
    private fun saveClaim(claim: Claim): Claim = claim
    private fun savePayment(payment: ClaimPayment): ClaimPayment = payment
    private fun processPaymentThroughGateway(payment: ClaimPayment): PaymentResult = 
        PaymentResult("PAY-${System.currentTimeMillis()}", PaymentStatus.COMPLETED, "CONF-${System.currentTimeMillis()}")
}

// Data classes and enums
data class ClaimSubmissionRequest(
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val claimantInfo: ClaimantInfo,
    val description: String
)

data class Claim(
    val claimId: String,
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val dateReported: LocalDate,
    var status: ClaimStatus,
    val claimantInfo: ClaimantInfo,
    val description: String,
    var investigationRequired: Boolean = false,
    var investigationCompleted: Boolean = false,
    var investigationResult: InvestigationResult? = null,
    val reviews: MutableList<ClaimReview> = mutableListOf(),
    var paidAmount: BigDecimal? = null,
    var paidDate: LocalDateTime? = null,
    val lastModified: LocalDateTime = LocalDateTime.now()
)

data class ClaimantInfo(
    val name: String,
    val relationship: String,
    val contactInfo: ContactInfo
)

data class ContactInfo(
    val phone: String,
    val email: String,
    val address: Address
)

data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String
)

data class ClaimReview(
    val reviewId: String,
    val claimId: String,
    val reviewerId: String,
    val reviewDate: LocalDateTime,
    val decision: ReviewDecision,
    val comments: String
)

data class ReviewDecision(
    val type: ReviewDecisionType,
    val comments: String
)

data class ClaimReviewResult(
    val claimId: String,
    val reviewId: String,
    val newStatus: ClaimStatus,
    val nextAction: String
)

data class ClaimPayment(
    val paymentId: String,
    val claimId: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    var status: PaymentStatus,
    var confirmationNumber: String? = null
)

data class ClaimAnalytics(
    val reportPeriod: DateRange,
    val totalClaims: Int,
    val totalClaimAmount: BigDecimal,
    val averageClaimAmount: BigDecimal,
    val claimsByType: Map<ClaimType, Int>,
    val claimsByStatus: Map<ClaimStatus, Int>,
    val averageProcessingTime: Double,
    val fraudDetectionStats: FraudDetectionStats
)

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class InvestigationResult(
    val investigatorId: String,
    val completionDate: LocalDateTime,
    val findings: String,
    val recommendation: InvestigationRecommendation,
    val fraudConfirmed: Boolean = false
)

data class FraudDetectionStats(
    val suspiciousClaims: Int,
    val confirmedFraud: Int,
    val fraudRate: Double
)

enum class ClaimType {
    DEATH_BENEFIT, DISABILITY, CRITICAL_ILLNESS, ACCIDENT, SURRENDER
}

enum class ClaimStatus {
    SUBMITTED, UNDER_REVIEW, UNDER_INVESTIGATION, PENDING_INFORMATION, 
    APPROVED, DENIED, PAID, SIU_REFERRAL
}

enum class ReviewDecisionType {
    APPROVE, DENY, INVESTIGATE, REQUEST_INFO
}

enum class InvestigationRecommendation {
    APPROVE, DENY, FURTHER_REVIEW, REFER_TO_SIU
}

enum class FraudIndicator {
    LARGE_CLAIM_AMOUNT, EARLY_CLAIM, MULTIPLE_CLAIMS, WEEKEND_INCIDENT, 
    INCONSISTENT_STATEMENTS, SUSPICIOUS_DOCUMENTATION
}

class ClaimNotFoundException(message: String) : RuntimeException(message)package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Service for processing insurance claims
 * Handles claim submission, review, approval, and payment
 */
@Service
class ClaimsProcessor {

    /**
     * Submits a new insurance claim
     */
    fun submitClaim(request: ClaimSubmissionRequest): Claim {
        validateClaimRequest(request)
        
        val claim = Claim(
            claimId = generateClaimId(),
            policyNumber = request.policyNumber,
            claimType = request.claimType,
            claimAmount = request.claimAmount,
            dateOfLoss = request.dateOfLoss,
            dateReported = LocalDate.now(),
            status = ClaimStatus.SUBMITTED,
            claimantInfo = request.claimantInfo,
            description = request.description
        )
        
        // Determine if investigation is required
        val requiresInvestigation = shouldInvestigate(claim)
        if (requiresInvestigation) {
            claim.investigationRequired = true
            claim.status = ClaimStatus.UNDER_INVESTIGATION
        }
        
        return saveClaim(claim)
    }

    /**
     * Reviews a submitted claim
     */
    fun reviewClaim(claimId: String, reviewerId: String, decision: ReviewDecision): ClaimReviewResult {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        val review = ClaimReview(
            reviewId = UUID.randomUUID().toString(),
            claimId = claimId,
            reviewerId = reviewerId,
            reviewDate = LocalDateTime.now(),
            decision = decision,
            comments = decision.comments
        )
        
        // Update claim status based on review decision
        claim.status = when (decision.type) {
            ReviewDecisionType.APPROVE -> ClaimStatus.APPROVED
            ReviewDecisionType.DENY -> ClaimStatus.DENIED
            ReviewDecisionType.INVESTIGATE -> ClaimStatus.UNDER_INVESTIGATION
            ReviewDecisionType.REQUEST_INFO -> ClaimStatus.PENDING_INFORMATION
        }
        
        claim.reviews.add(review)
        saveClaim(claim)
        
        return ClaimReviewResult(
            claimId = claimId,
            reviewId = review.reviewId,
            newStatus = claim.status,
            nextAction = determineNextAction(claim)
        )
    }

    /**
     * Processes claim payment
     */
    fun processClaimPayment(claimId: String, paymentMethod: PaymentMethod): ClaimPayment {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        if (claim.status != ClaimStatus.APPROVED) {
            throw IllegalStateException("Cannot process payment for non-approved claim")
        }
        
        val payment = ClaimPayment(
            paymentId = generatePaymentId(),
            claimId = claimId,
            amount = claim.claimAmount,
            paymentMethod = paymentMethod,
            paymentDate = LocalDateTime.now(),
            status = PaymentStatus.PROCESSING
        )
        
        // Process payment through payment gateway
        val paymentResult = processPaymentThroughGateway(payment)
        payment.status = paymentResult.status
        payment.confirmationNumber = paymentResult.confirmationNumber
        
        if (payment.status == PaymentStatus.COMPLETED) {
            claim.status = ClaimStatus.PAID
            claim.paidAmount = payment.amount
            claim.paidDate = payment.paymentDate
        }
        
        saveClaim(claim)
        savePayment(payment)
        
        return payment
    }

    /**
     * Calculates claim reserves for financial reporting
     */
    fun calculateClaimReserves(claimType: ClaimType, claimAmount: BigDecimal): BigDecimal {
        val reservePercentage = when (claimType) {
            ClaimType.DEATH_BENEFIT -> BigDecimal("1.00") // 100% reserve
            ClaimType.DISABILITY -> BigDecimal("0.85") // 85% reserve
            ClaimType.CRITICAL_ILLNESS -> BigDecimal("0.90") // 90% reserve
            ClaimType.ACCIDENT -> BigDecimal("0.75") // 75% reserve
            ClaimType.SURRENDER -> BigDecimal("1.00") // 100% reserve
        }
        
        return claimAmount.multiply(reservePercentage).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Generates claim analytics report
     */
    fun generateClaimAnalytics(startDate: LocalDate, endDate: LocalDate): ClaimAnalytics {
        val claims = findClaimsByDateRange(startDate, endDate)
        
        val totalClaims = claims.size
        val totalClaimAmount = claims.sumOf { it.claimAmount }
        val averageClaimAmount = if (totalClaims > 0) totalClaimAmount.divide(BigDecimal(totalClaims), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val claimsByType = claims.groupBy { it.claimType }
            .mapValues { (_, claimList) -> claimList.size }
        
        val claimsByStatus = claims.groupBy { it.status }
            .mapValues { (_, claimList) -> claimList.size }
        
        val averageProcessingTime = calculateAverageProcessingTime(claims)
        
        return ClaimAnalytics(
            reportPeriod = DateRange(startDate, endDate),
            totalClaims = totalClaims,
            totalClaimAmount = totalClaimAmount,
            averageClaimAmount = averageClaimAmount,
            claimsByType = claimsByType,
            claimsByStatus = claimsByStatus,
            averageProcessingTime = averageProcessingTime,
            fraudDetectionStats = calculateFraudStats(claims)
        )
    }

    /**
     * Detects potential fraud indicators
     */
    fun detectFraudIndicators(claim: Claim): List<FraudIndicator> {
        val indicators = mutableListOf<FraudIndicator>()
        
        // Check for large claim amounts
        if (claim.claimAmount > BigDecimal("100000")) {
            indicators.add(FraudIndicator.LARGE_CLAIM_AMOUNT)
        }
        
        // Check for claims soon after policy issue
        val policy = findPolicyByNumber(claim.policyNumber)
        if (policy != null) {
            val daysSinceIssue = java.time.Period.between(policy.issueDate, claim.dateOfLoss).days
            if (daysSinceIssue < 730) { // Less than 2 years
                indicators.add(FraudIndicator.EARLY_CLAIM)
            }
        }
        
        // Check for multiple claims from same claimant
        val claimantClaims = findClaimsByClaimant(claim.claimantInfo.name)
        if (claimantClaims.size > 2) {
            indicators.add(FraudIndicator.MULTIPLE_CLAIMS)
        }
        
        // Check for weekend/holiday claims
        val dayOfWeek = claim.dateOfLoss.dayOfWeek
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            indicators.add(FraudIndicator.WEEKEND_INCIDENT)
        }
        
        return indicators
    }

    /**
     * Processes claim investigation results
     */
    fun processInvestigationResults(claimId: String, investigationResult: InvestigationResult): Claim {
        val claim = findClaimById(claimId)
            ?: throw ClaimNotFoundException("Claim not found: $claimId")
        
        claim.investigationResult = investigationResult
        claim.investigationCompleted = true
        
        // Update claim status based on investigation findings
        claim.status = when (investigationResult.recommendation) {
            InvestigationRecommendation.APPROVE -> ClaimStatus.APPROVED
            InvestigationRecommendation.DENY -> ClaimStatus.DENIED
            InvestigationRecommendation.FURTHER_REVIEW -> ClaimStatus.UNDER_REVIEW
            InvestigationRecommendation.REFER_TO_SIU -> ClaimStatus.SIU_REFERRAL
        }
        
        return saveClaim(claim)
    }

    // Private helper methods
    private fun validateClaimRequest(request: ClaimSubmissionRequest) {
        require(request.policyNumber.isNotBlank()) { "Policy number is required" }
        require(request.claimAmount > BigDecimal.ZERO) { "Claim amount must be positive" }
        require(request.dateOfLoss <= LocalDate.now()) { "Date of loss cannot be in the future" }
        require(request.claimantInfo.name.isNotBlank()) { "Claimant name is required" }
    }

    private fun shouldInvestigate(claim: Claim): Boolean {
        // Investigation required for large claims or fraud indicators
        return claim.claimAmount > BigDecimal("50000") || 
               detectFraudIndicators(claim).isNotEmpty()
    }

    private fun determineNextAction(claim: Claim): String {
        return when (claim.status) {
            ClaimStatus.APPROVED -> "Process payment"
            ClaimStatus.DENIED -> "Send denial letter"
            ClaimStatus.UNDER_INVESTIGATION -> "Assign investigator"
            ClaimStatus.PENDING_INFORMATION -> "Request additional documentation"
            else -> "Continue processing"
        }
    }

    private fun calculateAverageProcessingTime(claims: List<Claim>): Double {
        val completedClaims = claims.filter { it.status in setOf(ClaimStatus.PAID, ClaimStatus.DENIED) }
        if (completedClaims.isEmpty()) return 0.0
        
        val totalDays = completedClaims.sumOf { claim ->
            val endDate = claim.paidDate ?: claim.lastModified.toLocalDate()
            java.time.Period.between(claim.dateReported, endDate).days
        }
        
        return totalDays.toDouble() / completedClaims.size
    }

    private fun calculateFraudStats(claims: List<Claim>): FraudDetectionStats {
        val suspiciousClaims = claims.count { detectFraudIndicators(it).isNotEmpty() }
        val confirmedFraud = claims.count { it.status == ClaimStatus.DENIED && it.investigationResult?.fraudConfirmed == true }
        
        return FraudDetectionStats(
            suspiciousClaims = suspiciousClaims,
            confirmedFraud = confirmedFraud,
            fraudRate = if (claims.isNotEmpty()) confirmedFraud.toDouble() / claims.size else 0.0
        )
    }

    private fun generateClaimId(): String = "CLM-${System.currentTimeMillis()}"
    private fun generatePaymentId(): String = "PAY-${System.currentTimeMillis()}"

    // Mock database operations
    private fun findClaimById(claimId: String): Claim? = null
    private fun findPolicyByNumber(policyNumber: String): Policy? = null
    private fun findClaimsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Claim> = emptyList()
    private fun findClaimsByClaimant(claimantName: String): List<Claim> = emptyList()
    private fun saveClaim(claim: Claim): Claim = claim
    private fun savePayment(payment: ClaimPayment): ClaimPayment = payment
    private fun processPaymentThroughGateway(payment: ClaimPayment): PaymentResult = 
        PaymentResult("PAY-${System.currentTimeMillis()}", PaymentStatus.COMPLETED, "CONF-${System.currentTimeMillis()}")
}

// Data classes and enums
data class ClaimSubmissionRequest(
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val claimantInfo: ClaimantInfo,
    val description: String
)

data class Claim(
    val claimId: String,
    val policyNumber: String,
    val claimType: ClaimType,
    val claimAmount: BigDecimal,
    val dateOfLoss: LocalDate,
    val dateReported: LocalDate,
    var status: ClaimStatus,
    val claimantInfo: ClaimantInfo,
    val description: String,
    var investigationRequired: Boolean = false,
    var investigationCompleted: Boolean = false,
    var investigationResult: InvestigationResult? = null,
    val reviews: MutableList<ClaimReview> = mutableListOf(),
    var paidAmount: BigDecimal? = null,
    var paidDate: LocalDateTime? = null,
    val lastModified: LocalDateTime = LocalDateTime.now()
)

data class ClaimantInfo(
    val name: String,
    val relationship: String,
    val contactInfo: ContactInfo
)

data class ContactInfo(
    val phone: String,
    val email: String,
    val address: Address
)

data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String
)

data class ClaimReview(
    val reviewId: String,
    val claimId: String,
    val reviewerId: String,
    val reviewDate: LocalDateTime,
    val decision: ReviewDecision,
    val comments: String
)

data class ReviewDecision(
    val type: ReviewDecisionType,
    val comments: String
)

data class ClaimReviewResult(
    val claimId: String,
    val reviewId: String,
    val newStatus: ClaimStatus,
    val nextAction: String
)

data class ClaimPayment(
    val paymentId: String,
    val claimId: String,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDate: LocalDateTime,
    var status: PaymentStatus,
    var confirmationNumber: String? = null
)

data class ClaimAnalytics(
    val reportPeriod: DateRange,
    val totalClaims: Int,
    val totalClaimAmount: BigDecimal,
    val averageClaimAmount: BigDecimal,
    val claimsByType: Map<ClaimType, Int>,
    val claimsByStatus: Map<ClaimStatus, Int>,
    val averageProcessingTime: Double,
    val fraudDetectionStats: FraudDetectionStats
)

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class InvestigationResult(
    val investigatorId: String,
    val completionDate: LocalDateTime,
    val findings: String,
    val recommendation: InvestigationRecommendation,
    val fraudConfirmed: Boolean = false
)

data class FraudDetectionStats(
    val suspiciousClaims: Int,
    val confirmedFraud: Int,
    val fraudRate: Double
)

enum class ClaimType {
    DEATH_BENEFIT, DISABILITY, CRITICAL_ILLNESS, ACCIDENT, SURRENDER
}

enum class ClaimStatus {
    SUBMITTED, UNDER_REVIEW, UNDER_INVESTIGATION, PENDING_INFORMATION, 
    APPROVED, DENIED, PAID, SIU_REFERRAL
}

enum class ReviewDecisionType {
    APPROVE, DENY, INVESTIGATE, REQUEST_INFO
}

enum class InvestigationRecommendation {
    APPROVE, DENY, FURTHER_REVIEW, REFER_TO_SIU
}

enum class FraudIndicator {
    LARGE_CLAIM_AMOUNT, EARLY_CLAIM, MULTIPLE_CLAIMS, WEEKEND_INCIDENT, 
    INCONSISTENT_STATEMENTS, SUSPICIOUS_DOCUMENTATION
}

class ClaimNotFoundException(message: String) : RuntimeException(message)
