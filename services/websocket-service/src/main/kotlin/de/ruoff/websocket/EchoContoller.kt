package de.ruoff.websocket

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller

@Controller
class EchoController {

    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    fun echo(message: String): String {
        println(" Echo empfangen: $message")
        return "Echo: $message"
    }
}
