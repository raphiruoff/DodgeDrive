plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
}


group = "de.ruoff"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}
