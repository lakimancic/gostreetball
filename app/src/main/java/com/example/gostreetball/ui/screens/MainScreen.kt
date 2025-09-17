package com.example.gostreetball.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gostreetball.Screens
import com.example.gostreetball.location.LocationService
import com.example.gostreetball.ui.CourtsViewModel
import com.example.gostreetball.ui.MapViewModel
import com.example.gostreetball.ui.ProfileViewModel
import com.example.gostreetball.ui.theme.GoStreetBallTheme

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navHostController: NavHostController?
) {
    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(2) }

    val mainBackStackEntry = navHostController!!
        .currentBackStackEntryAsState()
        .value
        ?.let { navHostController.getBackStackEntry(Screens.MainScreen.name) }

    val courtsViewModel: CourtsViewModel? = mainBackStackEntry?.let { hiltViewModel(it) }
    if (courtsViewModel == null) return;

    val mapViewModel: MapViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val isTrackingOn by profileViewModel.isTrackingOn.collectAsState()

    val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val tabs = listOf(
        Triple("Map", Icons.Default.LocationOn, "map"),
        Triple("Courts", Icons.AutoMirrored.Filled.List, "courts"),
        Triple("Home", Icons.Default.Home, "home"),
        Triple("Scores", Icons.Default.Star, "scores"),
        Triple("Profile", Icons.Default.Person, "profile")
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }

        if (locationGranted) {
            mapViewModel.onLocationPermissionsGranted()
        }

        if (notificationGranted) {
            val intent = Intent(context, LocationService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    LaunchedEffect(isTrackingOn) {
        val locationGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val notificationsGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (isTrackingOn) {
            if (!locationGranted || !notificationsGranted) {
                permissionLauncher.launch(requiredPermissions)
            } else if (!LocationService.isRunning) {
                val intent = Intent(context, LocationService::class.java)
                ContextCompat.startForegroundService(context, intent)
            }
        } else {
            val intent = Intent(context, LocationService::class.java)
            context.stopService(intent)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                tabs.forEachIndexed { index, (title, icon, _) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = index == selectedTabIndex,
                        onClick = {
                            selectedTabIndex = index
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> MapScreen(
                modifier = modifier.padding(innerPadding),
                navigateToAdd = { navHostController.navigate(Screens.AddCourtScreen.name) },
                navigateToFilter = { navHostController.navigate(Screens.FilterScreen.name) },
                navigateToCourt = { navHostController.navigate("${Screens.CourtScreen}/$it")},
                courtsViewModel = courtsViewModel
            )
            1 -> CourtsScreen(
                modifier = modifier.padding(innerPadding),
                navigateToFilter = { navHostController.navigate(Screens.FilterScreen.name) },
                navigateToCourt = { navHostController.navigate("${Screens.CourtScreen}/$it")},
                viewModel = courtsViewModel
            )
            2 -> HomeScreen()
            3 -> ScoresScreen(
                modifier = modifier.padding(innerPadding),
                navigateToUser = { navHostController.navigate("${Screens.UserScreen}/$it")}
            )
            4 -> ProfileScreen(
                modifier = modifier.padding(innerPadding),
                navigateToWelcome = { navHostController.popBackStack(Screens.WelcomeScreen.name, false) },
                navigateToUser = { navHostController.navigate("${Screens.UserScreen}/$it")}
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
fun MainScreenPreview() {
    GoStreetBallTheme {
        MainScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            navHostController = null
        )
    }
}