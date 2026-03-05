package com.example.aichallengeapp.feature.chat.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
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
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.feature.chat.R
import com.example.aichallengeapp.feature.chat.ui.theme.SendBlue
import com.example.aichallengeapp.feature.chat.ui.theme.SendBlueDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCROLL_BUTTON_HIDE_DELAY_MS = 2000L
private const val CLEAR_ANIM_DURATION_MS = 250
private const val CLEAR_COMPLETE_DELAY_MS = 280L
private const val SCROLL_SUPPRESS_DELAY_MS = 100L
private const val JUMP_BUTTON_ANIM_DURATION_MS = 150
private const val SCROLL_ITEM_END_OFFSET = Int.MAX_VALUE / 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (activeChatId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var clearAnimating by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    BackHandler { viewModel.onNavigatingBack(onNavigateBack) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_chat_title)) },
            text = { Text(stringResource(R.string.dialog_clear_chat_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmDialog = false
                    clearAnimating = true
                }) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_no))
                }
            }
        )
    }

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
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.title_chat),
                            style = MaterialTheme.typography.titleLarge
                        )
                        state.currentProfileName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { viewModel.onNavigatingBack(onNavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onIntent(ChatIntent.CreateCheckpoint) },
                        enabled = state.messages.isNotEmpty() && !state.isLoading && state.branches.size < 2
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = stringResource(R.string.cd_create_checkpoint)
                        )
                    }
                    IconButton(onClick = { onNavigateToSettings(state.activeChatId) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
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
                        onClick = { showClearConfirmDialog = true },
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

            if (state.currentTask != null) {
                TaskStageIndicator(currentStage = state.currentTaskStage)
            }

            if (state.branches.size > 1) {
                BranchSwitcherRow(
                    branches = state.branches,
                    activeBranchIndex = state.activeBranchIndex,
                    onBranchSelected = { branchId -> viewModel.onIntent(ChatIntent.SwitchBranch(branchId)) }
                )
            }

            val clearAlpha = remember { Animatable(1f) }
            val clearScale = remember { Animatable(1f) }

            LaunchedEffect(clearAnimating) {
                if (clearAnimating) {
                    launch { clearAlpha.animateTo(0f, tween(CLEAR_ANIM_DURATION_MS)) }
                    launch { clearScale.animateTo(0.95f, tween(CLEAR_ANIM_DURATION_MS)) }
                    delay(CLEAR_COMPLETE_DELAY_MS)
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
                        scrollOffset = SCROLL_ITEM_END_OFFSET
                    )
                    delay(SCROLL_SUPPRESS_DELAY_MS)
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
                        },
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
                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_size))
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopEnd).padding(dimensionResource(R.dimen.scroll_jump_button_padding))) {
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

                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(dimensionResource(R.dimen.scroll_jump_button_padding))) {
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
                                    scrollOffset = SCROLL_ITEM_END_OFFSET
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
        enter = fadeIn(tween(JUMP_BUTTON_ANIM_DURATION_MS)) + scaleIn(tween(JUMP_BUTTON_ANIM_DURATION_MS)),
        exit = fadeOut(tween(JUMP_BUTTON_ANIM_DURATION_MS)) + scaleOut(tween(JUMP_BUTTON_ANIM_DURATION_MS))
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
            .padding(
                horizontal = dimensionResource(R.dimen.metrics_sheet_padding_horizontal),
                vertical = dimensionResource(R.dimen.metrics_sheet_padding_vertical)
            )
    ) {
        Text(
            text = "Token Usage",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.metrics_title_bottom_padding))
        )
        MetricRow(label = "Last request (prompt)", value = "${metrics.lastRequestTokens} tokens")
        HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.metrics_divider_padding_vertical)))
        MetricRow(label = "Last response (completion)", value = "${metrics.lastResponseTokens} tokens")
        HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.metrics_divider_padding_vertical)))
        MetricRow(
            label = "Total for this chat",
            value = "${metrics.totalTokens} tokens",
            bold = true
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.metrics_spacer_height)))
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
private fun BranchSwitcherRow(
    branches: List<BranchInfo>,
    activeBranchIndex: Int,
    onBranchSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(R.dimen.branch_switcher_padding_horizontal),
                vertical = dimensionResource(R.dimen.branch_switcher_padding_vertical)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.branch_chip_spacing)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        branches.forEach { branch ->
            FilterChip(
                selected = branch.branchIndex == activeBranchIndex,
                onClick = { if (branch.branchIndex != activeBranchIndex) onBranchSelected(branch.sessionId) },
                label = { Text("Branch ${branch.branchIndex}") }
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    if (message.role == ChatMessage.ROLE_SUMMARY) {
        Card(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.chat_bubble_content_padding))
            ) {
                Text(
                    text = stringResource(R.string.label_summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.chat_bubble_label_spacing)))
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    if (message.role == ChatMessage.ROLE_FACTS) {
        Card(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.chat_bubble_content_padding))
            ) {
                Text(
                    text = stringResource(R.string.label_facts),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.chat_bubble_label_spacing)))
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    val isUser = message.role == ChatMessage.ROLE_USER
    val cornerLarge = dimensionResource(R.dimen.chat_bubble_corner_large)
    val cornerSmall = dimensionResource(R.dimen.chat_bubble_corner_small)
    val shape = RoundedCornerShape(
        topStart = cornerLarge,
        topEnd = cornerLarge,
        bottomStart = if (isUser) cornerLarge else cornerSmall,
        bottomEnd = if (isUser) cornerSmall else cornerLarge
    )

    if (isUser) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
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
            modifier = modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
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

@Composable
private fun TaskStageIndicator(
    currentStage: TaskStage,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        TaskStage.PLANNING to "Планирование",
        TaskStage.EXECUTION to "Выполнение",
        TaskStage.EVALUATION to "Оценка",
        TaskStage.DONE to "Выполнено"
    )
    val currentIndex = stages.indexOfFirst { it.first == currentStage }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stages.forEachIndexed { index, (_, label) ->
                val isActive = index == currentIndex
                val isDone = index < currentIndex

                val textColor: Color = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isDone -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(textColor)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (index < stages.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        color = if (isDone) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}
