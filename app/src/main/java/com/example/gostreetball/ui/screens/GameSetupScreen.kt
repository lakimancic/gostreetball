package com.example.gostreetball.ui.screens

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.ui.GameSetupViewModel
import com.example.gostreetball.ui.screens.game_setup_steps.GameInvitesStep
import com.example.gostreetball.ui.screens.game_setup_steps.GameSettingsStep
import com.example.gostreetball.ui.theme.GoStreetBallTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GameSetupScreen(
    modifier: Modifier = Modifier,
    gameType: GameType,
    courtId: String,
    viewModel: GameSetupViewModel = hiltViewModel(),
    navigateToGame: (GameType, String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(1) }

    val canStart by viewModel.canStartGame.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setGameType(gameType)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToGame.collect {
            if (state.gameId.isNotBlank())
                navigateToGame(gameType, state.gameId)
        }
    }

    LaunchedEffect(courtId, state.gameId) {
        viewModel.observeActivePlayers(courtId)
        if (state.gameId.isNotBlank()) {
            viewModel.observeInvites(state.gameId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { step / 2f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                1 -> GameSettingsStep(gameType = gameType, state = state, viewModel = viewModel)
                2 -> GameInvitesStep(gameType = gameType, state = state, viewModel = viewModel)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (step == 1) {
                    viewModel.createGame()
                    step++
                }
                else {
                    viewModel.startGame()
                }
            },
            enabled = step == 1 || canStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.navigationBars.asPaddingValues())
        ) {
            Text(if (step == 1) "Create Game" else "Start Game")
        }
    }
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Dark Preview"
)
@Composable
private fun GameSetupPreview() {
    GoStreetBallTheme {
        GameSetupScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            gameType = GameType.THREE_X_THREE,
            courtId = "",
            navigateToGame = { _, _ -> }
        )
    }
}