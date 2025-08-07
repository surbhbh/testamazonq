package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Workflow engine for managing business processes
 */
@Service
class WorkflowEngine {

    private val activeWorkflows = mutableMapOf<String, WorkflowInstance>()
    private val workflowDefinitions = mutableMapOf<String, WorkflowDefinition>()

    init {
        initializeWorkflowDefinitions()
    }

    fun startWorkflow(workflowType: String, initiator: String, context: Map<String, Any>): WorkflowInstance {
        val definition = workflowDefinitions[workflowType]
            ?: throw IllegalArgumentException("Unknown workflow type: $workflowType")

        val workflowId = UUID.randomUUID().toString()
        val instance = WorkflowInstance(
            workflowId = workflowId,
            workflowType = workflowType,
            definition = definition,
            status = WorkflowStatus.RUNNING,
            initiator = initiator,
            context = context.toMutableMap(),
            currentStep = definition.steps.first(),
            startTime = LocalDateTime.now(),
            history = mutableListOf()
        )

        activeWorkflows[workflowId] = instance
        executeCurrentStep(instance)
        
        return instance
    }

    fun completeTask(workflowId: String, taskId: String, userId: String, result: TaskResult): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Complete the task
        task.status = TaskStatus.COMPLETED
        task.completedBy = userId
        task.completedAt = LocalDateTime.now()
        task.result = result

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_COMPLETED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Task ${task.taskName} completed",
            data = mapOf("taskId" to taskId, "result" to result)
        ))

        // Update context with task result
        instance.context.putAll(result.data)

        // Check if all tasks in current step are completed
        val currentStepTasks = instance.pendingTasks.filter { it.stepId == instance.currentStep.stepId }
        if (currentStepTasks.all { it.status == TaskStatus.COMPLETED }) {
            moveToNextStep(instance)
        }

        return instance
    }

    fun getWorkflowInstance(workflowId: String): WorkflowInstance? {
        return activeWorkflows[workflowId]
    }

    fun getActiveWorkflows(): List<WorkflowInstance> {
        return activeWorkflows.values.toList()
    }

    fun getWorkflowsByUser(userId: String): List<WorkflowInstance> {
        return activeWorkflows.values.filter { workflow ->
            workflow.pendingTasks.any { task -> 
                task.assignedTo == userId && task.status == TaskStatus.PENDING 
            }
        }
    }

    fun cancelWorkflow(workflowId: String, userId: String, reason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.status = WorkflowStatus.CANCELLED
        instance.endTime = LocalDateTime.now()

        // Cancel all pending tasks
        instance.pendingTasks.filter { it.status == TaskStatus.PENDING }
            .forEach { task ->
                task.status = TaskStatus.CANCELLED
                task.completedAt = LocalDateTime.now()
            }

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.WORKFLOW_CANCELLED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Workflow cancelled: $reason",
            data = mapOf("reason" to reason)
        ))

        return instance
    }

    fun escalateTask(workflowId: String, taskId: String, escalationReason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Escalate to supervisor
        val originalAssignee = task.assignedTo
        task.assignedTo = getEscalationTarget(task.assignedTo)
        task.escalationLevel++
        task.escalatedAt = LocalDateTime.now()

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_ESCALATED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Task escalated from $originalAssignee to ${task.assignedTo}",
            data = mapOf(
                "taskId" to taskId,
                "reason" to escalationReason,
                "originalAssignee" to originalAssignee,
                "newAssignee" to task.assignedTo
            )
        ))

        return instance
    }

    fun reassignTask(workflowId: String, taskId: String, newAssignee: String, reassignedBy: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val originalAssignee = task.assignedTo
        task.assignedTo = newAssignee

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_REASSIGNED,
            timestamp = LocalDateTime.now(),
            userId = reassignedBy,
            description = "Task reassigned from $originalAssignee to $newAssignee",
            data = mapOf(
                "taskId" to taskId,
                "originalAssignee" to originalAssignee,
                "newAssignee" to newAssignee
            )
        ))

        return instance
    }

    fun addComment(workflowId: String, userId: String, comment: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.COMMENT_ADDED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = comment,
            data = emptyMap()
        ))

        return instance
    }

    fun getWorkflowMetrics(): WorkflowMetrics {
        val totalWorkflows = activeWorkflows.size
        val runningWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.RUNNING }
        val completedWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.COMPLETED }
        val cancelledWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.CANCELLED }

        val averageCompletionTime = activeWorkflows.values
            .filter { it.status == WorkflowStatus.COMPLETED && it.endTime != null }
            .map { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }
            .average()

        val taskMetrics = calculateTaskMetrics()

        return WorkflowMetrics(
            totalWorkflows = totalWorkflows,
            runningWorkflows = runningWorkflows,
            completedWorkflows = completedWorkflows,
            cancelledWorkflows = cancelledWorkflows,
            averageCompletionTimeMinutes = averageCompletionTime,
            taskMetrics = taskMetrics,
            workflowsByType = activeWorkflows.values.groupBy { it.workflowType }.mapValues { it.value.size }
        )
    }

    // Private helper methods
    private fun initializeWorkflowDefinitions() {
        // Policy Application Workflow
        workflowDefinitions["POLICY_APPLICATION"] = WorkflowDefinition(
            workflowType = "POLICY_APPLICATION",
            name = "Policy Application Processing",
            description = "Complete workflow for processing new policy applications",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial review of application completeness",
                    requiredRoles = listOf("UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "MEDICAL_UNDERWRITING",
                    stepName = "Medical Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Medical underwriting assessment",
                    requiredRoles = listOf("MEDICAL_UNDERWRITER"),
                    timeoutMinutes = 4320, // 72 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINANCIAL_UNDERWRITING",
                    stepName = "Financial Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Financial underwriting assessment",
                    requiredRoles = listOf("FINANCIAL_UNDERWRITER"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINAL_DECISION",
                    stepName = "Final Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final underwriting decision",
                    requiredRoles = listOf("SENIOR_UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "POLICY_ISSUANCE",
                    stepName = "Policy Issuance",
                    stepType = StepType.AUTOMATED,
                    description = "Automated policy issuance",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 60,
                    autoAssign = false
                )
            )
        )

        // Claims Processing Workflow
        workflowDefinitions["CLAIMS_PROCESSING"] = WorkflowDefinition(
            workflowType = "CLAIMS_PROCESSING",
            name = "Claims Processing",
            description = "Complete workflow for processing insurance claims",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "CLAIM_INTAKE",
                    stepName = "Claim Intake",
                    stepType = StepType.AUTOMATED,
                    description = "Automated claim intake and validation",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 30,
                    autoAssign = false
                ),
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial claim review and triage",
                    requiredRoles = listOf("CLAIMS_EXAMINER"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "INVESTIGATION",
                    stepName = "Investigation",
                    stepType = StepType.HUMAN_TASK,
                    description = "Detailed claim investigation",
                    requiredRoles = listOf("CLAIMS_INVESTIGATOR"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "DECISION",
                    stepName = "Claim Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final claim decision",
                    requiredRoles = listOf("CLAIMS_MANAGER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "PAYMENT_PROCESSING",
                    stepName = "Payment Processing",
                    stepType = StepType.AUTOMATED,
                    description = "Automated payment processing",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 120,
                    autoAssign = false
                )
            )
        )

        // Customer Service Workflow
        workflowDefinitions["CUSTOMER_SERVICE"] = WorkflowDefinition(
            workflowType = "CUSTOMER_SERVICE",
            name = "Customer Service Request",
            description = "Workflow for handling customer service requests",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "REQUEST_TRIAGE",
                    stepName = "Request Triage",
                    stepType = StepType.HUMAN_TASK,
                    description = "Triage and categorize customer request",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 240, // 4 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "RESOLUTION",
                    stepName = "Issue Resolution",
                    stepType = StepType.HUMAN_TASK,
                    description = "Resolve customer issue",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP", "SPECIALIST"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FOLLOW_UP",
                    stepName = "Customer Follow-up",
                    stepType = StepType.HUMAN_TASK,
                    description = "Follow up with customer on resolution",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                )
            )
        )
    }

    private fun executeCurrentStep(instance: WorkflowInstance) {
        val currentStep = instance.currentStep

        when (currentStep.stepType) {
            StepType.AUTOMATED -> {
                executeAutomatedStep(instance, currentStep)
            }
            StepType.HUMAN_TASK -> {
                createHumanTasks(instance, currentStep)
            }
            StepType.DECISION -> {
                executeDecisionStep(instance, currentStep)
            }
            StepType.PARALLEL -> {
                executeParallelStep(instance, currentStep)
            }
        }

        // Add step started event
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.STEP_STARTED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Step ${currentStep.stepName} started",
            data = mapOf("stepId" to currentStep.stepId)
        ))
    }

    private fun executeAutomatedStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Simulate automated processing
        when (step.stepId) {
            "CLAIM_INTAKE" -> {
                instance.context["claimValidated"] = true
                instance.context["claimAmount"] = 50000
            }
            "POLICY_ISSUANCE" -> {
                instance.context["policyNumber"] = "POL-${System.currentTimeMillis()}"
                instance.context["policyIssued"] = true
            }
            "PAYMENT_PROCESSING" -> {
                instance.context["paymentProcessed"] = true
                instance.context["paymentId"] = "PAY-${System.currentTimeMillis()}"
            }
        }

        // Automatically move to next step for automated tasks
        moveToNextStep(instance)
    }

    private fun createHumanTasks(instance: WorkflowInstance, step: WorkflowStep) {
        val taskId = UUID.randomUUID().toString()
        val assignee = if (step.autoAssign) {
            assignTaskToUser(step.requiredRoles.first())
        } else {
            null
        }

        val task = WorkflowTask(
            taskId = taskId,
            stepId = step.stepId,
            taskName = step.stepName,
            description = step.description,
            assignedTo = assignee,
            status = TaskStatus.PENDING,
            createdAt = LocalDateTime.now(),
            dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
            priority = determinePriority(instance, step),
            requiredRoles = step.requiredRoles,
            escalationLevel = 0
        )

        instance.pendingTasks.add(task)
    }

    private fun executeDecisionStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Decision logic based on context
        val decision = makeAutomatedDecision(instance, step)
        instance.context["decision"] = decision
        moveToNextStep(instance)
    }

    private fun executeParallelStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Create multiple parallel tasks
        step.requiredRoles.forEach { role ->
            val taskId = UUID.randomUUID().toString()
            val task = WorkflowTask(
                taskId = taskId,
                stepId = step.stepId,
                taskName = "${step.stepName} - $role",
                description = step.description,
                assignedTo = assignTaskToUser(role),
                status = TaskStatus.PENDING,
                createdAt = LocalDateTime.now(),
                dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
                priority = determinePriority(instance, step),
                requiredRoles = listOf(role),
                escalationLevel = 0
            )
            instance.pendingTasks.add(task)
        }
    }

    private fun moveToNextStep(instance: WorkflowInstance) {
        val currentStepIndex = instance.definition.steps.indexOf(instance.currentStep)
        
        if (currentStepIndex < instance.definition.steps.size - 1) {
            // Move to next step
            instance.currentStep = instance.definition.steps[currentStepIndex + 1]
            executeCurrentStep(instance)
        } else {
            // Workflow completed
            instance.status = WorkflowStatus.COMPLETED
            instance.endTime = LocalDateTime.now()
            
            instance.history.add(WorkflowEvent(
                eventId = UUID.randomUUID().toString(),
                eventType = EventType.WORKFLOW_COMPLETED,
                timestamp = LocalDateTime.now(),
                userId = "SYSTEM",
                description = "Workflow completed successfully",
                data = emptyMap()
            ))
        }
    }

    private fun assignTaskToUser(role: String): String {
        // Simple round-robin assignment based on role
        return when (role) {
            "UNDERWRITER" -> "underwriter${(1..5).random()}"
            "MEDICAL_UNDERWRITER" -> "med_uw${(1..3).random()}"
            "FINANCIAL_UNDERWRITER" -> "fin_uw${(1..3).random()}"
            "SENIOR_UNDERWRITER" -> "senior_uw${(1..2).random()}"
            "CLAIMS_EXAMINER" -> "claims_exam${(1..10).random()}"
            "CLAIMS_INVESTIGATOR" -> "claims_inv${(1..5).random()}"
            "CLAIMS_MANAGER" -> "claims_mgr${(1..3).random()}"
            "CUSTOMER_SERVICE_REP" -> "csr${(1..15).random()}"
            "SPECIALIST" -> "specialist${(1..5).random()}"
            else -> "user${(1..10).random()}"
        }
    }

    private fun determinePriority(instance: WorkflowInstance, step: WorkflowStep): TaskPriority {
        return when {
            instance.context["urgent"] == true -> TaskPriority.HIGH
            instance.context["claimAmount"] as? Int ?: 0 > 100000 -> TaskPriority.HIGH
            step.timeoutMinutes < 480 -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }
    }

    private fun makeAutomatedDecision(instance: WorkflowInstance, step: WorkflowStep): String {
        // Simple decision logic based on context
        return when {
            instance.context["riskScore"] as? Double ?: 0.0 > 0.8 -> "DECLINE"
            instance.context["claimAmount"] as? Int ?: 0 > 500000 -> "INVESTIGATE"
            else -> "APPROVE"
        }
    }

    private fun getEscalationTarget(currentAssignee: String): String {
        // Simple escalation logic
        return when {
            currentAssignee.startsWith("underwriter") -> "senior_uw1"
            currentAssignee.startsWith("claims_exam") -> "claims_mgr1"
            currentAssignee.startsWith("csr") -> "supervisor1"
            else -> "manager1"
        }
    }

    private fun calculateTaskMetrics(): TaskMetrics {
        val allTasks = activeWorkflows.values.flatMap { it.pendingTasks }
        val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED }
        val pendingTasks = allTasks.filter { it.status == TaskStatus.PENDING }
        val overdueTasks = pendingTasks.filter { it.dueDate.isBefore(LocalDateTime.now()) }

        val averageCompletionTime = completedTasks
            .filter { it.completedAt != null }
            .map { java.time.Duration.between(it.createdAt, it.completedAt).toMinutes() }
            .average()

        return TaskMetrics(
            totalTasks = allTasks.size,
            completedTasks = completedTasks.size,
            pendingTasks = pendingTasks.size,
            overdueTasks = overdueTasks.size,
            averageCompletionTimeMinutes = averageCompletionTime,
            tasksByPriority = allTasks.groupBy { it.priority }.mapValues { it.value.size },
            tasksByStatus = allTasks.groupBy { it.status }.mapValues { it.value.size }
        )
    }
}

