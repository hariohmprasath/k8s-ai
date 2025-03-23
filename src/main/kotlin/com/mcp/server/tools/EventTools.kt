package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class EventTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "getRecentEvents", description = "Get recent events from a namespace for troubleshooting")
    fun getRecentEvents(
        @ToolParam(description = "The Kubernetes namespace to get events from") 
        namespace: String = "default"
    ): String {
        return try {
            val events = coreV1Api.listNamespacedEvent(namespace, null, null, null, null, null, null, null, null, null, null)
            val now = OffsetDateTime.now()
            
            val recentEvents = events.items
                .filter { event ->
                    event.lastTimestamp?.let { timestamp ->
                        ChronoUnit.HOURS.between(timestamp, now) <= 1
                    } ?: true
                }
                .sortedByDescending { it.lastTimestamp }
            
            if (recentEvents.isEmpty()) {
                "No events found in namespace $namespace in the last hour"
            } else {
                """
                Recent Events in namespace $namespace:
                
                ${recentEvents.joinToString("\n\n") { event ->
                    """
                    Time: ${event.lastTimestamp}
                    Type: ${event.type}
                    Reason: ${event.reason}
                    Object: ${event.involvedObject.kind}/${event.involvedObject.name}
                    Message: ${event.message}
                    Count: ${event.count ?: 1}
                    """.trimIndent()
                }}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error getting events: ${e.message}"
        }
    }

    @Tool(name = "getResourceEvents", description = "Get events for a specific resource")
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
                .filter { event ->
                    event.involvedObject.kind.equals(resourceType, ignoreCase = true) &&
                    event.involvedObject.name == resourceName
                }
                .sortedByDescending { it.lastTimestamp }
            
            if (resourceEvents.isEmpty()) {
                "No events found for $resourceType/$resourceName in namespace $namespace"
            } else {
                """
                Events for $resourceType/$resourceName in namespace $namespace:
                
                ${resourceEvents.joinToString("\n\n") { event ->
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
