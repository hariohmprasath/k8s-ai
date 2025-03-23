package com.mcp.server.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.Streams
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream


@Service
class PodTools(
    private val coreV1Api: CoreV1Api
) {
    @Tool(name = "listPods", description = "Lists all Kubernetes pods in the specified namespace")
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

    @Tool(name = "getPodLogs", description = "Retrieves logs from a specific Kubernetes pod")
    fun getPodLogs(
        @ToolParam(description = "Name of the pod to get logs from") podName: String,
        @ToolParam(description = "The Kubernetes namespace where the pod is located") namespace: String = "default",
        @ToolParam(description = "Number of lines to retrieve from the end of the logs") tailLines: Int = 100
    ): String {
        return try {
            coreV1Api.readNamespacedPodLog(
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
        } catch (e: Exception) {
            "Error retrieving logs: ${e.message}"
        }
    }

    @Tool(name = "describePod", description = "Gets detailed information about a specific Kubernetes pod")
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

    @Tool(name = "diagnosePods", description = "Analyzes problematic pods and provides troubleshooting recommendations")
    fun analyzePodIssues(
        @ToolParam(description = "The Kubernetes namespace to analyze pods from") 
        namespace: String = "default"
    ): String {
        return try {
            val pods = coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .filter { pod ->
                    val containerStatuses = pod.status?.containerStatuses ?: emptyList()
                    val isProblematic = containerStatuses.any { status ->
                        status.state?.waiting?.reason in listOf(
                            "CrashLoopBackOff",
                            "ImagePullBackOff",
                            "ErrImagePull",
                            "CreateContainerError",
                            "OOMKilled"
                        ) || 
                        (status.restartCount > 0 && status.lastState?.terminated?.reason in listOf(
                            "OOMKilled",
                            "Error",
                            "ContainerCannotRun"
                        ))
                    }
                    isProblematic
                }

            if (pods.isEmpty()) {
                return "No problematic pods found in namespace '$namespace'"
            }

            val results = pods.map { pod ->
                val analysis = StringBuilder()
                analysis.append("\n=== Pod: ${pod.metadata?.name} ===\n")
                analysis.append("Status: ${pod.status?.phase}\n")

                pod.status?.containerStatuses?.forEach { status ->
                    analysis.append("\nContainer: ${status.name}\n")
                    analysis.append("Restart Count: ${status.restartCount}\n")

                    val state = status.state
                    val lastState = status.lastState
                    val currentIssue = when {
                        state?.waiting?.reason == "CrashLoopBackOff" -> {
                            val logs = try {
                                coreV1Api.readNamespacedPodLog(
                                    pod.metadata?.name!!, 
                                    namespace,
                                    status.name,
                                    null,
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    100,
                                    null
                                )
                            } catch (e: Exception) { "Unable to fetch logs: ${e.message}" }

                            "CrashLoopBackOff - Container is repeatedly crashing\n" +
                            "Recent Logs:\n$logs\n" +
                            "Recommendations:\n" +
                            "1. Check application logs for error messages\n" +
                            "2. Verify the container's resource limits and requests\n" +
                            "3. Ensure the application can handle its configuration\n" +
                            "4. Check for proper initialization and readiness probes"
                        }
                        state?.waiting?.reason?.contains("ImagePull") == true -> {
                            val image = pod.spec?.containers?.find { it.name == status.name }?.image
                            "Image Pull Error - Unable to pull image: $image\n" +
                            "Reason: ${state.waiting?.message}\n" +
                            "Recommendations:\n" +
                            "1. Verify the image name and tag are correct\n" +
                            "2. Ensure the image exists in the specified registry\n" +
                            "3. Check if the registry requires authentication\n" +
                            "4. Verify network connectivity to the registry"
                        }
                        lastState?.terminated?.reason == "OOMKilled" -> {
                            val container = pod.spec?.containers?.find { it.name == status.name }
                            "Out of Memory Error\n" +
                            "Memory Limits: ${container?.resources?.limits?.get("memory")}\n" +
                            "Memory Requests: ${container?.resources?.requests?.get("memory")}\n" +
                            "Recommendations:\n" +
                            "1. Increase the memory limit in the pod spec\n" +
                            "2. Analyze application memory usage patterns\n" +
                            "3. Check for memory leaks in the application\n" +
                            "4. Consider implementing memory optimization strategies"
                        }
                        state?.waiting?.reason == "CreateContainerError" -> {
                            "Container Creation Error\n" +
                            "Error: ${state.waiting?.message}\n" +
                            "Recommendations:\n" +
                            "1. Verify container configuration in the pod spec\n" +
                            "2. Check for missing or invalid volume mounts\n" +
                            "3. Ensure all required environment variables are set\n" +
                            "4. Validate container security context settings"
                        }
                        else -> "Unknown issue - Check events and logs for more details"
                    }
                    analysis.append(currentIssue)
                }

                // Add events for more context
                val events = coreV1Api.listNamespacedEvent(
                    namespace,
                    null,
                    null,
                    null,
                    "involvedObject.name=${pod.metadata?.name}",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ).items

                if (events.isNotEmpty()) {
                    analysis.append("\n\nRecent Events:\n")
                    events.takeLast(5).forEach { event ->
                        analysis.append("${event.type}: ${event.reason} - ${event.message}\n")
                    }
                }

                analysis.toString()
            }

            "Found ${pods.size} problematic pod(s) in namespace '$namespace':\n" + results.joinToString("\n\n")
        } catch (e: Exception) {
            "Error analyzing pods: ${e.message}"
        }
    }

    @Tool(name = "getPodMetrics", description = "Get resource usage metrics for a specific pod")
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

    @Tool(name = "execInPod", description = "Execute a command in a pod container")
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
