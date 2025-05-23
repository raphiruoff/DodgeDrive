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
        originTimestamp: Long?
    ) {
        val log = LogModel(
            gameId = gameId,
            timestamp = Instant.now(),
            username = username,
            eventType = eventType,
            delayMs = delayMs,
            originTimestamp = originTimestamp?.let { Instant.ofEpochMilli(it) }
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

        val logs = repository.findByGameId(gameId)
        if (logs.isEmpty()) {
            println("Keine Logs für gameId=$gameId gefunden.")
            return
        }

        val sorted = logs.sortedBy { it.timestamp }
        val csv = StringBuilder()
        csv.appendLine("gameId;username;eventType;ms")

        val base = sorted.find { it.eventType == "game_created" }?.originTimestamp?.toEpochMilli()
        if (base == null) {
            println("Kein game_created Event vorhanden")
            return
        }

        csv.appendLine("$gameId;${sorted.first().username};game_created;0")

        val includedEvents = listOf("game_start", "obstacle_spawned", "opponent_update", "score_update", "opponent_score_visible")
        includedEvents.forEach { event ->
            sorted.filter { it.eventType == event && it.originTimestamp != null }
                .groupBy { it.username }
                .forEach { (user, entries) ->
                    val first = entries.minByOrNull { it.originTimestamp!! }!!
                    val diff = first.originTimestamp!!.toEpochMilli() - base
                    csv.appendLine("${first.gameId};$user;$event;$diff")
                }
        }



        file.writeText(csv.toString())
        println("📝 Exportierte Logdatei: ${file.absolutePath}")
    }
}
