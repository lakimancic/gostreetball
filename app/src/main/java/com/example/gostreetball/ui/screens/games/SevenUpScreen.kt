package com.example.gostreetball.ui.screens.games

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.data.model.User
import com.example.gostreetball.ui.games.SevenUpViewModel
import com.example.gostreetball.ui.theme.GoStreetBallTheme

@Composable
fun SevenUpScreen(
    modifier: Modifier = Modifier,
    gameId: String = "",
    viewModel: SevenUpViewModel = hiltViewModel(),
    onBackHome: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val playerOnTurn: Pair<User, Int> =
        (state.players.getOrNull(state.playerWithBall) ?: User()) to (state.scores.getOrNull(state.playerWithBall) ?: 0)
    val accumulated = state.accumulated
    val playersOnCourt by viewModel.playersOnCourt.collectAsState()
    val playerIndexOnCourt by viewModel.playerIndexOnCourt.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadGameWithPlayers(gameId)
    }

    val winner = state.winner
    if (winner != null) {
        WinnerScreen(
            modifier = modifier,
            winners = listOf(winner),
            gameType = GameType.SEVEN_UP,
            onBackToHome = onBackHome
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(32.dp),
    ) {
        Text(
            text = "Seven Up Game",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.4f
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Turn:",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (playerOnTurn.first.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = playerOnTurn.first.profileImageUrl,
                        contentDescription = "Player image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default user icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Text(
                    text = playerOnTurn.first.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Points: ${playerOnTurn.second}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        BasketballHalfCourt(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            players = playersOnCourt,
            playerWithBall = playerIndexOnCourt
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Accumulated points: $accumulated",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { viewModel.hitShot() }, modifier = Modifier.weight(1f)) {
                Text("Hit")
            }
            Button(onClick = { viewModel.missShot() }, modifier = Modifier.weight(1f)) {
                Text("Miss")
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Preview")
@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Dark Preview"
)
@Composable
fun SevenUpScreenPreview() {
    GoStreetBallTheme {
        SevenUpScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
        )
    }
}