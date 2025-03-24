## Prompt for finding buggy deployments
Identify problematic deployments in my Kubernetes cluster using the MCP tool. For each affected deployment:

Diagnose the root cause by analyzing the associated pod status and logs.

Categorize the issues (e.g., Scheduling, Resource Limits, CrashLoopBackOff, ImagePullFailure, Networking, Storage).

Generate concise recommendations based on the findings.

Response Format:

Deployment Name: <deployment-name>

Issue Category: <category>

Root Cause: <concise description of issue>

Recommendation: <actionable suggestion>

Only use the MCP tool and server for analysis. Do not generate Kubernetes commands or suggest manual CLI operations. Present the response in a structured, easy-to-read format. Be concise and to the point, dont use extra words.

## Prompt to analyze cluster events
Identify and analyze cluster events in my Kubernetes cluster using the MCP tool. For each event:

Diagnose the root cause by analyzing the associated pod status and logs.

Categorize the issues (e.g., Scheduling, Resource Limits, CrashLoopBackOff, ImagePullFailure, Networking, Storage).

Generate concise recommendations based on the findings.

Response Format (tabular):

Event Name: <event-name>

Issue Category: <category>

Root Cause: <concise description of issue>

Recommendation: <actionable suggestion>

Only use the MCP tool and server for analysis. Do not generate Kubernetes commands or suggest manual CLI operations. Present the response in a structured, easy-to-read format. Be concise and to the point, dont use extra words.

## Prompt for Helm Release Management
Analyze and manage Helm releases in my Kubernetes cluster using the MCP tool. For each operation:

List and analyze existing Helm releases in the specified namespace.

Provide status information about specific releases including:
- Release health
- Current version
- Configured values
- Any deployment issues

For installations/upgrades:
- Validate chart compatibility
- Confirm repository availability
- Verify namespace exists
- Check for potential conflicts

Response Format:

Operation Type: <list|install|upgrade|status>

Target Release: <release-name>

Status: <current-state>

Details:
- Version: <chart-version>
- Namespace: <namespace>
- Health: <health-status>

Recommendations:
<actionable-items-if-any>

Only use the MCP Helm tools for analysis. Present information in a clear, structured format focusing on key details. Be concise and actionable.
