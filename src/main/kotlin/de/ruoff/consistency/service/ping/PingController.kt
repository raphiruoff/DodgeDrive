package de.ruoff.consistency.service.ping

import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class PingController : PingServiceGrpc.PingServiceImplBase() {

    override fun ping(request: PingRequest, responseObserver: StreamObserver<PingResponse>) {
        val response = PingResponse.newBuilder()
            .setMessage("Pong – Server ist online")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
