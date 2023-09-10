dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "egk-webapps"

include ("encryptserver")
include ("encryptclient")
include ("keyceremonytrustee")
include ("keyceremony")
include ("decryptingtrustee")
include ("decryption")
include ("egklib")

