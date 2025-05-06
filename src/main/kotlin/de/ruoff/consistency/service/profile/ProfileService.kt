package de.ruoff.consistency.service.profile

import de.ruoff.consistency.events.HighscoreEvent
import de.ruoff.consistency.service.profile.events.HighscoreProducer
import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val highscoreProducer: HighscoreProducer
) {
    fun getProfileByUsername(username: String): ProfileModel? {
        return profileRepository.findByUsername(username)
    }

    fun updateHighscoreIfNeeded(username: String, newScore: Int) {
        val profile = profileRepository.findByUsername(username)
        if (profile != null && newScore > profile.highscore) {
            profile.highscore = newScore
            profileRepository.save(profile)

            val event = HighscoreEvent(
                username = profile.username,
                highscore = profile.highscore
            )
            highscoreProducer.send(event)
        }
    }
}

