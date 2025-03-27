package com.k8s.tools

import io.kubernetes.client.openapi.apis.*
import io.kubernetes.client.util.Config
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ToolsConfig {
    @Bean
    open fun kubernetesClient() = run {
        val kubeConfigPath = System.getProperty("user.home") + "/.kube/config"
        try {
            Config.fromConfig(kubeConfigPath)
        } catch (e: Exception) {
            throw IllegalStateException("Could not initialize Kubernetes client from $kubeConfigPath", e)
        }
    }

    @Bean
    open fun coreV1Api(client: io.kubernetes.client.openapi.ApiClient) = CoreV1Api(client)

    @Bean
    open fun appsV1Api(client: io.kubernetes.client.openapi.ApiClient) = AppsV1Api(client)

    @Bean
    open fun storageV1Api(client: io.kubernetes.client.openapi.ApiClient) = StorageV1Api(client)

    @Bean
    open fun schedulingV1Api(client: io.kubernetes.client.openapi.ApiClient) = SchedulingV1Api(client)

    @Bean
    open fun networkingV1Api(client: io.kubernetes.client.openapi.ApiClient) = NetworkingV1Api(client)

    @Bean
    open fun eventsV1Api(client: io.kubernetes.client.openapi.ApiClient) = EventsV1Api(client)

    @Bean
    open fun batchV1Api(client: io.kubernetes.client.openapi.ApiClient) = BatchV1Api(client)

    @Bean
    open fun k8sTools(
        podTools: PodTools,
        nodeTools: NodeTools,
        serviceTools: ServiceTools,
        storageTools: StorageTools,
        schedulingTools: SchedulingTools,
        deploymentTools: DeploymentTools,
        configMapAndSecretTools: ConfigMapAndSecretTools,
        networkTools: NetworkTools,
        resourceManagementTools: ResourceManagementTools,
        eventTools: EventTools,
        healthTools: HealthTools,
        helmTools: HelmTools,
        jobTools: JobTools
    ): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(
                podTools,
                nodeTools,
                serviceTools,
                storageTools,
                schedulingTools,
                deploymentTools,
                configMapAndSecretTools,
                networkTools,
                resourceManagementTools,
                jobTools,
                eventTools,
                healthTools,
                helmTools
            )
            .build()
    }
}