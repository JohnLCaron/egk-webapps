plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
    mavenCentral()
}

// 9/9/2023
val coroutinesVersion = "1.6.4" // "1.7.3" see issue #362
val jupitorVersion = "5.10.0"
val kotlinVersion = "1.9.10"
val kotlinxCliVersion = "0.3.6"
val kotlinxDatetimeVersion = "0.4.1"
val kotlinxSerializationCoreVersion = "1.6.0"
val kotestVersion = "5.7.2"
val ktorVersion = "2.3.4"
val logbackVersion = "1.4.11"
val microutilsLoggingVersion = "3.0.5"
val mockkVersion = "1.13.7"
val pbandkVersion = "0.14.2"
val resultVersion = "1.1.18"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")

    //// https://docs.gradle.org/current/userguide/plugins.html Gradle will look for a Plugin Marker Artifact
    // with the coordinates plugin.id:plugin.id.gradle.plugin:plugin.version
    implementation("io.ktor.plugin:io.ktor.plugin.gradle.plugin:${ktorVersion}")
    implementation("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:${kotlinVersion}")

    //// regular libraries
    implementation("com.michael-bull.kotlin-result:kotlin-result:${resultVersion}")
    implementation("pro.streem.pbandk:pbandk-runtime:${pbandkVersion}")
    implementation("io.github.microutils:kotlin-logging:${microutilsLoggingVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:${kotlinxCliVersion}") // TODO This library is obsolete
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${kotlinxDatetimeVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")

    implementation("io.ktor:ktor-serialization-kotlinx-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-server-call-logging:${ktorVersion}")

    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-java:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-auth:${ktorVersion}")
    implementation("io.ktor:ktor-client-logging:${ktorVersion}")

    testImplementation("org.jetbrains.kotlin:kotlin-test:${kotlinVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${kotlinVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${kotlinVersion}")
    testImplementation("io.mockk:mockk:${mockkVersion}")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:${ktorVersion}")
}

group = "electionguard.webapps.server"
version = "2.0.0"
