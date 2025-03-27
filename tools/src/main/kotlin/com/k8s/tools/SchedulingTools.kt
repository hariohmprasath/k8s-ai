package com.k8s.tools

import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.SchedulingV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class SchedulingTools(
    private val coreV1Api: CoreV1Api,
    private val schedulingV1Api: SchedulingV1Api
) {
    @Tool(name = "list_priority_classes", description = "Lists all priority classes in the cluster")
    fun listPriorityClasses(): List<String> {
        return try {
            schedulingV1Api.listPriorityClass(null, null, null, null, null, null, null, null, null, null)
                .items
                .map { pc ->
                    "${pc.metadata?.name}" +
                            "\n  - Value: ${pc.value}" +
                            "\n  - Global Default: ${pc.globalDefault}" +
                            "\n  - Description: ${pc.description ?: "N/A"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describe_priority_class", description = "Get detailed information about a specific priority class")
    fun describePriorityClass(
        @ToolParam(description = "Name of the priority class to describe")
        priorityClassName: String
    ): String {
        return try {
            val pc = schedulingV1Api.readPriorityClass(priorityClassName, null)
            """
            Priority Class: ${pc.metadata?.name}
            Value: ${pc.value}
            Global Default: ${pc.globalDefault}
            Description: ${pc.description ?: "N/A"}
            
            Metadata:
              Created: ${pc.metadata?.creationTimestamp}
              Labels: ${pc.metadata?.labels?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "none"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing priority class: ${e.message}"
        }
    }

    @Tool(name = "list_node_taints", description = "Lists all taints on the specified node")
    fun listNodeTaints(
        @ToolParam(description = "Name of the node to get taints from")
        nodeName: String
    ): String {
        return try {
            val node = coreV1Api.readNode(nodeName, null)
            val taints = node.spec?.taints ?: emptyList()
            if (taints.isEmpty()) {
                "No taints found on node $nodeName"
            } else {
                """
                Node: $nodeName
                Taints:
                ${taints.joinToString("\n") { taint ->
                    "  - ${taint.key}=${taint.value}:${taint.effect}"
                }}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error listing node taints: ${e.message}"
        }
    }

    @Tool(name = "list_pod_tolerations", description = "Lists all tolerations on pods in a namespace")
    fun listPodTolerations(
        @ToolParam(description = "The Kubernetes namespace to list pod tolerations from")
        namespace: String = "default"
    ): List<String> {
        return try {
            coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .filter { it.spec?.tolerations?.isNotEmpty() == true }
                .map { pod ->
                    "${pod.metadata?.name}:" +
                            "\n  Tolerations:" +
                            pod.spec?.tolerations?.joinToString("") { toleration ->
                                "\n    - ${toleration.key ?: "<all>"}" +
                                        (if (toleration.value != null) "=${toleration.value}" else "") +
                                        ":${toleration.effect}" +
                                        (if (toleration.tolerationSeconds != null) " for ${toleration.tolerationSeconds}s" else "")
                            }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "list_pod_node_affinity", description = "Lists node affinity rules for pods in a namespace")
    fun listPodNodeAffinity(
        @ToolParam(description = "The Kubernetes namespace to list pod node affinity rules from")
        namespace: String = "default"
    ): List<String> {
        return try {
            coreV1Api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .filter { it.spec?.affinity?.nodeAffinity != null }
                .map { pod ->
                    val nodeAffinity = pod.spec?.affinity?.nodeAffinity
                    "${pod.metadata?.name}:" +
                            "\n  Required Rules:" +
                            (nodeAffinity?.requiredDuringSchedulingIgnoredDuringExecution?.nodeSelectorTerms
                                ?.joinToString("") { term ->
                                    "\n    Match Expressions:" +
                                            term.matchExpressions?.joinToString("") { expr ->
                                                "\n      - ${expr.key} ${expr.operator} [${expr.values?.joinToString(", ")}]"
                                            }
                                } ?: "\n    None") +
                            "\n  Preferred Rules:" +
                            (nodeAffinity?.preferredDuringSchedulingIgnoredDuringExecution
                                ?.joinToString("") { pref ->
                                    "\n    - Weight: ${pref.weight}" +
                                            "\n      Match Expressions:" +
                                            pref.preference.matchExpressions?.joinToString("") { expr ->
                                                "\n        - ${expr.key} ${expr.operator} [${expr.values?.joinToString(", ")}]"
                                            }
                                } ?: "\n    None")
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}