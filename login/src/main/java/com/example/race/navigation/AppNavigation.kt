package com.example.race.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.race.ui.login.LoginScreen
import com.example.race.ui.session.SessionScreen
import com.example.race.ui.racegame.RaceGameScreen

object Routes {
    const val LOGIN = "login"
    const val SESSION = "session"
    const val RACEGAME = "racegame"
}


@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(onNavigateToSession = {
                navController.navigate(Routes.SESSION)
            })
        }
        composable(Routes.SESSION) {
            SessionScreen(onNavigateToRaceGame = {
                navController.navigate(Routes.RACEGAME)
            })
        }

        composable("racegame") {
            RaceGameScreen()
        }
    }
}
