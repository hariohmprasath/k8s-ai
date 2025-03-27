package com.k8s.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service


@Service
class ConfigMapAndSecretTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "list_config_maps", description = "Lists all ConfigMaps in the specified namespace")
    fun listConfigMaps(
        @ToolParam(description = "The Kubernetes namespace to list ConfigMaps from")
        namespace: String = "default"
    ): List<String> {
        return try {
            coreV1Api.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { configMap ->
                    "${configMap.metadata?.name}" +
                            "\n  - Data Keys: ${configMap.data?.keys?.joinToString(", ") ?: "No data"}" +
                            "\n  - Created: ${configMap.metadata?.creationTimestamp}" +
                            "\n  - Labels: ${configMap.metadata?.labels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "No labels"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describe_config_map", description = "Get detailed information about a specific ConfigMap")
    fun describeConfigMap(
        @ToolParam(description = "Name of the ConfigMap to describe")
        configMapName: String,
        @ToolParam(description = "The Kubernetes namespace where the ConfigMap is located")
        namespace: String = "default"
    ): String {
        return try {
            val configMap = coreV1Api.readNamespacedConfigMap(configMapName, namespace, null)
            """
            ConfigMap: ${configMap.metadata?.name}
            Namespace: $namespace
            Created: ${configMap.metadata?.creationTimestamp}
            
            Labels:
            ${configMap.metadata?.labels?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No labels"}
            
            Annotations:
            ${configMap.metadata?.annotations?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No annotations"}
            
            Data:
            ${configMap.data?.entries?.joinToString("\n") { "  ${it.key}: ${it.value.take(100)}${if (it.value.length > 100) "..." else ""}" } ?: "  No data"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing ConfigMap: ${e.message}"
        }
    }

    @Tool(name = "list_secrets", description = "Lists all Secrets in the specified namespace (names only for security)")
    fun listSecrets(
        @ToolParam(description = "The Kubernetes namespace to list Secrets from")
        namespace: String = "default"
    ): List<String> {
        return try {
            coreV1Api.listNamespacedSecret(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { secret ->
                    "${secret.metadata?.name}" +
                            "\n  - Type: ${secret.type}" +
                            "\n  - Created: ${secret.metadata?.creationTimestamp}" +
                            "\n  - Labels: ${secret.metadata?.labels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "No labels"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describe_secret", description = "Get metadata about a specific Secret (no secret values shown)")
    fun describeSecret(
        @ToolParam(description = "Name of the Secret to describe")
        secretName: String,
        @ToolParam(description = "The Kubernetes namespace where the Secret is located")
        namespace: String = "default"
    ): String {
        return try {
            val secret = coreV1Api.readNamespacedSecret(secretName, namespace, null)
            """
            Secret: ${secret.metadata?.name}
            Namespace: $namespace
            Type: ${secret.type}
            Created: ${secret.metadata?.creationTimestamp}
            
            Labels:
            ${secret.metadata?.labels?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No labels"}
            
            Annotations:
            ${secret.metadata?.annotations?.entries?.joinToString("\n") { "  ${it.key}: ${it.value}" } ?: "  No annotations"}
            
            Data Keys:
            ${secret.data?.keys?.joinToString("\n") { "  - $it" } ?: "  No data"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing Secret: ${e.message}"
        }
    }
}