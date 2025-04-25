package com.example.race.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.common.TokenUtils
import com.example.race.data.network.ProfileClient
import com.example.race.data.network.TokenHolder

@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit
) {
    val username = remember { TokenUtils.decodeUsername(TokenHolder.jwtToken) }
    val profileClient = remember { ProfileClient() }
    var profile by remember { mutableStateOf<Profile.ProfileResponse?>(null) }

    LaunchedEffect(username) {
        username?.let {
            profile = profileClient.loadProfile(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "👥 Deine Freunde",
            style = MaterialTheme.typography.headlineMedium
        )

        profile?.let {
            Text(
                text = "👤 Profil: ${it.displayName}",
                style = MaterialTheme.typography.titleMedium
            )
            if (it.bio.isNotBlank()) {
                Text(
                    text = "📄 Bio: ${it.bio}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = { /* TODO: Freunde verwalten */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Freunde anzeigen")
        }

        Button(onClick = { /* TODO: Anfragen verwalten */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Freundschaftsanfragen")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onNavigateBack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⬅️ Zurück zum Hauptmenü")
        }
    }
}
