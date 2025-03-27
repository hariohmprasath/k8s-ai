# 🧰 Kubernetes Tools Module

This module provides the core Kubernetes integration utilities for the Kubernetes AI Management System, offering a comprehensive set of tools for interacting with Kubernetes clusters.

## ✨ Overview

The Tools module is a library that:
- Provides a unified interface for Kubernetes API operations
- Offers utilities for pod, deployment, service, and job management
- Includes Helm chart management capabilities
- Implements security and validation for Kubernetes operations

## 🎁 Features

### 🔄 Kubernetes API Integration
- 📋 Pod lifecycle management
- 🔍 Resource monitoring and metrics collection
- 📝 Log retrieval and analysis
- ⚙️ Configuration management

### ⎈ Helm Utilities
- 📦 Chart installation and management
- 🔄 Release upgrades and rollbacks
- 🗃️ Repository management
- 📊 Values configuration and tracking

### 🛡️ Security Utilities
- 🔐 RBAC validation
- 🔒 Secure command execution
- 🔍 Permission checking
- 🛑 Operation validation

## 🛠️ Prerequisites

| Requirement | Version |
|------------|----------|
| ☕ JDK | 17 or later |
| 🧰 Maven | 3.8 or later |
| ⎈ Kubernetes | Client libraries |

## 🏗️ Building the Module

```bash
# From the tools directory
mvn clean package

# From the project root
mvn clean package -pl tools
```

## 🔧 Usage

This module is used as a dependency by other modules in the project. To include it in another module, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com</groupId>
    <artifactId>tools</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 📚 Key Components

- **KubernetesClient**: Factory for creating and managing Kubernetes client connections
- **PodManager**: Utilities for pod operations and lifecycle management
- **LogRetriever**: Tools for fetching and analyzing pod logs
- **HelmManager**: Interface for Helm chart operations
- **ResourceMonitor**: Utilities for monitoring resource usage and metrics
- **SecurityValidator**: Tools for validating operations against RBAC policies

## 🤝 Integration with Other Modules

This tools module is used by:
- **agent**: For processing Kubernetes operations requested via natural language
- **mcp-server**: For implementing the management control plane functionality

It serves as the foundation for all Kubernetes interactions in the system.
