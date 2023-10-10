import gradle.kotlin.dsl.accessors._876355aa29f9023816cc3fbdd2324fe4.implementation

plugins {
    id("egk.common-conventions")
    id("io.ktor.plugin")
    application
}

dependencies {

    implementation("io.ktor:ktor-serialization-kotlinx-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-java")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-logging")
    testImplementation("io.mockk:mockk")
}
