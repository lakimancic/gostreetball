package com.example.gostreetball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gostreetball.data.local.AppPreferences
import com.example.gostreetball.data.local.ThemeEnum
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.ui.screens.AddCourtScreen
import com.example.gostreetball.ui.screens.AddReviewScreen
import com.example.gostreetball.ui.screens.GameSetupScreen
import com.example.gostreetball.ui.screens.CourtScreen
import com.example.gostreetball.ui.screens.FilterScreen
import com.example.gostreetball.ui.screens.MainScreen
import com.example.gostreetball.ui.screens.ReviewsScreen
import com.example.gostreetball.ui.screens.UserScreen
import com.example.gostreetball.ui.screens.auth.LoginScreen
import com.example.gostreetball.ui.screens.auth.RegistrationScreen
import com.example.gostreetball.ui.screens.auth.WelcomeScreen
import com.example.gostreetball.ui.theme.GoStreetBallTheme
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val selectedTheme by appPreferences.selectedTheme.collectAsState(initial = ThemeEnum.LIGHT)

            GoStreetBallTheme(
                darkTheme = selectedTheme == ThemeEnum.DARK
            ) {
                GoStreetBallApp(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Composable
fun GoStreetBallApp(
    modifier: Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screens.WelcomeScreen.name) {
        composable(Screens.WelcomeScreen.name) {
            WelcomeScreen(
                modifier = modifier,
                navigateToLogin = { navController.navigate(Screens.LoginScreen.name) },
                navigateToRegister = { navController.navigate(Screens.RegisterScreen.name) }
            )
        }
        composable(Screens.LoginScreen.name) {
            LoginScreen(
                modifier = modifier,
                navigateBack = { navController.popBackStack(Screens.WelcomeScreen.name, false) },
                navigateToRegister = {
                    navController.navigate(Screens.RegisterScreen.name) {
                        popUpTo(Screens.WelcomeScreen.name) {
                            inclusive = false
                        }
                    }
                },
                navigateToMain = {
                    navController.navigate(Screens.MainScreen.name) {
                        popUpTo(Screens.WelcomeScreen.name) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screens.RegisterScreen.name) {
            RegistrationScreen(
                modifier = modifier,
                navigateBack = { navController.popBackStack(Screens.WelcomeScreen.name, false) },
                navigateToLogin = {
                    navController.navigate(Screens.LoginScreen.name) {
                        popUpTo(Screens.WelcomeScreen.name) {
                            inclusive = false
                        }
                    }
                }
            )
        }
        composable(Screens.MainScreen.name) {
            MainScreen(
                modifier = modifier,
                navHostController = navController
            )
        }
        composable(Screens.AddCourtScreen.name) {
            AddCourtScreen(
                modifier = modifier,
                navigateBack = { navController.popBackStack(Screens.MainScreen.name, false) },
            )
        }
        composable(Screens.FilterScreen.name) {
            FilterScreen(
                modifier = modifier,
                navController = navController
            )
        }
        composable(
            route = "${Screens.CourtScreen}/{courtId}",
            arguments = listOf(navArgument("courtId") { type = NavType.StringType })
        ) { backStackEntry ->
            val courtId = backStackEntry.arguments?.getString("courtId") ?: ""
            CourtScreen(
                courtId = courtId,
                navigateBack = { navController.popBackStack() },
                navigateToReview = { navController.navigate("${Screens.AddReviewScreen.name}/$courtId/${true}") },
                navigateToReviews = { navController.navigate("${Screens.ReviewsScreen.name}/$courtId/${true}") }
            )
        }
        composable(
            route = "${Screens.UserScreen}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserScreen(
                userId = userId,
                navigateBack = { navController.popBackStack() },
                navigateToReview = { navController.navigate("${Screens.AddReviewScreen.name}/$userId/${false}")},
                navigateToReviews = { navController.navigate("${Screens.ReviewsScreen.name}/$userId/${false}") }
            )
        }
        composable(
            route = "${Screens.AddReviewScreen.name}/{itemId}/{isForCourt}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("isForCourt") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val isForCourt = backStackEntry.arguments?.getBoolean("isForCourt") ?: true

            AddReviewScreen(
                itemId = itemId,
                isForCourt = isForCourt,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Screens.ReviewsScreen.name}/{itemId}/{isForCourt}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("isForCourt") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val isForCourt = backStackEntry.arguments?.getBoolean("isForCourt") ?: true

            ReviewsScreen(
                itemId = itemId,
                isForCourt = isForCourt,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Screens.GameSetupScreen.name}/{courtId}/{gameType}",
            arguments = listOf(
                navArgument("courtId") { type = NavType.StringType },
                navArgument("gameType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courtId = backStackEntry.arguments?.getString("courtId") ?: ""
            val gameTypeStr = backStackEntry.arguments?.getString("gameType") ?: GameType.ONE_VS_ONE.name
            val gameType = GameType.valueOf(gameTypeStr)

            GameSetupScreen(
                courtId = courtId,
                gameType = gameType
            )
        }
    }
}

enum class Screens {
    WelcomeScreen,
    LoginScreen,
    RegisterScreen,
    MainScreen,
    AddCourtScreen,
    FilterScreen,
    CourtScreen,
    UserScreen,
    AddReviewScreen,
    ReviewsScreen,
    GameSetupScreen
}
