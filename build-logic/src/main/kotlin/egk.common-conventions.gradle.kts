plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

val coroutinesVersion = "1.7.3"
val kotlinVersion = "1.9.10"
val kotlinResultVersion = "1.1.18"
val kotlinxCliVersion = "0.3.6"
val kotlinxDatetimeVersion = "0.4.1"
val ktorVersion = "2.3.4"
val logbackVersion = "1.4.11"
val microutilsLoggingVersion = "3.0.5"
val mockkVersion = "1.13.7"
val pbandkVersion = "0.14.2"

dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("org.apache.commons:commons-text:1.10.0")
    }

    // TODO use versionCatalogs for the ones with versions ??
    implementation(files("../libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("com.michael-bull.kotlin-result:kotlin-result:$kotlinResultVersion")
    implementation("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
    implementation("io.github.microutils:kotlin-logging:$microutilsLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("io.ktor:ktor-serialization-kotlinx-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("ch.qos.logback:logback-classic")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.9.3")
        }
    }
}
