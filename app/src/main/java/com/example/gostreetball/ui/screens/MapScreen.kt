package com.example.gostreetball.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gostreetball.ui.CourtsViewModel
import com.example.gostreetball.ui.MapViewModel
import com.example.gostreetball.utils.createCourtMarkerBitmap
import com.example.gostreetball.utils.createCourtMarkerIcon
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = hiltViewModel(),
    courtsViewModel: CourtsViewModel = hiltViewModel(),
    navigateToAdd: () -> Unit,
    navigateToFilter: () -> Unit,
    targetLocation: LatLng? = null
) {
    val context = LocalContext.current
    val currentLocation by mapViewModel.currentLocation.collectAsState()
    val isLocationAvailable by mapViewModel.isLocationAvailable.collectAsState()
    val locationPermissionGranted by mapViewModel.locationPermissionGranted.collectAsState()
    val courtsState by courtsViewModel.uiState.collectAsState()
    val markerIcon = remember { createCourtMarkerIcon(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            mapViewModel.onLocationPermissionsGranted()
        }
    }

    val initialLocation = targetLocation ?: currentLocation ?: LatLng(
        44.787197,
        20.457273
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    LaunchedEffect(targetLocation) {
        targetLocation?.let { location ->
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(location, 16f)
        }
    }

    LaunchedEffect(Unit) {
        courtsViewModel.fetchCourts()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface (
            modifier = Modifier
                .fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = courtsState.searchQuery,
                    onValueChange = { courtsViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search courts...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (courtsState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { courtsViewModel.setSearchQuery("") }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { }
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted && isLocationAvailable,
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true
                )
            ) {
                courtsState.courts.forEach { court ->
                    court.location?.let { geo ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(geo.latitude, geo.longitude)
                            ),
                            title = court.name,
                            icon = markerIcon,
                            snippet = "Type: ${court.type}, ${court.boardType} Board"
                        )
                    }
                }
            }

            if (locationPermissionGranted) {
                FloatingActionButton(
                    onClick = { navigateToAdd() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Court",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!locationPermissionGranted) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Location is required",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To add court, you need to allow location permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val permissions = mapViewModel.requestLocationPermissions()
                                permissionLauncher.launch(permissions)
                            }
                        ) {
                            Text(
                                text = "Activate Location",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}