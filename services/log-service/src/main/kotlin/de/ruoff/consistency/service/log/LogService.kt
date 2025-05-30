package de.ruoff.consistency.service.log

import jakarta.annotation.PostConstruct
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class LogService(
    private val repository: LogRepository,
    private val logMetricsService: LogMetricsService,
    private val retryTemplate: RetryTemplate
) {

    fun saveLogWithRetry(
        gameId: String,
        username: String,
        eventType: String,
        delayMs: Long,
        originTimestamp: Long?,
        score: Int? = null,
        opponentUsername: String? = null
    ) {
        var attempt = 0
        retryTemplate.execute<Void, Exception> {
            attempt++
            if (attempt > 1) { // Ab dem zweiten Versuch ist es ein Retry
                logMetricsService.countRetryAttempt(eventType)
            }
            saveLog(gameId, username, eventType, delayMs, originTimestamp, score, opponentUsername)
            null
        }
    }

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
            println("Log bereits vorhanden → $eventId")
            logMetricsService.countDuplicateEvent(eventType)
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

        logMetricsService.countLogEvent(eventType, username)
        logMetricsService.recordLatency(eventType, delayMs)
    }

    fun exportLogsToCsv(gameId: String) {
        val logsDir = File("/app/export")
        if (!logsDir.exists()) logsDir.mkdirs()

        val timestamp = Instant.now().atZone(java.time.ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
        val formattedTime = formatter.format(timestamp)
        val file = File(logsDir, "log_game_$formattedTime.csv")
        val relevantEvents = setOf(
            "game_start",
            "obstacle_spawned_latency",
            "score_roundtrip",
            "invitation_accepted",
            "start_grpc_duration"
        )

        val logs = repository.findByGameId(gameId)
            .filter { it.eventType in relevantEvents }

        if (logs.isEmpty()) {
            println("Keine Logs für gameId=$gameId gefunden.")
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
        println("Exportierte Logdatei: ${file.absolutePath}")
    }

    @PostConstruct
    fun runTestsOnStartup() {
        testDuplicateLogging()
        testRetryLogging()
    }

    fun testDuplicateLogging() {
        val gameId = "test-game-123"
        val username = "TestUser"
        val eventType = "latency_grpc_backend"
        val delayMs = 100L
        val originTimestamp = System.currentTimeMillis()

        println("Starte mehrfachen Log-Call (5x mit exakt gleichen Daten)")
        repeat(5) { i ->
            println("Log-Call Nr. ${i + 1}")
            saveLog(gameId, username, eventType, delayMs, originTimestamp)
        }
    }

    fun testRetryLogging() {
        val gameId = "retry-test-game"
        val username = "RetryUser"
        val eventType = "retry_event"
        val delayMs = 100L
        val originTimestamp = System.currentTimeMillis()

        println("Starte Retry-Test (Simuliert Fehler bis zum 3. Versuch)")
        var attempt = 0

        try {
            retryTemplate.execute<Void, Exception> {
                attempt++
                println("Retry Versuch $attempt")
                if (attempt < 3) {
                    throw RuntimeException("Simulierter Fehler zum Testen des Retry-Mechanismus")
                }
                saveLogWithRetry(gameId, username, eventType, delayMs, originTimestamp)
                null
            }
            println("Log erfolgreich gespeichert nach $attempt Versuchen")
        } catch (e: Exception) {
            println("Retry ist fehlgeschlagen nach $attempt Versuchen: ${e.message}")
        }
    }

}
