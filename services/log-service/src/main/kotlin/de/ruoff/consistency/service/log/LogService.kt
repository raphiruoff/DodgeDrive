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

    fun saveLog(
        gameId: String,
        username: String,
        eventType: String,
        delayMs: Long,
        originTimestamp: Long?,
        score: Int? = null,
        opponentUsername: String? = null
    ) {
        val eventId = "$gameId-$eventType-$username-${originTimestamp ?: System.currentTimeMillis()}"

        if (repository.existsById(eventId)) {
            println("⚠️ Log bereits vorhanden → $eventId")
            return
        }

        val log = LogModel(
            eventId = eventId,
            gameId = gameId,
            timestamp = Instant.now(),
            username = username,
            eventType = eventType,
            delayMs = delayMs,
            originTimestamp = originTimestamp?.let { Instant.ofEpochMilli(it) },
            score = score,
            opponentUsername = opponentUsername
        )
        repository.save(log)
    }


    fun exportLogsToCsv(gameId: String) {
        val logsDir = File("/app/export")
        if (!logsDir.exists()) logsDir.mkdirs()

        val timestamp = Instant.now().atZone(java.time.ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
        val formattedTime = formatter.format(timestamp)
        val file = File(logsDir, "log_export_${gameId}_$formattedTime.csv")
        val relevantEvents = setOf(
            "game_start",
            "obstacle_spawned_latency",
            "score_update_latency",
            "opponent_update_latency",
            "score_updated",
            "opponent_updated"
        )

        val logs = repository.findByGameId(gameId)
            .filter { it.eventType in relevantEvents }
        if (logs.isEmpty()) {
            println("❗ Keine Logs für gameId=$gameId gefunden.")
            return
        }

        val csv = StringBuilder()
        csv.appendLine("Event;Spieler;Geplant;Angezeigt;Latenz")

        logs.sortedBy { it.originTimestamp }.forEach { log ->
            val geplant = log.originTimestamp?.toString() ?: ""
            val angezeigt = log.timestamp.toString()
            val latenzen = log.delayMs.toString()

            csv.appendLine(
                "${log.eventType};" +
                        "${log.username};" +
                        "$geplant;" +
                        "$angezeigt;" +
                        "$latenzen"
            )
        }

        file.writeText(csv.toString())
        println("✅ Exportierte vereinfachte Logdatei: ${file.absolutePath}")
    }





}
