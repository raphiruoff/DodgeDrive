package com.example.race.data.network


import de.ruoff.consistency.service.profile.Profile
import de.ruoff.consistency.service.profile.ProfileServiceGrpc
import io.grpc.ClientInterceptors

class ProfileClient : BaseClient(overridePort = 9095) {

    private val jwtInterceptor = JwtClientInterceptor { TokenHolder.jwtToken }
    private val interceptedChannel = ClientInterceptors.intercept(channel, jwtInterceptor)
    private val stub = ProfileServiceGrpc.newBlockingStub(interceptedChannel)

    fun loadProfile(username: String): Profile.ProfileResponse? {
        val request = Profile.UserIdRequest.newBuilder()
            .setUsername(username)
            .build()

        return try {
            stub.getProfile(request)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
