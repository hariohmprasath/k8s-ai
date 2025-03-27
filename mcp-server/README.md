# ⎈ Kubernetes MCP Server

This module serves as the MCP server for the Kubernetes AI Management System, providing comprehensive cluster management capabilities with AI-powered assistance.

## ✨ Overview

The MCP Server is a Spring Boot application that:
- Serves as the central management point for Kubernetes operations
- Provides advanced diagnostics and monitoring capabilities
- Integrates with the Agent module for AI-powered interactions
- Offers a comprehensive API for cluster management

## 🎁 Features

### 🔄 Pod Management
- 📋 List and analyze pods in real-time
- 📝 Smart log analysis with error pattern detection
- 🔍 AI-powered pod diagnostics with recommendations
- ⚡ Secure command execution in pods

### 🎯 Job Management
- 📋 List and analyze jobs in any namespace
- 🔍 Get detailed job status and execution history
- 🗑️ Clean up completed or failed jobs

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

## 🏗️ Building the Module

```bash
# From the mcp-server directory
mvn clean package

# From the project root
mvn clean package -pl mcp-server
```


## 🤝 Integration with Claude Desktop

1. Download the latest JAR file from the [Releases](https://github.com/hariohmprasath/k8s-ai/releases) page
2. Update claude desktop json config to use the new MCP server
```json
{
    "mcpServers": {
      "spring-ai-mcp-k8s": {
        "command": "java",
        "args": [
          "-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-jar",          
          "<<jar_file_location>>"
        ]
      }
    }
  }
```

3. Restart Claude Desktop
4. Use the sample prompts in Claude Desktop to test the new MCP server
