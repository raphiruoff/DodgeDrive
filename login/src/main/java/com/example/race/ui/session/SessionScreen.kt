package com.example.race.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.common.TokenUtils
import com.example.race.data.network.FriendListClient
import com.example.race.data.network.TokenHolder
import kotlinx.coroutines.launch

@Composable
fun SessionScreen(
    onNavigateToRaceGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    val friendClient = remember { FriendListClient() }
    val coroutineScope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var invitedFriends by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf("") }

    LaunchedEffect(username) {
        username?.let {
            try {
                friends = friendClient.getFriends(it)
            } catch (e: Exception) {
                infoMessage = "Fehler beim Laden der Freunde: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🎯 Session erstellen", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(friends) { friend ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(friend)

                        if (invitedFriends.contains(friend)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Button(onClick = {
                                coroutineScope.launch {
                                    invitedFriends = invitedFriends + friend
                                    isLoading = true
                                    // TODO: Später echte gRPC-Einladung senden
                                    kotlinx.coroutines.delay(3000) // Simulation
                                    isLoading = false
                                }
                            }) {
                                Text("Einladen")
                            }
                        }
                    }
                }
            }
        }

        if (infoMessage.isNotBlank()) {
            Text(infoMessage)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToRaceGame,
            modifier = Modifier.fillMaxWidth(),
            enabled = invitedFriends.isNotEmpty()
        ) {
            Text("🚗 Spiel starten")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Zurück")
        }
    }
}
