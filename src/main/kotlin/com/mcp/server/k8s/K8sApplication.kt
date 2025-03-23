package com.mcp.server.k8s

import com.mcp.server.tools.*
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.EventsV1Api
import io.kubernetes.client.openapi.apis.NetworkingV1Api
import io.kubernetes.client.openapi.apis.SchedulingV1Api
import io.kubernetes.client.openapi.apis.StorageV1Api
import io.kubernetes.client.util.Config
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootApplication(scanBasePackages = ["com.mcp.server"])
class K8sApplication

@Configuration
class K8sConfig {
    @Bean
    fun kubernetesClient() = run {
        val kubeConfigPath = System.getProperty("user.home") + "/.kube/config"
        try {
            Config.fromConfig(kubeConfigPath)
        } catch (e: Exception) {
            throw IllegalStateException("Could not initialize Kubernetes client from $kubeConfigPath", e)
        }
    }

    @Bean
    fun coreV1Api(client: io.kubernetes.client.openapi.ApiClient) = CoreV1Api(client)

    @Bean
    fun appsV1Api(client: io.kubernetes.client.openapi.ApiClient) = AppsV1Api(client)

    @Bean
    fun storageV1Api(client: io.kubernetes.client.openapi.ApiClient) = StorageV1Api(client)

    @Bean
    fun schedulingV1Api(client: io.kubernetes.client.openapi.ApiClient) = SchedulingV1Api(client)

    @Bean
    fun networkingV1Api(client: io.kubernetes.client.openapi.ApiClient) = NetworkingV1Api(client)

    @Bean
    fun eventsV1Api(client: io.kubernetes.client.openapi.ApiClient) = EventsV1Api(client)

    @Bean
    fun k8sTools(
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
        healthTools: HealthTools
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
                eventTools,
                healthTools
            )
            .build()
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(K8sApplication::class.java, *args)
}

