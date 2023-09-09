plugins {
    id("egk.ktor-server-conventions")
}

application {
    mainClass.set("electionguard.webapps.decryptingtrustee.RunDecryptingTrusteeKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}