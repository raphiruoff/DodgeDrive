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
        val origin = if (request.originTimestamp > 0) request.originTimestamp else System.currentTimeMillis()

        val kafkaEvent = GameLogEvent(
            gameId = request.gameId,
            username = request.username,
            eventType = request.eventType,
            originTimestamp = origin,
            isWinner = false
        )
        println("📤 Sende an Kafka: $kafkaEvent")

        // Sende an Kafka
        kafkaTemplate.send("game-log-topic", kafkaEvent)

        // Optional: zusätzlich lokal speichern (MongoDB)
        logService.saveLog(
            gameId = request.gameId,
            username = request.username,
            eventType = request.eventType,
            delayMs = request.delayMs,
            originTimestamp = origin
        )


        val response = LogEventResponse.newBuilder()
            .setSuccess(true)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
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
