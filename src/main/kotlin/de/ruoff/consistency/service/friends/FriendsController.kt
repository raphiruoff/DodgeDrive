package de.ruoff.consistency.service.friends

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import de.ruoff.consistency.service.friends.Friends.FriendRequest
import de.ruoff.consistency.service.friends.Friends.FriendResponse
import de.ruoff.consistency.service.friends.Friends.UserIdRequest
import de.ruoff.consistency.service.friends.Friends.FriendListResponse
import de.ruoff.consistency.service.friends.Friends.PendingRequestListResponse
import de.ruoff.consistency.service.profile.ProfileRepository

@GrpcService
class FriendsController(
    private val friendRepository: FriendsRepository,
    private val profileRepository: ProfileRepository
) : FriendServiceGrpc.FriendServiceImplBase() {

    override fun sendRequest(request: FriendRequest, responseObserver: StreamObserver<FriendResponse>) {
        try {
            // Überprüfen, ob der Empfänger existiert
            val receiver = profileRepository.findByUsername(request.toUsername)
            if (receiver == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Benutzer nicht gefunden").asRuntimeException())
                return
            }

            // Überprüfen, ob bereits eine Freundschaft besteht
            val existingFriendship = friendRepository.findByRequesterUsernameAndReceiverUsername(request.fromUsername, request.toUsername)
            if (existingFriendship != null && existingFriendship.accepted) {
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Bereits Freunde oder Anfrage bereits akzeptiert").asRuntimeException())
                return
            }

            // Überprüfen, ob bereits eine Anfrage gesendet wurde
            val existingRequest = friendRepository.findByRequesterUsernameAndReceiverUsername(request.fromUsername, request.toUsername)
            if (existingRequest != null) {
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Freundschaftsanfrage existiert bereits").asRuntimeException())
                return
            }

            // Neue Anfrage speichern
            val newRequest = FriendsModel(requesterUsername = request.fromUsername, receiverUsername = request.toUsername)
            friendRepository.save(newRequest)

            responseObserver.onNext(FriendResponse.newBuilder().setMessage("Anfrage gesendet!").build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Senden der Anfrage").withCause(e).asRuntimeException())
        }
    }

    // Methode zum Annehmen einer Freundschaftsanfrage
    override fun acceptRequest(request: FriendRequest, responseObserver: StreamObserver<FriendResponse>) {
        try {
            val pending = friendRepository.findByRequesterUsernameAndReceiverUsername(request.fromUsername, request.toUsername)
            if (pending == null || pending.accepted) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Keine Anfrage gefunden oder bereits akzeptiert").asRuntimeException())
                return
            }

            // Anfrage als akzeptiert markieren
            val updated = pending.copy(accepted = true)
            friendRepository.save(updated)

            responseObserver.onNext(FriendResponse.newBuilder().setMessage("Anfrage angenommen!").build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Akzeptieren der Anfrage").withCause(e).asRuntimeException())
        }
    }

    // Methode zum Ablehnen einer Freundschaftsanfrage
    override fun declineRequest(request: FriendRequest, responseObserver: StreamObserver<FriendResponse>) {
        try {
            val pending = friendRepository.findByRequesterUsernameAndReceiverUsername(request.fromUsername, request.toUsername)
            if (pending == null || pending.accepted) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Keine Anfrage gefunden oder bereits akzeptiert").asRuntimeException())
                return
            }

            // Anfrage löschen
            friendRepository.delete(pending)

            responseObserver.onNext(FriendResponse.newBuilder().setMessage("Anfrage abgelehnt!").build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Ablehnen der Anfrage").withCause(e).asRuntimeException())
        }
    }

    // Methode zum Abrufen der ausstehenden Freundschaftsanfragen
    override fun getPendingRequests(request: UserIdRequest, responseObserver: StreamObserver<PendingRequestListResponse>) {
        try {
            val requests = friendRepository.findByReceiverUsernameAndAcceptedIsFalse(request.username)
            val usernames = requests.map { it.requesterUsername }

            val response = PendingRequestListResponse.newBuilder().addAllRequests(usernames).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Abrufen der ausstehenden Anfragen").withCause(e).asRuntimeException())
        }
    }

    // Methode zum Abrufen der Freunde eines Benutzers
    override fun getFriends(request: UserIdRequest, responseObserver: StreamObserver<FriendListResponse>) {
        try {
            val accepted = friendRepository.findByRequesterUsernameOrReceiverUsernameAndAcceptedTrue(request.username, request.username)
            val friends = accepted.map {
                if (it.requesterUsername == request.username) it.receiverUsername else it.requesterUsername
            }

            val response = FriendListResponse.newBuilder().addAllFriends(friends).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Abrufen der Freunde").withCause(e).asRuntimeException())
        }
    }
}
