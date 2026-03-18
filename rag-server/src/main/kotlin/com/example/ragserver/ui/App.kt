package com.example.ragserver.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ragserver.data.DocumentStorage
import com.example.ragserver.deepwiki.DeepWikiImportService
import com.example.ragserver.service.IndexingService
import com.example.ragserver.ui.screens.DocumentsScreen
import com.example.ragserver.ui.screens.IndexingScreen
import com.example.ragserver.ui.screens.SettingsScreen

@Composable
fun App(
    indexingService: IndexingService,
    documentStorage: DocumentStorage,
    deepWikiImportService: DeepWikiImportService,
    settings: SettingsState
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Documents", "Indexing", "Settings")

    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DocumentsScreen(documentStorage, deepWikiImportService)
                1 -> IndexingScreen(indexingService)
                2 -> SettingsScreen(settings) { settings.save() }
            }
        }
    }
}
