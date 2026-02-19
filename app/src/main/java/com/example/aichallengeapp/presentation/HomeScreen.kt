package com.example.aichallengeapp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.R
import com.example.aichallengeapp.domain.model.ChatMessage
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_chat)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.onIntent(HomeIntent.ClearChat) },
                        enabled = state.messages.isNotEmpty() && !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.cd_new_chat)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            val listState = rememberLazyListState()

            LaunchedEffect(state.messages.size, state.isLoading) {
                if (state.messages.isNotEmpty()) {
                    listState.animateScrollToItem(state.messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.chat_horizontal_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.chat_vertical_spacing)),
                contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.chat_vertical_spacing))
            ) {
                items(state.messages) { message ->
                    ChatBubble(message = message)
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.chat_horizontal_padding)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_size))
                            )
                        }
                    }
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.error_padding_horizontal),
                        vertical = dimensionResource(R.dimen.error_padding_vertical)
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.chat_horizontal_padding),
                        vertical = dimensionResource(R.dimen.chat_horizontal_padding)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { viewModel.onIntent(HomeIntent.UpdateInput(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.hint_message_input)) },
                    maxLines = 4
                )
                IconButton(
                    onClick = { viewModel.onIntent(HomeIntent.SendMessage) },
                    enabled = state.inputText.isNotBlank() && !state.isLoading
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.ROLE_USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cornerLarge = dimensionResource(R.dimen.chat_bubble_corner_large)
    val cornerSmall = dimensionResource(R.dimen.chat_bubble_corner_small)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = dimensionResource(R.dimen.chat_bubble_max_width))
                .clip(
                    RoundedCornerShape(
                        topStart = cornerLarge,
                        topEnd = cornerLarge,
                        bottomStart = if (isUser) cornerLarge else cornerSmall,
                        bottomEnd = if (isUser) cornerSmall else cornerLarge
                    )
                )
                .background(bubbleColor)
                .padding(dimensionResource(R.dimen.chat_bubble_content_padding))
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
