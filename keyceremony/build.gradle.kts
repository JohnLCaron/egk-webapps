plugins {
    id("egk.ktor-client-conventions")
}

application {
    mainClass.set("electionguard.webapps.keyceremony.RunRemoteKeyCeremonyKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

val mockkVersion = "1.13.7"

dependencies {
    testImplementation("io.mockk:mockk:${mockkVersion}")
}