// Data classes for workflow management
data class WorkflowDefinition(
    val workflowType: String,
    val name: String,
    val description: String,
    val version: String,
    val steps: List<WorkflowStep>
)

data class WorkflowStep(
    val stepId: String,
    val stepName: String,
    val stepType: StepType,
    val description: String,
    val requiredRoles: List<String>,
    val timeoutMinutes: Int,
    val autoAssign: Boolean
)

data class WorkflowInstance(
    val workflowId: String,
    val workflowType: String,
    val definition: WorkflowDefinition,
    var status: WorkflowStatus,
    val initiator: String,
    val context: MutableMap<String, Any>,
    var currentStep: WorkflowStep,
    val startTime: LocalDateTime,
    var endTime: LocalDateTime? = null,
    val pendingTasks: MutableList<WorkflowTask> = mutableListOf(),
    val history: MutableList<WorkflowEvent> = mutableListOf()
)

data class WorkflowTask(
    val taskId: String,
    val stepId: String,
    val taskName: String,
    val description: String,
    var assignedTo: String?,
    var status: TaskStatus,
    val createdAt: LocalDateTime,
    val dueDate: LocalDateTime,
    val priority: TaskPriority,
    val requiredRoles: List<String>,
    var escalationLevel: Int,
    var completedBy: String? = null,
    var completedAt: LocalDateTime? = null,
    var escalatedAt: LocalDateTime? = null,
    var result: TaskResult? = null
)

data class TaskResult(
    val decision: String,
    val comments: String,
    val data: Map<String, Any>
)

data class WorkflowEvent(
    val eventId: String,
    val eventType: EventType,
    val timestamp: LocalDateTime,
    val userId: String,
    val description: String,
    val data: Map<String, Any>
)

data class WorkflowMetrics(
    val totalWorkflows: Int,
    val runningWorkflows: Int,
    val completedWorkflows: Int,
    val cancelledWorkflows: Int,
    val averageCompletionTimeMinutes: Double,
    val taskMetrics: TaskMetrics,
    val workflowsByType: Map<String, Int>
)

data class TaskMetrics(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    val averageCompletionTimeMinutes: Double,
    val tasksByPriority: Map<TaskPriority, Int>,
    val tasksByStatus: Map<TaskStatus, Int>
)

// Enums
enum class WorkflowStatus {
    RUNNING, COMPLETED, CANCELLED, SUSPENDED, ERROR
}

