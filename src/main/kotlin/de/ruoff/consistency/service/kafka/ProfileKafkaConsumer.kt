package de.ruoff.consistency.service.kafka


import de.ruoff.consistency.service.profile.ProfileModel
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class ProfileKafkaConsumer {

    @KafkaListener(topics = ["profile-created"], groupId = "profile-group")
    fun consumeProfile(record: ConsumerRecord<String, ProfileModel>) {
        val profile = record.value()
        println("Received Profile: ${profile.firstName} ${profile.lastName}")

    }
}
