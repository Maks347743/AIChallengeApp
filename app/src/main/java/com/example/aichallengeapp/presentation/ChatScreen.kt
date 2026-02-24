package com.example.aichallengeapp.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.R
import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ResponseMetrics
import com.example.aichallengeapp.ui.theme.SendBlue
import com.example.aichallengeapp.ui.theme.SendBlueDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(key = chatId) { parametersOf(chatId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var clearAnimating by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_chat)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onIntent(ChatIntent.ToggleMetrics) },
                        enabled = state.lastMetrics != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.cd_toggle_metrics),
                            tint = if (state.showMetrics) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.lastMetrics != null) 1f else 0.38f)
                            }
                        )
                    }
                    IconButton(
                        onClick = { clearAnimating = true },
                        enabled = state.messages.isNotEmpty() && !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.cd_new_chat)
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

            val clearAlpha = remember { Animatable(1f) }
            val clearScale = remember { Animatable(1f) }

            LaunchedEffect(clearAnimating) {
                if (clearAnimating) {
                    launch { clearAlpha.animateTo(0f, tween(250)) }
                    launch { clearScale.animateTo(0.95f, tween(250)) }
                    delay(280L)
                    viewModel.onIntent(ChatIntent.ClearChat)
                    clearAlpha.snapTo(1f)
                    clearScale.snapTo(1f)
                    clearAnimating = false
                }
            }

            val animAlpha = clearAlpha.value
            val animScale = clearScale.value

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
                    .graphicsLayer {
                        alpha = animAlpha
                        scaleX = animScale
                        scaleY = animScale
                    }
                    .padding(horizontal = dimensionResource(R.dimen.chat_horizontal_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.chat_vertical_spacing)),
                contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.chat_vertical_spacing))
            ) {
                itemsIndexed(state.messages, key = { index, _ -> index }) { _, message ->
                    ChatBubble(message = message, modifier = Modifier.animateItem(fadeOutSpec = null, placementSpec = null))
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

            var rememberedMetrics by remember { mutableStateOf<ResponseMetrics?>(null) }
            LaunchedEffect(state.lastMetrics) {
                if (state.lastMetrics != null) rememberedMetrics = state.lastMetrics
            }

            AnimatedVisibility(
                visible = state.showMetrics,
                enter = slideInVertically(tween(250)) { it } + expandVertically(tween(250)),
                exit = slideOutVertically(tween(220)) { it } + shrinkVertically(tween(220))
            ) {
                rememberedMetrics?.let { MetricsBar(metrics = it) }
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

            Surface(tonalElevation = 2.dp) {
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
                        onValueChange = { viewModel.onIntent(ChatIntent.UpdateInput(it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.hint_message_input)) },
                        maxLines = 4
                    )
                    IconButton(
                        onClick = { viewModel.onIntent(ChatIntent.SendMessage) },
                        enabled = state.inputText.isNotBlank() && !state.isLoading,
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.chat_horizontal_padding))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            tint = (if (isSystemInDarkTheme()) SendBlueDark else SendBlue)
                                .copy(alpha = if (state.inputText.isNotBlank() && !state.isLoading) 1f else 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsBar(metrics: ResponseMetrics) {
    val timeLabel = if (metrics.responseTimeMs < 1000) {
        "${metrics.responseTimeMs} ms"
    } else {
        "${"%.1f".format(metrics.responseTimeMs / 1000.0)} s"
    }
    val costLabel = if (metrics.costUsd == 0.0) "Free" else "$${"%.6f".format(metrics.costUsd)}"

    Column {
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricItem(label = "Time", value = timeLabel)
            MetricItem(label = "Tokens", value = "${metrics.totalTokens} (${metrics.promptTokens}+${metrics.completionTokens})")
            MetricItem(label = "Cost", value = costLabel)
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatMessage.ROLE_USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val cornerLarge = dimensionResource(R.dimen.chat_bubble_corner_large)
    val cornerSmall = dimensionResource(R.dimen.chat_bubble_corner_small)
    val shape = RoundedCornerShape(
        topStart = cornerLarge,
        topEnd = cornerLarge,
        bottomStart = if (isUser) cornerLarge else cornerSmall,
        bottomEnd = if (isUser) cornerSmall else cornerLarge
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .widthIn(max = dimensionResource(R.dimen.chat_bubble_max_width))
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(dimensionResource(R.dimen.chat_bubble_content_padding))
            ) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.widthIn(max = dimensionResource(R.dimen.chat_bubble_max_width)),
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.padding(dimensionResource(R.dimen.chat_bubble_content_padding))) {
                    SelectionContainer {
                        Text(
                            text = message.content,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
