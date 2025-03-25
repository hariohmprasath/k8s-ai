package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class NodeTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "list_nodes", description = "Lists all Kubernetes nodes in the cluster")
    fun listNodes(): List<String> {
        return try {
            coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null)
                .items
                .map { node ->
                    "${node.metadata?.name}" +
                            "\n  - Ready: ${node.status?.conditions?.find { it.type == "Ready" }?.status == "True"}" +
                            "\n  - Roles: ${node.metadata?.labels?.filterKeys { it.startsWith("node-role.kubernetes.io/") }?.keys?.map { it.removePrefix("node-role.kubernetes.io/") } ?: listOf("none")}" +
                            "\n  - Internal IP: ${node.status?.addresses?.find { it.type == "InternalIP" }?.address ?: "N/A"}" +
                            "\n  - OS Image: ${node.status?.nodeInfo?.osImage ?: "N/A"}" +
                            "\n  - Kubernetes Version: ${node.status?.nodeInfo?.kubeletVersion ?: "N/A"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describe_node", description = "Get detailed information about a specific node")
    fun describeNode(
        @ToolParam(description = "Name of the node to describe") 
        nodeName: String
    ): String {
        return try {
            val node = coreV1Api.readNode(nodeName, null)
            """
            Node: ${node.metadata?.name}
            Labels: ${node.metadata?.labels?.entries?.joinToString(", ") { "${it.key}=${it.value}" }}
            
            Status:
              Conditions:
            ${node.status?.conditions?.joinToString("\n") { "    - ${it.type}: ${it.status} (${it.message})" }}
              
              Capacity:
            ${node.status?.capacity?.entries?.joinToString("\n") { "    - ${it.key}: ${it.value}" }}
              
              Allocatable:
            ${node.status?.allocatable?.entries?.joinToString("\n") { "    - ${it.key}: ${it.value}" }}
              
              System Info:
                OS Image: ${node.status?.nodeInfo?.osImage}
                Container Runtime: ${node.status?.nodeInfo?.containerRuntimeVersion}
                Kubelet Version: ${node.status?.nodeInfo?.kubeletVersion}
                Kube-Proxy Version: ${node.status?.nodeInfo?.kubeProxyVersion}
                Operating System: ${node.status?.nodeInfo?.operatingSystem}
                Architecture: ${node.status?.nodeInfo?.architecture}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing node: ${e.message}"
        }
    }

    @Tool(name = "get_node_metrics", description = "Get resource usage metrics for a specific node")
    fun getNodeMetrics(
        @ToolParam(description = "Name of the node to get metrics for") 
        nodeName: String
    ): String {
        return try {
            val node = coreV1Api.readNode(nodeName, null)
            val allocatable = node.status?.allocatable
            val capacity = node.status?.capacity
            val pods = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            val nodePods = pods.items.filter { it.spec?.nodeName == nodeName }
            
            val usedCPU = nodePods.sumOf { pod ->
                pod.spec?.containers?.sumOf { container ->
                    container.resources?.requests?.get("cpu")?.number?.toDouble() ?: 0.0
                } ?: 0.0
            }
            
            val usedMemory = nodePods.sumOf { pod ->
                pod.spec?.containers?.sumOf { container ->
                    container.resources?.requests?.get("memory")?.number?.toDouble() ?: 0.0
                } ?: 0.0
            }
            
            """
            Node: ${node.metadata?.name}
            
            Capacity:
              CPU: ${capacity?.get("cpu") ?: "N/A"}
              Memory: ${capacity?.get("memory") ?: "N/A"}
              Pods: ${capacity?.get("pods") ?: "N/A"}
              Ephemeral Storage: ${capacity?.get("ephemeral-storage") ?: "N/A"}
            
            Allocatable:
              CPU: ${allocatable?.get("cpu") ?: "N/A"}
              Memory: ${allocatable?.get("memory") ?: "N/A"}
              Pods: ${allocatable?.get("pods") ?: "N/A"}
              Ephemeral Storage: ${allocatable?.get("ephemeral-storage") ?: "N/A"}
            
            Current Usage:
              CPU: $usedCPU
              Memory: ${usedMemory}GB
              Running Pods: ${nodePods.count { it.status?.phase == "Running" }}
            
            Conditions:
            ${node.status?.conditions?.joinToString("\n") { condition ->
                "  - ${condition.type}: ${condition.status} (Last update: ${condition.lastTransitionTime})"
            } ?: "  No conditions available"}
            
            System Info:
              OS Image: ${node.status?.nodeInfo?.osImage ?: "N/A"}
              Container Runtime: ${node.status?.nodeInfo?.containerRuntimeVersion ?: "N/A"}
              Kubelet Version: ${node.status?.nodeInfo?.kubeletVersion ?: "N/A"}
              Kernel Version: ${node.status?.nodeInfo?.kernelVersion ?: "N/A"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting node metrics: ${e.message}"
        }
    }
    
    @Tool(name = "get_node_events", description = "Get recent events for a specific node")
    fun getNodeEvents(
        @ToolParam(description = "Name of the node to get events for") 
        nodeName: String
    ): String {
        return try {
            val events = coreV1Api.listEventForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            val now = OffsetDateTime.now()
            
            val nodeEvents = events.items
                .filter { event ->
                    event.involvedObject.kind == "Node" &&
                    event.involvedObject.name == nodeName &&
                    event.lastTimestamp?.let { timestamp ->
                        ChronoUnit.HOURS.between(timestamp, now) <= 24
                    } ?: true
                }
                .sortedByDescending { it.lastTimestamp }
            
            if (nodeEvents.isEmpty()) {
                "No events found for node $nodeName in the last 24 hours"
            } else {
                """
                Recent events for node $nodeName:
                
                ${nodeEvents.joinToString("\n\n") { event ->
                    """
                    Time: ${event.lastTimestamp}
                    Type: ${event.type}
                    Reason: ${event.reason}
                    Message: ${event.message}
                    Count: ${event.count ?: 1}
                    Component: ${event.source?.component ?: "N/A"}
                    """.trimIndent()
                }}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error getting node events: ${e.message}"
        }
    }
    
    @Tool(name = "drain_node", description = "Mark a node as unschedulable and evict pods for maintenance")
    fun drainNode(
        @ToolParam(description = "Name of the node to drain") 
        nodeName: String
    ): String {
        return try {
            // Mark node as unschedulable
            val node = coreV1Api.readNode(nodeName, null)
            node.spec?.unschedulable = true
            coreV1Api.replaceNode(nodeName, node, null, null, null, null)
            
            // Get pods on the node
            val pods = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null)
            val nodePods = pods.items.filter { it.spec?.nodeName == nodeName }
            
            """
            Node $nodeName marked as unschedulable.
            Found ${nodePods.size} pods to evict.
            
            Note: Manual pod eviction required. Please use kubectl to evict pods:
            ${nodePods.joinToString("\n") { pod ->
                "kubectl delete pod ${pod.metadata?.name} -n ${pod.metadata?.namespace}"
            }}
            
            To make the node schedulable again, run:
            kubectl uncordon $nodeName
            """.trimIndent()
        } catch (e: Exception) {
            "Error draining node: ${e.message}"
        }
    }
}
