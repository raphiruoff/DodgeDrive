package de.ruoff.consistency.service.log

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
        val logsDir = File("/app/export")  // Containerpfad
        if (!logsDir.exists()) logsDir.mkdirs()

        val timestamp = Instant.now().atZone(java.time.ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
        val formattedTime = formatter.format(timestamp)
        val file = File(logsDir, "log_export_${gameId}_$formattedTime.csv")

        val logs = repository.findByGameId(gameId)
        if (logs.isEmpty()) {
            println("Keine Logs für gameId=$gameId gefunden.")
            return
        }

        val sortedLogs = logs.sortedBy { it.timestamp }

        val csv = StringBuilder()
        csv.appendLine("gameId;timestamp;username;eventType;delayMs")
        sortedLogs.forEach { log ->
            csv.appendLine("${log.gameId};${log.timestamp};${log.username};${log.eventType};${log.delayMs}")
        }

        // Game Start Delay berechnen (Unterschied zwischen frühestem und spätestem game_start)
        val gameStartLogs = sortedLogs.filter { it.eventType == "game_start" }
        if (gameStartLogs.size >= 2) {
            val minTs = gameStartLogs.minOf { it.timestamp.toEpochMilli() }
            val maxTs = gameStartLogs.maxOf { it.timestamp.toEpochMilli() }
            val delay = maxTs - minTs
            val latestTimestamp = Instant.ofEpochMilli(maxTs)
            csv.appendLine("${gameId};${latestTimestamp};SYSTEM;METRIC_game_start_delay;$delay")
        } else {
            println("⚠Nur ${gameStartLogs.size} game_start-Event(s) gefunden – keine Verzögerung berechnet.")
        }

        file.writeText(csv.toString())
        println("Logs for game $gameId exported to ${file.absolutePath}")
    }




}
