package com.example.race.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.race.ui.friends.FriendsScreen
import com.example.race.ui.login.LoginScreen
import com.example.race.ui.main.MainScreen
import com.example.race.ui.racegame.RaceGameScreen
import com.example.race.ui.session.SessionScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val RACEGAME = "racegame/{gameId}/{username}"
    const val FRIENDS = "friends"
    const val SESSION = "session"

    fun raceGameWithArgs(gameId: String, username: String) = "racegame/$gameId/$username"
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
                onNavigateToRaceGame = { gameId, username ->
                    navController.navigate(Routes.raceGameWithArgs(gameId, username))
                },
                onNavigateToCreateSession = { navController.navigate(Routes.SESSION) },
                onManageFriends = { navController.navigate(Routes.FRIENDS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RACEGAME,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val username = backStackEntry.arguments?.getString("username") ?: ""
            RaceGameScreen(navController, gameId, username)
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(onNavigateBack = {
                navController.popBackStack(Routes.MAIN, inclusive = false)
            })
        }

        composable(Routes.SESSION) {
            SessionScreen(
                onNavigateToRaceGame = { gameId, username ->
                    navController.navigate(Routes.raceGameWithArgs(gameId, username))
                },
                onNavigateBack = { navController.popBackStack(Routes.MAIN, inclusive = false) }
            )
        }
    }
}
