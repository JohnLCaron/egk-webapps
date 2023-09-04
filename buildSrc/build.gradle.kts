val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

repositories {
    mavenCentral()
}

group = "electionguard.webapps.server"
version = "2.0.0"

dependencies {
    implementation(files("/home/stormy/dev/github/egk-webapps/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
}
