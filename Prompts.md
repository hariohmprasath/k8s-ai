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
