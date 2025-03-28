# 🎯 Kubernetes AI Management System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-18.0.1-326CE5.svg)](https://kubernetes.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7541E8.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> AI-Powered Kubernetes Management (MCP + Agent)

```ascii
    ⎈ K8s AI Management
    ├── 🤖 MCP Server
    ├── 🔍 K8s Tools
    └── 🚀 Agent mode with Rest API
```

## ✨ Overview

This project combines the power of AI with Kubernetes management. Users can perform real-time diagnostics, resource monitoring, and smart log analysis. It simplifies Kubernetes management through conversational AI, providing a modern alternative.

> 💡 Just ask questions naturally - no need to memorize commands!

## 🏗️ Project Structure

The project is organized into the following modules:

- **agent**: Agent mode backed by Rest API to analyze the cluster using natural language
- **mcp-server**: MCP server backed by tools which can be integrated with MCP host (like Claude desktop) to provide a full experience
- **tools**: Kubernetes tools for cluster analysis/management (used by both agent and mcp-server)

## 🎁 Features

This AI-powered system understands natural language queries about your Kubernetes cluster. Here are some of the capabilities provided by the system which can be queried using natural language:

### 🏥 Cluster Health and Diagnostics

- "What's the status of my cluster?"
- "Show me all pods in the default namespace"
- "Are there any failing pods? in default namespace"
- "What's using the most resources in my cluster?"
- "Give me a complete health check of the cluster"
- "Are there any nodes not in Ready state?"
- "Show me pods in default namespace that have been running for more than 7 days"
- "Identify any pods running in default namespace with high restart counts"

### 🌐 Network Analysis

- "Show me the logs for the payment service"
- "List all ingresses in the cluster"
- "Show me all services and their endpoints"
- "Check if my service 'api-gateway' has any endpoints"
- "Show me all exposed services with external IPs"

### 💾 Storage Management

- "List all persistent volumes in the cluster"
- "Show me storage claims that are unbound"
- "What storage classes are available in the cluster?"
- "Which pods are using persistent storage?"
- "Are there any storage volumes nearing capacity?"

### ⏱️ Job and CronJob Analysis

- "List all running jobs in the batch namespace"
- "Show me failed jobs from the last 24 hours"
- "What CronJobs are scheduled to run in the next hour?"
- "Show me the execution history of the 'backup' job"

### ⎈ Helm Release Management

- "List all Helm releases"
- "Upgrade the MongoDB chart to version 12.1.0"
- "What values are configured for my Prometheus release?"
- "Rollback the failed Elasticsearch release"
- "Show me the revision history for my Prometheus release"
- "Compare values between different Helm releases"
- "Check for outdated Helm charts in my cluster"
- "What are the dependencies for my Elasticsearch chart?"


> Note: The system uses AI to analyze patterns in logs, events, and resource usage to provide intelligent diagnostics and recommendations.

## 🛠️ Prerequisites

| Requirement | Version |
|------------|----------|
| ☕ JDK | 17 or later |
| 🧰 Maven | 3.8 or later |
| ⎈ Minikube/Any Kubernetes cluster | Configured `~/.kube/config` |

> **Note:** The system uses the kubeconfig file from `~/.kube/config`, so make sure it is properly configured.

---

## 🏗️ 1. Project Build

```bash
# Build all modules
mvn clean package

# Run the MCP server
java -jar mcp-server/target/mcp-server-1.0-SNAPSHOT.jar

# Alternatively, run the agent directly
java -jar agent/target/agent-*-fat.jar
```

## 🛠️ 2. Minikube setup
Install minikube and create a nginx deployment:

```bash
# Install minikube
brew install minikube

# Start minikube
minikube start

# Make sure kubeconfig is set
kubectl config use-context minikube

# Deploy nginx
kubectl create deployment nginx --image=nginx:latest

# Check whether nginx is running
kubectl get pods
```

> **Note:** You should see `nginx` pod in the output


## 🛠️ 3. Testing project
### 🤝 3.1 MCP Server integration with Claude Desktop

Refer to [mcp-server/README.md](mcp-server/README.md) for instructions on how to integrate with Claude Desktop

### 3.2. Agent Mode with Rest API

Refer to [agent/README.md](agent/README.md) for instructions on how to run the agent

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
