package com.example.ragserver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ragserver.chunking.FixedSizeChunkingStrategy
import com.example.ragserver.chunking.StructuralChunkingStrategy
import com.example.ragserver.service.IndexingService
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun IndexingScreen(indexingService: IndexingService) {
    val scope = rememberCoroutineScope()
    var useStructural by remember { mutableStateOf(true) }
    val logs = indexingService.logs
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Chunking Strategy", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = useStructural, onClick = { useStructural = true })
            Text("Structural (by headers)")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = !useStructural, onClick = { useStructural = false })
            Text("Fixed Size (500 words, 50 overlap)")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    indexingService.logs.clear()
                    scope.launch {
                        val strategy = if (useStructural) {
                            StructuralChunkingStrategy()
                        } else {
                            FixedSizeChunkingStrategy()
                        }
                        indexingService.runIndexing(strategy)
                    }
                },
                enabled = !indexingService.isRunning
            ) {
                Text(if (indexingService.isRunning) "Running..." else "Run Vectorization")
            }
        }

        val lastRun = indexingService.lastRunTimestamp
        val statusText = buildString {
            append("Index status: ${indexingService.indexedChunks} chunks")
            if (lastRun != null) {
                append(" | last run: ${SimpleDateFormat("HH:mm:ss").format(Date(lastRun))}")
            }
        }
        Text(statusText)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs:", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = {
                    val text = logs.joinToString("\n")
                    val selection = StringSelection(text)
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                },
                enabled = logs.isNotEmpty()
            ) {
                Text("Copy")
            }
        }

        Card(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { log ->
                    Text(log, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
