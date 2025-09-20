package com.example.gostreetball.ui.screens.game_setup_steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.ui.GameSetupUiState
import com.example.gostreetball.ui.GameSetupViewModel

@Composable
fun GameSettingsStep(
    modifier: Modifier = Modifier,
    gameType: GameType,
    state: GameSetupUiState,
    viewModel: GameSetupViewModel
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Game Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (gameType == GameType.ONE_VS_ONE || gameType == GameType.THREE_X_THREE) {
            Text(
                text="Select target points:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(11, 16, 21).forEach { points ->
                    Button(
                        onClick = { viewModel.setTargetPoints(points) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.gameSetup.targetPoints == points)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(points.toString())
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Win by 2", color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = state.gameSetup.winByTwo,
                    onCheckedChange = { viewModel.toggleWinByTwo() }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Make it take it", color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = state.gameSetup.makeItTakeIt,
                    onCheckedChange = { viewModel.toggleMakeItTakeIt() }
                )
            }
        }

        if (gameType == GameType.AROUND_THE_WORLD) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Back if air ball", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = state.gameSetup.missCount,
                    onCheckedChange = { viewModel.toggleMissCount() }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Long route", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = state.gameSetup.longRoute,
                    onCheckedChange = { viewModel.toggleLongRoute() }
                )
            }
        }

        if (gameType == GameType.SEVEN_UP) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Air ball counts", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = state.gameSetup.missCount,
                    onCheckedChange = { }
                )
            }
        }
    }
}