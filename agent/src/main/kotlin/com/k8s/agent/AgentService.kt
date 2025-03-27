package com.k8s.agent

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Service
import kotlin.math.pow


/**
 * Service responsible for handling AI agent interactions for Kubernetes management.
 * Uses Spring AI to communicate with LLM APIs and process user requests.
 */
@Service
open class AgentService(private val chatBuilder: ChatClient.Builder, private val tools: ToolCallbackProvider) {



    companion object {
        private const val MAX_ITERATIONS = 3
        private const val MAX_RETRIES = 3
        
        // HTML template for error messages
        private const val ERROR_HTML_TEMPLATE = """<div style="font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; border-left: 5px solid #e74c3c; background-color: #fadbd8; margin: 15px 0; border-radius: 0 5px 5px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h3 style="color: #c0392b; margin-top: 0; font-size: 18px;">Error Processing Request</h3>
            <p style="margin: 10px 0; line-height: 1.5;">%s</p>
            <p style="margin: 10px 0; line-height: 1.5;">This may be due to a connection timeout with the OpenAI API. Please try again or check your network connection.</p>
            </div>"""
        
        // System prompt for the Kubernetes assistant
        private val GENERATOR_SYSTEM_PROMPT = """
            You are a Kubernetes management assistant with access to specialized tools for managing Kubernetes resources. 
            Your role is to help users manage their Kubernetes cluster by utilizing the provided tool methods directly. 
            
            Important Rules:
            1. NEVER generate or suggest kubectl commands - use the provided tool methods instead
            2. Always use the appropriate tool methods for each operation (e.g., PodTools for pod operations, NetworkTools for network operations)
            3. Ensure operations are safe and confirm potentially dangerous actions with users
            4. When managing resources, always verify the target namespace
            5. For complex operations, break them down into smaller, manageable steps
            6. If namespace is not explicitly specified, use the default namespace
            7. Be concise in your response and try and provide only the necessary information
            
            CRITICAL FORMATTING REQUIREMENT: You MUST format your ENTIRE response as valid HTML with proper styling.
            
            HTML FORMATTING RULES (MANDATORY):
            1. Your COMPLETE response must be valid HTML - DO NOT include any markdown or plain text outside HTML tags
            2. ALWAYS wrap your entire response in a root <div> with appropriate styling
            3. Use semantic HTML elements appropriately:
               - <h1>, <h2>, <h3> for headings (with proper hierarchy)
               - <p> for paragraphs
               - <ul>/<ol> with <li> for lists
               - <table> with <thead>, <tbody>, <tr>, <th>, <td> for tabular data
               - <pre><code> for code blocks with syntax highlighting
               - <strong>, <em>, <span> for text emphasis
            4. Apply consistent styling with inline CSS:
               - Use a clean, professional color scheme
               - Set font-family, font-size, line-height, and margins for readability
               - Use contrasting colors for headings and important information
               - Apply borders, padding, and background colors to create visual separation
            5. For code or command examples:
               - Always use <pre><code> tags with monospace font and syntax highlighting
               - Add background color and padding for better readability
            6. For tables:
               - Use proper table structure with <thead> and <tbody>
               - Apply alternating row colors and border styling
               - Center-align headers and appropriate align data cells
            7. For status information:
               - Use color-coding (<span> with appropriate colors) for success/warning/error states
               - Use icons or symbols for visual indicators when appropriate
            
            EXAMPLE HTML STRUCTURE (follow this pattern):
            <div style="font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px;">
              <h2 style="color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px;">Main Heading</h2>
              <p>Explanatory text goes here with <strong>important points</strong> highlighted.</p>
              <div style="background-color: #f8f9fa; border-left: 4px solid #3498db; padding: 15px; margin: 15px 0;">
                <h3 style="margin-top: 0; color: #2c3e50;">Section Heading</h3>
                <p>Section content with details about the Kubernetes resources.</p>
                <ul style="margin-left: 20px;">
                  <li>List item one with details</li>
                  <li>List item two with details</li>
                </ul>
              </div>
              <pre style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; overflow-x: auto;"><code>Example code or output here</code></pre>
            </div>
            
            Available Tool Categories:
            - Pod Management
            - Node Operations
            - Service Management
            - Storage Operations
            - Scheduling
            - Deployments
            - ConfigMaps and Secrets
            - Network Management
            - Resource Management
            - Jobs and Batch Operations
            - Event Monitoring
            - Health Checks
            - Helm Operations
            
            Remember: Security and stability are paramount. Always validate inputs and handle errors appropriately.
        """.trimIndent()
        
        // System prompt for the evaluator
        private val EVALUATOR_SYSTEM_PROMPT = """
            You are an expert evaluator for Kubernetes management responses. Your task is to assess if the given response 
            properly addresses the user's request using the appropriate Kubernetes management tools.
            
            Evaluate the response based on these criteria:
            1. Does it use the appropriate tool methods instead of kubectl commands?
            2. Does it address all aspects of the user's request?
            3. Is it safe and does it verify namespaces when needed?
            4. Is it concise and provides only necessary information?
            5. Does it handle potential errors appropriately?
            6. HTML FORMATTING (CRITICAL): Is the response properly formatted as valid HTML with appropriate styling?
               - The entire response must be valid HTML (not markdown)
               - Must use proper semantic HTML elements (h1-h6, p, ul/ol, table, pre/code, etc.)
               - Must have consistent, professional styling with inline CSS
               - Must be visually well-structured and readable
               - Must not contain any plain text outside of HTML tags
            
            Provide your evaluation in the following format:
            - RATING: [PASS or NEEDS_IMPROVEMENT]
            - FEEDBACK: [Detailed feedback explaining issues and suggestions for improvement]
            
            If the response needs improvement, be specific about what needs to be fixed and how.
            
            IMPORTANT: If the response is not properly formatted as HTML or contains markdown instead of HTML, 
            ALWAYS rate it as NEEDS_IMPROVEMENT and provide specific feedback on the HTML formatting issues.
        """.trimIndent()
    }