enum class StepType {
    AUTOMATED, HUMAN_TASK, DECISION, PARALLEL
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ESCALATED
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class EventType {
    WORKFLOW_STARTED, WORKFLOW_COMPLETED, WORKFLOW_CANCELLED,
    STEP_STARTED, STEP_COMPLETED,
    TASK_CREATED, TASK_ASSIGNED, TASK_COMPLETED, TASK_ESCALATED, TASK_REASSIGNED,
    COMMENT_ADDED, ERROR_OCCURRED
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Workflow engine for managing business processes
 */
@Service
class WorkflowEngine {

    private val activeWorkflows = mutableMapOf<String, WorkflowInstance>()
    private val workflowDefinitions = mutableMapOf<String, WorkflowDefinition>()

    init {
        initializeWorkflowDefinitions()
    }

    fun startWorkflow(workflowType: String, initiator: String, context: Map<String, Any>): WorkflowInstance {
        val definition = workflowDefinitions[workflowType]
            ?: throw IllegalArgumentException("Unknown workflow type: $workflowType")

        val workflowId = UUID.randomUUID().toString()
        val instance = WorkflowInstance(
            workflowId = workflowId,
            workflowType = workflowType,
            definition = definition,
            status = WorkflowStatus.RUNNING,
            initiator = initiator,
            context = context.toMutableMap(),
            currentStep = definition.steps.first(),
            startTime = LocalDateTime.now(),
            history = mutableListOf()
        )

        activeWorkflows[workflowId] = instance
        executeCurrentStep(instance)
        
        return instance
    }

    fun completeTask(workflowId: String, taskId: String, userId: String, result: TaskResult): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Complete the task
        task.status = TaskStatus.COMPLETED
        task.completedBy = userId
        task.completedAt = LocalDateTime.now()
        task.result = result

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_COMPLETED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Task ${task.taskName} completed",
            data = mapOf("taskId" to taskId, "result" to result)
        ))

        // Update context with task result
        instance.context.putAll(result.data)

        // Check if all tasks in current step are completed
        val currentStepTasks = instance.pendingTasks.filter { it.stepId == instance.currentStep.stepId }
        if (currentStepTasks.all { it.status == TaskStatus.COMPLETED }) {
            moveToNextStep(instance)
        }

        return instance
    }

    fun getWorkflowInstance(workflowId: String): WorkflowInstance? {
        return activeWorkflows[workflowId]
    }

    fun getActiveWorkflows(): List<WorkflowInstance> {
        return activeWorkflows.values.toList()
    }

    fun getWorkflowsByUser(userId: String): List<WorkflowInstance> {
        return activeWorkflows.values.filter { workflow ->
            workflow.pendingTasks.any { task -> 
                task.assignedTo == userId && task.status == TaskStatus.PENDING 
            }
        }
    }

    fun cancelWorkflow(workflowId: String, userId: String, reason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.status = WorkflowStatus.CANCELLED
        instance.endTime = LocalDateTime.now()

        // Cancel all pending tasks
        instance.pendingTasks.filter { it.status == TaskStatus.PENDING }
            .forEach { task ->
                task.status = TaskStatus.CANCELLED
                task.completedAt = LocalDateTime.now()
            }

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.WORKFLOW_CANCELLED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Workflow cancelled: $reason",
            data = mapOf("reason" to reason)
        ))

        return instance
    }

    fun escalateTask(workflowId: String, taskId: String, escalationReason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Escalate to supervisor
        val originalAssignee = task.assignedTo
        task.assignedTo = getEscalationTarget(task.assignedTo)
        task.escalationLevel++
        task.escalatedAt = LocalDateTime.now()

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_ESCALATED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Task escalated from $originalAssignee to ${task.assignedTo}",
            data = mapOf(
                "taskId" to taskId,
                "reason" to escalationReason,
                "originalAssignee" to originalAssignee,
                "newAssignee" to task.assignedTo
            )
        ))

        return instance
    }

    fun reassignTask(workflowId: String, taskId: String, newAssignee: String, reassignedBy: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val originalAssignee = task.assignedTo
        task.assignedTo = newAssignee

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_REASSIGNED,
            timestamp = LocalDateTime.now(),
            userId = reassignedBy,
            description = "Task reassigned from $originalAssignee to $newAssignee",
            data = mapOf(
                "taskId" to taskId,
                "originalAssignee" to originalAssignee,
                "newAssignee" to newAssignee
            )
        ))

        return instance
    }

    fun addComment(workflowId: String, userId: String, comment: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.COMMENT_ADDED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = comment,
            data = emptyMap()
        ))

        return instance
    }

    fun getWorkflowMetrics(): WorkflowMetrics {
        val totalWorkflows = activeWorkflows.size
        val runningWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.RUNNING }
        val completedWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.COMPLETED }
        val cancelledWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.CANCELLED }

        val averageCompletionTime = activeWorkflows.values
            .filter { it.status == WorkflowStatus.COMPLETED && it.endTime != null }
            .map { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }
            .average()

        val taskMetrics = calculateTaskMetrics()

        return WorkflowMetrics(
            totalWorkflows = totalWorkflows,
            runningWorkflows = runningWorkflows,
            completedWorkflows = completedWorkflows,
            cancelledWorkflows = cancelledWorkflows,
            averageCompletionTimeMinutes = averageCompletionTime,
            taskMetrics = taskMetrics,
            workflowsByType = activeWorkflows.values.groupBy { it.workflowType }.mapValues { it.value.size }
        )
    }

    // Private helper methods
    private fun initializeWorkflowDefinitions() {
        // Policy Application Workflow
        workflowDefinitions["POLICY_APPLICATION"] = WorkflowDefinition(
            workflowType = "POLICY_APPLICATION",
            name = "Policy Application Processing",
            description = "Complete workflow for processing new policy applications",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial review of application completeness",
                    requiredRoles = listOf("UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "MEDICAL_UNDERWRITING",
                    stepName = "Medical Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Medical underwriting assessment",
                    requiredRoles = listOf("MEDICAL_UNDERWRITER"),
                    timeoutMinutes = 4320, // 72 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINANCIAL_UNDERWRITING",
                    stepName = "Financial Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Financial underwriting assessment",
                    requiredRoles = listOf("FINANCIAL_UNDERWRITER"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINAL_DECISION",
                    stepName = "Final Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final underwriting decision",
                    requiredRoles = listOf("SENIOR_UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "POLICY_ISSUANCE",
                    stepName = "Policy Issuance",
                    stepType = StepType.AUTOMATED,
                    description = "Automated policy issuance",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 60,
                    autoAssign = false
                )
            )
        )

        // Claims Processing Workflow
        workflowDefinitions["CLAIMS_PROCESSING"] = WorkflowDefinition(
            workflowType = "CLAIMS_PROCESSING",
            name = "Claims Processing",
            description = "Complete workflow for processing insurance claims",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "CLAIM_INTAKE",
                    stepName = "Claim Intake",
                    stepType = StepType.AUTOMATED,
                    description = "Automated claim intake and validation",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 30,
                    autoAssign = false
                ),
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial claim review and triage",
                    requiredRoles = listOf("CLAIMS_EXAMINER"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "INVESTIGATION",
                    stepName = "Investigation",
                    stepType = StepType.HUMAN_TASK,
                    description = "Detailed claim investigation",
                    requiredRoles = listOf("CLAIMS_INVESTIGATOR"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "DECISION",
                    stepName = "Claim Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final claim decision",
                    requiredRoles = listOf("CLAIMS_MANAGER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "PAYMENT_PROCESSING",
                    stepName = "Payment Processing",
                    stepType = StepType.AUTOMATED,
                    description = "Automated payment processing",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 120,
                    autoAssign = false
                )
            )
        )

        // Customer Service Workflow
        workflowDefinitions["CUSTOMER_SERVICE"] = WorkflowDefinition(
            workflowType = "CUSTOMER_SERVICE",
            name = "Customer Service Request",
            description = "Workflow for handling customer service requests",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "REQUEST_TRIAGE",
                    stepName = "Request Triage",
                    stepType = StepType.HUMAN_TASK,
                    description = "Triage and categorize customer request",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 240, // 4 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "RESOLUTION",
                    stepName = "Issue Resolution",
                    stepType = StepType.HUMAN_TASK,
                    description = "Resolve customer issue",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP", "SPECIALIST"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FOLLOW_UP",
                    stepName = "Customer Follow-up",
                    stepType = StepType.HUMAN_TASK,
                    description = "Follow up with customer on resolution",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                )
            )
        )
    }

    private fun executeCurrentStep(instance: WorkflowInstance) {
        val currentStep = instance.currentStep

        when (currentStep.stepType) {
            StepType.AUTOMATED -> {
                executeAutomatedStep(instance, currentStep)
            }
            StepType.HUMAN_TASK -> {
                createHumanTasks(instance, currentStep)
            }
            StepType.DECISION -> {
                executeDecisionStep(instance, currentStep)
            }
            StepType.PARALLEL -> {
                executeParallelStep(instance, currentStep)
            }
        }

        // Add step started event
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.STEP_STARTED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Step ${currentStep.stepName} started",
            data = mapOf("stepId" to currentStep.stepId)
        ))
    }

    private fun executeAutomatedStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Simulate automated processing
        when (step.stepId) {
            "CLAIM_INTAKE" -> {
                instance.context["claimValidated"] = true
                instance.context["claimAmount"] = 50000
            }
            "POLICY_ISSUANCE" -> {
                instance.context["policyNumber"] = "POL-${System.currentTimeMillis()}"
                instance.context["policyIssued"] = true
            }
            "PAYMENT_PROCESSING" -> {
                instance.context["paymentProcessed"] = true
                instance.context["paymentId"] = "PAY-${System.currentTimeMillis()}"
            }
        }

        // Automatically move to next step for automated tasks
        moveToNextStep(instance)
    }

    private fun createHumanTasks(instance: WorkflowInstance, step: WorkflowStep) {
        val taskId = UUID.randomUUID().toString()
        val assignee = if (step.autoAssign) {
            assignTaskToUser(step.requiredRoles.first())
        } else {
            null
        }

        val task = WorkflowTask(
            taskId = taskId,
            stepId = step.stepId,
            taskName = step.stepName,
            description = step.description,
            assignedTo = assignee,
            status = TaskStatus.PENDING,
            createdAt = LocalDateTime.now(),
            dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
            priority = determinePriority(instance, step),
            requiredRoles = step.requiredRoles,
            escalationLevel = 0
        )

        instance.pendingTasks.add(task)
    }

    private fun executeDecisionStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Decision logic based on context
        val decision = makeAutomatedDecision(instance, step)
        instance.context["decision"] = decision
        moveToNextStep(instance)
    }

    private fun executeParallelStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Create multiple parallel tasks
        step.requiredRoles.forEach { role ->
            val taskId = UUID.randomUUID().toString()
            val task = WorkflowTask(
                taskId = taskId,
                stepId = step.stepId,
                taskName = "${step.stepName} - $role",
                description = step.description,
                assignedTo = assignTaskToUser(role),
                status = TaskStatus.PENDING,
                createdAt = LocalDateTime.now(),
                dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
                priority = determinePriority(instance, step),
                requiredRoles = listOf(role),
                escalationLevel = 0
            )
            instance.pendingTasks.add(task)
        }
    }

    private fun moveToNextStep(instance: WorkflowInstance) {
        val currentStepIndex = instance.definition.steps.indexOf(instance.currentStep)
        
        if (currentStepIndex < instance.definition.steps.size - 1) {
            // Move to next step
            instance.currentStep = instance.definition.steps[currentStepIndex + 1]
            executeCurrentStep(instance)
        } else {
            // Workflow completed
            instance.status = WorkflowStatus.COMPLETED
            instance.endTime = LocalDateTime.now()
            
            instance.history.add(WorkflowEvent(
                eventId = UUID.randomUUID().toString(),
                eventType = EventType.WORKFLOW_COMPLETED,
                timestamp = LocalDateTime.now(),
                userId = "SYSTEM",
                description = "Workflow completed successfully",
                data = emptyMap()
            ))
        }
    }

    private fun assignTaskToUser(role: String): String {
        // Simple round-robin assignment based on role
        return when (role) {
            "UNDERWRITER" -> "underwriter${(1..5).random()}"
            "MEDICAL_UNDERWRITER" -> "med_uw${(1..3).random()}"
            "FINANCIAL_UNDERWRITER" -> "fin_uw${(1..3).random()}"
            "SENIOR_UNDERWRITER" -> "senior_uw${(1..2).random()}"
            "CLAIMS_EXAMINER" -> "claims_exam${(1..10).random()}"
            "CLAIMS_INVESTIGATOR" -> "claims_inv${(1..5).random()}"
            "CLAIMS_MANAGER" -> "claims_mgr${(1..3).random()}"
            "CUSTOMER_SERVICE_REP" -> "csr${(1..15).random()}"
            "SPECIALIST" -> "specialist${(1..5).random()}"
            else -> "user${(1..10).random()}"
        }
    }

    private fun determinePriority(instance: WorkflowInstance, step: WorkflowStep): TaskPriority {
        return when {
            instance.context["urgent"] == true -> TaskPriority.HIGH
            instance.context["claimAmount"] as? Int ?: 0 > 100000 -> TaskPriority.HIGH
            step.timeoutMinutes < 480 -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }
    }

    private fun makeAutomatedDecision(instance: WorkflowInstance, step: WorkflowStep): String {
        // Simple decision logic based on context
        return when {
            instance.context["riskScore"] as? Double ?: 0.0 > 0.8 -> "DECLINE"
            instance.context["claimAmount"] as? Int ?: 0 > 500000 -> "INVESTIGATE"
            else -> "APPROVE"
        }
    }

    private fun getEscalationTarget(currentAssignee: String): String {
        // Simple escalation logic
        return when {
            currentAssignee.startsWith("underwriter") -> "senior_uw1"
            currentAssignee.startsWith("claims_exam") -> "claims_mgr1"
            currentAssignee.startsWith("csr") -> "supervisor1"
            else -> "manager1"
        }
    }

    private fun calculateTaskMetrics(): TaskMetrics {
        val allTasks = activeWorkflows.values.flatMap { it.pendingTasks }
        val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED }
        val pendingTasks = allTasks.filter { it.status == TaskStatus.PENDING }
        val overdueTasks = pendingTasks.filter { it.dueDate.isBefore(LocalDateTime.now()) }

        val averageCompletionTime = completedTasks
            .filter { it.completedAt != null }
            .map { java.time.Duration.between(it.createdAt, it.completedAt).toMinutes() }
            .average()

        return TaskMetrics(
            totalTasks = allTasks.size,
            completedTasks = completedTasks.size,
            pendingTasks = pendingTasks.size,
            overdueTasks = overdueTasks.size,
            averageCompletionTimeMinutes = averageCompletionTime,
            tasksByPriority = allTasks.groupBy { it.priority }.mapValues { it.value.size },
            tasksByStatus = allTasks.groupBy { it.status }.mapValues { it.value.size }
        )
    }
}

