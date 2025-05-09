package de.ruoff.consistency.service.notification

import org.springframework.stereotype.Service

@Service
class NotificationService {

    fun logSomethingInternally() {
        println("🔧 NotificationService: interne Logik aufgerufen")
    }
}
