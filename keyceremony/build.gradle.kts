buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.9.10"
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("electionguard.webapps.keyceremony.RunRemoteKeyCeremonyKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(files("../libs/egklib-jvm-2.0.4-SNAPSHOT.jar"))
    implementation(libs.bundles.eglib)
    implementation(libs.bundles.ktor.client)

    testImplementation(libs.bundles.ktor.client.test)
}

