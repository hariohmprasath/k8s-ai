package com.mcp.server.tools

import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.models.V1Job
import mu.KotlinLogging
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class JobTools(private val batchV1Api: BatchV1Api) {

    @Tool(name = "list_jobs", description = "List all jobs in a namespace")
    fun listJobs(@ToolParam(description = "The Kubernetes namespace to list jobs from") namespace: String): String {
        return try {
            val jobs = batchV1Api.listNamespacedJob(namespace, null, null, null, null, null, null, null, null, null, null)
            formatJobList(jobs.items)
        } catch (e: ApiException) {
            "Error listing jobs: ${e.message}"
        }
    }

    @Tool(name = "get_job_status", description = "Get detailed status of a specific job")
    fun getJobStatus(
        @ToolParam(description = "Name of the job") jobName: String,
        @ToolParam(description = "The Kubernetes namespace of the job") namespace: String
    ): String {
        return try {
            val job = batchV1Api.readNamespacedJob(jobName, namespace, null)
            formatJobStatus(job)
        } catch (e: ApiException) {
            "Error getting job status: ${e.message}"
        }
    }

    @Tool(name = "delete_job", description = "Delete a job from the cluster")
    fun deleteJob(
        @ToolParam(description = "Name of the job to delete") jobName: String,
        @ToolParam(description = "The Kubernetes namespace of the job") namespace: String
    ): String {
        return try {
            batchV1Api.deleteNamespacedJob(jobName, namespace, null, null, null, null, null, null)
            "Successfully deleted job $jobName in namespace $namespace"
        } catch (e: ApiException) {
            "Error deleting job: ${e.message}"
        }
    }

    private fun formatJobList(jobs: List<V1Job>): String {
        if (jobs.isEmpty()) return "No jobs found in the namespace"
        
        return buildString {
            appendLine("Found ${jobs.size} jobs:")
            jobs.forEach { job ->
                val name = job.metadata?.name ?: "unknown"
                val completions = job.status?.succeeded ?: 0
                val failed = job.status?.failed ?: 0
                val active = job.status?.active ?: 0
                val startTime = job.status?.startTime?.toString() ?: "N/A"
                val completionTime = job.status?.completionTime?.toString() ?: "N/A"
                
                appendLine("Job: $name")
                appendLine("  - Status: Active: $active, Succeeded: $completions, Failed: $failed")
                appendLine("  - Start Time: $startTime")
                appendLine("  - Completion Time: $completionTime")
                appendLine()
            }
        }
    }

    private fun formatJobStatus(job: V1Job): String {
        val name = job.metadata?.name ?: "unknown"
        val namespace = job.metadata?.namespace ?: "unknown"
        val completions = job.spec?.completions ?: 0
        val parallelism = job.spec?.parallelism ?: 0
        val succeeded = job.status?.succeeded ?: 0
        val failed = job.status?.failed ?: 0
        val active = job.status?.active ?: 0
        val startTime = job.status?.startTime?.toString() ?: "N/A"
        val completionTime = job.status?.completionTime?.toString() ?: "N/A"

        return buildString {
            appendLine("Job Details for $name in namespace $namespace:")
            appendLine("Specifications:")
            appendLine("  - Desired Completions: $completions")
            appendLine("  - Parallelism: $parallelism")
            appendLine("\nStatus:")
            appendLine("  - Active: $active")
            appendLine("  - Succeeded: $succeeded")
            appendLine("  - Failed: $failed")
            appendLine("  - Start Time: $startTime")
            appendLine("  - Completion Time: $completionTime")
            
            job.status?.conditions?.forEach { condition ->
                appendLine("\nCondition: ${condition.type}")
                appendLine("  - Status: ${condition.status}")
                appendLine("  - Last Transition: ${condition.lastTransitionTime}")
                if (condition.message != null) {
                    appendLine("  - Message: ${condition.message}")
                }
            }
        }
    }
}
