// Root build file — global config for Chikura (知蔵) KMP app
// Black & white terminal theme, neat folder app/
// Kotlin 2.0+, Compose Multiplatform 1.7+, Gradle 8

plugins {
    // Root does not need plugins; version catalog is declared in libs.versions.toml
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jogamp.org/deployment/maven")
    }
}