// Data classes for workflow management
data class WorkflowDefinition(
    val workflowType: String,
    val name: String,
    val description: String,
    val version: String,
    val steps: List<WorkflowStep>
)

data class WorkflowStep(
    val stepId: String,
    val stepName: String,
    val stepType: StepType,
    val description: String,
    val requiredRoles: List<String>,
    val timeoutMinutes: Int,
    val autoAssign: Boolean
)

data class WorkflowInstance(
    val workflowId: String,
    val workflowType: String,
    val definition: WorkflowDefinition,
    var status: WorkflowStatus,
    val initiator: String,
    val context: MutableMap<String, Any>,
    var currentStep: WorkflowStep,
    val startTime: LocalDateTime,
    var endTime: LocalDateTime? = null,
    val pendingTasks: MutableList<WorkflowTask> = mutableListOf(),
    val history: MutableList<WorkflowEvent> = mutableListOf()
)

data class WorkflowTask(
    val taskId: String,
    val stepId: String,
    val taskName: String,
    val description: String,
    var assignedTo: String?,
    var status: TaskStatus,
    val createdAt: LocalDateTime,
    val dueDate: LocalDateTime,
    val priority: TaskPriority,
    val requiredRoles: List<String>,
    var escalationLevel: Int,
    var completedBy: String? = null,
    var completedAt: LocalDateTime? = null,
    var escalatedAt: LocalDateTime? = null,
    var result: TaskResult? = null
)

data class TaskResult(
    val decision: String,
    val comments: String,
    val data: Map<String, Any>
)

data class WorkflowEvent(
    val eventId: String,
    val eventType: EventType,
    val timestamp: LocalDateTime,
    val userId: String,
    val description: String,
    val data: Map<String, Any>
)

data class WorkflowMetrics(
    val totalWorkflows: Int,
    val runningWorkflows: Int,
    val completedWorkflows: Int,
    val cancelledWorkflows: Int,
    val averageCompletionTimeMinutes: Double,
    val taskMetrics: TaskMetrics,
    val workflowsByType: Map<String, Int>
)

data class TaskMetrics(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    val averageCompletionTimeMinutes: Double,
    val tasksByPriority: Map<TaskPriority, Int>,
    val tasksByStatus: Map<TaskStatus, Int>
)

// Enums
enum class WorkflowStatus {
    RUNNING, COMPLETED, CANCELLED, SUSPENDED, ERROR
}

enum class StepType {
    AUTOMATED, HUMAN_TASK, DECISION, PARALLEL
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ESCALATED
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class EventType {
    WORKFLOW_STARTED, WORKFLOW_COMPLETED, WORKFLOW_CANCELLED,
    STEP_STARTED, STEP_COMPLETED,
    TASK_CREATED, TASK_ASSIGNED, TASK_COMPLETED, TASK_ESCALATED, TASK_REASSIGNED,
    COMMENT_ADDED, ERROR_OCCURRED
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Workflow engine for managing business processes
 */
@Service
class WorkflowEngine {

    private val activeWorkflows = mutableMapOf<String, WorkflowInstance>()
    private val workflowDefinitions = mutableMapOf<String, WorkflowDefinition>()

    init {
        initializeWorkflowDefinitions()
    }

    fun startWorkflow(workflowType: String, initiator: String, context: Map<String, Any>): WorkflowInstance {
        val definition = workflowDefinitions[workflowType]
            ?: throw IllegalArgumentException("Unknown workflow type: $workflowType")

        val workflowId = UUID.randomUUID().toString()
        val instance = WorkflowInstance(
            workflowId = workflowId,
            workflowType = workflowType,
            definition = definition,
            status = WorkflowStatus.RUNNING,
            initiator = initiator,
            context = context.toMutableMap(),
            currentStep = definition.steps.first(),
            startTime = LocalDateTime.now(),
            history = mutableListOf()
        )

        activeWorkflows[workflowId] = instance
        executeCurrentStep(instance)
        
        return instance
    }

    fun completeTask(workflowId: String, taskId: String, userId: String, result: TaskResult): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Complete the task
        task.status = TaskStatus.COMPLETED
        task.completedBy = userId
        task.completedAt = LocalDateTime.now()
        task.result = result

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_COMPLETED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Task ${task.taskName} completed",
            data = mapOf("taskId" to taskId, "result" to result)
        ))

