pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DodgeDrive"
include("login")
include(":services:session-service")
include(":services:event-models")
include(":services:game-service")
include(":services:leaderboard-service")
include(":services:profile-service")



