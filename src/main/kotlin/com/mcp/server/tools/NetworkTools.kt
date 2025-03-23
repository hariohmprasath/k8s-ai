package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.NetworkingV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class NetworkTools(
    private val networkingV1Api: NetworkingV1Api
) {
    @Tool(name = "listIngresses", description = "Lists all Ingresses in the specified namespace")
    fun listIngresses(
        @ToolParam(description = "The Kubernetes namespace to list Ingresses from") 
        namespace: String = "default"
    ): List<String> {
        return try {
            networkingV1Api.listNamespacedIngress(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { ingress ->
                    "${ingress.metadata?.name}" +
                            "\n  - Hosts: ${ingress.spec?.rules?.mapNotNull { it.host }?.joinToString(", ") ?: "No hosts"}" +
                            "\n  - TLS: ${ingress.spec?.tls?.mapNotNull { it.hosts?.joinToString(", ") }?.joinToString("; ") ?: "No TLS"}" +
                            "\n  - Class: ${ingress.spec?.ingressClassName ?: "default"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describeIngress", description = "Get detailed information about a specific Ingress")
    fun describeIngress(
        @ToolParam(description = "Name of the Ingress to describe") 
        ingressName: String,
        @ToolParam(description = "The Kubernetes namespace where the Ingress is located") 
        namespace: String = "default"
    ): String {
        return try {
            val ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null)
            """
            Ingress: ${ingress.metadata?.name}
            Namespace: $namespace
            Class: ${ingress.spec?.ingressClassName ?: "default"}
            Created: ${ingress.metadata?.creationTimestamp}
            
            Rules:
            ${ingress.spec?.rules?.joinToString("\n") { rule ->
                """  Host: ${rule.host ?: "*"}
                   Paths:
                   ${rule.http?.paths?.joinToString("\n") { path ->
                       """    - Path: ${path.path ?: "/"}
                          PathType: ${path.pathType}
                          Backend:
                            Service: ${path.backend?.service?.name}
                            Port: ${path.backend?.service?.port?.number}"""
                   } ?: "    No paths defined"}"""
            } ?: "  No rules defined"}
            
            TLS:
            ${ingress.spec?.tls?.joinToString("\n") { tls ->
                """  - Hosts: ${tls.hosts?.joinToString(", ")}
                   SecretName: ${tls.secretName}"""
            } ?: "  No TLS configured"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing Ingress: ${e.message}"
        }
    }

    @Tool(name = "listNetworkPolicies", description = "Lists all NetworkPolicies in the specified namespace")
    fun listNetworkPolicies(
        @ToolParam(description = "The Kubernetes namespace to list NetworkPolicies from") 
        namespace: String = "default"
    ): List<String> {
        return try {
            networkingV1Api.listNamespacedNetworkPolicy(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { policy ->
                    "${policy.metadata?.name}" +
                            "\n  - Pod Selector: ${policy.spec?.podSelector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "All pods"}" +
                            "\n  - Policy Types: ${policy.spec?.policyTypes?.joinToString(", ") ?: "None"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describeNetworkPolicy", description = "Get detailed information about a specific NetworkPolicy")
    fun describeNetworkPolicy(
        @ToolParam(description = "Name of the NetworkPolicy to describe") 
        policyName: String,
        @ToolParam(description = "The Kubernetes namespace where the NetworkPolicy is located") 
        namespace: String = "default"
    ): String {
        return try {
            val policy = networkingV1Api.readNamespacedNetworkPolicy(policyName, namespace, null)
            """
            NetworkPolicy: ${policy.metadata?.name}
            Namespace: $namespace
            Created: ${policy.metadata?.creationTimestamp}
            
            Pod Selector:
              Labels: ${policy.spec?.podSelector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "All pods"}
            
            Policy Types: ${policy.spec?.policyTypes?.joinToString(", ") ?: "None"}
            
            Ingress Rules:
            ${policy.spec?.ingress?.joinToString("\n") { ingress ->
                """  From:
                   ${ingress.from?.joinToString("\n") { from ->
                       """    - Pod Selector: ${from.podSelector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "Any"}
                          Namespace Selector: ${from.namespaceSelector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "Any"}
                          IP Block: ${from.ipBlock?.cidr ?: "None"} (Except: ${from.ipBlock?.except?.joinToString(", ") ?: "None"})"""
                   } ?: "    No from rules"}
                   Ports:
                   ${ingress.ports?.joinToString("\n") { port ->
                       """    - Protocol: ${port.protocol}
                          Port: ${port.port}"""
                   } ?: "    No port rules"}"""
            } ?: "  No ingress rules"}
            
            Egress Rules:
            ${policy.spec?.egress?.joinToString("\n") { egress ->
                """  To:
                   ${egress.to?.joinToString("\n") { to ->
                       """    - Pod Selector: ${to.podSelector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "Any"}
                          Namespace Selector: ${to.namespaceSelector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "Any"}
                          IP Block: ${to.ipBlock?.cidr ?: "None"} (Except: ${to.ipBlock?.except?.joinToString(", ") ?: "None"})"""
                   } ?: "    No to rules"}
                   Ports:
                   ${egress.ports?.joinToString("\n") { port ->
                       """    - Protocol: ${port.protocol}
                          Port: ${port.port}"""
                   } ?: "    No port rules"}"""
            } ?: "  No egress rules"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing NetworkPolicy: ${e.message}"
        }
    }
}
