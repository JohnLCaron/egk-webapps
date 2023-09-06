buildscript {
    repositories {
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version providers.gradleProperty("kotlin_version").get()
}

group = "electionguard.ekglib"
version = "2.0.0"

dependencies {
    implementation(files("../libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

tasks {
    register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "electionguard.verifier.RunVerifierKt")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}