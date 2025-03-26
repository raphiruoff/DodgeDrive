package com.example.race.ui.racegame.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun HoldableButton(
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressStart()
                        tryAwaitRelease()
                        onPressEnd()
                    }
                )
            }
            .padding(8.dp)
    ) {
        content()
    }
}
