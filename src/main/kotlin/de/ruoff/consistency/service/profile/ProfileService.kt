package de.ruoff.consistency.service.profile

import de.ruoff.consistency.service.kafka.ProfileKafkaProducer
import de.ruoff.consistency.service.*
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val profileKafkaProducer: ProfileKafkaProducer
) : ProfileServiceGrpc.ProfileServiceImplBase() {

    override fun createProfile(
        request: CreateProfileRequest,
        responseObserver: StreamObserver<CreateProfileResponse>
    ) {
        val profile = ProfileModel(
            firstName = request.firstName,
            lastName = request.lastName
        )

        val savedProfile = profileRepository.save(profile)


        profileKafkaProducer.publishProfile(savedProfile)

        val response = CreateProfileResponse.newBuilder()
            .setMessage("Profile created with ID: ${savedProfile.id}")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
