package com.example.gostreetball.ui.screens

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.gostreetball.R
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.GameInvite
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.data.model.InviteStatus
import com.example.gostreetball.data.model.User
import com.example.gostreetball.ui.HomeViewModel
import com.example.gostreetball.ui.theme.GoStreetBallTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigateToGameSetup: (String, GameType) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentCourt()
        viewModel.observeLatestInvite()
    }

    val state by viewModel.uiState.collectAsState()

    val court: Court? = state.court
    val invite: GameInvite? = state.invite
    val inviterUser: User? = state.inviterUser

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 16.dp)
        )

        if (court == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsBasketball,
                    contentDescription = "Basketball",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter some court to play games.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
        else {
            val tabs = listOf("Games", "Invites")

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .background(Color.Transparent),
                containerColor = Color.Transparent
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        modifier = Modifier
                            .background(Color.Transparent)
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        text = "Choose game to start as judge:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GameButton(GameType.ONE_VS_ONE) { navigateToGameSetup(court.id, it) }
                            GameButton(GameType.THREE_X_THREE) { navigateToGameSetup(court.id, it) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GameButton(GameType.SEVEN_UP) { navigateToGameSetup(court.id, it) }
                            GameButton(GameType.AROUND_THE_WORLD) {
                                navigateToGameSetup(
                                    court.id,
                                    it
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(onClick = { viewModel.leaveCourt() }) {
                        Text("Leave Court")
                    }
                }
                1 -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (invite == null || inviterUser == null) {
                        Text(
                            text = "No invites",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "You are invited to play by",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            UserRow(inviterUser)
                            Spacer(modifier = Modifier.height(16.dp))
                            val gameImageRes = when (invite.gameType) {
                                GameType.ONE_VS_ONE -> R.drawable.one_vs_one
                                GameType.THREE_X_THREE -> R.drawable.three_x_three
                                GameType.SEVEN_UP -> R.drawable.seven_up
                                GameType.AROUND_THE_WORLD -> R.drawable.around_the_world
                            }
                            Image(
                                painter = painterResource(gameImageRes),
                                contentDescription = invite.gameType.name,
                                modifier = Modifier.size(200.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(onClick = { viewModel.respondToInvite(invite.id, InviteStatus.ACCEPTED) }) {
                                    Text("Accept")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.respondToInvite(invite.id, InviteStatus.REJECTED) },
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRow(
    user: User,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 30.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (user.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default user icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = modifier.width(10.dp))
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Rating",
                tint = Color.Yellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "%.1f".format(user.rating),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun GameButton(type: GameType, onClick: (GameType) -> Unit) {
    val imageRes = when (type) {
        GameType.ONE_VS_ONE -> R.drawable.one_vs_one
        GameType.THREE_X_THREE -> R.drawable.three_x_three
        GameType.SEVEN_UP -> R.drawable.seven_up
        GameType.AROUND_THE_WORLD -> R.drawable.around_the_world
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = { onClick(type) },
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f))
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = type.name,
                modifier = Modifier.size(100.dp)
            )
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
fun HomeScreenPreview() {
    GoStreetBallTheme {
        HomeScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            navigateToGameSetup = { _, _ -> }
        )
    }
}