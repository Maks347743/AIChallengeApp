package com.example.aichallengeapp.feature.settings.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.feature.settings.R
import com.example.aichallengeapp.feature.settings.domain.model.DeepSeekModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TEMPERATURE_FORMAT = "%.2f"
private val TEMPERATURE_RANGE = 0f..2f
private const val TEMPERATURE_STEPS = 19
private const val SYSTEM_PROMPT_MIN_LINES = 4
private const val SYSTEM_PROMPT_MAX_LINES = 8

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(key = chatId) { parametersOf(chatId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dimensionResource(R.dimen.settings_content_padding_horizontal),
                    vertical = dimensionResource(R.dimen.settings_content_padding_vertical)
                )
        ) {
            // Model section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Text(
                        text = stringResource(R.string.label_model),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                    DeepSeekModel.entries.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onIntent(SettingsIntent.UpdateModel(model)) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.model == model,
                                onClick = { viewModel.onIntent(SettingsIntent.UpdateModel(model)) }
                            )
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_card_spacing)))

            // System Prompt section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Text(
                        text = stringResource(R.string.label_system_prompt),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                    OutlinedTextField(
                        value = settings.systemPrompt,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateSystemPrompt(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = SYSTEM_PROMPT_MIN_LINES,
                        maxLines = SYSTEM_PROMPT_MAX_LINES
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_card_spacing)))

            // Max Tokens section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Text(
                        text = stringResource(R.string.label_max_tokens),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                    OutlinedTextField(
                        value = state.maxTokensText,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateMaxTokens(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.hint_max_tokens)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_card_spacing)))

            // Temperature section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_temperature),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format(TEMPERATURE_FORMAT, settings.temperature),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                    Slider(
                        value = settings.temperature,
                        onValueChange = { viewModel.onIntent(SettingsIntent.UpdateTemperature(it)) },
                        valueRange = TEMPERATURE_RANGE,
                        steps = TEMPERATURE_STEPS,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.temperature_min),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.temperature_max),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_card_spacing)))

            // Summary Mode section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_summary_mode),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.summaryEnabled,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.ToggleSummary(it)) }
                        )
                    }
                    AnimatedVisibility(visible = settings.summaryEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                            OutlinedTextField(
                                value = state.maxRecentMessagesText,
                                onValueChange = { viewModel.onIntent(SettingsIntent.UpdateSummaryRecentMessages(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_summary_recent_messages)) },
                                isError = state.maxRecentMessagesText.toIntOrNull()?.let { it > 0 } != true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                            OutlinedTextField(
                                value = state.summaryMaxTokensText,
                                onValueChange = { viewModel.onIntent(SettingsIntent.UpdateSummaryMaxTokens(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_summary_max_tokens)) },
                                isError = state.summaryMaxTokensText.toIntOrNull()?.let { it > 0 } != true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_card_spacing)))

            // Sliding Window Mode section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_sliding_window_mode),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.slidingWindowEnabled,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.ToggleSlidingWindow(it)) }
                        )
                    }
                    AnimatedVisibility(visible = settings.slidingWindowEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                            OutlinedTextField(
                                value = state.slidingWindowSizeText,
                                onValueChange = { viewModel.onIntent(SettingsIntent.UpdateSlidingWindowSize(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_sliding_window_size)) },
                                isError = state.slidingWindowSizeText.toIntOrNull()?.let { it > 0 } != true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_card_spacing)))

            // Sticky Facts Mode section
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.settings_card_content_padding))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_sticky_facts_mode),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.stickyFactsEnabled,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.ToggleStickyFacts(it)) }
                        )
                    }
                    AnimatedVisibility(visible = settings.stickyFactsEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                            OutlinedTextField(
                                value = state.stickyFactsRecentMessagesText,
                                onValueChange = { viewModel.onIntent(SettingsIntent.UpdateStickyFactsRecentMessages(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_sticky_facts_recent_messages)) },
                                isError = state.stickyFactsRecentMessagesText.toIntOrNull()?.let { it > 0 } != true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}
