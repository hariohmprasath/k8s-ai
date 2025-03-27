package com.k8s.tools

import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1ContainerStatus
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import io.kubernetes.client.util.Streams
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream


@Service
class PodTools(
    private val coreV1Api: CoreV1Api,
    private val appsV1Api: AppsV1Api
) {
    @Tool(name = "list_pods", description = "Lists all Kubernetes pods in the specified namespace")
    fun listPods(@ToolParam(description = "The Kubernetes namespace to list pods from") namespace: String = "default"): List<String> {
        return try {
            coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { pod ->
                    "${pod.metadata?.name} (${pod.status?.phase})" +
                            "\n  - Ready: ${pod.status?.containerStatuses?.all { it.ready } ?: false}" +
                            "\n  - IP: ${pod.status?.podIP ?: "N/A"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "get_pod_logs", description = "Retrieves logs from a specific Kubernetes pod with error pattern detection")
    fun getPodLogs(
        @ToolParam(description = "Name of the pod to get logs from") podName: String,
        @ToolParam(description = "The Kubernetes namespace where the pod is located") namespace: String = "default",
        @ToolParam(description = "Number of lines to retrieve from the end of the logs") tailLines: Int = 100
    ): String {
        return try {
            val logs = coreV1Api.readNamespacedPodLog(
                podName,
                namespace,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                tailLines,
                null
            )

            // Analyze logs for common error patterns
            val errorPatterns = mapOf(
                "OutOfMemoryError" to "Memory issues detected",
                "Exception" to "Application exceptions found",
                "Error" to "General errors detected",
                "Failed to pull image" to "Image pull issues",
                "Connection refused" to "Network connectivity issues",
                "Permission denied" to "Permission/RBAC issues"
            )

            val analysis = errorPatterns.filter { (pattern, _) ->
                logs.contains(pattern, ignoreCase = true)
            }.map { (_, description) -> description }.distinct()

            if (analysis.isEmpty()) {
                logs
            } else {
                """
                Log Analysis:
                ${analysis.joinToString("\n") { "- $it" }}
                
                Logs:
                $logs
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error retrieving logs: ${e.message}"
        }
    }

    @Tool(name = "describe_pod", description = "Gets detailed information about a specific Kubernetes pod")
    fun describePod(
        @ToolParam(description = "Name of the pod to describe") podName: String,
        @ToolParam(description = "The Kubernetes namespace where the pod is located") namespace: String = "default"
    ): String {
        return try {
            val pod = coreV1Api.readNamespacedPod(podName, namespace, null)
            "Pod: ${pod.metadata?.name}\n" +
                    "Namespace: ${pod.metadata?.namespace}\n" +
                    "Status: ${pod.status?.phase}\n" +
                    "IP: ${pod.status?.podIP}\n" +
                    "Node: ${pod.spec?.nodeName}\n" +
                    "Containers:\n" +
                    pod.spec?.containers?.joinToString("\n") { container ->
                        "  - ${container.name}:\n" +
                                "    Image: ${container.image}\n" +
                                "    Ready: ${pod.status?.containerStatuses?.find { it.name == container.name }?.ready}"
                    }
        } catch (e: Exception) {
            "Error describing pod: ${e.message}"
        }
    }

    @Tool(name = "diagnose_pods", description = "Analyzes problematic pods and provides troubleshooting recommendations")
    fun analyzePodIssues(
        @ToolParam(description = "The Kubernetes namespace to analyze pods from")
        namespace: String = "default"
    ): String {
        return try {
            val pods = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
            analyzePodList(pods, namespace)
        } catch (e: Exception) {
            "Error analyzing pods: ${e.message}"
        }
    }

    private fun analyzePodList(pods: V1PodList, namespace: String): String {
        val problematicPods = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        pods.items.forEach { pod: V1Pod ->
            analyzePod(pod, problematicPods, recommendations)
        }

        return formatAnalysisResults(namespace, problematicPods, recommendations)
    }

    private fun analyzePod(
        pod: V1Pod,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        val podName = pod.metadata?.name ?: "unknown"
        val containerStatuses = pod.status?.containerStatuses ?: emptyList()

        when (pod.status?.phase) {
            "Pending" -> handlePendingPod(pod, podName, problematicPods, recommendations)
            "Failed" -> handleFailedPod(podName, problematicPods, recommendations)
            null -> handleUnknownPod(podName, problematicPods, recommendations)
            else -> {
                analyzeContainerStatuses(podName, containerStatuses, problematicPods, recommendations)
                checkResourceConstraints(pod, podName, recommendations)
            }
        }
    }

    private fun handlePendingPod(
        pod: V1Pod,
        podName: String,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        problematicPods.add("$podName (Pending)")
        if (pod.status?.conditions?.any { it.type == "PodScheduled" && it.status == "False" } == true) {
            recommendations.add("Pod $podName: Scheduling issues detected. Check node resources and pod affinity/anti-affinity rules.")
        } else {
            recommendations.add("Pod $podName: Pod is pending. Check events for more details.")
        }
    }

    private fun handleFailedPod(
        podName: String,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        problematicPods.add("$podName (Failed)")
        recommendations.add("Pod $podName: Pod failed. Check logs using 'getPodLogs' for more details.")
    }

    private fun handleUnknownPod(
        podName: String,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        problematicPods.add("$podName (Unknown Phase)")
        recommendations.add("Pod $podName: Pod phase is unknown. This might indicate a cluster issue.")
    }

    private fun analyzeContainerStatuses(
        podName: String,
        containerStatuses: List<V1ContainerStatus>,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        containerStatuses.forEach { status: V1ContainerStatus ->
            val containerName = status.name
            checkRestartCount(status, podName, containerName, problematicPods, recommendations)
            checkContainerState(status, podName, containerName, problematicPods, recommendations)
            checkOOMKills(status, podName, containerName, problematicPods, recommendations)
        }
    }

    private fun checkRestartCount(
        status: V1ContainerStatus,
        podName: String,
        containerName: String,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        if (status.restartCount > 3) {
            problematicPods.add("$podName (CrashLooping - Container: $containerName)")
            recommendations.add("Pod $podName, Container $containerName: High restart count (${status.restartCount}). Check logs and memory/CPU limits.")
        }
    }

    private fun checkContainerState(
        status: V1ContainerStatus,
        podName: String,
        containerName: String,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        status.state?.waiting?.let { waiting ->
            when (waiting.reason) {
                "CrashLoopBackOff" -> {
                    problematicPods.add("$podName (CrashLoopBackOff - Container: $containerName)")
                    recommendations.add("Pod $podName, Container $containerName: Application repeatedly crashing. Check logs and application health.")
                }
                "ImagePullBackOff", "ErrImagePull" -> {
                    problematicPods.add("$podName (Image Pull Issue - Container: $containerName)")
                    recommendations.add("Pod $podName, Container $containerName: Image pull failed. Check image name, registry credentials, and network connectivity.")
                }
                "CreateContainerError" -> {
                    problematicPods.add("$podName (Container Creation Failed - Container: $containerName)")
                    recommendations.add("Pod $podName, Container $containerName: Container creation failed. Check container configuration and volume mounts.")
                }
                null -> { /* Ignore null reasons */ }
                else -> {
                    problematicPods.add("$podName (${waiting.reason} - Container: $containerName)")
                    recommendations.add("Pod $podName, Container $containerName: Container in waiting state: ${waiting.reason}. Message: ${waiting.message ?: "Unknown"}")
                }
            }
        }
    }

    private fun checkOOMKills(
        status: V1ContainerStatus,
        podName: String,
        containerName: String,
        problematicPods: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        status.lastState?.terminated?.let { terminated ->
            if (terminated.reason == "OOMKilled") {
                problematicPods.add("$podName (OOM Killed - Container: $containerName)")
                recommendations.add("Pod $podName, Container $containerName: Out of memory. Consider increasing memory limits or investigating memory leaks.")
            }
        }
    }

    private fun checkResourceConstraints(
        pod: V1Pod,
        podName: String,
        recommendations: MutableList<String>
    ) {
        pod.spec?.containers?.forEach { container: V1Container ->
            val resources = container.resources
            if (resources?.limits == null && resources?.requests == null) {
                recommendations.add("Pod $podName, Container ${container.name}: No resource limits/requests set. Consider adding them for better resource management.")
            }
        }
    }

    private fun formatAnalysisResults(
        namespace: String,
        problematicPods: List<String>,
        recommendations: List<String>
    ): String {
        return if (problematicPods.isEmpty()) {
            "No problematic pods found in namespace $namespace"
        } else {
            """
            Problematic Pods in namespace $namespace:
            ${problematicPods.joinToString("\n") { "- $it" }}
            
            Recommendations:
            ${recommendations.joinToString("\n") { "- $it" }}
            """.trimIndent()
        }
    }

    @Tool(name = "get_pod_metrics", description = "Get resource usage metrics for a specific pod")
    fun getPodMetrics(
        @ToolParam(description = "Name of the pod to get metrics for")
        podName: String,
        @ToolParam(description = "The Kubernetes namespace where the pod is located")
        namespace: String = "default"
    ): String {
        return try {
            val pod = coreV1Api.readNamespacedPod(podName, namespace, null)

            val containerMetrics = pod.spec?.containers?.map { container ->
                val requests = container.resources?.requests
                val limits = container.resources?.limits

                """
                Container: ${container.name}
                Resource Requests:
                  CPU: ${requests?.get("cpu")?.number?.toDouble() ?: "Not set"}
                  Memory: ${requests?.get("memory")?.number?.toDouble() ?: "Not set"}
                Resource Limits:
                  CPU: ${limits?.get("cpu")?.number?.toDouble() ?: "Not set"}
                  Memory: ${limits?.get("memory")?.number?.toDouble() ?: "Not set"}
                Status:
                  Ready: ${pod.status?.containerStatuses?.find { it.name == container.name }?.ready ?: false}
                  Restarts: ${pod.status?.containerStatuses?.find { it.name == container.name }?.restartCount ?: 0}
                  State: ${pod.status?.containerStatuses?.find { it.name == container.name }?.state?.let { state ->
                    when {
                        state.running != null -> state.running?.let { "Running since ${it.startedAt}" } ?: "Unknown"
                        state.waiting != null -> state.waiting?.let { "Waiting (${it.reason})" } ?: "Unknown"
                        state.terminated != null -> state.terminated?.let { "Terminated (${it.reason})" } ?: "Unknown"
                        else -> "Unknown"
                    }
                  } ?: "Unknown"}
                """.trimIndent()
            } ?: emptyList()

            """
            Pod Metrics for $podName:
            Node: ${pod.spec?.nodeName ?: "Not scheduled"}
            Phase: ${pod.status?.phase}
            Start Time: ${pod.status?.startTime}
            
            Container Metrics:
            ${containerMetrics.joinToString("\n\n")}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting pod metrics: ${e.message}"
        }
    }

    @Tool(name = "exec_in_pod", description = "Execute a command in a pod container")
    fun execInPod(
        @ToolParam(description = "Name of the pod to execute command in")
        podName: String,
        @ToolParam(description = "The Kubernetes namespace where the pod is located")
        namespace: String = "default",
        @ToolParam(description = "Command to execute")
        command: String
    ): String {
        return try {
            val pod = coreV1Api.readNamespacedPod(podName, namespace, null)
            if (pod.status?.phase != "Running") {
                return "Cannot execute command: Pod is not running (current phase: ${pod.status?.phase})"
            }

            val process = ProcessBuilder()
                .command("kubectl", "exec", "-n", namespace, podName, "--", "/bin/sh", "-c", command)
                .start()

            val output = ByteArrayOutputStream()
            Streams.copy(process.inputStream, output)
            val error = ByteArrayOutputStream()
            Streams.copy(process.errorStream, error)

            process.waitFor()

            val outputStr = output.toString()
            val errorStr = error.toString()

            if (process.exitValue() != 0) {
                "Command failed with exit code ${process.exitValue()}\nError: $errorStr"
            } else {
                "Command output:\n$outputStr"
            }
        } catch (e: Exception) {
            "Error executing command in pod: ${e.message}"
        }
    }
}