package de.ruoff.consistency.service.profile

import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val profileRepository: ProfileRepository
) {
    fun getProfileByUsername(username: String): ProfileModel? {
        return profileRepository.findByUsername(username)
    }
}