    /**
     * Main entry point for processing user prompts
     * @param userPrompt The user's input request
     * @return HTML-formatted response from the AI
     */
    fun invokeAgent(userPrompt: String): String? {
        try {
            return evaluatorOptimizerLoop(userPrompt)
        } catch (e: Exception) {
            e.printStackTrace()
            return ERROR_HTML_TEMPLATE.format(e.message)
        }
    }

    /**
     * Core processing loop that generates and evaluates responses
     * @param userPrompt The user's input request
     * @return Final HTML-formatted response
     */
    private fun evaluatorOptimizerLoop(userPrompt: String): String {
        var currentResponse: String? = null
        var iterationCount = 0
        val chainOfThought = StringBuilder()

        while (iterationCount < MAX_ITERATIONS) {
            iterationCount++
            
            // Generate response with retry logic
            currentResponse = generateResponse(
                userPrompt = userPrompt,
                currentResponse = currentResponse,
                chainOfThought = chainOfThought,
                iterationCount = iterationCount
            )
            
            // If this is the last iteration, return the response without evaluation
            if (iterationCount == MAX_ITERATIONS) {
                chainOfThought.append("\n\nIteration $iterationCount (final):\n$currentResponse")
                break
            }
            
            // Evaluate response quality
            val evaluation = evaluateResponse(userPrompt, currentResponse)
            chainOfThought.append("\n\nIteration $iterationCount:\n$currentResponse\n\nEvaluation:\n$evaluation")
            
            // Check if the response is satisfactory
            if (evaluation.contains("RATING: PASS")) {
                break
            }
        }
        
        // For debugging purposes, you can log the chain of thought
        // logger.debug("Chain of thought: $chainOfThought")
        
        // Ensure the response is valid HTML
        return ensureHtmlFormat(currentResponse ?: "Failed to generate a response")
    }
    
    /**
     * Generates a response based on user prompt and previous feedback
     */
    private fun generateResponse(
        userPrompt: String,
        currentResponse: String?,
        chainOfThought: StringBuilder,
        iterationCount: Int
    ): String {
        // Create the generation prompt based on whether this is the first iteration
        val generationPrompt = if (currentResponse == null) {
            userPrompt
        } else {
            val feedback = chainOfThought.toString().split("FEEDBACK:").lastOrNull()?.trim() ?: ""
            """Original user request: $userPrompt
               
               Your previous response: $currentResponse
               
               Feedback on your previous response: $feedback
               
               Please provide an improved response that addresses the feedback.""".trimIndent()
        }
        
        // Implement retry logic with exponential backoff
        var retryCount = 0
        var generatedResponse: String? = null
        
        while (generatedResponse == null) {
            try {
                generatedResponse = chatBuilder.build().prompt()
                    .user(generationPrompt)
                    .system(GENERATOR_SYSTEM_PROMPT)
                    .tools(tools)
                    .call().content()
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= MAX_ITERATIONS) throw e
                
                println("API call failed (attempt $retryCount/$MAX_ITERATIONS): ${e.message}. Retrying...")
                Thread.sleep(calculateBackoffMs(retryCount))
            }
        }
        
