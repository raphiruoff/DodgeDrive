package com.example.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val authClient = AuthClient()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun register(username: String, password: String) {
        viewModelScope.launch {
            val response = authClient.register(username, password)
            _message.value = response
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val response = authClient.login(username, password)
            _message.value = response
        }
    }
}
