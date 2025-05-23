package com.example.race.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.common.TokenUtils
import com.example.race.data.network.FriendClient
import com.example.race.data.network.ProfileClient
import com.example.race.data.network.TokenHolder
import de.ruoff.consistency.service.friends.Friends.FriendRequest
import de.ruoff.consistency.service.profile.Profile
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    val profileClient = remember { ProfileClient() }
    val friendClient = remember { FriendClient() }
    val coroutineScope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<Profile.ProfileResponse?>(null) }
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<String>>(emptyList()) }
    var newFriendUsername by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf("") }
    val streamRegistered = remember { mutableStateOf(false) }

    fun refreshFriendData() {
        coroutineScope.launch {
            username?.let {
                friends = withContext(Dispatchers.IO) { friendClient.getFriends(it) }
                pendingRequests = withContext(Dispatchers.IO) { friendClient.getPendingRequests(it) }
            }
        }
    }

    LaunchedEffect(username) {
        username?.let {
            try {
                profile = profileClient.loadProfile(it)
                refreshFriendData()

                if (!streamRegistered.value) {
                    streamRegistered.value = true
                    friendClient.streamRequests(it, object : StreamObserver<FriendRequest> {
                        override fun onNext(value: FriendRequest) {
                            coroutineScope.launch {
                                refreshFriendData()
                                infoMessage = when {
                                    pendingRequests.contains(value.fromUsername) -> "📨 Neue Anfrage von ${value.fromUsername}"
                                    friends.contains(value.fromUsername) -> "🤝 ${value.fromUsername} hat deine Anfrage angenommen!"
                                    else -> "🔔 Neues Ereignis von ${value.fromUsername}"
                                }
                            }
                        }

                        override fun onError(t: Throwable) {}
                        override fun onCompleted() {}
                    })
                }
            } catch (e: Exception) {
                infoMessage = " Fehler beim Laden: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👥 Deine Freunde",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        profile?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👤 Profil: ${it.username}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Freunde", style = MaterialTheme.typography.titleMedium)
                if (friends.isEmpty()) {
                    Text("Noch keine Freunde hinzugefügt.")
                } else {
                    friends.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (pendingRequests.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📨 Freundschaftsanfragen", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    pendingRequests.forEach { requester ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(requester)
                            Row {
                                Button(onClick = {
                                    username?.let { currentUser ->
                                        infoMessage = friendClient.acceptRequest(requester, currentUser)
                                        refreshFriendData()
                                    }
                                }) {
                                    Text("✔️")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    username?.let { currentUser ->
                                        infoMessage = friendClient.declineRequest(requester, currentUser)
                                        refreshFriendData()
                                    }
                                }) {
                                    Text("❌")
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = newFriendUsername,
            onValueChange = { newFriendUsername = it },
            label = { Text("👤 Username hinzufügen") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                username?.let {
                    try {
                        infoMessage = friendClient.sendFriendRequest(it, newFriendUsername)
                        newFriendUsername = ""
                    } catch (e: Exception) {
                        infoMessage = " Fehler beim Senden: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Anfrage senden")
        }

        if (infoMessage.isNotBlank()) {
            Text(
                text = infoMessage,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
