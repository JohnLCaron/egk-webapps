buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.9.22"
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(files("../libs/egk-ec-2.1-SNAPSHOT.jar"))
    implementation(files("../libs/verificatum-vecj-2.2.0.jar"))
    implementation(libs.bundles.eglib)
    implementation(libs.bundles.ktor.server)

    testImplementation(libs.bundles.ktor.server.test)
}