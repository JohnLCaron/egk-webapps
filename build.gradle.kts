val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

group = "electionguard.webapps.server"
version = "2.0.0"

application {
    mainClass.set("electionguard.webapps.server.RunEgkServerKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    flatDir {
        dirs("libs")
    }
    mavenCentral()
}

dependencies {
    implementation(files("/home/stormy/dev/github/egk-webapps/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
    implementation("ch.qos.logback:logback-classic:1.3.4")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation("io.ktor:ktor-client-content-negotiation-jvm")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
}
