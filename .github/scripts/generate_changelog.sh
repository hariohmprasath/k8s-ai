#!/bin/bash

# Get commits since last tag or last 20 commits if no tag exists
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
if [ -z "$LAST_TAG" ]; then
  COMMITS=$(git log -n 20 --pretty=format:"- %s (%an)" | sed 's/^/  /')
else
  COMMITS=$(git log ${LAST_TAG}..HEAD --pretty=format:"- %s (%an)" | sed 's/^/  /')
fi

# Create changelog content
cat > changelog.md << EOF
# Changes in this release
$COMMITS

## Integration with Claude Desktop

### Option 1: Using the MCP Server + Claude Desktop (Recommended)

1. Download the JAR
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

### Option 2: Using the Agent Server Directly

\`\`\`bash
# Download the Agent JAR file from this release
# Run the K8s AI Agent Server
java -jar agent-*-fat.jar

# The server will start on port 8080 by default
# You can now connect to the API at http://localhost:8080/api/v1/chat
\`\`\`
EOF
