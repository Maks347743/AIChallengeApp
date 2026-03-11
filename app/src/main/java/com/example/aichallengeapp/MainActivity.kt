package com.example.aichallengeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.aichallengeapp.feature.chat.ChatRoute
import com.example.aichallengeapp.feature.chat.presentation.ChatScreen
import com.example.aichallengeapp.feature.chatlist.ChatListRoute
import com.example.aichallengeapp.feature.chatlist.presentation.ChatListScreen
import com.example.aichallengeapp.feature.explore.presentation.ExploreGitHubScreen
import com.example.aichallengeapp.feature.settings.SettingsRoute
import com.example.aichallengeapp.feature.settings.presentation.SettingsScreen
import com.example.aichallengeapp.feature.userpreferences.UserPreferencesRoute
import com.example.aichallengeapp.feature.userpreferences.UserProfileEditRoute
import com.example.aichallengeapp.feature.userpreferences.presentation.profileedit.UserProfileEditScreen
import com.example.aichallengeapp.feature.userpreferences.presentation.profilelist.UserProfileListScreen
import com.example.aichallengeapp.ui.theme.AIChallengeAppTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIChallengeAppTheme {
                AppNavigation(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val chatBackStack = remember { mutableStateListOf<Any>(ChatListRoute) }
    val showBottomBar = selectedTab == 1 || chatBackStack.size == 1

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
                        label = { Text("Chats") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ChatsTab(
                backStack = chatBackStack,
                modifier = modifier.padding(innerPadding)
            )
            1 -> ExploreGitHubScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ChatsTab(
    backStack: MutableList<Any>,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            slideInHorizontally(tween(350, easing = FastOutSlowInEasing)) { it } togetherWith
            slideOutHorizontally(tween(350, easing = FastOutSlowInEasing)) { -it / 3 }
        },
        popTransitionSpec = {
            slideInHorizontally(tween(350, easing = FastOutSlowInEasing)) { -it / 3 } togetherWith
            slideOutHorizontally(tween(350, easing = FastOutSlowInEasing)) { it }
        },
        entryProvider = entryProvider {
            entry<ChatListRoute> {
                ChatListScreen(
                    onNavigateToChat = { chatId, branchIndex, profileId ->
                        backStack.add(ChatRoute(chatId, branchIndex, profileId))
                    },
                    onNavigateToUserPreferences = { backStack.add(UserPreferencesRoute) },
                    modifier = modifier
                )
            }
            entry<UserPreferencesRoute> {
                UserProfileListScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToEditProfile = { profileId -> backStack.add(UserProfileEditRoute(profileId)) },
                    modifier = modifier
                )
            }
            entry<UserProfileEditRoute> { entry ->
                UserProfileEditScreen(
                    profileId = entry.profileId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = modifier
                )
            }
            entry<ChatRoute> { entry ->
                ChatScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToSettings = { activeChatId -> backStack.add(SettingsRoute(activeChatId)) },
                    modifier = modifier,
                    viewModel = koinViewModel(key = entry.chatId) {
                        parametersOf(entry.chatId, entry.branchIndex, entry.profileId)
                    }
                )
            }
            entry<SettingsRoute> { entry ->
                SettingsScreen(
                    chatId = entry.chatId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = modifier
                )
            }
        }
    )
}
