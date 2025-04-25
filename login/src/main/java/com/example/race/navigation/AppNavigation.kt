package com.example.race.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.race.ui.friends.FriendsScreen
import com.example.race.ui.login.LoginScreen
import com.example.race.ui.session.SessionScreen
import com.example.race.ui.racegame.RaceGameScreen

object Routes {
    const val LOGIN = "login"
    const val SESSION = "session"
    const val RACEGAME = "racegame"
    const val FRIENDS = "friends"
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(onNavigateToSession = {
                navController.navigate(Routes.SESSION) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.SESSION) {
            SessionScreen(
                onNavigateToRaceGame = { navController.navigate(Routes.RACEGAME) },
                onManageFriends = { navController.navigate(Routes.FRIENDS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true } // kompletter Stack-Reset
                    }
                }
            )
        }

        composable(Routes.RACEGAME) {
            RaceGameScreen(navController)
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(onNavigateBack = {
                navController.popBackStack(Routes.SESSION, inclusive = false)
            })
        }
    }
}
