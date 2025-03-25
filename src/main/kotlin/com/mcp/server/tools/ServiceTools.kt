package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class ServiceTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "list_services", description = "Lists all Kubernetes services in the specified namespace")
    fun listServices(
        @ToolParam(description = "The Kubernetes namespace to list services from") 
        namespace: String = "default"
    ): List<String> {
        return try {
            coreV1Api.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { service ->
                    "${service.metadata?.name}" +
                            "\n  - Type: ${service.spec?.type ?: "N/A"}" +
                            "\n  - Cluster IP: ${service.spec?.clusterIP ?: "N/A"}" +
                            "\n  - External IP: ${service.status?.loadBalancer?.ingress?.firstOrNull()?.ip ?: "N/A"}" +
                            "\n  - Ports: ${service.spec?.ports?.joinToString(", ") { "${it.port}:${it.targetPort}/${it.protocol}" } ?: "N/A"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describe_service", description = "Get detailed information about a specific service")
    fun describeService(
        @ToolParam(description = "Name of the service to describe") 
        serviceName: String,
        @ToolParam(description = "The Kubernetes namespace where the service is located") 
        namespace: String = "default"
    ): String {
        return try {
            val service = coreV1Api.readNamespacedService(serviceName, namespace, null)
            """
            Service: ${service.metadata?.name}
            Namespace: ${service.metadata?.namespace}
            Labels: ${service.metadata?.labels?.entries?.joinToString(", ") { "${it.key}=${it.value}" }}
            
            Spec:
              Type: ${service.spec?.type}
              Cluster IP: ${service.spec?.clusterIP}
              External IPs: ${service.spec?.externalIPs?.joinToString(", ") ?: "none"}
              Ports:
            ${service.spec?.ports?.joinToString("\n") { port ->
                "    - ${port.name ?: "unnamed"}: ${port.port}:${port.targetPort}/${port.protocol}"
            }}
              
              Selector:
            ${service.spec?.selector?.entries?.joinToString("\n") { "    ${it.key}: ${it.value}" }}
              
            Status:
              Load Balancer:
                Ingress:
            ${service.status?.loadBalancer?.ingress?.joinToString("\n") { ingress ->
                "    - IP: ${ingress.ip ?: "N/A"}, Hostname: ${ingress.hostname ?: "N/A"}"
            } ?: "    None"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing service: ${e.message}"
        }
    }

    @Tool(name = "get_service_endpoints", description = "Get endpoints (pod IPs) for a specific service")
    fun getServiceEndpoints(
        @ToolParam(description = "Name of the service to get endpoints for") 
        serviceName: String,
        @ToolParam(description = "The Kubernetes namespace where the service is located") 
        namespace: String = "default"
    ): String {
        return try {
            val endpoints = coreV1Api.readNamespacedEndpoints(serviceName, namespace, null)
            """
            Service: $serviceName
            Endpoints:
            ${endpoints.subsets?.flatMap { subset ->
                subset.addresses?.map { address ->
                    "  - ${address.ip} (${address.targetRef?.name ?: "unknown"})"
                } ?: emptyList()
            }?.joinToString("\n") ?: "No endpoints found"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting service endpoints: ${e.message}"
        }
    }
}
