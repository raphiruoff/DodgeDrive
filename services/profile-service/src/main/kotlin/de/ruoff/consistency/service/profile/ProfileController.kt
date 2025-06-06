package de.ruoff.consistency.service.profile

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class ProfileController(
    private val profileService: ProfileService
) : ProfileServiceGrpc.ProfileServiceImplBase() {

    override fun getProfile(
        request: Profile.UserIdRequest,
        responseObserver: StreamObserver<Profile.ProfileResponse>
    ) {
        val profile = profileService.getProfileByUsername(request.username)

        if (profile == null) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Profil nicht gefunden.")
                    .asRuntimeException()
            )
            return
        }

        val response = Profile.ProfileResponse.newBuilder()
            .setUsername(profile.username)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun createProfile(
        request: Profile.CreateProfileRequest,
        responseObserver: StreamObserver<Profile.CreateProfileResponse>
    ) {
        try {
            val profile = ProfileModel(
                username = request.username
            )
            profileService.saveProfile(profile)

            val response = Profile.CreateProfileResponse.newBuilder()
                .setMessage("Profil erfolgreich erstellt")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Fehler beim Erstellen des Profils: ${e.message}")
                    .asRuntimeException()
            )
        }
    }
}
