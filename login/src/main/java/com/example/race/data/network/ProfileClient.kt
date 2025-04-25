package com.example.race.data.network


import io.grpc.ClientInterceptors

class ProfileClient : BaseClient() {

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
