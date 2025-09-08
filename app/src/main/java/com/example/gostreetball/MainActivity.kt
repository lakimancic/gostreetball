package com.example.gostreetball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gostreetball.ui.screens.AddCourtScreen
import com.example.gostreetball.ui.screens.MainScreen
import com.example.gostreetball.ui.screens.auth.LoginScreen
import com.example.gostreetball.ui.screens.auth.RegistrationScreen
import com.example.gostreetball.ui.screens.auth.WelcomeScreen
import com.example.gostreetball.ui.theme.GoStreetBallTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoStreetBallTheme {
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
    }
}

enum class Screens {
    WelcomeScreen,
    LoginScreen,
    RegisterScreen,
    MainScreen,
    AddCourtScreen
}
