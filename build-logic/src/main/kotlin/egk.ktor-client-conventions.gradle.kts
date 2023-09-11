plugins {
    id("egk.common-conventions")
    id("io.ktor.plugin")
    application
}

dependencies {
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-java")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-logging")
    testImplementation("io.mockk:mockk")
}
