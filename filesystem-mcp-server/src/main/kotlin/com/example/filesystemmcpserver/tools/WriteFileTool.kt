package com.example.filesystemmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val LCS_LINE_LIMIT = 500
private const val CONTEXT_LINES = 3
private const val MIN_HUNK_GAP = 6

class WriteFileTool(private val projectDir: String) : FilesystemToolHandler {

    override val name = "write_file"

    override val description =
        "Writes (creates or overwrites) a file at the given path with the provided content. Returns a unified diff of the changes."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Relative path to the file from the project root.")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "The content to write to the file.")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("path"))
            add(JsonPrimitive("content"))
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val path = (arguments?.get("path") as? JsonPrimitive)?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required argument: path")),
                isError = true
            )
        val content = (arguments.get("content") as? JsonPrimitive)?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required argument: content")),
                isError = true
            )

        return withContext(Dispatchers.IO) {
            try {
                val file = sandboxedPath(projectDir, path)
                val oldContent = if (file.exists() && file.isFile) file.readText() else ""

                file.parentFile?.mkdirs()
                file.writeText(content)

                val diff = generateUnifiedDiff(
                    oldLines = oldContent.lines(),
                    newLines = content.lines(),
                    filePath = path
                )

                McpCallToolResult(content = listOf(McpContent(text = "DIFF:\n$diff")))
            } catch (e: IllegalArgumentException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = e.message ?: "Invalid path")),
                    isError = true
                )
            } catch (e: Exception) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "Error writing file: ${e.message}")),
                    isError = true
                )
            }
        }
    }

    private fun generateUnifiedDiff(
        oldLines: List<String>,
        newLines: List<String>,
        filePath: String
    ): String {
        // For very large files avoid O(n²) LCS computation
        if (oldLines.size > LCS_LINE_LIMIT && newLines.size > LCS_LINE_LIMIT) {
            return buildSimpleReplaceDiff(oldLines, newLines, filePath)
        }

        val edits = computeEdits(oldLines, newLines)

        if (edits.all { it.type == EditType.EQUAL }) {
            return "(no changes)"
        }

        val hunks = buildHunks(edits, oldLines, newLines)

        val isNewFile = oldLines.isEmpty() || (oldLines.size == 1 && oldLines[0].isEmpty())
        val header = if (isNewFile) {
            "--- /dev/null\n+++ b/$filePath"
        } else {
            "--- a/$filePath\n+++ b/$filePath"
        }

        return buildString {
            appendLine(header)
            hunks.forEach { hunk ->
                appendLine(hunk.header)
                hunk.lines.forEach { appendLine(it) }
            }
        }.trimEnd()
    }

    // ── Edit model ────────────────────────────────────────────────────────────

    private enum class EditType { EQUAL, DELETE, INSERT }

    private data class Edit(val type: EditType, val oldIndex: Int, val newIndex: Int)

    // ── LCS + backtrack to produce flat edit list ─────────────────────────────

    private fun computeEdits(oldLines: List<String>, newLines: List<String>): List<Edit> {
        val m = oldLines.size
        val n = newLines.size

        val lcs = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                lcs[i][j] = if (oldLines[i - 1] == newLines[j - 1]) {
                    lcs[i - 1][j - 1] + 1
                } else {
                    maxOf(lcs[i - 1][j], lcs[i][j - 1])
                }
            }
        }

        val edits = mutableListOf<Edit>()
        var i = m
        var j = n
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1] -> {
                    edits.add(Edit(EditType.EQUAL, i - 1, j - 1))
                    i--; j--
                }
                j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j]) -> {
                    edits.add(Edit(EditType.INSERT, i, j - 1))
                    j--
                }
                else -> {
                    edits.add(Edit(EditType.DELETE, i - 1, j))
                    i--
                }
            }
        }
        edits.reverse()
        return edits
    }

    // ── Hunk building with context lines ─────────────────────────────────────

    private data class Hunk(val header: String, val lines: List<String>)

    private fun buildHunks(
        edits: List<Edit>,
        oldLines: List<String>,
        newLines: List<String>
    ): List<Hunk> {
        // Indices in edits[] where something changed
        val changeIndices = edits.indices.filter { edits[it].type != EditType.EQUAL }
        if (changeIndices.isEmpty()) return emptyList()

        // Cluster changed-edit indices: start a new cluster whenever the gap of
        // equal lines between two changes is >= MIN_HUNK_GAP (they won't share context)
        val clusterRanges = mutableListOf<IntRange>()
        var clusterStart = changeIndices[0]
        var clusterEnd = changeIndices[0]

        for (k in 1 until changeIndices.size) {
            // Count how many of the gap edits are EQUAL lines
            val equalGap = (changeIndices[k - 1] + 1 until changeIndices[k])
                .count { edits[it].type == EditType.EQUAL }
            if (equalGap >= MIN_HUNK_GAP) {
                clusterRanges.add(clusterStart..clusterEnd)
                clusterStart = changeIndices[k]
            }
            clusterEnd = changeIndices[k]
        }
        clusterRanges.add(clusterStart..clusterEnd)

        return clusterRanges.map { cluster ->
            val editStart = maxOf(0, cluster.first - CONTEXT_LINES)
            val editEnd = minOf(edits.size - 1, cluster.last + CONTEXT_LINES)
            val hunkEdits = edits.subList(editStart, editEnd + 1)

            // Determine old/new starting line numbers and counts for the @@ header
            var oldStart = Int.MAX_VALUE
            var newStart = Int.MAX_VALUE
            var oldCount = 0
            var newCount = 0

            for (edit in hunkEdits) {
                when (edit.type) {
                    EditType.EQUAL -> {
                        oldStart = minOf(oldStart, edit.oldIndex + 1)
                        newStart = minOf(newStart, edit.newIndex + 1)
                        oldCount++
                        newCount++
                    }
                    EditType.DELETE -> {
                        oldStart = minOf(oldStart, edit.oldIndex + 1)
                        newStart = minOf(newStart, edit.newIndex + 1)
                        oldCount++
                    }
                    EditType.INSERT -> {
                        oldStart = minOf(oldStart, edit.oldIndex + 1)
                        newStart = minOf(newStart, edit.newIndex + 1)
                        newCount++
                    }
                }
            }

            if (oldStart == Int.MAX_VALUE) oldStart = 1
            if (newStart == Int.MAX_VALUE) newStart = 1

            val hunkHeader = "@@ -$oldStart,$oldCount +$newStart,$newCount @@"
            val hunkLines = hunkEdits.map { edit ->
                when (edit.type) {
                    EditType.EQUAL -> " ${oldLines[edit.oldIndex]}"
                    EditType.DELETE -> "-${oldLines[edit.oldIndex]}"
                    EditType.INSERT -> "+${newLines[edit.newIndex]}"
                }
            }

            Hunk(hunkHeader, hunkLines)
        }
    }

    // ── Fallback for very large files ─────────────────────────────────────────

    private fun buildSimpleReplaceDiff(
        oldLines: List<String>,
        newLines: List<String>,
        filePath: String
    ): String {
        val isNewFile = oldLines.isEmpty() || (oldLines.size == 1 && oldLines[0].isEmpty())
        val header = if (isNewFile) {
            "--- /dev/null\n+++ b/$filePath"
        } else {
            "--- a/$filePath\n+++ b/$filePath"
        }
        return buildString {
            appendLine(header)
            appendLine("@@ -1,${oldLines.size} +1,${newLines.size} @@")
            oldLines.forEach { appendLine("-$it") }
            newLines.forEach { appendLine("+$it") }
        }.trimEnd()
    }
}
