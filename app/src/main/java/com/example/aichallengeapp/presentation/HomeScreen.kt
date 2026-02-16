package com.example.aichallengeapp.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aichallengeapp.R
import com.example.aichallengeapp.ui.theme.StatusError
import com.example.aichallengeapp.ui.theme.StatusSuccess
import org.koin.androidx.compose.koinViewModel

private const val CROSSFADE_DURATION_MS = 400
private val ICON_SIZE = 48.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreenContent(
        modifier = modifier,
        state = state,
        onCheckConnection = { viewModel.onIntent(HomeIntent.CheckConnection) }
    )
}

@Composable
private fun HomeScreenContent(
    modifier: Modifier = Modifier,
    state: HomeState,
    onCheckConnection: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusIndicator(
            status = state.status,
            response = state.response,
            error = state.error,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val gradient = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            )
        )
        val disabledGradient = Brush.horizontalGradient(
            listOf(Color.Gray, Color.Gray)
        )

        Button(
            onClick = onCheckConnection,
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = if (!state.isLoading) gradient else disabledGradient,
                        shape = ButtonDefaults.shape
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.check_connection),
                    color = if (!state.isLoading) MaterialTheme.colorScheme.onPrimary else Color.White
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    status: ConnectionStatus,
    response: String?,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Response/error text — no animation
        when (status) {
            ConnectionStatus.Success -> if (response != null) {
                Text(
                    text = response,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            ConnectionStatus.Error -> if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        // Icon: Crossfade spinner → result icon, result stays visible
        if (status != ConnectionStatus.Idle) {
            Crossfade(
                targetState = status,
                animationSpec = tween(CROSSFADE_DURATION_MS),
                label = "IconCrossfade"
            ) { targetStatus ->
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (targetStatus) {
                        ConnectionStatus.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(ICON_SIZE),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        ConnectionStatus.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(ICON_SIZE),
                                tint = StatusSuccess
                            )
                        }
                        ConnectionStatus.Error -> {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(ICON_SIZE),
                                tint = StatusError
                            )
                        }
                        ConnectionStatus.Idle -> {}
                    }
                }
            }
        }
    }
}
