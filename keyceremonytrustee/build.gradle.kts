plugins {
    application
}

application {
    mainClass.set("electionguard.webapps.keyceremonytrustee.KeyCeremonyRemoteTrusteeKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.kotlin.result)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging.server)

    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.kotlin.test.junit) // for some reason, we cant use junit5
}