plugins {
    id("egk.common-conventions")
    id("io.ktor.plugin")
    application
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-network-tls-certificates")
    implementation("io.ktor:ktor-server-call-logging")

    testImplementation("io.ktor:ktor-server-tests")
}