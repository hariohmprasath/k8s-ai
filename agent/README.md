# 🤖 Kubernetes AI Agent

This module provides the Webservice layer to invoke kubernetes agent that uses MCP tools to manage your Kubernetes cluster. 

## ✨ Overview

The Agent module is a Spring Boot application that:
- Processes natural language queries about Kubernetes
- Integrates with Spring AI for intelligent responses
- Provides a REST API endpoint for chat interactions
- Formats responses in HTML for easy reading

## 🎁 Features

- 🧠 Natural language understanding of Kubernetes concepts
- 🔄 Integration with Kubernetes API for real-time data
- 📝 HTML formatting for structured responses
- 🔌 REST API for easy integration with frontends

## 🛠️ Prerequisites

| Requirement | Version |
|------------|----------|
| ☕ JDK | 17 or later |
| 🧰 Maven | 3.8 or later |
| ⎈ Kubernetes | Configured `~/.kube/config` |

## 🏗️ Building the Module

```bash
# From the agent directory
mvn clean package

# From the project root
mvn clean package -pl agent
```

## 🚀 Running the Agent

```bash
# Run the agent directly (includes all dependencies)
export OPENAI_API_KEY=your_api_key
java -jar target/agent-*-fat.jar
```

The server will start on port 8080 by default, and you can access the chat API at:
```
http://localhost:8080/api/v1/chat
```

## 🔌 API Usage

The agent exposes a simple REST API for chat interactions:

```
POST /api/v1/chat
Content-Type: text/plain

What pods are running in the default namespace?
```

The response will be HTML-formatted text that can be rendered in a chat interface.

## 🔧 Configuration

The agent can be configured through `application.properties`:

```properties
# Server configuration
server.port=8080

# Spring AI configuration
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4
spring.ai.openai.chat.options.temperature=0.7
```