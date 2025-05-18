package de.ruoff.consistency.service.auth

import de.ruoff.consistency.service.profile.ProfileServiceGrpc
import de.ruoff.consistency.service.profile.Profile.CreateProfileRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.stereotype.Component

@Component("profileClientAuth")
class ProfileClient {

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("profile-service", 9095)
        .usePlaintext()
        .build()

    private val stub = ProfileServiceGrpc.newBlockingStub(channel)

    fun createProfile(username: String): Boolean {
        return try {
            println("📨 Sende createProfile für $username")
            val request = CreateProfileRequest.newBuilder()
                .setUsername(username)
                .build()
            stub.createProfile(request)
            println("✅ Profil erfolgreich erstellt für $username")
            true
        } catch (e: Exception) {
            println("❌ Fehler beim createProfile: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
