package de.ruoff.consistency.service.friends

import de.ruoff.consistency.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import de.ruoff.consistency.service.friends.FriendServiceGrpc
import de.ruoff.consistency.service.friends.Friends.FriendRequest
import de.ruoff.consistency.service.friends.Friends.FriendResponse
import de.ruoff.consistency.service.friends.Friends.UserIdRequest
import de.ruoff.consistency.service.friends.Friends.FriendListResponse
import de.ruoff.consistency.service.friends.Friends.PendingRequestListResponse
import org.slf4j.LoggerFactory

@GrpcService
class FriendsController(
    private val friendRepository: FriendsRepository
) : FriendServiceGrpc.FriendServiceImplBase() {

    private val logger = LoggerFactory.getLogger(FriendsController::class.java)

    // Methode zum Senden einer Freundschaftsanfrage
    override fun sendRequest(request: FriendRequest, responseObserver: StreamObserver<FriendResponse>) {
        try {
            // Überprüfen, ob bereits eine Anfrage existiert
            val existing = friendRepository.findByRequesterUsernameAndReceiverUsername(request.fromUsername, request.toUsername)
            if (existing != null) {
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Anfrage existiert bereits").asRuntimeException())
                return
            }

            // Neue Anfrage speichern
            val newRequest = FriendsModel(requesterUsername = request.fromUsername, receiverUsername = request.toUsername)
            friendRepository.save(newRequest)

            logger.info("Freundschaftsanfrage von ${request.fromUsername} an ${request.toUsername} gesendet.")
            responseObserver.onNext(FriendResponse.newBuilder().setMessage("Anfrage gesendet!").build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Fehler beim Senden der Anfrage: ${e.message}", e)
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Senden der Anfrage").withCause(e).asRuntimeException())
        }
    }

    // Methode zum Annehmen einer Freundschaftsanfrage
    override fun acceptRequest(request: FriendRequest, responseObserver: StreamObserver<FriendResponse>) {
        try {
            // Überprüfen, ob die Anfrage existiert und noch nicht akzeptiert wurde
            val pending = friendRepository.findByRequesterUsernameAndReceiverUsername(request.fromUsername, request.toUsername)
            if (pending == null || pending.accepted) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Keine Anfrage gefunden oder bereits akzeptiert").asRuntimeException())
                return
            }

            // Anfrage als akzeptiert markieren und speichern
            val updated = pending.copy(accepted = true)
            friendRepository.save(updated)

            logger.info("Freundschaftsanfrage von ${request.fromUsername} an ${request.toUsername} wurde akzeptiert.")
            responseObserver.onNext(FriendResponse.newBuilder().setMessage("Anfrage angenommen!").build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Fehler beim Akzeptieren der Anfrage: ${e.message}", e)
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Akzeptieren der Anfrage").withCause(e).asRuntimeException())
        }
    }

    override fun getPendingRequests(request: UserIdRequest, responseObserver: StreamObserver<PendingRequestListResponse>) {
        try {
            // Abrufen der ausstehenden Anfragen für den angegebenen Benutzer
            val requests = friendRepository.findByReceiverUsernameAndAcceptedIsFalse(request.username)

            if (requests.isEmpty()) {
                // Keine ausstehenden Anfragen gefunden
                logger.info("Keine ausstehenden Anfragen für ${request.username}")
            }

            val usernames = requests.map { it.requesterUsername }

            logger.info("Ausstehende Anfragen für ${request.username}: ${usernames}")
            val response = PendingRequestListResponse.newBuilder().addAllRequests(usernames).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            // Fehler beim Abrufen der ausstehenden Anfragen
            logger.error("Fehler beim Abrufen der ausstehenden Anfragen für ${request.username}: ${e.message}", e)
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Abrufen der Anfragen").withCause(e).asRuntimeException())
        }
    }


    // Methode zum Abrufen der Freunde eines Benutzers
    override fun getFriends(request: UserIdRequest, responseObserver: StreamObserver<FriendListResponse>) {
        try {
            val accepted = friendRepository.findByRequesterUsernameOrReceiverUsernameAndAcceptedTrue(request.username, request.username)
            val friends = accepted.map {
                if (it.requesterUsername == request.username) it.receiverUsername else it.requesterUsername
            }

            logger.info("Freunde von ${request.username}: ${friends}")
            val response = FriendListResponse.newBuilder().addAllFriends(friends).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Fehler beim Abrufen der Freunde für ${request.username}: ${e.message}", e)
            responseObserver.onError(Status.INTERNAL.withDescription("Fehler beim Abrufen der Freunde").withCause(e).asRuntimeException())
        }
    }
}
