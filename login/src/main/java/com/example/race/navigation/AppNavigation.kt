package com.example.race.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.race.ui.friends.FriendsScreen
import com.example.race.ui.login.LoginScreen
import com.example.race.ui.racegame.RaceGameScreen
import com.example.race.ui.main.MainScreen
import com.example.race.ui.session.SessionScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val RACEGAME = "racegame"
    const val FRIENDS = "friends"
    const val SESSION = "session"
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(onNavigateToSession = {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToRaceGame = { navController.navigate(Routes.RACEGAME) },
                onNavigateToCreateSession = { navController.navigate(Routes.SESSION) },
                onManageFriends = { navController.navigate(Routes.FRIENDS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RACEGAME) {
            RaceGameScreen(navController)
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(onNavigateBack = {
                navController.popBackStack(Routes.MAIN, inclusive = false)
            })
        }

        composable(Routes.SESSION) {
            SessionScreen(
                onNavigateToRaceGame = { navController.navigate(Routes.RACEGAME) },
                onNavigateBack = { navController.popBackStack(Routes.MAIN, inclusive = false) }
            )
        }
    }
}
