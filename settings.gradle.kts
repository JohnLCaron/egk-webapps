rootProject.name = "egk-webapps"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libsw") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

include ("encryptserver")
include ("encryptclient")
include ("keyceremonytrustee")
include ("keyceremony")
include ("decryptingtrustee")
include ("decryption")
