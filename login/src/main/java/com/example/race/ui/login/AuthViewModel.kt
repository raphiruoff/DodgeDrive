package com.example.race.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.race.data.network.AuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authClient = AuthClient()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun testGrpcConnection() {
        viewModelScope.launch {
            _message.value = authClient.testGrpcConnection()
        }
    }

    fun sendPing() {
        viewModelScope.launch {
            _message.value = authClient.sendPing()
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val message = authClient.login(username, password)
                _message.value = message
                if (message.contains("erfolgreich", ignoreCase = true)) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _message.value = "Fehler beim Login: ${e.message}"
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            try {
                val message = authClient.register(username, password)
                _message.value = message
            } catch (e: Exception) {
                _message.value = "Fehler bei Registrierung: ${e.message}"
            }
        }
    }
}
