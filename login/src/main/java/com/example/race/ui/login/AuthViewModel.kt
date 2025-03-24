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
}
