package de.ruoff.consistency.service.logging

import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class LogService(
    private val repository: LogRepository
) {

    fun saveLog(gameId: String, username: String, eventType: String, delayMs: Long) {
        val log = LogModel(
            gameId = gameId,
            timestamp = Instant.now(),
            username = username,
            eventType = eventType,
            delayMs = delayMs
        )
        repository.save(log)
    }

    fun exportLogsToCsv(gameId: String) {
        val logsDir = File("${System.getProperty("user.dir")}/export")
        if (!logsDir.exists()) logsDir.mkdirs()

        val timestamp = Instant.now().atZone(java.time.ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
        val formattedTime = formatter.format(timestamp)
        val file = File(logsDir, "log_export_${gameId}_$formattedTime.csv")

        val logs = repository.findAll().filter { it.gameId == gameId }
        if (logs.isEmpty()) {
            println("⚠️ Keine Logs für gameId=$gameId gefunden.")
            return
        }
        val csv = StringBuilder()
        csv.appendLine("gameId;timestamp;username;eventType;delayMs")

        logs.forEach { log ->
            csv.appendLine("${log.gameId};${log.timestamp};${log.username};${log.eventType};${log.delayMs}")
        }

        file.writeText(csv.toString())
        println("✅ Logs for game $gameId exported to ${file.absolutePath}")
    }

}
