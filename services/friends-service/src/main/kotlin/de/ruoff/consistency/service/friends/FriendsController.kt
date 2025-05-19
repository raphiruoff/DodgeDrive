package de.ruoff.consistency.service.friends

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import de.ruoff.consistency.service.friends.Friends.*
import de.ruoff.consistency.service.friends.stream.FriendStreamService

@GrpcService
class FriendsController(
    private val friendService: FriendService,
    private val friendStreamService: FriendStreamService
) : FriendServiceGrpc.FriendServiceImplBase() {

    override fun sendRequest(request: FriendRequest, responseObserver: StreamObserver<FriendResponse>) {
        try {
            val message = friendService.sendFriendRequest(request.fromUsername, request.toUsername)

            responseObserver.onNext(
                FriendResponse.newBuilder()
                    .setMessage(message)
                    .build()
            )
            responseObserver.onCompleted()
        } catch (e: IllegalArgumentException) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException())
        } catch (e: IllegalStateException) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException())
        } catch (e: NoSuchElementException) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.message).asRuntimeException())
        } catch (e: Exception) {
            e.printStackTrace()
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Senden der Anfrage").withCause(e).asRuntimeException())
        }
    }


    override fun acceptRequest(
        request: FriendRequest,
        responseObserver: StreamObserver<FriendResponse>
    ) {
        try {
            val success = friendService.acceptFriendRequest(request.fromUsername, request.toUsername)
            if (success) {
                val response = FriendResponse.newBuilder().setMessage("Anfrage angenommen!").build()
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } else {
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Anfrage nicht gefunden oder bereits angenommen.")
                    .asRuntimeException())
            }
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Fehler beim Akzeptieren der Anfrage")
                .withCause(e).asRuntimeException())
        }
    }

    override fun declineRequest(
        request: FriendRequest,
        responseObserver: StreamObserver<FriendResponse>
    ) {
        try {
            val success = friendService.declineFriendRequest(request.fromUsername, request.toUsername)
            if (success) {
                val response = FriendResponse.newBuilder().setMessage("Anfrage abgelehnt!").build()
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } else {
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Anfrage nicht gefunden oder bereits beantwortet.")
                    .asRuntimeException())
            }
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Fehler beim Ablehnen der Anfrage")
                .withCause(e).asRuntimeException())
        }
    }

    override fun getPendingRequests(
        request: UserIdRequest,
        responseObserver: StreamObserver<PendingRequestListResponse>
    ) {
        try {
            val requests = friendService.getPendingRequestsForUser(request.username)
            val response = PendingRequestListResponse.newBuilder()
                .addAllRequests(requests)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Fehler beim Abrufen der Anfragen")
                .withCause(e).asRuntimeException())
        }
    }

    override fun getFriends(
        request: UserIdRequest,
        responseObserver: StreamObserver<FriendListResponse>
    ) {
        try {
            val friends = friendService.getFriendsOfUser(request.username)
            val response = FriendListResponse.newBuilder()
                .addAllFriends(friends)
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Fehler beim Abrufen der Freunde")
                .withCause(e).asRuntimeException())
        }
    }

    override fun streamRequests(
        request: UserIdRequest,
        responseObserver: StreamObserver<FriendRequest>
    ) {
        // Optional: validieren, ob User existiert
        friendStreamService.register(request.username, responseObserver)
    }
}
