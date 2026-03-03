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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.aichallengeapp.feature.chat.ChatRoute
import com.example.aichallengeapp.feature.chat.presentation.ChatScreen
import com.example.aichallengeapp.feature.chatlist.ChatListRoute
import com.example.aichallengeapp.feature.chatlist.presentation.ChatListScreen
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
    val backStack = remember { mutableStateListOf<Any>(ChatListRoute) }

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
