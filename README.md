<div align="center">

# 🎯 Kubernetes MCP Server

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.x-326CE5.svg)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> Your AI-Powered Kubernetes Control Plane

```ascii
    ⎈ K8s MCP Server
    ├── 🤖 AI-Powered
    ├── 🔍 Smart Diagnostics
    ├── 🛡️ Enhanced Security
    └── 🚀 Developer Friendly
```

</div>

## ✨ Overview

Welcome to the next generation of Kubernetes management! This Spring Boot-based MCP server combines the power of AI with cluster management capabilities.

## 🎁 Features

### 🔄 Pod Management
- 📋 List and analyze pods in real-time
- 📝 Smart log analysis with error pattern detection
- 🔍 AI-powered pod diagnostics with recommendations
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
| 🐘 Gradle | 7.x or later |
| ⎈ Kubernetes | Configured `~/.kube/config` |
| 🎡 Helm | CLI installed |

> **Note:** MCP tool always uses the kubeconfig file from `~/.kube/config`, so make sure it is properly configured.

## 🏗️ Building the Project

```bash
./gradlew clean build
```

## 🤝 Integration with Claude Desktop

1. Install Claude Desktop
2. Configure the MCP server connection:
```json
   {
    "mcpServers": {
      "spring-ai-mcp-weather": {
        "command": "java",
        "args": [
          "-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-jar",          
          "<<location>>/k8s-0.0.1-SNAPSHOT.jar"
        ]
      }
    }
  }
```

## Integration with Other MCP Hosts

The server follows the standard MCP protocol and can be integrated with any MCP host that supports Spring-based MCP servers. Configure your host to point to the server's URL.

## 🗣️ Natural Language Interactions

> 💡 Just ask questions naturally - no need to memorize commands!

This AI-powered MCP server understands natural language queries about your Kubernetes cluster. Here are examples of questions you can ask:

### 🏥 Cluster Health and Diagnostics
```markdown
📊 What's the overall health of my cluster?
🔍 Are there any resource bottlenecks in the 'production' namespace?
🚨 Show me problematic pods in the 'dev' namespace with recommendations
📅 What events happened in the cluster in the last hour?
```

### 📱 Application Management
```markdown
📋 List all pods in the 'backend' namespace and their status
❓ Why is the 'auth-service' pod failing to start?
📝 Show me the logs from the 'payment-processor' pod with error highlighting
📈 What's using the most resources in the 'monitoring' namespace?
```

### ⎈ Helm Release Management
```markdown
📦 What Helm releases are installed in the 'staging' namespace?
⚡ Install the 'prometheus' chart from the official repository
⚙️ What values are configured for the 'elasticsearch' release?
🔄 Update the 'kafka' release to version 2.0.0
```

> **Note:** I have noticed sometimes LLM will try and generate kubectl commands on the fly, since we would like to stick with the existing MCP tools you can suffix the prompt with "use existing MCP tools, dont generate kubectl commands"

## 🌟 Unique Features

> What makes our MCP server special? Let's dive in!

### 🔍 Advanced Diagnostics
| Feature | Description |
|---------|-------------|
| 🤖 **Intelligent Pod Analysis** | Automatically detects common failure patterns and provides targeted recommendations |
| 📊 **Resource Bottleneck Detection** | Proactively identifies resource constraints across namespaces |
| 🎯 **Smart Event Analysis** | Categorizes events by severity and impact, helping prioritize issues |

### 🛡️ Enhanced Security
| Feature | Description |
|---------|-------------|
| 🔒 **Secure Command Execution** | Built-in validation and sanitization for pod exec commands |
| 🏰 **Namespace Isolation** | Strong namespace-based access controls |
| 📝 **Audit Logging** | Comprehensive logging of all operations with context |

### ⎈ Helm Integration
| Feature | Description |
|---------|-------------|
| 🎯 **Smart Chart Management** | Validates chart compatibility before installation |
| ✅ **Value Validation** | Checks Helm values against schema before applying |
| 📊 **Release Tracking** | Monitors release health and configuration drift |

### 👩‍💻 Developer Experience
| Feature | Description |
|---------|-------------|
| 🗣️ **Natural Language Interface** | No need to memorize kubectl commands |
| 💡 **Contextual Help** | Provides relevant suggestions based on cluster state |
| 🔍 **Rich Error Information** | Detailed error messages with troubleshooting steps |

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
<div align="center">
Made with ❤️ by a naive k8s and MCP fan
</div>
