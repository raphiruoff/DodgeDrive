package com.example.race.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.common.TokenUtils
import com.example.race.data.network.FriendListClient
import com.example.race.data.network.ProfileClient
import com.example.race.data.network.TokenHolder

@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    val profileClient = remember { ProfileClient() }
    val friendClient = remember { FriendListClient() }

    var profile by remember { mutableStateOf<Profile.ProfileResponse?>(null) }
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<String>>(emptyList()) }
    var newFriendUsername by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }

    LaunchedEffect(username) {
        username?.let {
            try {
                profile = profileClient.loadProfile(it)
                friends = friendClient.getFriends(it)
                pendingRequests = friendClient.getPendingRequests(it)
            } catch (e: Exception) {
                infoMessage = "Fehler beim Laden der Daten: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👥 Deine Freunde", style = MaterialTheme.typography.headlineMedium)

        profile?.let {
            Text("👤 Profil: ${it.displayName}", style = MaterialTheme.typography.titleMedium)
            if (it.bio.isNotBlank()) {
                Text("📄 Bio: ${it.bio}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Divider()

        Text("✅ Freunde:", style = MaterialTheme.typography.titleMedium)
        if (friends.isEmpty()) {
            Text("Noch keine Freunde hinzugefügt.", style = MaterialTheme.typography.bodyMedium)
        } else {
            friends.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("📨 Freundschaftsanfragen:", style = MaterialTheme.typography.titleMedium)
        if (pendingRequests.isEmpty()) {
            Text("Keine ausstehenden Anfragen.", style = MaterialTheme.typography.bodyMedium)
        } else {
            pendingRequests.forEach { requester ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(requester)
                    Button(onClick = {
                        username?.let { currentUser ->
                            infoMessage = friendClient.acceptRequest(requester, currentUser)
                            pendingRequests = friendClient.getPendingRequests(currentUser)
                            friends = friendClient.getFriends(currentUser)
                        }
                    }) {
                        Text("Annehmen")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = newFriendUsername,
            onValueChange = { newFriendUsername = it },
            label = { Text("Username hinzufügen") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                username?.let {
                    try {
                        infoMessage = friendClient.sendFriendRequest(it, newFriendUsername)
                        newFriendUsername = ""
                    } catch (e: Exception) {
                        infoMessage = "Fehler beim Senden der Anfrage: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Anfrage senden")
        }

        if (infoMessage.isNotBlank()) {
            Text(text = infoMessage, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Zurück zum Hauptmenü")
        }
    }
}
