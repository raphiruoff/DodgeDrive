package de.ruoff.consistency.service.logging

import de.ruoff.consistency.service.logging.LoggingServiceGrpc.LoggingServiceImplBase
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class LogController(
    private val logService: LogService
) : LoggingServiceImplBase() {

    override fun logEvent(
        request: LogEventRequest,
        responseObserver: StreamObserver<LogEventResponse>
    ) {
        logService.saveLog(
            gameId = request.gameId,
            username = request.username,
            eventType = request.eventType,
            delayMs = request.delayMs
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
