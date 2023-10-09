plugins {
    id("egk.common-conventions")
}

val kotlinVersion = "1.9.10"
val ktorVersion = "2.3.4"

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${ktorVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${kotlinVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${kotlinVersion}")
}

tasks {
    register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "electionguard.cli.RunVerifierKt")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}