        // Update context with task result
        instance.context.putAll(result.data)

        // Check if all tasks in current step are completed
        val currentStepTasks = instance.pendingTasks.filter { it.stepId == instance.currentStep.stepId }
        if (currentStepTasks.all { it.status == TaskStatus.COMPLETED }) {
            moveToNextStep(instance)
        }

        return instance
    }

    fun getWorkflowInstance(workflowId: String): WorkflowInstance? {
        return activeWorkflows[workflowId]
    }

    fun getActiveWorkflows(): List<WorkflowInstance> {
        return activeWorkflows.values.toList()
    }

    fun getWorkflowsByUser(userId: String): List<WorkflowInstance> {
        return activeWorkflows.values.filter { workflow ->
            workflow.pendingTasks.any { task -> 
                task.assignedTo == userId && task.status == TaskStatus.PENDING 
            }
        }
    }

    fun cancelWorkflow(workflowId: String, userId: String, reason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.status = WorkflowStatus.CANCELLED
        instance.endTime = LocalDateTime.now()

        // Cancel all pending tasks
        instance.pendingTasks.filter { it.status == TaskStatus.PENDING }
            .forEach { task ->
                task.status = TaskStatus.CANCELLED
                task.completedAt = LocalDateTime.now()
            }

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.WORKFLOW_CANCELLED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Workflow cancelled: $reason",
            data = mapOf("reason" to reason)
        ))

        return instance
    }

    fun escalateTask(workflowId: String, taskId: String, escalationReason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Escalate to supervisor
        val originalAssignee = task.assignedTo
        task.assignedTo = getEscalationTarget(task.assignedTo)
        task.escalationLevel++
        task.escalatedAt = LocalDateTime.now()

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_ESCALATED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Task escalated from $originalAssignee to ${task.assignedTo}",
            data = mapOf(
                "taskId" to taskId,
                "reason" to escalationReason,
                "originalAssignee" to originalAssignee,
                "newAssignee" to task.assignedTo
            )
        ))

        return instance
    }

    fun reassignTask(workflowId: String, taskId: String, newAssignee: String, reassignedBy: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val originalAssignee = task.assignedTo
        task.assignedTo = newAssignee

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_REASSIGNED,
            timestamp = LocalDateTime.now(),
            userId = reassignedBy,
            description = "Task reassigned from $originalAssignee to $newAssignee",
            data = mapOf(
                "taskId" to taskId,
                "originalAssignee" to originalAssignee,
                "newAssignee" to newAssignee
            )
        ))

        return instance
    }

    fun addComment(workflowId: String, userId: String, comment: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.COMMENT_ADDED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = comment,
            data = emptyMap()
        ))

        return instance
    }

    fun getWorkflowMetrics(): WorkflowMetrics {
        val totalWorkflows = activeWorkflows.size
        val runningWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.RUNNING }
        val completedWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.COMPLETED }
        val cancelledWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.CANCELLED }

        val averageCompletionTime = activeWorkflows.values
            .filter { it.status == WorkflowStatus.COMPLETED && it.endTime != null }
            .map { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }
            .average()

        val taskMetrics = calculateTaskMetrics()

        return WorkflowMetrics(
            totalWorkflows = totalWorkflows,
            runningWorkflows = runningWorkflows,
            completedWorkflows = completedWorkflows,
            cancelledWorkflows = cancelledWorkflows,
            averageCompletionTimeMinutes = averageCompletionTime,
            taskMetrics = taskMetrics,
            workflowsByType = activeWorkflows.values.groupBy { it.workflowType }.mapValues { it.value.size }
        )
    }

    // Private helper methods
    private fun initializeWorkflowDefinitions() {
        // Policy Application Workflow
        workflowDefinitions["POLICY_APPLICATION"] = WorkflowDefinition(
            workflowType = "POLICY_APPLICATION",
            name = "Policy Application Processing",
            description = "Complete workflow for processing new policy applications",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial review of application completeness",
                    requiredRoles = listOf("UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "MEDICAL_UNDERWRITING",
                    stepName = "Medical Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Medical underwriting assessment",
                    requiredRoles = listOf("MEDICAL_UNDERWRITER"),
                    timeoutMinutes = 4320, // 72 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINANCIAL_UNDERWRITING",
                    stepName = "Financial Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Financial underwriting assessment",
                    requiredRoles = listOf("FINANCIAL_UNDERWRITER"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINAL_DECISION",
                    stepName = "Final Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final underwriting decision",
                    requiredRoles = listOf("SENIOR_UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "POLICY_ISSUANCE",
                    stepName = "Policy Issuance",
                    stepType = StepType.AUTOMATED,
                    description = "Automated policy issuance",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 60,
                    autoAssign = false
                )
            )
        )

        // Claims Processing Workflow
        workflowDefinitions["CLAIMS_PROCESSING"] = WorkflowDefinition(
            workflowType = "CLAIMS_PROCESSING",
            name = "Claims Processing",
            description = "Complete workflow for processing insurance claims",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "CLAIM_INTAKE",
                    stepName = "Claim Intake",
                    stepType = StepType.AUTOMATED,
                    description = "Automated claim intake and validation",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 30,
                    autoAssign = false
                ),
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial claim review and triage",
                    requiredRoles = listOf("CLAIMS_EXAMINER"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "INVESTIGATION",
                    stepName = "Investigation",
                    stepType = StepType.HUMAN_TASK,
                    description = "Detailed claim investigation",
                    requiredRoles = listOf("CLAIMS_INVESTIGATOR"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "DECISION",
                    stepName = "Claim Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final claim decision",
                    requiredRoles = listOf("CLAIMS_MANAGER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "PAYMENT_PROCESSING",
                    stepName = "Payment Processing",
                    stepType = StepType.AUTOMATED,
                    description = "Automated payment processing",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 120,
                    autoAssign = false
                )
            )
        )

        // Customer Service Workflow
        workflowDefinitions["CUSTOMER_SERVICE"] = WorkflowDefinition(
            workflowType = "CUSTOMER_SERVICE",
            name = "Customer Service Request",
            description = "Workflow for handling customer service requests",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "REQUEST_TRIAGE",
                    stepName = "Request Triage",
                    stepType = StepType.HUMAN_TASK,
                    description = "Triage and categorize customer request",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 240, // 4 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "RESOLUTION",
                    stepName = "Issue Resolution",
                    stepType = StepType.HUMAN_TASK,
                    description = "Resolve customer issue",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP", "SPECIALIST"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FOLLOW_UP",
                    stepName = "Customer Follow-up",
                    stepType = StepType.HUMAN_TASK,
                    description = "Follow up with customer on resolution",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                )
            )
        )
    }

    private fun executeCurrentStep(instance: WorkflowInstance) {
        val currentStep = instance.currentStep

        when (currentStep.stepType) {
            StepType.AUTOMATED -> {
                executeAutomatedStep(instance, currentStep)
            }
            StepType.HUMAN_TASK -> {
                createHumanTasks(instance, currentStep)
            }
            StepType.DECISION -> {
                executeDecisionStep(instance, currentStep)
            }
            StepType.PARALLEL -> {
                executeParallelStep(instance, currentStep)
            }
        }

        // Add step started event
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.STEP_STARTED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Step ${currentStep.stepName} started",
            data = mapOf("stepId" to currentStep.stepId)
        ))
    }

    private fun executeAutomatedStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Simulate automated processing
        when (step.stepId) {
            "CLAIM_INTAKE" -> {
                instance.context["claimValidated"] = true
                instance.context["claimAmount"] = 50000
            }
            "POLICY_ISSUANCE" -> {
                instance.context["policyNumber"] = "POL-${System.currentTimeMillis()}"
                instance.context["policyIssued"] = true
            }
            "PAYMENT_PROCESSING" -> {
                instance.context["paymentProcessed"] = true
                instance.context["paymentId"] = "PAY-${System.currentTimeMillis()}"
            }
        }

        // Automatically move to next step for automated tasks
        moveToNextStep(instance)
    }

    private fun createHumanTasks(instance: WorkflowInstance, step: WorkflowStep) {
        val taskId = UUID.randomUUID().toString()
        val assignee = if (step.autoAssign) {
            assignTaskToUser(step.requiredRoles.first())
        } else {
            null
        }

        val task = WorkflowTask(
            taskId = taskId,
            stepId = step.stepId,
            taskName = step.stepName,
            description = step.description,
            assignedTo = assignee,
            status = TaskStatus.PENDING,
            createdAt = LocalDateTime.now(),
            dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
            priority = determinePriority(instance, step),
            requiredRoles = step.requiredRoles,
            escalationLevel = 0
        )

        instance.pendingTasks.add(task)
    }

    private fun executeDecisionStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Decision logic based on context
        val decision = makeAutomatedDecision(instance, step)
        instance.context["decision"] = decision
        moveToNextStep(instance)
    }

    private fun executeParallelStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Create multiple parallel tasks
        step.requiredRoles.forEach { role ->
            val taskId = UUID.randomUUID().toString()
            val task = WorkflowTask(
                taskId = taskId,
                stepId = step.stepId,
                taskName = "${step.stepName} - $role",
                description = step.description,
                assignedTo = assignTaskToUser(role),
                status = TaskStatus.PENDING,
                createdAt = LocalDateTime.now(),
                dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
                priority = determinePriority(instance, step),
                requiredRoles = listOf(role),
                escalationLevel = 0
            )
            instance.pendingTasks.add(task)
        }
    }

    private fun moveToNextStep(instance: WorkflowInstance) {
        val currentStepIndex = instance.definition.steps.indexOf(instance.currentStep)
        
        if (currentStepIndex < instance.definition.steps.size - 1) {
            // Move to next step
            instance.currentStep = instance.definition.steps[currentStepIndex + 1]
            executeCurrentStep(instance)
        } else {
            // Workflow completed
            instance.status = WorkflowStatus.COMPLETED
            instance.endTime = LocalDateTime.now()
            
            instance.history.add(WorkflowEvent(
                eventId = UUID.randomUUID().toString(),
                eventType = EventType.WORKFLOW_COMPLETED,
                timestamp = LocalDateTime.now(),
                userId = "SYSTEM",
                description = "Workflow completed successfully",
                data = emptyMap()
            ))
        }
    }

    private fun assignTaskToUser(role: String): String {
        // Simple round-robin assignment based on role
        return when (role) {
            "UNDERWRITER" -> "underwriter${(1..5).random()}"
            "MEDICAL_UNDERWRITER" -> "med_uw${(1..3).random()}"
            "FINANCIAL_UNDERWRITER" -> "fin_uw${(1..3).random()}"
            "SENIOR_UNDERWRITER" -> "senior_uw${(1..2).random()}"
            "CLAIMS_EXAMINER" -> "claims_exam${(1..10).random()}"
            "CLAIMS_INVESTIGATOR" -> "claims_inv${(1..5).random()}"
            "CLAIMS_MANAGER" -> "claims_mgr${(1..3).random()}"
            "CUSTOMER_SERVICE_REP" -> "csr${(1..15).random()}"
            "SPECIALIST" -> "specialist${(1..5).random()}"
            else -> "user${(1..10).random()}"
        }
    }

    private fun determinePriority(instance: WorkflowInstance, step: WorkflowStep): TaskPriority {
        return when {
            instance.context["urgent"] == true -> TaskPriority.HIGH
            instance.context["claimAmount"] as? Int ?: 0 > 100000 -> TaskPriority.HIGH
            step.timeoutMinutes < 480 -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }
    }

    private fun makeAutomatedDecision(instance: WorkflowInstance, step: WorkflowStep): String {
        // Simple decision logic based on context
        return when {
            instance.context["riskScore"] as? Double ?: 0.0 > 0.8 -> "DECLINE"
            instance.context["claimAmount"] as? Int ?: 0 > 500000 -> "INVESTIGATE"
            else -> "APPROVE"
        }
    }

    private fun getEscalationTarget(currentAssignee: String): String {
        // Simple escalation logic
        return when {
            currentAssignee.startsWith("underwriter") -> "senior_uw1"
            currentAssignee.startsWith("claims_exam") -> "claims_mgr1"
            currentAssignee.startsWith("csr") -> "supervisor1"
            else -> "manager1"
        }
    }

    private fun calculateTaskMetrics(): TaskMetrics {
        val allTasks = activeWorkflows.values.flatMap { it.pendingTasks }
        val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED }
        val pendingTasks = allTasks.filter { it.status == TaskStatus.PENDING }
        val overdueTasks = pendingTasks.filter { it.dueDate.isBefore(LocalDateTime.now()) }

        val averageCompletionTime = completedTasks
            .filter { it.completedAt != null }
            .map { java.time.Duration.between(it.createdAt, it.completedAt).toMinutes() }
            .average()

        return TaskMetrics(
            totalTasks = allTasks.size,
            completedTasks = completedTasks.size,
            pendingTasks = pendingTasks.size,
            overdueTasks = overdueTasks.size,
            averageCompletionTimeMinutes = averageCompletionTime,
            tasksByPriority = allTasks.groupBy { it.priority }.mapValues { it.value.size },
            tasksByStatus = allTasks.groupBy { it.status }.mapValues { it.value.size }
        )
    }
}

