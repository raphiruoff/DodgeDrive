package com.example.race.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.race.common.TokenUtils
import com.example.race.data.network.TokenHolder
import de.ruoff.consistency.service.leaderboard.LeaderboardEntry
import com.example.race.data.network.LeaderboardClient

@Composable
fun MainScreen(
    onNavigateToRaceGame: (String, String) -> Unit,
    onNavigateToCreateSession: () -> Unit,
    onManageFriends: () -> Unit,
    onLogout: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    var leaderboard by remember { mutableStateOf(emptyList<LeaderboardEntry>()) }

    LaunchedEffect(Unit) {
        leaderboard = LeaderboardClient().getTopScores()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏁 Willkommen, $username!",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Was möchtest du tun?",
            fontSize = 18.sp
        )

        Button(
            onClick = onManageFriends,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("👥 Freunde verwalten")
        }

        Button(
            onClick = onNavigateToCreateSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🎯 Neue Session starten")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 Bestenliste",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (leaderboard.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        leaderboard.forEachIndexed { index, entry ->
                            val isCurrentUser = entry.username == username
                            Text(
                                text = "${index + 1}. ${entry.username} — ${entry.highscore} Punkte",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (username != null) {
                    onNavigateToRaceGame("dummy-game-id", username)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🚗 Spiel starten")
        }

        Button(
            onClick = {
                TokenHolder.jwtToken = null
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("🚪 Logout")
        }
    }
}
