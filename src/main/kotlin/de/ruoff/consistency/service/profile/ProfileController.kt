package de.ruoff.consistency.service.profile

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class ProfileController(
    private val profileRepository: ProfileRepository
) : ProfileServiceGrpc.ProfileServiceImplBase() {

    override fun getProfile(
        request: Profile.UserIdRequest,
        responseObserver: StreamObserver<Profile.ProfileResponse>
    ) {
        val profile = profileRepository.findByUsername(request.username)

        if (profile == null) {
            responseObserver.onError(
                Status.NOT_FOUND
                .withDescription("Profil nicht gefunden.")
                .asRuntimeException())
            return
        }

        val response = Profile.ProfileResponse.newBuilder()
            .setDisplayName(profile.displayName)
            .setBio(profile.bio ?: "")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}

