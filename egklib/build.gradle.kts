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
    implementation(files("/home/stormy/dev/github/egk-webapps/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
    implementation("ch.qos.logback:logback-classic:1.3.4")
}

tasks {
    register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "electionguard.wtf")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}