        return generatedResponse ?: "No response generated"
    }
    
    /**
     * Evaluates the quality of a generated response
     */
    private fun evaluateResponse(userPrompt: String, currentResponse: String?): String {
        val evaluationPrompt = """User request: $userPrompt
                                  
                                  Response to evaluate: $currentResponse
                                  
                                  Evaluate if this response properly addresses the user's request.""".trimIndent()
        
        var evalRetryCount = 0
        var evaluation: String? = null
        
        while (evaluation == null) {
            try {
                evaluation = chatBuilder.build().prompt()
                    .user(evaluationPrompt)
                    .system(EVALUATOR_SYSTEM_PROMPT)
                    .call().content()
            } catch (e: Exception) {
                evalRetryCount++
                if (evalRetryCount >= MAX_RETRIES) {
                    println("Evaluation API call failed after $MAX_RETRIES attempts: ${e.message}. Continuing with current response.")
                    return "RATING: PASS\nFEEDBACK: Unable to evaluate due to API timeout, but continuing with current response."
                }
                
                println("Evaluation API call failed (attempt $evalRetryCount/$MAX_RETRIES): ${e.message}. Retrying...")
                Thread.sleep(calculateBackoffMs(evalRetryCount))
            }
        }
        
        // At this point, evaluation is guaranteed to be non-null because of the while loop condition
        return evaluation
    }
    
    /**
     * Calculates backoff time for retries using exponential backoff
     */
    private fun calculateBackoffMs(retryCount: Int): Long {
        return (1000L * (2.0.pow(retryCount.toDouble()))).toLong()
    }
    
    /**
     * Ensures response is properly formatted as HTML
     * This function performs several checks and transformations:
     * 1. Validates if the response is already valid HTML
     * 2. Converts markdown to HTML if needed
     * 3. Wraps plain text in proper HTML structure
     * 4. Adds styling for better readability
     */
    private fun ensureHtmlFormat(response: String): String {
        val trimmedResponse = response.trim()
        
        // Check if response is already valid HTML (starts with HTML tag and ends with closing tag)
        if (trimmedResponse.startsWith("<") && 
            (trimmedResponse.endsWith(">") || trimmedResponse.endsWith("</div>")) && 
            trimmedResponse.contains("<div")) {
            
            // Basic validation to ensure it has proper HTML structure
            if (validateHtmlStructure(trimmedResponse)) {
                return trimmedResponse
            }
        }
        
        // If it's not valid HTML, convert markdown or wrap in HTML
        val convertedHtml = convertMarkdownToHtml(trimmedResponse)
        
        // Apply default styling wrapper
        return """<div style="font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px;">
            $convertedHtml
        </div>""".trimIndent()
    }
    
    /**
     * Validates if the HTML has proper structure
     * This is a basic check to ensure it has opening and closing tags
     */
    private fun validateHtmlStructure(html: String): Boolean {
        // Check for basic HTML structure requirements
        return html.contains("<div") && 
               html.contains("</div>") && 
               !html.contains("```") && // No markdown code blocks
               !html.contains("\n#") && // No markdown headers
               !html.contains("\n-") // No markdown list items
    }
    
    /**
     * Converts markdown to HTML
     * Handles common markdown patterns and converts them to proper HTML
     */
    private fun convertMarkdownToHtml(markdown: String): String {
        var html = markdown
        
        // Convert markdown code blocks to HTML
        html = html.replace("```([\\w]*)\\n([\\s\\S]*?)\\n```".toRegex()) { matchResult ->
            val language = matchResult.groupValues[1]
            val code = matchResult.groupValues[2]
            """<pre style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; overflow-x: auto;"><code class="language-$language">$code</code></pre>"""
        }
        
        // Convert inline code
        html = html.replace("`([^`]+)`".toRegex()) { matchResult ->
            val code = matchResult.groupValues[1]
            """<code style="background-color: #f5f5f5; padding: 2px 4px; border-radius: 3px; font-family: monospace;">$code</code>"""
        }
        
        // Convert headers
        html = html.replace("^# (.+)$".toRegex(RegexOption.MULTILINE)) { matchResult ->
            val header = matchResult.groupValues[1]
            """<h1 style="color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px;">$header</h1>"""
        }
        
        html = html.replace("^## (.+)$".toRegex(RegexOption.MULTILINE)) { matchResult ->
            val header = matchResult.groupValues[1]
            """<h2 style="color: #2c3e50; border-bottom: 1px solid #3498db; padding-bottom: 8px;">$header</h2>"""
        }
        
        html = html.replace("^### (.+)$".toRegex(RegexOption.MULTILINE)) { matchResult ->
            val header = matchResult.groupValues[1]
            """<h3 style="color: #2c3e50; margin-top: 15px;">$header</h3>"""
        }
        
        // Convert bullet lists
        html = html.replace("^- (.+)$".toRegex(RegexOption.MULTILINE)) { matchResult ->
            val item = matchResult.groupValues[1]
            "<li>$item</li>"
        }
        
        // Wrap lists in <ul> tags
        if (html.contains("<li>")) {
            html = html.replace("(<li>.*?</li>)+".toRegex(RegexOption.DOT_MATCHES_ALL)) { matchResult ->
                """<ul style="margin-left: 20px;">${matchResult.value}</ul>"""
            }
        }
        
        // Convert paragraphs (lines not already converted)
        html = html.replace("^([^<].+)$".toRegex(RegexOption.MULTILINE)) { matchResult ->
            val line = matchResult.groupValues[1]
            if (line.isNotBlank() && !line.startsWith("<")) {
                "<p>$line</p>"
            } else {
                line
            }
        }
        
        // Convert strong/bold
        html = html.replace("\\*\\*([^*]+)\\*\\*".toRegex()) { matchResult ->
            val text = matchResult.groupValues[1]
            "<strong>$text</strong>"
        }
        
        // Convert emphasis/italic
        html = html.replace("\\*([^*]+)\\*".toRegex()) { matchResult ->
            val text = matchResult.groupValues[1]
            "<em>$text</em>"
        }
        
        return html
    }
}