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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.aichallengeapp.feature.chat.ChatRoute
import com.example.aichallengeapp.feature.chat.presentation.ChatScreen
import com.example.aichallengeapp.feature.chatlist.ChatListRoute
import com.example.aichallengeapp.feature.chatlist.presentation.ChatListScreen
import com.example.aichallengeapp.feature.settings.SettingsRoute
import com.example.aichallengeapp.feature.settings.presentation.SettingsScreen
import com.example.aichallengeapp.ui.theme.AIChallengeAppTheme

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
                    onNavigateToChat = { chatId -> backStack.add(ChatRoute(chatId)) },
                    modifier = modifier
                )
            }
            entry<ChatRoute> { entry ->
                ChatScreen(
                    chatId = entry.chatId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToSettings = { backStack.add(SettingsRoute(entry.chatId)) },
                    modifier = modifier
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
