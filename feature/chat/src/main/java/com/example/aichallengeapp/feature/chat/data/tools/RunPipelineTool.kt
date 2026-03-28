package com.example.aichallengeapp.feature.chat.data.tools

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.core.mcp.model.FunctionDefinition
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClientManager
import com.example.aichallengeapp.feature.settings.domain.model.ModelEndpoint
import com.example.aichallengeapp.feature.settings.domain.model.resolveEndpoint
import com.example.aichallengeapp.feature.settings.domain.repository.AppSettingsRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber

class RunPipelineTool(
    private val mcpToolClientManager: McpToolClientManager,
    private val chatRepository: ChatRepository,
    private val settingsRepository: AppSettingsRepository
) : LocalToolHandler {

    override val definition = ToolDefinition(
        function = FunctionDefinition(
            name = TOOL_NAME,
            description = "Execute a sequential pipeline of MCP tool calls with data extraction between steps. " +
                    "Use this when you need to chain multiple tool calls where later calls depend on results from earlier ones. " +
                    "Define all steps upfront. Between tool_call steps, use extract steps to pull specific data from previous results. " +
                    "Use {{variable_name}} placeholders in tool arguments to reference extracted values. " +
                    "IMPORTANT: Each extract step produces a single flat string value. " +
                    "Use separate extract steps for each piece of data with distinct names (e.g. repo_owner, repo_name), " +
                    "NOT dot notation (e.g. repo.owner). Extract exactly one value per step.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("steps") {
                        put("type", "array")
                        put(
                            "description",
                            "Array of pipeline steps. Each step is either: " +
                                    "{\"type\": \"tool_call\", \"tool\": \"<name>\", \"args\": {<json args, may contain {{var}}>}} or " +
                                    "{\"type\": \"extract\", \"prompt\": \"<what to extract from previous result>\", \"output_var\": \"<variable name>\"}"
                        )
                        putJsonObject("items") {
                            put("type", "object")
                        }
                    }
                    putJsonObject("summary_prompt") {
                        put("type", "string")
                        put("description", "Instruction for final summarization of all pipeline results")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("steps"))
                    add(JsonPrimitive("summary_prompt"))
                }
            }
        )
    )

    override suspend fun execute(arguments: JsonObject?, chatId: String): String {
        if (arguments == null) return "Missing arguments for run_pipeline"
        val endpoint = settingsRepository.load().resolveEndpoint()

        val stepsElement = arguments["steps"] ?: return "Missing required parameter: steps"
        val steps: JsonArray = try {
            stepsElement.jsonArray
        } catch (_: Exception) {
            return "Parameter 'steps' must be a JSON array"
        }
        val summaryPrompt = arguments["summary_prompt"]?.jsonPrimitive?.contentOrNull
            ?: return "Missing required parameter: summary_prompt"

        if (steps.isEmpty()) return "Pipeline has no steps"

        val effectiveSteps = if (steps.size > MAX_STEPS) {
            Timber.tag(TAG).w("Pipeline has ${steps.size} steps, limiting to $MAX_STEPS")
            JsonArray(steps.take(MAX_STEPS))
        } else {
            steps
        }

        val total = effectiveSteps.size
        Timber.tag(TAG).i(SEPARATOR)
        Timber.tag(TAG).i("PIPELINE START | $total steps")
        Timber.tag(TAG).i(SEPARATOR)

        val variables = mutableMapOf<String, String>()
        var lastResult = ""
        val allResults = mutableListOf<Pair<String, String>>()

        for ((index, stepElement) in effectiveSteps.withIndex()) {
            val stepNum = index + 1
            val step: JsonObject = try {
                stepElement.jsonObject
            } catch (_: Exception) {
                Timber.tag(TAG).w("$STEP_PREFIX [$stepNum/$total] INVALID FORMAT — skipping")
                continue
            }

            val type = step["type"]?.jsonPrimitive?.contentOrNull
            when (type) {
                "tool_call" -> {
                    val toolName = step["tool"]?.jsonPrimitive?.contentOrNull
                    if (toolName == null) {
                        Timber.tag(TAG).w("$STEP_PREFIX [$stepNum/$total] TOOL_CALL — missing 'tool' field, skipping")
                        continue
                    }

                    val rawArgs = step["args"]?.jsonObject
                    val resolvedArgs = rawArgs?.let { resolveTemplateArgs(it, variables) }

                    val unresolvedVars = resolvedArgs?.let { findUnresolvedVars(it) } ?: emptyList()
                    if (unresolvedVars.isNotEmpty()) {
                        val errorMsg = "Unresolved variables: ${unresolvedVars.joinToString()}"
                        Timber.tag(TAG).e("$STEP_PREFIX [$stepNum/$total] TOOL_CALL '$toolName' SKIPPED | $errorMsg | available: ${variables.keys}")
                        allResults.add("tool_call: $toolName (skipped)" to errorMsg)
                        lastResult = errorMsg
                        continue
                    }

                    val serverName = mcpToolClientManager.getServerName(toolName) ?: "unknown"
                    Timber.tag(TAG).i("$STEP_PREFIX [$stepNum/$total] TOOL_CALL '$toolName' [$serverName] | args=$resolvedArgs")

                    try {
                        val result = mcpToolClientManager.callTool(toolName, resolvedArgs)
                        val resultText = result.content.mapNotNull { it.text }.joinToString("\n")
                        val preview = resultText.take(200).replace("\n", " ")

                        if (result.isError == true) {
                            Timber.tag(TAG).e("$STEP_PREFIX [$stepNum/$total] TOOL_CALL '$toolName' FAILED | $preview")
                            allResults.add("tool_call: $toolName (error)" to resultText)
                        } else {
                            Timber.tag(TAG).i("$STEP_PREFIX [$stepNum/$total] TOOL_CALL '$toolName' OK | ${resultText.length} chars | $preview")
                            allResults.add("tool_call: $toolName" to resultText)
                        }
                        lastResult = resultText
                    } catch (e: Exception) {
                        val errorMsg = "Tool call failed: ${e.message}"
                        Timber.tag(TAG).e("$STEP_PREFIX [$stepNum/$total] TOOL_CALL '$toolName' EXCEPTION | ${e.message}")
                        allResults.add("tool_call: $toolName (error)" to errorMsg)
                        lastResult = errorMsg
                    }
                }

                "extract" -> {
                    val prompt = step["prompt"]?.jsonPrimitive?.contentOrNull
                    val outputVar = step["output_var"]?.jsonPrimitive?.contentOrNull
                    if (prompt == null || outputVar == null) {
                        Timber.tag(TAG).w("$STEP_PREFIX [$stepNum/$total] EXTRACT — missing 'prompt' or 'output_var', skipping")
                        continue
                    }

                    Timber.tag(TAG).i("$STEP_PREFIX [$stepNum/$total] EXTRACT | var='$outputVar' | prompt='$prompt'")

                    val extractedValue = try {
                        extractData(lastResult, prompt, endpoint)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e("$STEP_PREFIX [$stepNum/$total] EXTRACT FAILED | ${e.message}")
                        ""
                    }

                    Timber.tag(TAG).i("$STEP_PREFIX [$stepNum/$total] EXTRACT OK | $outputVar = '$extractedValue'")
                    variables[outputVar] = extractedValue
                    lastResult = extractedValue
                }

                else -> {
                    Timber.tag(TAG).w("$STEP_PREFIX [$stepNum/$total] UNKNOWN TYPE '$type' — skipping")
                }
            }
        }

        if (allResults.isEmpty()) return "Pipeline completed with no results"

        Timber.tag(TAG).i(SEPARATOR)
        Timber.tag(TAG).i("SUMMARIZE | ${allResults.size} results | prompt='${summaryPrompt.take(80)}'")
        val summary = summarize(allResults, summaryPrompt, endpoint)
        Timber.tag(TAG).i("SUMMARIZE OK | ${summary.length} chars")
        Timber.tag(TAG).i("PIPELINE DONE | ${allResults.size} results collected")
        Timber.tag(TAG).i(SEPARATOR)
        return summary
    }

    private suspend fun extractData(data: String, prompt: String, endpoint: ModelEndpoint): String {
        val messages = listOf(
            ChatMessage(
                role = ChatMessage.ROLE_SYSTEM,
                content = "You are a data extractor. Extract exactly what is requested. Respond with ONLY the extracted value, nothing else."
            ),
            ChatMessage(
                role = ChatMessage.ROLE_USER,
                content = "Data:\n$data\n\nExtract: $prompt"
            )
        )
        val result = chatRepository.sendMessage(
            messages = messages,
            maxTokens = 200,
            temperature = 0.1f,
            model = endpoint.modelId,
            baseUrlOverride = endpoint.baseUrlOverride,
            apiKeyOverride = endpoint.apiKeyOverride
        )
        return result.getOrNull()?.message?.trim() ?: ""
    }

    private suspend fun summarize(
        allResults: List<Pair<String, String>>,
        summaryPrompt: String,
        endpoint: ModelEndpoint
    ): String {
        val dataBlock = allResults.joinToString("\n\n") { (label, result) ->
            "## $label\n$result"
        }
        val messages = listOf(
            ChatMessage(
                role = ChatMessage.ROLE_SYSTEM,
                content = "You are a helpful assistant that summarizes data."
            ),
            ChatMessage(
                role = ChatMessage.ROLE_USER,
                content = "Data:\n$dataBlock\n\nTask: $summaryPrompt"
            )
        )
        val result = chatRepository.sendMessage(
            messages = messages,
            maxTokens = 1000,
            temperature = 0.3f,
            model = endpoint.modelId,
            baseUrlOverride = endpoint.baseUrlOverride,
            apiKeyOverride = endpoint.apiKeyOverride
        )
        return result.getOrNull()?.message ?: "Pipeline completed but summarization failed"
    }

    private fun resolveTemplate(template: String, variables: Map<String, String>): String {
        return unresolvedPattern.replace(template) { match ->
            val varName = match.groupValues[1]
            when {
                variables.containsKey(varName) -> variables[varName]!!
                varName.contains(".") -> {
                    val parent = varName.substringBefore(".")
                    variables[parent] ?: match.value
                }
                else -> match.value
            }
        }
    }

    private fun resolveTemplateArgs(args: JsonObject, variables: Map<String, String>): JsonObject {
        if (variables.isEmpty()) return args
        val resolved = mutableMapOf<String, JsonElement>()
        for ((key, value) in args) {
            resolved[key] = resolveJsonElement(value, variables)
        }
        return JsonObject(resolved)
    }

    private fun resolveJsonElement(element: JsonElement, variables: Map<String, String>): JsonElement {
        return when (element) {
            is JsonPrimitive -> {
                if (!element.isString) return element
                val content = element.contentOrNull ?: return element
                val resolved = resolveTemplate(content, variables)
                if (resolved == content) element else JsonPrimitive(resolved)
            }
            is JsonObject -> {
                val resolved = element.mapValues { (_, v) -> resolveJsonElement(v, variables) }
                JsonObject(resolved)
            }
            is JsonArray -> {
                JsonArray(element.map { resolveJsonElement(it, variables) })
            }
        }
    }

    private val unresolvedPattern = Regex("""\{\{(.+?)\}\}""")

    private fun findUnresolvedVars(args: JsonObject): List<String> {
        val json = args.toString()
        return unresolvedPattern.findAll(json).map { it.groupValues[1] }.toList()
    }

    companion object {
        private const val TAG = "Pipeline"
        private const val TOOL_NAME = "run_pipeline"
        private const val MAX_STEPS = 10
        private const val SEPARATOR = "═══════════════════════════════════════════════════"
        private const val STEP_PREFIX = "▶"
    }
}
