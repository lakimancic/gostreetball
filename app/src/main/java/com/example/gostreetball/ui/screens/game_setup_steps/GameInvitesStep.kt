package com.example.gostreetball.ui.screens.game_setup_steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.data.model.InviteStatus
import com.example.gostreetball.data.model.User
import com.example.gostreetball.ui.GameSetupUiState
import com.example.gostreetball.ui.GameSetupViewModel
import com.example.gostreetball.ui.PlayerInviteStatus

@Composable
fun GameInvitesStep(
    gameType: GameType,
    state: GameSetupUiState,
    viewModel: GameSetupViewModel
) {
    val inviteStatuses by viewModel.playerInviteStatuses.collectAsState()

    val canInviteA by viewModel.canInviteA.collectAsState()
    val canInviteB by viewModel.canInviteB.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (inviteStatuses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No players yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(inviteStatuses) { status ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (status.user.profileImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = status.user.profileImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default user",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = status.user.username,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    when (gameType) {
                        GameType.ONE_VS_ONE, GameType.THREE_X_THREE -> {
                            InviteColumn(
                                label = "A",
                                status = status.statusA,
                                otherStatus = status.statusB, // check exclusivity
                                canInvite = canInviteA,
                                onInvite = { viewModel.sendInvite(status.user, true) },
                                onCancel = { viewModel.cancelInvite(status.user.uid) }
                            )
                            Spacer(Modifier.width(8.dp))
                            InviteColumn(
                                label = "B",
                                status = status.statusB,
                                otherStatus = status.statusA, // check exclusivity
                                canInvite = canInviteB,
                                onInvite = { viewModel.sendInvite(status.user, false) },
                                onCancel = { viewModel.cancelInvite(status.user.uid) }
                            )
                        }

                        GameType.SEVEN_UP, GameType.AROUND_THE_WORLD -> {
                            InviteColumn(
                                label = "",
                                status = status.statusA,
                                otherStatus = null, // single column
                                onInvite = { viewModel.sendInvite(status.user, true) },
                                onCancel = { viewModel.cancelInvite(status.user.uid) },
                                canInvite = canInviteA,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteColumn(
    label: String,
    status: InviteStatus,
    otherStatus: InviteStatus?,
    canInvite: Boolean,
    onInvite: () -> Unit,
    onCancel: () -> Unit
) {
    when (status) {
        InviteStatus.ACCEPTED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Accepted",
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$label Accepted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Green
                )
            }
        }

        InviteStatus.PENDING -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cancel invite",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$label Pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        InviteStatus.REJECTED -> {
            if (canInvite && (otherStatus == null || otherStatus == InviteStatus.REJECTED)) {
                Button(
                    onClick = onInvite,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (label.isNotBlank()) "Invite to $label" else "Invite")
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }
    }
}
