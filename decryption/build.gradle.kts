
buildscript {
    repositories {
    }
}

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

application {
    mainClass.set("electionguard.webapps.decryption.RunRemoteDecryptionKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(files("/home/stormy/dev/github/egk-webapps/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.result)
    implementation(libs.pbandk) // needed to encrypt Contest data
    implementation(libs.microutils.logging)

    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.bundles.logging.client)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.ktor.client.content.negotiation)
}