// Data classes for workflow management
data class WorkflowDefinition(
    val workflowType: String,
    val name: String,
    val description: String,
    val version: String,
    val steps: List<WorkflowStep>
)

data class WorkflowStep(
    val stepId: String,
    val stepName: String,
    val stepType: StepType,
    val description: String,
    val requiredRoles: List<String>,
    val timeoutMinutes: Int,
    val autoAssign: Boolean
)

data class WorkflowInstance(
    val workflowId: String,
    val workflowType: String,
    val definition: WorkflowDefinition,
    var status: WorkflowStatus,
    val initiator: String,
    val context: MutableMap<String, Any>,
    var currentStep: WorkflowStep,
    val startTime: LocalDateTime,
    var endTime: LocalDateTime? = null,
    val pendingTasks: MutableList<WorkflowTask> = mutableListOf(),
    val history: MutableList<WorkflowEvent> = mutableListOf()
)

data class WorkflowTask(
    val taskId: String,
    val stepId: String,
    val taskName: String,
    val description: String,
    var assignedTo: String?,
    var status: TaskStatus,
    val createdAt: LocalDateTime,
    val dueDate: LocalDateTime,
    val priority: TaskPriority,
    val requiredRoles: List<String>,
    var escalationLevel: Int,
    var completedBy: String? = null,
    var completedAt: LocalDateTime? = null,
    var escalatedAt: LocalDateTime? = null,
    var result: TaskResult? = null
)

data class TaskResult(
    val decision: String,
    val comments: String,
    val data: Map<String, Any>
)

data class WorkflowEvent(
    val eventId: String,
    val eventType: EventType,
    val timestamp: LocalDateTime,
    val userId: String,
    val description: String,
    val data: Map<String, Any>
)

data class WorkflowMetrics(
    val totalWorkflows: Int,
    val runningWorkflows: Int,
    val completedWorkflows: Int,
    val cancelledWorkflows: Int,
    val averageCompletionTimeMinutes: Double,
    val taskMetrics: TaskMetrics,
    val workflowsByType: Map<String, Int>
)

data class TaskMetrics(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    val averageCompletionTimeMinutes: Double,
    val tasksByPriority: Map<TaskPriority, Int>,
    val tasksByStatus: Map<TaskStatus, Int>
)

// Enums
enum class WorkflowStatus {
    RUNNING, COMPLETED, CANCELLED, SUSPENDED, ERROR
}

enum class StepType {
    AUTOMATED, HUMAN_TASK, DECISION, PARALLEL
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ESCALATED
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class EventType {
    WORKFLOW_STARTED, WORKFLOW_COMPLETED, WORKFLOW_CANCELLED,
    STEP_STARTED, STEP_COMPLETED,
    TASK_CREATED, TASK_ASSIGNED, TASK_COMPLETED, TASK_ESCALATED, TASK_REASSIGNED,
    COMMENT_ADDED, ERROR_OCCURRED
}package com.transamerica.platform.amazonq

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Workflow engine for managing business processes
 */
@Service
class WorkflowEngine {

    private val activeWorkflows = mutableMapOf<String, WorkflowInstance>()
    private val workflowDefinitions = mutableMapOf<String, WorkflowDefinition>()

    init {
        initializeWorkflowDefinitions()
    }

    fun startWorkflow(workflowType: String, initiator: String, context: Map<String, Any>): WorkflowInstance {
        val definition = workflowDefinitions[workflowType]
            ?: throw IllegalArgumentException("Unknown workflow type: $workflowType")

        val workflowId = UUID.randomUUID().toString()
        val instance = WorkflowInstance(
            workflowId = workflowId,
            workflowType = workflowType,
            definition = definition,
            status = WorkflowStatus.RUNNING,
            initiator = initiator,
            context = context.toMutableMap(),
            currentStep = definition.steps.first(),
            startTime = LocalDateTime.now(),
            history = mutableListOf()
        )

        activeWorkflows[workflowId] = instance
        executeCurrentStep(instance)
        
        return instance
    }

