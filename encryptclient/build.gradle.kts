buildscript {
    repositories {
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version providers.gradleProperty("kotlin_version").get()
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "electionguard.webapps.client"
version = "2.0.0"
application {
    mainClass.set("electionguard.webapps.client.RunEgkClientKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(files("/home/stormy/dev/github/egk-webapps/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
    implementation("ch.qos.logback:logback-classic:1.3.4")

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.logging.client)

    testImplementation(libs.kotlin.test.junit)
    // mocking only available on jvm
    testImplementation(libs.mockk)
}