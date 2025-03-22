package com.example.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel = viewModel()) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val message by authViewModel.message.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { authViewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { authViewModel.register(username, password) },
            modifier = Modifier.fillMaxWidth()) {
            Text("Registrieren")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { authViewModel.testConnection() },
            modifier = Modifier.fillMaxWidth()) {
            Text("Verbindung testen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = message)
    }
}
