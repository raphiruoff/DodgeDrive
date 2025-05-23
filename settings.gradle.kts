pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "DodgeDrive"
include("login")
include(":services:session-service")
include(":services:event-models")
include(":services:game-service")
include(":services:leaderboard-service")
include(":services:profile-service")
include(":services:auth-service")
include(":services:friends-service")
include(":services:log-service")
include(":services:websocket-service")





