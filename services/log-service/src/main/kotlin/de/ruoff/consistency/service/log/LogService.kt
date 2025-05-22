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
            println("❌ Keine Logs für gameId=$gameId gefunden.")
            return
        }

        val sortedLogs = logs.sortedBy { it.timestamp }

        // ➕ Debug-Ausgabe: wie viele Events pro Typ
        println("📊 Event-Verteilung für gameId=$gameId:")
        sortedLogs.groupBy { it.eventType }.forEach { (type, list) ->
            println("  - $type: ${list.size}")
        }

        val csv = StringBuilder()
        csv.appendLine("gameId;timestamp;username;eventType;delayMs;originTimestamp")

        // ➕ Schreibe ALLE Events raus – auch mit originTimestamp = null
        sortedLogs.forEach { log ->
            val originTsString = log.originTimestamp?.toString() ?: "NULL"
            csv.appendLine(
                "${log.gameId};" +
                        "${log.timestamp};" +
                        "${log.username};" +
                        "${log.eventType};" +
                        "${log.delayMs};" +
                        originTsString
            )
        }

        // ➕ Delay-Metriken nur berechnen, wenn möglich – sonst nur Hinweis
        fun appendDelayMetric(eventType: String, metricName: String) {
            val relevantLogs = sortedLogs.filter {
                it.eventType == eventType && it.originTimestamp != null
            }

            if (relevantLogs.size >= 2) {
                val timestamps = relevantLogs.mapNotNull { it.originTimestamp?.toEpochMilli() }
                if (timestamps.size >= 2) {
                    val minTs = timestamps.minOrNull()!!
                    val maxTs = timestamps.maxOrNull()!!
                    val delay = maxTs - minTs
                    val latestTimestamp = Instant.ofEpochMilli(maxTs)
                    csv.appendLine("${gameId};${latestTimestamp};SYSTEM;$metricName;$delay;")
                } else {
                    println("⚠ Nur ${timestamps.size} Event(s) für '$eventType' mit gültigem originTimestamp → keine $metricName-Metrik berechnet.")
                }
            } else {
                println("⚠ Nur ${relevantLogs.size} Event(s) für '$eventType' → keine $metricName-Metrik berechnet.")
            }
        }


        appendDelayMetric("game_start", "METRIC_game_start_delay")
        appendDelayMetric("obstacle_spawned", "METRIC_obstacle_spawn_delay")
        appendDelayMetric("opponent_score_visible", "METRIC_opponent_score_visible_delay")

        file.writeText(csv.toString())
        println("✅ Logs für Spiel $gameId exportiert: ${file.absolutePath}")
    }







}
