package de.ruoff.consistency.service.friends

import de.ruoff.consistency.service.profile.ProfileServiceGrpc
import de.ruoff.consistency.service.profile.Profile
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.stereotype.Component

@Component("profileClientFriends")
class ProfileClient {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("profile-service", 9095)
        .usePlaintext()
        .build()

    private val stub = ProfileServiceGrpc.newBlockingStub(channel)

    fun userExists(username: String): Boolean {
        return try {
            val request = Profile.UserIdRequest.newBuilder()
                .setUsername(username)
                .build()

            val response = stub.getProfile(request)
            response.username.isNotBlank()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