    fun completeTask(workflowId: String, taskId: String, userId: String, result: TaskResult): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Complete the task
        task.status = TaskStatus.COMPLETED
        task.completedBy = userId
        task.completedAt = LocalDateTime.now()
        task.result = result

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_COMPLETED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Task ${task.taskName} completed",
            data = mapOf("taskId" to taskId, "result" to result)
        ))

        // Update context with task result
        instance.context.putAll(result.data)

        // Check if all tasks in current step are completed
        val currentStepTasks = instance.pendingTasks.filter { it.stepId == instance.currentStep.stepId }
        if (currentStepTasks.all { it.status == TaskStatus.COMPLETED }) {
            moveToNextStep(instance)
        }

        return instance
    }

    fun getWorkflowInstance(workflowId: String): WorkflowInstance? {
        return activeWorkflows[workflowId]
    }

    fun getActiveWorkflows(): List<WorkflowInstance> {
        return activeWorkflows.values.toList()
    }

    fun getWorkflowsByUser(userId: String): List<WorkflowInstance> {
        return activeWorkflows.values.filter { workflow ->
            workflow.pendingTasks.any { task -> 
                task.assignedTo == userId && task.status == TaskStatus.PENDING 
            }
        }
    }

    fun cancelWorkflow(workflowId: String, userId: String, reason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.status = WorkflowStatus.CANCELLED
        instance.endTime = LocalDateTime.now()

        // Cancel all pending tasks
        instance.pendingTasks.filter { it.status == TaskStatus.PENDING }
            .forEach { task ->
                task.status = TaskStatus.CANCELLED
                task.completedAt = LocalDateTime.now()
            }

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.WORKFLOW_CANCELLED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = "Workflow cancelled: $reason",
            data = mapOf("reason" to reason)
        ))

        return instance
    }

    fun escalateTask(workflowId: String, taskId: String, escalationReason: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        // Escalate to supervisor
        val originalAssignee = task.assignedTo
        task.assignedTo = getEscalationTarget(task.assignedTo)
        task.escalationLevel++
        task.escalatedAt = LocalDateTime.now()

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_ESCALATED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Task escalated from $originalAssignee to ${task.assignedTo}",
            data = mapOf(
                "taskId" to taskId,
                "reason" to escalationReason,
                "originalAssignee" to originalAssignee,
                "newAssignee" to task.assignedTo
            )
        ))

        return instance
    }

    fun reassignTask(workflowId: String, taskId: String, newAssignee: String, reassignedBy: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        val task = instance.pendingTasks.find { it.taskId == taskId }
            ?: throw IllegalArgumentException("Task not found: $taskId")

        val originalAssignee = task.assignedTo
        task.assignedTo = newAssignee

        // Add to history
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.TASK_REASSIGNED,
            timestamp = LocalDateTime.now(),
            userId = reassignedBy,
            description = "Task reassigned from $originalAssignee to $newAssignee",
            data = mapOf(
                "taskId" to taskId,
                "originalAssignee" to originalAssignee,
                "newAssignee" to newAssignee
            )
        ))

        return instance
    }

    fun addComment(workflowId: String, userId: String, comment: String): WorkflowInstance {
        val instance = activeWorkflows[workflowId]
            ?: throw IllegalArgumentException("Workflow not found: $workflowId")

        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.COMMENT_ADDED,
            timestamp = LocalDateTime.now(),
            userId = userId,
            description = comment,
            data = emptyMap()
        ))

        return instance
    }

    fun getWorkflowMetrics(): WorkflowMetrics {
        val totalWorkflows = activeWorkflows.size
        val runningWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.RUNNING }
        val completedWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.COMPLETED }
        val cancelledWorkflows = activeWorkflows.values.count { it.status == WorkflowStatus.CANCELLED }

        val averageCompletionTime = activeWorkflows.values
            .filter { it.status == WorkflowStatus.COMPLETED && it.endTime != null }
            .map { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }
            .average()

        val taskMetrics = calculateTaskMetrics()

        return WorkflowMetrics(
            totalWorkflows = totalWorkflows,
            runningWorkflows = runningWorkflows,
            completedWorkflows = completedWorkflows,
            cancelledWorkflows = cancelledWorkflows,
            averageCompletionTimeMinutes = averageCompletionTime,
            taskMetrics = taskMetrics,
            workflowsByType = activeWorkflows.values.groupBy { it.workflowType }.mapValues { it.value.size }
        )
    }

    // Private helper methods
    private fun initializeWorkflowDefinitions() {
        // Policy Application Workflow
        workflowDefinitions["POLICY_APPLICATION"] = WorkflowDefinition(
            workflowType = "POLICY_APPLICATION",
            name = "Policy Application Processing",
            description = "Complete workflow for processing new policy applications",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial review of application completeness",
                    requiredRoles = listOf("UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "MEDICAL_UNDERWRITING",
                    stepName = "Medical Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Medical underwriting assessment",
                    requiredRoles = listOf("MEDICAL_UNDERWRITER"),
                    timeoutMinutes = 4320, // 72 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINANCIAL_UNDERWRITING",
                    stepName = "Financial Underwriting",
                    stepType = StepType.HUMAN_TASK,
                    description = "Financial underwriting assessment",
                    requiredRoles = listOf("FINANCIAL_UNDERWRITER"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FINAL_DECISION",
                    stepName = "Final Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final underwriting decision",
                    requiredRoles = listOf("SENIOR_UNDERWRITER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "POLICY_ISSUANCE",
                    stepName = "Policy Issuance",
                    stepType = StepType.AUTOMATED,
                    description = "Automated policy issuance",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 60,
                    autoAssign = false
                )
            )
        )

        // Claims Processing Workflow
        workflowDefinitions["CLAIMS_PROCESSING"] = WorkflowDefinition(
            workflowType = "CLAIMS_PROCESSING",
            name = "Claims Processing",
            description = "Complete workflow for processing insurance claims",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "CLAIM_INTAKE",
                    stepName = "Claim Intake",
                    stepType = StepType.AUTOMATED,
                    description = "Automated claim intake and validation",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 30,
                    autoAssign = false
                ),
                WorkflowStep(
                    stepId = "INITIAL_REVIEW",
                    stepName = "Initial Review",
                    stepType = StepType.HUMAN_TASK,
                    description = "Initial claim review and triage",
                    requiredRoles = listOf("CLAIMS_EXAMINER"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "INVESTIGATION",
                    stepName = "Investigation",
                    stepType = StepType.HUMAN_TASK,
                    description = "Detailed claim investigation",
                    requiredRoles = listOf("CLAIMS_INVESTIGATOR"),
                    timeoutMinutes = 2880, // 48 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "DECISION",
                    stepName = "Claim Decision",
                    stepType = StepType.HUMAN_TASK,
                    description = "Final claim decision",
                    requiredRoles = listOf("CLAIMS_MANAGER"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "PAYMENT_PROCESSING",
                    stepName = "Payment Processing",
                    stepType = StepType.AUTOMATED,
                    description = "Automated payment processing",
                    requiredRoles = emptyList(),
                    timeoutMinutes = 120,
                    autoAssign = false
                )
            )
        )

        // Customer Service Workflow
        workflowDefinitions["CUSTOMER_SERVICE"] = WorkflowDefinition(
            workflowType = "CUSTOMER_SERVICE",
            name = "Customer Service Request",
            description = "Workflow for handling customer service requests",
            version = "1.0",
            steps = listOf(
                WorkflowStep(
                    stepId = "REQUEST_TRIAGE",
                    stepName = "Request Triage",
                    stepType = StepType.HUMAN_TASK,
                    description = "Triage and categorize customer request",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 240, // 4 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "RESOLUTION",
                    stepName = "Issue Resolution",
                    stepType = StepType.HUMAN_TASK,
                    description = "Resolve customer issue",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP", "SPECIALIST"),
                    timeoutMinutes = 1440, // 24 hours
                    autoAssign = true
                ),
                WorkflowStep(
                    stepId = "FOLLOW_UP",
                    stepName = "Customer Follow-up",
                    stepType = StepType.HUMAN_TASK,
                    description = "Follow up with customer on resolution",
                    requiredRoles = listOf("CUSTOMER_SERVICE_REP"),
                    timeoutMinutes = 480, // 8 hours
                    autoAssign = true
                )
            )
        )
    }

    private fun executeCurrentStep(instance: WorkflowInstance) {
        val currentStep = instance.currentStep

        when (currentStep.stepType) {
            StepType.AUTOMATED -> {
                executeAutomatedStep(instance, currentStep)
            }
            StepType.HUMAN_TASK -> {
                createHumanTasks(instance, currentStep)
            }
            StepType.DECISION -> {
                executeDecisionStep(instance, currentStep)
            }
            StepType.PARALLEL -> {
                executeParallelStep(instance, currentStep)
            }
        }

        // Add step started event
        instance.history.add(WorkflowEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = EventType.STEP_STARTED,
            timestamp = LocalDateTime.now(),
            userId = "SYSTEM",
            description = "Step ${currentStep.stepName} started",
            data = mapOf("stepId" to currentStep.stepId)
        ))
    }

    private fun executeAutomatedStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Simulate automated processing
        when (step.stepId) {
            "CLAIM_INTAKE" -> {
                instance.context["claimValidated"] = true
                instance.context["claimAmount"] = 50000
            }
            "POLICY_ISSUANCE" -> {
                instance.context["policyNumber"] = "POL-${System.currentTimeMillis()}"
                instance.context["policyIssued"] = true
            }
            "PAYMENT_PROCESSING" -> {
                instance.context["paymentProcessed"] = true
                instance.context["paymentId"] = "PAY-${System.currentTimeMillis()}"
            }
        }

        // Automatically move to next step for automated tasks
        moveToNextStep(instance)
    }

    private fun createHumanTasks(instance: WorkflowInstance, step: WorkflowStep) {
        val taskId = UUID.randomUUID().toString()
        val assignee = if (step.autoAssign) {
            assignTaskToUser(step.requiredRoles.first())
        } else {
            null
        }

        val task = WorkflowTask(
            taskId = taskId,
            stepId = step.stepId,
            taskName = step.stepName,
            description = step.description,
            assignedTo = assignee,
            status = TaskStatus.PENDING,
            createdAt = LocalDateTime.now(),
            dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
            priority = determinePriority(instance, step),
            requiredRoles = step.requiredRoles,
            escalationLevel = 0
        )

        instance.pendingTasks.add(task)
    }

    private fun executeDecisionStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Decision logic based on context
        val decision = makeAutomatedDecision(instance, step)
        instance.context["decision"] = decision
        moveToNextStep(instance)
    }

    private fun executeParallelStep(instance: WorkflowInstance, step: WorkflowStep) {
        // Create multiple parallel tasks
        step.requiredRoles.forEach { role ->
            val taskId = UUID.randomUUID().toString()
            val task = WorkflowTask(
                taskId = taskId,
                stepId = step.stepId,
                taskName = "${step.stepName} - $role",
                description = step.description,
                assignedTo = assignTaskToUser(role),
                status = TaskStatus.PENDING,
                createdAt = LocalDateTime.now(),
                dueDate = LocalDateTime.now().plusMinutes(step.timeoutMinutes.toLong()),
                priority = determinePriority(instance, step),
                requiredRoles = listOf(role),
                escalationLevel = 0
            )
            instance.pendingTasks.add(task)
        }
    }

    private fun moveToNextStep(instance: WorkflowInstance) {
        val currentStepIndex = instance.definition.steps.indexOf(instance.currentStep)
        
        if (currentStepIndex < instance.definition.steps.size - 1) {
            // Move to next step
            instance.currentStep = instance.definition.steps[currentStepIndex + 1]
            executeCurrentStep(instance)
        } else {
            // Workflow completed
            instance.status = WorkflowStatus.COMPLETED
            instance.endTime = LocalDateTime.now()
            
            instance.history.add(WorkflowEvent(
                eventId = UUID.randomUUID().toString(),
                eventType = EventType.WORKFLOW_COMPLETED,
                timestamp = LocalDateTime.now(),
                userId = "SYSTEM",
                description = "Workflow completed successfully",
                data = emptyMap()
            ))
        }
    }

    private fun assignTaskToUser(role: String): String {
        // Simple round-robin assignment based on role
        return when (role) {
            "UNDERWRITER" -> "underwriter${(1..5).random()}"
            "MEDICAL_UNDERWRITER" -> "med_uw${(1..3).random()}"
            "FINANCIAL_UNDERWRITER" -> "fin_uw${(1..3).random()}"
            "SENIOR_UNDERWRITER" -> "senior_uw${(1..2).random()}"
            "CLAIMS_EXAMINER" -> "claims_exam${(1..10).random()}"
            "CLAIMS_INVESTIGATOR" -> "claims_inv${(1..5).random()}"
            "CLAIMS_MANAGER" -> "claims_mgr${(1..3).random()}"
            "CUSTOMER_SERVICE_REP" -> "csr${(1..15).random()}"
            "SPECIALIST" -> "specialist${(1..5).random()}"
            else -> "user${(1..10).random()}"
        }
    }

    private fun determinePriority(instance: WorkflowInstance, step: WorkflowStep): TaskPriority {
        return when {
            instance.context["urgent"] == true -> TaskPriority.HIGH
            instance.context["claimAmount"] as? Int ?: 0 > 100000 -> TaskPriority.HIGH
            step.timeoutMinutes < 480 -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }
    }

    private fun makeAutomatedDecision(instance: WorkflowInstance, step: WorkflowStep): String {
        // Simple decision logic based on context
        return when {
            instance.context["riskScore"] as? Double ?: 0.0 > 0.8 -> "DECLINE"
            instance.context["claimAmount"] as? Int ?: 0 > 500000 -> "INVESTIGATE"
            else -> "APPROVE"
        }
    }

    private fun getEscalationTarget(currentAssignee: String): String {
        // Simple escalation logic
        return when {
            currentAssignee.startsWith("underwriter") -> "senior_uw1"
            currentAssignee.startsWith("claims_exam") -> "claims_mgr1"
            currentAssignee.startsWith("csr") -> "supervisor1"
            else -> "manager1"
        }
    }

    private fun calculateTaskMetrics(): TaskMetrics {
        val allTasks = activeWorkflows.values.flatMap { it.pendingTasks }
        val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED }
        val pendingTasks = allTasks.filter { it.status == TaskStatus.PENDING }
        val overdueTasks = pendingTasks.filter { it.dueDate.isBefore(LocalDateTime.now()) }

        val averageCompletionTime = completedTasks
            .filter { it.completedAt != null }
            .map { java.time.Duration.between(it.createdAt, it.completedAt).toMinutes() }
            .average()

        return TaskMetrics(
            totalTasks = allTasks.size,
            completedTasks = completedTasks.size,
            pendingTasks = pendingTasks.size,
            overdueTasks = overdueTasks.size,
            averageCompletionTimeMinutes = averageCompletionTime,
            tasksByPriority = allTasks.groupBy { it.priority }.mapValues { it.value.size },
            tasksByStatus = allTasks.groupBy { it.status }.mapValues { it.value.size }
        )
    }
}

