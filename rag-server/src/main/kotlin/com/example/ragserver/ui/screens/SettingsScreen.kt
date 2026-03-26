package com.example.ragserver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ragserver.ui.SettingsState

private const val API_KEY_MASK_MIN_LENGTH = 12
private const val API_KEY_VISIBLE_CHARS = 6

@Composable
fun SettingsScreen(settings: SettingsState, onSave: () -> Unit) {
    var topKText by remember { mutableStateOf(settings.topK.toString()) }
    var initialKText by remember { mutableStateOf(settings.initialK.toString()) }
    var thresholdText by remember { mutableStateOf(settings.similarityThreshold.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Local Mode ---
        Text("Local Mode", style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Switch(
                checked = settings.useLocalModel,
                onCheckedChange = { settings.useLocalModel = it; onSave() }
            )
            Text("Use Ollama for all AI calls (no cloud APIs needed)")
        }
        if (settings.useLocalModel) {
            OutlinedTextField(
                value = settings.ollamaBaseUrl,
                onValueChange = { settings.ollamaBaseUrl = it },
                label = { Text("Ollama Base URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = settings.ollamaChatModel,
                onValueChange = { settings.ollamaChatModel = it },
                label = { Text("Ollama Chat Model (for rewriting + reranking)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = settings.ollamaEmbeddingModel,
                onValueChange = { settings.ollamaEmbeddingModel = it },
                label = { Text("Ollama Embedding Model") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // --- Query Rewriting ---
        Text("Query Rewriting", style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Switch(
                checked = settings.useQueryRewrite,
                onCheckedChange = { settings.useQueryRewrite = it; onSave() }
            )
            Text(if (settings.useLocalModel) "Enable query rewriting via Ollama" else "Enable query rewriting via DeepSeek")
        }
        MaskedApiKeyField(
            value = settings.deepSeekApiKey,
            onValueChange = { settings.deepSeekApiKey = it },
            label = "DeepSeek API Key",
            enabled = settings.useQueryRewrite && !settings.useLocalModel
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // --- Reranking ---
        Text("Reranking", style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Switch(
                checked = settings.useRerank,
                onCheckedChange = { settings.useRerank = it; onSave() }
            )
            Text(if (settings.useLocalModel) "Enable reranking via Ollama" else "Enable reranking via Jina AI")
        }
        MaskedApiKeyField(
            value = settings.jinaApiKey,
            onValueChange = { settings.jinaApiKey = it },
            label = "Jina API Key",
            enabled = settings.useRerank && !settings.useLocalModel
        )
        OutlinedTextField(
            value = thresholdText,
            onValueChange = {
                thresholdText = it
                it.toFloatOrNull()?.let { f -> settings.similarityThreshold = f }
            },
            label = { Text("Similarity Threshold (0.0–1.0)") },
            enabled = settings.useRerank,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // --- Retrieval ---
        Text("Retrieval", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = topKText,
            onValueChange = {
                topKText = it
                it.toIntOrNull()?.let { n -> settings.topK = n }
            },
            label = { Text("Top K (final results)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = initialKText,
            onValueChange = {
                initialKText = it
                it.toIntOrNull()?.let { n -> settings.initialK = n }
            },
            label = { Text("Initial K (candidates before reranking)") },
            enabled = settings.useRerank,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                topKText.toIntOrNull()?.let { settings.topK = it }
                initialKText.toIntOrNull()?.let { settings.initialK = it }
                thresholdText.toFloatOrNull()?.let { settings.similarityThreshold = it }
                onSave()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

        Text(
            "Settings saved to ~/.ragserver/config.json (outside project directory, not tracked by git)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MaskedApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }

    val maskedTransformation = remember {
        VisualTransformation { text ->
            val innerText = text.text
            val masked = if (innerText.length > API_KEY_MASK_MIN_LENGTH) {
                innerText.take(API_KEY_VISIBLE_CHARS) + "•".repeat(innerText.length - API_KEY_MASK_MIN_LENGTH) + innerText.takeLast(API_KEY_VISIBLE_CHARS)
            } else {
                innerText
            }
            TransformedText(AnnotatedString(masked), OffsetMapping.Identity)
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        visualTransformation = if (isFocused) VisualTransformation.None else maskedTransformation,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
    )
}
