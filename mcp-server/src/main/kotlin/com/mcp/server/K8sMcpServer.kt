package com.mcp.server

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["com"])
open class K8sMcpServer

fun main(args: Array<String>) {
    SpringApplication.run(K8sMcpServer::class.java, *args)
}