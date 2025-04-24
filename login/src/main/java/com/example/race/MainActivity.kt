package com.example.race

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.race.navigation.AppNavigation
import com.example.race.ui.theme.ConsistencyserviceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsistencyserviceTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}


