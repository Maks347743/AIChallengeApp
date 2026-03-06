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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.model.UserProfile
import com.example.aichallengeapp.feature.chatlist.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (chatId: String, branchIndex: Int, profileId: String) -> Unit,
    onNavigateToUserPreferences: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = koinViewModel()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var showProfileSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState
        ) {
            ProfileSelectionSheetContent(
                profiles = profiles,
                onProfileSelected = { profile ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showProfileSheet = false
                        onNavigateToChat(viewModel.newSessionId(), 0, profile.id)
                    }
                }
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_chat_list)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToUserPreferences) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_global_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (profiles.size) {
                        1 -> onNavigateToChat(viewModel.newSessionId(), 0, profiles[0].id)
                        else -> showProfileSheet = true
                    }
                }
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
            val profileNames = remember(profiles) { profiles.associateBy({ it.id }, { it.name }) }
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
                        profileName = session.profileId?.let { profileNames[it] },
                        onClick = { onNavigateToChat(session.id, session.branchIndex, session.profileId ?: "") },
                        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.chat_list_item_spacing))
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSelectionSheetContent(
    profiles: List<UserProfile>,
    onProfileSelected: (UserProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.select_profile_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (profiles.isEmpty()) {
            Text(
                text = stringResource(R.string.no_profiles),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.no_profiles_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            LazyColumn {
                items(profiles, key = { it.id }) { profile ->
                    ProfileSheetItem(
                        profile = profile,
                        onClick = { onProfileSelected(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSheetItem(
    profile: UserProfile,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Column {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge
            )
            if (profile.description.isNotBlank()) {
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatSessionCard(
    session: ChatSession,
    profileName: String?,
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
            if (profileName != null || session.branchIndex > 0) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        dimensionResource(R.dimen.chat_list_branch_chip_spacing)
                    ),
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.chat_list_branch_chip_spacing))
                ) {
                    if (profileName != null) {
                        Text(
                            text = profileName,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                )
                                .padding(
                                    horizontal = dimensionResource(R.dimen.chat_list_branch_chip_padding_horizontal),
                                    vertical = dimensionResource(R.dimen.chat_list_branch_chip_padding_vertical)
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (session.branchIndex > 0) {
                        Text(
                            text = stringResource(R.string.label_branch, session.branchIndex),
                            modifier = Modifier
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
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatSessionCardPreview() {
    MaterialTheme {
        ChatSessionCard(
            session = ChatSession(
                id = "1",
                messages = listOf(
                    ChatMessage(role = ChatMessage.ROLE_USER, content = "Привет, расскажи мне о Kotlin")
                )
            ),
            profileName = "Developer",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSelectionSheetContentPreview() {
    MaterialTheme {
        ProfileSelectionSheetContent(
            profiles = listOf(
                UserProfile(id = "1", name = "Developer", description = "Kotlin developer profile", createdAt = 0L),
                UserProfile(id = "2", name = "Writer", description = "Creative writing assistant", createdAt = 0L)
            ),
            onProfileSelected = {}
        )
    }
}