// Data classes for workflow management
data class WorkflowDefinition(
    val workflowType: String,
    val name: String,
    val description: String,
    val version: String,
    val steps: List<WorkflowStep>
)

data class WorkflowStep(
    val stepId: String,
    val stepName: String,
    val stepType: StepType,
    val description: String,
    val requiredRoles: List<String>,
    val timeoutMinutes: Int,
    val autoAssign: Boolean
)

data class WorkflowInstance(
    val workflowId: String,
    val workflowType: String,
    val definition: WorkflowDefinition,
    var status: WorkflowStatus,
    val initiator: String,
    val context: MutableMap<String, Any>,
    var currentStep: WorkflowStep,
    val startTime: LocalDateTime,
    var endTime: LocalDateTime? = null,
    val pendingTasks: MutableList<WorkflowTask> = mutableListOf(),
    val history: MutableList<WorkflowEvent> = mutableListOf()
)

data class WorkflowTask(
    val taskId: String,
    val stepId: String,
    val taskName: String,
    val description: String,
    var assignedTo: String?,
    var status: TaskStatus,
    val createdAt: LocalDateTime,
    val dueDate: LocalDateTime,
    val priority: TaskPriority,
    val requiredRoles: List<String>,
    var escalationLevel: Int,
    var completedBy: String? = null,
    var completedAt: LocalDateTime? = null,
    var escalatedAt: LocalDateTime? = null,
    var result: TaskResult? = null
)

data class TaskResult(
    val decision: String,
    val comments: String,
    val data: Map<String, Any>
)

data class WorkflowEvent(
    val eventId: String,
    val eventType: EventType,
    val timestamp: LocalDateTime,
    val userId: String,
    val description: String,
    val data: Map<String, Any>
)

data class WorkflowMetrics(
    val totalWorkflows: Int,
    val runningWorkflows: Int,
    val completedWorkflows: Int,
    val cancelledWorkflows: Int,
    val averageCompletionTimeMinutes: Double,
    val taskMetrics: TaskMetrics,
    val workflowsByType: Map<String, Int>
)

data class TaskMetrics(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    val averageCompletionTimeMinutes: Double,
    val tasksByPriority: Map<TaskPriority, Int>,
    val tasksByStatus: Map<TaskStatus, Int>
)

// Enums
enum class WorkflowStatus {
    RUNNING, COMPLETED, CANCELLED, SUSPENDED, ERROR
}

enum class StepType {
    AUTOMATED, HUMAN_TASK, DECISION, PARALLEL
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ESCALATED
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class EventType {
    WORKFLOW_STARTED, WORKFLOW_COMPLETED, WORKFLOW_CANCELLED,
    STEP_STARTED, STEP_COMPLETED,
    TASK_CREATED, TASK_ASSIGNED, TASK_COMPLETED, TASK_ESCALATED, TASK_REASSIGNED,
    COMMENT_ADDED, ERROR_OCCURRED
}
