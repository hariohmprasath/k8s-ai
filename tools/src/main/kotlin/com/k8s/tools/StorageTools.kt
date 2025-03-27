package com.k8s.tools

import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class StorageTools(
    private val coreV1Api: CoreV1Api,
    private val storageV1Api: StorageV1Api
) {
    @Tool(name = "list_persistent_volumes", description = "Lists all persistent volumes in the cluster")
    fun listPersistentVolumes(): List<String> {
        return try {
            coreV1Api.listPersistentVolume(null, null, null, null, null, null, null, null, null, null)
                .items
                .map { pv ->
                    "${pv.metadata?.name}" +
                            "\n  - Status: ${pv.status?.phase}" +
                            "\n  - Capacity: ${pv.spec?.capacity?.get("storage")}" +
                            "\n  - Access Modes: ${pv.spec?.accessModes?.joinToString(", ")}" +
                            "\n  - Storage Class: ${pv.spec?.storageClassName}" +
                            "\n  - Reclaim Policy: ${pv.spec?.persistentVolumeReclaimPolicy}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "list_persistent_volume_claims", description = "Lists all persistent volume claims in the specified namespace")
    fun listPersistentVolumeClaims(
        @ToolParam(description = "The Kubernetes namespace to list PVCs from")
        namespace: String = "default"
    ): List<String> {
        return try {
            coreV1Api.listNamespacedPersistentVolumeClaim(namespace, null, null, null, null, null, null, null, null, null, null)
                .items
                .map { pvc ->
                    "${pvc.metadata?.name}" +
                            "\n  - Status: ${pvc.status?.phase}" +
                            "\n  - Volume: ${pvc.spec?.volumeName}" +
                            "\n  - Capacity: ${pvc.status?.capacity?.get("storage")}" +
                            "\n  - Access Modes: ${pvc.spec?.accessModes?.joinToString(", ")}" +
                            "\n  - Storage Class: ${pvc.spec?.storageClassName}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "list_storage_classes", description = "Lists all storage classes in the cluster")
    fun listStorageClasses(): List<String> {
        return try {
            storageV1Api.listStorageClass(null, null, null, null, null, null, null, null, null, null)
                .items
                .map { sc ->
                    "${sc.metadata?.name}" +
                            "\n  - Provisioner: ${sc.provisioner}" +
                            "\n  - Reclaim Policy: ${sc.reclaimPolicy}" +
                            "\n  - Volume Binding Mode: ${sc.volumeBindingMode}" +
                            "\n  - Allow Volume Expansion: ${sc.allowVolumeExpansion}" +
                            "\n  - Default: ${sc.metadata?.annotations?.get("storageclass.kubernetes.io/is-default-class") == "true"}"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Tool(name = "describe_persistent_volume", description = "Get detailed information about a specific persistent volume")
    fun describePersistentVolume(
        @ToolParam(description = "Name of the persistent volume to describe")
        pvName: String
    ): String {
        return try {
            val pv = coreV1Api.readPersistentVolume(pvName, null)
            """
            Persistent Volume: ${pv.metadata?.name}
            Status: ${pv.status?.phase}
            
            Spec:
              Capacity: ${pv.spec?.capacity?.get("storage")}
              Access Modes: ${pv.spec?.accessModes?.joinToString(", ")}
              Storage Class: ${pv.spec?.storageClassName}
              Reclaim Policy: ${pv.spec?.persistentVolumeReclaimPolicy}
              
              Source:
            ${when {
                pv.spec?.hostPath != null -> "    HostPath:\n      Path: ${pv.spec?.hostPath?.path}"
                pv.spec?.nfs != null -> "    NFS:\n      Server: ${pv.spec?.nfs?.server}\n      Path: ${pv.spec?.nfs?.path}"
                pv.spec?.awsElasticBlockStore != null -> "    AWS EBS:\n      Volume ID: ${pv.spec?.awsElasticBlockStore?.volumeID}"
                else -> "    Other storage type"
            }}
              
            Claim:
              Name: ${pv.spec?.claimRef?.name ?: "none"}
              Namespace: ${pv.spec?.claimRef?.namespace ?: "none"}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing persistent volume: ${e.message}"
        }
    }

    @Tool(name = "describe_persistent_volume_claim", description = "Get detailed information about a specific persistent volume claim")
    fun describePersistentVolumeClaim(
        @ToolParam(description = "Name of the persistent volume claim to describe")
        pvcName: String,
        @ToolParam(description = "The Kubernetes namespace where the PVC is located")
        namespace: String = "default"
    ): String {
        return try {
            val pvc = coreV1Api.readNamespacedPersistentVolumeClaim(pvcName, namespace, null)
            """
            Persistent Volume Claim: ${pvc.metadata?.name}
            Namespace: ${pvc.metadata?.namespace}
            Status: ${pvc.status?.phase}
            
            Spec:
              Access Modes: ${pvc.spec?.accessModes?.joinToString(", ")}
              Resources:
                Requests:
                  Storage: ${pvc.spec?.resources?.requests?.get("storage")}
              Storage Class: ${pvc.spec?.storageClassName}
              Volume Name: ${pvc.spec?.volumeName}
              
            Status:
              Capacity: ${pvc.status?.capacity?.get("storage")}
              Access Modes: ${pvc.status?.accessModes?.joinToString(", ")}
            """.trimIndent()
        } catch (e: Exception) {
            "Error describing persistent volume claim: ${e.message}"
        }
    }
}