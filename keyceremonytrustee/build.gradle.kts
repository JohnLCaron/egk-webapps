plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    application
}

application {
    mainClass.set("electionguard.webapps.keyceremonytrustee.RunKeyCeremonyTrusteeKt")
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

    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging.server)

    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.kotlin.test.junit) // for some reason, we cant use junit5
}