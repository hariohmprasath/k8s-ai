package com.k8s.agent

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(scanBasePackages = ["com"])
@RestController
@RequestMapping("/api/v1")
open class K8sAgentService(var agentService: AgentService) {
    @PostMapping("/chat", consumes = [org.springframework.http.MediaType.TEXT_PLAIN_VALUE])
    fun invokeChat(@RequestBody userPrompt: String) : String? {
        return agentService.invokeAgent(userPrompt)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(K8sAgentService::class.java, *args)
}