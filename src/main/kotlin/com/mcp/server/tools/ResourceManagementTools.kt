package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.custom.Quantity
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class ResourceManagementTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "get_namespace_resource_quotas", description = "Get resource quotas for a namespace")
    fun getNamespaceResourceQuotas(
        @ToolParam(description = "The Kubernetes namespace to get resource quotas from") 
        namespace: String = "default"
    ): String {
        return try {
            val quotas = coreV1Api.listNamespacedResourceQuota(namespace, null, null, null, null, null, null, null, null, null, null)
            val quotaStrings = quotas.items.map { quota ->
                """
                ResourceQuota: ${quota.metadata?.name}
                Status:
                ${quota.status?.hard?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No hard limits defined"}
                
                Usage:
                ${quota.status?.used?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No usage data"}
                """.trimIndent()
            }
            
            if (quotaStrings.isEmpty()) "No resource quotas found in namespace $namespace"
            else quotaStrings.joinToString("\n\n")
        } catch (e: Exception) {
            "Error getting resource quotas: ${e.message}"
        }
    }

    @Tool(name = "describe_limit_range", description = "Get limit range details for a namespace")
    fun describeLimitRange(
        @ToolParam(description = "Name of the LimitRange to describe") 
        name: String,
        @ToolParam(description = "The Kubernetes namespace where the LimitRange is located") 
        namespace: String = "default"
    ): String {
        return try {
            val limitRange = coreV1Api.readNamespacedLimitRange(name, namespace, null)
            """
            LimitRange: ${limitRange.metadata?.name}
            Namespace: $namespace
            
            Limits:
            ${limitRange.spec?.limits?.joinToString("\n\n") { limit ->
                """
                Type: ${limit.type}
                Default:
                ${limit.default?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No defaults defined"}
                
                DefaultRequest:
                ${limit.defaultRequest?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No default requests defined"}
                
                Max:
                ${limit.max?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No max limits defined"}
                
                Min:
                ${limit.min?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No min limits defined"}
                """.trimIndent()
            } ?: "No limits defined"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing LimitRange: ${e.message}"
        }
    }

    @Tool(name = "get_cluster_resource_usage", description = "Get overall cluster resource utilization")
    fun getClusterResourceUsage(): String {
        return try {
            val nodes = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null)
            val pods = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            
            var totalCPU = 0.0
            var totalMemory = 0.0
            var usedCPU = 0.0
            var usedMemory = 0.0
            
            nodes.items.forEach { node ->
                node.status?.capacity?.get("cpu")?.number?.toDouble()?.let { totalCPU += it }
                node.status?.capacity?.get("memory")?.number?.toDouble()?.let { totalMemory += it }
            }
            
            pods.items.forEach { pod ->
                pod.spec?.containers?.forEach { container ->
                    container.resources?.requests?.get("cpu")?.number?.toDouble()?.let { usedCPU += it }
                    container.resources?.requests?.get("memory")?.number?.toDouble()?.let { usedMemory += it }
                }
            }
            
            """
            Cluster Resource Usage:
            
            CPU:
              Total: $totalCPU
              Used: $usedCPU
              Usage: ${(usedCPU.toString().toDoubleOrNull() ?: 0.0) / (totalCPU.toString().toDoubleOrNull() ?: 1.0) * 100}%
            
            Memory:
              Total: $totalMemory
              Used: $usedMemory
              Usage: ${(usedMemory.toString().toDoubleOrNull() ?: 0.0) / (totalMemory.toString().toDoubleOrNull() ?: 1.0) * 100}%
            
            Nodes: ${nodes.items.size}
            Pods: ${pods.items.size}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting cluster resource usage: ${e.message}"
        }
    }

    @Tool(name = "get_namespace_resource_usage", description = "Get resource usage for a specific namespace")
    fun getNamespaceResourceUsage(
        @ToolParam(description = "The Kubernetes namespace to get resource usage from") 
        namespace: String = "default"
    ): String {
        return try {
            val pods = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
            
            var cpuRequests = 0.0
            var memoryRequests = 0.0
            var cpuLimits = 0.0
            var memoryLimits = 0.0
            
            pods.items.forEach { pod ->
                pod.spec?.containers?.forEach { container ->
                    container.resources?.requests?.get("cpu")?.toString()?.toDoubleOrNull()?.let { cpuRequests += it }
                    container.resources?.requests?.get("memory")?.toString()?.toDoubleOrNull()?.let { memoryRequests += it }
                    container.resources?.limits?.get("cpu")?.toString()?.toDoubleOrNull()?.let { cpuLimits += it }
                    container.resources?.limits?.get("memory")?.toString()?.toDoubleOrNull()?.let { memoryLimits += it }
                }
            }
            
            """
            Namespace Resource Usage: $namespace
            
            CPU:
              Requests: $cpuRequests
              Limits: $cpuLimits
            
            Memory:
              Requests: $memoryRequests
              Limits: $memoryLimits
            
            Total Pods: ${pods.items.size}
            Running Pods: ${pods.items.count { it.status?.phase == "Running" }}
            Pending Pods: ${pods.items.count { it.status?.phase == "Pending" }}
            Failed Pods: ${pods.items.count { it.status?.phase == "Failed" }}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting namespace resource usage: ${e.message}"
        }
    }
}
