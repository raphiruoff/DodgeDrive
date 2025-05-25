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

        val logs = repository.findByGameId(gameId)
        if (logs.isEmpty()) {
            println("❗ Keine Logs für gameId=$gameId gefunden.")
            return
        }

        val csv = StringBuilder()
        csv.appendLine("gameId;username;eventType;originTimestamp;delayMs;score;opponentUsername;description")

        logs.sortedBy { it.originTimestamp }.forEach { log ->
            val scoreText = log.score?.toString() ?: "null"
            val opponentText = log.opponentUsername ?: "null"

            val description = when (log.eventType) {
                "score_updated" ->
                    "${log.username} updated their score to $scoreText"
                "opponent_update" ->
                    "${log.username} saw opponent $opponentText had score $scoreText"
                "obstacle_spawned" ->
                    "${log.username} rendered an obstacle"
                "game_start" ->
                    "${log.username} started the game"
                else -> ""
            }

            csv.appendLine(
                "${log.gameId};" +
                        "${log.username};" +
                        "${log.eventType};" +
                        "${log.originTimestamp ?: ""};" +
                        "${log.delayMs};" +
                        "${log.score ?: ""};" +
                        "${log.opponentUsername ?: ""};" +
                        description
            )
        }

        file.writeText(csv.toString())
        println("✅ Exportierte vollständige Logdatei: ${file.absolutePath}")
    }




}
