package com.example.gostreetball.ui.screens

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.gostreetball.data.local.ThemeEnum
import com.example.gostreetball.ui.ProfileViewModel
import com.example.gostreetball.ui.theme.GoStreetBallTheme
import com.example.gostreetball.utils.rememberCameraCapture
import com.example.gostreetball.utils.rememberImagePicker
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
    navigateToWelcome: () -> Unit,
    navigateToUser: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isTracking by viewModel.isTrackingOn.collectAsState()
    val checkRadius by viewModel.checkRadius.collectAsState()
    val selectedTheme by viewModel.theme.collectAsState()

    val scrollState = rememberScrollState()

    val galleryLauncher = rememberImagePicker { bitmap ->
        viewModel.changeProfileImage(bitmap)
    }

    val cameraLauncher = rememberCameraCapture { bitmap ->
        viewModel.changeProfileImage(bitmap)
    }

    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadOwnProfileImage()
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Account Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { navigateToUser(viewModel.getCurrentUserId()) }) {
                Text("View Profile Page")
            }

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    navigateToWelcome()
                },
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text("Logout")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            if (state.image != null) {
                Image(
                    bitmap = state.image!!,
                    contentDescription = "Profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile placeholder",
                    modifier = Modifier.size(150.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text("Upload Photo")
            }
            VerticalDivider()
            OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text("Take Photo")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tracking")
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = isTracking,
                    onCheckedChange = { viewModel.toggleTracking() }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Theme")
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = selectedTheme == ThemeEnum.DARK,
                    onCheckedChange = { viewModel.toggleTheme() }
                )
            }
        }
        if (isTracking) {
            Spacer(Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tracking Radius: ${checkRadius}m")
                Slider(
                    value = checkRadius.toFloat(),
                    onValueChange = { viewModel.setCheckRadius(it.roundToInt()) },
                    valueRange = 100f..1000f
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text("Change Password", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.oldPassword,
                onValueChange = { viewModel.updateOldPassword(it) },
                label = { Text("Old Password") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                        Icon(
                            if (oldPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (oldPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = { viewModel.updateNewPassword(it) },
                label = { Text("New Password") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.changePassword() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Update Password")
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
fun ProfileScreenPreview() {
    GoStreetBallTheme {
        ProfileScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            navigateToWelcome = {},
            navigateToUser = {}
        )
    }
}