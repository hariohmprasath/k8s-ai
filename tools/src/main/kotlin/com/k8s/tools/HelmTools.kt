package com.k8s.tools

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

@Service
class HelmTools {

    @Tool(name = "list_releases", description = "List all Helm releases in a namespace")
    fun listReleases(@ToolParam(description = "The Kubernetes namespace to list releases from") namespace: String): String {
        return executeHelmCommand("list", "--namespace", namespace)
    }

    @Tool(name = "install_chart", description = "Install a Helm chart with optional values")
    fun installChart(
        @ToolParam(description = "Name for the release") releaseName: String,
        @ToolParam(description = "Name of the chart to install") chartName: String,
        @ToolParam(description = "The Kubernetes namespace to install into") namespace: String,
        @ToolParam(description = "Optional version of the chart") version: String? = null,
        @ToolParam(description = "Optional key-value pairs for chart values") values: Map<String, String>? = null
    ): String {
        val command = mutableListOf("install", releaseName, chartName, "--namespace", namespace)

        version?.let { command.addAll(listOf("--version", it)) }
        values?.forEach { (key, value) ->
            command.addAll(listOf("--set", "$key=$value"))
        }

        return executeHelmCommand(*command.toTypedArray())
    }

    @Tool(name = "uninstall_release", description = "Uninstall a Helm release")
    fun uninstallRelease(
        @ToolParam(description = "Name of the release to uninstall") releaseName: String,
        @ToolParam(description = "The Kubernetes namespace of the release") namespace: String
    ): String {
        return executeHelmCommand("uninstall", releaseName, "--namespace", namespace)
    }

    @Tool(name = "upgrade_release", description = "Upgrade an existing Helm release")
    fun upgradeRelease(
        @ToolParam(description = "Name of the release to upgrade") releaseName: String,
        @ToolParam(description = "Name of the chart to upgrade to") chartName: String,
        @ToolParam(description = "The Kubernetes namespace of the release") namespace: String,
        @ToolParam(description = "Optional version to upgrade to") version: String? = null,
        @ToolParam(description = "Optional key-value pairs for chart values") values: Map<String, String>? = null
    ): String {
        val command = mutableListOf("upgrade", releaseName, chartName, "--namespace", namespace)

        version?.let { command.addAll(listOf("--version", it)) }
        values?.forEach { (key, value) ->
            command.addAll(listOf("--set", "$key=$value"))
        }

        return executeHelmCommand(*command.toTypedArray())
    }

    @Tool(name = "get_release_status", description = "Get the status of a Helm release")
    fun getReleaseStatus(
        @ToolParam(description = "Name of the release to check") releaseName: String,
        @ToolParam(description = "The Kubernetes namespace of the release") namespace: String
    ): String {
        return executeHelmCommand("status", releaseName, "--namespace", namespace)
    }

    @Tool(name = "add_repository", description = "Add a Helm repository")
    fun addRepository(
        @ToolParam(description = "Name for the repository") name: String,
        @ToolParam(description = "URL of the repository") url: String
    ): String {
        return executeHelmCommand("repo", "add", name, url)
    }

    @Tool(name = "update_repositories", description = "Update all Helm repositories")
    fun updateRepositories(): String {
        return executeHelmCommand("repo", "update")
    }

    @Tool(name = "show_values", description = "Show the values for a release")
    fun showValues(
        @ToolParam(description = "Name of the release") releaseName: String,
        @ToolParam(description = "The Kubernetes namespace of the release") namespace: String
    ): String {
        return executeHelmCommand("get", "values", releaseName, "--namespace", namespace)
    }

    private fun executeHelmCommand(vararg args: String): String {
        try {
            val command = mutableListOf("helm").apply { addAll(args) }
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroy()
                throw RuntimeException("Helm command timed out")
            }

            if (process.exitValue() != 0) {
                throw RuntimeException("Helm command failed: ${output.toString()}")
            }

            return output.toString()
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute Helm command: ${e.message}")
        }
    }
}