# 🎯 Kubernetes AI Management System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-18.0.1-326CE5.svg)](https://kubernetes.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7541E8.svg)](https://kotlinlang.org/)
[![Next.js](https://img.shields.io/badge/Next.js-14.0.0-black.svg)](https://nextjs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> AI-Powered Kubernetes Management with Modern UI

```ascii
    ⎈ K8s AI Management
    ├── 🤖 AI-Powered Chatbot
    ├── 🔍 Smart Diagnostics
    ├── 🖥️ Modern UI Interface
    └── 🚀 Developer Friendly
```

## ✨ Overview

This project combines the power of AI with Kubernetes management capabilities, providing an intuitive chat interface for interacting with your Kubernetes clusters. The system consists of multiple modules that work together to provide a comprehensive solution.

## 🏗️ Project Structure

The project is organized into the following modules:

- **agent**: Spring Boot application with AI integration for processing natural language queries
- **mcp-server**: Main Spring Boot application that serves as the Management Control Plane
- **tools**: Kubernetes tools and utilities for cluster management
- **ui**: Next.js frontend with shadcn components providing a modern chat interface

## 🎁 Features

### 🤖 AI-Powered Chat Interface

- 💬 Natural language interaction with your Kubernetes cluster
- 📝 Markdown rendering for readable responses
- 📋 Chat history tracking
- 🎨 Modern, responsive UI design

### 🔄 Kubernetes Management

- 📋 List and analyze pods in real-time
- 📝 Smart log analysis with error pattern detection
- 🔍 AI-powered diagnostics with recommendations
- ⚡ Secure command execution in pods

### ⎈ Helm Integration

- 📦 Intelligent chart management
- 🔄 Seamless release upgrades
- 🗃️ Repository management
- 📊 Configuration tracking

### 📈 Event Analysis

- 🎯 Real-time event monitoring
- 🚨 Smart bottleneck detection
- 📱 Live deployment tracking

## 🛠️ Prerequisites

| Requirement | Version |
|------------|----------|
| ☕ JDK | 17 or later |
| 🧰 Maven | 3.8 or later |
| ⎈ Kubernetes | Configured `~/.kube/config` |
| 🎡 Helm | CLI installed |
| 🟢 Node.js | 18 or later (for UI development) |

> **Note:** The system uses the kubeconfig file from `~/.kube/config`, so make sure it is properly configured.

## 🏗️ Building the Project

```bash
# Build all modules
mvn clean package

# Run the MCP server
java -jar mcp-server/target/mcp-server-*-fat.jar

# Alternatively, run the agent directly
java -jar agent/target/agent-*-fat.jar
```

## 🤝 Integration with Claude Desktop

1. Refer to [mcp-server/README.md](mcp-server/README.md) for instructions on how to integrate with Claude Desktop

## 🗣️ Natural Language Interactions

> 💡 Just ask questions naturally - no need to memorize commands!

This AI-powered system understands natural language queries about your Kubernetes cluster. Here are examples of questions you can ask:

### 🏥 Cluster Health and Diagnostics

- "What's the status of my cluster?"
- "Show me all pods in the default namespace"
- "Are there any failing pods?"
- "What's using the most resources in my cluster?"

### 📱 Application Management

- "Deploy a new Redis instance"
- "Scale the frontend deployment to 3 replicas"
- "Restart the authentication service"
- "Show me the logs for the payment service"

### ⎈ Helm Release Management

- "List all Helm releases"
- "Upgrade the MongoDB chart to version 12.1.0"
- "What values are configured for my Prometheus release?"
- "Rollback the failed Elasticsearch release"


> Note: The system uses AI to analyze patterns in logs, events, and resource usage to provide intelligent diagnostics and recommendations.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
