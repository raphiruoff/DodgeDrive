package de.ruoff.consistency.service.log

import de.ruoff.consistency.events.GameLogEvent
import de.ruoff.consistency.service.logging.ExportRequest
import de.ruoff.consistency.service.logging.ExportResponse
import de.ruoff.consistency.service.logging.LogEventRequest
import de.ruoff.consistency.service.logging.LogEventResponse
import de.ruoff.consistency.service.logging.LoggingServiceGrpc.LoggingServiceImplBase
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import org.springframework.kafka.core.KafkaTemplate
import java.time.Instant

@GrpcService
class LogController(
    private val logService: LogService,
    private val kafkaTemplate: KafkaTemplate<String, GameLogEvent>
) : LoggingServiceImplBase() {

    override fun logEvent(
        request: LogEventRequest,
        responseObserver: StreamObserver<LogEventResponse>
    ) {
        try {
            val origin = if (request.originTimestamp > 0) request.originTimestamp else System.currentTimeMillis()

            val score = if (request.score != 0) request.score else null
            val opponentUsername = if (request.opponentUsername.isNotBlank()) request.opponentUsername else null

            val kafkaEvent = GameLogEvent(
                gameId = request.gameId,
                username = request.username,
                eventType = request.eventType,
                originTimestamp = origin,
                isWinner = false,
                score = score,
                opponentUsername = opponentUsername
            )

            println("📤 Sende an Kafka: $kafkaEvent")
            kafkaTemplate.send("game-log-topic", kafkaEvent)

            logService.saveLog(
                gameId = request.gameId,
                username = request.username,
                eventType = request.eventType,
                delayMs = request.delayMs,
                originTimestamp = origin,
                score = score,
                opponentUsername = opponentUsername
            )

            val response = LogEventResponse.newBuilder()
                .setSuccess(true)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

        } catch (e: Exception) {
            println("❌ Fehler beim Verarbeiten von logEvent:")
            e.printStackTrace()

            val errorResponse = LogEventResponse.newBuilder()
                .setSuccess(false)
                .build()

            responseObserver.onNext(errorResponse)
            responseObserver.onCompleted()
        }
    }

    override fun exportLogs(
        request: ExportRequest,
        responseObserver: StreamObserver<ExportResponse>
    ) {
        logService.exportLogsToCsv(request.gameId)
        val response = ExportResponse.newBuilder().setSuccess(true).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}

