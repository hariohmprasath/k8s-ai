package com.k8s.tools

import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class DeploymentTools(
    private val appsV1Api: AppsV1Api,
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "list_deployments", description = "Lists all Kubernetes deployments in the specified namespace")
    fun listDeployments(
        @ToolParam(description = "The Kubernetes namespace to list deployments from")
        namespace: String = "default"
    ): String {
        return try {
            val deployments = appsV1Api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null, null)
                .items

            if (deployments.isEmpty()) {
                return "No deployments found in namespace '$namespace'"
            }

            val deploymentList = deployments.mapNotNull { deployment ->
                val name = deployment.metadata?.name ?: return@mapNotNull null
                val availableReplicas = deployment.status?.availableReplicas ?: 0
                val desiredReplicas = deployment.spec?.replicas ?: 0
                val strategy = deployment.spec?.strategy?.type ?: "Not set"

                val resourceInfo = deployment.spec?.template?.spec?.containers?.mapNotNull { container ->
                    val resources = container.resources
                    val cpuRequest = resources?.requests?.get("cpu")?.toString() ?: "Not set"
                    val memRequest = resources?.requests?.get("memory")?.toString() ?: "Not set"
                    val image = container.image ?: "No image"
                    "\n    ${container.name}:\n" +
                    "      Image: $image\n" +
                    "      CPU Request: $cpuRequest\n" +
                    "      Memory Request: $memRequest"
                }?.joinToString("\n") ?: "No container specs found"

                "Deployment: $name\n" +
                "  Status:\n" +
                "    Replicas: $availableReplicas/$desiredReplicas\n" +
                "    Strategy: $strategy\n" +
                "  Containers:$resourceInfo\n"
            }

            if (deploymentList.isEmpty()) {
                "No valid deployments found in namespace '$namespace'"
            } else {
                "Found ${deploymentList.size} deployment(s) in namespace '$namespace':\n\n${deploymentList.joinToString("\n")}"
            }
        } catch (e: Exception) {
            "Error listing deployments in namespace '$namespace': ${e.message}\n" +
            "Please ensure:\n" +
            "1. You have a valid kubeconfig file\n" +
            "2. The cluster is accessible\n" +
            "3. You have permissions to list deployments in the '$namespace' namespace"
        }
    }

    @Tool(name = "describe_deployment", description = "Get detailed information about a specific deployment")
    fun describeDeployment(
        @ToolParam(description = "Name of the deployment to describe")
        deploymentName: String,
        @ToolParam(description = "The Kubernetes namespace where the deployment is located")
        namespace: String = "default"
    ): String {
        return try {
            val deployment = appsV1Api.readNamespacedDeployment(deploymentName, namespace, null)
            val selector = deployment.spec?.selector?.matchLabels?.entries?.joinToString(",") { "${it.key}=${it.value}" }
            val pods = coreV1Api.listNamespacedPod(
                namespace,
                null,
                null,
                null,
                null,
                selector,
                null,
                null,
                null,
                null,
                null
            ).items

            """
            Deployment: ${deployment.metadata?.name}
            Namespace: ${deployment.metadata?.namespace}
            
            Spec:
              Replicas: ${deployment.spec?.replicas}
              Strategy: ${deployment.spec?.strategy?.type}
              Selector: ${deployment.spec?.selector?.matchLabels?.entries?.joinToString(", ") { "${it.key}=${it.value}" }}
              
            Template:
              Labels: ${deployment.spec?.template?.metadata?.labels?.entries?.joinToString(", ") { "${it.key}=${it.value}" }}
              Containers:
            ${deployment.spec?.template?.spec?.containers?.joinToString("\n") { container ->
                """    - ${container.name}:
                  Image: ${container.image}
                  Ports: ${container.ports?.joinToString(", ") { "${it.containerPort}/${it.protocol}" }}
                  Resources:
                    Requests: ${container.resources?.requests?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }}
                    Limits: ${container.resources?.limits?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }}"""
            }}
              
            Status:
              Available Replicas: ${deployment.status?.availableReplicas}
              Ready Replicas: ${deployment.status?.readyReplicas}
              Updated Replicas: ${deployment.status?.updatedReplicas}
              Conditions:
            ${deployment.status?.conditions?.joinToString("\n") { condition ->
                "    - ${condition.type}: ${condition.status} (${condition.message})"
            }}
              
            Pods:
            ${pods.joinToString("\n") { pod ->
                """    - ${pod.metadata?.name}:
                  Status: ${pod.status?.phase}
                  Ready: ${pod.status?.containerStatuses?.all { it.ready } ?: false}
                  Restarts: ${pod.status?.containerStatuses?.sumOf { it.restartCount ?: 0 } ?: 0}"""
            }}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing deployment '${deploymentName}' in namespace '${namespace}': ${e.message}\n" +
            "Please ensure:\n" +
            "1. You have a valid kubeconfig file\n" +
            "2. The cluster is accessible\n" +
            "3. The deployment '${deploymentName}' exists in namespace '${namespace}'\n" +
            "4. You have permissions to view deployments in this namespace"
        }
    }

    @Tool(name = "analyze_deployment", description = "Analyzes the health and status of a deployment")
    fun analyzeDeploymentHealth(
        @ToolParam(description = "Name of the deployment to analyze")
        deploymentName: String,
        @ToolParam(description = "The Kubernetes namespace where the deployment is located")
        namespace: String = "default"
    ): String {
        return try {
            val deployment = appsV1Api.readNamespacedDeployment(deploymentName, namespace, null)
            val selector = deployment.spec?.selector?.matchLabels?.entries?.joinToString(",") { "${it.key}=${it.value}" }
            val pods = coreV1Api.listNamespacedPod(
                namespace,
                null,
                null,
                null,
                null,
                selector,
                null,
                null,
                null,
                null,
                null
            ).items

            val analysis = StringBuilder()
            analysis.append("=== Deployment Health Analysis: ${deployment.metadata?.name} ===\n\n")

            // Analyze replica status
            val desiredReplicas = deployment.spec?.replicas ?: 0
            val availableReplicas = deployment.status?.availableReplicas ?: 0
            val readyReplicas = deployment.status?.readyReplicas ?: 0
            val updatedReplicas = deployment.status?.updatedReplicas ?: 0

            analysis.append("Replica Status:\n")
            analysis.append("- Desired: $desiredReplicas\n")
            analysis.append("- Available: $availableReplicas\n")
            analysis.append("- Ready: $readyReplicas\n")
            analysis.append("- Updated: $updatedReplicas\n\n")

            if (availableReplicas < desiredReplicas) {
                analysis.append("⚠️ Warning: Not all desired replicas are available\n")
                analysis.append("Recommendations:\n")
                analysis.append("1. Check pod events for scheduling issues\n")
                analysis.append("2. Verify resource quotas and limits\n")
                analysis.append("3. Check node capacity and availability\n\n")
            }

            // Analyze pod health
            val unhealthyPods = pods.filter { pod ->
                pod.status?.phase != "Running" ||
                pod.status?.containerStatuses?.any { !it.ready || it.restartCount > 0 } == true
            }

            if (unhealthyPods.isNotEmpty()) {
                analysis.append("Pod Health Issues:\n")
                unhealthyPods.forEach { pod ->
                    analysis.append("Pod: ${pod.metadata?.name}\n")
                    analysis.append("- Phase: ${pod.status?.phase}\n")
                    pod.status?.containerStatuses?.forEach { status ->
                        analysis.append("- Container: ${status.name}\n")
                        analysis.append("  Ready: ${status.ready}\n")
                        analysis.append("  Restart Count: ${status.restartCount}\n")
                        if (status.state?.waiting != null) {
                            analysis.append("  Waiting: ${status.state?.waiting?.reason} - ${status.state?.waiting?.message}\n")
                        }
                    }
                    analysis.append("\n")
                }
            } else {
                analysis.append("✅ All pods are healthy\n\n")
            }

            // Analyze deployment conditions
            analysis.append("Deployment Conditions:\n")
            deployment.status?.conditions?.forEach { condition ->
                analysis.append("- ${condition.type}: ${condition.status}\n")
                if (condition.status != "True") {
                    analysis.append("  Message: ${condition.message}\n")
                    analysis.append("  Last Update: ${condition.lastUpdateTime}\n")
                }
            }

            // Add recommendations based on analysis
            analysis.append("\nRecommendations:\n")
            if (unhealthyPods.isNotEmpty()) {
                analysis.append("1. Check pod logs for application errors\n")
                analysis.append("2. Verify container resource limits and requests\n")
                analysis.append("3. Check for image pull issues\n")
                analysis.append("4. Review liveness and readiness probe configurations\n")
            }
            if (deployment.status?.conditions?.any { it.type == "Progressing" && it.status != "True" } == true) {
                analysis.append("5. Review deployment strategy and rollout status\n")
                analysis.append("6. Check for configuration or dependency issues\n")
            }

            analysis.toString()
        } catch (e: Exception) {
            "Error analyzing deployment health: ${e.message}"
        }
    }
}