plugins {
    id("egk.ktor-server-conventions")
}

application {
    mainClass.set("electionguard.webapps.server.RunEgkServerKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}