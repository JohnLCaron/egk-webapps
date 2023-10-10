import gradle.kotlin.dsl.accessors._876355aa29f9023816cc3fbdd2324fe4.implementation

plugins {
    id("egk.common-conventions")
    id("io.ktor.plugin")
    application
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-network-tls-certificates")
    implementation("io.ktor:ktor-server-call-logging")

    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-server-tests")
}