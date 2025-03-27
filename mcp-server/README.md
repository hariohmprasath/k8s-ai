# ⎈ Kubernetes MCP Server

This module serves as the MCP server for the Kubernetes AI Management System, providing comprehensive cluster management capabilities with AI-powered assistance.

For useful prompts, checkout the [main README.md](../README.md)

## 🛠️ Prerequisites

| Requirement | Version |
|------------|----------|
| ☕ JDK | 17 or later |
| 🧰 Maven | 3.8 or later |
| ⎈ Kubernetes | Configured `~/.kube/config` |
| 🎡 Helm | CLI installed |

## 🏗️ 1. Project Build

```bash
# From the mcp-server directory
mvn clean package

# From the project root
mvn clean package -pl mcp-server
```


## 🤝 2. Integration with Claude Desktop

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
4. Use the sample prompts provided in [main README.md](../README.md) to test the new MCP server
