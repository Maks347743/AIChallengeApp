package com.example.aichallengeapp.feature.settings.presentation.appsettings

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.feature.settings.R
import com.example.aichallengeapp.feature.settings.domain.model.DeepSeekModel
import org.koin.androidx.compose.koinViewModel

private object TokenCenterMaskTransformation : VisualTransformation {
    private const val MASK_CHAR = '•'
    private const val VISIBLE_CHARS = 6

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val transformed = if (s.length > VISIBLE_CHARS * 2) {
            s.take(VISIBLE_CHARS) + MASK_CHAR.toString().repeat(s.length - VISIBLE_CHARS * 2) + s.takeLast(VISIBLE_CHARS)
        } else {
            s
        }
        return TransformedText(AnnotatedString(transformed), OffsetMapping.Identity)
    }
}

private object CloudflareUrlVisualTransformation : VisualTransformation {
    private const val MASK_CHAR = '•'
    private const val KEYWORD = "trycloudflare"

    override fun filter(text: AnnotatedString): TransformedText {
        val idx = text.text.indexOf(KEYWORD)
        val transformed = if (idx > 0) {
            MASK_CHAR.toString().repeat(idx) + text.text.substring(idx)
        } else {
            text.text
        }
        return TransformedText(AnnotatedString(transformed), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppSettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_app_settings)) },
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
                                .clickable { viewModel.onIntent(AppSettingsIntent.UpdateModel(model)) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.model == model,
                                onClick = { viewModel.onIntent(AppSettingsIntent.UpdateModel(model)) }
                            )
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    AnimatedVisibility(visible = settings.model == DeepSeekModel.OLLAMA) {
                        Column {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                            OutlinedTextField(
                                value = settings.ollamaModelName,
                                onValueChange = { viewModel.onIntent(AppSettingsIntent.UpdateOllamaModelName(it)) },
                                label = { Text(stringResource(R.string.label_ollama_model_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                    OutlinedTextField(
                        value = settings.serverBaseUrl,
                        onValueChange = { viewModel.onIntent(AppSettingsIntent.UpdateServerBaseUrl(it)) },
                        label = { Text(stringResource(R.string.label_server_base_url)) },
                        placeholder = { Text("https://abc123.trycloudflare.com", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = CloudflareUrlVisualTransformation
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.settings_title_spacing)))
                    var tokenFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = settings.mcpServerToken,
                        onValueChange = { viewModel.onIntent(AppSettingsIntent.UpdateMcpServerToken(it)) },
                        label = { Text(stringResource(R.string.label_mcp_server_token)) },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { tokenFocused = it.isFocused },
                        singleLine = true,
                        visualTransformation = if (tokenFocused) VisualTransformation.None else TokenCenterMaskTransformation
                    )
                }
            }
        }
    }
}
