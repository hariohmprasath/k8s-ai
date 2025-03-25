package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.CoreV1Event
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class EventTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "get_recent_events", description = "Get recent events from a namespace for troubleshooting with severity analysis")
    fun getRecentEvents(
        @ToolParam(description = "The Kubernetes namespace to get events from") 
        namespace: String = "default"
    ): String {
        return try {
            val events = coreV1Api.listNamespacedEvent(namespace, null, null, null, null, null, null, null, null, null, null)
            val now = OffsetDateTime.now()
            
            val recentEvents = events.items
                .filter { event: CoreV1Event ->
                    event.lastTimestamp?.let { timestamp ->
                        ChronoUnit.HOURS.between(timestamp, now) <= 1
                    } ?: true
                }
                .sortedByDescending { it.lastTimestamp }

            if (recentEvents.isEmpty()) {
                "No events found in namespace $namespace in the last hour"
            } else {
                val criticalEvents = mutableListOf<String>()
                val warningEvents = mutableListOf<String>()
                val normalEvents = mutableListOf<String>()

                recentEvents.forEach { event: CoreV1Event ->
                    val eventSummary = """
                        Time: ${event.lastTimestamp}
                        Type: ${event.type}
                        Reason: ${event.reason}
                        Object: ${event.involvedObject.kind}/${event.involvedObject.name}
                        Message: ${event.message}
                        Count: ${event.count ?: 1}
                        Component: ${event.source?.component ?: "N/A"}
                        """.trimIndent()

                    when {
                        event.type == "Warning" && isCriticalReason(event.reason) -> criticalEvents.add(eventSummary)
                        event.type == "Warning" -> warningEvents.add(eventSummary)
                        else -> normalEvents.add(eventSummary)
                    }
                }

                val analysis = analyzeEvents(criticalEvents.size, warningEvents.size, normalEvents.size)
                
                """
                Event Analysis for namespace $namespace:
                $analysis
                
                ${if (criticalEvents.isNotEmpty()) """
                Critical Events (Require Immediate Attention):
                ${criticalEvents.joinToString("\n\n")}
                """ else ""}
                
                ${if (warningEvents.isNotEmpty()) """
                Warning Events:
                ${warningEvents.joinToString("\n\n")}
                """ else ""}
                
                ${if (normalEvents.isNotEmpty()) """
                Normal Events:
                ${normalEvents.joinToString("\n\n")}
                """ else ""}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error getting events: ${e.message}"
        }
    }

    private fun isCriticalReason(reason: String?): Boolean {
        return reason in setOf(
            "Failed",
            "FailedCreate",
            "FailedScheduling",
            "BackOff",
            "Error",
            "NodeNotReady",
            "KillContainer"
        )
    }

    private fun analyzeEvents(criticalCount: Int, warningCount: Int, normalCount: Int): String {
        val total = criticalCount + warningCount + normalCount
        val recommendations = mutableListOf<String>()

        if (criticalCount > 0) {
            recommendations.add("Critical events detected! Immediate attention required.")
        }

        if (warningCount > normalCount && warningCount > 2) {
            recommendations.add("High number of warning events. System might be unstable.")
        }

        if (criticalCount + warningCount == 0 && normalCount > 0) {
            recommendations.add("System appears to be healthy.")
        }

        return """
            Summary:
            - Total Events: $total
            - Critical Events: $criticalCount
            - Warning Events: $warningCount
            - Normal Events: $normalCount
            
            Recommendations:
            ${recommendations.joinToString("\n") { rec -> "- $rec" }}
            """.trimIndent()
    }

    @Tool(name = "get_resource_events", description = "Get events for a specific resource")
    fun getResourceEvents(
        @ToolParam(description = "Type of resource (Pod, Deployment, etc)") 
        resourceType: String,
        @ToolParam(description = "Name of the resource") 
        resourceName: String,
        @ToolParam(description = "The Kubernetes namespace where the resource is located") 
        namespace: String = "default"
    ): String {
        return try {
            val events = coreV1Api.listNamespacedEvent(namespace, null, null, null, null, null, null, null, null, null, null)
            
            val resourceEvents = events.items
                .filter { event: CoreV1Event ->
                    event.involvedObject.kind.equals(resourceType, ignoreCase = true) &&
                    event.involvedObject.name == resourceName
                }
                .sortedByDescending { it.lastTimestamp }
            
            if (resourceEvents.isEmpty()) {
                "No events found for $resourceType/$resourceName in namespace $namespace"
            } else {
                """
                Events for $resourceType/$resourceName in namespace $namespace:
                
                ${resourceEvents.joinToString("\n\n") { event: CoreV1Event ->
                    """
                    Time: ${event.lastTimestamp}
                    Type: ${event.type}
                    Reason: ${event.reason}
                    Message: ${event.message}
                    Count: ${event.count ?: 1}
                    Component: ${event.source?.component ?: "N/A"}
                    Host: ${event.source?.host ?: "N/A"}
                    """.trimIndent()
                }}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error getting resource events: ${e.message}"
        }
    }
}
