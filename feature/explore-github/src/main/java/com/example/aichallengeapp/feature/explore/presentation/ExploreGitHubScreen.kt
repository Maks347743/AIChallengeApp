package com.example.aichallengeapp.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.core.mcp.model.McpTool
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExploreGitHubScreen(
    modifier: Modifier = Modifier,
    viewModel: ExploreGitHubViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExploreGitHubContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@Composable
private fun ExploreGitHubContent(
    uiState: ExploreGitHubUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        when (uiState) {
            is ExploreGitHubUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ExploreGitHubUiState.Empty -> {
                Text(
                    text = "No tools available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ExploreGitHubUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
            is ExploreGitHubUiState.Success -> {
                ToolsList(tools = uiState.tools)
            }
        }
    }
}

@Composable
private fun ToolsList(
    tools: List<McpTool>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "MCP Tools (${tools.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        itemsIndexed(tools) { index, tool ->
            ToolItem(index = index + 1, tool = tool)
        }
    }
}

@Composable
private fun ToolItem(
    index: Int,
    tool: McpTool,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$index. ${tool.name}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        tool.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreGitHubLoadingPreview() {
    ExploreGitHubContent(
        uiState = ExploreGitHubUiState.Loading,
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ExploreGitHubSuccessPreview() {
    ExploreGitHubContent(
        uiState = ExploreGitHubUiState.Success(
            tools = listOf(
                McpTool(name = "code_search", description = "Search for code across repositories"),
                McpTool(name = "get_file", description = "Get the contents of a file"),
                McpTool(name = "list_repos", description = null)
            )
        ),
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ExploreGitHubErrorPreview() {
    ExploreGitHubContent(
        uiState = ExploreGitHubUiState.Error(message = "Authentication failed. Check your GitHub Key."),
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ExploreGitHubEmptyPreview() {
    ExploreGitHubContent(
        uiState = ExploreGitHubUiState.Empty,
        onRetry = {}
    )
}
