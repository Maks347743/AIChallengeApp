package com.example.aichallengeapp.feature.chatlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.feature.chatlist.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (chatId: String, branchIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = koinViewModel()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_chat_list)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToChat(viewModel.newSessionId(), 0) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_new_chat)
                )
            }
        }
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_chat_list),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = dimensionResource(R.dimen.chat_list_padding_horizontal),
                        vertical = dimensionResource(R.dimen.chat_list_padding_vertical)
                    )
            ) {
                items(sessions, key = { it.id }) { session ->
                    ChatSessionCard(
                        session = session,
                        onClick = { onNavigateToChat(session.id, session.branchIndex) },
                        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.chat_list_item_spacing))
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSessionCard(
    session: ChatSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.chat_list_card_padding_horizontal),
                vertical = dimensionResource(R.dimen.chat_list_card_padding_vertical)
            )
        ) {
            Text(
                text = session.previewText(),
                style = MaterialTheme.typography.bodyMedium
            )
            if (session.branchIndex > 0) {
                Text(
                    text = stringResource(R.string.label_branch, session.branchIndex),
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.chat_list_branch_chip_spacing))
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(
                            horizontal = dimensionResource(R.dimen.chat_list_branch_chip_padding_horizontal),
                            vertical = dimensionResource(R.dimen.chat_list_branch_chip_padding_vertical)
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
