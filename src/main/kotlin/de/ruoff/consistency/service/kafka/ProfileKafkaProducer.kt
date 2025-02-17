package de.ruoff.consistency.service.kafka


import de.ruoff.consistency.service.profile.ProfileModel
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ProfileKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, ProfileModel>
) {
    fun publishProfile(profile: ProfileModel) {
        kafkaTemplate.send("profile-created", profile)
    }
}
