package com.example.aichallengeapp.feature.chat.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.feature.chat.R
import com.example.aichallengeapp.feature.chat.ui.theme.SendBlue
import com.example.aichallengeapp.feature.chat.ui.theme.SendBlueDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SCROLL_BUTTON_HIDE_DELAY_MS = 2000L

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
    val sheetState = rememberModalBottomSheetState()

    if (state.showMetrics && state.chatMetrics != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(ChatIntent.ToggleMetrics) },
            sheetState = sheetState
        ) {
            MetricsBottomSheetContent(metrics = state.chatMetrics!!)
        }
    }

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
                        enabled = state.chatMetrics != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.cd_toggle_metrics),
                            tint = if (state.showMetrics) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.chatMetrics != null) 1f else 0.38f)
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
            val scope = rememberCoroutineScope()

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

            // Scroll jump button state — use plain object to avoid recomposition on job change
            var topButtonVisible by remember { mutableStateOf(false) }
            var bottomButtonVisible by remember { mutableStateOf(false) }
            // When true, snapshotFlow direction events are ignored (programmatic scroll in progress)
            var suppressScrollDetection by remember { mutableStateOf(false) }
            val scrollJobs = remember {
                object {
                    var topHide: Job? = null
                    var bottomHide: Job? = null
                }
            }

            fun hideScrollButtons() {
                topButtonVisible = false
                bottomButtonVisible = false
                scrollJobs.topHide?.cancel()
                scrollJobs.bottomHide?.cancel()
            }

            LaunchedEffect(state.messages.size, state.isLoading) {
                if (state.messages.isNotEmpty()) {
                    suppressScrollDetection = true
                    listState.scrollToItem(
                        index = state.messages.size - 1,
                        scrollOffset = Int.MAX_VALUE / 2
                    )
                    delay(100)
                    suppressScrollDetection = false
                }
            }

            LaunchedEffect(listState) {
                var prevIndex = listState.firstVisibleItemIndex
                var prevOffset = listState.firstVisibleItemScrollOffset
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }.collect { (index, offset) ->
                    if (!suppressScrollDetection) {
                        val goingUp = index < prevIndex || (index == prevIndex && offset < prevOffset)
                        val goingDown = index > prevIndex || (index == prevIndex && offset > prevOffset)
                        when {
                            goingUp -> {
                                topButtonVisible = true
                                bottomButtonVisible = false
                                scrollJobs.topHide?.cancel()
                                scrollJobs.topHide = scope.launch {
                                    delay(SCROLL_BUTTON_HIDE_DELAY_MS)
                                    topButtonVisible = false
                                }
                            }
                            goingDown -> {
                                bottomButtonVisible = true
                                topButtonVisible = false
                                scrollJobs.bottomHide?.cancel()
                                scrollJobs.bottomHide = scope.launch {
                                    delay(SCROLL_BUTTON_HIDE_DELAY_MS)
                                    bottomButtonVisible = false
                                }
                            }
                        }
                    }
                    prevIndex = index
                    prevOffset = offset
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
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

                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    ScrollJumpButton(
                        visible = topButtonVisible,
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        onClick = {
                            hideScrollButtons()
                            suppressScrollDetection = true
                            scope.launch {
                                listState.scrollToItem(0)
                                delay(100)
                                suppressScrollDetection = false
                            }
                        }
                    )
                }

                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    ScrollJumpButton(
                        visible = bottomButtonVisible,
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        onClick = {
                            hideScrollButtons()
                            suppressScrollDetection = true
                            scope.launch {
                                listState.scrollToItem(
                                    index = listState.layoutInfo.totalItemsCount - 1,
                                    scrollOffset = Int.MAX_VALUE / 2
                                )
                                delay(100)
                                suppressScrollDetection = false
                            }
                        }
                    )
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

// Private composable with no scope receiver so AnimatedVisibility resolves to the standalone overload,
// avoiding the ColumnScope.AnimatedVisibility conflict when called from inside a Box inside a Column.
@Composable
private fun ScrollJumpButton(
    visible: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + scaleIn(tween(150)),
        exit = fadeOut(tween(150)) + scaleOut(tween(150))
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun MetricsBottomSheetContent(metrics: ChatMetrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Token Usage",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        MetricRow(label = "Last request (prompt)", value = "${metrics.lastRequestTokens} tokens")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        MetricRow(label = "Last response (completion)", value = "${metrics.lastResponseTokens} tokens")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        MetricRow(
            label = "Total for this chat",
            value = "${metrics.totalTokens} tokens",
            bold = true
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
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
