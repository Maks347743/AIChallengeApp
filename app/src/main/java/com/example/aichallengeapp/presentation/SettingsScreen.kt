package com.example.aichallengeapp.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.example.aichallengeapp.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
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
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dimensionResource(R.dimen.settings_content_padding_horizontal),
                    vertical = dimensionResource(R.dimen.settings_content_padding_vertical)
                )
        ) {
            Text(
                text = stringResource(R.string.label_system_prompt),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_label_spacing)))
            OutlinedTextField(
                value = settings.systemPrompt,
                onValueChange = { viewModel.onIntent(HomeIntent.UpdateSystemPrompt(it)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_section_spacing)))

            Text(
                text = stringResource(R.string.label_max_tokens),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_label_spacing)))
            OutlinedTextField(
                value = settings.maxTokensText,
                onValueChange = { viewModel.onIntent(HomeIntent.UpdateMaxTokens(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.hint_max_tokens)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_section_spacing)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_temperature),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%.2f", settings.temperature),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_label_spacing)))
            Slider(
                value = settings.temperature,
                onValueChange = { viewModel.onIntent(HomeIntent.UpdateTemperature(it)) },
                valueRange = 0f..2f,
                steps = 19,
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
}
