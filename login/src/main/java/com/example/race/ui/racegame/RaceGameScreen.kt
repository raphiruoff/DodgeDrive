package com.example.race.ui.racegame

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.race.ui.racegame.components.Car
import com.example.race.ui.racegame.components.Controls
import com.example.race.ui.racegame.components.RaceTrack
import com.example.race.ui.racegame.control.GameController

@Composable
fun RaceGameScreen() {
    val controller = remember { GameController() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            RaceTrack(Modifier.fillMaxSize())
            Car(gameState = controller.gameState, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(16.dp))

        Controls(controller)
    }


}
