package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.AppsV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class HealthTools(
    private val coreV1Api: CoreV1Api,
    private val appsV1Api: AppsV1Api
) {
    @Tool(name = "checkClusterHealth", description = "Check overall cluster health")
    fun checkClusterHealth(): String {
        return try {
            val nodes = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null)
            val pods = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            val deployments = appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            
            val nodeHealth = nodes.items.map { node ->
                val conditions = node.status?.conditions ?: emptyList()
                val ready = conditions.find { it.type == "Ready" }?.status == "True"
                val problems = conditions.filter { 
                    it.type != "Ready" && it.status == "True" 
                }.map { it.type }
                
                "${node.metadata?.name}: ${if (ready) "Ready" else "Not Ready"}" +
                        if (problems.isNotEmpty()) " (Issues: ${problems.joinToString(", ")})" else ""
            }
            
            val podIssues = pods.items.filter { pod ->
                pod.status?.phase != "Running" && pod.status?.phase != "Succeeded"
            }.map { pod ->
                "${pod.metadata?.namespace}/${pod.metadata?.name}: ${pod.status?.phase}" +
                        (pod.status?.reason?.let { " ($it)" } ?: "")
            }
            
            val deploymentIssues = deployments.items.filter { deployment ->
                (deployment.status?.readyReplicas ?: 0) < (deployment.status?.replicas ?: 0)
            }.map { deployment ->
                "${deployment.metadata?.namespace}/${deployment.metadata?.name}: " +
                        "Ready: ${deployment.status?.readyReplicas ?: 0}/${deployment.status?.replicas ?: 0}"
            }
            
            """
            Cluster Health Check:
            
            Nodes (${nodes.items.size}):
            ${nodeHealth.joinToString("\n") { "  - $it" }}
            
            Pod Issues (${podIssues.size}):
            ${if (podIssues.isEmpty()) "  None" else podIssues.joinToString("\n") { "  - $it" }}
            
            Deployment Issues (${deploymentIssues.size}):
            ${if (deploymentIssues.isEmpty()) "  None" else deploymentIssues.joinToString("\n") { "  - $it" }}
            
            Summary:
              - Nodes: ${nodes.items.count { it.status?.conditions?.find { c -> c.type == "Ready" }?.status == "True" }}/${nodes.items.size} ready
              - Pods: ${pods.items.count { it.status?.phase == "Running" || it.status?.phase == "Succeeded" }}/${pods.items.size} healthy
              - Deployments: ${deployments.items.count { (it.status?.readyReplicas ?: 0) >= (it.status?.replicas ?: 0) }}/${deployments.items.size} healthy
            """.trimIndent()
        } catch (e: Exception) {
            "Error checking cluster health: ${e.message}"
        }
    }

    @Tool(name = "getFailedWorkloads", description = "List all failed pods/jobs in a namespace")
    fun getFailedWorkloads(
        @ToolParam(description = "The Kubernetes namespace to check for failed workloads") 
        namespace: String = "default"
    ): String {
        return try {
            val pods = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
            val failedPods = pods.items.filter { pod ->
                pod.status?.phase == "Failed" || 
                pod.status?.containerStatuses?.any { 
                    it.state?.waiting?.reason in listOf("CrashLoopBackOff", "Error", "ImagePullBackOff") ||
                    it.state?.terminated?.exitCode != 0
                } == true
            }
            
            if (failedPods.isEmpty()) {
                "No failed workloads found in namespace $namespace"
            } else {
                """
                Failed Workloads in namespace $namespace:
                
                ${failedPods.joinToString("\n\n") { pod ->
                    """
                    Pod: ${pod.metadata?.name}
                    Phase: ${pod.status?.phase}
                    Reason: ${pod.status?.reason ?: "N/A"}
                    Message: ${pod.status?.message ?: "N/A"}
                    
                    Container Status:
                    ${pod.status?.containerStatuses?.joinToString("\n") { container ->
                        """
                        - ${container.name}:
                          Ready: ${container.ready}
                          RestartCount: ${container.restartCount}
                          ${when {
                              container.state?.waiting != null -> "Waiting: ${container.state?.waiting?.reason} (${container.state?.waiting?.message ?: "N/A"})"
                              container.state?.terminated != null -> "Terminated: Exit ${container.state?.terminated?.exitCode} (${container.state?.terminated?.reason ?: "N/A"})"
                              else -> "Running"
                          }}
                        """.trimIndent()
                    } ?: "No container status available"}
                    """.trimIndent()
                }}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error getting failed workloads: ${e.message}"
        }
    }

    @Tool(name = "analyzeResourceBottlenecks", description = "Identify resource constraints in a namespace")
    fun analyzeResourceBottlenecks(
        @ToolParam(description = "The Kubernetes namespace to analyze for resource bottlenecks") 
        namespace: String = "default"
    ): String {
        return try {
            val pods = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
            val events = coreV1Api.listNamespacedEvent(namespace, null, null, null, null, null, null, null, null, null, null)
            
            val resourceIssues = mutableListOf<String>()
            
            // Check for resource-related pod issues
            pods.items.forEach { pod ->
                val podName = pod.metadata?.name ?: "unknown"
                
                // Check container resource usage and limits
                pod.spec?.containers?.forEach { container ->
                    val requests = container.resources?.requests
                    val limits = container.resources?.limits
                    
                    if (requests == null || limits == null) {
                        resourceIssues.add("$podName/${container.name}: No resource requests/limits defined")
                    }
                }
                
                // Check for resource-related status conditions
                pod.status?.conditions?.forEach { condition ->
                    if (condition.type == "PodScheduled" && condition.status == "False" && 
                        condition.reason == "Unschedulable" && condition.message?.contains("Insufficient") == true) {
                        resourceIssues.add("$podName: ${condition.message}")
                    }
                }
            }
            
            // Check for resource-related events
            val resourceEvents = events.items.filter { event ->
                event.reason in listOf(
                    "FailedScheduling",
                    "OutOfmemory",
                    "OutOfcpu",
                    "BackOff"
                ) && event.message?.contains("Insufficient") == true
            }
            
            """
            Resource Bottleneck Analysis for namespace $namespace:
            
            Resource Configuration Issues:
            ${if (resourceIssues.isEmpty()) "None found" else resourceIssues.joinToString("\n") { "- $it" }}
            
            Recent Resource-Related Events:
            ${if (resourceEvents.isEmpty()) "None found" else resourceEvents.joinToString("\n") { event ->
                """
                - Time: ${event.lastTimestamp}
                  Resource: ${event.involvedObject.kind}/${event.involvedObject.name}
                  Issue: ${event.reason}
                  Message: ${event.message}
                """.trimIndent()
            }}
            
            Recommendations:
            ${if (resourceIssues.isEmpty() && resourceEvents.isEmpty())
                "No immediate resource bottlenecks detected"
            else
                """
                1. Review and adjust resource requests/limits for pods with issues
                2. Consider increasing cluster capacity if resource constraints are frequent
                3. Implement horizontal pod autoscaling for workloads with variable resource needs
                4. Review pod scheduling and node affinity rules
                """.trimIndent()
            }
            """.trimIndent()
        } catch (e: Exception) {
            "Error analyzing resource bottlenecks: ${e.message}"
        }
    }
}
