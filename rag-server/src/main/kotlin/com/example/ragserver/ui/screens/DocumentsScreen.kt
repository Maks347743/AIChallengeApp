package com.example.ragserver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ragserver.data.Document
import com.example.ragserver.data.DocumentStorage
import com.example.ragserver.deepwiki.DeepWikiImportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import javax.swing.JFileChooser

@Composable
fun DocumentsScreen(documentStorage: DocumentStorage, deepWikiImportService: DeepWikiImportService) {
    var documents by remember { mutableStateOf(documentStorage.loadAll()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeepWikiDialog by remember { mutableStateOf(false) }
    var showFolderImportDialog by remember { mutableStateOf(false) }
    var folderImportPath by remember { mutableStateOf<File?>(null) }
    var prefillFromFile by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (documents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No documents. Click + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(documents) { doc ->
                    DocumentItem(doc, documentStorage.pathOf(doc.id)) {
                        documentStorage.delete(doc.id)
                        documents = documentStorage.loadAll()
                    }
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { showDeepWikiDialog = true }) {
                Text("Import DeepWiki")
            }
            OutlinedButton(onClick = {
                val folder = pickFolder()
                if (folder != null) {
                    folderImportPath = folder
                    showFolderImportDialog = true
                }
            }) {
                Text("Import Folder")
            }
            OutlinedButton(onClick = {
                val file = pickFile()
                if (file != null) {
                    prefillFromFile = Triple(file.nameWithoutExtension, file.name, file.readText())
                    showAddDialog = true
                }
            }) {
                Text("Open File")
            }
            Button(onClick = { showAddDialog = true }) {
                Text("+ Add Document")
            }
        }
    }

    if (showAddDialog) {
        AddDocumentDialog(
            prefill = prefillFromFile,
            onDismiss = {
                showAddDialog = false
                prefillFromFile = null
            },
            onSave = { title, source, content ->
                documentStorage.create(title, source, content)
                documents = documentStorage.loadAll()
                showAddDialog = false
                prefillFromFile = null
            }
        )
    }

    if (showDeepWikiDialog) {
        DeepWikiImportDialog(
            importService = deepWikiImportService,
            onDismiss = {
                showDeepWikiDialog = false
                documents = documentStorage.loadAll()
            }
        )
    }

    if (showFolderImportDialog && folderImportPath != null) {
        FolderImportDialog(
            folder = folderImportPath!!,
            documentStorage = documentStorage,
            onDismiss = {
                showFolderImportDialog = false
                folderImportPath = null
                documents = documentStorage.loadAll()
            }
        )
    }
}

@Composable
private fun DeepWikiImportDialog(
    importService: DeepWikiImportService,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var repoUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = { Text("Import from DeepWiki") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository (e.g. JetBrains/compose-multiplatform)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting,
                    singleLine = true
                )
                if (logs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Logs:", style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(
                            onClick = {
                                val selection = StringSelection(logs.joinToString("\n"))
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                            }
                        ) { Text("Copy") }
                    }
                    Card(Modifier.fillMaxWidth().height(200.dp)) {
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
                if (isImporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text("Importing...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (!isDone) {
                Button(
                    onClick = {
                        isImporting = true
                        logs.clear()
                        scope.launch {
                            importService.importRepo(repoUrl) { msg -> logs.add(msg) }
                            isImporting = false
                            isDone = true
                        }
                    },
                    enabled = repoUrl.isNotBlank() && !isImporting
                ) {
                    Text("Import")
                }
            } else {
                Button(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun FolderImportDialog(
    folder: File,
    documentStorage: DocumentStorage,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var isImporting by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    LaunchedEffect(Unit) {
        isImporting = true
        scope.launch {
            withContext(Dispatchers.IO) {
                val extensions = setOf("md", "txt", "rst")
                val files = folder.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in extensions }
                    .toList()

                logs.add("Found ${files.size} files in ${folder.absolutePath}")

                if (files.isEmpty()) {
                    logs.add("No .md, .txt or .rst files found.")
                    isImporting = false
                    isDone = true
                    return@withContext
                }

                val existingBySource = documentStorage.loadAll().associateBy { it.source }
                var imported = 0
                var updated = 0

                files.forEach { file ->
                    val source = file.absolutePath
                    val existing = existingBySource[source]
                    if (existing != null) {
                        documentStorage.delete(existing.id)
                        updated++
                    } else {
                        imported++
                    }
                    documentStorage.create(
                        title = file.nameWithoutExtension,
                        source = source,
                        content = file.readText()
                    )
                    logs.add("${if (existing != null) "Updated" else "Imported"}: ${file.name}")
                }

                logs.add("Done. $imported new, $updated updated. Go to Indexing tab to vectorize.")
            }
            isImporting = false
            isDone = true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = { Text("Import Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    folder.absolutePath,
                    style = MaterialTheme.typography.bodySmall
                )
                if (logs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Logs:", style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(
                            onClick = {
                                val selection = StringSelection(logs.joinToString("\n"))
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                            }
                        ) { Text("Copy") }
                    }
                    Card(Modifier.fillMaxWidth().height(200.dp)) {
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
                if (isImporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text("Importing...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, enabled = isDone) { Text("Done") }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private fun pickFolder(): File? {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.dialogTitle = "Select a folder to import"
    chooser.isAcceptAllFileFilterUsed = false
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun pickFile(): File? {
    val dialog = FileDialog(null as Frame?, "Select a document", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name ->
        name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".rst")
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(dir, file)
}

@Composable
private fun DocumentItem(doc: Document, savedPath: String, onDelete: () -> Unit) {
    val wordCount = doc.content.split(Regex("\\s+")).size
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium)
                Text(doc.source, style = MaterialTheme.typography.bodySmall)
                Text(savedPath, style = MaterialTheme.typography.bodySmall)
                Text("$wordCount words", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun AddDocumentDialog(
    prefill: Triple<String, String, String>?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember(prefill) { mutableStateOf(prefill?.first ?: "") }
    var source by remember(prefill) { mutableStateOf(prefill?.second ?: "") }
    var content by remember(prefill) { mutableStateOf(prefill?.third ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source (e.g. README.md)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, source, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
